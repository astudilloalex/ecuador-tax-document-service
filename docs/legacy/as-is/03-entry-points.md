# 03 - Puntos de entrada

## Alcance del lote

Este documento inventaria los puntos que inician comportamiento en el sistema
actual: arranque del proceso NestJS, superficies HTTP, archivos estaticos,
consumidores BullMQ, listeners de eventos y hooks del ciclo de vida. El analisis
es estatico y acotado a los artefactos citados; no se ejecuto la aplicacion ni se
validaron dependencias externas.

La cobertura de este lote incluye:

- 1 bootstrap de aplicacion.
- 1 montaje de Swagger.
- 5 raices de archivos estaticos.
- 79 rutas declaradas en controllers, incluidas `/` y `/status`.
- 2 consumidores BullMQ.
- 2 listeners de eventos internos.
- 4 hooks de inicio o cierre.

Los IDs `EP-001` a `EP-094` son estables para este inventario.

## Convenciones de transporte

- Las rutas se muestran tal como se componen desde `@Controller()` y el
  decorador HTTP del metodo. En el bootstrap analizado no se configura un
  prefijo HTTP global.
- Los controllers estan sujetos por defecto a los guards globales JWT, roles y
  rate limiting. `POST /auth/login`, `POST /auth/refresh`, `GET /` y
  `GET /status` estan marcados como publicos.
- La validacion global transforma entradas, descarta propiedades no declaradas
  y rechaza propiedades no permitidas.
- Los valores de credenciales, tokens, certificados y secretos de firma no se
  reproducen en este documento.

**Evidence**
- File: `src/main.ts`; `src/app.module.ts`; `src/modules/auth/auth.controller.ts`; `src/modules/status/status.controller.ts`
- Function / Method / Procedure: `bootstrap`; providers `APP_GUARD`; `AuthController.login`; `AuthController.refresh`; `StatusController`
- Line / Section: `src/main.ts:21-63`; `src/app.module.ts:120-140`; `src/modules/auth/auth.controller.ts:42-63`; `src/modules/status/status.controller.ts:15-17`
- Condition / Query / Statement: `ValidationPipe` se registra globalmente; `JwtAuthGuard`, `RolesGuard` y `ThrottlerGuard` se registran como guards globales; `@Public()` exceptua las cuatro rutas controller indicadas.
- Confidence: High

## Arranque, documentacion y archivos estaticos

| ID | Type | Name | Trigger | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|---|---|
| EP-001 | Runtime | Bootstrap NestJS | Ejecucion de `bootstrap()` mediante scripts npm o `node dist/main` | Variables de configuracion requeridas por `ConfigModule` | Aplicacion Nest escuchando en el puerto configurado; hooks de cierre habilitados | `AppModule` | PFV-008 |
| EP-002 | HTTP auxiliar | Swagger UI | `GET /api` | Peticion HTTP | Interfaz Swagger generada desde controllers y DTOs | Bootstrap | - |
| EP-003 | HTTP estatico | PDFs firmados | `GET /pdfs/con_firma/*` | Ruta relativa de archivo | Contenido del archivo bajo el directorio de PDFs firmados | `AppModule` | PFV-009 |
| EP-004 | HTTP estatico | Otros PDFs | `GET /pdfs/others/*` | Ruta relativa de archivo | Contenido del archivo bajo el directorio `others` | `AppModule` | PFV-009 |
| EP-005 | HTTP estatico | Documentos generados | `GET /pdfs/documents/*` | Ruta relativa de archivo | Contenido del archivo bajo el directorio `documents` | `AppModule` | PFV-009 |
| EP-006 | HTTP estatico | Raiz de PDFs | `GET /pdfs/*` | Ruta relativa de archivo | Contenido del archivo bajo el directorio raiz de PDFs | `AppModule` | PFV-009 |
| EP-007 | HTTP estatico | Imagenes | `GET /images/*` | Ruta relativa de archivo | Contenido del subdirectorio de imagenes | `AppModule` | PFV-009 |

**Evidence**
- File: `src/main.ts`; `src/app.module.ts`; `package.json`; `Dockerfile`
- Function / Method / Procedure: `bootstrap`; `SwaggerModule.setup`; `ServeStaticModule.forRootAsync`; scripts `start`, `start:dev`, `start:prod`; comando de contenedor
- Line / Section: `src/main.ts:21-23,121-150`; `src/app.module.ts:63-92`; `package.json:23-30`; `Dockerfile:56-64`
- Condition / Query / Statement: Nest crea `AppModule`, monta Swagger en `api`, monta cinco `serveRoot`, habilita shutdown hooks y escucha el puerto obtenido de configuracion.
- Confidence: High

## Status y raiz HTTP

