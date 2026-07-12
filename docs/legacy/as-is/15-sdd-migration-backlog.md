# 15 - Backlog de preservacion para SDD

## Alcance y criterio de uso

Este backlog convierte los comportamientos AS-IS confirmados del lote 2 en
insumos trazables para una especificacion posterior. Cubre el flujo comun SRI
solo en la medida en que es ejercido por Factura: acceso, despacho, validacion,
calculo, secuencial, clave de acceso, XML, firma, SOAP, persistencia, eventos,
respuesta, preview, debug y datos de cola.

Los items no describen una arquitectura destino ni convierten defectos o
ambiguedades en requisitos. Cuando el codigo muestra una brecha, contradiccion
o politica no confirmada, su eventual preservacion queda condicionada a cerrar
los `PFV-*` relacionados.

| ID | Comportamiento base | Dependencias AS-IS principales | Estado para SDD | PFV relacionados |
|---|---|---|---|---|
| MIG-001 | Contrato y bifurcacion sincrona/asincrona de emision | HTTP, configuracion, `SriService`, BullMQ | Parcialmente listo; contrato externo condicionado | PFV-001, PFV-005, PFV-007, PFV-031 |
| MIG-002 | Acceso por RUC/tenant y limite de solicitudes | JWT, usuario actual, `EmisoresService`, throttling | Caracterizacion lista; politica de aislamiento condicionada | PFV-036 |
| MIG-003 | Validaciones HTTP, de identificacion y catalogos | `ValidationPipe`, DTO, catalogos PostgreSQL/cache | Parcialmente listo; brechas funcionales condicionadas | PFV-005, PFV-019, PFV-020, PFV-026, PFV-027, PFV-028, PFV-037 |
| MIG-004 | Calculo de detalles, impuestos y totales | DTO Factura, `Decimal`, datos fiscales de solicitud | Algoritmo confirmado; autoridad y consistencia condicionadas | PFV-021, PFV-022 |
| MIG-005 | Reserva de secuencial y generacion de clave | Punto de emision, PostgreSQL, aleatoriedad, Modulo 11 | Camino automatico listo; camino manual/retry condicionado | PFV-020, PFV-023, PFV-030, PFV-032, PFV-033 |
| MIG-006 | Representacion XML y firma del emisor | Builder XML, P12, filesystem, password cifrado, cache | Formato confirmado; fuente/vigencia condicionadas | PFV-012, PFV-022, PFV-029, PFV-036 |
| MIG-007 | Recepcion y autorizacion SOAP | Clientes SOAP, configuracion SRI, esperas/backoff | Matriz observada; destinos, estados y timeouts condicionados | PFV-010, PFV-025, PFV-034 |
| MIG-008 | Persistencia relacional y de XML | PostgreSQL, filesystem, emisor y punto de emision | Grafo confirmado; reconciliacion y casos manuales condicionados | PFV-006, PFV-015, PFV-023, PFV-024, PFV-035 |
| MIG-009 | Eventos, errores y respuesta de Factura | Event emitter, webhooks, resultado SRI | Mapeo observado; cobertura y vocabulario condicionados | PFV-004, PFV-005, PFV-006, PFV-024, PFV-025, PFV-035 |
| MIG-010 | Preview y debug firmado | Acceso por RUC, builder XML, firmador, configuracion | Listo como caracterizacion; politicas fiscales condicionadas | PFV-020, PFV-022, PFV-029, PFV-036 |
| MIG-011 | Payload, ejecucion y retencion declarada de la cola | BullMQ, Redis, processor, configuracion de colas | Caracterizacion parcial; politica efectiva condicionada | PFV-016, PFV-031, PFV-032, PFV-038 |

## MIG-001 - Preservar la bifurcacion observable de emision

**Comportamiento AS-IS a preservar**

El handler de factura recibe un `CreateFacturaDto`, valida acceso al RUC y
delega en `SriService`. Solo el literal de configuracion
`SRI_EMISION_ASYNC === 'false'` ejecuta `FacturaService` dentro de la solicitud.
Cualquier otro valor, incluida la ausencia de la variable, crea un job
`emision` y responde con `mensaje`, `jobId` y estado `EN_COLA`. El controller
fija actualmente codigo 201, aunque la descripcion OpenAPI tambien presenta
una respuesta sincrona 200.

