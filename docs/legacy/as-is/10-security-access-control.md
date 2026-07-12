# 10 - Seguridad y control de acceso

## Alcance del lote

Este documento cubre exclusivamente los controles observables en las rutas
SRI relacionadas con factura y en sus dependencias directas de emisor, firma y
cola. La cobertura es parcial: no constituye un inventario completo de
autenticacion, ciclo de vida de tokens, administracion de usuarios, permisos de
otros modulos ni seguridad de toda la plataforma.

Las decisiones sobre si un RUC debe ser globalmente unico y sobre el alcance
correcto de emisores sin tenant permanecen abiertas en `PFV-036`. El acceso y
la retencion de datos fiscales y personales en BullMQ/Redis permanecen abiertos
en `PFV-038`. Estas preguntas no se tratan como politicas confirmadas.

Los IDs `SEC-001` a `SEC-008` son estables para este inventario.

## Inventario de controles

| ID | Area | Comportamiento AS-IS | Alcance / Trigger | Confianza | PFV relacionados |
|---|---|---|---|---|---|
| SEC-001 | Autenticacion HTTP | `JwtAuthGuard` esta registrado como guard global y omite solo las rutas marcadas `@Public()`. `POST /sri/emitir/factura` depende de ese guard global; preview y debug de factura, consulta de autorizacion, validacion de XML y sincronizacion ademas declaran `@UseGuards(JwtAuthGuard)` en la ruta. | Rutas HTTP SRI/Factura | High | - |
| SEC-002 | Autorizacion por emisor | Emision, preview y debug validan el RUC antes de delegar. `SUPERADMIN` recibe acceso al emisor encontrado. Para otros roles, la ausencia de `user.tenantId` bloquea; un tenant no coincidente bloquea solo cuando el emisor tiene `tenantId`. Por tanto, un emisor con tenant nulo pasa este control para cualquier usuario no `SUPERADMIN` que si tenga tenant. | Acceso por RUC desde DTO | High | PFV-036 |
| SEC-003 | Acceso por clave SRI | Antes de consultar, descargar, anular, reintentar o verificar un comprobante por clave de acceso, el controlador valida que la clave tenga 49 digitos numericos, extrae el RUC de las posiciones 10 a 22 y aplica `validateRucAccess`. Esta utilidad no comprueba el digito verificador. | Rutas con `:claveAcceso` | High | PFV-030, PFV-036 |
| SEC-004 | Limitacion de solicitudes | La emision HTTP de factura declara un limite de 10 solicitudes por ventana de 60 segundos. `ThrottlerGuard` esta registrado globalmente para ejecutar el control. | `POST /sri/emitir/factura` | High | - |
| SEC-005 | Resolucion tecnica del emisor | Tras el control HTTP, Factura resuelve el emisor solo por RUC y usa cache `emisor:ruc:{ruc}`; solo en cache miss consulta estado activo. El firmador aplica el mismo orden con una cache en memoria por RUC. No se propaga `emisorId` o `tenantId` desde la guarda hasta estas selecciones. | Procesamiento y firma de factura | High | PFV-036 |
| SEC-006 | Alcance de unicidad por RUC | El servicio de creacion rechaza un RUC encontrado globalmente, pero el DDL permite `tenant_id` nulo y unicidad `(tenant_id, ruc)`. Las consultas no incluyen tenant/orden y la vinculacion P12 actualiza por RUC. Con varias filas, la seleccion/actualizacion no queda aislada. | Tabla `emisores`, consultas y vinculacion P12 | Medium | PFV-036 |
| SEC-007 | Datos en cola y retencion | En modo asincrono se encola el DTO completo de factura, que incluye datos fiscales del emisor, identificacion/contacto del comprador, detalles, pagos e informacion adicional; el job contiene `tipo` y `dto`, sin identidad JWT o tenant. La configuracion de BullMQ declara conservacion por conteo de jobs completados y fallidos, y los despliegues Redis revisados activan AOF y un volumen persistente. La politica efectiva de acceso, minimizacion y retencion requiere validacion operativa. | BullMQ `sri-emision` y Redis | Medium | PFV-031, PFV-038 |
| SEC-008 | Administracion P12 | Las rutas de listar, eliminar, informar y validar certificados dependen del JWT global, pero no declaran rol ni tenant. Upload valida acceso al RUC solo cuando se informa RUC; sin el, conserva un archivo no vinculado. Delete opera por nombre de archivo y limpia todas las filas que lo referencian. | `/certificates/*` usado por firma SRI | High | PFV-039 |

## Evidencia detallada

### SEC-001 - Autenticacion HTTP

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/guards/jwt-auth.guard.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: registro de `APP_GUARD`; `JwtAuthGuard.canActivate`; `SriController.emitirFactura`, `consultarAutorizacion`, `previewFactura`, `validarXml`, `debugFacturaFirmada`, `sincronizar`
- Line / Section: app 120-135; guard 11-28; controlador 84-110, 221-268, 306-327, 522-591
- Condition / Query / Statement: el guard JWT global delega a Passport salvo metadata `@Public()`; emitir factura no declara un guard local, mientras las rutas indicadas si usan `@UseGuards(JwtAuthGuard)` y ninguna ruta de `SriController` esta marcada `@Public()`.
- Confidence: High

