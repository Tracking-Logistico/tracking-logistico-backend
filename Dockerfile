FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn -B -Dmaven.test.skip=true -Djacoco.skip=true clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=postgres \
    SPRING_PROFILE=postgres \
    DB_PORT=5432 \
    DB_NAME=tracking_psep \
    DB_USER=tracking_psep_user \
    MAIL_PROVIDER=resend \
    MAIL_LOG_ACTION_LINKS=false

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
