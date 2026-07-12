# 08 - Integraciones AS-IS

## Estado del análisis

Los dos primeros lotes acotados cubren integraciones confirmadas por
configuración y llamadas de código. El segundo lote amplía el comportamiento
SOAP y el límite PostgreSQL/filesystem observados en la emisión de Factura.

| ID | Sistema o dependencia | Dirección | Mecanismo | Datos intercambiados | Disparador |
|---|---|---|---|---|---|
| INT-001 | SRI Ecuador | Saliente | SOAP sobre WSDL | XML firmado en Base64, clave de acceso, respuestas de recepción/autorización | Emisión, consulta, reintento y sincronización |
| INT-002 | Carbone | Saliente | HTTP con multipart y JSON | Plantilla, datos de render, estado y documento binario | Generación de PDF o documento |
| INT-003 | Receptores webhook | Saliente | HTTP POST JSON con HMAC-SHA256 | Evento, payload, fecha y cabeceras de firma | Eventos internos de comprobante |
| INT-004 | PostgreSQL | Bidireccional | Protocolo PostgreSQL mediante `pg` | Datos operativos, auditoría, configuración y logs | Arranque y operaciones de aplicación |
| INT-005 | Redis | Bidireccional | BullMQ e ioredis/cache-manager | Jobs, estados de cola y entradas de caché | Emisión asíncrona, webhooks y consultas cacheadas |
| INT-006 | Filesystem persistente | Bidireccional | API local de archivos y volúmenes Docker | Plantillas, PDFs, imágenes, P12 y XML | Carga, firma, render, emisión y descarga |
| INT-007 | Fuentes de imagen | Saliente (obtencion) | HTTP(S) GET o lectura local | Bytes PNG/JPG | Posprocesamiento de PDF |

## INT-001 - SRI Ecuador SOAP

La fábrica crea y mantiene en memoria clientes SOAP separados para recepción y
autorización, en ambientes `1` y `2`. Las URLs usadas por las llamadas de
negocio están codificadas en la fábrica. Recepción envía el XML firmado
codificado en Base64; autorización envía la clave de acceso.

**Evidence**
- File: `src/modules/sri/services/sri-soap-factory.service.ts`; `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: `getRecepcionClient`, `getAutorizacionClient`, `validarComprobante`, `autorizarComprobante`
- Line / Section: sri-soap-factory.service.ts 9-20, 27-65; sri-soap.client.ts 25-76
- Condition / Query / Statement: `soap.createClientAsync`, `validarComprobanteAsync`, `autorizacionComprobanteAsync`
- Confidence: High

El cliente reintenta las excepciones de recepción, registradas por el código
como errores de red, con backoff. Una
respuesta `DEVUELTA` es terminal para esta operación y evita consultar
autorización. Si recepción no devuelve ese estado, el cliente sondea
autorización y retorna `AUTORIZADO` o `NO AUTORIZADO` tan pronto observa uno de
esos estados; al agotar el sondeo retorna `EN PROCESO`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: `enviarYAutorizar`
- Line / Section: 84-212
- Condition / Query / Statement: bucles de reintento, `delayWithBackoff` y evaluación de estados de recepción/autorización
- Confidence: High

Una excepción durante una consulta de autorización se propaga
desde `autorizarComprobante` y termina `enviarYAutorizar` sin continuar el
sondeo. La configuración declara `timeoutMs` para recepción y autorización, pero
el cliente no lee ni pasa esos valores a las llamadas SOAP observadas. La
semántica operativa esperada de timeout y reintento queda pendiente en
`PFV-034`.

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: configuración `sri.rateLimiting`; `validarComprobante`, `autorizarComprobante`, `enviarYAutorizar`
- Line / Section: configuration.ts 53-66; sri-soap.client.ts 25-76, 84-212
- Condition / Query / Statement: existen ambos `timeoutMs`; el sondeo llama `autorizarComprobante` sin manejo local de excepciones y no consume los timeouts configurados
- Confidence: High

La configuración exige WSDL de recepción y autorización, y el health check usa
el WSDL configurado de recepción. Las llamadas de negocio no consumen esas dos
propiedades. La autoridad del destino permanece en `PFV-010`.

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/status/sri.health.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`
- Function / Method / Procedure: configuración SRI, `SriHealthIndicator.isHealthy`, fábrica SOAP
- Line / Section: configuration.ts 40-46; sri.health.ts 21-40; sri-soap-factory.service.ts 12-20
- Condition / Query / Statement: health usa `sri.wsdl.reception`; la fábrica usa `WSDL_URLS` local
- Confidence: High

## INT-002 - Carbone

