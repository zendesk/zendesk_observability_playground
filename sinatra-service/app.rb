require 'sinatra'
require 'ddtrace'
require 'ddtrace/contrib/sinatra/tracer'
require 'dotenv'
require 'json'

Dotenv.load

Datadog.configure do |c|
  c.use :sinatra, {}
end

get '/sinatra/ping' do
	Datadog.tracer.trace(request.path) do |span|
		  # Build a context from headers (`env` must be a Hash)
  		context = Datadog::HTTPPropagator.extract(request.env)
  		Datadog.tracer.provider.context = context if context.trace_id
    	span.set_tag('http.url', request.path )
    	span.set_tag('http.method', request.request_method)
    	span.set_tag('http.host', request.host)
    	span.set_tag('http.client_ip', request.ip)

      content_type :json
      { :message => 'pong'}.to_json
  	end
end
