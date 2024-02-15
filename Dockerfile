FROM gcr.io/distroless/java21-debian12:nonroot
ENV TZ="Europe/Oslo"
ENV APP_NAME=familie-ef-sak
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY ./target/familie-ef-sak.jar "app.jar"
CMD ["-jar", "/app.jar"]