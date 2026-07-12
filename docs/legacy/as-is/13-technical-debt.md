# 13 - Deuda técnica AS-IS

## Estado del análisis

Este documento registra únicamente deuda respaldada por artefactos de los dos
primeros lotes acotados: descubrimiento estructural y flujo común SRI/Factura.
Los riesgos no resueltos enlazan la validación funcional u operativa
centralizada; no se propone una arquitectura destino.

## RISK-001 - Bootstrap PostgreSQL no autocontenido

El dump usa `extensions.uuid_generate_v4()`, pero no crea esa extensión. La guía
manual alterna entre los nombres `db_sri` y `db_sri1` y contiene pasos
destructivos de recuperación. El procedimiento autoritativo permanece en
`PFV-011`.

**Evidence**
- File: `database/init.sql`; `database/Install BD.txt`
- Function / Method / Procedure: defaults UUID y procedimiento manual
- Line / Section: init.sql 205-206 y usos repetidos hasta 685; Install BD.txt 3, 18-23, 30-44
- Condition / Query / Statement: dependencia de `extensions.uuid_generate_v4()`, bases distintas y recreación de `public`
- Confidence: High

## RISK-002 - Destino de health check distinto del cliente SRI

La configuración obliga a declarar dos WSDL y el health check usa el de
recepción configurado. La fábrica usada por las operaciones mantiene cuatro
URLs codificadas. La autoridad de esos destinos permanece en `PFV-010`.

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/status/sri.health.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`
- Function / Method / Procedure: configuración, health check y fábrica SOAP
- Line / Section: configuration.ts 40-46; sri.health.ts 21-40; sri-soap-factory.service.ts 12-20, 27-65
- Condition / Query / Statement: dos fuentes distintas de URL para verificación y operación
- Confidence: High

## RISK-003 - Certificado duplicado con lectura efectiva desde disco

Multer escribe el archivo en filesystem. Cuando se informa RUC y la vinculacion
termina, el flujo tambien copia el binario a PostgreSQL; sin ese vinculo queda
solo el archivo. El firmador usa la fila para localizar/descifrar el archivo y
lee los bytes del filesystem. Una restauracion parcial puede conservar una
copia que el flujo actual no utiliza. La fuente autoritativa permanece en
`PFV-012`.

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: vinculación y carga de certificado
- Line / Section: certificate.controller.ts 255-287, 323-363; xml-signer.service.ts 303-380
- Condition / Query / Statement: la vinculacion opcional ejecuta `certificado_p12 = $5`, mientras la firma siempre usa `readFileSync(certPath)`.
- Confidence: High

## RISK-004 - Filtros incompatibles con el dump físico

Los filtros opcionales generan condiciones contra
`c.identificacion_comprador`, `c.establecimiento` y `c.punto_emision`. Esas
columnas no están en `comprobantes` del dump; la misma consulta proyecta los
valores desde `receptor_identificacion` y joins de establecimiento/punto. El
esquema desplegado debe validarse en `PFV-013`.

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: `findComprobantes`; DDL `comprobantes`
- Line / Section: sri-repository.service.ts 439-501, 542-570; init.sql 354-399
- Condition / Query / Statement: condiciones sobre columnas ausentes frente a aliases de columnas físicas existentes
- Confidence: High

## RISK-005 - Nombres de tablas dormantes divergentes

El repositorio permite `impuestos_doc_sustento`, `destinatarios_guia` y
`detalles_guia`, pero el dump no crea esas tablas y usa `guia_destinatarios` y
`guia_detalles`. No se localizaron llamadas a los tres métodos asociados en el
lote. La intención permanece en `PFV-014`.

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: whitelist y métodos de inserción; DDL de guía
- Line / Section: sri-repository.service.ts 34-49, 347-351, 397-408; init.sql 538-584
- Condition / Query / Statement: nombres permitidos/insertados ausentes del DDL y nombres físicos alternativos
- Confidence: Medium

## RISK-006 - Retención XML declarada sin mecanismo localizado

El servicio declara organización para retención de siete años, pero el código
revisado solo crea directorios, escribe y lee archivos. No se encontró un job de
expiración, verificación o archivo. La responsabilidad permanece en `PFV-015`.

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: comentario de clase y servicio completo
- Line / Section: 6-9, 20-148
- Condition / Query / Statement: declaración de retención sin operación de ciclo de vida en el servicio
- Confidence: Medium

## RISK-007 - Eviction de Redis compartido con BullMQ

Producción usa `allkeys-lru`, mientras desarrollo usa `noeviction`. Las colas y
la caché comparten la misma instancia, aunque usan bases lógicas distintas. La
aceptación operacional de eviction permanece en `PFV-016`.

**Evidence**
- File: `docker-compose.prod.yml`; `docker-compose.yml`; `src/common/queues/queue.module.ts`; `src/common/cache/redis-cache.module.ts`
- Function / Method / Procedure: configuración Redis, BullMQ y caché
- Line / Section: docker-compose.prod.yml 26-34; docker-compose.yml 15-26; queue.module.ts 15-70; redis-cache.module.ts 19-32
- Condition / Query / Statement: `allkeys-lru` frente a `noeviction` y una instancia para jobs/cache
- Confidence: High

## RISK-008 - Guía de despliegue desalineada

La guía embebe una composición que solo declara la aplicación y un conjunto
incompleto de variables. Los Compose actuales añaden Redis, y la configuración
de código exige además WSDL, JWT, cifrado y cuatro directorios. La fuente
operativa vigente permanece en `PFV-017`.

**Evidence**
- File: `DEPLOYMENT.md`; `docker-compose.prod.yml`; `src/config/configuration.ts`
- Function / Method / Procedure: ejemplo de despliegue, Compose actual y `requireEnv`
- Line / Section: DEPLOYMENT.md 91-137; docker-compose.prod.yml 20-104; configuration.ts 7-12, 40-51, 69-72, 89-91, 111-117
- Condition / Query / Statement: servicios y variables requeridas no coincidentes
- Confidence: High

## RISK-009 - Identidad administrativa fija en el dump

El dump inserta una identidad `SUPERADMIN` fija con un hash de contraseña. Los
valores se omiten deliberadamente. Su rotación o desactivación por entorno no
puede confirmarse con el repositorio y permanece en `PFV-018`.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: carga inicial de `usuarios`
- Line / Section: 1008-1015
- Condition / Query / Statement: `INSERT INTO public.usuarios ... 'SUPERADMIN' ... ON CONFLICT DO NOTHING`
- Confidence: High

## RISK-010 - Puerto interno fijo en artefactos Docker

La aplicación escucha el `PORT` configurado, pero Docker expone y verifica
siempre el puerto interno 3001. Una configuración distinta puede dejar el
contenedor inaccesible o no saludable. La intención contractual permanece en
`PFV-008`.

**Evidence**
- File: `src/main.ts`; `Dockerfile`; `docker-compose.prod.yml`
- Function / Method / Procedure: `bootstrap`, `EXPOSE`/`HEALTHCHECK`, mapeo/healthcheck Compose
- Line / Section: main.ts 137-150; Dockerfile 56-61; docker-compose.prod.yml 64-66, 79-93
- Condition / Query / Statement: `app.listen(port)` frente a puerto interno y URL de health fijos en 3001
- Confidence: High

## RISK-011 - Doble log ante respuesta webhook no exitosa

El worker registra la respuesta HTTP antes de evaluar `response.ok`. Para una
respuesta no exitosa registra un intento fallido, lanza una excepción y el
`catch` vuelve a registrar el mismo intento como fallo. Esto puede producir dos
filas de `webhook_logs` para un único intento HTTP.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `WebhookProcessor.process`
- Line / Section: 61-108
- Condition / Query / Statement: primera llamada a `logWebhook`, `throw` por `!response.ok` y segunda llamada en `catch`
- Confidence: High

## RISK-012 - Catálogo de eventos webhook mayor que sus listeners

El contrato de entrada acepta siete eventos; solo se localizaron listeners para
`comprobante.autorizado` y `comprobante.rechazado`. Las suscripciones a los
otros nombres pueden quedar sin productor dentro de este repositorio. La
cobertura funcional permanece en `PFV-004`.

**Evidence**
- File: `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `WEBHOOK_EVENTS`; listeners `@OnEvent`
- Line / Section: webhook.dto.ts 14-24; webhooks.service.ts 33-55
- Condition / Query / Statement: siete valores aceptados frente a dos listeners localizados
- Confidence: High

