import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import datadog.trace.api.CorrelationIdentifier
import spray.json._

object JsonLogger {

  def logRequest(req: HttpRequest, resp: HttpResponse): String = {
    val headers = req.headers.map( h => JsObject((h.name(), JsString(h.value())))).toVector
    val jsonLogMessage = JsObject(
      ("http.method", JsString(req.method.value)),
      ("http.uri", JsString(req.uri.toString())),
      ("http.status_code", JsNumber(resp.status.intValue())),
      ("http.headers", JsArray(headers)),
      ("dd.trace_id", JsString(CorrelationIdentifier.getTraceId)),
      ("dd.span_id", JsString(CorrelationIdentifier.getSpanId)),
    )
    jsonLogMessage.compactPrint
  }
}


