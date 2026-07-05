# AS-IS 09 - Batch Processes

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. No se ejecuto ningun job. No se encontro scheduler NestJS/cron en busqueda inicial; los procesos identificados son workers BullMQ y endpoint manual.

| ID | Name | Trigger / Schedule | Script / Program | Inputs | Outputs | Dependencies | Failure Handling | Evidence | Confidence |
|---|---|---|---|---|---|---|---|---|---|
| BAT-001 | Emision asincrona SRI | BullMQ job en cola `sri-emision`; schedule not found | `SriEmisionProcessor` | `{ tipo, dto }` | resultado servicio por tipo o error | Redis/BullMQ, servicios SRI | logs error y rethrow; attempts/backoff de cola | `src/modules/sri/processors/sri-emision.processor.ts:10-47`, `src/common/queues/queue.module.ts:29-49` | High |
| BAT-002 | Dispatch asincrono webhooks | BullMQ job en cola `webhook-dispatch`; schedule not found | `WebhookProcessor` | `WebhookJobData` | POST externo + `webhook_logs` | Redis/BullMQ, HTTP externo, DB | log intento; no-OK/error relanza para retry | `src/modules/webhooks/webhook.processor.ts:20-144`, `src/common/queues/queue.module.ts:51-71` | High |
| BAT-003 | Sincronizacion con SRI | Manual HTTP `POST /sri/sincronizar`; schedule not found | `SriController.sincronizar` / `SriService.sincronizarConSri` | estados, reintentar, limite, rucEmisor | conteo procesados/actualizados/reintentados/errores/detalle | SRI SOAP, DB, XML storage | cuenta errores por comprobante; sigue lote | `src/modules/sri/sri.controller.ts:522-591`, `src/modules/sri/sri.service.ts:670-897` | High |

## Hallazgo Sobre Scheduling

BAT-FIND-001: En la busqueda inicial no se encontro `@Cron` ni `ScheduleModule`; por evidencia local, la sincronizacion SRI expuesta en este lote es manual via HTTP y las tareas asincronas se activan por colas.

**Evidence**
- File: `src/`, `package.json`, `docker-compose*.yml`
- Function / Method / Procedure: `rg -n "@Cron|ScheduleModule|cron|scheduler|schedule"`
- Line / Section: command output
- Condition / Query / Statement: no aparecen decoradores/scheduler; aparecen referencias a sincronizacion manual y colas.
- Confidence: Medium