| ID | Method and path | Trigger / access | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|---|
| EP-008 | `GET /` | HTTP publico | Ninguno | Redireccion HTTP 302 a `/status` | `StatusModule` | PFV-002 |
| EP-009 | `GET /status` | HTTP publico; tambien usado por health checks de contenedor | Ninguno | Estado base de directorios, conteos y templates, mas checks de base de datos, Redis, heap, RSS y conectividad SRI | `StatusModule` | PFV-008 |

El resultado de `EP-009` incorpora metadatos producidos por los indicadores de
salud. El indicador SRI agrega la URL WSDL configurada al resultado cuando la
dependencia es alcanzable; el valor concreto no se reproduce aqui.

**Evidence**
- File: `src/modules/status/status.controller.ts`; `src/modules/status/status.service.ts`; `src/modules/status/sri.health.ts`; `src/modules/status/status.module.ts`; `Dockerfile`; `docker-compose.yml`; `docker-compose.prod.yml`
- Function / Method / Procedure: `StatusController.root`; `StatusController.getStatus`; `StatusService.getStatus`; `SriHealthIndicator.isHealthy`
- Line / Section: `src/modules/status/status.controller.ts:29-72`; `src/modules/status/status.service.ts:28-70`; `src/modules/status/sri.health.ts:21-41`; `src/modules/status/status.module.ts:12-20`; `Dockerfile:59-61`; `docker-compose.yml:68-81`; `docker-compose.prod.yml:79-93`
- Condition / Query / Statement: `/` usa `@Redirect('/status', 302)`; `/status` ejecuta cinco checks y combina su resultado con el estado de almacenamiento; los health checks de contenedor consultan `/status`.
- Confidence: High

## Autenticacion

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-010 | `POST /auth/login` | Body `LoginDto` | `AuthResponseDto` | `AuthModule` | - |
| EP-011 | `POST /auth/refresh` | Body `RefreshTokenDto` | `AuthResponseDto` | `AuthModule` | - |
| EP-012 | `POST /auth/register` | Body `RegisterUserDto`; usuario con rol requerido | Identificador, email, rol y tenant del usuario creado | `AuthModule` | - |
| EP-013 | `GET /auth/me` | Usuario JWT actual | Perfil con id, email, rol y tenant | `AuthModule` | - |
| EP-014 | `PATCH /auth/change-password` | Usuario JWT actual; body `ChangePasswordDto` | Mensaje de confirmacion | `AuthModule` | - |

**Evidence**
- File: `src/modules/auth/auth.controller.ts`; `src/modules/auth/auth.service.ts`; `src/modules/auth/auth.module.ts`
- Function / Method / Procedure: `AuthController.login`; `refresh`; `register`; `getProfile`; `changePassword`; `AuthService.register`
- Line / Section: `src/modules/auth/auth.controller.ts:38-55,57-73,75-91,93-109,111-133`; `src/modules/auth/auth.service.ts:136-141`; `src/modules/auth/auth.module.ts:25-27`
- Condition / Query / Statement: Los decoradores definen cinco rutas; login y refresh usan `@Public()`; las firmas de metodo y retornos declaran las entradas y salidas inventariadas.
- Confidence: High

## Tenants

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-015 | `GET /tenants` | Query `QueryTenantsDto` | `PaginatedTenantsResponseDto` | `TenantsModule` | - |
| EP-016 | `GET /tenants/:id` | Path `id` | `TenantResponseDto` | `TenantsModule` | - |
| EP-017 | `POST /tenants` | Body `CreateTenantDto` | `TenantResponseDto` | `TenantsModule` | - |
| EP-018 | `PUT /tenants/:id` | Path `id`; body `UpdateTenantDto` | `TenantResponseDto` | `TenantsModule` | - |
| EP-019 | `DELETE /tenants/:id` | Path `id` | `TenantResponseDto` | `TenantsModule` | - |

**Evidence**
- File: `src/modules/tenants/tenants.controller.ts`; `src/modules/tenants/tenants.module.ts`
- Function / Method / Procedure: `TenantsController.findAll`; `findOne`; `create`; `update`; `delete`
- Line / Section: `src/modules/tenants/tenants.controller.ts:29-99`; `src/modules/tenants/tenants.module.ts:6-10`
- Condition / Query / Statement: `@Controller('tenants')` combina cinco decoradores HTTP con DTOs y tipos de respuesta explicitos; el controller completo requiere rol `SUPERADMIN`.
- Confidence: High

## Emisores

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-020 | `GET /emisores` | Query `QueryEmisoresDto`; usuario JWT | `PaginatedEmisoresResponseDto` | `EmisoresModule` | - |
| EP-021 | `GET /emisores/:id` | Path `id`; usuario JWT | `EmisorResponseDto` | `EmisoresModule` | - |
| EP-022 | `POST /emisores` | Body `CreateEmisorDto`; usuario JWT | `EmisorResponseDto` | `EmisoresModule` | - |
| EP-023 | `PUT /emisores/:id` | Path `id`; body `UpdateEmisorDto`; usuario JWT | `EmisorResponseDto` | `EmisoresModule` | - |
| EP-024 | `DELETE /emisores/:id` | Path `id`; usuario JWT | `EmisorResponseDto` | `EmisoresModule` | - |

