FROM navikt/java:11

ENV APP_NAME=familie-ef-sak
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
COPY ./target/familie-ef-sak.jar "app.jar"
