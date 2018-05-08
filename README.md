# lambdaroyal-ws-printing-server
A locally installed printing server connected via websocket to cloud-based server that want to stream labels and PDF documents to local printers

# Building

> mvn install

# Running

> java -Dserver.port=8070 -jar ws-printing-server-0.1.0.jar -ws ws://localhost:8080/autobahn -jwt /home/gix/Documents/jwt

where 

*ws* denotes the websocket address to connect to.
*jwt* is an optional file containing JSON webtoken file used to authenticate the printing server with a host app that sends in the labels
to print
