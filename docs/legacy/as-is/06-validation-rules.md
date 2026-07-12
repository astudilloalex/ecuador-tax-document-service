# 06. Reglas de validacion

## Alcance del lote

Este documento registra las validaciones confirmadas para el flujo comun SRI y
la emision de facturas. El lote cubre la tuberia HTTP global, los DTO de factura,
las identificaciones, los catalogos almacenados en PostgreSQL, los calculos
monetarios, la clave de acceso, el acceso al RUC del emisor, las precondiciones
operativas de emision y el limite de solicitudes.

El analisis es estatico. No se ejecutaron la aplicacion, pruebas, jobs, consultas
contra una base desplegada ni llamadas al SRI. Las diferencias entre validacion
sincrona y asincrona se documentan expresamente.

## Resumen

| ID | Field / Object | Validation | Failure Behavior | Confidence | Related PFV |
|---|---|---|---|---|---|
| VR-001 | Payload HTTP | Transformacion, lista blanca y rechazo de propiedades no declaradas | Excepcion HTTP serializada por el filtro global | High | - |
| VR-002 | Emision de factura | Las validaciones de servicio ocurren antes de responder solo en modo sincrono | HTTP 400 en modo sincrono; job fallido despues del 201 en modo asincrono | High | PFV-005 |
| VR-003 | `ambiente`, `tipoEmision` | Valores opcionales restringidos a sus enums | HTTP 400 por DTO | High | - |
| VR-004 | `fechaEmision` | Patron `dd/mm/yyyy` | HTTP 400 si no coincide con el patron | High | PFV-020 |
| VR-005 | `secuencial` | Uno a nueve digitos o generacion automatica | Punto ausente: 400 sincrono/job fallido; fallo de reserva propagado: 500 para `Error` plano o 503 sin pool en sincrono, job fallido en asincrono | High | PFV-023, PFV-033 |
| VR-006 | Emisor DTO | Si el bloque existe, formato y obligatoriedad de sus datos tributarios basicos | HTTP 400 por campo invalido; el bloque ausente no se rechaza en DTO | High | PFV-019 |
| VR-007 | Comprador DTO | Si el bloque existe, tipo, identificacion y razon social; opcionales como strings | HTTP 400 por campo invalido; el bloque ausente no se rechaza en DTO | High | PFV-019, PFV-026, PFV-027 |
| VR-008 | Detalle de factura | Textos requeridos y numeros no negativos | HTTP 400 por DTO | High | PFV-019, PFV-021 |
| VR-009 | Impuesto de detalle | Codigos string y valores numericos no negativos | HTTP 400 por DTO | High | PFV-019, PFV-021 |
| VR-010 | Pago | Forma enumerada, total no negativo y plazo/unidad opcionales | HTTP 400 por DTO | High | PFV-019, PFV-021 |
| VR-011 | Informacion adicional | Nombre y valor no vacios con validacion anidada | HTTP 400 por DTO | High | - |
| VR-012 | Tipo de identificacion | Despacho por codigo `04` a `08` | HTTP 400 cuando el validador retorna invalido | High | PFV-026, PFV-027 |
| VR-013 | Cedula | Longitud, provincia, tercer digito y Modulo 10 | HTTP 400 mediante el wrapper comun | High | - |
| VR-014 | RUC del comprador tipo `04` | Longitud, provincia, tipo de entidad, sufijo y digito verificador | HTTP 400 sincrono o job fallido asincrono mediante el wrapper comun | High | PFV-022, PFV-036 |
| VR-015 | Pasaporte, consumidor final y exterior | Longitud o valor fijo segun tipo; exterior sin control estructural | HTTP 400 cuando aplica una regla y falla | High | PFV-027 |
| VR-016 | Catalogo de identificaciones | Existencia de codigo activo | HTTP 400 o job fallido | High | PFV-026, PFV-027 |
| VR-017 | Catalogo de impuestos | Existencia de una tarifa activa y no vencida para la pareja; no comprueba inicio de vigencia ni estado del impuesto padre | HTTP 400 o job fallido | High | PFV-021, PFV-028, PFV-037 |
| VR-018 | Catalogo de pagos | Existencia de cada forma de pago activa | HTTP 400 o job fallido | High | - |
| VR-019 | Descuento por detalle | No puede superar cantidad por precio unitario | HTTP 400 o job fallido | High | PFV-019, PFV-021 |
| VR-020 | Totales de factura | Agregacion y redondeo de valores suministrados | No contiene rechazo cruzado adicional | High | PFV-021 |
| VR-021 | Generacion de clave de acceso | RUC limpio de 13 digitos y digito Modulo 11 | `Error` para RUC de longitud invalida | High | - |
| VR-022 | `ClaveAccesoService.validate` | Longitud, contenido numerico y checksum Modulo 11 | Retorna `false`; `parse` retorna `null` | High | - |
| VR-023 | Clave de acceso en rutas HTTP | No vacia, 49 caracteres y solo digitos | `Error` plano serializado como HTTP 500 | High | PFV-030 |
| VR-024 | Preview y debug de factura | Secuencial explicito obligatorio | HTTP 400 | High | - |
| VR-025 | RUC y tenant | Busca solo por RUC; `SUPERADMIN` pasa; para otros roles, falta de tenant se rechaza y, con tenant, un emisor con tenant nulo pasa la guarda | HTTP 404 o 403 cuando aplica la guarda | High | PFV-036 |
| VR-026 | Emisor, punto y certificado | En cache miss filtra estados activos; una cache hit se reutiliza; el secuencial automatico exige punto y la firma exige metadatos | HTTP 400 o job fallido cuando aplica una guarda | High | PFV-023, PFV-029, PFV-036 |
| VR-027 | Endpoint de factura | Maximo 10 solicitudes por 60 segundos | Solicitud bloqueada por `ThrottlerGuard` | High | - |

