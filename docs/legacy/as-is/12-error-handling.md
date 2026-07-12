# 12 - Manejo de errores AS-IS

## Alcance del lote

Este documento cubre el manejo de errores confirmado para el flujo comun SRI y,
en detalle, la emision y el reintento de facturas. Distingue validaciones HTTP,
resultados funcionales del SRI, excepciones tecnicas, fallos de persistencia y
errores de jobs asincronos. No confirma politicas operativas que no aparecen en
los artefactos; esas decisiones permanecen en los PFV relacionados.

## Resumen

| ID | Area | Condicion | Comportamiento observado | PFV relacionado |
|---|---|---|---|---|
| ERR-001 | Entrada HTTP | DTO no valido o propiedad no permitida | La tuberia global rechaza la solicitud antes del servicio | - |
| ERR-002 | Precondiciones de factura | Tenant, emisor, punto automatico o certificado no validos | Acceso previo: HTTP 403/404; precondiciones internas: 400 sincrono o job fallido; con tenant de usuario presente, un emisor de tenant nulo no se rechaza | PFV-023, PFV-036 |
| ERR-003 | Recepcion SRI | Se agotan los intentos por excepcion | Intenta `PENDIENTE`; relanza el error original solo si esa persistencia no falla | PFV-006, PFV-032, PFV-035 |
| ERR-004 | Resultado SRI | `DEVUELTA`, `NO AUTORIZADO` o `EN PROCESO` | Resultado `success: false`; persistencia condicional y sin excepcion tecnica | PFV-023, PFV-025, PFV-035 |
| ERR-005 | Autorizacion SRI | Cualquier excepcion durante una consulta | Sale del bucle de autorizacion en ese intento | PFV-034 |
| ERR-006 | Persistencia de factura | Falla una escritura dentro de la transaccion | Rollback, evento de alerta y propagacion | PFV-006, PFV-035 |
| ERR-007 | Persistencia XML | Falla DB despues de escribir archivos | El rollback DB no elimina XML ya escritos | PFV-035 |
| ERR-008 | Reintento manual | Estado o ruta/contenido XML no elegible | `BadRequestException`; un error tecnico de lectura se propaga sin conversion | PFV-025 |
| ERR-009 | Resultado del reintento | Estado actualizado y XML autorizado ausente o falla su almacenamiento | Estado y XML pueden quedar desalineados | PFV-035 |
| ERR-010 | Firma XML | Falla carga, lectura, P12 o firma despues de la prevalidacion | 500 en modo sincrono; job fallido en modo asincrono | PFV-029 |
| ERR-011 | Worker SRI | El servicio delegado falla | El worker registra y relanza el error del job | PFV-005, PFV-031, PFV-032 |
| ERR-012 | Filtro global | Llega un `Error` no HTTP | Devuelve 500 con el mensaje interno; en desarrollo agrega stack | - |
| ERR-013 | Clave de acceso | Parametro nulo, longitud o caracteres invalidos | La utilidad lanza `Error`; el filtro lo convierte en 500 | PFV-030 |
| ERR-014 | Preview y debug | No se entrega secuencial | `BadRequestException` antes de generar el XML | - |

## ERR-001 - Rechazo global de DTO y propiedades no permitidas

La aplicacion instala una `ValidationPipe` global con transformacion, lista
blanca y rechazo de propiedades no declaradas. Para factura, el DTO declara el
formato de fecha, el formato del secuencial y la validacion anidada de emisor,
comprador, detalles y pagos. Los incumplimientos procesados por esta tuberia no
entran a `FacturaService` y se representan como error HTTP de validacion.

**Evidence**
- File: `src/main.ts`; `src/modules/sri/dto/factura.dto.ts`
- Function / Method / Procedure: `bootstrap`; `CreateFacturaDto`
- Line / Section: main.ts 53-60; factura.dto.ts 20-95
- Condition / Query / Statement: `ValidationPipe({ transform: true, whitelist: true, forbidNonWhitelisted: true })` y decoradores `class-validator` del DTO
- Confidence: High

## ERR-002 - Precondiciones de acceso, emisor, punto y certificado

