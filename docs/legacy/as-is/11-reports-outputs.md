# 11 - Reportes y salidas AS-IS

## Estado del analisis

Este documento cubre de forma parcial las salidas confirmadas del lote 2:
componentes comunes SRI y emision de Factura. En este alcance no se identifico
un reporte analitico o contable; las salidas observadas son respuestas HTTP,
resultados de jobs, XML, eventos y mensajes de integracion. No representa aun
el inventario completo de reportes y salidas de la plataforma.

| ID | Salida | Productor | Formato o canal | Condicion o destino |
|---|---|---|---|---|
| REP-001 | Aceptacion de emision asincrona | `SriService.emitirFactura` | JSON HTTP con `mensaje`, `jobId` y `estado` | Se devuelve cuando `SRI_EMISION_ASYNC` no es exactamente `false`; estado `EN_COLA` |
| REP-002 | Resultado final de emision de Factura | `FacturaService.emitirFactura` | `FacturaResponseDto`, por HTTP sincrono o como retorno del worker | Se produce tras completar la llamada SRI; su exposicion al cliente asincrono no esta confirmada |
| REP-003 | Vista previa XML de Factura | `previewFactura` | JSON HTTP `{ xml }` | Requiere secuencial explicito; no firma, envia ni persiste |
| REP-004 | Salida de depuracion de Factura firmada | `debugFacturaFirmada` | JSON HTTP con clave, XML sin firma y XML firmado | Solo fuera de produccion; no envia ni persiste |
| REP-005 | Artefactos XML persistidos | `persistirFactura` / `XmlStorageService` | Archivos XML y rutas relativas en PostgreSQL | XML firmado siempre y autorizado cuando existe, pero solo si se ejecuta la persistencia |
| REP-006 | Eventos y jobs de webhook de Factura | `FacturaService` / `WebhooksService` | Eventos internos y jobs `webhook-dispatch` | Autorizado; rechazado solo para `RECHAZADO` o `DEVUELTA` en el flujo directo |
| REP-007 | Estados y mensajes de la operacion SRI | `SriSoapClient` | Campos de `SriOperationResult` proyectados en la respuesta | `AUTORIZADO`, `NO AUTORIZADO`, `DEVUELTA` o `EN PROCESO`, con mensajes cuando existen |

## REP-001 - Aceptacion de emision asincrona

La emision es asincrona salvo que `SRI_EMISION_ASYNC` tenga exactamente el
valor de cadena `false`. En la rama asincrona se agrega a `sri-emision` un job
con tipo `FACTURA` y el DTO completo. La respuesta inmediata contiene el ID del
job, el estado `EN_COLA` y el mensaje `Factura encolada para emisión
asíncrona`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/dto/emision-encolada.dto.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `EmisionEncoladaResponseDto`
- Line / Section: sri.service.ts 56-67; emision-encolada.dto.ts 3-11
- Condition / Query / Statement: `SRI_EMISION_ASYNC !== 'false'`, `emisionQueue.add('emision', { tipo: 'FACTURA', dto })` y respuesta con `jobId`/`EN_COLA`
- Confidence: High

El handler fija codigo HTTP `201` para ambas ramas, aunque Swagger tambien
declara `200` para el resultado sincrono. El codigo no contiene una ruta de
consulta del job ni otro mecanismo dirigido que entregue al cliente el
resultado final asociado al `jobId`. El contrato HTTP queda en `PFV-007` y el
cierre del flujo asincrono en `PFV-005`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; inventario de rutas de `SriController`; `SriService.emitirFactura`
- Line / Section: sri.controller.ts 84-110, 334-591; sri.service.ts 56-67
- Condition / Query / Statement: `@HttpCode(HttpStatus.CREATED)` convive con una respuesta Swagger 200; la busqueda dirigida no encontro `getJob`, `QueueEvents` ni una ruta de resultado de job
- Confidence: Medium

## REP-002 - Resultado final de emision de Factura

