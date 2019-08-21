package main

import (
	gintrace "gopkg.in/DataDog/dd-trace-go.v1/contrib/gin-gonic/gin"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
	"net"
	"os"

	"github.com/gin-gonic/gin"
)

func main() {

	addr := net.JoinHostPort(
		os.Getenv("DD_AGENT_HOST"),
		os.Getenv("DD_TRACE_AGENT_PORT"),
	)

	tracer.Start(tracer.WithAgentAddr(addr))
	defer tracer.Stop()

	// Create a gin.Engine
	r := gin.Default()

	// Use the tracer middleware with your desired service name.
	r.Use(gintrace.Middleware("go-service"))

	// Continue using the router as normal.
	r.GET("/go/hello", func(c *gin.Context) {
		c.String(200, "Hello World!")
	})

	r.GET("/go/ping", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "pong",
		})
	})

	r.Run(":9001")
}