Antes de encolar o ejecutar una factura, el controlador valida que el RUC sea
accesible para el usuario. Un emisor inexistente genera 404 y un tenant no
autorizado y no nulo genera 403; una fila de emisor con tenant nulo supera esa
condicion. Dentro de la emision, la ausencia del punto impide solo la
generacion automatica del secuencial con `BadRequestException`; la falta de
metadatos del certificado genera la misma excepcion antes de firmar. Estas dos
fallas internas producen 400 solo en modo sincrono y marcan el job como fallido
en modo asincrono. La excepcion del punto cuando se recibe un secuencial manual
se mantiene en `PFV-023`; el alcance por RUC y tenant, en `PFV-036`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; `validateRucAccess`; `SriService.emitirFactura`; `SriEmisionProcessor.process`; `FacturaService.emitirFactura`
- Line / Section: controller 84-110; emisores 222-252; SriService 56-67; processor 24-46; factura 76-104, 128-138
- Condition / Query / Statement: la busqueda/comparacion de acceso precede al despacho; punto y certificado se validan dentro del servicio ejecutado directamente o por el worker.
- Confidence: High

## ERR-003 - Agotamiento de recepcion SRI y persistencia PENDIENTE

La recepcion reintenta las excepciones hasta el maximo configurado, esperando
con backoff antes de cada intento posterior. Si todos fallan, relanza el ultimo
error. Factura intercepta esa excepcion y, cuando conserva emisor y punto,
intenta persistir el comprobante como `PENDIENTE` con un mensaje sintetico
`SRI_TIMEOUT`. Si esa persistencia termina, vuelve a lanzar la excepcion SRI; si
la persistencia lanza, el `throw` original no se alcanza y se propaga el nuevo
error. El mecanismo de recuperacion, reconciliacion e idempotencia se mantiene
en `PFV-006`, `PFV-032` y `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`; `FacturaService.emitirFactura`
- Line / Section: sri-soap.client.ts 84-145, 215-227; factura.service.ts 147-185
- Condition / Query / Statement: bucle de recepcion, `throw ultimoError` y transaccion `PENDIENTE` previa al `throw error`; una excepcion de esa transaccion interrumpe el catch.
- Confidence: High

## ERR-004 - Estados SRI negativos tratados como resultados

Una recepcion `DEVUELTA`, una autorizacion `NO AUTORIZADO` y el agotamiento de
consultas con estado `EN PROCESO` producen un `SriOperationResult` con
`success: false`; no generan una excepcion tecnica. Factura persiste ese estado
solo cuando dispone de emisor y punto, y despues devuelve el resultado. En la
rama manual sin punto puede omitir la fila local. Solo `DEVUELTA` o `RECHAZADO`
activan el evento local de rechazo, por lo que la semantica de estados y
notificaciones permanece en `PFV-025` y la reconciliacion en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`; `FacturaService.emitirFactura`, `persistirFactura`
- Line / Section: sri-soap.client.ts 137-145, 167-212; factura.service.ts 187-230, 355-369
- Condition / Query / Statement: retornos con `success: false`; insercion de `resultado.estado`; evento restringido a `RECHAZADO` o `DEVUELTA`
- Confidence: High

## ERR-005 - Excepcion durante autorizacion

Despues de una recepcion aceptada, el cliente repite consultas de autorizacion
cuando la respuesta aun no contiene un resultado final. La llamada a
`autorizarComprobante` no esta dentro de un `try/catch` en ese bucle: cualquier
excepcion se propaga en el primer intento afectado, aunque queden consultas
configuradas. Factura la captura, la etiqueta como `SRI_TIMEOUT` y usa la misma
rama que intenta registrar `PENDIENTE`. La politica operativa esperada
permanece en `PFV-034`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`, `autorizarComprobante`; `FacturaService.emitirFactura`
- Line / Section: sri-soap.client.ts 47-77, 147-195; factura.service.ts 147-185
- Condition / Query / Statement: `await this.autorizarComprobante(claveAcceso)` sin captura interna y captura exterior de toda excepcion SOAP
- Confidence: High

## ERR-006 - Rollback y evento ante fallo de persistencia

La cabecera, detalles, impuestos, adicionales, totales, pagos, ruta XML e
informacion adicional se escriben con el mismo `PoolClient`. Ante cualquier
fallo, `DatabaseService.transaction` ejecuta rollback. `persistirFactura`
registra el error, emite `comprobante.persistencia_fallida` y lo relanza. No se
encontro un listener para ese evento en el repositorio; su consumo operativo
permanece en `PFV-006` y la reconciliacion en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/database/database.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `SriRepositoryService.executeInTransaction`; `DatabaseService.transaction`; listeners `@OnEvent`
- Line / Section: factura.service.ts 340-520; sri-repository.service.ts 426-430; database.service.ts 169-185; webhooks.service.ts 29-55
- Condition / Query / Statement: escrituras con `client`, `ROLLBACK`, emision de `comprobante.persistencia_fallida` y ausencia de listener homonimo entre los listeners observados
- Confidence: High