## Validaciones de entrada HTTP

### VR-001 - Tuberia global de DTO

La aplicacion transforma el payload a DTO, aplica lista blanca y rechaza las
propiedades que no tienen decoradores. Las excepciones HTTP mantienen su codigo;
el filtro global responde con `success: false` y coloca el mensaje en `error`.

**Evidence**
- File: `src/main.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `bootstrap`; `AllExceptionsFilter.catch`
- Line / Section: main 53-60; filtro 15-45
- Condition / Query / Statement: `ValidationPipe` usa `transform: true`, `whitelist: true` y `forbidNonWhitelisted: true`; el filtro toma el estado de `HttpException` y serializa la respuesta.
- Confidence: High

### VR-002 - Momento de las validaciones de servicio

Cuando `SRI_EMISION_ASYNC` es exactamente `false`, `FacturaService` valida antes
de completar la peticion. En cualquier otro valor, incluida la ausencia de la
variable, la API primero crea un job y devuelve `EN_COLA`; el procesador ejecuta
despues las validaciones de identificacion, catalogos y calculos. Por tanto, una
misma infraccion expresada como `BadRequestException` produce HTTP 400 en modo
sincrono o un job fallido despues de la respuesta HTTP inicial en modo asincrono.

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`; `FacturaService.emitirFactura`; `SriController.emitirFactura`
- Line / Section: servicio 56-67; procesador 24-45; factura 42-64; controlador 84-110
- Condition / Query / Statement: el servicio llama directamente a `FacturaService` solo cuando la configuracion es `false`; de otro modo encola el DTO y el worker invoca posteriormente el mismo servicio.
- Confidence: High

Related PFV: `PFV-005`.

### VR-003 - Ambiente y tipo de emision

`ambiente` y `tipoEmision` son opcionales. Si llegan en el payload deben
pertenecer respectivamente a `Ambiente` y `TipoEmision`, cuyos valores definidos
son `1` y `2`. La factura usa el ambiente derivado de configuracion cuando se
omite y usa emision normal (`1`) como tipo por defecto.

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/constants/sri.enums.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `CreateFacturaDto`; enums `Ambiente` y `TipoEmision`; `FacturaService.emitirFactura`; `SriBaseService.getDefaultAmbiente`
- Line / Section: DTO 20-37; enums 17-28; factura 66-74; base 26-32
- Condition / Query / Statement: `@IsOptional` y `@IsEnum` restringen los valores informados; el servicio aplica los valores por defecto.
- Confidence: High

### VR-004 - Formato de fecha de emision

