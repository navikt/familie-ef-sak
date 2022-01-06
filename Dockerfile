FROM navikt/java:17

ENV APP_NAME=familie-ef-sak
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY ./target/familie-ef-sak.jar "app.jar"
