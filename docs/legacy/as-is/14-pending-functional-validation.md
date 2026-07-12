# 14. Validaciones funcionales pendientes

## Alcance del lote

Este registro centraliza contradicciones, contratos incompletos y decisiones
operativas que no pueden confirmarse solo con el repositorio. Ninguna fila de
esta tabla constituye un requisito confirmado. Los dos primeros lotes cubren el
descubrimiento estructural y la extraccion detallada del flujo comun SRI y de
Factura; los otros tipos de comprobante aun requieren lotes separados.

## Registro PFV

| ID | Area | Question | Evidence Found | Risk | Suggested Owner | Answer
|---|---|---|---|---|---|---|
| PFV-001 | Contrato API | ¿Que ruta de emision de factura debe preservarse como contrato? | El README usa `/sri/factura/emitir`; el controlador y la coleccion usan `/sri/emitir/factura`. | Incompatibilidad con clientes existentes. | Product Owner / API Owner | Mantener el de el readme pero se va a hacer una reingeniería total usando clean architecture para todo, así que no es necesario mantener la compatibilidad de endpoints |
| PFV-002 | Endpoint raiz | ¿`GET /` debe redirigir con 302 o responder 200 con contenido? | El controlador redirige a `/status`; la prueba E2E espera 200 y `Hello World!`. | Health checks o pruebas dependientes de un contrato obsoleto. | API Owner / QA | Debe responder 200 con `{"status": "healthy"}` |
| PFV-003 | Evidencia documental | ¿Deben recuperarse los nueve documentos versionados que faltan del worktree para incorporarlos al analisis? | El indice Git los contiene y el worktree los marca eliminados. | Reglas o contratos historicos pueden quedar fuera del AS-IS. | Maintainer / Functional Analyst | No se debe recuperar los archivos borrados se borraron intencionalmente |
| PFV-004 | Webhooks | ¿Cuales de los eventos configurables producen notificaciones efectivas? | El catalogo expone siete eventos y solo se encontraron listeners para autorizado y rechazado. | Suscripciones aceptadas que nunca se disparan. | Product Owner / Backend | Mantener solo autorizado y rechazado |
| PFV-005 | Emision asincrona | ¿Como consulta o recibe el cliente el resultado final asociado con un `jobId`? | La emision devuelve `jobId` y estado en cola; no se encontro consulta de job ni endpoint de resultado. | El cliente puede no tener correlacion ni cierre confirmado. | Product Owner / Backend | No se encontró evidencia de un mecanismo mediante el cual el cliente consulte o reciba el resultado final utilizando el `jobId`. Se requiere validación con el Product Owner y el equipo Backend para determinar si existe un endpoint no documentado, un mecanismo de notificación o un proceso de consulta externo |
| PFV-006 | Persistencia fallida | ¿Que componente operativo consume `comprobante.persistencia_fallida`? | Cinco servicios emiten el evento y no se encontro un listener en el repositorio. | Fallos de persistencia sin alerta o reconciliacion confirmada. | Operations / Backend | No aplica en el AS IS |
| PFV-007 | Contrato HTTP SRI | ¿Que codigo HTTP debe devolver una emision sincrona: 200 o 201? | Los handlers fijan 201 y Swagger tambien declara una variante sincrona 200. | Clientes y pruebas pueden interpretar de forma distinta el resultado. | API Owner / QA | Debería ser 201 |
| PFV-008 | Puerto del contenedor | ¿El puerto interno esta fijado contractualmente en 3001 o puede cambiar mediante `PORT`? | La aplicacion escucha `PORT`; Docker expone y sondea el puerto interno 3001. | Contenedor no accesible o marcado como no saludable. | Operations | Se debería poder cambiar por PORT |
| PFV-009 | Archivos estaticos | ¿Los PDF e imagenes deben ser publicos y cual es la precedencia entre el montaje `/images/*` y `ImageController`? | Los directorios se montan como estaticos y el controlador comparte el prefijo `/images`. | Exposicion o resolucion de rutas diferente de la esperada. | Security / Backend | No son públicos por ahora |
| PFV-010 | SRI SOAP | ¿Las variables WSDL o las URLs codificadas en la fabrica son la fuente autoritativa? | La configuracion exige dos WSDL y el health check usa configuracion; las llamadas de negocio seleccionan URLs codificadas. | El health check puede probar un destino distinto al usado para emitir. | Operations / SRI Integration Owner | Si |
| PFV-011 | Inicializacion de datos | ¿Cual es el procedimiento autoritativo y repetible para crear o evolucionar la base de datos? | Solo existe un dump PostgreSQL 17 y una guia manual separada para la extension, con nombres de base distintos. | Instalacion no reproducible, incompatibilidad de version o perdida de datos. | DBA / Maintainer | Posteriormente se usará Flyway cuando se migre este sistema a Quarkus |
| PFV-012 | Certificados | ¿El binario en PostgreSQL o el archivo P12 es la copia autoritativa que debe preservarse? | Multer escribe el archivo; si se informa RUC y el vinculo termina, tambien se escribe el binario en DB. El firmador usa los bytes del filesystem. | Restauraciones o migraciones pueden conservar solo una copia inutilizable. | Security / Backend | 
| PFV-013 | Esquema de consultas | ¿El esquema desplegado contiene columnas de filtros ausentes del dump? | El repositorio consulta `identificacion_comprador`, `establecimiento` y `punto_emision`; el dump modela otros nombres y relaciones. | Consultas filtradas pueden fallar contra el esquema documentado. | DBA / Backend |
| PFV-014 | Tablas de detalle | ¿Las referencias a `impuestos_doc_sustento`, `destinatarios_guia` y `detalles_guia` son API legada o DDL faltante? | El servicio permite esos nombres; el dump contiene `guia_destinatarios` y `guia_detalles`, y no se hallaron llamadas a los metodos asociados. | Migrar contratos muertos o perder estructuras requeridas. | Backend / Functional Analyst |
| PFV-015 | Retencion XML | ¿Donde se aplica y supervisa la retencion declarada de siete anos para XML? | El servicio declara esa retencion en un comentario, pero solo se encontro organizacion, lectura y escritura de archivos. | Eliminacion prematura o acumulacion sin control. | Legal / Operations |
| PFV-016 | Redis | ¿La politica `allkeys-lru` de produccion es intencional para una instancia que tambien aloja BullMQ? | Produccion permite eviction; desarrollo usa `noeviction`; colas y cache comparten la instancia Redis. | Eviccion de claves de jobs o historial bajo presion de memoria. | Operations / Backend |
| PFV-017 | Despliegue | ¿Que guia y composicion representan el despliegue operativo vigente? | `DEPLOYMENT.md` describe una composicion distinta y omite dependencias/variables requeridas por el codigo actual. | Despliegue incompleto o configuracion inconsistente. | Operations / Maintainer |
| PFV-018 | Usuario inicial | ¿La identidad administrativa sembrada sigue activa o se rota/deshabilita en cada entorno? | El dump inserta una identidad SUPERADMIN fija y los artefactos de consumo publican credenciales de ejemplo; los valores se omiten aqui. | Acceso administrativo predecible si el entorno conserva los valores iniciales. | Security / Operations |
| PFV-019 | Contenido factura | ¿Cuales son las cardinalidades minimas y que valores deben ser estrictamente positivos? | Emisor/comprador no usan `IsDefined`; detalles, impuestos y pagos no exigen elementos; cantidades e importes admiten cero. | Facturas vacias o fallo tardio en el worker/servicio. | Product Owner / SRI Functional Analyst |
| PFV-020 | Fecha emision | ¿Que fechas calendario y rangos temporales son validos para emitir? | El DTO solo valida `dd/mm/yyyy`; `Date` normaliza combinaciones imposibles mientras el XML conserva el texto. | Clave de acceso y XML con fechas distintas. | Product Owner / SRI Functional Analyst |
| PFV-021 | Aritmetica fiscal | ¿El servidor debe recalcular o comparar tarifa, base, impuesto y suma de pagos? | Solo se valida la existencia de codigos; los valores monetarios provienen del DTO y alimentan los totales. | Totales fiscalmente inconsistentes aceptados por la API. | Product Owner / Accounting / QA |
| PFV-022 | Datos emisor | ¿Los datos fiscales y ambiente del XML provienen de la solicitud, del maestro `emisores` o deben coincidir? | El maestro recuperado aporta ID/certificado; razon social, direcciones, obligaciones y ambiente salen del DTO/configuracion. | Comprobante emitido con datos distintos del maestro autorizado. | Product Owner / SRI Functional Analyst |
| PFV-023 | Secuencial manual | ¿Un secuencial explicito exige punto activo, unicidad y actualizacion del contador automatico? | La rama manual solo rellena el valor, puede continuar sin punto y no modifica `secuenciales`. | Autorizacion sin persistencia o reutilizacion futura del numero. | Product Owner / Accounting / Backend |
| PFV-024 | Numero autorizacion | ¿Debe guardarse la clave de acceso como numero de autorizacion en estados no autorizados? | Persistencia usa `numeroAutorizacion` y, si falta, `claveAcceso`; la respuesta API no aplica el fallback. | Semantica divergente entre DB y contrato HTTP. | Product Owner / Data Owner |
| PFV-025 | Estados SRI | ¿Cual es el vocabulario canonico y que estados deben notificar o reintentarse? | SOAP produce valores con espacios; DTO, indice y reintento usan guion bajo; `NO AUTORIZADO` no emite rechazo directo. | Registros no consultables/reintentables y webhooks omitidos. | Product Owner / SRI Integration Owner |
| PFV-026 | Tipo identificacion | ¿La identificacion `09` PLACA esta soportada en factura? | El enum DTO la acepta; el validador la rechaza y el dump solo carga tipos `04` a `08`. | Contrato que acepta y rechaza el mismo codigo en capas distintas. | Product Owner / SRI Functional Analyst |
| PFV-027 | Catalogo identificacion | ¿Longitud y regex del catalogo o los algoritmos en codigo son la autoridad? | Los metadatos se cargan en cache, pero no se aplican al valor de identificacion. | Validacion distinta de la configuracion de datos. | Data Owner / SRI Functional Analyst |
| PFV-028 | Vigencia tarifas | ¿Como se selecciona la revision temporal vigente de una tarifa? | La tabla admite revisiones por `vigente_desde`; la carga no filtra ese inicio ni ordena, y el mapa conserva una fila por pareja. | Uso anticipado o seleccion no determinista de una revision. | Accounting / Data Owner |
| PFV-029 | Vigencia certificado | ¿Debe comprobarse la vigencia en cada firma y despues de vencer un P12 ya cargado? | La carga inicial valida fechas; el firmador no consulta `notAfter` ni `certificado_valido_hasta`. | Firma con certificado vencido o aun no valido. | Security / SRI Integration Owner |
| PFV-030 | Clave acceso | ¿Todas las rutas por clave deben validar Modulo 11 y devolver 4xx por formato/checksum invalido? | El servicio posee validacion completa; los controllers usan una utilidad de longitud/digitos que lanza `Error` plano. | Entradas invalidas tratadas como error interno y checksum omitido. | API Owner / QA |
| PFV-031 | Cola SRI | ¿Que registro de `sri-emision` determina realmente intentos, backoff y retencion? | La cola se registra globalmente con opciones y localmente en `SriModule` sin ellas. | Politica operativa de retry distinta de la documentada. | Backend / Operations |
| PFV-032 | Idempotencia | Ante retry BullMQ o reenvio HTTP, ¿deben reutilizarse secuencial, clave y registro del primer intento? | El job no usa identidad determinista y cada ejecucion automatica reserva secuencial y codigo numerico nuevos. | Multiples comprobantes o intentos SRI para una operacion logica. | Product Owner / Backend / Operations |
| PFV-033 | Huecos secuenciales | ¿Se aceptan secuenciales consumidos cuando falla certificado, firma o un paso previo a SRI? | La reserva se confirma en una transaccion separada antes de comprobar certificado y firmar. | Huecos operativos sin politica confirmada. | Accounting / Product Owner |
| PFV-034 | Retry SOAP | ¿Los timeouts configurados y reintentos de autorizacion deben cubrir las excepciones de consulta? | Los timeouts no se aplican al cliente y cualquier excepcion de autorizacion sale del bucle en el primer intento afectado. | Latencia no acotada o menor tolerancia a fallos que la configurada. | SRI Integration Owner / Operations |
| PFV-035 | Reconciliacion | ¿Como se detectan y reparan autorizaciones sin DB, filas sin XML y XML huerfanos? | SRI, PostgreSQL y filesystem tienen limites independientes; solo existe un evento sin consumidor confirmado para parte de los fallos. | Divergencia fiscal y operativa sin recuperacion confirmada. | Operations / Accounting / Backend |
| PFV-036 | Alcance por RUC | ¿El RUC es globalmente unico y como deben tratarse emisores sin tenant o repetidos entre tenants? | Create rechaza globalmente, DDL permite por tenant y guardas/emision/firma/vinculacion/caches usan solo RUC; para un usuario no `SUPERADMIN` con tenant, un emisor de tenant nulo supera la guarda. | Seleccion o actualizacion de otro emisor/certificado y ruptura de aislamiento. | Security / Data Owner / Backend |
| PFV-037 | Estado de impuesto | ¿Una tarifa activa debe invalidarse cuando su impuesto padre esta inactivo? | La carga filtra actividad de la tarifa, pero no `catalogo_impuestos.activo`. | Uso de codigos tributarios deshabilitados. | Accounting / Data Owner |
| PFV-038 | Datos en cola | ¿Que acceso y retencion deben tener los DTO fiscales/personales almacenados en BullMQ/Redis? | La emision encola el DTO completo; los defaults retienen jobs por conteo y Redis usa AOF/volumen. | Exposicion o conservacion de datos mas alla de la necesidad operativa. | Security / Privacy / Operations |
| PFV-039 | Administracion P12 | ¿Que roles y alcance tenant deben regir listado, upload sin vinculo, informacion, validacion y eliminacion de certificados? | Las rutas usan JWT global sin roles; solo el vinculo con RUC valida acceso, y delete opera globalmente por nombre. | Exposicion de listado o metadatos, validacion o eliminacion sobre certificados de otro emisor/tenant. | Security / Product Owner / Backend |