## RISK-013 - RUC validado pero no aplicado a la sincronización

Para usuarios no `SUPERADMIN`, el controlador exige y valida acceso al
`rucEmisor` recibido. Luego entrega el cuerpo al servicio, pero
`sincronizarConSri` no lee esa propiedad y lista comprobantes sin filtro por RUC
o emisor. La validación previa no limita el conjunto físico procesado.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.sincronizar`; `SriService.sincronizarConSri`
- Line / Section: sri.controller.ts 561-591; sri.service.ts 677-698, 718-726
- Condition / Query / Statement: `validateRucAccess(body.rucEmisor, user)` y llamada posterior a `listarComprobantes` sin ese filtro
- Confidence: High

## RISK-014 - Offset de sincronización no aplicado por el repositorio

La sincronización avanza un `offset` y `listarComprobantes` lo traduce a
`page`. `findComprobantes` recibe `page`, pero la consulta construida solo aplica
`LIMIT`; no usa `OFFSET` y solo activa keyset cuando existe `cursor`. Lotes que
conserven el estado filtrado pueden volver a aparecer en iteraciones posteriores.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/services/sri-repository.service.ts`
- Function / Method / Procedure: `listarComprobantes`, `sincronizarConSri`, `findComprobantes`
- Line / Section: sri.service.ts 268-297, 718-726, 882-884; sri-repository.service.ts 439-452, 504-570
- Condition / Query / Statement: `offset` -> `page`, incremento de offset y SQL sin cláusula `OFFSET`
- Confidence: High

