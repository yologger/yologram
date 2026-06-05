FROM amazoncorretto:17-alpine
WORKDIR /app
COPY build/libs/*.jar application.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "application.jar"]
