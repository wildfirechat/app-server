FROM openjdk:8-jre-alpine

COPY ../target/app-*.jar /opt/app-server/app.jar
COPY ../config  /opt/app-server/config

WORKDIR /opt/app-server

VOLUME /opt/app-server/config

EXPOSE 8888/tcp

ENV JVM_XMX 256M
ENV JVM_XMS 256M

CMD java -server -Xmx$JVM_XMX -Xms$JVM_XMS -jar app.jar
