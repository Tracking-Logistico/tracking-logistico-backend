# Tracking Logístico · Backend

Backend Java 17 / Spring Boot 4.1.1 / PostgreSQL, preparado para desplegarse como Docker Web Service en Render. No requiere cambiar las rutas de la API utilizadas por el frontend.

## Configuración incluida en Dockerfile y Compose

El Dockerfile fija los valores de ejemplo que ya existían: `SPRING_PROFILE=postgres`, `DB_PORT=5432`, `DB_NAME=logistica_db`, `DB_USER=logistica_user`, `MAIL_PROVIDER=resend`, `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587` y `MAIL_USERNAME=logistrack01@gmail.com`. Las variables del servicio de Render pueden sobrescribirlos. El Compose fija los datos de desarrollo de PostgreSQL (`logistica_db`, `logistica_user`, `logistica_pass`) y configura Gmail SMTP únicamente para uso local, pasando `MAIL_PASSWORD` desde el entorno del equipo.

**No se pueden inferir de los ejemplos** el host real de PostgreSQL en Render ni una clave API de Resend verificada. Configura `DB_HOST`, `DB_PASSWORD`, `RESEND_API_KEY` y `MAIL_FROM` para el proveedor Resend en el entorno real. No se vuelve a incluir la contraseña Gmail previamente expuesta. En Render Free Gmail SMTP por el puerto 587 no funcionará aunque tenga contraseña.

## Render: variables necesarias

En **Environment** del servicio Docker configura:

| Variable | Valor esperado |
| --- | --- |
| `SPRING_PROFILE` | `postgres` (también es el valor por defecto) |
| `DB_HOST` | Host interno o externo de tu PostgreSQL (NO `localhost` si está en otro servicio) |
| `DB_PORT` | `5432` u otro puerto de tu instancia |
| `DB_NAME` | Nombre de tu base de datos |
| `DB_USER` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña real; **no se incluye en el código** |
| `DB_JDBC_URL` | Opcional. JDBC completo `jdbc:postgresql://host:5432/base`; reemplaza host/puerto/nombre. No uses aquí la URI `postgresql://` de Render sin adaptarla. |
| `CORS_ALLOWED_ORIGIN` | Origen real de tu frontend sin barra final, ej. `https://mi-front.com` |
| `APP_VERIFICATION_URL` | URL pública de `/api/v1/clientes/verificar` (se conserva el dominio original si no se define) |
| `AUTH_PASSWORD_RESET_URL` | URL pública del formulario frontend de restablecimiento |
| `MAIL_PROVIDER` | `resend` para Render Free (HTTPS 443); `smtp` solo si tu servicio puede acceder al servidor SMTP |
| `RESEND_API_KEY` | API key de Resend **si eliges** `resend` |
| `MAIL_FROM` | Remitente **verificado** por Resend o tu proveedor SMTP |

No se deben configurar secretos en el repositorio. `spring.jpa.hibernate.ddl-auto=validate` y Flyway están activos con PostgreSQL: el backend **no borra ni recrea la base**. La migración `V10` exige unicidad de rutas por conductor/fecha y de envíos pendientes: si hay duplicados históricos, hay que conciliarlos antes de desplegar; no se eliminan datos automáticamente.

En **Render Free**, los puertos de salida SMTP `25`, `465` y `587` están bloqueados. La integración `resend` usa HTTPS. Si faltan la clave o el remitente, registro y recuperación responden `503` en vez de dejar operaciones inconsistentes o de disfrazarlas como `403`.

Puedes habilitar Swagger temporalmente con `SWAGGER_ENABLED=true`. Se desactiva por defecto en producción. Para desarrollo H2 temporal, establece expresamente `SPRING_PROFILE=h2` (no lo uses en Render).

## Compose local

Desde esta carpeta: `docker compose up --build -d`. Se levantan tanto PostgreSQL con volumen persistente como el backend y se conectan usando `DB_HOST=postgres`. Si necesitas que los correos se envíen en local por SMTP, define `MAIL_PASSWORD` en un `.env` local ignorado por Git. En producción Render usa el Dockerfile, **no** este Compose.

El Dockerfile empaqueta la aplicación sin ejecutar ni compilar pruebas, según lo solicitado.