## Evidencia detallada

### PFV-001 - Ruta de emision de factura

**Evidence**
- File: `README.md`; `src/modules/sri/sri.controller.ts`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: ejemplo de consumo; `SriController.emitirFactura`; request Postman de emision
- Line / Section: README 22, 174, 262, 299; controlador 61, 84-110; coleccion 869, 898
- Condition / Query / Statement: dos formas de ruta incompatibles aparecen como contrato de la misma operacion.
- Confidence: High

### PFV-002 - Comportamiento de la raiz

**Evidence**
- File: `src/modules/status/status.controller.ts`; `test/app.e2e-spec.ts`
- Function / Method / Procedure: `StatusController.root`; prueba `GET /`
- Line / Section: controlador 63-72; prueba 19-23
- Condition / Query / Statement: el codigo declara redireccion 302 y la prueba espera respuesta 200 con un cuerpo diferente.
- Confidence: High

### PFV-003 - Documentos ausentes

**Evidence**
- File: indice y worktree Git; `docs/*.md`
- Function / Method / Procedure: `git ls-files docs`; `git status --short`
- Line / Section: no aplica; nueve rutas versionadas aparecen eliminadas
- Condition / Query / Statement: los archivos no son legibles en el estado actual y no fueron restaurados ni modificados durante este lote.
- Confidence: High

