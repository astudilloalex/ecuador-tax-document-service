# 00 - Vista general del sistema AS-IS

## Estado del documento

| Campo | Valor |
|---|---|
| Lote | 1-2 - Inventario estructural y analisis acotado del nucleo SRI comun y Factura |
| Cobertura | Manifiestos, bootstrap, modulos NestJS, configuracion, datos, despliegue y contratos disponibles; reglas, validaciones, flujos, acceso acotado, salidas, errores y backlog SDD del camino de Factura y sus servicios SRI compartidos |
| Fuera de alcance | Analisis profundo de Nota de Credito, Nota de Debito, Retencion, Guia de Remision y de los demas modulos funcionales; comportamiento operativo en ejecucion |
| Fecha de corte | 2026-07-10 |

Este documento consolida el mapa estructural del lote 1 y la cobertura funcional acotada del lote 2. No representa cobertura funcional completa ni confirma que los artefactos de despliegue o documentacion coincidan con el entorno actualmente operado.

**Evidence**
- File: `package.json`; `src/app.module.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/factura.dto.ts`
- Function / Method / Procedure: Manifiesto NPM; `AppModule`; metodos compartidos de `SriBaseService`; `FacturaService.emitirFactura`; `CreateFacturaDto`
- Line / Section: `package.json:2-4,23-43`; `src/app.module.ts:41-143`; `src/modules/sri/services/sri-base.service.ts:23-175`; `src/modules/sri/services/factura.service.ts:42-230`; `src/modules/sri/dto/factura.dto.ts:20-96`
- Condition / Query / Statement: El lote 1 inventario la composicion estatica; el lote 2 inspecciono el flujo comun y de Factura sin ejecutar la aplicacion, jobs, llamadas SRI ni pruebas.
- Confidence: High

## Proposito confirmado

El sistema es una API NestJS orientada a emitir y gestionar comprobantes electronicos para el SRI de Ecuador. El codigo expone operaciones para facturas, notas de credito, notas de debito, retenciones y guias de remision, ademas de consultas y gestion local de comprobantes.

**Evidence**
- File: `package.json`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: Descripcion del paquete; `SriController`
- Line / Section: `package.json:2-10`; `src/modules/sri/sri.controller.ts:61-212,221-288,334-561`
- Condition / Query / Statement: El manifiesto identifica la API como facturacion electronica SRI y el controlador declara endpoints para los cinco tipos de comprobante y su gestion.
- Confidence: High

La aplicacion tambien incluye capacidades separadas para autenticacion, tenants, emisores, puntos de emision, webhooks, certificados P12, plantillas, documentos, PDF, firma de PDF, imagenes y estado del servicio. El detalle de cada limite se mantiene en `02-module-inventory.md` mediante `MOD-001` a `MOD-020`.

**Evidence**
- File: `src/app.module.ts`
- Function / Method / Procedure: `AppModule`
- Line / Section: `src/app.module.ts:20-37,94-119`
- Condition / Query / Statement: `AppModule` importa de forma explicita los modulos de infraestructura y los trece modulos funcionales descubiertos.
- Confidence: High

## Limite tecnico observado

### Entrada HTTP

El proceso arranca una aplicacion HTTP NestJS, aplica validacion global, un filtro global de excepciones, CORS, cabeceras mediante Helmet y publica Swagger UI bajo `/api`.

**Evidence**
- File: `src/main.ts`
- Function / Method / Procedure: `bootstrap`
- Line / Section: `src/main.ts:21-63,65-126,145-150`
- Condition / Query / Statement: `bootstrap` crea `AppModule`, registra componentes HTTP globales, configura Swagger y escucha en el puerto configurado.
- Confidence: High

### Persistencia y estado

PostgreSQL es el almacen relacional accedido mediante un pool compartido. El repositorio incluye un dump SQL con un tipo enumerado, 29 tablas, indices, claves y datos iniciales; la autoridad operativa de ese bootstrap permanece pendiente en `PFV-011` y el estado operativo del usuario sembrado en `PFV-018`.