**Dependencias AS-IS**

- `SriController`, `SriService` y `FacturaService`.
- `ConfigService` y la variable literal `SRI_EMISION_ASYNC`.
- Cola BullMQ `sri-emision` y `SriEmisionProcessor` para el camino asincrono.

**Riesgos**

- Cambiar la condicion literal altera cuando las validaciones y errores son
  visibles en HTTP.
- El cliente asincrono recibe un identificador, pero no hay un mecanismo de
  consulta de resultado final confirmado en este lote.
- La ruta publicada y el codigo HTTP sincronico tienen evidencia
  contradictoria fuera del handler.

**Consideraciones de aceptacion**

- Caracterizar por separado configuracion igual a `false`, ausente y con otro
  valor, verificando ejecucion directa frente a creacion de job.
- Conservar las dos formas de respuesta observadas como linea base de
  compatibilidad.
- No fijar como requisito definitivo la ruta publica, el codigo HTTP sincronico,
  la consulta de resultado ni los retries de cola hasta resolver `PFV-001`,
  `PFV-005`, `PFV-007` y `PFV-031`.

**PFV relacionados:** `PFV-001`, `PFV-005`, `PFV-007`, `PFV-031`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; `SriService.emitirFactura`; `SriEmisionProcessor.process`
- Line / Section: controller 84-110; service 56-67; processor 24-46
- Condition / Query / Statement: el controller fija 201 y delega tras validar el RUC; el servicio solo ejecuta directamente cuando el valor es exactamente `false`, de lo contrario encola `{ tipo: 'FACTURA', dto }`; el processor invoca `FacturaService`.
- Confidence: High