**Evidence**
- File: `src/modules/emisores/emisores.controller.ts`; `src/modules/emisores/emisores.module.ts`
- Function / Method / Procedure: `EmisoresController.findAll`; `findOne`; `create`; `update`; `delete`
- Line / Section: `src/modules/emisores/emisores.controller.ts:29-122`; `src/modules/emisores/emisores.module.ts:6-10`
- Condition / Query / Statement: `@Controller('emisores')` define una ruta de coleccion y rutas dinamicas `:id`; los handlers delegan a `EmisoresService` y declaran los DTOs de respuesta.
- Confidence: High

## Puntos de emision y secuenciales

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-025 | `GET /emisores/puntos-emision/:emisorId` | Path `emisorId`; usuario JWT | `PuntoEmisionResponseDto[]` | `PuntosEmisionModule` | - |
| EP-026 | `GET /emisores/puntos-emision/:emisorId/:puntoEmisionId` | Dos path params; usuario JWT | `PuntoEmisionResponseDto` | `PuntosEmisionModule` | - |
| EP-027 | `POST /emisores/puntos-emision/:emisorId` | Path `emisorId`; body `CreatePuntoEmisionDto`; usuario JWT | `PuntoEmisionResponseDto` | `PuntosEmisionModule` | - |
| EP-028 | `PUT /emisores/puntos-emision/:emisorId/:puntoEmisionId` | Dos path params; body `UpdatePuntoEmisionDto`; usuario JWT | `PuntoEmisionResponseDto` | `PuntosEmisionModule` | - |
| EP-029 | `DELETE /emisores/puntos-emision/:emisorId/:puntoEmisionId` | Dos path params; usuario JWT | `PuntoEmisionResponseDto` | `PuntosEmisionModule` | - |
| EP-030 | `GET /emisores/secuenciales/:emisorId` | Path `emisorId`; usuario JWT | Secuenciales con establecimiento y punto de emision | `PuntosEmisionModule` | - |
| EP-031 | `GET /emisores/secuenciales/:emisorId/:puntoEmisionId` | Dos path params; usuario JWT | `SecuencialResponseDto[]` | `PuntosEmisionModule` | - |
| EP-032 | `PATCH /emisores/secuenciales/:emisorId/:puntoEmisionId/:tipoComprobante` | Tres path params; body `UpdateSecuencialDto`; usuario JWT | `SecuencialResponseDto` | `PuntosEmisionModule` | - |

**Evidence**
- File: `src/modules/puntos-emision/puntos-emision.controller.ts`; `src/modules/puntos-emision/secuenciales.controller.ts`; `src/modules/puntos-emision/puntos-emision.module.ts`; `src/modules/emisores/emisores.controller.ts`
- Function / Method / Procedure: `PuntosEmisionController.findAll`; `findOne`; `create`; `update`; `delete`; `SecuencialesController.getAllByEmisor`; `getSecuenciales`; `updateSecuencial`
- Line / Section: `src/modules/puntos-emision/puntos-emision.controller.ts:28-148`; `src/modules/puntos-emision/secuenciales.controller.ts:15-111`; `src/modules/puntos-emision/puntos-emision.module.ts:8-12`; `src/modules/emisores/emisores.controller.ts:31,56-68`
- Condition / Query / Statement: Dos controllers comparten el prefijo literal `/emisores` con `EmisoresController`; sus decoradores y firmas declaran ocho rutas y validan acceso al emisor antes de delegar.
- Confidence: High

## Catalogos SRI

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-033 | `GET /catalogos/impuestos` | Sin parametros de handler | Lista `impuestos` | `SriModule` | - |
| EP-034 | `GET /catalogos/retenciones` | Sin parametros de handler | Lista `retenciones` | `SriModule` | - |
| EP-035 | `GET /catalogos/formas-pago` | Sin parametros de handler | Lista `formasPago` | `SriModule` | - |
| EP-036 | `GET /catalogos/tipos-identificacion` | Sin parametros de handler | Lista `tiposIdentificacion` | `SriModule` | - |
| EP-037 | `GET /catalogos/documentos-sustento` | Sin parametros de handler | Lista `documentosSustento` | `SriModule` | - |
| EP-038 | `GET /catalogos/motivos-traslado` | Sin parametros de handler | Lista `motivosTraslado` | `SriModule` | - |

