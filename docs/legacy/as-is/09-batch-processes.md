# 09 - Procesos batch y asíncronos AS-IS

## Estado del análisis

Los dos primeros lotes documentan dos colas BullMQ activadas por jobs en Redis
y un proceso manual por lotes iniciado mediante una petición HTTP. El segundo
lote precisa la política incierta de retry y la reejecución completa de Factura
en `sri-emision`.

## BAT-001 - Emisión asíncrona SRI

| Campo | Comportamiento observado |
|---|---|
| Trigger | Llamada a uno de los métodos de emisión; se usa cola salvo que `SRI_EMISION_ASYNC` sea exactamente `false`. |
| Schedule | No aplica al flujo observado; trigger event-driven. |
| Cola / job | Cola `sri-emision`, job `emision`. |
| Entradas | `{ tipo, dto }`, con tipos `FACTURA`, `NOTA_CREDITO`, `NOTA_DEBITO`, `RETENCION`, `GUIA_REMISION`. |
| Salida inmediata | `jobId`, mensaje y estado `EN_COLA`. |
| Procesador | Delega al servicio específico según `tipo`. |
| Dependencias | Redis/BullMQ, PostgreSQL, filesystem de XML/certificados y SRI SOAP. |
| Fallo | El processor registra y relanza. La aplicación efectiva de intentos y backoff depende de qué registro de la cola prevalezca en runtime. |

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: métodos `emitir*`; `SriEmisionProcessor.process`
- Line / Section: sri.service.ts 56-159; sri-emision.processor.ts 10-45
- Condition / Query / Statement: `emisionQueue.add('emision', { tipo, dto })` y `switch (tipo)`
- Confidence: High

`QueueModule` registra `sri-emision` con intentos, backoff exponencial y
retención parametrizables; `SriModule` vuelve a registrar la misma cola sin
opciones. El valor por defecto configurado es tres intentos, pero la
configuración efectiva de la cola no puede confirmarse estáticamente debido al
doble registro. Esta incertidumbre se mantiene en `PFV-031` y `RISK-018`.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`; `src/config/configuration.ts`
- Function / Method / Procedure: registros de `sri-emision`; configuración de colas
- Line / Section: queue.module.ts 29-49; sri.module.ts 27-32; configuration.ts 138-145
- Condition / Query / Statement: registro global con `defaultJobOptions` frente a registro local `BullModule.registerQueue({ name: 'sri-emision' })` sin opciones
- Confidence: High

El processor relanza cualquier error del servicio. Si la configuración
efectiva habilita más de un intento, BullMQ vuelve a ejecutar el job completo;
para factura esto vuelve a invocar un flujo que reserva el secuencial antes de
firmar, llamar al SRI y persistir. No se observó en el processor una clave de
idempotencia ni una reanudación desde la fase fallida. La semántica de reuso o
duplicación queda en `PFV-032` y `RISK-019`; esos elementos tambien cubren la
repeticion de la solicitud HTTP, que crea otra ejecucion sin identidad comun.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; `FacturaService.emitirFactura`
- Line / Section: SriService 56-67; processor 24-46; factura 42-230
- Condition / Query / Statement: `queue.add` no fija identidad; el processor relanza y factura vuelve a ejecutar reserva, firma, SRI y persistencia.
- Confidence: Medium

El dato del job contiene el DTO completo de Factura. Los defaults globales
conservan por conteo jobs completados y fallidos, y Redis de produccion usa AOF
con volumen persistente. La politica de acceso y retencion se mantiene en
`PFV-038` y `RISK-029`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; registro de `sri-emision`; configuracion de colas; servicio Redis
- Line / Section: SriService 56-67; QueueModule 29-49; configuracion 138-145; compose 21-40, 102-104
- Condition / Query / Statement: el job recibe `{ tipo, dto }`, los defaults retienen por conteo y Redis conserva AOF en volumen.
- Confidence: High

No se encontró en el módulo SRI una operación que recupere el job por el
`jobId` devuelto. El mecanismo de cierre/correlación para el cliente permanece
en `PFV-005`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: API de emisión y controladores SRI revisados
- Line / Section: sri.service.ts 56-159; sri.controller.ts 62-592
- Condition / Query / Statement: se devuelve `jobId`; no se localizó `getJob` ni un endpoint de resultado en el módulo
- Confidence: Medium

## BAT-002 - Despacho asíncrono de webhooks

| Campo | Comportamiento observado |
|---|---|
| Trigger | Eventos internos `comprobante.autorizado` o `comprobante.rechazado`. |
| Schedule | No aplica al flujo observado; trigger event-driven. |
| Cola / job | Cola `webhook-dispatch`; nombre `webhook-{evento}`. |
| Entradas | ID de configuración, URL, secreto, evento y payload. |
| Fan-out | Un job por configuración activa suscrita al evento y compatible con el emisor. |
| Salida | POST JSON firmado y fila de intento en `webhook_logs`. |
| Dependencias | PostgreSQL, Redis/BullMQ y endpoint HTTP del suscriptor. |
| Fallo | Timeout de 30 segundos o HTTP no exitoso; el error se relanza para reintento. |

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`; `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: listeners `@OnEvent`, `emit`, `WebhookProcessor.process`
- Line / Section: webhooks.service.ts 33-55, 271-320; webhook.processor.ts 20-108
- Condition / Query / Statement: consulta de suscriptores, `webhookQueue.add(...)` y POST con HMAC
- Confidence: High

Los defaults de la cola definen cinco intentos y backoff exponencial; cada job
puede reemplazar los intentos con `reintentos_max` de la configuración y fija
un delay de 3000 ms.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: registro de `webhook-dispatch`; `emit`
- Line / Section: queue.module.ts 51-70; webhooks.service.ts 299-319
- Condition / Query / Statement: opciones por defecto y opciones pasadas por job
- Confidence: High

El catálogo acepta siete eventos, pero este lote solo encontró listeners para
dos. La cobertura del fan-out permanece en `PFV-004`.

**Evidence**
- File: `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `WEBHOOK_EVENTS`; listeners de eventos
- Line / Section: webhook.dto.ts 14-24; webhooks.service.ts 33-55
- Condition / Query / Statement: siete eventos configurables y dos listeners localizados
- Confidence: High

