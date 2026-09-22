# ==========================================
# Etapa 1: Construcción (Build)
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el archivo pom.xml y descargamos dependencias (optimiza la caché)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente de la aplicación
COPY src ./src

# Compilamos el proyecto y generamos el .jar omitiendo las pruebas (para mayor rapidez)
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Ejecución (Runtime)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos únicamente el .jar generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto en el que correrá Spring Boot (Render usa la variable PORT)
EXPOSE 8080

# Comando de inicio de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
