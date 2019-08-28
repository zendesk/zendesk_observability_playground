FROM golang:1.12.6

RUN  go get gopkg.in/DataDog/dd-trace-go.v1/contrib/gorilla/mux \
  && go get github.com/sirupsen/logrus

WORKDIR /app
ENV GOPATH="/go"
ADD ./server.go /app

EXPOSE 9001
CMD ["go", "run", "."]