### PFV-004 - Cobertura de eventos webhook

**Evidence**
- File: `src/modules/webhooks/dto/webhook.dto.ts`; `src/modules/webhooks/webhooks.controller.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: catalogo `WEBHOOK_EVENTOS`; `eventosDisponibles`; handlers `@OnEvent`
- Line / Section: DTO 14-22; controlador 42-60; servicio 33-55
- Condition / Query / Statement: se publican siete opciones configurables y solo hay dos listeners locales.
- Confidence: High

### PFV-005 - Resultado de jobs SRI

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: cinco metodos `emitir*`; inventario de rutas de `SriController`
- Line / Section: servicio 56-67, 86-99, 106-119, 126-139, 146-159; controlador 84-218, 334-591
- Condition / Query / Statement: la respuesta asincrona incluye `jobId`; la busqueda dirigida no encontro `getJob`, `QueueEvents` ni ruta de estado de job.
- Confidence: Medium

### PFV-006 - Evento de persistencia fallida

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `nota-credito.service.ts`; `nota-debito.service.ts`; `retencion.service.ts`; `guia-remision.service.ts`
- Function / Method / Procedure: manejo de fallo posterior a emision
- Line / Section: factura 512; nota de credito 329; nota de debito 312; retencion 282; guia 320
- Condition / Query / Statement: los cinco servicios emiten `comprobante.persistencia_fallida`; no se encontro un `@OnEvent` correspondiente.
- Confidence: Medium

### PFV-007 - Codigo HTTP de emision sincrona

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `emitirFactura`, `emitirNotaCredito`, `emitirNotaDebito`, `emitirRetencion`, `emitirGuiaRemision`
- Line / Section: 84-110, 113-137, 140-164, 167-191, 194-218
- Condition / Query / Statement: `@HttpCode(HttpStatus.CREATED)` fija 201 mientras las respuestas Swagger describen tambien 200 para ejecucion sincrona.
- Confidence: High

### PFV-008 - Puerto interno

**Evidence**
- File: `src/main.ts`; `Dockerfile`; `docker-compose.yml`; `docker-compose.prod.yml`
- Function / Method / Procedure: `bootstrap`; exposicion y health checks de contenedor
- Line / Section: main 138-150; Dockerfile 57, 60-64; compose desarrollo 53-55, 68-78; compose produccion 64-66, 79-89
- Condition / Query / Statement: el listener usa configuracion y los artefactos de contenedor apuntan al puerto interno 3001.
- Confidence: High

### PFV-009 - Montajes estaticos

**Evidence**
- File: `src/app.module.ts`; `src/modules/image/image.controller.ts`
- Function / Method / Procedure: `ServeStaticModule.forRootAsync`; `ImageController`
- Line / Section: AppModule 63-89; controlador 31, 39, 108, 138
- Condition / Query / Statement: el montaje estatico `/images` y las rutas API usan el mismo prefijo; el acceso efectivo no fue probado en runtime.
- Confidence: Medium

### PFV-010 - Destino SRI SOAP

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/status/sri.health.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`
- Function / Method / Procedure: configuracion WSDL; `SriHealthIndicator.isHealthy`; fabrica de clientes SOAP
- Line / Section: configuracion 40-46; health 21-41; fabrica 12-20, 27-65
- Condition / Query / Statement: health y configuracion usan valores de entorno, pero la fabrica selecciona URLs constantes para las llamadas.
- Confidence: High

