FROM maven:3.9-eclipse-temurin-21-alpine as build
WORKDIR /workspace/app

# Copy pom.xml to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || echo "Skipping offline deps"

COPY src src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target
COPY --from=build ${DEPENDENCY}/*.jar app.jar

# Run with production profile by default
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-jar","/app.jar"]
