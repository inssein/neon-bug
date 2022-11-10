FROM gradle:7.5-jdk17 AS build
COPY . /tmp
WORKDIR /tmp
RUN gradle bootJar --no-daemon

FROM amazoncorretto:17
COPY --from=build /tmp/build/libs/bug-0.0.1-SNAPSHOT.jar bug-0.0.1-SNAPSHOT.jar
EXPOSE 8080
CMD ["java", "-jar", "bug-0.0.1-SNAPSHOT.jar"]