**Evidence**
- File: `src/modules/sri/catalogos.controller.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `CatalogosController.listarImpuestos`; `listarRetenciones`; `listarFormasPago`; `listarTiposIdentificacion`; `listarDocumentosSustento`; `listarMotivosTraslado`
- Line / Section: `src/modules/sri/catalogos.controller.ts:6-27,60-72,96-107,115-126,135-146,154-165`; `src/modules/sri/sri.module.ts:27-34`
- Condition / Query / Statement: Seis metodos `GET` consultan `CatalogoValidatorService` y retornan objetos con la coleccion nominal indicada.
- Confidence: High

## Emision SRI

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-039 | `POST /sri/emitir/factura` | Body `CreateFacturaDto`; usuario JWT | `EmisionEncoladaResponseDto` o `FacturaResponseDto` | `SriModule` | PFV-001, PFV-005, PFV-007 |
| EP-040 | `POST /sri/emitir/nota-credito` | Body `CreateNotaCreditoDto`; usuario JWT | `EmisionEncoladaResponseDto` o `NotaCreditoResponseDto` | `SriModule` | PFV-005, PFV-007 |
| EP-041 | `POST /sri/emitir/nota-debito` | Body `CreateNotaDebitoDto`; usuario JWT | `EmisionEncoladaResponseDto` o `NotaDebitoResponseDto` | `SriModule` | PFV-005, PFV-007 |
| EP-042 | `POST /sri/emitir/retencion` | Body `CreateRetencionDto`; usuario JWT | `EmisionEncoladaResponseDto` o `RetencionResponseDto` | `SriModule` | PFV-005, PFV-007 |
| EP-043 | `POST /sri/emitir/guia-remision` | Body `CreateGuiaRemisionDto`; usuario JWT | `EmisionEncoladaResponseDto` o `GuiaRemisionResponseDto` | `SriModule` | PFV-005, PFV-007 |
| EP-044 | `GET /sri/autorizar/:claveAcceso` | Path `claveAcceso`; usuario JWT | `FacturaResponseDto` | `SriModule` | - |
| EP-045 | `POST /sri/preview/factura` | Body `CreateFacturaDto`; usuario JWT | Objeto con XML sin enviar | `SriModule` | - |
| EP-046 | `POST /sri/validar` | Multipart, campo `file` | `{ valido, errores }` | `SriModule` | - |
| EP-047 | `POST /sri/debug/factura-firmada` | Body `CreateFacturaDto`; usuario JWT | Clave de acceso, XML sin firma y XML firmado | `SriModule` | - |

Los cinco endpoints `emitir` seleccionan ejecucion asincrona salvo que la
configuracion de emision asincrona sea exactamente `false`. En modo asincrono
se encola un job y se devuelve su identificador; el mecanismo de consulta del
resultado se centraliza en `PFV-005`. Los handlers fijan codigo `201`, mientras
Swagger declara tambien una respuesta sincrona `200`; la definicion contractual
se centraliza en `PFV-007`.

Para la factura, el README publica `/sri/factura/emitir`, mientras el codigo y
la coleccion Postman usan `/sri/emitir/factura`; la ruta que debe preservarse se
centraliza en `PFV-001`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/sri.module.ts`; `README.md`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: `SriController.emitirFactura`; `emitirNotaCredito`; `emitirNotaDebito`; `emitirRetencion`; `emitirGuiaRemision`; `consultarAutorizacion`; `previewFactura`; `validarXml`; `debugFacturaFirmada`; metodos de emision homonimos de `SriService`; ejemplos de factura
- Line / Section: `src/modules/sri/sri.controller.ts:84-218,221-327`; `src/modules/sri/sri.service.ts:56-67,86-99,106-119,126-139,146-159`; `src/modules/sri/sri.module.ts:27-52`; `README.md:22,174,262,299`; `Collection/Api_Facturacion_Sri.json:869-898`
- Condition / Query / Statement: Cada handler valida acceso al emisor antes de delegar; `SriService` bifurca entre llamada directa y cola; README y codigo/coleccion publican dos rutas diferentes para factura.
- Confidence: High

