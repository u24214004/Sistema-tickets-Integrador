# API REST · v0.1

Base: http://localhost:8080/api. JSON UTF-8. Endpoints protegidos: `Authorization: Bearer <token>`. Los errores tienen `message`; los errores de validación también pueden incluir `fields`.

| Método | Ruta | Acceso | Resultado |
| --- | --- | --- | --- |
| GET | /health | Público | Estado del servicio |
| POST | /auth/login | Público | JWT, expiresAt y user |
| GET | /auth/me | Autenticado | Perfil actual sin hash |
| GET | /companies | Interno | Catálogo para creación |
| GET | /tickets | Autenticado | Página dentro del alcance del usuario |
| GET | /tickets/summary | Autenticado | Indicadores de todos los tickets accesibles |
| GET | /tickets/{id} | Autenticado | Detalle o 404 |
| POST | /tickets | Autenticado | Ticket creado; HTTP 201 |

## Login

```json
{"email":"cliente@demo.local","password":"Demo2026!"}
```

## Creación

```json
{"title":"Error al contabilizar","description":"No se registra el asiento en SAP.","severity":"ALTA"}
```

El cliente omite companyId: el servidor usa su empresa. El personal interno debe enviar companyId con una empresa existente. Un cliente que envía otro companyId recibe 403.

Asunto obligatorio hasta 150 caracteres; descripción obligatoria hasta 4000. Severidades CRITICA, ALTA, MEDIA y BAJA. El estado inicial siempre es ABIERTO; no puede enviarse otro estado. Las propiedades desconocidas se rechazan.

## Listado

Parámetros: page (desde 0), size (1 a 50; predeterminado 20), status y severity opcionales. Estados: ABIERTO, EN_PROCESO, EN_ESPERA, RESUELTO.

Respuesta: content, totalElements, totalPages, number. Orden de creación descendente con ID como desempate. Los filtros se aplican después del alcance por empresa, nunca lo sustituyen. Un cliente recibe 404 si pide un ticket de otra empresa, evitando revelar su existencia.

## Resumen

Respuesta: total, open, inProgress, resolved. Se calcula para el conjunto accesible al usuario, independientemente de la página y de los filtros activos en la bandeja.

## Códigos de error

400: datos o parámetros inválidos. 401: credenciales o token inválidos. 403: operación fuera de los permisos. 404: ticket no disponible. 500: fallo interno con mensaje genérico.
