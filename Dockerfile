FROM navikt/java:11
ENV APP_NAME=familie-ef-sak
COPY ./target/familie-ef-sak.jar "app.jar"