`CARBONE_API` es obligatorio. Los servicios de documentos y PDF cargan una
plantilla en `/template`, solicitan render en `/render/{templateId}`, consultan
`/status` y descargan `/render/{renderId}` como binario.

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/document/document.service.ts`; `src/modules/pdf/pdf.service.ts`
- Function / Method / Procedure: configuración Carbone, `generateDocument`, `generatePDF`
- Line / Section: configuration.ts 11-29; document.service.ts 93-185; pdf.service.ts 42-118
- Condition / Query / Statement: llamadas Axios POST/GET a los cuatro recursos de Carbone
- Confidence: High

El polling se limita por `pdfRender.maxAttempts`; una respuesta sin los IDs
esperados o el agotamiento de intentos termina en excepción.

**Evidence**
- File: `src/modules/document/document.service.ts`; `src/modules/pdf/pdf.service.ts`
- Function / Method / Procedure: `generateDocument`, `generatePDF`
- Line / Section: document.service.ts 131-185; pdf.service.ts 71-118
- Condition / Query / Statement: validación de `templateId`/`renderId`, bucle de polling y error por tiempo agotado
- Confidence: High

## INT-003 - Receptores webhook

La configuración se almacena en PostgreSQL. Los listeners internos observados
para `comprobante.autorizado` y `comprobante.rechazado` seleccionan suscripciones
activas y crean un job `webhook-dispatch` por destino.

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `handleComprobanteAutorizado`, `handleComprobanteRechazado`, `emit`
- Line / Section: 29-55, 271-320
- Condition / Query / Statement: consulta `webhook_configs` y `webhookQueue.add(...)`
- Confidence: High

El worker genera un cuerpo JSON, calcula HMAC-SHA256 con el secreto almacenado,
envía un POST con timeout de 30 segundos y persiste el resultado en
`webhook_logs`. Una respuesta HTTP no exitosa se relanza para que BullMQ
reintente.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `process`, `logWebhook`
- Line / Section: 28-143
- Condition / Query / Statement: `createHmac('sha256', ...)`, `fetch(url, ...)`, `INSERT INTO webhook_logs`
- Confidence: High

El DTO acepta siete nombres de evento, mientras que solo se localizaron los dos
listeners anteriores. La cobertura funcional efectiva permanece en `PFV-004`.

**Evidence**
- File: `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `WEBHOOK_EVENTS`; listeners `@OnEvent`
- Line / Section: webhook.dto.ts 14-24; webhooks.service.ts 33-55
- Condition / Query / Statement: siete eventos configurables frente a dos listeners localizados
- Confidence: High

## INT-004 - PostgreSQL

La aplicación abre un pool y expone consultas parametrizadas, adquisición de
cliente y transacciones con `BEGIN`, `COMMIT` y `ROLLBACK`. El servicio cierra el
pool durante `OnModuleDestroy`.

**Evidence**
- File: `src/database/database.service.ts`
- Function / Method / Procedure: `onModuleInit`, `onModuleDestroy`, `query`, `transaction`
- Line / Section: 22-75, 95-131, 156-186
- Condition / Query / Statement: pool `pg`, consultas parametrizadas y control transaccional
- Confidence: High

PostgreSQL no se aprovisiona en los archivos Compose actuales; host y
credenciales se reciben por variables. El bootstrap del esquema se trata en
`PFV-011`.

**Evidence**
- File: `docker-compose.yml`; `docker-compose.prod.yml`; `src/config/configuration.ts`
- Function / Method / Procedure: servicios Compose y configuración de base
- Line / Section: docker-compose.yml 9-92; docker-compose.prod.yml 20-104; configuration.ts 93-101
- Condition / Query / Statement: Compose declara Redis y aplicación, sin servicio PostgreSQL
- Confidence: High

## INT-005 - Redis