En modo sincrono, `SriService` retorna directamente el resultado de
`FacturaService`. En modo asincrono, el worker hace la misma llamada y devuelve
su resultado como retorno del procesamiento BullMQ. El resultado se proyecta a
`FacturaResponseDto` con `success`, `claveAcceso`, `estado` y, cuando estan
disponibles, `fechaAutorizacion`, `numeroAutorizacion`, `xmlAutorizado` y
`mensajes`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/factura.dto.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; `FacturaService.mapResultToResponse`; `FacturaResponseDto`
- Line / Section: sri.service.ts 56-67; sri-emision.processor.ts 24-46; factura.service.ts 207-230, 713-722; factura.dto.ts 98-118
- Condition / Query / Statement: la rama sincrona y el caso `FACTURA` del worker invocan `emitirFactura`; `mapResultToResponse` copia los campos del resultado SRI al DTO
- Confidence: High

La fila persistida usa la clave de acceso como `numero_autorizacion` cuando el
resultado no trae numero SRI, mientras la respuesta deja
`numeroAutorizacion` sin ese reemplazo. Esta diferencia de salida entre base y
API esta pendiente en `PFV-024`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `persistirFactura`; `mapResultToResponse`
- Line / Section: 365-370, 713-722
- Condition / Query / Statement: persistencia usa `resultado.numeroAutorizacion || claveAcceso`; la respuesta usa directamente `result.numeroAutorizacion`
- Confidence: High

## REP-003 - Vista previa XML de Factura

`POST /sri/preview/factura` responde `{ xml }`. El servicio exige un
secuencial explicito, genera clave de acceso y construye el XML de Factura. No
lo firma, no llama al SRI y no lo persiste.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriController.previewFactura`; `FacturaService.generarXmlPreview`
- Line / Section: sri.controller.ts 245-265; factura.service.ts 237-277
- Condition / Query / Statement: el controlador envuelve el texto como `{ xml }`; el metodo termina en `buildFactura` y exige `dto.secuencial`, sin firma, SOAP ni repositorio
- Confidence: High

## REP-004 - Salida de depuracion de Factura firmada

`POST /sri/debug/factura-firmada` devuelve `claveAcceso`, `xmlSinFirma` y
`xmlFirmado`. Exige secuencial explicito, usa el certificado vinculado al RUC y
no envia el resultado al SRI ni lo persiste. El controlador bloquea esta salida
cuando `NODE_ENV` es `production`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriController.debugFacturaFirmada`; `FacturaService.generarFacturaFirmadaDebug`
- Line / Section: sri.controller.ts 306-328; factura.service.ts 279-335
- Condition / Query / Statement: el controlador rechaza produccion; el servicio construye y firma el XML y retorna los tres campos sin invocar SOAP ni persistencia
- Confidence: High

## REP-005 - Artefactos XML persistidos

Cuando se ejecuta `persistirFactura`, se guarda siempre el XML firmado y se
guarda el XML autorizado solo si `resultado.xmlAutorizado` existe. El XML sin
firma no se guarda en este flujo. Los archivos se organizan bajo
`{RUC}/{anio}/{mes}/firmados|autorizados/{claveAcceso}.xml`; las rutas relativas
se registran como `xml_firmado_path` y `xml_autorizado_path` en PostgreSQL.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `XmlStorageService.saveAllXmls`; `XmlStorageService.saveXml`
- Line / Section: factura.service.ts 470-491; xml-storage.service.ts 30-81, 84-130
- Condition / Query / Statement: `saveAllXmls` recibe `undefined` para XML sin firma, recibe siempre `xmlFirmado` y recibe condicionalmente `resultado.xmlAutorizado`; `saveXml` devuelve la ruta relativa escrita en la fila XML
- Confidence: High

