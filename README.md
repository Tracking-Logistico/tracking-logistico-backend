# Tracking Logístico — entrega incremental

Monolito Spring Boot con módulos `usuarios`, `pedidos`, `rutas`; PostgreSQL/Flyway en local/Render y H2 + Hibernate `create-drop` en tests.

## Docker (backend y frontend)

Descomprime ambos ZIP en la **misma carpeta padre**, manteniendo los nombres originales de carpeta:
`tracking-logistico-backend-main` y `tracking-logistico-frontend-main`.

```bash
cd tracking-logistico-backend-main
docker compose up --build
```

* Frontend: http://localhost:5173
* Backend: http://localhost:8080
* Swagger: http://localhost:8080/swagger-ui.html
* Bandeja de correos Mailpit: http://localhost:8025
* PostgreSQL: localhost:5432; DB `tracking_psep`, usuario `tracking_psep_user`, password definida en `docker-compose.yml`.

En Docker, el nombre de host entre contenedores es `postgres`, mientras que desde el computador es `localhost`.
Se mantienen los valores de credenciales originales solicitados en Compose; si ya hay volumen `postgres_data` creado con **otro usuario o password**, `docker compose up` no lo cambia. Para iniciar base nueva se puede borrar el volumen, **solo si no hay datos que conservar**: `docker compose down -v`.

## Crear usuarios internos por CLI (no existe endpoint público de creación)

```bash
docker compose exec backend java -jar app.jar --spring.profiles.active=postgres,cli crear --nombre='Operador Prueba' --email='operador@ejemplo.com' --telefono='+573001234567' --rol=OPERADOR --codigoEmpleado=OP001

docker compose exec backend java -jar app.jar --spring.profiles.active=postgres,cli crear --nombre='Conductor Prueba' --email='conductor@ejemplo.com' --telefono='+573001234568' --rol=CONDUCTOR --licencia=LIC001
```

El comando imprime **una sola vez** la contraseña temporal. Luego iniciar sesión en la web y cambiarla; el acceso operativo permanece restringido hasta hacerlo.

## Endpoints de administración DBA

`GET /api/v1/admin/usuarios` y `PATCH /api/v1/admin/usuarios/{id}/rol` requieren `X-DBA-Key` configurada en Compose como `dev-dba-key`. No se muestra ni almacena en el navegador. Solo se puede reasignar roles entre `OPERADOR` y `CONDUCTOR`, aportando `licencia` o `codigoEmpleado` en el body cuando corresponde.

## Tests locales

```bash
./mvnw test
```

Tests: perfil `test`, H2 en memoria, Hibernate `create-drop`, sin correos reales ni Flyway. PostgreSQL de aplicación utiliza Flyway + `ddl-auto=validate`.

## Render Free y Netlify

Se mantienen `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SPRING_MAIL_*`, `AUTH_PASSWORD_RESET_URL` y `CORS_ALLOWED_ORIGIN` del servicio actual. Agregar:

```dotenv
APP_VERIFICATION_URL=https://tracking-logistico.netlify.app/verificar
MAIL_ENABLED=true
MAIL_PROVIDER=resend
RESEND_API_KEY=<clave-nueva-de-resend>
MAIL_FROM=LogisTrack <no-reply@dominio-verificado>
DBA_API_KEY=<clave-exclusiva-desarrollo-dba>
```

**No usar SMTP Gmail para Render Free** (puertos bloqueados). Un password de aplicación Gmail NO es una clave válida para Gmail HTTP API. Resend HTTP requiere una cuenta, una API key y un remitente verificado; no puede enviar mensajes reales desde Render hasta configurarlos. SMTP de Mailpit se usa solo en Docker local. Establecer `VITE_API_URL` en Netlify al URL público del backend con `/api/v1`.

> Importante: los documentos legales enlazados son demostrativos y requieren aprobación para uso real. El módulo usuarios/auth es el primer bloque revisado. Las HU completas de pedidos y rutas requieren una ronda adicional de validación funcional e integración. No se afirma que pruebas de Docker/Maven se hayan ejecutado en este entorno.


## Bloque 2 — solicitudes, validación, tracking y correo tolerante a fallos

- Un **CLIENTE verificado** crea un pedido en `/api/v1/pedidos` usando su sesión: el backend fija `clienteId`, nombre y correo del remitente. No se aceptan IDs arbitrarios en el cuerpo.
- Se solicitan ciudad y teléfono del remitente y del destinatario, dirección y datos positivos con hasta 2 decimales. Código postal opcional donde no aplica. Los límites generales y de Express son configurables mediante `SHIPMENT_*`.
- La bandeja `/pedidos/pendientes` separa solicitudes sin validar de solicitudes observadas. El operador aprueba, rechaza con motivo o solicita corrección indicando campo. Se registra historial; corregir obliga a validar nuevamente.
- `/pedidos/{id}/activar-tracking` solo funciona tras aprobación y es idempotente, con bloqueo pesimista de la fila; crea guía `LT` de 12 caracteres. Generación PDF/QR no crea una guía nueva.
- La migración Flyway **V12** agrega datos de origen/destino/remitente y versión para concurrencia. No modifica scripts antiguos.

