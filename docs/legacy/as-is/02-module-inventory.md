# 02 - Inventario de modulos AS-IS

## Alcance

Este documento conserva los IDs `MOD-001` a `MOD-020` asignados en el primer lote. Los limites se basan en modulos NestJS, paquetes estructurales y responsabilidades directamente visibles. El lote 2 agrego cobertura funcional profunda solo para los servicios SRI comunes seleccionados y Factura dentro de `MOD-012`; no se han extraido aun todas las reglas, validaciones ni alternativas de los otros comprobantes o modulos.

**Evidence**
- File: `src/app.module.ts`; `src/modules/*/*.module.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `AppModule`; decoradores `@Module`; `FacturaService.emitirFactura`; `CreateFacturaDto`; metodos compartidos de `SriBaseService`
- Line / Section: `src/app.module.ts:41-143`; declaraciones de modulo citadas por cada entrada; `src/modules/sri/services/factura.service.ts:42-723`; `src/modules/sri/dto/factura.dto.ts:20-127`; `src/modules/sri/services/sri-base.service.ts:23-197`
- Condition / Query / Statement: El mapa conserva sus limites estructurales y el analisis detallado agregado se circunscribe a Factura y colaboradores SRI compartidos, sin ejecucion runtime.
- Confidence: High

## Resumen

| ID | Modulo | Responsabilidad resumida | Entry points principales | Dependencias observadas |
|---|---|---|---|---|
| MOD-001 | Aplicacion | Bootstrap y composicion global | Proceso `main`, HTTP, Swagger | Todos los modulos |
| MOD-002 | Configuracion | Configuracion centralizada | Carga de entorno | `ConfigModule` |
| MOD-003 | Base de datos | Acceso PostgreSQL | API interna de datos | `pg`, configuracion |
| MOD-004 | Cache | Cache distribuida | API interna `CacheModule` | Redis |
| MOD-005 | Colas | Registro BullMQ | `sri-emision`, `webhook-dispatch` | Redis |
| MOD-006 | Cifrado | Cifrado y descifrado | API interna | Configuracion criptografica |
| MOD-007 | Auditoria | Registro e interceptor | Interceptor global | PostgreSQL |
| MOD-008 | Autenticacion | Sesion y usuarios | `/auth/*` | PostgreSQL, JWT, Passport |
| MOD-009 | Tenants | Gestion de tenants | `/tenants` | PostgreSQL |
| MOD-010 | Emisores | Gestion de emisores | `/emisores` | PostgreSQL, tenants logicos |
| MOD-011 | Puntos de emision | Puntos y secuenciales | `/emisores/puntos-emision`, `/emisores/secuenciales` | PostgreSQL, emisores |
| MOD-012 | SRI | Comprobantes, XML, firma y SOAP | `/sri`, `/catalogos` | Emisores, PostgreSQL, cache, BullMQ, SRI SOAP |
| MOD-013 | Webhooks | Configuracion y despacho | `/webhooks`, worker | PostgreSQL, emisores, BullMQ |
| MOD-014 | Certificados | Gestion de P12 | `/certificates` | Filesystem, emisores, SRI |
| MOD-015 | Plantillas | Gestion de plantillas | `/templates` | Filesystem |
| MOD-016 | Documentos | Generacion multi-formato | `/documents` | Plantillas, Carbone, filesystem |
| MOD-017 | PDF | Generacion y gestion PDF | `/generate-pdf` | Plantillas, Carbone, filesystem |
| MOD-018 | Firma PDF | Firma de documentos PDF | `/signature` | PDF, certificados, plantillas |
| MOD-019 | Imagenes | Gestion de imagenes | `/images` | Filesystem, configuracion |
| MOD-020 | Estado | Estado y health checks | `/`, `/status` | DB, Redis, memoria, SRI SOAP |

## Detalle por modulo

### MOD-001 - Aplicacion

Compone el proceso NestJS, los modulos funcionales y los componentes globales HTTP. Incluye Swagger, validacion, CORS, Helmet, filtro de excepciones, guardas, throttling, auditoria y archivos estaticos. El contrato de `/` permanece relacionado con `PFV-002` y la autoridad de la guia operativa con `PFV-017`.

**Evidence**
- File: `src/main.ts`; `src/app.module.ts`
- Function / Method / Procedure: `bootstrap`; `AppModule`
- Line / Section: `src/main.ts:21-150`; `src/app.module.ts:41-143`
- Condition / Query / Statement: El bootstrap registra el comportamiento HTTP y `AppModule` importa infraestructura y funcionalidades, ademas de proveedores globales.
- Confidence: High

### MOD-002 - Configuracion

Centraliza configuracion de servidor, Carbone, firma, SRI, JWT, CORS, cifrado, PostgreSQL, Redis, directorios, tenants, health checks, cache y colas. Distingue variables requeridas y opcionales mediante utilidades de entorno.

**Evidence**
- File: `src/config/configuration.ts`; `src/common/utils/env.utils.ts`; `src/app.module.ts`
- Function / Method / Procedure: Funcion de configuracion por defecto; `requireEnv`, `optionalEnv`, `resolveDir`; `ConfigModule.forRoot`
- Line / Section: `src/config/configuration.ts:1-153`; `src/common/utils/env.utils.ts:1-44`; `src/app.module.ts:45-49`
- Condition / Query / Statement: `ConfigModule` carga el objeto central, que obtiene valores requeridos y opcionales desde el entorno.
- Confidence: High

### MOD-003 - Base de datos

Expone globalmente un pool PostgreSQL y operaciones genericas de consulta, transaccion e insercion. El procedimiento de bootstrap y la condicion operativa del dato inicial no se presumen; se referencian `PFV-011` y `PFV-018`.

**Evidence**
- File: `src/database/database.module.ts`; `src/database/database.service.ts`; `database/init.sql`
- Function / Method / Procedure: `DatabaseModule`; `DatabaseService.onModuleInit`, `query`, `transaction`; dump PostgreSQL
- Line / Section: `src/database/database.module.ts:4-8`; `src/database/database.service.ts:12-45,95-210`; `database/init.sql:35-748,1012-1015`
- Condition / Query / Statement: El modulo exporta `DatabaseService`, que administra el pool y operaciones parametrizadas; el dump es evidencia separada, no un procedimiento confirmado de despliegue.
- Confidence: High

### MOD-004 - Cache

Registra una cache Redis global y selecciona una base Redis separada de la utilizada por BullMQ.

**Evidence**
- File: `src/common/cache/redis-cache.module.ts`
- Function / Method / Procedure: `RedisCacheModule`
- Line / Section: `src/common/cache/redis-cache.module.ts:12-38`
- Condition / Query / Statement: `CacheModule.registerAsync` crea el store Redis, configura TTL y usa DB 1.
- Confidence: High

### MOD-005 - Colas

Configura BullMQ globalmente y registra las colas `sri-emision` y
`webhook-dispatch`, con intentos, backoff y retencion parametrizables.
`SriModule` vuelve a registrar `sri-emision` sin esas opciones, por lo que su
configuracion efectiva permanece en `PFV-031`. El dato del job contiene el DTO
completo y su politica de acceso/retencion permanece en `PFV-038`.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`; `src/modules/sri/sri.service.ts`; `src/config/configuration.ts`
- Function / Method / Procedure: `QueueModule`; registro local de cola; `SriService.emitirFactura`; configuracion de colas
- Line / Section: QueueModule 11-74; SriModule 27-32; SriService 56-67; configuracion 138-145
- Condition / Query / Statement: la cola SRI aparece con dos registros y el job recibe `{ tipo, dto }`; las opciones globales declaran retencion por conteo.
- Confidence: High

### MOD-006 - Cifrado

Expone globalmente operaciones de cifrado y descifrado para texto. Los valores de claves y salt no se documentan.

**Evidence**
- File: `src/common/services/encryption.module.ts`; `src/common/services/encryption.service.ts`
- Function / Method / Procedure: `EncryptionModule`; `EncryptionService.encrypt`, `decrypt`
- Line / Section: `src/common/services/encryption.module.ts:4-8`; `src/common/services/encryption.service.ts:15-85`
- Condition / Query / Statement: El modulo exporta el servicio y este implementa las dos operaciones usando material criptografico obtenido de configuracion.
- Confidence: High

### MOD-007 - Auditoria

Registra eventos de auditoria en PostgreSQL y participa globalmente mediante un interceptor HTTP. El alcance funcional de las acciones auditadas requiere un lote posterior.

**Evidence**
- File: `src/common/services/audit.module.ts`; `src/common/services/audit.service.ts`; `src/common/interceptors/audit.interceptor.ts`; `src/app.module.ts`
- Function / Method / Procedure: `AuditModule`; `AuditService.log`, `search`; `AuditInterceptor.intercept`; proveedor `APP_INTERCEPTOR`
- Line / Section: `src/common/services/audit.module.ts:4-8`; `src/common/services/audit.service.ts:22-98`; `src/common/interceptors/audit.interceptor.ts:23-99`; `src/app.module.ts:136-140`
- Condition / Query / Statement: El servicio persiste y consulta auditoria, mientras el interceptor esta registrado para la aplicacion completa.
- Confidence: High

### MOD-008 - Autenticacion

Expone login, renovacion de token, registro de usuario, perfil y cambio de contrasena. Usa PostgreSQL, Passport y JWT; el detalle de reglas de credenciales y roles queda para el lote de seguridad.

**Evidence**
- File: `src/modules/auth/auth.module.ts`; `src/modules/auth/auth.controller.ts`; `src/modules/auth/auth.service.ts`
- Function / Method / Procedure: `AuthModule`; `AuthController`; `AuthService.login`, `refreshToken`, `register`, `changePassword`
- Line / Section: `src/modules/auth/auth.module.ts:10-28`; `src/modules/auth/auth.controller.ts:31-132`; `src/modules/auth/auth.service.ts:22-223`
- Condition / Query / Statement: El modulo configura JWT y Passport, y el controlador delega las cinco operaciones al servicio.
- Confidence: High

### MOD-009 - Tenants

Gestiona listado, consulta, creacion, actualizacion e inactivacion de tenants mediante PostgreSQL. El controlador limita estructuralmente sus operaciones al rol `SUPERADMIN`; las reglas completas se documentaran en seguridad.

**Evidence**
- File: `src/modules/tenants/tenants.module.ts`; `src/modules/tenants/tenants.controller.ts`; `src/modules/tenants/tenants.service.ts`
- Function / Method / Procedure: `TenantsModule`; `TenantsController`; metodos CRUD de `TenantsService`
- Line / Section: `src/modules/tenants/tenants.module.ts:6-11`; `src/modules/tenants/tenants.controller.ts:29-99`; `src/modules/tenants/tenants.service.ts:21-146`
- Condition / Query / Statement: El modulo importa base de datos y publica endpoints CRUD protegidos por decorador de rol.
- Confidence: High

### MOD-010 - Emisores

Gestiona emisores con listado, consulta, creacion, actualizacion e inactivacion.
Los listados/operaciones por ID aplican decisiones de alcance por tenant. La
guarda por RUC busca sin tenant y permite una fila con tenant nulo; el DDL solo
declara unicidad por tenant/RUC. Esta semantica permanece en `PFV-036`.

**Evidence**
- File: `src/modules/emisores/emisores.module.ts`; `src/modules/emisores/emisores.controller.ts`; `src/modules/emisores/emisores.service.ts`; `database/init.sql`
- Function / Method / Procedure: `EmisoresModule`; `EmisoresController`; CRUD; `findByRuc`; `validateRucAccess`; constraint de emisor
- Line / Section: modulo 6-11; controller 29-122; service 24-280; dump 432-454, 1239-1243
- Condition / Query / Statement: el modulo diferencia accesos por tenant, pero la ruta de validacion por RUC no recibe tenant y su condicion acepta una fila no asignada.
- Confidence: High

### MOD-011 - Puntos de emision

Gestiona puntos de emision y secuenciales asociados a un emisor. Publica dos controladores y valida acceso al emisor antes de delegar operaciones.

**Evidence**
- File: `src/modules/puntos-emision/puntos-emision.module.ts`; `src/modules/puntos-emision/puntos-emision.controller.ts`; `src/modules/puntos-emision/secuenciales.controller.ts`; `src/modules/puntos-emision/puntos-emision.service.ts`
- Function / Method / Procedure: `PuntosEmisionModule`; ambos controladores; metodos de puntos y secuenciales
- Line / Section: `src/modules/puntos-emision/puntos-emision.module.ts:8-13`; `src/modules/puntos-emision/puntos-emision.controller.ts:28-148`; `src/modules/puntos-emision/secuenciales.controller.ts:15-112`; `src/modules/puntos-emision/puntos-emision.service.ts:28-348`
- Condition / Query / Statement: El modulo importa base de datos y emisores, y expone rutas separadas para puntos y secuenciales.
- Confidence: High

### MOD-012 - SRI

Es el modulo de comprobantes electronicos. Agrupa controladores de SRI y catalogos, servicios por tipo de comprobante, generacion y firma XML, SOAP, persistencia, almacenamiento XML y procesamiento en cola. Los contratos de ruta y WSDL se mantienen vinculados a `PFV-001` y `PFV-010`.

**Evidence**
- File: `src/modules/sri/sri.module.ts`; `src/modules/sri/sri.controller.ts`; `src/modules/sri/catalogos.controller.ts`; `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `SriModule`; `SriController`; `CatalogosController`; `SriEmisionProcessor.process`
- Line / Section: `src/modules/sri/sri.module.ts:27-68`; `src/modules/sri/sri.controller.ts:61-591`; `src/modules/sri/catalogos.controller.ts:6-165`; `src/modules/sri/processors/sri-emision.processor.ts:10-48`
- Condition / Query / Statement: El modulo registra dos controladores, dieciseis servicios/proveedores funcionales y un worker para la cola de emision.
- Confidence: High

El lote 2 documento el despacho directo y asincrono de Factura, sus validaciones DTO y de catalogo, secuencial, clave de acceso, calculos, XML, firma, intercambio SOAP, persistencia, eventos, acceso acotado y alternativas de error. Las reglas confirmadas se mantienen en `05-business-rules.md`, las validaciones en `06-validation-rules.md`, los flujos en `07-process-flows.md`, el acceso en `10-security-access-control.md`, las salidas en `11-reports-outputs.md` y los errores en `12-error-handling.md`; `15-sdd-migration-backlog.md` prepara los comportamientos confirmados para una especificacion posterior. Las decisiones no confirmadas de este alcance se registran principalmente en `PFV-019` a `PFV-039`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; `CreateFacturaDto`; validaciones compartidas; `FacturaService.emitirFactura`; generacion de clave; `SriSoapClient.enviarYAutorizar`
- Line / Section: `src/modules/sri/sri.service.ts:52-80`; `src/modules/sri/processors/sri-emision.processor.ts:24-46`; `src/modules/sri/dto/factura.dto.ts:20-127`; `src/modules/sri/services/sri-base.service.ts:23-197`; `src/modules/sri/services/factura.service.ts:42-723`; `src/modules/sri/services/clave-acceso.service.ts:28-145`; `src/modules/sri/services/sri-soap.client.ts:84-212`
- Condition / Query / Statement: El flujo de Factura atraviesa esos proveedores desde el despacho hasta el resultado y fue el unico comprobante profundizado en este lote.
- Confidence: High

Nota de Credito, Nota de Debito, Retencion y Guia de Remision conservan cobertura de descubrimiento: se confirmaron sus clases, registro y puntos de delegacion, pero no se promueven sus cuerpos a reglas AS-IS detalladas. Nota de Credito queda seleccionada como siguiente lote, incluyendo el contraste de las estructuras de documento modificado compartidas con Nota de Debito.

**Evidence**
- File: `src/modules/sri/sri.module.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`
- Function / Method / Procedure: providers de `SriModule`; metodos delegadores de `SriService`; clases de servicio por comprobante
- Line / Section: `src/modules/sri/sri.module.ts:34-68`; `src/modules/sri/sri.service.ts:82-160`; servicios de los cuatro comprobantes completos solo delimitados
- Condition / Query / Statement: Las clases estan registradas y reciben delegacion, pero el lote 2 no extrajo exhaustivamente sus validaciones, calculos, persistencia ni alternativas.
- Confidence: High

### MOD-013 - Webhooks

Gestiona configuraciones, eventos y logs de webhooks, y procesa su entrega asincrona mediante BullMQ. Depende de PostgreSQL y de la validacion de acceso a emisores.

**Evidence**
- File: `src/modules/webhooks/webhooks.module.ts`; `src/modules/webhooks/webhooks.controller.ts`; `src/modules/webhooks/webhooks.service.ts`; `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `WebhooksModule`; `WebhooksController`; `WebhooksService.emit`; `WebhookProcessor.process`
- Line / Section: `src/modules/webhooks/webhooks.module.ts:8-14`; `src/modules/webhooks/webhooks.controller.ts:31-196`; `src/modules/webhooks/webhooks.service.ts:21-271`; `src/modules/webhooks/webhook.processor.ts:20-70`
- Condition / Query / Statement: El modulo publica CRUD y logs, y el worker consume `webhook-dispatch` para realizar entregas HTTP.
- Confidence: High

### MOD-014 - Certificados

Gestiona archivos de certificado P12: listado, carga, eliminacion, informacion y validacion. Usa almacenamiento local y colaboracion con emisores y SRI. En el alcance observado, esas rutas no declaran roles ni tenant y la vinculacion valida RUC solo cuando se solicita; la politica permanece en `PFV-039`.

**Evidence**
- File: `src/modules/certificate/certificate.module.ts`; `src/modules/certificate/certificate.controller.ts`; `src/modules/certificate/certificate.service.ts`; `src/app.module.ts`; `src/modules/auth/guards/roles.guard.ts`
- Function / Method / Procedure: `CertificateModule`; endpoints de `CertificateController`; servicio; guardas globales; `RolesGuard.canActivate`
- Line / Section: modulo 7-12; controller 47-443; service 62-260; app 120-135; roles guard 16-48
- Condition / Query / Statement: el modulo opera sobre archivos y emisor; el controller no declara roles/tenant, RolesGuard permite handlers sin metadata y el RUC se valida solo en la vinculacion del upload.
- Confidence: High

### MOD-015 - Plantillas

Gestiona plantillas almacenadas en filesystem: listado con metadatos, busqueda, carga y eliminacion.

**Evidence**
- File: `src/modules/template/template.module.ts`; `src/modules/template/template.controller.ts`; `src/modules/template/template.service.ts`
- Function / Method / Procedure: `TemplateModule`; `TemplateController`; `TemplateService.listTemplatesWithMetadata`, `findTemplate`, `deleteTemplate`
- Line / Section: `src/modules/template/template.module.ts:5-9`; `src/modules/template/template.controller.ts:30-144`; `src/modules/template/template.service.ts:20-226`
- Condition / Query / Statement: El servicio opera sobre `STORAGE_PATHS.templates` y el controlador expone listar, cargar y eliminar.
- Confidence: High

### MOD-016 - Documentos

Genera documentos en multiples formatos desde plantillas, con opciones de descarga o guardado. Delega renderizado a una API Carbone externa.

**Evidence**
- File: `src/modules/document/document.module.ts`; `src/modules/document/document.controller.ts`; `src/modules/document/document.service.ts`
- Function / Method / Procedure: `DocumentModule`; `generateDocumentAndDownload`, `generateDocumentAndSave`; `DocumentService`
- Line / Section: `src/modules/document/document.module.ts:6-11`; `src/modules/document/document.controller.ts:34-209`; `src/modules/document/document.service.ts:52-180`
- Condition / Query / Statement: El modulo importa plantillas; el servicio usa `carboneApi` y el controlador devuelve o persiste el resultado.
- Confidence: High

### MOD-017 - PDF

Genera PDF desde plantillas, permite agregar imagenes y gestiona archivos PDF almacenados por tipo.

**Evidence**
- File: `src/modules/pdf/pdf.module.ts`; `src/modules/pdf/pdf.controller.ts`; `src/modules/pdf/pdf.service.ts`; `src/modules/pdf/pdf-image.service.ts`
- Function / Method / Procedure: `PdfModule`; `PdfController`; `PdfService.generatePDF`, `generatePDFWithImages`; `PdfImageService.addImagesToPdf`
- Line / Section: `src/modules/pdf/pdf.module.ts:7-12`; `src/modules/pdf/pdf.controller.ts:49-483`; `src/modules/pdf/pdf.service.ts:19-145`; `src/modules/pdf/pdf-image.service.ts:18-133`
- Condition / Query / Statement: El modulo importa plantillas, registra dos servicios y expone generacion, listado, carga y eliminacion.
- Confidence: High

### MOD-018 - Firma PDF

Firma PDF existentes o generados, obtiene informacion del certificado y puede agregar una representacion visual con QR.

**Evidence**
- File: `src/modules/signature/signature.module.ts`; `src/modules/signature/signature.controller.ts`; `src/modules/signature/signature.service.ts`
- Function / Method / Procedure: `SignatureModule`; endpoints de `SignatureController`; `generateQR`, `extractCertificateInfo`, `addVisualSignature`, `signPDF`
- Line / Section: `src/modules/signature/signature.module.ts:8-13`; `src/modules/signature/signature.controller.ts:28-340`; `src/modules/signature/signature.service.ts:37-330`
- Condition / Query / Statement: El modulo importa PDF, certificados y plantillas, y el servicio implementa preparacion visual y firma criptografica del PDF.
- Confidence: High

### MOD-019 - Imagenes

Gestiona carga, listado, URL publica y eliminacion de imagenes almacenadas en el directorio de imagenes de PDF.

**Evidence**
- File: `src/modules/image/image.module.ts`; `src/modules/image/image.controller.ts`; `src/modules/image/image.service.ts`
- Function / Method / Procedure: `ImageModule`; `ImageController`; `ImageService.listImages`, `deleteImage`, `buildImageUrl`
- Line / Section: `src/modules/image/image.module.ts:5-9`; `src/modules/image/image.controller.ts:31-154`; `src/modules/image/image.service.ts:16-125`
- Condition / Query / Statement: El controlador publica carga, listado y eliminacion, mientras el servicio usa `STORAGE_PATHS.pdfsImages` y `publicUrl`.
- Confidence: High

### MOD-020 - Estado

Publica `/status` con verificaciones de PostgreSQL, Redis, memoria y conectividad SOAP SRI, y define el endpoint raiz. La contradiccion del contrato raiz se mantiene en `PFV-002`.

**Evidence**
- File: `src/modules/status/status.module.ts`; `src/modules/status/status.controller.ts`; `src/modules/status/database.health.ts`; `src/modules/status/redis.health.ts`; `src/modules/status/sri.health.ts`
- Function / Method / Procedure: `StatusModule`; `StatusController.getStatus`, `root`; indicadores de salud
- Line / Section: `src/modules/status/status.module.ts:12-21`; `src/modules/status/status.controller.ts:16-72`; `src/modules/status/database.health.ts:1-26`; `src/modules/status/redis.health.ts:1-48`; `src/modules/status/sri.health.ts:15-44`
- Condition / Query / Statement: El controlador combina los indicadores y redirige `/` a `/status`; la prueba E2E espera un resultado distinto.
- Confidence: High

## Modulos no confirmados

No se registran como modulos AS-IS el dashboard web, reportes, notificaciones por email, firma HSM, OAuth/OIDC, WebSockets u observabilidad OpenTelemetry descritos en versiones marcadas como propuesta. No existen imports equivalentes en `AppModule` dentro del snapshot.

**Evidence**
- File: `CHANGELOG.md`; `src/app.module.ts`
- Function / Method / Procedure: Secciones de versiones propuestas; imports de `AppModule`
- Line / Section: `CHANGELOG.md:9-36,40-46`; `src/app.module.ts:20-39,94-119`
- Condition / Query / Statement: El changelog etiqueta esas capacidades como planificadas o propuestas y el modulo raiz no importa implementaciones correspondientes.
- Confidence: High

## Cobertura pendiente por modulo

La presencia de un modulo no confirma por si sola todas sus reglas ni que cada camino se encuentre operativo en produccion. En `MOD-012`, la cobertura profunda actual corresponde al nucleo compartido seleccionado y Factura; los otros cuatro comprobantes siguen pendientes, comenzando por Nota de Credito. Los siguientes lotes tambien deben analizar servicios, DTO, consultas, errores, eventos y pruebas de los demas modulos, preservando estos IDs.

**Evidence**
- File: `src/modules/`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`
- Function / Method / Procedure: Inventario de controladores, servicios, DTO, procesadores y pruebas; servicios por comprobante
- Line / Section: 101 archivos bajo los trece directorios funcionales; `src/modules/sri/services/factura.service.ts:42-723`; servicios restantes completos solo delimitados
- Condition / Query / Statement: Factura recibio analisis funcional detallado; la existencia estructural de los otros servicios y modulos no se trata como cobertura de su logica interna.
- Confidence: High