### PFV-011 - Procedimiento de base de datos

**Evidence**
- File: `database/init.sql`; `database/Install BD.txt`
- Function / Method / Procedure: cabecera/DDL del dump; guia de instalacion de `uuid-ossp`
- Line / Section: dump 6-7, 39-43, 205-206 y usos posteriores; guia 1-23, 30-44
- Condition / Query / Statement: el dump fue generado con PostgreSQL 17, depende de `extensions.uuid_generate_v4()` y la preparacion esta en un procedimiento separado con nombres de base inconsistentes.
- Confidence: High

### PFV-012 - Fuente de certificados

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/sri/services/xml-signer.service.ts`; `database/init.sql`
- Function / Method / Procedure: carga y vinculacion de certificado; `loadEmisorCertificate`; DDL `emisores`
- Line / Section: controlador 165-173, 255-287, 323-363; firmador 303-380; DDL 432-454
- Condition / Query / Statement: el upload siempre parte de un archivo Multer; la actualizacion del binario DB esta condicionada a RUC/vinculacion, mientras la firma recupera el archivo por nombre.
- Confidence: High

### PFV-013 - Columnas de filtros

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: construccion de filtros de comprobantes; DDL `comprobantes`
- Line / Section: repositorio 466-501, 556-561; dump 354-399
- Condition / Query / Statement: tres columnas usadas en filtros no aparecen en la tabla fisica documentada; otras partes de la consulta si usan joins y nombres del dump.
- Confidence: High

### PFV-014 - Nombres de tablas de detalle

**Evidence**
- File: `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: lista de tablas permitidas y metodos de persistencia; DDL de guias
- Line / Section: repositorio 34-49, 347-351, 397-408; dump 538-584
- Condition / Query / Statement: el codigo admite tres nombres ausentes del dump; no se encontraron call sites para esos metodos en el repositorio.
- Confidence: Medium