## RISK-015 - Secuencial manual permite envio sin punto persistible

Con secuencial explicito, la ausencia de un punto activo no detiene la construccion, firma ni llamada SOAP. La persistencia posterior esta protegida por `if (emisor && puntoEmisionInfo)`, por lo que el servicio puede devolver un resultado SRI y emitir evento sin crear el comprobante local. La intencion se mantiene en `PFV-023` y la reconciliacion en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `emitirFactura`
- Line / Section: 76-104, 128-151, 187-230
- Condition / Query / Statement: solo la rama automatica exige punto; la persistencia es condicional y los eventos/respuesta ocurren despues del guard.
- Confidence: High

## RISK-016 - Vocabulario de estados incompatible

SOAP retorna `NO AUTORIZADO` y `EN PROCESO`, mientras filtros, reintentos e indice usan `NO_AUTORIZADO` y `EN_PROCESO`. El flujo directo tampoco clasifica `NO AUTORIZADO` como evento rechazado. La definicion canonica permanece en `PFV-025`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/dto/query-comprobantes.dto.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/services/factura.service.ts`; `database/init.sql`
- Function / Method / Procedure: `enviarYAutorizar`; `EstadoComprobante`; `reintentarComprobante`; `sincronizarConSri`; eventos de factura; indice parcial
- Line / Section: SOAP 179-212; DTO 15-24; SriService 493-505, 693-698; factura 207-228; DDL 1480-1484
- Condition / Query / Statement: representaciones literales distintas gobiernan persistencia, seleccion, retry y eventos.
- Confidence: High

## RISK-017 - Limites no atomicos entre SRI, PostgreSQL y filesystem

La reserva, la llamada SRI y la persistencia usan limites separados. Ademas los XML se escriben dentro del callback de base de datos sin compensacion: un rollback puede dejar archivos huerfanos y una autorizacion puede quedar sin filas locales. La politica de reparacion permanece en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: fases de `emitirFactura`; `persistirFactura`; `saveAllXmls`; `saveXml`; `transaction`
- Line / Section: factura 85-205, 470-520; storage 53-130; database 171-185
- Condition / Query / Statement: SRI queda fuera de transaccion y `writeFileSync` no se revierte con `ROLLBACK`.
- Confidence: High

## RISK-018 - Registro duplicado de la cola SRI

`sri-emision` se registra globalmente con intentos/backoff/retencion y otra vez dentro de `SriModule` sin opciones. La configuracion efectiva depende de la resolucion de providers que no fue verificada en runtime; se mantiene en `PFV-031`.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: registros BullMQ de `sri-emision`
- Line / Section: QueueModule 29-49; SriModule 27-32
- Condition / Query / Statement: dos registros del mismo nombre declaran opciones diferentes.
- Confidence: Medium

## RISK-019 - Reejecucion no idempotente de la emision

El processor relanza cualquier error y `queue.add` no fija una identidad de job.
Si la cola reintenta, o si el cliente repite la solicitud HTTP, el DTO vuelve a
ejecutar todo el flujo: una factura sin secuencial reserva otro numero y genera
otro codigo numerico. Una falla despues de autorizar o persistir `PENDIENTE`
puede producir intentos/registros adicionales. La garantia permanece en
`PFV-032`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/common/queues/queue.module.ts`
- Function / Method / Procedure: despacho; `process`; `emitirFactura`; `generateCodigoNumerico`; opciones de cola
- Line / Section: SriService 56-67; processor 24-46; factura 85-117, 147-185, 505-520; clave 38-55, 121-123; queue 29-49
- Condition / Query / Statement: no se fija `jobId` ni se reutiliza identidad entre invocaciones; el retry de cola es condicional, pero la repeticion HTTP tambien crea una ejecucion nueva.
- Confidence: Medium

