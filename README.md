Welcome to the Zendesk Observability Playground! This repo contains working examples of how to implement various observability patterns across our infrastructre and in our applications. This is not intended to be close to our production infrastructure, but rather provide simple to understand components and make it easy to experiment with them. 

Currently, we have the following routing topology:
```
                            +--------------+
                            |              |
                            |    golang    |
                            |              |
                            +------^-------+
                                   |
+-------+      +-------+           |          +-------------+
|       |      |       +-----------+          |             |
| NGINX +----->+ Envoy |----------------------> scala-akka  |
|       |      |       +-----------+          |             |
+-------+      +-------+           |          +-------------+
                                   |
                            +------v-------+
                            |              |
                            | ruby-sinatra |
                            |              |
                            +--------------+
```


### Getting Started ###
- Clone this repo
- Install Docker with `docker-compose`
- `export DD_API_KEY=<your api key>` (get a key from datadoghq.com/account/settings#api)
- Run `docker-compose up`
- Try some of the services: `curl localhost:8080/go/ping`

### Explore ###
- Check out `nginx/nginx.conf`. Nginx is currently the "edge" proxy.
- Check out `envoy/envoy.yaml` to see how it routes to each service.