## Consulta y sincronizacion SRI

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-048 | `GET /sri/comprobantes` | Query `QueryComprobantesDto`; usuario JWT | `PaginatedComprobantesDto` | `SriModule` | - |
| EP-049 | `GET /sri/comprobantes/:claveAcceso` | Path `claveAcceso`; usuario JWT | `ComprobanteDetalladoDto` | `SriModule` | - |
| EP-050 | `GET /sri/comprobantes/:claveAcceso/xml` | Path `claveAcceso`; usuario JWT | Descarga `application/xml` | `SriModule` | - |
| EP-051 | `PATCH /sri/comprobantes/:claveAcceso/anular` | Path `claveAcceso`; usuario JWT | Mensaje, clave y estado anterior | `SriModule` | - |
| EP-052 | `POST /sri/comprobantes/:claveAcceso/reintentar` | Path `claveAcceso`; usuario JWT | Estado y resultado del reintento | `SriModule` | - |
| EP-053 | `GET /sri/verificar/:claveAcceso` | Path `claveAcceso`; usuario JWT | Comparacion de estado SRI/local | `SriModule` | - |
| EP-054 | `POST /sri/sincronizar` | Body con estados, reintento, limite y RUC opcionales; usuario JWT | Contadores y detalle por comprobante | `SriModule` | - |

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `SriController.listarComprobantes`; `obtenerComprobante`; `descargarXml`; `anularComprobante`; `reintentarComprobante`; `verificarEnSri`; `sincronizar`
- Line / Section: `src/modules/sri/sri.controller.ts:330-591`; `src/modules/sri/sri.module.ts:27-52`
- Condition / Query / Statement: Siete decoradores HTTP exponen consulta, descarga, anulacion, reintento, verificacion y sincronizacion; las firmas declaran sus parametros y estructuras de retorno.
- Confidence: High

## Certificados

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-055 | `GET /certificates/list-certs` | Query opcional `page`, `limit` | Lista de certificados, total y paginacion | `CertificateModule` | - |
| EP-056 | `DELETE /certificates/delete-cert/:fileName` | Path `fileName` | Confirmacion y emisores desvinculados | `CertificateModule` | - |
| EP-057 | `POST /certificates/upload-cert` | Multipart `cert`; body `UploadCertificateDto`; usuario JWT | Resultado de carga, validacion y vinculacion opcional | `CertificateModule` | - |
| EP-058 | `GET /certificates/cert-info/:fileName` | Path `fileName` | Informacion del certificado | `CertificateModule` | - |
| EP-059 | `POST /certificates/validate-cert/:fileName` | Path `fileName`; body `ValidateCertificateDto` | Resultado de validacion | `CertificateModule` | - |

