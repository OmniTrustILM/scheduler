# Build stage
FROM maven:3.9.16-eclipse-temurin-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
COPY settings.xml /root/.m2/settings.xml
COPY docker /home/app/docker
ARG SERVER_USERNAME
ARG SERVER_PASSWORD
RUN mvn -f /home/app/pom.xml clean package

# Package stage
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.authors="ILM <support@otilm.com>"

# add non root user otilm
RUN addgroup --system --gid 10001 otilm && adduser --system --home /opt/otilm --uid 10001 --ingroup otilm otilm

COPY --from=build /home/app/docker /
COPY --from=build /home/app/target/*.jar /opt/otilm/app.jar

WORKDIR /opt/otilm

ENV JDBC_URL=
ENV JDBC_USERNAME=
ENV JDBC_PASSWORD=
ENV DB_SCHEMA=scheduler
ENV PORT=8080
ENV JAVA_OPTS=
ENV BROKER_URL=
ENV BROKER_HOST=
ENV BROKER_PORT=5672
ENV BROKER_USERNAME=
ENV BROKER_PASSWORD=
ENV BROKER_VIRTUAL_HOST=/
ENV BROKER_EXCHANGE=ilm
ENV BROKER_ROUTING_KEY_SCHEDULER=scheduler
ENV BROKER_TYPE=RABBITMQ


USER 10001

ENTRYPOINT ["/opt/otilm/entry.sh"]