**Evidence**
- File: `src/database/database.service.ts`; `database/init.sql`
- Function / Method / Procedure: `DatabaseService.onModuleInit`, `query`, `transaction`; dump PostgreSQL
- Line / Section: `src/database/database.service.ts:22-45,95-186`; `database/init.sql:35-47,51-75,97-748,1012-1015`
- Condition / Query / Statement: El servicio crea un `Pool` de PostgreSQL y el dump define las estructuras persistentes e incluye una fila inicial de usuario cuyos valores no se reproducen.
- Confidence: High

Redis cumple dos funciones estructurales: cache distribuida y backend de BullMQ. La cache usa una base separada de las colas; BullMQ registra las colas `sri-emision` y `webhook-dispatch`.

**Evidence**
- File: `src/common/cache/redis-cache.module.ts`; `src/common/queues/queue.module.ts`
- Function / Method / Procedure: `RedisCacheModule`; `QueueModule`
- Line / Section: `src/common/cache/redis-cache.module.ts:12-38`; `src/common/queues/queue.module.ts:11-74`
- Condition / Query / Statement: Ambos modulos obtienen la conexion Redis desde configuracion; la cache selecciona DB 1 y las colas usan la DB configurada.
- Confidence: High

El filesystem conserva plantillas, PDF, certificados y XML en directorios configurables. La aplicacion sirve subconjuntos de PDF e imagenes como contenido estatico.

**Evidence**
- File: `src/config/configuration.ts`; `src/app.module.ts`; `Dockerfile`
- Function / Method / Procedure: Configuracion `directories`; `ServeStaticModule.forRootAsync`; etapa de produccion Docker
- Line / Section: `src/config/configuration.ts:111-117`; `src/app.module.ts:63-92`; `Dockerfile:52-54`
- Condition / Query / Statement: Las rutas se requieren desde el entorno, se montan como directorios persistentes y varias se publican mediante rutas HTTP estaticas.
- Confidence: High

## Cobertura funcional confirmada del lote 2

### Despacho de Factura

La emision de Factura se ejecuta directamente solo cuando `SRI_EMISION_ASYNC` es exactamente `false`; en los demas casos se encola el DTO completo y se devuelve `EN_COLA` con el identificador del job. El worker delega el tipo `FACTURA` a `FacturaService` y relanza sus errores. La cola se registra con opciones globales y tambien dentro de `SriModule` sin esas opciones, por lo que la configuracion efectiva de intentos y backoff permanece en `PFV-031`; la idempotencia ante retry o reenvio HTTP permanece en `PFV-032`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; registros BullMQ de `sri-emision`
- Line / Section: `src/modules/sri/sri.service.ts:56-67`; `src/modules/sri/processors/sri-emision.processor.ts:24-46`; `src/common/queues/queue.module.ts:29-49`; `src/modules/sri/sri.module.ts:27-32`
- Condition / Query / Statement: El servicio decide entre llamada directa y job; el processor ejecuta Factura; la misma cola aparece registrada con dos formas distintas.
- Confidence: High

El job conserva el DTO fiscal completo. Las opciones globales declaran
retencion por conteo de jobs completados/fallidos y Redis de produccion usa AOF
con volumen. La politica de acceso y retencion de esos datos permanece en
`PFV-038`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; registro de `sri-emision`; configuracion de colas; servicio Redis
- Line / Section: `src/modules/sri/sri.service.ts:56-67`; `src/common/queues/queue.module.ts:29-49`; `src/config/configuration.ts:138-145`; `docker-compose.prod.yml:21-40,102-104`
- Condition / Query / Statement: `queue.add` recibe el DTO; las opciones retienen por conteo y Redis persiste AOF en un volumen.
- Confidence: High

### Emision y persistencia de Factura

