import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.Materializer
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat, _}

import scala.concurrent.ExecutionContext
import scala.util.{Random, Success}

object ZendeskTracing {
  import SnakifiedSprayJsonSupport._

  def epocNanoseconds: BigInt = Instant.now().toEpochMilli * 1000000

  val apmHost: String = System.getenv("DD_AGENT_HOST")
  val apmPort: String = System.getenv("DD_TRACE_AGENT_PORT")

  case class StartedSpan(traceId: BigInt, parentSpanId: BigInt) {
    // Datadog APM expects nanoseconds
    val start: BigInt = ZendeskTracing.epocNanoseconds
    val spanId: BigInt = {
      val span = BigInt(Random.nextBytes(8))
      if (span < 0) span * -1 else span
    }
  }

  sealed trait ServiceType {val stringRepr: String}
  case object Web extends ServiceType { val stringRepr = "web"}
  case object Database extends ServiceType { val stringRepr = "db"}
  case object Cache extends ServiceType { val stringRepr = "cache"}
  case object Custom extends ServiceType { val stringRepr = "custom"}

  // Traces use a lot of 64 bit unsigned values, which unfortunately don't work
  // as java Long(s) because Long is signed. :sadpanda:
  case class CompletedSpan(
    traceId: BigInt, spanId: BigInt, name: String, resource: String,
    service: String, `type`: Option[ServiceType], start: BigInt,
    duration: BigInt, parentId: Option[BigInt], error: Int = 0,
    meta: Map[String, String] = Map.empty,
    metrics: Map[String, Float] = Map.empty) {
  }

  def extractTraceInfo(r: HttpRequest): Option[StartedSpan] = {
    val traceId: Option[BigInt] = r.headers.find(_.name().toLowerCase == "x-datadog-trace-id")
      .map( h => BigInt(h.value()))
    val parentId: Option[BigInt] = r.headers.find(_.name().toLowerCase == "x-datadog-parent-id")
      .map( h => BigInt(h.value()))
    val samplingPriority: Option[Int] = r.headers
      .find(_.name().toLowerCase == "x-datadog-sampling-priority").flatMap(h => h.value().toIntOption)
    (samplingPriority, traceId, parentId) match {
      case (Some(1), Some(tid), Some(pid)) =>
        // Only sample if we have all three. In production, we might want to start
        // traces in some cases if some of these values are missing. For this example,
        // we will just support distributed traces.
        Some(StartedSpan(traceId = tid, parentSpanId = pid))
      case (_, _, _) =>
        None // Don't trace if any value is missing
    }
  }

  val serviceName: String = "scala-akka"
  val serviceType: Web.type = Web
  val resourceName: String = "ping"

  def completeSpan(started: StartedSpan, req: HttpRequest, resp: HttpResponse)
    (implicit system: ActorSystem, m: Materializer, ec: ExecutionContext): Unit = {
    val completed = CompletedSpan(
      traceId = started.traceId,
      spanId = started.spanId,
      name = req.uri.path.toString(),
      resource = resourceName,
      service = serviceName,
      `type` = Some(serviceType),
      start = started.start,
      duration = epocNanoseconds - started.start,
      parentId = Some(started.parentSpanId),
      meta = Map(
        "http.status_code" -> resp.status.intValue().toString,
        "http.method" -> req.method.value,
        "http.url" -> req.uri.path.toString()
      )
    )
    submitSpan(completed)
  }

  def completeFailedSpan(started: StartedSpan, req: HttpRequest, ex: Throwable)
    (implicit system: ActorSystem, m: Materializer, ec: ExecutionContext): Unit = {
    val completed = CompletedSpan(
      traceId = started.traceId,
      spanId = started.spanId,
      name = req.uri.path.toString(),
      resource = resourceName,
      service = serviceName,
      `type` = Some(serviceType),
      start = started.start,
      duration = epocNanoseconds - started.start,
      parentId = Some(started.parentSpanId),
      error = 1,
      meta = Map("exception" -> ex.getMessage)
    )
    submitSpan(completed)
  }

  implicit object ServiceTypeFormat extends RootJsonFormat[ServiceType] {
    def write(st: ServiceType) = JsString(st.stringRepr)
    def read(value: JsValue): ServiceType = value match {
      case JsString(Web.stringRepr) => Web
      case JsString(Database.stringRepr) => Database
      case JsString(Cache.stringRepr) => Cache
      case JsString(Custom.stringRepr) => Custom
      case _ => throw DeserializationException("Couldn't determine ServiceType")
    }
  }
  implicit val spanFormat: RootJsonFormat[CompletedSpan] = jsonFormat12(CompletedSpan)

  def submitSpan(s: CompletedSpan)
    (implicit system: ActorSystem, m: Materializer, ec: ExecutionContext): Unit = {
    // https://docs.datadoghq.com/api/?lang=python#send-traces
    val entity: RequestEntity = HttpEntity(
      ContentTypes.`application/json`,
      List(List(s)).toJson.compactPrint)
    // Super naive implementation, quickly fails when volume is high because it runs out of threads.
    // Should be a stream of traces so they can be bulk-posted.
    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"http://$apmHost:$apmPort/v0.3/traces",
      entity = entity)
    println(s"Submitting trace: ${List(List(s)).toJson.prettyPrint}")
    val responseFuture = Http().singleRequest(request)
    responseFuture.onComplete {
      case Success(response) if response.status == StatusCodes.OK =>
        println("Successfully submitted a trace")
      case Success(response) =>
        println(s"Failed to submit a trace. Status:${response.status}")
    }
  }
}