## ERR-007 - Escrituras de filesystem fuera del rollback PostgreSQL

El XML firmado se guarda siempre durante `persistirFactura` y el autorizado se
guarda cuando esta presente. `saveAllXmls` usa escrituras sincronicas de
filesystem antes de registrar las rutas y antes de la informacion adicional.
Aunque la operacion ocurre dentro del callback temporal de la transaccion, el
rollback PostgreSQL no compensa archivos ya creados. La limpieza o
reconciliacion permanece en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `XmlStorageService.saveAllXmls`, `saveXml`; `DatabaseService.transaction`
- Line / Section: factura.service.ts 470-503; xml-storage.service.ts 43-81, 84-130; database.service.ts 169-185
- Condition / Query / Statement: `writeFileSync` precede a `repository.saveXml` y no existe compensacion de archivos en la rama `ROLLBACK`
- Confidence: High

## ERR-008 - Guardas del reintento manual

El reintento manual exige un comprobante existente, uno de cuatro estados
admitidos, una ruta de XML firmado en PostgreSQL y contenido no vacio leido del
filesystem. La ausencia de fila, estado elegible, ruta o contenido produce
`BadRequestException` y evita la llamada SOAP. Si el archivo existe pero
`readFileSync` lanza, `readXml` no captura ese error tecnico. Las diferencias de
vocabulario y la elegibilidad funcional permanecen en `PFV-025`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `SriService.reintentarComprobante`; `XmlStorageService.readXml`
- Line / Section: sri.service.ts 473-536; xml-storage.service.ts 132-140
- Condition / Query / Statement: guardas para existencia, `DEVUELTA|RECHAZADO|PENDIENTE|EN_PROCESO`, ruta y contenido antes de SOAP; `readFileSync` queda fuera de manejo local.
- Confidence: High

## ERR-009 - Estado y XML del reintento sin transaccion comun

Tras reenviar, el servicio actualiza primero el estado del comprobante. Solo si
fue autorizado y el resultado contiene `xmlAutorizado`, despues escribe el XML
en disco y registra su ruta mediante llamadas sin `PoolClient`. La ausencia del
XML omite ese bloque; un fallo de archivo o de `saveXml` ocurre despues de que
el estado pudo quedar `AUTORIZADO`. No hay rollback compartido ni evento de
compensacion. La reconciliacion se mantiene en `PFV-035`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `SriService.reintentarComprobante`; `SriRepositoryService.updateComprobante`, `saveXml`; `XmlStorageService.saveXml`
- Line / Section: sri.service.ts 538-586; sri-repository.service.ts 254-280, 358-372; xml-storage.service.ts 53-81
- Condition / Query / Statement: `updateComprobante` antecede al bloque condicional `esAutorizado && resultado.xmlAutorizado`; las llamadas XML no reciben cliente transaccional.
- Confidence: High

## ERR-010 - Errores tecnicos de firma convertidos en 500

Superada la prevalidacion de metadatos, el firmador puede lanzar `Error` por fila
de emisor incompleta, archivo P12 ausente, contenido P12 invalido, clave o
certificado ausentes, XML sin raiz o fallo de firma. Esos errores no se
convierten en `HttpException`: `FacturaService` los registra y relanza. En modo
sincrono el filtro global responde 500; en modo asincrono el worker recibe y
relanza el error despues del 201 inicial. La validacion de vigencia al momento
de firma se mantiene en `PFV-029`.

