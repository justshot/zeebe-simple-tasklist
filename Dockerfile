FROM openjdk:17.0.1-jdk-slim

COPY --chown=app:app target/*.jar /libs/simple-task-list.jar

EXPOSE 8081

ENTRYPOINT ["java", "-server", "-jar", "/libs/simple-task-list.jar"]