## BAT-003 - Sincronización manual con SRI

| Campo | Comportamiento observado |
|---|---|
| Trigger | `POST /sri/sincronizar` autenticado. |
| Schedule | No aplica al flujo observado; trigger HTTP manual. |
| Entradas | Estados, indicador de reintento, límite y RUC presentado al controlador. |
| Alcance por RUC | El controlador valida el RUC para usuarios no `SUPERADMIN`, pero el servicio no lo incluye en los filtros del lote. |
| Tamaño de lote | 50 registros por iteración. |
| Límite | Mínimo entre límite solicitado, 200 por defecto y `SRI_SYNC_MAX_LIMIT` con default 500. |
| Proceso | Lista comprobantes, consulta autorización SRI, actualiza estado/XML y opcionalmente reintenta. |
| Salida | Conteos de procesados, actualizados, reintentados, errores y detalle por clave. |
| Fallo | Error aislado por comprobante; incrementa contador y continúa. |

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.sincronizar`; `SriService.sincronizarConSri`
- Line / Section: sri.controller.ts 522-591; sri.service.ts 670-897
- Condition / Query / Statement: endpoint manual, bucle por lotes de 50, consulta SOAP, actualización y resumen
- Confidence: High

El controlador recibe y valida `rucEmisor`, pero `sincronizarConSri` no declara
ni lee esa propiedad y llama a `listarComprobantes` solo con estados, límite y
offset. El lote ejecutado no queda filtrado por el RUC validado.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.sincronizar`; `SriService.sincronizarConSri`
- Line / Section: sri.controller.ts 561-591; sri.service.ts 677-698, 718-726
- Condition / Query / Statement: validación de `body.rucEmisor` seguida de una consulta que no pasa `rucEmisor`
- Confidence: High

El proceso incrementa `offset`, y `listarComprobantes` lo convierte en `page`.
El repositorio acepta `page`, pero su SQL usa `LIMIT` sin `OFFSET`; por tanto, el
avance posicional solicitado no se aplica en la consulta física.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/services/sri-repository.service.ts`
- Function / Method / Procedure: `listarComprobantes`, `sincronizarConSri`, `findComprobantes`
- Line / Section: sri.service.ts 268-297, 718-726, 882-884; sri-repository.service.ts 439-452, 504-570
- Condition / Query / Statement: cálculo/incremento de página y offset frente a SQL con `LIMIT` y sin `OFFSET`
- Confidence: High

Cuando una autorización se confirma, el proceso actualiza PostgreSQL, guarda el
XML autorizado y emite el evento interno que puede iniciar `BAT-002`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `sincronizarConSri`
- Line / Section: 758-828
- Condition / Query / Statement: `updateComprobante`, `xmlStorage.saveXml`, `repository.saveXml`, `eventEmitter.emit`
- Confidence: High

## Procesos no confirmados

Los servicios de emisión pueden emitir `comprobante.persistencia_fallida`, pero
no se localizó un listener en el repositorio revisado. No se documenta como
batch confirmado; su responsabilidad operativa permanece en `PFV-006`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`
- Function / Method / Procedure: rutas de error de persistencia
- Line / Section: factura.service.ts 511-519; nota-credito.service.ts 326-336; nota-debito.service.ts 309-319; retencion.service.ts 279-289; guia-remision.service.ts 317-327
- Condition / Query / Statement: emisión de `comprobante.persistencia_fallida`; no se localizó `@OnEvent` correspondiente
- Confidence: Medium

## Dependencia de durabilidad

Las dos colas comparten la instancia Redis declarada por Compose, con AOF y un
volumen `redis_data`. En producción la política de memoria permite eviction;
la validez operacional para BullMQ permanece en `PFV-016`.

**Evidence**
- File: `docker-compose.prod.yml`; `src/common/queues/queue.module.ts`
- Function / Method / Procedure: servicio Redis y conexión BullMQ
- Line / Section: docker-compose.prod.yml 21-40, 102-104; queue.module.ts 15-70
- Condition / Query / Statement: `--appendonly yes`, volumen `redis_data`, `allkeys-lru` y dos colas BullMQ
- Confidence: High