La fecha debe ser un string que coincida con dos digitos de dia, dos de mes y
cuatro de anio separados por `/`. Esta regla confirma formato, no validez
calendaria.

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`
- Function / Method / Procedure: `CreateFacturaDto.fechaEmision`
- Line / Section: 39-44
- Condition / Query / Statement: `@Matches(/^\d{2}\/\d{2}\/\d{4}$/)` produce el mensaje de formato configurado cuando no coincide.
- Confidence: High

Related PFV: `PFV-020`.

### VR-005 - Secuencial de factura

El secuencial informado es opcional, debe ser string numerico de uno a nueve
digitos y se completa a nueve digitos con ceros a la izquierda. Si se omite, el
servicio intenta reservar el siguiente secuencial del punto de emision; la
ausencia de ese punto produce `BadRequestException`.

Una excepcion tecnica del upsert o de la transaccion no se convierte en
`BadRequestException`: se propaga. En modo sincrono, un `Error` plano de SQL se
serializa como 500 y la ausencia del pool produce la
`ServiceUnavailableException` 503 de `getClient`; en modo asincrono el job
falla.

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `CreateFacturaDto.secuencial`; `FacturaService.emitirFactura`; `getNextSecuencial`; `DatabaseService.getClient`, `transaction`
- Line / Section: DTO 46-55; factura 76-104; repositorio 199-219; database 159-185
- Condition / Query / Statement: forma/padding o reserva; solo la falta de punto se convierte a 400, mientras los fallos de consulta/transaccion se propagan y `getClient` usa 503 si el pool no existe.
- Confidence: High

Related PFV: `PFV-023`, `PFV-033`.

### VR-006 - Datos del emisor en el DTO

El RUC debe ser un string de exactamente 13 digitos. `razonSocial` y `dirMatriz`
son strings no vacios. `establecimiento` y `puntoEmision` son strings numericos
de exactamente tres digitos. `obligadoContabilidad` acepta `SI` o `NO`, y
`contribuyenteRimpe`, si se informa, solo acepta el literal declarado en el DTO.
Los demas campos del bloque son strings opcionales.

`CreateFacturaDto` no aplica `@IsDefined` al bloque `emisor`; por ello estas
reglas de campo solo operan cuando el bloque llega y su ausencia se mantiene en
`PFV-019`.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`
- Function / Method / Procedure: `EmisorDto`
- Line / Section: 17-76
- Condition / Query / Statement: decoradores `@IsString`, `@Length`, `@Matches`, `@IsNotEmpty`, `@IsEnum` y `@IsOptional` sobre los datos del emisor.
- Confidence: High

Related PFV: `PFV-019`.

### VR-007 - Datos del comprador en el DTO

El tipo debe pertenecer a `TipoIdentificacion`; `identificacion` y `razonSocial`
son strings no vacios. Direccion, telefono y email son strings opcionales. No se
encontro un validador de formato de correo o telefono en este DTO.
`CreateFacturaDto` tampoco aplica `@IsDefined` al bloque `comprador`; sus reglas
internas no convierten por si solas el bloque padre en obligatorio.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/constants/sri.enums.ts`
- Function / Method / Procedure: `CompradorDto`; enum `TipoIdentificacion`
- Line / Section: DTO 79-110; enum 33-40
- Condition / Query / Statement: el DTO aplica enum, string y no vacio; el enum declara codigos `04` a `09`.
- Confidence: High

Related PFV: `PFV-019`, `PFV-026`, `PFV-027`.

### VR-008 - Datos del detalle

`codigoPrincipal` y `descripcion` son strings no vacios. Cantidad, precio
unitario y descuento deben ser numeros mayores o iguales a cero. Los impuestos
son un arreglo con validacion anidada; los detalles adicionales son un arreglo
anidado opcional.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`
- Function / Method / Procedure: `DetalleFacturaDto`
- Line / Section: 150-206
- Condition / Query / Statement: decoradores de texto, `@IsNumber`, `@Min(0)`, `@IsArray`, `@ValidateNested` y `@Type` sobre cada detalle.
- Confidence: High

Related PFV: `PFV-019`, `PFV-021`.

### VR-009 - Datos del impuesto de detalle

`codigo` y `codigoPorcentaje` deben ser strings. Tarifa, base imponible y valor
deben ser numeros mayores o iguales a cero.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`
- Function / Method / Procedure: `ImpuestoDetalleDto`
- Line / Section: 113-135
- Condition / Query / Statement: `@IsString`, `@IsNumber` y `@Min(0)` validan la forma basica de cada impuesto.
- Confidence: High

Related PFV: `PFV-019`, `PFV-021`.

### VR-010 - Datos del pago

La forma de pago debe pertenecer al enum `FormaPago`. El total debe ser un
numero mayor o igual a cero. El plazo es un numero opcional sin minimo declarado;
la unidad opcional acepta `dias`, `meses` o `años`.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/constants/sri.enums.ts`
- Function / Method / Procedure: `PagoDto`; enum `FormaPago`
- Line / Section: DTO 209-230; enum 66-75
- Condition / Query / Statement: `@IsEnum`, `@IsNumber`, `@Min(0)` y validadores opcionales definen la forma del pago; `plazo` no tiene `@Min`.
- Confidence: High