Los DTOs y resultados pueden involucrar contrasenas o material criptografico;
este inventario solo registra la existencia de esos campos y no sus valores.

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/certificate/certificate.module.ts`
- Function / Method / Procedure: `CertificateController.listCertificates`; `deleteCertificate`; `uploadCertificate`; `getCertificateInfo`; `validateCertificate`
- Line / Section: `src/modules/certificate/certificate.controller.ts:45-143,146-305,374-443`; `src/modules/certificate/certificate.module.ts:7-11`
- Condition / Query / Statement: Cinco decoradores HTTP cubren listado, eliminacion, upload, inspeccion y validacion; el upload usa `FileInterceptor('cert')` y sanitiza el nombre antes de validar.
- Confidence: High

## Templates

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-060 | `GET /templates` | Sin parametros de handler | Lista de templates y resumen | `TemplateModule` | - |
| EP-061 | `POST /templates/upload` | Multipart `template` | Metadatos del template cargado | `TemplateModule` | - |
| EP-062 | `DELETE /templates/:id` | Path `id` | Metadatos y confirmacion de eliminacion | `TemplateModule` | - |

**Evidence**
- File: `src/modules/template/template.controller.ts`; `src/modules/template/template.module.ts`
- Function / Method / Procedure: `TemplateController.listTemplates`; `uploadTemplate`; `deleteTemplate`
- Line / Section: `src/modules/template/template.controller.ts:29-170`; `src/modules/template/template.module.ts:5-8`
- Condition / Query / Statement: El controller declara tres rutas; el upload recibe `FileInterceptor('template')` y el delete usa un `id` de path.
- Confidence: High

## Documentos multiformato

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-063 | `GET /documents/formats` | Sin parametros de handler | Formatos soportados y tipos MIME | `DocumentModule` | - |
| EP-064 | `POST /documents/download/:templateId` | Path `templateId`; body JSON; formato por header, query o body | Buffer con MIME y disposicion de descarga | `DocumentModule` | - |
| EP-065 | `POST /documents/save/:templateId` | Path `templateId`; body JSON; formato por header, query o body | Metadatos, formato y URL del archivo guardado | `DocumentModule` | - |

**Evidence**
- File: `src/modules/document/document.controller.ts`; `src/modules/document/document.module.ts`
- Function / Method / Procedure: `DocumentController.getOutputFormat`; `getSupportedFormats`; `generateDocumentAndDownload`; `generateDocumentAndSave`
- Line / Section: `src/modules/document/document.controller.ts:33-80,82-142,144-224`; `src/modules/document/document.module.ts:6-10`
- Condition / Query / Statement: El formato se resuelve con prioridad header `X-Output-Format`, query `format`, body `format`; una ruta devuelve el buffer y la otra persiste el resultado.
- Confidence: High

## PDFs

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-066 | `POST /generate-pdf/download/:templateId` | Path `templateId`; body `GeneratePdfDto` o JSON | PDF descargable | `PdfModule` | - |
| EP-067 | `POST /generate-pdf/save/:templateId` | Path `templateId`; body `GeneratePdfDto` o JSON | Metadatos y URL del PDF guardado | `PdfModule` | - |
| EP-068 | `POST /generate-pdf/with-images/download/:templateId` | Path `templateId`; body `GeneratePdfWithImagesDto` | PDF descargable con imagenes | `PdfModule` | - |
| EP-069 | `POST /generate-pdf/with-images/save/:templateId` | Path `templateId`; body `GeneratePdfWithImagesDto` | Metadatos y URL del PDF guardado | `PdfModule` | - |
| EP-070 | `GET /generate-pdf/list/:type` | Path `type`; query opcional `page`, `limit` | Lista de archivos, total y paginacion opcional | `PdfModule` | - |
| EP-071 | `POST /generate-pdf/upload/:type` | Path `type`; multipart `pdf` | Metadatos y URL del PDF cargado | `PdfModule` | - |
| EP-072 | `DELETE /generate-pdf/:type/:fileName` | Paths `type`, `fileName` | Confirmacion de eliminacion | `PdfModule` | - |

**Evidence**
- File: `src/modules/pdf/pdf.controller.ts`; `src/modules/pdf/pdf.module.ts`
- Function / Method / Procedure: `PdfController.generatePdfAndDownload`; `generatePdfAndSave`; `generatePdfWithImagesAndDownload`; `generatePdfWithImagesAndSave`; `listPdfs`; `uploadPdf`; `deletePdf`
- Line / Section: `src/modules/pdf/pdf.controller.ts:48-247,250-350,353-483`; `src/modules/pdf/pdf.module.ts:7-11`
- Condition / Query / Statement: Siete decoradores HTTP exponen generacion, persistencia, listado, upload y delete; las rutas de descarga escriben `application/pdf` en la respuesta.
- Confidence: High

## Firma de PDFs

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-073 | `POST /signature/sign-pdf/:fileName` | Path `fileName`; body `SignPdfDto` | Metadatos y URL del PDF firmado | `SignatureModule` | - |
| EP-074 | `POST /signature/generate-sign-pdf/:templateId` | Path `templateId`; body `GenerateAndSignPdfDto` | Metadatos y URL del PDF generado y firmado | `SignatureModule` | - |
| EP-075 | `POST /signature/generate-sign-pdf/download/:templateId` | Path `templateId`; body `GenerateAndSignPdfDto` | PDF firmado descargable | `SignatureModule` | - |

Los bodies incluyen referencias a certificado y credencial de apertura. Se
documentan como entradas de transporte sin reproducir valores sensibles.

**Evidence**
- File: `src/modules/signature/signature.controller.ts`; `src/modules/signature/signature.module.ts`
- Function / Method / Procedure: `SignatureController.signExistingPdf`; `generateAndSignPdf`; `generateAndSignPdfDownload`
- Line / Section: `src/modules/signature/signature.controller.ts:27-150,153-256,259-340`; `src/modules/signature/signature.module.ts:8-12`
- Condition / Query / Statement: Tres rutas validan certificado, generan o leen PDF, invocan `SignatureService.signPDF` y retornan metadatos o un buffer de descarga.
- Confidence: High

## Imagenes

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-076 | `POST /images/upload` | Multipart `image` | Metadatos y URL de imagen | `ImageModule` | PFV-009 |
| EP-077 | `GET /images/list` | Query opcional `page`, `limit` | Lista, total y paginacion | `ImageModule` | PFV-009 |
| EP-078 | `DELETE /images/:fileName` | Path `fileName` | Confirmacion de eliminacion | `ImageModule` | PFV-009 |

El prefijo del controller coincide con la raiz estatica de `EP-007`. La
exposicion y precedencia efectiva quedan centralizadas en `PFV-009`.

**Evidence**
- File: `src/modules/image/image.controller.ts`; `src/modules/image/image.module.ts`; `src/app.module.ts`
- Function / Method / Procedure: `ImageController.uploadImage`; `listImages`; `deleteImage`; `ServeStaticModule.forRootAsync`
- Line / Section: `src/modules/image/image.controller.ts:30-154`; `src/modules/image/image.module.ts:5-8`; `src/app.module.ts:63-89`
- Condition / Query / Statement: `@Controller('images')` declara tres rutas y `ServeStaticModule` monta simultaneamente `serveRoot: '/images'`.
- Confidence: High

## Webhooks HTTP

| ID | Method and path | Inputs | Outputs | Module | Related PFV |
|---|---|---|---|---|---|
| EP-079 | `GET /webhooks/eventos` | Sin parametros de handler | Eventos configurables y descripciones | `WebhooksModule` | PFV-004 |
| EP-080 | `GET /webhooks` | Query opcional `emisorId`; usuario JWT | `WebhookResponseDto[]` | `WebhooksModule` | - |
| EP-081 | `GET /webhooks/:id` | Path `id` | `WebhookResponseDto` | `WebhooksModule` | - |
| EP-082 | `POST /webhooks` | Body `CreateWebhookDto`; usuario JWT | `WebhookResponseDto` | `WebhooksModule` | PFV-004 |
| EP-083 | `PUT /webhooks/:id` | Path `id`; body `UpdateWebhookDto` | `WebhookResponseDto` | `WebhooksModule` | PFV-004 |
| EP-084 | `DELETE /webhooks/:id` | Path `id` | `WebhookResponseDto` | `WebhooksModule` | - |
| EP-085 | `POST /webhooks/:id/regenerar-secreto` | Path `id` | `WebhookResponseDto`; valor sensible no reproducido | `WebhooksModule` | - |
| EP-086 | `GET /webhooks/:id/logs` | Path `id`; query opcional `page`, `limit` | Logs, total, pagina y total de paginas | `WebhooksModule` | - |

El catalogo de `EP-079` permite siete nombres de evento, mientras el alcance de
listeners encontrado en este lote cubre dos. La cobertura funcional completa se
centraliza en `PFV-004`.

**Evidence**
- File: `src/modules/webhooks/webhooks.controller.ts`; `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.module.ts`
- Function / Method / Procedure: `WebhooksController.getEventos`; `findAll`; `findOne`; `create`; `update`; `delete`; `regenerateSecret`; `getLogs`; constante `WEBHOOK_EVENTS`
- Line / Section: `src/modules/webhooks/webhooks.controller.ts:31-196`; `src/modules/webhooks/dto/webhook.dto.ts:14-24`; `src/modules/webhooks/webhooks.module.ts:8-13`
- Condition / Query / Statement: Ocho rutas cubren catalogo, CRUD, rotacion de credencial y logs; `WEBHOOK_EVENTS` enumera siete eventos aceptados por los DTOs.
- Confidence: High

## Consumidores de cola

| ID | Type | Name / trigger | Inputs | Outputs / side effects | Module | Related PFV |
|---|---|---|---|---|---|---|
| EP-087 | BullMQ consumer | Queue `sri-emision`; `SriEmisionProcessor.process` | Job con `tipo` y `dto` | Delega a uno de cinco servicios de emision y retorna su resultado al job; relanza errores | `SriModule` | PFV-005 |
| EP-088 | BullMQ consumer | Queue `webhook-dispatch`; `WebhookProcessor.process` | Job con config, destino, evento, payload y material de firma no reproducido | Envia POST firmado, registra el intento y relanza fallos para retry | `WebhooksModule` | PFV-004 |

Las dos colas usan Redis. Sus opciones por defecto incluyen intentos, backoff
exponencial y limites de retencion de jobs completados o fallidos.

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/sri.module.ts`; `src/modules/webhooks/webhook.processor.ts`; `src/modules/webhooks/webhooks.module.ts`
- Function / Method / Procedure: `QueueModule`; `SriEmisionProcessor.process`; `WebhookProcessor.process`
- Line / Section: `src/common/queues/queue.module.ts:11-75`; `src/modules/sri/processors/sri-emision.processor.ts:10-46`; `src/modules/sri/sri.module.ts:25-52`; `src/modules/webhooks/webhook.processor.ts:7-13,20-109`; `src/modules/webhooks/webhooks.module.ts:8-13`
- Condition / Query / Statement: `@Processor` enlaza cada clase con su queue; el processor SRI selecciona servicio por `tipo`; el processor webhook construye un POST, registra respuesta/error y relanza fallos.
- Confidence: High

