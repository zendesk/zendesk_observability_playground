FROM golang:1.12.6

RUN go get gopkg.in/DataDog/dd-trace-go.v1/contrib/gin-gonic/gin \
  && go get github.com/opentracing/opentracing-go \
  && go get gopkg.in/DataDog/dd-trace-go.v1/ddtrace

WORKDIR /app
ENV GOPATH="/go"
ADD ./go/* /app

EXPOSE 9001
CMD ["go", "run", "."]