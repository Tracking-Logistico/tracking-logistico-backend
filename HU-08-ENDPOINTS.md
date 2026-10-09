# HU-08 - Endpoints del conductor

Todos los endpoints requieren una sesión Bearer válida y el rol `CONDUCTOR`.

## 1. Progreso de la jornada

### `GET /api/v1/conductor/panel/progreso`

Request: no body.

Response `200`:

```json
{
  "fecha": "2026-10-09",
  "totalEntregas": 3,
  "entregadas": 1,
  "pendientes": 1,
  "fallidas": 1,
  "canceladas": 0
}
```

Errores:

- `401`: `{"code":"SESION_REQUERIDA","error":"Se requiere una sesión válida"}`
- `403`: `{"code":"ROL_INSUFICIENTE","error":"No tienes permiso para realizar esta acción"}`

## 2. Entregas asignadas

### `GET /api/v1/conductor/panel/entregas`

Request: no body.

Response `200`:

```json
[
  {
    "idParada": 41,
    "pedidoId": 1001,
    "orden": 1,
    "numeroPedido": "PED-1001",
    "numeroTracking": "TRK-1001",
    "direccionDestino": "Calle 10 # 20-30",
    "ciudadDestino": "Bogotá",
    "destinatarioNombre": "Ana Pérez",
    "destinatarioTelefono": "+573001112233",
    "prioridad": "ALTA",
    "pesoKg": 2.5,
    "estadoParada": "PENDIENTE",
    "estadoPedido": "EN_REPARTO"
  }
]
```

Devuelve las paradas no canceladas de la ruta del día del conductor autenticado. Puede incluir una entrega ya completada para distinguirla del pendiente mediante `estadoParada` y `estadoPedido`.

Errores:

- `401`: sesión ausente o inválida.
- `403`: rol diferente de conductor.

## 3. Detalle de una entrega

### `GET /api/v1/conductor/panel/entregas/{pedidoId}`

Request: no body.

Response `200`:

```json
{
  "idParada": 41,
  "pedidoId": 1001,
  "orden": 1,
  "numeroPedido": "PED-1001",
  "numeroTracking": "TRK-1001",
  "direccionDestino": "Calle 10 # 20-30",
  "ciudadDestino": "Bogotá",
  "codigoPostalDestino": "110111",
  "destinatarioNombre": "Ana Pérez",
  "destinatarioTelefono": "+573001112233",
  "indicacionesAcceso": "Portería principal",
  "descripcionPaquete": "Caja mediana",
  "pesoKg": 2.5,
  "largoCm": 30.0,
  "anchoCm": 20.0,
  "altoCm": 15.0,
  "prioridad": "ALTA",
  "estadoParada": "PENDIENTE",
  "estadoPedido": "EN_REPARTO",
  "observacionesValidacion": null,
  "fechaEntregaReprogramada": null,
  "ultimosEventos": []
}
```

Errores:

- `401`: sesión ausente o inválida.
- `403`: el pedido no está asignado al conductor.
- `404`: pedido no encontrado.

## 4. Siguiente parada

### `GET /api/v1/conductor/panel/siguiente`

Request: no body.

Response `200`:

```json
{
  "idParada": 41,
  "pedidoId": 1001,
  "orden": 1,
  "numeroTracking": "TRK-1001",
  "direccionDestino": "Calle 10 # 20-30",
  "ciudadDestino": "Bogotá",
  "destinatarioNombre": "Ana Pérez",
  "destinatarioTelefono": "+573001112233",
  "indicacionesAcceso": "Portería principal",
  "pesoKg": 2.5
}
```

Si no hay paradas pendientes responde `404` sin body.

Errores adicionales:

- `401` y `403` por sesión o rol.

## 5. Ruta calculada

### `GET /api/v1/conductor/panel/ruta`

Request: no body.

Response `200`:

```json
{
  "paradas": [
    {
      "paradaId": 41,
      "pedidoId": 1001,
      "orden": 1,
      "direccion": "Calle 10 # 20-30",
      "ciudad": "Bogotá",
      "estado": "PENDIENTE",
      "sinUbicacion": false
    },
    {
      "paradaId": 42,
      "pedidoId": 1002,
      "orden": 2,
      "direccion": "Carrera 50 # 80-10",
      "ciudad": "Bogotá",
      "estado": "PENDIENTE",
      "sinUbicacion": true
    }
  ],
  "siguiente": {
    "paradaId": 41,
    "pedidoId": 1001,
    "orden": 1,
    "direccion": "Calle 10 # 20-30",
    "ciudad": "Bogotá",
    "estado": "PENDIENTE",
    "sinUbicacion": false
  }
}
```

