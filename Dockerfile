FROM openjdk:18-jdk
EXPOSE 8080

COPY ./app.jar app.jar
ENTRYPOINT [ "java", "-jar", "/app.jar" ]