**Evidence**
- File: `README.md`; `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`; `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: ejemplos de emision; `SriController.emitirFactura`; metodos de emision de `SriService`; request de emision en la coleccion
- Line / Section: README 22, 174, 262, 299; controller 84-110; SriService 56-67, 86-99, 106-119, 126-139, 146-159; collection 869, 898
- Condition / Query / Statement: los artefactos publican dos formas de ruta y respuestas 200/201; la busqueda dirigida en controller/service no encontro consulta de job, `QueueEvents` ni ruta de resultado final.
- Confidence: Medium

## MIG-002 - Preservar como linea base el control literal de acceso al RUC

**Comportamiento AS-IS a preservar**

La aplicacion aplica JWT globalmente y el endpoint limita diez solicitudes por
60 segundos. Antes de emitir, preview o debug, busca el emisor solo por RUC. Un
usuario `SUPERADMIN` supera la comprobacion. Para los otros roles, la ausencia
de `tenantId` se rechaza; tambien se rechaza un tenant distinto cuando el
emisor tiene tenant no nulo. Un emisor con tenant nulo supera esa segunda
comparacion. Emisor inexistente produce 404 y acceso rechazado produce 403.

Esta es una caracterizacion de seguridad actual, no la confirmacion de que el
acceso global por RUC o el caso tenant nulo deban mantenerse.

**Dependencias AS-IS**

- `JwtAuthGuard`, `ThrottlerGuard` y el `JwtPayload` del usuario actual.
- `EmisoresService.findByRuc` y `validateRucAccess` sobre PostgreSQL.
- RUC contenido en el bloque `emisor` de la solicitud.

**Riesgos**

- La seleccion solo por RUC y la aceptacion de tenant nulo pueden cruzar el
  limite esperado entre tenants.
- Cambiar la regla sin confirmar unicidad y propiedad del RUC puede bloquear
  emisores validos o autorizar el emisor equivocado.

**Consideraciones de aceptacion**

- Mantener casos de caracterizacion para `SUPERADMIN`, usuario sin tenant,
  tenant coincidente, tenant distinto, emisor con tenant nulo y RUC inexistente.
- Preservar el limite especifico de diez solicitudes por minuto mientras no se
  cambie expresamente el contrato operativo.
- Convertir la semantica de aislamiento en requisito funcional solo despues de
  resolver `PFV-036`.

**PFV relacionados:** `PFV-036`.

**Evidence**
- File: `src/app.module.ts`; `src/modules/sri/sri.controller.ts`; `src/modules/emisores/emisores.service.ts`
- Function / Method / Procedure: registro global de guards; `SriController.emitirFactura`, `previewFactura`, `debugFacturaFirmada`; `EmisoresService.validateRucAccess`
- Line / Section: AppModule 120-140; controller 84-110, 245-264, 306-327; emisores 227-258
- Condition / Query / Statement: JWT y throttling son guards globales; factura declara limite 10/60000; los tres handlers validan el RUC y la condicion de tenant acepta una fila con tenant nulo para un usuario que si posee tenant.
- Confidence: High

## MIG-003 - Preservar la secuencia confirmada de validaciones

**Comportamiento AS-IS a preservar**

La tuberia HTTP transforma el payload, aplica lista blanca y rechaza
propiedades no declaradas. Los DTO restringen enums, formatos y tipos basicos;
despues `FacturaService` valida la identificacion del comprador, la existencia
activa del tipo de identificacion, cada pareja impuesto/porcentaje y las formas
de pago informadas. El descuento mayor que `cantidad * precioUnitario` se
rechaza. En modo sincronico estas validaciones de servicio pueden responder
durante la solicitud; en modo asincrono ocurren despues de responder `EN_COLA`.

No se confirma como requisito preservar las brechas actuales: bloques padre o
colecciones sin cardinalidad minima, ceros permitidos, fecha solo por patron,
tipo `09` aceptado por DTO pero rechazado por algoritmo, metadatos de
identificacion no aplicados, vigencia inicial de tarifa ignorada o tarifa activa
sin comprobar el impuesto padre.

**Dependencias AS-IS**

- `ValidationPipe`, `CreateFacturaDto` y DTO comunes.
- `SriBaseService`, `IdentificacionValidatorService` y caches/catalogos de
  PostgreSQL.
- Momento de ejecucion elegido por MIG-001.

**Riesgos**

- Una especificacion derivada solo de los decoradores aceptaria documentos
  vacios o inconsistentes como si esa fuera una decision funcional.
- Sincronico y asincrono presentan el mismo fallo en canales y momentos
  diferentes.
- Catalogos y algoritmos pueden discrepar para una misma identificacion o
  tarifa.

**Consideraciones de aceptacion**

- Construir una matriz de caracterizacion que distinga validacion HTTP,
  identificacion algoritmica, catalogos y descuento calculado.
- Verificar el momento observado del rechazo tanto en ejecucion directa como
  en job.
- Mantener las brechas como casos pendientes, no como resultados obligatorios,
  hasta cerrar los PFV relacionados.

**PFV relacionados:** `PFV-005`, `PFV-019`, `PFV-020`, `PFV-026`, `PFV-027`,
`PFV-028`, `PFV-037`.

**Evidence**
- File: `src/main.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `bootstrap`; DTO `CreateFacturaDto` y DTO comunes; `FacturaService.emitirFactura`, `buildDetalles`; validadores de `SriBaseService`
- Line / Section: main 53-60; factura DTO 20-95; common DTO 17-243; factura service 45-64, 612-642; base 38-101, 133-175
- Condition / Query / Statement: la tuberia aplica transformacion/lista blanca; decoradores validan forma; el servicio ejecuta identificacion y catalogos antes de generar la clave y rechaza descuento superior al subtotal.
- Confidence: High

## MIG-004 - Preservar el algoritmo observado de calculo y armado fiscal

**Comportamiento AS-IS a preservar**

Por detalle, el subtotal es `cantidad * precioUnitario` y el neto sin impuesto
es subtotal menos descuento. Los totales usan `Decimal`: suman netos y
descuentos, agrupan bases y valores tributarios por
`codigo-codigoPorcentaje`, y calculan el importe total como total sin impuestos
mas impuestos agrupados. Los resultados se redondean a dos decimales. La moneda
se fija en `DOLAR`; email, telefono y direccion del comprador se agregan como
informacion adicional cuando existen.

