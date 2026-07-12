# 05 - Reglas de negocio

## Alcance del lote

Este lote documenta el comportamiento AS-IS del flujo común SRI y de la
emisión de facturas. El análisis es estático y cubre el acceso HTTP, la
preparación de la factura, el cálculo de totales, la construcción y firma del
XML, la interacción con el SRI, la persistencia y los modos de vista previa y
depuración.

Las reglas describen lo que ejecuta el código actual. Las decisiones cuya
intención funcional no puede confirmarse se mantienen en
`14-pending-functional-validation.md`; no se consideran requisitos confirmados.

Los IDs `BR-001` a `BR-017` son estables para este inventario.

## Inventario de reglas

| ID | Área | Regla AS-IS | Trigger / Contexto | Confianza | PFV relacionados |
|---|---|---|---|---|---|
| BR-001 | Acceso a emisión | La emisión HTTP de una factura solo se delega después de buscar un emisor por RUC. `SUPERADMIN` puede usar la fila encontrada; los demás usuarios requieren `tenantId` y se rechazan solo si esa fila tiene un tenant no nulo diferente. Una fila con tenant nulo supera esta comprobación. | `POST /sri/emitir/factura` | High | PFV-036 |
| BR-002 | Validación previa | Antes de construir la factura se valida la identificación del comprador, el tipo de identificación en catálogo, cada par de código de impuesto y porcentaje, y las formas de pago presentes. Un resultado inválido detiene el flujo con `BadRequestException`. | Inicio de `FacturaService.emitirFactura` | High | PFV-019, PFV-021, PFV-037 |
| BR-003 | Ambiente y tipo de emisión | El ambiente recibido en el DTO prevalece. Si no se informa, solo el valor de configuración `production` selecciona producción; cualquier otro valor selecciona pruebas. El tipo de emisión predeterminado es normal. | Preparación de factura | High | PFV-022 |
| BR-004 | Secuencial | Un secuencial recibido se completa con ceros a nueve dígitos. Si no se recibe, se reserva atómicamente el siguiente secuencial para el punto retornado por el repositorio; en cache miss, esa búsqueda exige establecimiento y punto activos. | Antes de generar la clave de acceso | High | PFV-023, PFV-033 |
| BR-005 | Clave de acceso | La clave de factura usa el tipo de comprobante `01` y concatena fecha, RUC, ambiente, establecimiento, punto de emisión, secuencial, código numérico, tipo de emisión y dígito verificador Módulo 11. El flujo de factura no proporciona código numérico, por lo que genera uno aleatorio nuevo de ocho dígitos. | Después de resolver el secuencial | High | PFV-020, PFV-032 |
| BR-006 | Neto por detalle | Para cada detalle, el subtotal es `cantidad * precioUnitario`; el descuento no puede ser mayor que ese subtotal y el precio total sin impuesto es el subtotal menos el descuento. | Construcción de detalles | High | PFV-021 |
| BR-007 | Totales de factura | El total sin impuestos suma los netos de los detalles; el total de descuento suma sus descuentos; los impuestos recibidos se agrupan por `codigo-codigoPorcentaje`; el importe total suma el total sin impuestos y los valores de impuesto agrupados. Los resultados se redondean a dos decimales. | Construcción de `InfoFactura` | High | PFV-021 |
| BR-008 | Contenido fiscal y adicional | La moneda se fija en `DOLAR`. Email, teléfono y dirección del comprador, cuando existen, se agregan al XML como campos adicionales y luego se anexan los campos adicionales recibidos en el DTO. | Construcción del objeto factura | High | PFV-022 |
| BR-009 | Representación XML | La factura se construye con raíz `factura`, id `comprobante` y versión `1.1.0`. Cantidad y precio unitario se representan con seis decimales; importes, bases, tarifas y valores monetarios con dos. | Construcción del XML sin firma | High | PFV-019 |
| BR-010 | Certificado y firma | La emisión exige metadatos de certificado en el emisor resuelto. En cache miss, repositorio y firmador consultan una fila activa por RUC; ambos aceptan previamente sus caches sin revalidar estado. El firmador carga el P12 y firma el XML. | Antes del envío al SRI | High | PFV-012, PFV-029, PFV-036 |
| BR-011 | Resolución de estado SRI | Una recepción `DEVUELTA` termina sin consultar autorización. En autorización, `AUTORIZADO` produce éxito, `NO AUTORIZADO` produce resultado terminal sin éxito y el agotamiento de consultas sin estado terminal produce `EN PROCESO`. | Envío y autorización SOAP | High | PFV-025, PFV-034 |
| BR-012 | Excepción SRI | Si `enviarYAutorizar` lanza cualquier excepción, se etiqueta `SRI_TIMEOUT` y se intenta persistir `PENDIENTE` cuando existen emisor y punto. Si esa persistencia termina, se relanza la excepción original; si falla, se propaga el error de persistencia. | Excepción durante recepción o autorización | High | PFV-005, PFV-006, PFV-031, PFV-032, PFV-034, PFV-035 |
| BR-013 | Eventos de resultado | Tras la fase condicional de persistencia se emite `comprobante.autorizado` cuando el resultado indica éxito o `AUTORIZADO`, y `comprobante.rechazado` solo para `RECHAZADO` o `DEVUELTA`. Si falta punto en la rama manual, esta evaluación puede ocurrir sin registro local. | Resultado SRI no excepcional | High | PFV-004, PFV-023, PFV-025, PFV-035 |
| BR-014 | Grafo de persistencia | Cuando existen emisor y punto de emisión, el comprobante, sus detalles, impuestos, detalles adicionales, totales, pagos, referencia de XML e información adicional se escriben mediante una misma transacción de base de datos. Un error revierte esa transacción, emite `comprobante.persistencia_fallida` y se propaga. | Resultado SRI o persistencia `PENDIENTE` | High | PFV-006, PFV-023, PFV-032, PFV-035 |
| BR-015 | Almacenamiento XML | Durante la persistencia se guarda siempre el XML firmado. El XML autorizado solo se guarda cuando el resultado lo contiene y el XML sin firma se omite. Las rutas se organizan por RUC, año, mes y tipo, y se registran en `comprobante_xmls`. | Persistencia de factura | High | PFV-015, PFV-035 |
| BR-016 | Número de autorización local | El registro local usa el número de autorización devuelto por el SRI; cuando no existe, almacena la clave de acceso como `numero_autorizacion`. La respuesta API no aplica ese fallback. | Creación de `comprobantes` y mapeo de respuesta | High | PFV-024 |
| BR-017 | Preview y depuración | Preview y debug requieren un secuencial proporcionado. Preview genera XML sin firmar ni enviar. Debug está deshabilitado cuando `NODE_ENV` es `production`, genera XML sin firma y firmado, y no lo envía al SRI ni lo persiste. Ambos endpoints validan acceso al RUC. | `POST /sri/preview/factura`; `POST /sri/debug/factura-firmada` | High | PFV-022, PFV-029 |

