package main

import (
	log "github.com/sirupsen/logrus"
	muxtrace "gopkg.in/DataDog/dd-trace-go.v1/contrib/gorilla/mux"

	"encoding/json"
	"fmt"
	"net/http"
	"os"
)

func main() {
	// Datadog provides a contributed wrapper for gorilla/mux
	// https://github.com/DataDog/dd-trace-go/blob/v1/contrib/gorilla/mux/example_test.go
	// This would look like `mux := mux.NewRouter()` without the wrapper
	mux := muxtrace.NewRouter(muxtrace.WithServiceName("go-mux"))

	// logrus is a popular Go structured logging library (https://github.com/sirupsen/logrus).
	// Here we're hard-coding to use JSON output, but can also switch
	// to regular output for development environments
	log.SetFormatter(&log.JSONFormatter{})
	log.SetOutput(os.Stdout)

	mux.HandleFunc("/go/ping", PingHandler)

	log.Fatal(http.ListenAndServe(":9001", mux))
}

func PingHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	err := json.NewEncoder(w).Encode(map[string]string{"message": "pong"})
	if err != nil {
		log.WithFields(log.Fields{
			"http.method":      r.Method,
			"http.path":        r.URL.Path,
			"http.status_code": http.StatusInternalServerError,
		}).Error(fmt.Errorf("failed to encode response: %v", err))
	} else {
		// TODO: How to log request processing time?
		log.WithFields(log.Fields{
			"http.method":      r.Method,
			"http.path":        r.URL.Path,
			"http.status_code": http.StatusOK,
		}).Info()
	}
}
