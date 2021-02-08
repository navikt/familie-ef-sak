FROM navikt/java:11

COPY init.sh /init-scripts/init.sh

ENV APP_NAME=familie-ef-sak
COPY ./target/familie-ef-sak.jar "app.jar"