### SEC-002 - Autorizacion por emisor

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/emisores/emisores.service.ts`
- Function / Method / Procedure: `SriController.emitirFactura`, `previewFactura`, `debugFacturaFirmada`; `EmisoresService.validateRucAccess`, `findByRuc`
- Line / Section: controlador 103-110, 257-264, 314-327; servicio 227-267
- Condition / Query / Statement: las tres rutas esperan `validateRucAccess(dto.emisor.ruc, user)`; el servicio retorna para `SUPERADMIN`, rechaza cuando falta `user.tenantId` y evalua la diferencia solo con `(emisor.tenantId && emisor.tenantId !== user.tenantId)` despues de buscar `WHERE ruc = $1`.
- Confidence: High

### SEC-003 - Acceso por clave SRI

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/utils/clave-acceso.utils.ts`
- Function / Method / Procedure: `SriController.validateClaveAccesoAccess`, `consultarAutorizacion`, `obtenerComprobante`, `descargarXml`, `anularComprobante`, `reintentarComprobante`, `verificarEnSri`; `extractRucFromClaveAcceso`, `validateClaveAcceso`
- Line / Section: controlador 71-82, 221-242, 378-519; utilidad 16-19, 38-51
- Condition / Query / Statement: cada ruta citada invoca el helper antes de acceder al comprobante; el helper valida presencia, longitud y contenido numerico, extrae `substring(10, 23)` y delega el RUC a `validateRucAccess`, sin recalcular Modulo 11.
- Confidence: High

### SEC-004 - Limitacion de solicitudes

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/app.module.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; configuracion de `ThrottlerModule`; registro global de `ThrottlerGuard`
- Line / Section: controlador 84-110; app 51-61, 131-135
- Condition / Query / Statement: la ruta declara `@Throttle({ default: { limit: 10, ttl: 60000 } })` y la aplicacion registra `ThrottlerGuard` mediante `APP_GUARD`.
- Confidence: High

### SEC-005 - Resolucion tecnica del emisor

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriRepositoryService.findEmisorByRuc`; `XmlSignerService.loadEmisorCertificate`, `signXmlForEmisor`
- Line / Section: factura 54-64, 128-144; repositorio 142-163; firmador 36-52, 285-311, 378-395
- Condition / Query / Statement: factura pasa solo `dto.emisor.ruc`; el repositorio consulta `WHERE ruc = $1 AND estado = $2` y usa `emisor:ruc:${ruc}`; el firmador consulta `WHERE ruc = $1 AND estado = 'ACTIVO'` y lee/escribe `emisorCertificateCache` por RUC.
- Confidence: High

### SEC-006 - Alcance de unicidad por RUC

**Evidence**
- File: `database/init.sql`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/certificate/certificate.controller.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: definicion/constraint; `findByRuc`; `create`; `findEmisorByRuc`; `loadEmisorCertificate`; `bindCertificateToEmisor`; `queryOne`
- Line / Section: DDL 432-453, 1239-1243; emisores 254-291; repositorio 142-163; firmador 303-311; certificate controller 323-380; database 134-143
- Condition / Query / Statement: create rechaza por RUC global, el DDL permite unicidad por tenant y las selecciones/`UPDATE ... WHERE ruc = $6` no reciben tenant; helpers de una fila toman el primer resultado.
- Confidence: Medium

### SEC-007 - Datos en cola y retencion

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/dto/common.dto.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `src/modules/sri/sri.module.ts`; `docker-compose.yml`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; `CreateFacturaDto` y DTO anidados; registro BullMQ de `sri-emision`; configuracion `queues.sriEmision`; servicio Redis
- Line / Section: servicio 56-67; processor 24-46; factura DTO 20-95; DTO comunes 17-110, 113-243; cola 15-49; configuracion 138-145; modulo SRI 27-32; compose 10-26, 91-93; compose produccion 21-35, 102-104
- Condition / Query / Statement: el productor agrega `{ tipo: 'FACTURA', dto }` y el worker consume esos dos campos; el DTO contiene los grupos de datos descritos; `defaultJobOptions` usa los conteos configurables de completados y fallidos (valores por defecto 1000 y 5000), mientras Redis se inicia con `appendonly yes` y `redis_data:/data`. `SriModule` registra adicionalmente la misma cola sin opciones, por lo que la efectividad de las opciones declaradas tambien depende de `PFV-031`.
- Confidence: Medium

### SEC-008 - Administracion de certificados P12

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/guards/roles.guard.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: guardas globales; `RolesGuard.canActivate`; endpoints list/delete/upload/info/validate; `bindCertificateToEmisor`
- Line / Section: app 120-135; roles guard 16-48; certificate controller 63-173, 190-287, 323-421
- Condition / Query / Statement: JWT es global y RolesGuard permite handlers sin `@Roles`; el controller no declara roles, aplica `validateRucAccess` solo dentro de `if (body.ruc)` y delete actualiza por `certificado_nombre` antes de borrar el archivo.
- Confidence: High

## Limites y preguntas abiertas

- `PFV-036` debe confirmar la politica funcional de unicidad del RUC, el alcance
  esperado de emisores con `tenant_id` nulo y la seleccion autorizada cuando un
  RUC sea compatible con mas de una fila.
- `PFV-038` debe identificar quienes pueden leer o administrar jobs BullMQ,
  cuanto tiempo deben persistir los DTO de factura y si se exige minimizacion,
  cifrado adicional o eliminacion por tiempo. La configuracion revisada expresa
  conteos, no una duracion de retencion.
- `PFV-039` debe confirmar roles y alcance tenant para listar, cargar sin
  vinculacion, inspeccionar, validar y eliminar certificados P12.
- El analisis de autenticacion completa, roles generales, auditoria y controles
  de otros endpoints queda fuera de este lote.
