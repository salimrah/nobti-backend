FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S noba && adduser -S noba -G noba

WORKDIR /app

COPY target/noba-backend-*.jar app.jar

RUN chown noba:noba app.jar

USER noba

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
