# ==========================================
# Etapa 1: Construcción (Build)
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el archivo pom.xml y descargamos dependencias (optimiza la caché)
COPY pom.xml .
RUN grep -q '<annotationProcessorPaths>' pom.xml \
 && grep -q '<proc>full</proc>' pom.xml \
 && grep -q '<lombok.version>1.18.48</lombok.version>' pom.xml \
 && echo 'POM_LOMBOK_CONFIG_OK'
RUN mvn dependency:go-offline -B

# Copiamos el código fuente de la aplicación
COPY src ./src

# Compilamos el proyecto y generamos el .jar omitiendo las pruebas (para mayor rapidez)
RUN mvn -B -Dmaven.compiler.proc=full -Dmaven.test.skip=true -Djacoco.skip=true clean package

# ==========================================
# Etapa 2: Ejecución (Runtime)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Valores de ejemplo existentes; Render puede sustituirlos al ejecutar el servicio.
# No establecer DB_HOST: el host real de PostgreSQL en Render no figura en el repositorio.
# No incorporar claves SMTP/API o contrasenas reales en una imagen publica.
ENV SPRING_PROFILE=postgres \
    DB_PORT=5432 \
    DB_NAME=tracking_psep \
    DB_USER=tracking_psep_user \
    MAIL_PROVIDER=resend \
    MAIL_LOG_ACTION_LINKS=false

# Copiamos únicamente el .jar generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto en el que correrá Spring Boot (Render usa la variable PORT)
EXPOSE 8080

# Comando de inicio de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
