# auth-gateway-service
This service acts as an gateway that authenticates requests and enforces distributed rate limits (Redis) before forwarding to backend services.

# Features
* Authentication (Bearer token header)
* Global rate-limiting scopes with Token bucket algo (using redis)
* Fail-open fallback with sliding window algo limiter when Redis is unavailable
* Routes to product service