`FacturaService` valida comprador y catalogos, resuelve emisor y punto, reserva el secuencial automatico en una transaccion corta, genera clave y XML, firma y llama al SRI fuera de esa transaccion, y abre otra transaccion para persistir el resultado. Si el DTO trae secuencial manual, esa rama no exige un punto activo; las ramas de persistencia si requieren `puntoEmisionInfo`, de modo que el flujo puede firmar y enviar sin crear el registro local. La politica esperada para ese caso se mantiene en `PFV-023`, y los huecos posteriores a una reserva automatica fallida en `PFV-033`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriRepositoryService.getNextSecuencial`
- Line / Section: `src/modules/sri/services/factura.service.ts:45-205`; `src/modules/sri/services/sri-repository.service.ts:199-219`
- Condition / Query / Statement: El codigo separa reserva, firma/envio y persistencia; la existencia del punto solo es obligatoria para secuencial automatico y condiciona ambas escrituras de resultado.
- Confidence: High

El DTO comprueba el patron textual de fecha y que detalles y pagos sean arreglos, mientras el servicio calcula netos y totales a partir de los valores recibidos. No se observa en ese camino una comprobacion calendaria ni una conciliacion cruzada entre pagos, bases, tarifas, impuestos e importe total; los minimos de contenido y esas consistencias permanecen en `PFV-019`, `PFV-020` y `PFV-021`.

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `CreateFacturaDto`; `FacturaService.emitirFactura`, `buildDetalles`, `calculateTotales`
- Line / Section: `src/modules/sri/dto/factura.dto.ts:39-80`; `src/modules/sri/services/factura.service.ts:69-74,612-710`
- Condition / Query / Statement: Las anotaciones validan formato y tipo estructural; la fecha se convierte por componentes y los totales se agregan sin comparaciones cruzadas adicionales.
- Confidence: High

La persistencia de Factura escribe el grafo relacional con un cliente transaccional, pero guarda los XML en filesystem dentro del callback. El rollback disponible revierte PostgreSQL y no contiene una operacion compensatoria sobre archivos; ademas, la llamada SRI ocurre antes de esa transaccion. La reconciliacion entre SRI, PostgreSQL y filesystem permanece en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`, `persistirFactura`; `XmlStorageService.saveAllXmls`, `saveXml`; `DatabaseService.transaction`
- Line / Section: `src/modules/sri/services/factura.service.ts:146-205,340-520`; `src/modules/sri/services/xml-storage.service.ts:53-130`; `src/database/database.service.ts:171-185`
- Condition / Query / Statement: El envio SRI precede a la persistencia; las filas usan una transaccion SQL y los XML se escriben sin una compensacion de filesystem en el rollback.
- Confidence: High

## Integraciones confirmadas

### Servicios SOAP del SRI

El modulo SRI crea clientes SOAP de recepcion y autorizacion para ambientes de pruebas y produccion. Existen mas de una fuente estatica o configurable para las URL WSDL; la fuente autoritativa se mantiene en `PFV-010`.

**Evidence**
- File: `src/modules/sri/services/sri-soap-factory.service.ts`; `src/config/configuration.ts`; `src/modules/sri/constants/sri-endpoints.constant.ts`; `src/modules/status/sri.health.ts`
- Function / Method / Procedure: `getRecepcionClient`, `getAutorizacionClient`; configuracion `sri.wsdl`; `SRI_ENDPOINTS`; `SriHealthIndicator.isHealthy`
- Line / Section: `src/modules/sri/services/sri-soap-factory.service.ts:12-67`; `src/config/configuration.ts:40-46`; `src/modules/sri/constants/sri-endpoints.constant.ts:4-23`; `src/modules/status/sri.health.ts:21-42`
- Condition / Query / Statement: La factoria usa URL internas, mientras configuracion y health check usan claves de entorno; no se presume cual fuente gobierna todos los caminos.
- Confidence: High