### PFV-015 - Retencion de XML

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: documentacion de `XmlStorageService`; `getComprobantePath`, `saveXml`, `readXml`
- Line / Section: 6-9, 30-81, 132-148
- Condition / Query / Statement: se declara retencion de siete anos, pero el codigo localizado solo organiza, escribe y lee archivos.
- Confidence: Medium

### PFV-016 - Eviccion Redis

**Evidence**
- File: `docker-compose.prod.yml`; `docker-compose.yml`; `src/common/queues/queue.module.ts`
- Function / Method / Procedure: comandos Redis; conexion BullMQ
- Line / Section: produccion 26-34; desarrollo 15-26; QueueModule 15-26
- Condition / Query / Statement: produccion configura `allkeys-lru`, desarrollo `noeviction`, y BullMQ usa la misma instancia Redis que la cache.
- Confidence: High

### PFV-017 - Fuente de despliegue

**Evidence**
- File: `DEPLOYMENT.md`; `docker-compose.yml`; `src/config/configuration.ts`
- Function / Method / Procedure: guia de despliegue; servicios Compose; carga de configuracion
- Line / Section: guia 93-137; compose 9-92; configuracion 8-12, 44-45, 69-72, 89-91, 111-117
- Condition / Query / Statement: la guia muestra una composicion diferente y no cubre varias dependencias o variables requeridas por el codigo actual.
- Confidence: High