## RISK-020 - Timeouts configurados no aplicados y retry parcial

La configuracion declara timeouts separados para recepcion y autorizacion, pero la fabrica/cliente no los aplica. El bucle de autorizacion reintenta respuestas no terminales, no excepciones, que salen en el primer fallo afectado. La expectativa operativa queda en `PFV-034`.

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`
- Function / Method / Procedure: configuracion de rate limiting; `autorizarComprobante`; `enviarYAutorizar`; creacion de clientes
- Line / Section: config 53-66; SOAP 47-77, 84-227; factory 27-65
- Condition / Query / Statement: `timeoutMs` se configura pero no se lee; el `for` de autorizacion no contiene `catch`.
- Confidence: High

## RISK-021 - Vigencia del certificado no comprobada al firmar

La carga rechaza un P12 vencido o aun no valido, pero la firma posterior consulta nombre/password, lee o reutiliza el certificado cacheado y no evalua sus fechas. Un certificado que vence despues de ser cargado puede seguir usandose. La regla esperada permanece en `PFV-029`.

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/certificate/certificate.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: carga; `validateCertificateExpiry`; `loadEmisorCertificate`; `signXmlForEmisor`
- Line / Section: controller 212-271; certificate service 287-332; signer 285-454
- Condition / Query / Statement: las fechas se controlan durante upload; el camino de firma no consulta `notAfter` ni `certificado_valido_hasta`.
- Confidence: High

## RISK-022 - Mensajes internos expuestos en errores 500

El filtro global usa `Error.message` como cuerpo tambien en produccion. Errores de DB, filesystem, P12, descifrado o SOAP pueden llegar al cliente; solo el stack esta condicionado a desarrollo.

**Evidence**
- File: `src/common/filters/http-exception.filter.ts`; `src/main.ts`
- Function / Method / Procedure: `AllExceptionsFilter.catch`; registro global del filtro
- Line / Section: filtro 20-45; main 62-63
- Condition / Query / Statement: excepciones no HTTP conservan status 500 y su mensaje sustituye al texto generico.
- Confidence: High

## RISK-023 - Secuencial manual no protege el contador

La rama manual no actualiza `secuenciales`. El dump solo garantiza unicidad del contador por punto/tipo y de la clave de acceso; no del numero de comprobante por punto/tipo. Como la clave incorpora un codigo aleatorio, el generador automatico puede alcanzar mas tarde un numero ya emitido manualmente. La politica queda en `PFV-023`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `database/init.sql`
- Function / Method / Procedure: seleccion de secuencial; `getNextSecuencial`; `generate`; constraints
- Line / Section: factura 85-104; repositorio 199-219; clave 38-53, 121-123; DDL 1207-1219, 1319-1323
- Condition / Query / Statement: solo el automatico incrementa el contador y no hay constraint por punto/tipo/numero en `comprobantes`.
- Confidence: High

## RISK-024 - Invalidacion incompleta de caches operativas

El repositorio cachea emisor y punto activos en Redis, pero los servicios CRUD no contienen invalidacion dirigida. Al vincular P12 se limpia la cache criptografica; al eliminarlo se limpian DB/archivo sin llamar esa invalidacion, por lo que material ya cargado puede permanecer hasta el TTL.

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `src/modules/emisores/emisores.service.ts`; `src/modules/puntos-emision/puntos-emision.service.ts`; `src/modules/certificate/certificate.controller.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: `findEmisorByRuc`; `findPuntoEmision`; CRUD; upload/delete de certificado; cache del firmador
- Line / Section: repositorio 142-190; emisores 284-422; puntos 102-283; certificate controller 101-143, 347-363; signer 285-300, 378-380, 455-470
- Condition / Query / Statement: las lecturas crean cache; no se localizaron `cacheManager.del` en CRUD y delete de P12 no llama `clearEmisorCache`.
- Confidence: Medium