Para Factura, el cliente reintenta excepciones de recepcion, termina de inmediato ante `DEVUELTA` y consulta autorizacion hasta obtener `AUTORIZADO`, `NO AUTORIZADO` o agotar el bucle como `EN PROCESO`. Una excepcion durante autorizacion se propaga desde la primera llamada que falla; el alcance esperado de timeouts y retries se conserva en `PFV-034`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`, `autorizarComprobante`
- Line / Section: `src/modules/sri/services/sri-soap.client.ts:60-77,84-212`
- Condition / Query / Statement: Recepcion captura excepciones dentro de su bucle; autorizacion espera estados funcionales pero no captura por intento una excepcion.
- Confidence: High

### Servicio de renderizado de documentos

Los modulos de documentos y PDF llaman mediante HTTP a una API Carbone configurada externamente y cargan archivos de plantilla desde el filesystem.

**Evidence**
- File: `src/modules/document/document.service.ts`; `src/modules/pdf/pdf.service.ts`; `src/config/configuration.ts`
- Function / Method / Procedure: `DocumentService` constructor; `PdfService.generatePDF`; configuracion `carboneApi`
- Line / Section: `src/modules/document/document.service.ts:52-69,109-176`; `src/modules/pdf/pdf.service.ts:19-109`; `src/config/configuration.ts:11-12`
- Condition / Query / Statement: Ambos servicios obtienen `carboneApi` desde configuracion y realizan solicitudes HTTP con una plantilla.
- Confidence: High

### Webhooks salientes

El procesador de webhooks consume trabajos de BullMQ y realiza solicitudes HTTP POST firmadas hacia la URL configurada para cada trabajo.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `WebhookProcessor.process`
- Line / Section: `src/modules/webhooks/webhook.processor.ts:20-59`
- Condition / Query / Statement: El procesador de `webhook-dispatch` construye el cuerpo, calcula una firma HMAC y envia el evento por HTTP.
- Confidence: High

## Seguridad y acceso observados

La composicion registra guardas globales JWT, roles y rate limiting, junto con un interceptor global de auditoria. El inventario estructural confirma su presencia, pero los lotes 1-2 no documentan aun todas las reglas de autorizacion, excepciones publicas ni cobertura de auditoria.

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/auth.module.ts`
- Function / Method / Procedure: Proveedores globales de `AppModule`; `AuthModule`
- Line / Section: `src/app.module.ts:120-140`; `src/modules/auth/auth.module.ts:10-28`
- Condition / Query / Statement: `APP_GUARD` registra tres guardas y `APP_INTERCEPTOR` registra auditoria; `AuthModule` configura JWT y Passport.
- Confidence: High

Para las rutas SRI por RUC, la guarda busca una fila solo por RUC. Un usuario no
`SUPERADMIN` con tenant puede usar una fila cuyo `tenant_id` sea nulo. El DDL
solo garantiza unicidad por `(tenant_id, ruc)`, mientras emision, firma,
vinculacion y caches tambien se identifican por RUC. La politica de aislamiento
permanece en `PFV-036`; el detalle acotado se registra en
`10-security-access-control.md`.

**Evidence**
- File: `database/init.sql`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: constraint de `emisores`; `findByRuc`; `validateRucAccess`; `findEmisorByRuc`; `loadEmisorCertificate`; vinculacion P12
- Line / Section: dump 432-454, 1239-1243; emisores 227-266; repositorio 142-160; signer 285-385; certificate controller 255-287, 323-380
- Condition / Query / Statement: la restriccion incluye tenant, pero las consultas, caches y actualizacion citadas usan RUC; tenant nulo no dispara el rechazo.
- Confidence: High

Las rutas de administracion P12 usan JWT global, pero no declaran rol o alcance
tenant; solo la vinculacion con un RUC ejecuta la guarda del emisor. La politica
esperada permanece en `PFV-039`.

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/guards/roles.guard.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: guardas globales; `RolesGuard.canActivate`; endpoints de certificados
- Line / Section: app 120-135; roles guard 16-48; certificate controller 63-173, 190-287, 378-421
- Condition / Query / Statement: no hay roles locales; el control por RUC solo se invoca al vincular y las demas operaciones usan nombre de archivo.
- Confidence: High

## Supuestos de ejecucion confirmados

El artefacto Docker construye con Node 22 Alpine, genera `dist`, expone el puerto 3001 y arranca `dist/main`. Compose incorpora Redis y monta almacenamiento persistente. La guia `DEPLOYMENT.md` contiene una variante operativa cuya vigencia se valida mediante `PFV-017`.

**Evidence**
- File: `Dockerfile`; `docker-compose.prod.yml`; `DEPLOYMENT.md`
- Function / Method / Procedure: Etapas Docker `builder` y `production`; servicios Compose; secciones de despliegue
- Line / Section: `Dockerfile:8-64`; `docker-compose.prod.yml:20-103`; `DEPLOYMENT.md:16-60,91-157,321-387`
- Condition / Query / Statement: Docker y Compose describen el empaquetado actual; la guia contiene rutas y ejemplos que no se promueven a hechos operativos sin validacion.
- Confidence: Medium