## Listeners de eventos internos

| ID | Type | Name / trigger | Inputs | Outputs / side effects | Module | Related PFV |
|---|---|---|---|---|---|---|
| EP-089 | Event listener | `comprobante.autorizado` | Payload interno del comprobante | Busca configuraciones suscritas y encola un job por webhook | `WebhooksModule` | PFV-004 |
| EP-090 | Event listener | `comprobante.rechazado` | Payload interno del comprobante | Busca configuraciones suscritas y encola un job por webhook | `WebhooksModule` | PFV-004 |

Tambien se encontro la emision de `comprobante.persistencia_fallida`, pero no
un listener correspondiente en el repositorio analizado. Al no existir un
handler confirmado, no se le asigna un `EP-`; la brecha se centraliza en
`PFV-006`.

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`
- Function / Method / Procedure: `WebhooksService.handleComprobanteAutorizado`; `handleComprobanteRechazado`; `emit`; bloques de persistencia de los cinco servicios SRI
- Line / Section: `src/modules/webhooks/webhooks.service.ts:29-55,268-320`; `src/modules/sri/services/factura.service.ts:505-519`; `src/modules/sri/services/nota-credito.service.ts:329`; `src/modules/sri/services/nota-debito.service.ts:312`; `src/modules/sri/services/retencion.service.ts:282`; `src/modules/sri/services/guia-remision.service.ts:320`
- Condition / Query / Statement: Dos `@OnEvent` reciben autorizado y rechazado; cinco servicios emiten `comprobante.persistencia_fallida`; la busqueda dirigida no encontro `@OnEvent('comprobante.persistencia_fallida')`.
- Confidence: High

## Hooks del ciclo de vida

| ID | Type | Name / trigger | Inputs | Outputs / side effects | Module | Related PFV |
|---|---|---|---|---|---|---|
| EP-091 | Lifecycle hook | `DatabaseService.onModuleInit` | Inicializacion del modulo | Crea pool PostgreSQL, registra listener de error y prueba una conexion; si falla, permite continuar sin pool | `DatabaseModule` | - |
| EP-092 | Lifecycle hook | `DatabaseService.onModuleDestroy` | Cierre Nest por shutdown hook | Cierra el pool si existe | `DatabaseModule` | - |
| EP-093 | Lifecycle hook | `XmlSignerService.onModuleInit` | Inicializacion de `SriModule` | Registra que los certificados se cargan por emisor | `SriModule` | - |
| EP-094 | Lifecycle hook | `XmlStorageService.onModuleInit` | Inicializacion de `SriModule` | Verifica y, si falta, crea el directorio base XML | `SriModule` | - |

**Evidence**
- File: `src/database/database.service.ts`; `src/main.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `DatabaseService.onModuleInit`; `DatabaseService.onModuleDestroy`; `XmlSignerService.onModuleInit`; `XmlStorageService.onModuleInit`; `bootstrap`
- Line / Section: `src/database/database.service.ts:12-75`; `src/main.ts:145-148`; `src/modules/sri/services/xml-signer.service.ts:29,62-68`; `src/modules/sri/services/xml-storage.service.ts:11,20-28`
- Condition / Query / Statement: Nest invoca los hooks al crear o destruir modulos; `enableShutdownHooks()` permite ejecutar el cierre de base de datos ante señales del proceso.
- Confidence: High

