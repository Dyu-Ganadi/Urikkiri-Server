FROM openjdk:26-ea-30-jdk-oraclelinux9
WORKDIR /app
EXPOSE 9484
COPY build/libs/Urikkiri-Server-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-Xms256m", "-Xmx512m", "-XX:+UseContainerSupport", "-jar", "app.jar"]