Related PFV: `PFV-019`, `PFV-021`.

### VR-011 - Detalles e informacion adicional

Los pares de nombre y valor de los detalles adicionales y de la informacion
adicional deben ser strings no vacios. Sus colecciones usan validacion anidada;
la informacion adicional de factura es opcional.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/dto/factura.dto.ts`
- Function / Method / Procedure: `DetalleAdicionalDto`; `CampoAdicionalDto`; `CreateFacturaDto.infoAdicional`
- Line / Section: common DTO 138-147, 189-197, 233-243; factura DTO 82-90
- Condition / Query / Statement: los objetos internos usan `@IsString` y `@IsNotEmpty`; los arreglos usan `@ValidateNested` y `@Type`.
- Confidence: High

## Identificaciones

### VR-012 - Despacho por tipo de identificacion

El servicio envia `04` a validacion de RUC, `05` a cedula, `06` a pasaporte,
`07` a consumidor final y `08` a identificacion exterior. Otro codigo retorna
invalido. El wrapper comun convierte ese resultado en `BadRequestException` con
contexto del comprador.

**Evidence**
- File: `src/modules/sri/services/identificacion-validator.service.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `IdentificacionValidatorService.validar`; `SriBaseService.validarIdentificacion`
- Line / Section: identificacion 16-36; base 38-58
- Condition / Query / Statement: el `switch` define los cinco tipos atendidos y el wrapper lanza cuando `valido` es falso.
- Confidence: High

Related PFV: `PFV-026`, `PFV-027`.

### VR-013 - Cedula ecuatoriana

La cedula debe contener exactamente 10 digitos, usar un codigo de provincia
entre 01 y 24, tener tercer digito menor o igual a 5 y satisfacer el digito
verificador calculado mediante Modulo 10.

**Evidence**
- File: `src/modules/sri/services/identificacion-validator.service.ts`
- Function / Method / Procedure: `validarCedula`
- Line / Section: 42-92
- Condition / Query / Statement: controles de regex, provincia, tercer digito y comparacion del digito calculado con coeficientes Modulo 10.
- Confidence: High

### VR-014 - RUC ecuatoriano

Cuando el comprador declara tipo `04`, su RUC debe contener exactamente 13
digitos y provincia entre 01 y 24. Para persona natural, los primeros 10
digitos deben formar una cedula valida y el
codigo de establecimiento numerico debe ser al menos 1. Para sociedad privada,
el tercer digito es 9, se aplica Modulo 11 y el RUC termina en `001`. Para
sociedad publica, el tercer digito es 6, se aplica Modulo 11 y termina en
`0001`. Otros terceros digitos se rechazan. Este algoritmo no se aplica al RUC
del emisor: ese valor tiene validacion de forma en el DTO y busquedas por RUC.

**Evidence**
- File: `src/modules/sri/services/identificacion-validator.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/common.dto.ts`
- Function / Method / Procedure: `validarRuc`; variantes por tipo; `FacturaService.emitirFactura`; `EmisorDto.ruc`
- Line / Section: identificacion 101-241; factura 48-52; DTO 17-22
- Condition / Query / Statement: factura entrega al algoritmo solo tipo/identificacion del comprador; el RUC emisor usa decoradores de 13 digitos y las busquedas posteriores.
- Confidence: High

Related PFV: `PFV-022`, `PFV-036`.

### VR-015 - Pasaporte, consumidor final e identificacion exterior

El pasaporte solo se controla por presencia y longitud entre 5 y 20 caracteres.
Consumidor final debe usar exactamente `9999999999999`. La identificacion del
exterior retorna valida sin comprobar su estructura; para el ingreso HTTP sigue
aplicando la regla de string no vacio del comprador.

**Evidence**
- File: `src/modules/sri/services/identificacion-validator.service.ts`; `src/modules/sri/dto/common.dto.ts`
- Function / Method / Procedure: `validar`; `validarPasaporte`; `validarConsumidorFinal`; `CompradorDto.identificacion`
- Line / Section: servicio 25-30, 247-270; DTO 87-90
- Condition / Query / Statement: el tipo `08` retorna directamente `{ valido: true }`; pasaporte usa solo limites de longitud y consumidor final compara un valor fijo.
- Confidence: High

Related PFV: `PFV-027`.

## Catalogos

### VR-016 - Existencia del tipo de identificacion