BullMQ usa la base Redis configurada, por defecto `0`. La caché distribuida usa
la misma instancia, pero fija la base `1`. Las colas registradas son
`sri-emision` y `webhook-dispatch`.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/common/cache/redis-cache.module.ts`
- Function / Method / Procedure: fábricas BullMQ y CacheModule
- Line / Section: queue.module.ts 15-70; redis-cache.module.ts 15-38
- Condition / Query / Statement: conexión Redis, nombres de cola, `db: redis.db` para BullMQ y `db: 1` para caché
- Confidence: High

Las claves distribuidas identificadas son `emisor:ruc:{ruc}` y
`punto-emision:{emisorId}:{establecimiento}:{puntoEmision}`. Los catálogos,
clientes SOAP y certificados firmantes conservan cachés adicionales en memoria
del proceso, no en Redis.

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: búsquedas cacheadas y mapas locales
- Line / Section: sri-repository.service.ts 127-190; catalogo-validator.service.ts 66-76, 321-445; sri-soap-factory.service.ts 9-10; xml-signer.service.ts 288-300, 378-380
- Condition / Query / Statement: CacheManager distribuido frente a instancias `Map` locales
- Confidence: High

Producción configura `allkeys-lru` y desarrollo `noeviction`. La validez de
eviction para una instancia que también contiene BullMQ permanece en
`PFV-016`.

**Evidence**
- File: `docker-compose.prod.yml`; `docker-compose.yml`
- Function / Method / Procedure: comando de Redis
- Line / Section: docker-compose.prod.yml 26-34; docker-compose.yml 15-26
- Condition / Query / Statement: `--maxmemory-policy allkeys-lru` frente a `--maxmemory-policy noeviction`
- Confidence: High

La emision asincrona de Factura almacena el DTO completo como dato del job.
Las opciones globales de `sri-emision` conservan jobs completados y fallidos
por los conteos configurados; Redis de produccion usa AOF y un volumen. El
acceso y la retencion de esos datos fiscales/personales permanecen en
`PFV-038`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; registro de `sri-emision`; configuracion de colas; servicio Redis
- Line / Section: SriService 56-67; QueueModule 29-49; configuracion 138-145; compose 21-40, 102-104
- Condition / Query / Statement: `queue.add` recibe `{ tipo, dto }`; las opciones retienen por conteo y Redis persiste AOF en volumen.
- Confidence: High

## INT-006 - Filesystem persistente

La aplicación requiere directorios para plantillas, PDFs, certificados y XML.
Docker crea y monta esos directorios; el servidor publica árboles de PDF e
imágenes mediante rutas estáticas.

**Evidence**
- File: `src/config/configuration.ts`; `Dockerfile`; `docker-compose.prod.yml`; `src/app.module.ts`
- Function / Method / Procedure: configuración, creación/montaje de directorios y `ServeStaticModule`
- Line / Section: configuration.ts 111-117; Dockerfile 52-54; docker-compose.prod.yml 68-73; app.module.ts 63-89
- Condition / Query / Statement: directorios `/data/*`, volúmenes y rutas `/pdfs*`/`/images`
- Confidence: High

La restauración de certificados y XML requiere coordinar filas PostgreSQL con
archivos. Las dudas de autoridad y retención están centralizadas en `PFV-012` y
`PFV-015`.

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/xml-storage.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: DDL de rutas/certificados; lectura de XML y certificado
- Line / Section: init.sql 323-347, 432-454; xml-storage.service.ts 132-148; xml-signer.service.ts 303-380
- Condition / Query / Statement: PostgreSQL conserva rutas/metadatos mientras los bytes requeridos se leen del filesystem
- Confidence: High

En la persistencia de una factura, `saveAllXmls` escribe los XML de forma
síncrona dentro del callback de la transacción PostgreSQL, antes del `COMMIT`.
El `ROLLBACK` cubre las operaciones de base de datos, pero no elimina archivos
ya escritos; por ello PostgreSQL y filesystem no forman una unidad atómica. La
autoridad y el mecanismo de reconciliación entre ambos quedan en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `emitirFactura`, `persistirFactura`, `executeInTransaction`, `saveAllXmls`, `saveXml`, `DatabaseService.transaction`
- Line / Section: factura.service.ts 187-204, 340-520; sri-repository.service.ts 426-430; xml-storage.service.ts 53-130; database.service.ts 171-185
- Condition / Query / Statement: `writeFileSync` ocurre dentro del callback pasado a `db.transaction`; `ROLLBACK` solo se ejecuta sobre el cliente PostgreSQL
- Confidence: High

## INT-007 - Fuentes de imagen

El posprocesamiento de PDF acepta una URL HTTP(S) y descarga bytes con Axios;
para otras cadenas construye o usa una ruta local. Los fallos de una imagen se
registran y el procesamiento continúa con las restantes.

**Evidence**
- File: `src/modules/pdf/pdf-image.service.ts`
- Function / Method / Procedure: `addImagesToPdf`
- Line / Section: 50-133
- Condition / Query / Statement: `axios.get(image.url)` o `readFileSync(imagePath)` dentro de manejo por imagen
- Confidence: High

## Brechas de evidencia del lote

- No se inspeccionó tráfico de runtime ni configuración del entorno desplegado.
- La guía de despliegue y los Compose no coinciden plenamente; la autoridad se
  valida en `PFV-017`.
- No se confirma que todas las integraciones configurables estén activas en
  producción.