Las tarifas, bases, valores de impuesto y pagos se consumen desde el DTO sin una
reconciliacion aritmetica cruzada confirmada. Los datos fiscales del emisor y el
ambiente usados en el XML tambien provienen del DTO o configuracion, mientras
el maestro aporta el ID y certificado. Preservar estas autoridades actuales
queda condicionado a `PFV-021` y `PFV-022`.

**Dependencias AS-IS**

- Detalles, impuestos, pagos, comprador y emisor del DTO.
- Biblioteca `Decimal` y `FacturaService`.
- Catalogos validados por MIG-003.

**Riesgos**

- Recalcular de forma distinta cambia XML, importes persistidos y respuesta del
  SRI.
- Copiar las brechas de consistencia puede perpetuar facturas aritmeticamente
  incompatibles con la politica fiscal esperada.
- Tratar el DTO como maestro puede emitir datos distintos de la fila del emisor.

**Consideraciones de aceptacion**

- Usar casos de caracterizacion con varios detalles, descuentos y dos o mas
  impuestos que compartan o no la clave de agrupacion.
- Comparar netos, descuentos, bases agrupadas, impuesto agrupado, importe total
  y redondeo contra el algoritmo actual.
- Definir validaciones cruzadas y fuente maestra solo despues de resolver
  `PFV-021` y `PFV-022`.

**PFV relacionados:** `PFV-021`, `PFV-022`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `buildFacturaFromDto`; `buildDetalles`; `calculateTotales`
- Line / Section: 55-64, 119-144, 526-710
- Condition / Query / Statement: el maestro recuperado se usa para ID y presencia de certificado mientras el armado copia datos fiscales del DTO, fija `DOLAR`, calcula netos, agrega campos adicionales y agrupa impuestos con `Decimal` antes de redondear.
- Confidence: High

## MIG-005 - Preservar secuencial y composicion de clave de acceso

**Comportamiento AS-IS a preservar**

Un secuencial explicito se completa a nueve digitos y no actualiza el contador.
Si se omite, el servicio exige un punto de emision resuelto y reserva el
siguiente valor mediante un upsert atomico por punto y tipo de comprobante
`01`, en una transaccion que termina antes de certificado, firma y SOAP. La
clave de Factura contiene 49 digitos y concatena fecha, tipo `01`, RUC,
ambiente, establecimiento, punto, secuencial, codigo numerico aleatorio de ocho
digitos, tipo de emision y verificador Modulo 11.

**Dependencias AS-IS**

- Emisor, establecimiento y punto de emision.
- Tablas `puntos_emision` y `secuenciales`, y transacciones PostgreSQL.
- `ClaveAccesoService`, generador aleatorio y fecha convertida desde el DTO.

**Riesgos**

- Reejecutar un job puede consumir otro secuencial y generar otra clave.
- Fallar despues de confirmar la reserva deja un hueco.
- La rama manual puede continuar sin punto persistible y no sincroniza el
  contador automatico.
- Otras rutas por clave aplican una validacion menor que el checksum disponible.

**Consideraciones de aceptacion**

- Caracterizar padding manual, primera reserva automatica, incremento
  concurrente, tipo `01`, longitud, posiciones y checksum de la clave.
- No fijar reutilizacion en retries, aceptacion de huecos, semantica manual ni
  politica uniforme de validacion de claves hasta resolver `PFV-023`,
  `PFV-030`, `PFV-032` y `PFV-033`.
- Resolver `PFV-020` antes de asumir que toda fecha aceptada genera componentes
  de clave y XML equivalentes.

**PFV relacionados:** `PFV-020`, `PFV-023`, `PFV-030`, `PFV-032`, `PFV-033`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: rama de secuencial de `FacturaService.emitirFactura`; `getNextSecuencial`; `ClaveAccesoService.generate`, `generateCodigoNumerico`, `calculateModulo11`; `DatabaseService.transaction`
- Line / Section: factura 85-117; repository 199-219; clave 28-56, 121-144; database 171-185
- Condition / Query / Statement: la rama manual solo aplica padding; la automatica confirma un upsert antes de firma; el generador concatena los componentes, crea el codigo numerico y agrega Modulo 11.
- Confidence: High