Ademas de la validacion algoritmica, la factura exige que el codigo del tipo de
identificacion exista en la cache de registros activos de
`catalogo_tipos_identificacion`. La cache se renueva como maximo cada cinco
minutos. La ausencia produce `BadRequestException`; una falla al cargar el
catalogo se propaga.

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `validateTipoIdentificacion`; `refreshCacheIfNeeded`; `loadCache`; `validarTipoIdentificacionCatalogo`; `emitirFactura`
- Line / Section: catalogo 66-76, 206-223, 321-337, 394-407; base 133-151; factura 54-64
- Condition / Query / Statement: la consulta carga solo filas activas y la validacion comprueba la presencia del codigo en `tiposIdentificacionCache`.
- Confidence: High

Related PFV: `PFV-026`, `PFV-027`.

### VR-017 - Existencia de codigos de impuesto

Cada impuesto de cada detalle se valida por la pareja
`codigo-codigoPorcentaje`. El catalogo contiene tarifas activas cuyo
`vigente_hasta` sea nulo o no anterior a la fecha actual. Los errores de todas
las parejas se acumulan; el wrapper lanza `BadRequestException` con mensaje
generico y una lista interna de errores. La carga no filtra `vigente_desde` ni
el estado `activo` de `catalogo_impuestos`; tampoco ordena revisiones y el mapa
conserva una sola entrada por pareja.

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `validateImpuesto`; `validateImpuestos`; `loadCache`; `validarImpuestosDetalles`; `emitirFactura`
- Line / Section: catalogo 84-119, 343-363; base 63-101; factura 54-64
- Condition / Query / Statement: lookup por clave compuesta contra tarifas `t` cargadas con `t.activo = true` y filtro de `t.vigente_hasta`; la consulta no filtra `t.vigente_desde` ni `i.activo` y el mapa usa una clave sin revision temporal.
- Confidence: High

Related PFV: `PFV-021`, `PFV-028`, `PFV-037`.

### VR-018 - Existencia de formas de pago

Cada forma de pago debe superar primero el enum del DTO y despues existir entre
los registros activos de `catalogo_formas_pago`. El validador acumula todas las
formas ausentes y el wrapper lanza `BadRequestException`.

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `PagoDto.formaPago`; `validateFormaPago`; `validateFormasPago`; `loadCache`; `validarFormasPagoCatalogo`; `emitirFactura`
- Line / Section: DTO 209-217; catalogo 170-200, 382-392; base 157-175; factura 54-64
- Condition / Query / Statement: la validacion efectiva es la interseccion del enum compilado y los codigos activos en base de datos.
- Confidence: High

## Validaciones y calculos monetarios

### VR-019 - Limite del descuento por detalle

El subtotal del detalle se calcula como cantidad por precio unitario. Si el
descuento es mayor que ese subtotal, la factura lanza `BadRequestException`. Se
permite un descuento igual al subtotal y el precio total sin impuesto se calcula
restando el descuento.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `buildDetalles`
- Line / Section: 612-642
- Condition / Query / Statement: comparacion `d.descuento > subtotal` y calculo `subtotal - d.descuento`.
- Confidence: High

Related PFV: `PFV-019`, `PFV-021`.

### VR-020 - Calculo de totales

El total sin impuestos suma los precios totales sin impuesto ya calculados. Los
impuestos se agrupan por codigo y codigo de porcentaje, sumando las bases y los
valores proporcionados en el DTO. El importe total suma el total sin impuestos
y esos valores de impuesto. Los resultados se redondean a dos decimales con
`decimal.js`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `calculateTotales`
- Line / Section: 645-710
- Condition / Query / Statement: acumulacion con `Decimal`, agrupacion por clave de impuesto y calculo de `importeTotal` sin usar los totales de pago.
- Confidence: High

Related PFV: `PFV-021`.

## Clave de acceso

### VR-021 - Generacion de la clave

La generacion elimina caracteres no numericos del RUC y exige que el resultado
tenga 13 digitos. Completa tipo de comprobante, establecimiento, punto y
secuencial a sus anchos, usa los valores por defecto de ambiente y tipo de
emision cuando faltan, genera un codigo numerico de ocho digitos cuando no se
recibe y agrega el digito verificador Modulo 11. Un RUC limpio de otra longitud
produce un `Error` plano.

**Evidence**
- File: `src/modules/sri/services/clave-acceso.service.ts`
- Function / Method / Procedure: `generate`; `validateRuc`; `generateCodigoNumerico`; `calculateModulo11`
- Line / Section: 28-56, 113-144
- Condition / Query / Statement: composicion secuencial de la clave, limpieza del RUC, generacion numerica y calculo de checksum.
- Confidence: High

### VR-022 - Validacion y parseo en `ClaveAccesoService`