## Evidencia detallada

### BR-001 - Acceso a emisión

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/emisores/emisores.service.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; `EmisoresService.validateRucAccess`
- Line / Section: controlador 103-110; servicio 227-251
- Condition / Query / Statement: el controlador valida el RUC antes de delegar; `findByRuc` no recibe tenant y la condición de acceso permite `SUPERADMIN` o, para otros roles con `tenantId`, una fila con tenant coincidente o nulo.
- Confidence: High

### BR-002 - Validación previa

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriBaseService.validarIdentificacion`; validadores de catálogo
- Line / Section: factura 48-64; base 38-101, 133-175
- Condition / Query / Statement: la identificación se valida sincrónicamente y las consultas de catálogo se esperan antes de resolver secuencial, clave, XML o llamada SRI.
- Confidence: High

### BR-003 - Ambiente y tipo de emisión

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/constants/sri.enums.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriBaseService.getDefaultAmbiente`; enums `Ambiente` y `TipoEmision`
- Line / Section: factura 67-74; base 26-32; enums 17-28
- Condition / Query / Statement: el DTO tiene precedencia; el fallback de ambiente compara literalmente con `production` y el tipo de emisión usa `NORMAL` cuando falta.
- Confidence: High

### BR-004 - Secuencial

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriRepositoryService.findPuntoEmision`; `getNextSecuencial`
- Line / Section: factura 76-104; repositorio 165-219
- Condition / Query / Statement: la rama manual solo aplica `padStart`; la automática exige `puntoEmisionInfo` y ejecuta un upsert atómico; `findPuntoEmision` retorna cache antes de su consulta por estados activos.
- Confidence: High

### BR-005 - Clave de acceso

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/modules/sri/constants/sri.enums.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `ClaveAccesoService.generate`; `generateCodigoNumerico`; `calculateModulo11`
- Line / Section: factura 108-117; clave 28-56, 121-145; enums 4-12
- Condition / Query / Statement: el servicio pasa `FACTURA` y los componentes resueltos sin `codigoNumerico`; el generador crea ese código, concatena los campos y añade el verificador.
- Confidence: High

### BR-006 - Neto por detalle

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.buildDetalles`
- Line / Section: 612-643
- Condition / Query / Statement: cada detalle calcula subtotal y neto; un descuento superior al subtotal lanza `BadRequestException`.
- Confidence: High

### BR-007 - Totales de factura

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/factura-totales.spec.ts`
- Function / Method / Procedure: `FacturaService.calculateTotales`; pruebas unitarias de totales
- Line / Section: servicio 645-710; pruebas 15-217
- Condition / Query / Statement: `Decimal` acumula netos, descuentos, bases y valores por clave de impuesto; los resultados y el importe total se redondean a dos decimales.
- Confidence: High

### BR-008 - Contenido fiscal y adicional

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.buildFacturaFromDto`
- Line / Section: 541-607
- Condition / Query / Statement: `InfoFactura` fija `DOLAR`; los datos de contacto se agregan primero y después se anexan los campos adicionales del DTO.
- Confidence: High

### BR-009 - Representación XML