### PFV-018 - Identidad administrativa inicial

**Evidence**
- File: `database/init.sql`; `README.md`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: seed de `usuarios`; ejemplos de autenticacion
- Line / Section: dump 1015; README 156-169; coleccion 4-8, 62-90
- Condition / Query / Statement: existe una identidad SUPERADMIN fija con material de autenticacion de ejemplo. Los valores se han redactado deliberadamente.
- Confidence: High

### PFV-019 - Contenido minimo de factura

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `CreateFacturaDto`; `EmisorDto`; `CompradorDto`; `DetalleFacturaDto`; `PagoDto`; `SriController.emitirFactura`
- Line / Section: factura DTO 57-80; common 79-110, 113-135, 150-230; controller 103-110
- Condition / Query / Statement: no hay `IsDefined` en los objetos anidados ni minimo de elementos; decoradores numericos usan `Min(0)` y el controller accede inmediatamente al RUC.
- Confidence: High

### PFV-020 - Fecha calendario

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `CreateFacturaDto.fechaEmision`; parseos de `emitirFactura` y `persistirFactura`
- Line / Section: DTO 39-44; servicio 69-74, 365, 471-475, 558
- Condition / Query / Statement: el regex comprueba forma; el objeto `Date` se usa para clave/ruta y el texto original para XML/DB.
- Confidence: High

### PFV-021 - Consistencia aritmetica

**Evidence**
- File: `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `validarImpuestosDetalles`; `validateImpuesto`; `buildDetalles`; `calculateTotales`
- Line / Section: base 63-101; catalogo 84-119; factura 572-577, 612-710
- Condition / Query / Statement: se verifica el par de codigos; tarifa/base/valor y pagos no se comparan con catalogo, neto o importe total.
- Confidence: High

### PFV-022 - Fuente de datos del emisor

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/interfaces/repository.interface.ts`
- Function / Method / Procedure: `emitirFactura`; `buildFacturaFromDto`; `EmisorRecord`
- Line / Section: factura 55-68, 128-145, 541-577; interface 175-196
- Condition / Query / Statement: la fila activa se usa para ID/certificado; el XML usa los datos fiscales recibidos y defaults globales.
- Confidence: High

