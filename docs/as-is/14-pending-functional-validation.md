# AS-IS 14 - Pending Functional Validation

Ultima actualizacion: 2026-07-05

| ID | Area | Question | Evidence Found | Risk | Suggested Owner |
|---|---|---|---|---|---|
| PFV-001 | API Contract SRI | Cual contrato de rutas debe considerarse vigente: README (`/sri/factura/emitir`) o codigo (`/sri/emitir/factura`)? | README lines 171-176 y 295-310 difieren de `src/modules/sri/sri.controller.ts:84-218`. | Clientes o pruebas migradas contra rutas incorrectas. | Product Owner / Backend Lead / QA |
| PFV-002 | RIDE/PDF | Las afirmaciones de README sobre RIDE con PDF+QR y templates Word/Excel estan completamente implementadas y cuales endpoints son productivos? | README lines 90-93 y 331-347; servicios PDF/Document detectados, pero no analizados profundamente en lote 1. | Requisitos de salidas/documentos incompletos en SDD. | Product Owner / Backend Lead |
| PFV-003 | Operacion SRI Async | En ambientes reales `SRI_EMISION_ASYNC` queda siempre true o existen despliegues sincronicos? | `src/modules/sri/sri.service.ts:56-159` cambia comportamiento segun env; `.env.example:64-68` define async true. | Migracion puede alterar latencia/respuestas esperadas. | DevOps / Backend Lead |
| PFV-004 | Scheduling | Existe un scheduler externo para `POST /sri/sincronizar` o la sincronizacion es solo manual? | No se encontro `@Cron`/`ScheduleModule`; endpoint manual en `src/modules/sri/sri.controller.ts:522-591`. | Procesos operativos periodicos podrian omitirse. | Operations / Backend Lead |
| PFV-005 | Credenciales Iniciales | La semilla de superadmin sigue usandose en despliegues productivos y cual es la politica de rotacion/deshabilitacion? | README documenta credenciales iniciales; SQL inserta superadmin con hash redacted. | Riesgo de acceso indebido si no se rota/deshabilita. | Security / Operations |
| PFV-006 | Webhook Contract | Consumidores externos esperan exactamente headers/body actuales y politica de retries actual? | `src/modules/webhooks/webhook.processor.ts:37-58` define body/headers; `QueueModule` y `WebhooksService` definen attempts/backoff. | Cambios en migracion rompen integraciones externas. | Product Owner / Integration Owner |