## MIG-006 - Preservar la representacion XML y la firma observadas

**Comportamiento AS-IS a preservar**

Factura se serializa con raiz `factura`, atributo `id="comprobante"` y version
`1.1.0`. Cantidad y precio unitario usan seis decimales; importes, tarifas,
bases y valores usan dos. Antes de enviar, el flujo exige metadatos de
certificado en el emisor. El firmador busca por RUC una fila activa en cache
miss, descifra el password, lee el P12 del filesystem, selecciona una clave y
certificado no CA, y agrega una firma XAdES con RSA-SHA1 y referencia SHA-1.

**Dependencias AS-IS**

- Objeto Factura producido por MIG-004 y clave producida por MIG-005.
- `XmlBuilderService`, constante de version y `XmlSignerService`.
- Metadatos en PostgreSQL, password cifrado, archivo P12 y cache en memoria.

**Riesgos**

- Cambiar orden, precision, version o algoritmos puede producir un comprobante
  distinto del aceptado por el flujo actual.
- El binario en base de datos y el archivo P12 no tienen autoridad confirmada.
- La carga para firma no comprueba fechas de vigencia del certificado.
- La seleccion por RUC comparte la incertidumbre de aislamiento de emisores.

**Consideraciones de aceptacion**

- Comparar estructura, campos opcionales, version y precision con muestras
  generadas por el comportamiento actual.
- Verificar que la firma se incorpora sobre la referencia del comprobante y
  que el XML firmado conserva compatibilidad con el SRI.
- Condicionar la fuente autoritativa del certificado, su vigencia en cada firma
  y la fuente de datos fiscales a `PFV-012`, `PFV-022` y `PFV-029`.
- No convertir la seleccion global por RUC en requisito hasta cerrar
  `PFV-036`.

**PFV relacionados:** `PFV-012`, `PFV-022`, `PFV-029`, `PFV-036`.

**Evidence**
- File: `src/modules/sri/services/xml-builder.service.ts`; `src/modules/sri/constants/sri-endpoints.constant.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: `XmlBuilderService.buildFactura`, `buildInfoFactura`, `buildDetalleFactura`; `FACTURA_VERSION`; `XmlSignerService.loadEmisorCertificate`, `signXmlForEmisor`
- Line / Section: builder 50-89, 122-237; constant 25-30; signer 285-454
- Condition / Query / Statement: el builder fija raiz, id, version y precision; el firmador carga P12 por RUC y agrega una firma XAdES usando RSA-SHA1 y SHA-1.
- Confidence: High

## MIG-007 - Preservar la matriz observada de recepcion y autorizacion SRI

**Comportamiento AS-IS a preservar**

Recepcion envia el XML firmado en Base64 y reintenta toda excepcion capturada
con espera y backoff. Una respuesta `DEVUELTA` termina sin consultar
autorizacion. En los demas casos se sondea por clave: `AUTORIZADO` retorna
exito, `NO AUTORIZADO` retorna resultado terminal sin exito y agotar consultas
sin esos estados retorna `EN PROCESO`. Una excepcion de transporte durante una
consulta de autorizacion se propaga de inmediato y no continua el bucle.

La configuracion declara retries, demoras y timeouts separados. El cliente lee
retries/demoras, pero no aplica los timeouts observados. La fabrica de llamadas
usa URLs constantes mientras configuracion y health usan WSDL configurados.

**Dependencias AS-IS**

- `SriSoapClient`, fabrica de clientes SOAP y clave de acceso.
- Configuracion de retries, demoras y multiplicador de backoff.
- Servicios SRI de recepcion y autorizacion.

**Riesgos**

- Cambiar estados terminales altera persistencia, eventos y respuesta.
- Los estados con espacios difieren de valores con guion bajo usados por otras
  consultas y reintentos.
- Timeouts no consumidos pueden dejar latencia sin el limite esperado.
- El health check puede observar un destino diferente del usado para emitir.

**Consideraciones de aceptacion**

- Caracterizar recepcion exitosa, `DEVUELTA`, errores repetidos de recepcion,
  `AUTORIZADO`, `NO AUTORIZADO`, agotamiento sin estado y excepcion durante
  autorizacion.
- Preservar como linea base los estados literales producidos, sin declararlos
  vocabulario canonico hasta resolver `PFV-025`.
- Definir autoridad de endpoints y semantica de timeout/retry solo despues de
  resolver `PFV-010` y `PFV-034`.

**PFV relacionados:** `PFV-010`, `PFV-025`, `PFV-034`.

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`; `src/config/configuration.ts`; `src/modules/sri/services/sri-soap-factory.service.ts`; `src/modules/status/sri.health.ts`
- Function / Method / Procedure: `validarComprobante`; `autorizarComprobante`; `enviarYAutorizar`; configuracion `sri.rateLimiting`; fabrica SOAP; `SriHealthIndicator.isHealthy`
- Line / Section: SOAP client 25-76, 84-227; configuration 40-66; factory 12-20, 27-65; health 21-40
- Condition / Query / Statement: el cliente implementa reintentos solo alrededor de recepcion, sondeo de autorizacion y retornos literales; los timeouts no son leidos; la fabrica usa URLs locales y health usa el WSDL configurado.
- Confidence: High