### PFV-023 - Secuencial manual

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: rama de secuencial; `getNextSecuencial`; constraints de comprobante/secuencial
- Line / Section: factura 76-104, 187-205; repositorio 199-219; DDL 1207-1219, 1319-1323
- Condition / Query / Statement: la rama explicita no requiere el punto ni toca el contador; el DDL no impone unicidad por punto/tipo/secuencial.
- Confidence: High

### PFV-024 - Fallback de autorizacion

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `persistirFactura`; `mapResultToResponse`
- Line / Section: 365-370, 713-721
- Condition / Query / Statement: la fila usa la clave cuando falta numero SRI; el DTO conserva `undefined`.
- Confidence: High

### PFV-025 - Estados y eventos

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/query-comprobantes.dto.ts`; `src/modules/sri/sri.service.ts`; `database/init.sql`
- Function / Method / Procedure: `enviarYAutorizar`; persistencia/eventos; `EstadoComprobante`; reintento/sincronizacion; indice de estados
- Line / Section: SOAP 167-212; factura 207-228, 366-367; DTO 15-24; SriService 493-505, 693-698; DDL 1480-1484
- Condition / Query / Statement: los mismos estados se representan con espacios o guiones bajos y participan en condiciones diferentes.
- Confidence: High

### PFV-026 - Identificacion PLACA

**Evidence**
- File: `src/modules/sri/constants/sri.enums.ts`; `src/modules/sri/services/identificacion-validator.service.ts`; `database/init.sql`
- Function / Method / Procedure: `TipoIdentificacion`; `validar`; carga inicial del catalogo
- Line / Section: enum 33-40; validador 16-36; DDL/datos 190-198, 891-895
- Condition / Query / Statement: `09` existe en el enum, no tiene rama en el validador y no aparece en los datos iniciales.
- Confidence: High

### PFV-027 - Metadatos de identificacion

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/identificacion-validator.service.ts`
- Function / Method / Procedure: `loadCache`; `validateTipoIdentificacion`; validadores por tipo
- Line / Section: catalogo 206-223, 394-407; identificacion 16-270
- Condition / Query / Statement: longitud y regex se almacenan en cache, pero la validacion del valor se resuelve por algoritmos/literales separados.
- Confidence: High

### PFV-028 - Seleccion temporal de tarifa

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/catalogo-validator.service.ts`
- Function / Method / Procedure: DDL `catalogo_tarifas_impuesto`; `loadCache`
- Line / Section: DDL 172-183, 1116-1124; catalogo 343-363
- Condition / Query / Statement: el modelo permite revisiones por inicio; la consulta omite inicio y orden, y `Map.set` usa una clave que no incluye la revision.
- Confidence: High

### PFV-029 - Vigencia al firmar

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/certificate/certificate.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: upload/validacion; `validateCertificateExpiry`; `loadEmisorCertificate`; `signXmlForEmisor`
- Line / Section: controller 212-271; certificate service 287-332; signer 285-454
- Condition / Query / Statement: la carga rechaza vigencia invalida; la firma posterior carga/cachea material sin verificar fechas del certificado.
- Confidence: High

### PFV-030 - Validacion de clave en rutas

**Evidence**
- File: `src/modules/sri/services/clave-acceso.service.ts`; `src/modules/sri/utils/clave-acceso.utils.ts`; `src/modules/sri/sri.controller.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `validate`; utilidad `validateClaveAcceso`; `validateClaveAccesoAccess`; filtro global
- Line / Section: servicio 62-75; utilidad 38-51; controller 76-81; filtro 20-45
- Condition / Query / Statement: la validacion Modulo 11 no se usa en controllers; la utilidad lanza `Error` y el filtro responde 500.
- Confidence: High

### PFV-031 - Opciones efectivas de cola

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: registros BullMQ de `sri-emision`
- Line / Section: QueueModule 29-49; SriModule 27-32
- Condition / Query / Statement: el registro global tiene opciones por defecto y el registro local del mismo nombre no las declara; no se ejecuto resolucion DI.
- Confidence: Medium

