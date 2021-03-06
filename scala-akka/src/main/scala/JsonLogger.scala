import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import spray.json._

object JsonLogger {

  def logRequest(req: HttpRequest, resp: HttpResponse): String = {
    val traceCtx = ZendeskTracing.extractInternalTraceContext(req)
    val headers = req.headers.map( h => JsObject((h.name(), JsString(h.value())))).toVector
    val jsonLogMessage = JsObject(
      ("http.method", JsString(req.method.value)),
      ("http.uri", JsString(req.uri.toString())),
      ("http.status_code", JsNumber(resp.status.intValue())),
      ("http.headers", JsArray(headers)),
      ("dd.trace_id", traceCtx.map(t => JsNumber(t.traceId)).getOrElse(JsNull)),
      ("dd.span_id", traceCtx.map(t => JsNumber(t.spanId)).getOrElse(JsNull)),
    )
    jsonLogMessage.compactPrint
  }
}