Las paradas con coordenadas se ordenan desde la primera parada usando el grafo de distancias Haversine y Dijkstra. Las que no tienen coordenadas van al final y se marcan con `sinUbicacion: true`.

Errores:

- `401`: sesión ausente o inválida.
- `403`: rol diferente de conductor.

## 6. Catálogo de novedades

### `GET /api/v1/conductor/panel/catalogo-novedades`

Request: no body.

Response `200`:

```json
[
  {
    "codigo": "CLIENTE_AUSENTE",
    "resultado": "ENTREGA_FALLIDA",
    "descripcion": "Cliente ausente"
  },
  {
    "codigo": "PAQUETE_RECHAZADO",
    "resultado": "DEVOLUCION_AL_REMITENTE",
    "descripcion": "Paquete rechazado por el cliente"
  }
]
```

Errores:

- `401`: sesión ausente o inválida.
- `403`: rol diferente de conductor.

## 7. Registrar resultado

### `POST /api/v1/conductor/panel/entregas/{pedidoId}/resultado`

Request:

```json
{
  "resultado": "ENTREGA_FALLIDA",
  "codigoNovedad": "CLIENTE_AUSENTE",
  "motivo": "No había nadie para recibir el paquete",
  "latitud": null,
  "longitud": null,
  "fechaEvento": "2026-10-09T12:30:00-05:00",
  "idEventoCliente": "550e8400-e29b-41d4-a716-446655440000"
}
```

Response `201`:

```json
{
  "idEventoCliente": "550e8400-e29b-41d4-a716-446655440000",
  "pedidoId": 1001,
  "resultado": "ENTREGA_FALLIDA",
  "estado": "APLICADO",
  "fechaEvento": "2026-10-09T12:30:00",
  "duplicado": false
}
```

Un reenvío idempotente responde `200` con `duplicado: true`.

Errores:

- `400`: JSON inválido, resultado inválido, motivo ausente/no válido, coordenadas inválidas o `fechaEvento` futura más de 5 minutos o con más de 24 horas de antigüedad.
- `401`: sesión ausente o inválida.
- `403`: pedido no asignado al conductor.
- `404`: pedido no encontrado.
- `409`: pedido no está en `EN_REPARTO` o conflicto concurrente.

## 8. Sincronización offline

### `POST /api/v1/conductor/panel/entregas/sincronizacion`

Request:

```json
{
  "eventos": [
    {
      "pedidoId": 1001,
      "evento": {
        "resultado": "ENTREGADO",
        "codigoNovedad": null,
        "motivo": null,
        "latitud": 4.6097,
        "longitud": -74.0817,
        "fechaEvento": "2026-10-09T12:20:00-05:00",
        "idEventoCliente": "550e8400-e29b-41d4-a716-446655440001"
      }
    },
    {
      "pedidoId": 1002,
      "evento": {
        "resultado": "ENTREGA_FALLIDA",
        "codigoNovedad": "CLIENTE_AUSENTE",
        "motivo": "Cliente ausente",
        "latitud": null,
        "longitud": null,
        "fechaEvento": "2026-10-09T12:25:00-05:00",
        "idEventoCliente": "550e8400-e29b-41d4-a716-446655440002"
      }
    }
  ]
}
```

Response `200`:

```json
{
  "resultados": [
    {
      "idEventoCliente": "550e8400-e29b-41d4-a716-446655440001",
      "estado": "APLICADO",
      "motivoRechazo": null
    },
    {
      "idEventoCliente": "550e8400-e29b-41d4-a716-446655440002",
      "estado": "RECHAZADO",
      "motivoRechazo": "El envío no está asignado al conductor autenticado"
    }
  ]
}
```

Los estados por evento son únicamente `APLICADO`, `DUPLICADO` o `RECHAZADO`. Los eventos se procesan por `fechaEvento`, y un evento rechazado no revierte los demás.

Aunque el frontend controla el límite de 100 eventos, el backend rechaza lotes de más de 500 eventos con `400`.

Errores:

- `400`: lote vacío, lote mayor de 500, campos inválidos o cualquier `fechaEvento` futura más de 5 minutos o con más de 24 horas de antigüedad.
- `401`: sesión ausente o inválida.
- `403`: rol diferente de conductor.