La persistencia del resultado, incluidos archivos y rutas, solo ocurre cuando
existen tanto el emisor como el punto de emision. Con secuencial automatico la
ausencia del punto produce error; con secuencial explicito el flujo puede
firmar, enviar y responder sin entrar a la persistencia. Este comportamiento
queda sujeto a `PFV-023`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`
- Line / Section: 76-104, 128-205
- Condition / Query / Statement: la rama manual no exige `puntoEmisionInfo`; tanto la persistencia `PENDIENTE` como la persistencia del resultado estan dentro de `if (emisor && puntoEmisionInfo)`
- Confidence: High

La escritura al filesystem ocurre dentro del callback de la transaccion de
PostgreSQL, pero un rollback de base no elimina los archivos ya escritos. La
reconciliacion de archivos, rutas y estado SRI permanece en `PFV-035`. El
comentario del servicio declara siete anos de retencion, sin que en este lote
se haya encontrado un proceso de expiracion o depuracion; esa politica sigue en
`PFV-015`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `XmlStorageService.saveXml`; `DatabaseService.transaction`
- Line / Section: factura.service.ts 340-520; xml-storage.service.ts 6-9, 53-130; database.service.ts 171-185
- Condition / Query / Statement: `writeFileSync` se ejecuta antes del fin del callback transaccional; `ROLLBACK` solo revierte PostgreSQL y no existe aqui una rutina de retencion o borrado de XML
- Confidence: High

## REP-006 - Eventos y webhooks de Factura

Despues de obtener una respuesta SRI, el flujo directo emite
`comprobante.autorizado` si `success` es verdadero o el estado es
`AUTORIZADO`. Emite `comprobante.rechazado` solo para `RECHAZADO` o
`DEVUELTA`. La emision de estos eventos ocurre despues del bloque condicional
de persistencia, por lo que no prueba que existan fila y archivos locales.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`
- Line / Section: 187-230
- Condition / Query / Statement: la persistencia esta protegida por `if (emisor && puntoEmisionInfo)` y luego se evaluan por separado las condiciones de eventos autorizado/rechazado
- Confidence: High

Los listeners de webhook reciben esos dos nombres de evento, seleccionan las
configuraciones activas suscritas y agregan un job `webhook-dispatch` por
destino. En el flujo directo de Factura, `NO AUTORIZADO` y `EN PROCESO` no
cumplen la condicion de evento rechazado y, por tanto, no originan ese webhook.
La semantica canonica de estados y notificaciones sigue en `PFV-025`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `WebhooksService.handleComprobanteAutorizado`; `handleComprobanteRechazado`; `emit`
- Line / Section: factura.service.ts 207-228; webhooks.service.ts 29-55, 271-320
- Condition / Query / Statement: Factura solo emite rechazo para `RECHAZADO`/`DEVUELTA`; los listeners convierten los dos eventos internos en jobs para suscriptores activos
- Confidence: High

## REP-007 - Estados y mensajes de la operacion SRI

La recepcion SOAP reconoce `RECIBIDA` y `DEVUELTA`. La operacion combinada
devuelve `DEVUELTA` con los mensajes de recepcion cuando ese estado es
terminal; devuelve `AUTORIZADO` o `NO AUTORIZADO` con mensajes de autorizacion;
y, al agotar las consultas sin resolucion, devuelve `EN PROCESO` con un mensaje
local `TIMEOUT` de tipo `ADVERTENCIA`. `FacturaResponseDto` expone esos
mensajes sin transformarlos.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`; `parseRecepcionResponse`; `extractMensajes`; `extractMensajesAutorizacion`; `FacturaService.mapResultToResponse`
- Line / Section: sri-soap.client.ts 115-212, 229-275; factura.service.ts 713-722
- Condition / Query / Statement: las ramas retornan los cuatro estados y sus arreglos `mensajes`; `mapResultToResponse` copia `result.mensajes`
- Confidence: High

Los estados con espacios (`NO AUTORIZADO`, `EN PROCESO`) difieren de valores
con guion bajo usados por consultas o reintentos en otras partes del modulo.
Ademas, como se indica en `REP-006`, esos dos estados no emiten rechazo en la
emision directa. La definicion del vocabulario de salida y sus efectos se
mantiene pendiente en `PFV-025`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/query-comprobantes.dto.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `enviarYAutorizar`; eventos de `emitirFactura`; `EstadoComprobante`; `reintentarComprobante`; `sincronizarConSri`
- Line / Section: sri-soap.client.ts 167-212; factura.service.ts 207-228; query-comprobantes.dto.ts 15-24; sri.service.ts 493-505, 693-698
- Condition / Query / Statement: SOAP produce estados con espacios, mientras DTO y reintento incluyen `NO_AUTORIZADO`/`EN_PROCESO`; las condiciones de evento directo no cubren esos retornos
- Confidence: High
