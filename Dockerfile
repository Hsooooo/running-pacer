# build stage
FROM gradle:8.14-jdk17 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle bootJar --no-daemon

# run stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]