## MIG-008 - Preservar el grafo relacional y los artefactos XML

**Comportamiento AS-IS a preservar**

Despues del resultado SOAP, y solo cuando existen emisor y punto, una
transaccion PostgreSQL crea comprobante, detalles, impuestos, detalles
adicionales, totales, pagos, referencia de XML e informacion adicional. El
numero de autorizacion persistido usa la clave de acceso como fallback. Dentro
del callback transaccional se escribe siempre el XML firmado y el autorizado
solo cuando esta presente; no se guarda el XML sin firma. Las rutas se organizan
por RUC, ano, mes y subdirectorio de tipo.

Ante una excepcion SOAP, se construye un resultado `PENDIENTE` con mensaje
`SRI_TIMEOUT`, se intenta persistirlo cuando existen emisor y punto y luego se
relanza el error. PostgreSQL puede hacer rollback, pero los archivos ya escritos
no participan de esa transaccion.

**Dependencias AS-IS**

- PostgreSQL y el grafo de tablas de comprobante.
- `XmlStorageService`, directorio persistente y tabla `comprobante_xmls`.
- Emisor/punto resueltos, XML firmado y resultado de MIG-007.

**Riesgos**

- Un resultado SRI puede existir sin filas locales; una fila puede perder su
  XML; un archivo puede quedar huerfano tras rollback.
- Un secuencial manual sin punto puede omitir toda persistencia.
- El fallback de numero de autorizacion no coincide con el mapeo HTTP.
- No se encontro automatizacion de la retencion de siete anos declarada para
  XML.

**Consideraciones de aceptacion**

- Caracterizar el grafo y rutas para resultado autorizado, no autorizado,
  devuelto, en proceso y excepcion SOAP persistida como pendiente.
- Verificar rollback de filas por error y documentar separadamente el efecto ya
  ocurrido en filesystem.
- Mantener fallback, persistencia sin punto y divergencias solo como linea base
  hasta resolver `PFV-023`, `PFV-024` y `PFV-035`.
- Definir consumo de alertas y retencion efectiva despues de `PFV-006` y
  `PFV-015`.

**PFV relacionados:** `PFV-006`, `PFV-015`, `PFV-023`, `PFV-024`, `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`, `persistirFactura`; `XmlStorageService.saveAllXmls`, `saveXml`; `DatabaseService.transaction`
- Line / Section: factura 147-205, 340-520; storage 6-9, 30-130; database 171-185
- Condition / Query / Statement: el servicio persiste el grafo con un cliente transaccional, escribe XML durante ese callback y usa la clave como fallback; el catch SOAP intenta guardar `PENDIENTE` antes de relanzar.
- Confidence: High

## MIG-009 - Preservar el cierre observado mediante eventos y respuesta

**Comportamiento AS-IS a preservar**