`validate` retorna `false` si la clave no contiene exactamente 49 digitos o si
el ultimo digito no coincide con el Modulo 11 calculado sobre los primeros 48.
`parse` retorna `null` cuando esa validacion falla y solo extrae componentes si
la clave la supera.

**Evidence**
- File: `src/modules/sri/services/clave-acceso.service.ts`
- Function / Method / Procedure: `validate`; `parse`
- Line / Section: 59-103
- Condition / Query / Statement: controles de longitud y regex, comparacion de checksum y retorno nulo previo al parseo.
- Confidence: High

### VR-023 - Validacion de clave aplicada por las rutas HTTP

Las rutas por clave extraen el RUC mediante una utilidad distinta. Esa utilidad
rechaza valor vacio, longitud diferente de 49 o caracteres no numericos, pero no
calcula Modulo 11. Lanza `Error`, no `HttpException`; el filtro global asigna
HTTP 500 a ese tipo de falla. El DTO `ConsultaAutorizacionDto` declara una regex
de 49 digitos, pero el handler de autorizacion recibe un `@Param` string sin usar
ese DTO.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/utils/clave-acceso.utils.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `validateClaveAccesoAccess`; `consultarAutorizacion`; `validateClaveAcceso`; `ConsultaAutorizacionDto`; `AllExceptionsFilter.catch`
- Line / Section: controlador 76-81, 221-242; utilidad 16-18, 38-51; DTO 121-127; filtro 20-45
- Condition / Query / Statement: el handler entrega el parametro crudo a `extractRucFromClaveAcceso`; la utilidad solo valida forma y el filtro usa 500 para errores no HTTP.
- Confidence: High

Related PFV: `PFV-030`.

### VR-024 - Secuencial en preview y debug

La vista previa XML y la generacion firmada de depuracion requieren un
secuencial explicito. Si falta, cada metodo lanza `BadRequestException`; si
existe se completa a nueve digitos.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `generarXmlPreview`; `generarFacturaFirmadaDebug`
- Line / Section: 237-267, 279-313
- Condition / Query / Statement: ambos metodos comprueban `!dto.secuencial`, lanzan HTTP 400 y aplican `padStart(9, '0')`.
- Confidence: High

## Acceso y precondiciones operativas

### VR-025 - Acceso al RUC del emisor por tenant

Antes de emitir o previsualizar una factura, el controlador exige que el RUC
corresponda a una fila encontrada por RUC. `SUPERADMIN` puede usar esa fila. Los
demas usuarios requieren `tenantId` y no pueden acceder cuando la fila tiene un
tenant no nulo diferente; una fila con tenant nulo supera la condicion. Un RUC
inexistente produce 404 y el tenant distinto produce 403. Las rutas por clave
extraen primero el RUC de la propia clave y aplican el mismo control. La consulta
no recibe tenant ni define orden cuando hay mas de una fila para el RUC.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/emisores/emisores.service.ts`
- Function / Method / Procedure: `SriController.validateClaveAccesoAccess`; `SriController.emitirFactura`; `SriController.previewFactura`; `EmisoresService.validateRucAccess`
- Line / Section: controlador 76-81, 103-110, 257-264; emisores 222-252
- Condition / Query / Statement: el controlador delega el RUC al servicio; `findByRuc` filtra solo por RUC y la condicion de `ForbiddenException` compara el tenant unicamente cuando `emisor.tenantId` es no nulo.
- Confidence: High

Related PFV: `PFV-036`.

### VR-026 - Emisor, punto de emision y certificado

El procesamiento consulta un emisor con estado `ACTIVO` cuando no existe una
entrada Redis; si hay cache, la retorna sin volver a evaluar estado. El punto
aplica el mismo patron: en cache miss exige establecimiento y punto `ACTIVO`.
La ausencia del punto se rechaza expresamente cuando debe generarse el secuencial automatico;
si el cliente suministra secuencial, ese rechazo no se ejecuta. Antes de firmar,
el servicio exige que el emisor localizado tenga nombre de certificado y
contrasena cifrada registrada; el firmador tambien retorna primero su cache y
solo consulta estado activo al recargar. La falta de metadatos lanza
`BadRequestException`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `findEmisorByRuc`; `findPuntoEmision`; `loadEmisorCertificate`
- Line / Section: factura 54-64, 76-104, 128-138; repositorio 142-193; firmador 285-385
- Condition / Query / Statement: los metodos retornan cache antes de sus consultas con estado activo; el rechazo de punto solo aplica al secuencial automatico y la falta de metadatos se comprueba antes de firmar.
- Confidence: High

Related PFV: `PFV-023`, `PFV-029`, `PFV-036`.

### VR-027 - Limite de solicitudes de emision de factura

El endpoint de emision de factura declara un maximo de 10 solicitudes en una
ventana de 60 segundos. `ThrottlerGuard` esta registrado como guard global, por
lo que aplica la configuracion especifica del decorador al endpoint y bloquea
solicitudes que exceden el limite.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/app.module.ts`
- Function / Method / Procedure: `SriController.emitirFactura`; configuracion `ThrottlerModule`; proveedor global `ThrottlerGuard`
- Line / Section: controlador 84-102; AppModule 51-61, 120-135
- Condition / Query / Statement: `@Throttle({ default: { limit: 10, ttl: 60000 } })` y registro de `ThrottlerGuard` mediante `APP_GUARD`.
- Confidence: High

