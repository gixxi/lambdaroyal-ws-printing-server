# lambdaroyal-ws-printing-server
A locally installed printing server connected via websocket to cloud-based server that want to stream labels and PDF documents to local printers

# Current Version

0.8.0

# Dependencies

Planet-Rocklog Build 1567 and above

# Building

> mvn install

# Running

> java -Dserver.port=8070 -jar ws-printing-server-0.8.0.jar -s gix-dell -jwt /home/gix/Documents/jwt-metas -url https://metas.planetrocklog.com/system/info

where 

- *s* denotes the name of the server that is registered as io-internal-proxy, e.g. the name of the host the printing server is started on
- *url* the webservice URL of Planet-Rocklog's /system/info service
- *jwt* is an optional file containing JSON webtoken file used to authenticate the printing server with a host app that sends in the labels
to print
- *tsp* telegramserverport
