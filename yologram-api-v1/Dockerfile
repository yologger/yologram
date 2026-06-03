FROM amazoncorretto:17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar application.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "application.jar"]
