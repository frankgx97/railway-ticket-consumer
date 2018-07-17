FROM openjdk:8-jre-alpine3.8

WORKDIR /app

COPY ./target/TicketSystemConsumer-0.0.1-SNAPSHOT.jar .

CMD java -jar /app/TicketSystemConsumer-0.0.1-SNAPSHOT.jar