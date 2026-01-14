FROM openjdk:17-jre-slim

WORKDIR /app

EXPOSE 8080

COPY build/libs/Urikkiri-Server-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-Xms256m", "-Xmx512m", "-XX:+UseContainerSupport", "-jar", "app.jar"]