### PFV-032 - Idempotencia de ejecucion

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `process`; `FacturaService.emitirFactura`; `generateCodigoNumerico`
- Line / Section: SriService 56-67; processor 24-46; factura 85-117, 147-185, 505-520; clave 38-55, 121-123
- Condition / Query / Statement: `queue.add` no fija `jobId`; el processor relanza y cualquier nueva invocacion automatica reserva otro numero y genera otra clave.
- Confidence: High

### PFV-033 - Huecos secuenciales

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: fases de `emitirFactura`; `transaction`
- Line / Section: factura 85-145; database 171-185
- Condition / Query / Statement: la reserva se confirma antes de verificar certificado, firmar o llamar al SRI; un fallo posterior no revierte ese contador.
- Confidence: High

### PFV-034 - Timeout y retry de autorizacion

**Evidence**
- File: `src/config/configuration.ts`; `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`
- Function / Method / Procedure: configuracion rate limiting; `enviarYAutorizar`; creacion de clientes
- Line / Section: config 53-66; SOAP 25-76, 84-227; factory 27-65
- Condition / Query / Statement: se configuran timeouts que no se pasan al cliente; el bucle de autorizacion no captura por intento ninguna excepcion.
- Confidence: High

### PFV-035 - Reconciliacion de persistencias

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: fases de emision; `persistirFactura`; `reintentarComprobante`; `sincronizarConSri`; almacenamiento XML; `transaction`; listeners
- Line / Section: factura 85-205, 470-520; SriService 538-586, 768-790; storage 53-140; database 171-185; webhooks 29-55
- Condition / Query / Statement: SRI, filas y archivos usan limites distintos; reintento/sincronizacion actualizan estado antes del XML, los archivos no se revierten y el evento no tiene consumidor confirmado.
- Confidence: High

### PFV-036 - Alcance por RUC y tenant

**Evidence**
- File: `database/init.sql`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: constraint; `findByRuc`; `validateRucAccess`; `create`; `findEmisorByRuc`; `loadEmisorCertificate`; `bindCertificateToEmisor`
- Line / Section: dump 432-454, 1239-1243; emisores 227-291; repositorio 142-160; signer 285-385; certificate controller 255-287, 323-380
- Condition / Query / Statement: create intenta unicidad global, el DDL la limita por tenant y consultas/caches/vinculacion usan RUC; la guarda no rechaza tenant nulo.
- Confidence: High

### PFV-037 - Actividad del impuesto padre

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/catalogo-validator.service.ts`
- Function / Method / Procedure: DDL de impuestos/tarifas; `loadCache`; `validateImpuesto`
- Line / Section: dump 126-137, 172-183; catalogo 84-100, 343-363
- Condition / Query / Statement: `catalogo_impuestos` tiene `activo`, pero la consulta unida solo filtra `t.activo` y la presencia en el mapa determina validez.
- Confidence: High

### PFV-038 - Retencion de DTO en Redis

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: `SriService.emitirFactura`; registro `sri-emision`; configuracion de colas; servicio Redis
- Line / Section: SriService 56-67; QueueModule 29-49; configuracion 138-145; compose 21-40, 102-104
- Condition / Query / Statement: el job contiene el DTO completo, los defaults retienen completados/fallidos por conteo y Redis conserva AOF en volumen.
- Confidence: High

### PFV-039 - Alcance de administracion P12

**Evidence**
- File: `src/app.module.ts`; `src/modules/auth/guards/roles.guard.ts`; `src/modules/certificate/certificate.controller.ts`
- Function / Method / Procedure: guardas globales; `RolesGuard.canActivate`; endpoints de certificados; `bindCertificateToEmisor`
- Line / Section: app 120-135; roles guard 16-48; certificate controller 63-173, 190-287, 323-421
- Condition / Query / Statement: no hay `@Roles` en el controller; acceso por RUC solo se valida al vincular y delete limpia por nombre de archivo sin tenant.
- Confidence: High

## Estado

Todos los elementos `PFV-001` a `PFV-039` permanecen abiertos. Resolverlos requiere confirmacion de responsables funcionales, operativos o de seguridad y, en varios casos, evidencia del entorno desplegado.