### Correo en Render Free / local

`MAIL_PROVIDER=resend` usa `https://api.resend.com` por **HTTPS/443**. La contraseña SMTP de Gmail existente NO se modifica y no habilita esa API; Resend necesita `RESEND_API_KEY` y `MAIL_FROM` de un dominio habilitado. Render Free bloquea SMTP saliente 25/465/587.

El adaptador registra `MAIL_SOLICITUD_ACEPTADA` o `MAIL_NO_ENTREGADO` y **no lanza errores del proveedor al proceso de negocio**: el cliente queda creado, los tokens se conservan y la solicitud de restablecimiento se registra. Si no se entrega verificación, la cuenta **permanece PENDIENTE_VERIFICACION** por la HU; no se omite esta protección. El cliente puede solicitar de nuevo el enlace con `POST /api/v1/clientes/verificacion/reenviar` (respuesta genérica y enfriamiento de un minuto por cuenta).

Local Docker usa Mailpit (`http://localhost:8025`) por SMTP interno. Para simular errores: configurar `MAIL_PROVIDER=resend` sin clave o detener Mailpit; revisar `docker compose logs -f backend`. `MAIL_LOG_ACTION_LINKS=true` permite visualizar ENLACES SECRETOS en logs **exclusivamente locales** para pruebas. En Render usar `MAIL_LOG_ACTION_LINKS=false`. Nunca escribir enlaces ni tokens en logs de producción.

Ejemplo de diagnóstico: `MAIL_NO_ENTREGADO evento=VERIFICACION proveedor=resend destinatario=a***@dominio.com motivo=Resend sin RESEND_API_KEY o MAIL_FROM ... resultado=PROCESO_CONTINUA`.

Al actualizar una instalación anterior, despliega el backend con Flyway V12 antes del frontend. Ejecutar desde el directorio backend: `docker compose up --build -d`, después `docker compose logs -f backend`, y `./mvnw test` con Java 17 o superior.

## Bloque 3 — conductores y planificación de rutas

Este ZIP contiene **todos los archivos de los bloques 1, 2 y 3**, no un parche. Se conserva el mismo Docker Compose, usuarios, contraseñas y conexión PostgreSQL/Render. **No ejecutes `docker compose down -v` al actualizar**: eliminaría la base local. Flyway aplica automáticamente la nueva `V13` al iniciar.

### Flujo operador → conductor

1. Con cuentas internas activadas por CLI, el operador prepara envíos en `RECIBIDO_EN_ORIGEN` o `EN_TRANSITO` desde el módulo de despachos.
2. En Rutas, selecciona **uno o varios envíos** y un conductor `ACTIVO`. Se muestran cantidad, peso y volumen asignados frente a sus límites. `POST /api/v1/rutas/asignaciones/lote` recibe `{ "conductorId": 5, "pedidoIds": [10, 11] }` y se ejecuta en **una transacción**. Se mantiene `POST /api/v1/rutas/asignaciones` para clientes API antiguos.
3. Se rechazan duplicados, estados incorrectos, conductor inactivo y exceso de cualquiera de las tres capacidades. La base conserva el índice único parcial de V10 para impedir dos paradas pendientes del mismo pedido; la asignación bloquea pesimistamente la fila del conductor para evitar sobrecarga concurrente.
4. Se muestra primero la prioridad ALTA, después MEDIA y BAJA, respetando una ordenación manual previa. `PUT /api/v1/rutas/{id}/orden` personaliza el orden, sin trasladar IDs de usuario en el cuerpo.
5. El operador puede reasignar una entrega con `PUT /api/v1/rutas/asignaciones/reasignar` (`pedidoId`, `nuevoConductorId`, `motivo`). La parada original se marca `CANCELADA`, la nueva queda `PENDIENTE`; el estado de pedido continúa `EN_REPARTO`. No se eliminan rutas ni entregas históricas.
6. El nuevo conductor ve un aviso interno y la entrega en `GET /api/v1/rutas/mi-ruta`. No necesita que funcione ningún correo saliente. Los avisos son persistentes: `GET /api/v1/rutas/mis-notificaciones` y `PATCH /api/v1/rutas/mis-notificaciones/{id}/leer`.

### Seguridad y auditoría

* `GET /api/v1/rutas/conductores/{usuarioId}` y las operaciones de asignación y ordenación son solo para `OPERADOR`. El conductor obtiene su ruta a partir de su sesión (`/mi-ruta`), no de un ID editable.
* El conductor solo puede obtener información detallada de pedidos con una parada **PENDIENTE** a su nombre: comprobación de pertenencia en backend incluso si manipula URLs `/pedidos/{id}` o `/pedidos/tracking/{codigo}`.
* `GET /api/v1/rutas/asignaciones/{pedidoId}/historial`, solo operador, consulta la nueva tabla `historial_asignaciones` (conductor anterior/nuevo, operador, motivo y fecha). La tabla `notificaciones_conductor` guarda avisos por destinatario sin exponerlos a otros usuarios.
* `OPERATIONS_TIME_ZONE=America/Bogota` define la fecha de la jornada independientemente de la zona horaria del servidor.

