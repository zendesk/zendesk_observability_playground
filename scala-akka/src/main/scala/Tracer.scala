import akka.http.scaladsl.server.Directive1

import scala.util.Try

trait Span {
}

trait Tracer {

}

trait TracingDirectives {
  def trace(tracer: Tracer)(operationName: String): Directive1[Span] = {
    extractRequest.flatMap { req =>
      val parent = Try(
        // This method will throw an IllegalArgumentException for a bad
        // tracer header, or return null for no header. Handle both cases as None
        tracer.extract(Format.Builtin.HTTP_HEADERS, new AkkaHttpHeaderExtractor(req.headers))
      ).filter(_ != null).toOption
      val span = parent.fold(
        tracer.buildSpan(operationName).start())(
        p => tracer.buildSpan(operationName).asChildOf(p).start()
      )
      mapResponse { resp =>
        span.setTag("http.status_code", resp.status.intValue())
        span.setTag("http.url", req.effectiveUri(securedConnection = false).toString())
        span.setTag("http.method", req.method.value)
        span.finish()
        resp
      } & provide(span)
    }
  }
}