Tras la persistencia condicional, `success` o estado `AUTORIZADO` emite
`comprobante.autorizado`; solo `RECHAZADO` o `DEVUELTA` emite
`comprobante.rechazado`. `NO AUTORIZADO` y `EN PROCESO` no generan esos eventos
en el flujo directo. Un fallo de persistencia emite
`comprobante.persistencia_fallida` y propaga el error. La respuesta de Factura
copia `success`, clave, estado, fecha, numero de autorizacion, XML autorizado y
mensajes desde el resultado SRI; no aplica el fallback usado por PostgreSQL.

**Dependencias AS-IS**

- Resultado de MIG-007 y finalizacion de MIG-008.
- `EventEmitter2`, listeners de webhooks y `mapResultToResponse`.
- En asincrono, resultado retornado internamente al job.

**Riesgos**

- Suscripciones para estados no emitidos pueden no recibir cierre.
- No hay consumidor confirmado del evento de persistencia fallida.
- La respuesta y la fila local pueden exponer distinto numero de autorizacion.
- El cliente asincrono no tiene recuperacion del resultado final confirmada.

**Consideraciones de aceptacion**

- Caracterizar una matriz de estado contra evento emitido, payload del evento y
  campos de respuesta.
- Verificar que los errores de persistencia se propagan despues de emitir la
  alerta.
- No ampliar/reducir estados notificables, aplicar el fallback a la respuesta ni
  definir cierre asincrono hasta resolver los PFV relacionados.

**PFV relacionados:** `PFV-004`, `PFV-005`, `PFV-006`, `PFV-024`, `PFV-025`,
`PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: bloque final de `FacturaService.emitirFactura`; `persistirFactura`; `mapResultToResponse`; listeners `handleComprobanteAutorizado` y `handleComprobanteRechazado`
- Line / Section: factura 207-230, 505-520, 713-722; webhooks 33-55
- Condition / Query / Statement: comparaciones literales disparan dos eventos de resultado; el catch de persistencia dispara la alerta; la respuesta copia el resultado y los listeners confirmados cubren autorizado/rechazado.
- Confidence: High

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: emision de `comprobante.persistencia_fallida` en `FacturaService.persistirFactura`; handlers `@OnEvent` de `WebhooksService`
- Line / Section: factura 505-520; webhooks 29-55
- Condition / Query / Statement: Factura emite la alerta y la busqueda dirigida en el consumidor de webhooks solo encontro listeners para `comprobante.autorizado` y `comprobante.rechazado`.
- Confidence: Medium

## MIG-010 - Preservar los modos preview y debug como flujos no emisores

**Comportamiento AS-IS a preservar**

Preview y debug exigen secuencial explicito, aplican padding, generan clave y
construyen el XML de Factura. Preview retorna solo XML sin firma. Debug retorna
clave, XML sin firma y XML firmado; el controller lo rechaza cuando
`NODE_ENV === 'production'`. Ninguno llama al SRI ni persiste comprobante. Ambos
handlers validan acceso al RUC. Estos servicios no repiten las validaciones de
identificacion, catalogos ni punto del flujo principal, aunque el payload si
atraviesa la validacion global de DTO.

**Dependencias AS-IS**

- DTO y acceso de MIG-002/MIG-003.
- Generacion de clave, builder XML y, solo para debug, firmador por RUC.
- Lectura literal de `NODE_ENV` en el controller de debug.

**Riesgos**

- Una vista previa puede representar datos que la emision principal rechazaria
  despues.
- Debug expone XML fiscal y firmado fuera de produccion.
- Fecha, datos del emisor, certificado y alcance por RUC comparten PFV del flujo
  principal.

**Consideraciones de aceptacion**

- Verificar secuencial obligatorio, padding y forma exacta de cada respuesta.
- Confirmar ausencia de llamadas SOAP y de escritura en PostgreSQL/filesystem
  de comprobantes para ambos modos.
- Verificar rechazo literal en `production` y firma solo en debug.
- No elevar la equivalencia fiscal entre preview/debug y emision a requisito
  hasta resolver `PFV-020`, `PFV-022`, `PFV-029` y `PFV-036`.

**PFV relacionados:** `PFV-020`, `PFV-022`, `PFV-029`, `PFV-036`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.previewFactura`, `debugFacturaFirmada`; `FacturaService.generarXmlPreview`, `generarFacturaFirmadaDebug`; delegados de `SriService`
- Line / Section: controller 245-264, 306-327; factura 237-335; SriService 70-80
- Condition / Query / Statement: los handlers validan RUC; los servicios exigen secuencial y se limitan a clave/XML/firma; debug tiene una guarda literal de produccion y ninguno invoca SOAP o persistencia.
- Confidence: High