**Evidence**
- File: `src/modules/sri/services/xml-signer.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: carga/firma; `FacturaService.emitirFactura`; despacho; `SriEmisionProcessor.process`; `AllExceptionsFilter.catch`
- Line / Section: signer 285-454; factura 128-144, 231-234; SriService 56-67; processor 24-46; filtro 20-45
- Condition / Query / Statement: errores de firma se propagan sin conversion; el modo directo alcanza el filtro HTTP y el modo asincrono los relanza dentro del worker.
- Confidence: High

## ERR-011 - El worker relanza el fallo del servicio delegado

El consumidor `sri-emision` delega segun el tipo de comprobante. Ante cualquier
error registra job, tipo, mensaje y stack, y vuelve a lanzar la misma excepcion.
El cliente HTTP ya recibio solo el identificador del job cuando la emision opera
en modo asincrono. La consulta del resultado permanece en `PFV-005`; la
configuracion efectiva de retry, debido a los registros de cola encontrados,
permanece en `PFV-031`, y la idempotencia del flujo completo en `PFV-032`.

**Evidence**
- File: `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `SriEmisionProcessor.process`; `SriService.emitirFactura`; registros BullMQ de `sri-emision`
- Line / Section: sri-emision.processor.ts 24-46; sri.service.ts 56-67; queue.module.ts 29-49; sri.module.ts 27-32
- Condition / Query / Statement: el processor ejecuta `throw error`; la respuesta HTTP contiene `jobId`; la misma cola aparece registrada con y sin `defaultJobOptions`
- Confidence: High

## ERR-012 - Mensaje interno expuesto por el filtro global

Para un `Error` que no sea `HttpException`, el filtro conserva el estado 500
pero reemplaza el mensaje generico inicial por `exception.message`. Ese texto se
devuelve en `error` tanto en produccion como en desarrollo; solo el stack queda
condicionado a `NODE_ENV === 'development'`. Por ello mensajes de DB,
filesystem, certificado o SOAP propagados por factura forman parte de la
respuesta HTTP observada en modo sincrono. En modo asincrono, los errores que
ocurren dentro del worker no atraviesan este filtro tras la respuesta inicial.

**Evidence**
- File: `src/common/filters/http-exception.filter.ts`; `src/main.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `AllExceptionsFilter.catch`; registro global; despacho; `SriEmisionProcessor.process`; `FacturaService.emitirFactura`
- Line / Section: filtro 15-45; main 62-63; SriService 56-67; processor 24-46; factura 231-234
- Condition / Query / Statement: el filtro devuelve `Error.message` en solicitudes HTTP; el modo asincrono responde antes y procesa/relanza el error dentro del worker.
- Confidence: High

## ERR-013 - Clave de acceso invalida convertida en 500

Las rutas por parametro validan el acceso del tenant extrayendo primero el RUC.
La utilidad de extraccion lanza `Error`, no `BadRequestException`, cuando la
clave esta vacia, no tiene 49 caracteres o contiene caracteres no numericos. El
filtro global clasifica ese error como 500 y devuelve su mensaje, que incluye el
valor invalido recibido para los dos ultimos casos. La utilidad no comprueba el
digito verificador; el checksum y la correccion del contrato 4xx permanecen en
`PFV-030`.

**Evidence**
- File: `src/modules/sri/utils/clave-acceso.utils.ts`; `src/modules/sri/sri.controller.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `extractRucFromClaveAcceso`, `validateClaveAcceso`; `SriController.validateClaveAccesoAccess`; `AllExceptionsFilter.catch`
- Line / Section: clave-acceso.utils.ts 16-19, 38-51; sri.controller.ts 76-82, 389-395, 413-420, 448-454, 474-486, 504-519; http-exception.filter.ts 20-45
- Condition / Query / Statement: la utilidad lanza `Error` con el dato invalido y el filtro asigna 500 a excepciones no HTTP
- Confidence: High

## ERR-014 - Secuencial obligatorio para preview y debug

Los caminos de preview sin firma y debug firmado no reservan un secuencial. En
ambos, la ausencia de `dto.secuencial` genera `BadRequestException` antes de
construir la clave y el XML. El preview no firma ni llama al SRI; el debug firma
despues de superar esta guarda.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.generarXmlPreview`, `generarFacturaFirmadaDebug`
- Line / Section: 237-277, 279-335
- Condition / Query / Statement: ambas ramas ejecutan `throw new BadRequestException(...)` cuando no existe `dto.secuencial`
- Confidence: High

## Limites de evidencia

- No se ejecuto el servidor ni se provocaron fallos contra SRI, PostgreSQL,
  Redis o filesystem.
- No se inspeccionaron jobs reales; el efecto runtime de registrar
  `sri-emision` en dos modulos permanece en `PFV-031`; la idempotencia ante un
  retry completo permanece en `PFV-032`.
- No se confirma un consumidor externo del evento
  `comprobante.persistencia_fallida`; permanece en `PFV-006`.
- Los PFV citados son preguntas de validacion, no requisitos confirmados.
