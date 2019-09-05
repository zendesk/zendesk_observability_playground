import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import akka.http.scaladsl.server.RouteResult
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import ZendeskTracing.optionalTraceContext

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object WebServer {
  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    val portNumber: Int = 8090

    def requestMethodAndResponseStatusAsInfo(req: HttpRequest): RouteResult => Option[LogEntry] = {
      case RouteResult.Complete(res) =>
        Some(LogEntry(JsonLogger.logRequest(req, res), Logging.InfoLevel))
      case RouteResult.Rejected(rejections) =>
        Some(LogEntry(s"Failed Request: $rejections", Logging.ErrorLevel))
    }
    val debugLogger = DebuggingDirectives.logRequestResult(requestMethodAndResponseStatusAsInfo _)

    case class PingResponse(message: String)
    implicit val pingFormat: RootJsonFormat[PingResponse] = jsonFormat1(PingResponse)

    // Extract the trace context in the route to be passed to other methods
    // such as JDBC calls or other HTTP calls
    val route = debugLogger { optionalTraceContext { traceContext =>
      path("scala-akka" / "ping") {
        get {
          complete(PingResponse("pong"))
        }
      }
    }}

    // This is a convenient way to wrap all requests/responses with tracing. Unfortunately
    // it does nothing to propagate the trace context to upstream services (database calls, additional http services, etc)
    def requestHandler(req: HttpRequest): Future[HttpResponse] = {
      val startedSpan = ZendeskTracing.extractTraceInfo(req)
      val responseFuture = Source.single(req)
        .via(ZendeskTracing.internalTraceFlow) // Add the internal trace header
        .via(route)
        .runWith(Sink.head)
      responseFuture.onComplete {
        case Success(response) =>
          startedSpan.foreach { ss =>
            ZendeskTracing.completeSpan(ss, req, response)}
        case Failure(ex) =>
          startedSpan.foreach { ss =>
            ZendeskTracing.completeFailedSpan(ss, req, ex)}
      }
      responseFuture
    }
    // Have to use bindAndHandleAsync for tracing to work.
    // https://github.com/DataDog/dd-trace-java/blob/master/dd-java-agent/instrumentation/akka-http-10.0/src/main/java/datadog/trace/instrumentation/akkahttp/AkkaHttpServerInstrumentation.java#L64-L69
    val bindingFuture = Http().bindAndHandleAsync(requestHandler, "0.0.0.0", portNumber)

    // Block forever so service doesn't exit
    while (true) {
      Thread.sleep(10000)
    }

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