## MIG-011 - Preservar como caracterizacion el contenido y ciclo de la cola

**Comportamiento AS-IS a preservar**

El job de emision almacena `{ tipo: 'FACTURA', dto }`, por lo que incluye el DTO
fiscal y personal completo. El processor vuelve a ejecutar todo
`FacturaService.emitirFactura` y relanza sus errores. La configuracion global
declara intentos, backoff exponencial y retencion por conteo de jobs completados
y fallidos. La composicion de produccion configura Redis con AOF, volumen
persistente y politica de eviction `allkeys-lru`.

La misma cola se vuelve a registrar en `SriModule` sin opciones. Por ello no se
confirma que los defaults globales sean los efectivos en runtime ni se eleva su
numero concreto a requisito.

**Dependencias AS-IS**

- BullMQ, Redis compartido con cache y `SriEmisionProcessor`.
- Registro global `QueueModule` y registro local `SriModule`.
- Opciones `queues.sriEmision` y almacenamiento Redis del despliegue.

**Riesgos**

- Retry completo sin identidad determinista puede repetir secuencial, clave,
  firma y envio SRI.
- El DTO completo puede conservar datos fiscales/personales mas tiempo o con
  mayor acceso del requerido.
- Eviction de Redis puede afectar jobs o historial.
- Dos registros dejan incierta la politica efectiva de intentos y retencion.

**Consideraciones de aceptacion**

- Caracterizar nombre, tipo, payload, despacho `FACTURA`, resultado interno y
  propagacion de errores del job.
- No fijar intentos, backoff, retencion, eviction ni reejecucion completa como
  requisitos definitivos hasta resolver `PFV-016`, `PFV-031`, `PFV-032` y
  `PFV-038`.
- Toda especificacion posterior debe tratar acceso y retencion del DTO como una
  decision de seguridad/privacidad pendiente, no como compatibilidad funcional
  ya aprobada.

**PFV relacionados:** `PFV-016`, `PFV-031`, `PFV-032`, `PFV-038`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/common/queues/queue.module.ts`; `src/config/configuration.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; registro global de `sri-emision`; configuracion `queues.sriEmision`
- Line / Section: SriService 56-67; processor 24-46; QueueModule 29-49; configuration 138-145
- Condition / Query / Statement: el job recibe tipo y DTO completos; el processor reejecuta Factura y relanza; el registro global declara attempts, backoff y retencion por conteo.
- Confidence: High

**Evidence**
- File: `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`; `docker-compose.prod.yml`
- Function / Method / Procedure: registros BullMQ de `sri-emision`; servicio y volumen Redis
- Line / Section: QueueModule 29-49; SriModule 27-32; compose 20-40, 102-104
- Condition / Query / Statement: el mismo nombre de cola se registra con y sin opciones; Redis de produccion declara AOF, volumen y `allkeys-lru`, pero no se verifico cual registro determina los defaults efectivos en runtime.
- Confidence: Medium

## Cobertura y siguiente lote

La cobertura de este backlog es parcial. Los `MIG-001` a `MIG-011` solo son
seguros como insumo SDD para el flujo de Factura y para componentes comunes
observados a traves de ese flujo. No confirman que Nota de Credito, Nota de
Debito, Retencion o Guia de Remision compartan todas las reglas, alternativas o
efectos.

El siguiente lote sugerido es **Nota de Credito**, incluyendo su referencia al
comprobante modificado, calculos, XML, firma, estados SRI, persistencia y eventos
propios. Este documento debera ampliarse con nuevos IDs `MIG-*` sin renumerar los
existentes cuando ese lote tenga evidencia suficiente.

No se ejecutaron la aplicacion, pruebas, jobs, consultas de base de datos,
migraciones ni llamadas externas para producir este backlog.