## Referencias de validacion funcional

Este documento no resuelve incertidumbres. Las siguientes referencias apuntan
al registro central en `14-pending-functional-validation.md`:

| PFV | Entry points related | Evidence gap retained here |
|---|---|---|
| PFV-001 | EP-039 | Ruta de factura publicada por README frente a la ruta declarada por el controlador y la coleccion. |
| PFV-002 | EP-008 | Contrato funcional esperado para la raiz `/` frente a la redireccion observada. |
| PFV-004 | EP-079, EP-082, EP-083, EP-088, EP-089, EP-090 | Cobertura real de los eventos ofrecidos por la configuracion de webhooks. |
| PFV-005 | EP-039 a EP-043, EP-087 | Mecanismo de consulta o entrega del resultado final asociado al `jobId`. |
| PFV-006 | Sin `EP-` confirmado | Consumidor o accion operativa esperada para `comprobante.persistencia_fallida`. |
| PFV-007 | EP-039 a EP-043 | Codigo HTTP contractual del camino sincrono frente al `201` fijado por controller. |
| PFV-008 | EP-001, EP-009 | Relacion contractual entre puerto configurable y health checks de contenedor fijados a 3001. |
| PFV-009 | EP-003 a EP-007, EP-076 a EP-078 | Exposicion de archivos estaticos y precedencia del prefijo `/images`. |

**Evidence**
- File: `README.md`; `Collection/Api_Facturacion_Sri.json`; `src/modules/webhooks/webhooks.controller.ts`; `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.service.ts`; `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`; `src/app.module.ts`; `src/main.ts`; `Dockerfile`
- Function / Method / Procedure: ejemplos de factura; `WebhooksController.getEventos`; listeners de `WebhooksService`; handlers de emision de `SriController`; productores de `SriService`; `ServeStaticModule.forRootAsync`; `bootstrap`; `HEALTHCHECK`
- Line / Section: `README.md:22,174,262,299`; `Collection/Api_Facturacion_Sri.json:869-898`; `src/modules/webhooks/webhooks.controller.ts:42-60`; `src/modules/webhooks/dto/webhook.dto.ts:14-22`; `src/modules/webhooks/webhooks.service.ts:33-55`; `src/modules/sri/sri.controller.ts:84-218`; `src/modules/sri/sri.service.ts:56-159`; `src/app.module.ts:63-89`; `src/main.ts:138-150`; `Dockerfile:56-64`
- Condition / Query / Statement: Las diferencias o ausencias enumeradas se conservan como preguntas centralizadas y no se promueven a comportamiento confirmado.
- Confidence: High