## Brechas de validacion y PFV relacionados

Estas filas no son reglas confirmadas. Son decisiones funcionales o contractuales
que requieren validacion antes de convertirse en requisitos de migracion.

| PFV | Area | Brecha observada | Riesgo |
|---|---|---|---|
| PFV-005 | Emision asincrona | Las validaciones semanticas pueden fallar en el worker despues de que la API devuelve `jobId`; este lote no identifica un contrato de consulta del resultado final. | El cliente puede considerar aceptado un documento que posteriormente falla validacion. |
| PFV-019 | Estructura y minimos | No hay `@IsDefined` en los objetos anidados emisor/comprador, ni `@ArrayNotEmpty` en detalles, pagos o impuestos; los campos con `@Min(0)` admiten cero. | Fallos no uniformes, facturas sin contenido o importes nulos si otras capas no los rechazan. |
| PFV-020 | Fecha | El DTO valida solo el patron y el servicio construye `Date` con componentes numericos sin comprobar que la fecha exista. | Normalizacion de fechas imposibles o rechazo posterior por el SRI. |
| PFV-021 | Aritmetica | No se compara tarifa con porcentaje de catalogo, base/valor de impuesto con el neto del detalle, ni suma de pagos con importe total; `plazo` no tiene minimo. | Totales internamente inconsistentes o rechazo del comprobante. |
| PFV-026 | Tipo `09` | El enum del DTO admite `PLACA = 09`, el validador algoritmico lo trata como desconocido y el dump no contiene ese codigo en el catalogo activo. | Contrato contradictorio para compradores identificados con placa. |
| PFV-027 | Autoridad de identificacion | El catalogo carga longitud y regex, pero la validacion por catalogo solo comprueba existencia; pasaporte usa solo longitud y exterior no valida estructura. | Reglas distintas entre datos maestros y codigo. |
| PFV-028 | Seleccion temporal de tarifa | La tabla admite revisiones por `vigente_desde`; la carga no filtra ese inicio, no ordena revisiones y el mapa sobrescribe por pareja. | Una tarifa futura o una revision no determinista puede representar la pareja. |
| PFV-030 | Clave de acceso HTTP | Las rutas no aplican checksum Modulo 11 y los errores de forma se convierten en HTTP 500. | Consultas con claves invalidas y contrato de error inadecuado. |
| PFV-036 | Alcance del RUC | La guarda, el repositorio de emision y caches buscan por RUC sin tenant; una fila con tenant nulo pasa la guarda. | Seleccion de otro emisor o certificado cuando el RUC no es globalmente unico. |
| PFV-037 | Estado del impuesto | La carga exige tarifa activa, pero no filtra el estado `activo` del impuesto padre. | Una pareja de un impuesto deshabilitado puede validarse. |

### Evidencia de PFV-005

**Evidence**
- File: `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `SriService.emitirFactura`; `SriEmisionProcessor.process`
- Line / Section: servicio 56-67; procesador 24-45
- Condition / Query / Statement: la respuesta asincrona contiene `jobId` y `EN_COLA`; las validaciones de `FacturaService` ocurren posteriormente dentro del worker.
- Confidence: High

### Evidencia de PFV-019

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `CreateFacturaDto`; `EmisorDto`; `CompradorDto`; `DetalleFacturaDto`; `ImpuestoDetalleDto`; `PagoDto`; `SriController.emitirFactura`
- Line / Section: factura DTO 57-90; common DTO 17-22, 79-90, 113-135, 150-230; controlador 103-110
- Condition / Query / Statement: objetos anidados sin `@IsDefined`, arreglos sin `@ArrayNotEmpty`, minimos numericos en cero y acceso inmediato a `dto.emisor.ruc` en el controlador.
- Confidence: Medium

### Evidencia de PFV-020

**Evidence**
- File: `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `CreateFacturaDto.fechaEmision`; `FacturaService.emitirFactura`; `generarXmlPreview`
- Line / Section: DTO 39-44; servicio 69-74, 251-256
- Condition / Query / Statement: regex de formato seguida de construccion de `Date` sin una comparacion explicita de los componentes resultantes.
- Confidence: High

