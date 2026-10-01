# Tracking Logístico — Backend

Proyecto Spring Boot modular (`usuarios`, `pedidos`, `rutas`). El backend y el frontend son repositorios independientes. El Docker Compose de este repositorio puede construir ambos cuando sus carpetas están al mismo nivel.

## Ejecución local desde Java

Con JDK 17 o superior, ejecuta `DemoApplication.java` directamente desde el IDE o inicia:

```bash
./mvnw spring-boot:run
```

El perfil predeterminado es `h2`. Hibernate crea y actualiza el esquema JPA en una base H2 en memoria; `db/h2/schema.sql` crea, después de JPA, las tablas auxiliares de historial de asignaciones y notificaciones utilizadas por JDBC. Al detener el proceso se pierde la información en memoria. No requiere Docker ni PostgreSQL. El correo saliente está desactivado por defecto en H2; puedes configurar `MAIL_ENABLED=true` y un servidor local si necesitas probarlo. H2 es un entorno de desarrollo, no una base equivalente a PostgreSQL para todas las consultas y restricciones.

`src/main/resources/application.yml` contiene opciones comunes versionadas y activa H2 por defecto. `application-h2.properties`, `application-postgres.properties` y `application-test.properties` son perfiles versionados. El archivo `src/main/resources/application.properties` es opcional y está ignorado por Git: úsalo únicamente para ajustes privados de tu computador. Si existe, puede sobreescribir los valores de `application.yml`. No guardes credenciales en archivos versionados.

## Docker local, PostgreSQL y frontend

Ubica ambos repositorios como carpetas hermanas, con los nombres `tracking-logistico-backend-main` y `tracking-logistico-frontend-main`:

```bash
cd tracking-logistico-backend-main
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Mailpit: http://localhost:8025
- PostgreSQL local: puerto 5432; las credenciales de desarrollo están en `docker-compose.yml`.

Docker selecciona explícitamente `postgres` con `SPRING_PROFILES_ACTIVE=postgres` y conserva las variables de entorno existentes, Flyway y `spring.jpa.hibernate.ddl-auto=validate`. El servicio publicado mediante este Dockerfile también utiliza PostgreSQL de forma explícita. No borres el volumen PostgreSQL si necesitas conservar sus datos; `docker compose down -v` los elimina. Si inicias el JAR fuera de este Dockerfile en Render u otro servidor, configura explícitamente `SPRING_PROFILES_ACTIVE=postgres` o `SPRING_PROFILE=postgres`, además de `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`. Verifica estas variables antes de desplegar: sin perfil explícito, el JAR utiliza H2.

## PostgreSQL desde el IDE sin Docker

Activa `postgres` desde la configuración de ejecución, con PostgreSQL funcionando y las variables de conexión:

```bash
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Si ejecutas solo el Main y quieres este perfil, configura `SPRING_PROFILES_ACTIVE=postgres` en tu IDE. No cambies el perfil local predeterminado de H2 para desplegar en producción.

## Crear usuarios internos en Docker

```bash
docker compose exec backend java -jar app.jar --spring.profiles.active=postgres,cli crear --nombre='Operador Prueba' --email='operador@ejemplo.com' --telefono='+573001234567' --rol=OPERADOR --codigoEmpleado=OP001

docker compose exec backend java -jar app.jar --spring.profiles.active=postgres,cli crear --nombre='Conductor Prueba' --email='conductor@ejemplo.com' --telefono='+573001234568' --rol=CONDUCTOR --licencia=LIC001
```

El CLI muestra una sola vez la contraseña temporal. Los endpoints administrativos requieren la cabecera `X-DBA-Key`. No reutilices las credenciales ni la clave de ejemplo de Compose en producción.

## Pruebas

```bash
./mvnw test
```

Las pruebas utilizan `application-test.properties` con H2 y Hibernate `create-drop`. El esquema de pruebas crea las tablas JDBC auxiliares mediante `src/test/resources/import.sql`. Las migraciones PostgreSQL de Flyway no se modifican ni se ejecutan sobre H2. Para comprobar el comportamiento real en PostgreSQL, levanta Docker y prueba los endpoints y los flujos completos.