### Pruebas recomendadas tras ejecutar Docker

```bash
# Desde la carpeta del backend, con ambas carpetas de proyecto al mismo nivel.
docker compose up --build -d
docker compose logs -f backend
./mvnw test
```

Prueba también el caso negativo: elige un envío ya asignado, intenta sobrepasar peso o cantidad, reasigna a otro conductor y comprueba que desaparezca de la ruta anterior y que se cree un aviso en la nueva. Los tests usan H2 y Hibernate `create-drop`; `src/test/resources/import.sql` aporta las tablas auxiliares JDBC de auditoría.

**Limitación de la entrega:** en este contenedor no está Maven instalado y Maven Wrapper no puede descargar su distribución. `npm ci --offline` tampoco puede resolver paquetes ausentes. Se validó sintaxis TS/TSX con el parser de TypeScript, pero no se verificaron compilación completa, cobertura ni prueba de integración Docker. Revisa los logs del comando anterior antes de desplegar en Render/Netlify.

## Bloque 4 — integración de sesiones y despacho (incremental)

Se conserva la estructura modular y **las mismas credenciales locales del Compose**. No hay migración nueva: se reutilizan V1–V13. La entrega de correo continúa siendo best-effort (Mailpit local, Resend HTTPS en Render Free): un fallo se registra y no aborta el caso de uso; la cuenta sin correo confirmado permanece pendiente como exige la HU.

### Contratos alineados

- `GET /api/v1/pedidos/despachos` (OPERADOR): incluye solicitudes **aprobadas** todavía sin guía y pedidos en `CREADO`, `RECIBIDO_EN_ORIGEN` o `EN_TRANSITO`. Excluye solicitudes sin validar y envíos en reparto o entregados. Se conserva `/activables` para no romper clientes previos.
- Pantalla de despachos: permite `CREADO → RECIBIDO_EN_ORIGEN → EN_TRANSITO`; no pierde el envío al actualizar su estado. El sistema de rutas sigue aceptando tanto `RECIBIDO_EN_ORIGEN` como `EN_TRANSITO`.
- El frontend renueva sesiones con `POST /api/v1/auth/refresh` ante 401, con una sola renovación simultánea y un solo reintento. Si falla o la cuenta está desactivada, limpia la sesión sin quedarse con tokens obsoletos. El backend **sigue** siendo la autoridad para expiración, roles e inactividad.
- El frontend cierra la UI por inactividad (30 minutos Cliente / 15 minutos Operador y Conductor) y respeta el destino de rol `/panel/cliente`, `/panel/operador`, `/panel/conductor`. Se preserva la obligación de cambio de contraseña al primer inicio.
- Las colisiones de actualización o bloqueo de filas devuelven HTTP 409 con una indicación de reintento, sin mostrar detalles internos.

### Ejecución y comprobaciones

```bash
# A partir de ZIPs completos del mismo bloque descomprimidos juntos:
cd tracking-logistico-backend-main
docker compose up --build -d
docker compose logs -f backend
# Desde otra terminal:
./mvnw test
# Frontend (dependencias disponibles):
cd ../tracking-logistico-frontend-main
npm ci && npm run build && npm run lint
```

Pruebas manuales de integración recomendadas:

1. Crear Cliente, abrir mensaje en Mailpit :8025, verificar cuenta e iniciar sesión; comprobar que no se puede entrar a pedidos antes de verificar.
2. Con Cliente verificado crear solicitud, comprobar bandeja, pedir corrección, corregir y aprobar con prioridad; comprobar historial.
3. Con Operador activar tracking (repetir la activación y observar que no cambia), generar PDF y QR, avanzar a recibido en origen y después tránsito. Comprobar que el envío persiste en la pantalla de despachos durante los cambios y aparece en rutas si no tiene parada activa.
4. Asignar varios pedidos respetando peso, volumen y número de entregas; reasignar y revisar historia y aviso al nuevo Conductor; intentar consultar otro envío desde su sesión y esperar 403.
5. En local configurar temporalmente `MAIL_PROVIDER=resend` y dejar vacías `RESEND_API_KEY`/`MAIL_FROM`; el registro debe conservarse pendiente, y los logs deben mostrar `MAIL_NO_ENTREGADO` sin tokens de producción.
6. Desactivar usuario y comprobar que su sesión deja de operar; solicitar recuperación de clave y comprobar que invalida sesiones previas. Dejar la pestaña inactiva 15/30 minutos según rol y observar el cierre local.

**Límite de esta entrega**: el entorno donde se generaron los ZIP carece de Maven, Docker y dependencias npm descargables. Se revisó el contrato cruzado y la sintaxis de TS/TSX, pero no se han ejecutado `./mvnw test`, `npm run build` ni los escenarios reales de PostgreSQL/Render. No interpretar esta revisión estática como certificación de despliegue.
