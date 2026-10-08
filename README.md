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

## Pedidos asociados a una cuenta

`GET /api/v1/pedidos/mios` requiere una sesión de cliente y devuelve una página de hasta 20 pedidos por defecto. Admite `page`, `size` (máximo 50), `sort=fechaCreacion,desc` o `sort=estado,asc`, además de los filtros opcionales `estado`, `fechaDesde=YYYY-MM-DD` y `fechaHasta=YYYY-MM-DD` (fechas de creación, ambas inclusive). La respuesta incluye número de pedido, tracking, remitente, destinatario, estado y fechas estimada y de creación. Un resultado vacío se representa con una página vacía para que el cliente muestre su estado vacío.

Los pedidos se asocian al remitente autenticado al crearlos. Para que un destinatario con cuenta también los vea, se puede enviar `destinatarioEmail` al crear el pedido; el destinatario solo verá el pedido después de que el operador lo pase a `CREADO`. La fecha estimada se calcula al crear o corregir el pedido: 1 día calendario para `EXPRESS` y 3 para `ESTANDAR` y `PROGRAMADO`. `GET /api/v1/pedidos/mios/tracking/{numeroTracking}` devuelve para un envío de la cuenta el estado actual y sus movimientos logísticos, sin exponer datos de contacto o dirección del otro participante. El remitente también conserva los endpoints existentes `GET /api/v1/pedidos/tracking/{numeroTracking}` y `GET /api/v1/pedidos/{id}/historial`.

## Checkpoints logísticos (HU-06) e incidencias (HU-07)

Conductor (`CONDUCTOR`):

- `POST /api/v1/pedidos/{id}/checkpoints`: registra el escaneo del QR de la etiqueta (`numeroTracking|checksum`) con etapa, coordenadas, precisión y `fechaDispositivo`. `idEventoCliente` (UUID generado por el dispositivo) hace idempotente el reenvío: 201 al crear, 200 si ya existía.
- `POST /api/v1/pedidos/checkpoints/sincronizacion`: lote (máx. 100) de eventos capturados sin conexión; se procesan en orden de `fechaDispositivo` y los que chocan con el estado actual quedan `PENDIENTE_REVISION`. La cola local y el contador de pendientes viven en el frontend.
- `GET /api/v1/pedidos/checkpoints/mios/pendientes-revision`.

Operador (`OPERADOR`): `GET /api/v1/pedidos/novedades`, `GET /api/v1/pedidos/checkpoints/pendientes-revision`, `GET /api/v1/pedidos/incidencias/tipos`, `POST|GET /api/v1/pedidos/{id}/incidencias` (enviar `versionEsperada` para detectar cambios concurrentes).

Cliente (`CLIENTE`): `GET /api/v1/pedidos/mios/tracking/{t}` incluye la novedad en lenguaje para el cliente; `GET .../reprogramacion/rango`, `POST .../reprogramacion` y `PUT .../direccion`.

Parámetros: `app.checkpoints.precision-maxima-metros` (100), `app.shipment.retencion-bodega-dias` (7), `app.incidencias.max-intentos-entrega` (3), `app.incidencias.plazo-verificacion-direccion-dias-habiles` (3, lunes a viernes, sin festivos) y `app.incidencias.escalamiento-cron`.

### Eventos para notificaciones (RabbitMQ)

Los módulos publican eventos de dominio de Spring; un único adaptador (`config/messaging`) los reenvía, después del commit, al exchange topic `logistica.eventos` con `message-id = eventId` (los consumidores deben descartar duplicados por ese id). Routing keys: `pedido.checkpoint.registrado`, `pedido.checkpoint.pendiente-revision`, `pedido.incidencia.registrada`, `pedido.direccion.verificacion-solicitada`, `pedido.entrega.reprogramada`, `pedido.devolucion.iniciada`. Las colas las declara el módulo de notificaciones. RabbitMQ se activa con `RABBITMQ_ENABLED=true` (perfil `postgres`, por defecto) y está desactivado en H2 y pruebas, donde los eventos solo se registran en el log. Docker Compose incluye RabbitMQ con consola en http://localhost:15672.