## RISK-025 - Seleccion temporal incompleta de tarifas

El modelo fisico permite revisiones por `vigente_desde`, pero la consulta solo
exige activo y que `vigente_hasta` no haya vencido. No filtra el inicio ni
ordena revisiones; el mapa sobrescribe por impuesto/porcentaje. Una fila futura
puede validarse antes de su fecha y, con revisiones simultaneas, la seleccion
depende del orden de filas. La semantica se mantiene en `PFV-028`.

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/catalogo-validator.service.ts`
- Function / Method / Procedure: DDL `catalogo_tarifas_impuesto`; `CatalogoValidatorService.loadCache`
- Line / Section: DDL 172-183, 1116-1124; servicio 343-363
- Condition / Query / Statement: `vigente_desde` y `ORDER BY` no aparecen en la consulta; la clave de `Map.set` omite la revision temporal.
- Confidence: High

## RISK-026 - Validacion de clave divergente en controllers

Existe validacion de 49 digitos mas Modulo 11, pero las rutas extraen el RUC con otra utilidad que solo valida longitud/digitos y lanza `Error` plano. Una clave con checksum incorrecto puede superar esa capa; una clave mal formada produce 500 y puede incluir el valor completo en mensaje/log. La decision permanece en `PFV-030`.

**Evidence**
- File: `src/modules/sri/services/clave-acceso.service.ts`; `src/modules/sri/utils/clave-acceso.utils.ts`; `src/modules/sri/sri.controller.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `ClaveAccesoService.validate`; utilidad `validateClaveAcceso`; `validateClaveAccesoAccess`; filtro global
- Line / Section: servicio 62-75; utilidad 38-51; controller 76-81; filtro 20-45
- Condition / Query / Statement: la validacion completa no tiene call site de produccion; la utilidad alternativa gobierna los parametros HTTP.
- Confidence: High

## RISK-027 - Seleccion de emisor por RUC sin aislamiento consistente

El servicio de creacion rechaza un RUC encontrado globalmente, pero el DDL
permite repetirlo entre tenants y permite `tenant_id` nulo. La guarda HTTP, el repositorio de emision, el firmador y sus caches
buscan solo por RUC; la guarda permite una fila con tenant nulo a cualquier
usuario que tenga tenant. La vinculacion de certificado tambien actualiza por
RUC. Si existen filas repetidas, distintas capas pueden seleccionar o modificar
otro emisor/certificado. La politica de unicidad y alcance queda en `PFV-036`.