### Evidencia de PFV-021

**Evidence**
- File: `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `ImpuestoDetalleDto`; `PagoDto`; `validarImpuestosDetalles`; `buildDetalles`; `calculateTotales`
- Line / Section: DTO 113-135, 209-230; base 63-101; factura 612-710
- Condition / Query / Statement: el catalogo comprueba solo la pareja de codigos; factura conserva tarifa/base/valor suministrados, agrega sus valores y no usa los pagos para validar `importeTotal`.
- Confidence: High

### Evidencia de PFV-026

**Evidence**
- File: `src/modules/sri/constants/sri.enums.ts`; `src/modules/sri/services/identificacion-validator.service.ts`; `src/modules/sri/services/catalogo-validator.service.ts`; `database/init.sql`
- Function / Method / Procedure: enum `TipoIdentificacion`; `IdentificacionValidatorService.validar`; carga del catalogo de identificaciones; datos iniciales del catalogo
- Line / Section: enum 33-40; validador 16-36; catalogo 394-407; dump 888-895
- Condition / Query / Statement: `09` aparece en el enum, no tiene caso en el `switch` y no aparece entre los codigos `04` a `08` insertados en el dump.
- Confidence: High

### Evidencia de PFV-027

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/identificacion-validator.service.ts`; `database/init.sql`
- Function / Method / Procedure: `validateTipoIdentificacion`; carga del catalogo; `validarPasaporte`; caso exterior; DDL y datos de tipos de identificacion
- Line / Section: catalogo 206-223, 394-407; identificacion 25-30, 247-254; dump 190-197, 891-895
- Condition / Query / Statement: longitud y regex se almacenan en cache, pero el metodo de validacion solo busca el codigo; los validadores efectivos no consumen esos atributos.
- Confidence: High

### Evidencia de PFV-028

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `database/init.sql`
- Function / Method / Procedure: carga de tarifas; DDL y unicidad de `catalogo_tarifas_impuesto`
- Line / Section: catalogo 343-363; dump 172-183, 1116-1124
- Condition / Query / Statement: la tabla permite revisiones por `vigente_desde`; la consulta no filtra ese campo ni usa `ORDER BY`, y `Map.set` conserva una sola fila por impuesto/porcentaje.
- Confidence: High

### Evidencia de PFV-030

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/utils/clave-acceso.utils.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `validateClaveAccesoAccess`; `validateClaveAcceso`; `ClaveAccesoService.validate`; `AllExceptionsFilter.catch`
- Line / Section: controlador 76-81; utilidad 38-51; servicio 62-75; filtro 20-45
- Condition / Query / Statement: la ruta usa el validador de forma, mientras el checksum existe en otro servicio sin ser invocado; los errores planos reciben estado 500.
- Confidence: High

### Evidencia de PFV-036

**Evidence**
- File: `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `database/init.sql`
- Function / Method / Procedure: `findByRuc`; `validateRucAccess`; `create`; `findEmisorByRuc`; `loadEmisorCertificate`; constraint de `emisores`
- Line / Section: emisores 227-291; repositorio 142-160; firmador 285-385; dump 432-454, 1239-1243
- Condition / Query / Statement: create rechaza un RUC encontrado globalmente, el DDL permite unicidad por tenant y las consultas/caches operativas usan solo RUC; la guarda permite tenant nulo.
- Confidence: High

### Evidencia de PFV-037

**Evidence**
- File: `src/modules/sri/services/catalogo-validator.service.ts`; `database/init.sql`
- Function / Method / Procedure: `validateImpuesto`; `loadCache`; DDL de impuestos y tarifas
- Line / Section: catalogo 84-100, 343-363; dump 126-137, 172-183
- Condition / Query / Statement: la consulta une `catalogo_impuestos i` pero solo filtra actividad y fin de vigencia de `t`; la presencia en el mapa basta para validar.
- Confidence: High

## Estado del lote

Las reglas `VR-001` a `VR-027` quedan confirmadas para el alcance analizado. Las
brechas listadas permanecen abiertas en los PFV centralizados y no deben
interpretarse como requisitos confirmados.