**Evidence**
- File: `src/modules/sri/services/xml-builder.service.ts`; `src/modules/sri/constants/sri-endpoints.constant.ts`
- Function / Method / Procedure: `XmlBuilderService.buildFactura`; `buildInfoFactura`; `buildDetalleFactura`; `formatDecimal`
- Line / Section: builder 50-90, 122-238; constantes 25-32
- Condition / Query / Statement: el constructor fija raíz, id y versión, y aplica las precisiones decimales indicadas a cabecera, detalles e impuestos.
- Confidence: High

### BR-010 - Certificado y firma

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `XmlSignerService.loadEmisorCertificate`; `signXmlForEmisor`
- Line / Section: factura 128-145; firmador 285-385, 388-430
- Condition / Query / Statement: la falta de metadatos detiene la emisión; repositorio y firmador retornan primero sus caches y, en cache miss, consultan por RUC/estado activo antes de cargar y firmar.
- Confidence: High

### BR-011 - Resolución de estado SRI

**Evidence**
- File: `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: `SriSoapClient.enviarYAutorizar`
- Line / Section: 84-213
- Condition / Query / Statement: recepción `DEVUELTA`, autorización positiva, autorización negativa y agotamiento de consultas retornan los estados descritos.
- Confidence: High

### BR-012 - Excepción SRI

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriService.emitirFactura`; `SriEmisionProcessor.process`
- Line / Section: factura 146-185; SriService 56-67; processor 24-46
- Condition / Query / Statement: FacturaService sintetiza `SRI_TIMEOUT`, intenta la transacción `PENDIENTE` cuando hay punto y solo alcanza `throw error` si esa operación no lanza otra excepción; BullMQ invoca el mismo método.
- Confidence: High

### BR-013 - Eventos de resultado

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; listeners `handleComprobanteAutorizado` y `handleComprobanteRechazado`
- Line / Section: factura 187-228; webhooks 33-55
- Condition / Query / Statement: la persistencia precedente está bajo `if (emisor && puntoEmisionInfo)`, mientras las comparaciones de eventos quedan fuera de ese guard y los listeners convierten los eventos en jobs webhook.
- Confidence: High

### BR-014 - Grafo de persistencia

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `SriRepositoryService.executeInTransaction`; `DatabaseService.transaction`
- Line / Section: factura 187-205, 340-520; repositorio 426-430; database 168-185
- Condition / Query / Statement: los repositorios reciben el mismo `PoolClient`; el wrapper confirma al completar o revierte ante excepción, y el catch de factura emite la alerta de persistencia fallida.
- Confidence: High

### BR-015 - Almacenamiento XML

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/modules/sri/services/sri-repository.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `XmlStorageService.saveAllXmls`; `saveXml`; `SriRepositoryService.saveXml`
- Line / Section: factura 470-491; storage 30-130; repositorio 358-373
- Condition / Query / Statement: factura pasa `undefined` para el XML sin firma, siempre pasa el firmado y pasa el autorizado solo si está presente; las rutas retornadas se insertan o actualizan en `comprobante_xmls`.
- Confidence: High

### BR-016 - Número de autorización local

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `mapResultToResponse`
- Line / Section: 355-382, 713-722
- Condition / Query / Statement: la inserción usa `resultado.numeroAutorizacion || claveAcceso`; el DTO de respuesta conserva directamente `resultado.numeroAutorizacion`.
- Confidence: High

### BR-017 - Preview y depuración

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `SriController.previewFactura`; `debugFacturaFirmada`; `FacturaService.generarXmlPreview`; `generarFacturaFirmadaDebug`
- Line / Section: controlador 245-265, 306-328; factura 237-335; SriService 70-80
- Condition / Query / Statement: ambos endpoints validan acceso; los dos métodos requieren secuencial, preview solo construye XML y debug construye y firma sin llamar al cliente SOAP ni a la persistencia.
- Confidence: High

## Límites de interpretación

- Los valores numéricos de impuestos y pagos se documentan como datos que el
  flujo actual consume, no como cálculos fiscalmente confirmados; véase
  `PFV-021`.
- La inclusión de la fecha en la clave no confirma que la validación de formato
  actual garantice una fecha calendaria válida; véase `PFV-020`.
- Los datos fiscales suministrados en el DTO no se promueven a fuente maestra;
  véase `PFV-022`.
- Los estados y fallbacks locales se describen literalmente y no implican que
  su vocabulario sea el contrato funcional deseado; véanse `PFV-024` y
  `PFV-025`.
- La ejecución asíncrona, sus opciones efectivas y su idempotencia permanecen
  sujetas a `PFV-005`, `PFV-031` y `PFV-032`.
- La unicidad y selección del emisor por RUC entre tenants, incluido el alcance
  de sus caches, permanecen en `PFV-036`.
- La activación del impuesto padre de una tarifa permanece en `PFV-037`.
- El acceso y la retención del DTO fiscal completo en BullMQ/Redis permanecen
  en `PFV-038`.
- El código HTTP efectivo de la emisión síncrona o asíncrona permanece sujeto a
  `PFV-007`; no forma parte de las reglas fiscales confirmadas de este lote.