**Evidence**
- File: `database/init.sql`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: constraint; `findByRuc`; `validateRucAccess`; `create`; `findEmisorByRuc`; `loadEmisorCertificate`; `bindCertificateToEmisor`
- Line / Section: dump 432-454, 1239-1243; emisores 227-291; repositorio 142-160; signer 285-385; certificate controller 255-287, 323-380
- Condition / Query / Statement: create intenta unicidad global, el DDL usa tenant/RUC y consultas/caches/`UPDATE` usan solo RUC; tenant nulo no activa el rechazo.
- Confidence: High

## RISK-028 - Tarifa valida aunque el impuesto padre este inactivo

La carga de tarifas une `catalogo_impuestos`, pero solo filtra la actividad de
la tarifa. `validateImpuesto` considera valida la pareja por su presencia en el
mapa, por lo que una tarifa activa de un impuesto padre inactivo sigue
disponible para Factura. La regla funcional queda en `PFV-037`.

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/catalogo-validator.service.ts`
- Function / Method / Procedure: DDL de impuestos/tarifas; `loadCache`; `validateImpuesto`
- Line / Section: dump 126-137, 172-183; catalogo 84-100, 343-363
- Condition / Query / Statement: `i.activo` no aparece en el predicado y la validacion solo comprueba la entrada del mapa.
- Confidence: High

## RISK-029 - DTO fiscal completo retenido en BullMQ/Redis

La emision asincrona guarda `{ tipo, dto }`, incluido el contenido completo de
Factura, como dato del job. Las opciones globales retienen por conteo jobs
completados y fallidos, y Redis de produccion usa AOF y volumen persistente. No
se localizo en este lote una politica especifica de minimizacion, acceso o plazo
para esos datos. La decision operativa queda en `PFV-038`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; registro `sri-emision`; configuracion de colas; servicio Redis
- Line / Section: SriService 56-67; QueueModule 29-49; configuracion 138-145; compose 21-40, 102-104
- Condition / Query / Statement: `queue.add` recibe el DTO; los defaults conservan jobs por conteo y Redis persiste AOF en volumen.
- Confidence: Medium

## RISK-030 - Administracion de P12 sin alcance por rol o tenant

El controller de certificados depende de las guardas globales, pero no declara
roles. Listado, informacion, validacion y eliminacion no reciben tenant; upload
solo valida el RUC cuando se solicita vinculacion. La eliminacion opera por
nombre de archivo y limpia todas las filas que lo referencian. La politica de
administracion permanece en `PFV-039`.

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/guards/roles.guard.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: guardas globales; `RolesGuard.canActivate`; endpoints de certificados; `bindCertificateToEmisor`
- Line / Section: app 120-135; roles guard 16-48; certificate controller 63-173, 190-287, 323-421
- Condition / Query / Statement: RolesGuard permite rutas sin metadata; el controller no declara roles, valida RUC solo en vinculacion y delete actualiza por `certificado_nombre`.
- Confidence: High

## Resumen de PFV relacionados

| Riesgo | Validación pendiente |
|---|---|
| RISK-001 | PFV-011 |
| RISK-002 | PFV-010 |
| RISK-003 | PFV-012 |
| RISK-004 | PFV-013 |
| RISK-005 | PFV-014 |
| RISK-006 | PFV-015 |
| RISK-007 | PFV-016 |
| RISK-008 | PFV-017 |
| RISK-009 | PFV-018 |
| RISK-010 | PFV-008 |
| RISK-012 | PFV-004 |
| RISK-015 | PFV-023, PFV-035 |
| RISK-016 | PFV-025 |
| RISK-017 | PFV-035 |
| RISK-018 | PFV-031 |
| RISK-019 | PFV-032 |
| RISK-020 | PFV-034 |
| RISK-021 | PFV-029 |
| RISK-023 | PFV-023 |
| RISK-025 | PFV-028 |
| RISK-026 | PFV-030 |
| RISK-027 | PFV-036 |
| RISK-028 | PFV-037 |
| RISK-029 | PFV-038 |
| RISK-030 | PFV-039 |

RISK-011, RISK-013, RISK-014, RISK-022 y RISK-024 se apoyan en secuencias o
ausencias tecnicas directas y no requieren convertir una suposicion funcional
en requisito. Sus impactos deben considerarse al interpretar `webhook_logs`,
operar la sincronizacion y manejar caches/errores.