## Brechas de evidencia y PFV relacionados

### PFV-001 - Contrato de emision de factura

La ruta mostrada en el flujo del README difiere de la ruta compuesta por el controlador y de la coleccion Postman. No se decide en este lote cual contrato externo debe preservarse.

**Evidence**
- File: `README.md`; `src/modules/sri/sri.controller.ts`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: Flujo introductorio; `SriController.emitirFactura`; solicitud Postman de factura
- Line / Section: `README.md:21-24`; `src/modules/sri/sri.controller.ts:61,84-103`; `Collection/Api_Facturacion_Sri.json:869-898`
- Condition / Query / Statement: Los artefactos publican dos formas distintas de la ruta de emision de factura.
- Confidence: High

### PFV-002 - Contrato del endpoint raiz

El controlador redirige `/` a `/status`, mientras la prueba E2E espera una respuesta 200 con otro cuerpo. El comportamiento contractual requiere validacion.

**Evidence**
- File: `src/modules/status/status.controller.ts`; `test/app.e2e-spec.ts`
- Function / Method / Procedure: `StatusController.root`; prueba `/(GET)`
- Line / Section: `src/modules/status/status.controller.ts:63-72`; `test/app.e2e-spec.ts:19-23`
- Condition / Query / Statement: Codigo y prueba definen resultados incompatibles para la misma entrada HTTP.
- Confidence: High

### PFV-003 - Documentacion rastreada ausente

Nueve documentos historicos bajo `docs/` estan rastreados por Git pero eliminados del worktree analizado. No se presume su contenido ni vigencia.

**Evidence**
- File: `docs/README.md`; `docs/api-sri.md`; `docs/base-datos.md`; `docs/catalogos.md`; `docs/configuracion.md`; `docs/guia-remision.md`; `docs/nota-credito.md`; `docs/nota-debito.md`; `docs/retenciones.md`
- Function / Method / Procedure: Estado del indice y worktree mediante `git status --short`
- Line / Section: Archivos completos no disponibles en el snapshot
- Condition / Query / Statement: Git marca las nueve rutas como eliminadas y no existe contenido legible para este lote.
- Confidence: High

### PFV-011 y PFV-018 - Bootstrap y datos iniciales

El dump y la guia manual no bastan para confirmar el procedimiento autoritativo de inicializacion ni si el usuario inicial sigue habilitado en un entorno real.

**Evidence**
- File: `database/init.sql`; `database/Install BD.txt`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: Dump PostgreSQL; pasos de instalacion `uuid-ossp`; descripcion de autenticacion de la coleccion
- Line / Section: `database/init.sql:1-7,35-47,1012-1015`; `database/Install BD.txt:1-23,30-44`; `Collection/Api_Facturacion_Sri.json:2-6`
- Condition / Query / Statement: Hay multiples instrucciones de preparacion y material de usuario inicial; sus valores sensibles se omiten y su estado operativo no se confirma.
- Confidence: Medium

## Cobertura pendiente

El nucleo comun usado por Factura y su flujo de emision ya tienen cobertura detallada en `05-business-rules.md`, `06-validation-rules.md`, `07-process-flows.md`, `10-security-access-control.md`, `11-reports-outputs.md` y `12-error-handling.md`; `15-sdd-migration-backlog.md` conserva los comportamientos ya trazables sin convertir los PFV en requisitos. Permanecen pendientes los otros tipos de comprobante SRI y los lotes profundos de los demas modulos, ademas de evidencia runtime. Hasta completar esa cobertura, este overview no debe usarse como especificacion funcional completa.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`; `src/modules/*`
- Function / Method / Procedure: Flujo y DTO de Factura; servicios de los otros comprobantes; inventario de modulos
- Line / Section: `src/modules/sri/services/factura.service.ts:42-723`; `src/modules/sri/dto/factura.dto.ts:20-127`; servicios restantes completos no analizados en profundidad; 101 archivos bajo `src/modules/`
- Condition / Query / Statement: El lote 2 profundizo Factura y servicios compartidos seleccionados; la presencia de los otros servicios y modulos no equivale a cobertura funcional de sus cuerpos.
- Confidence: High
