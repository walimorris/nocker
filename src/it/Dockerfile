FROM alpine:latest

RUN apk add --no-cache socat

CMD socat TCP-LISTEN:8082,fork EXEC:'/bin/cat' & \
    socat TCP-LISTEN:8087,fork EXEC:'/bin/cat' & \
    sleep infinity
