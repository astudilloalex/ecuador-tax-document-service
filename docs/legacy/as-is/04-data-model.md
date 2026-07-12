# 04 - Modelo de datos AS-IS

## Estado del análisis

Este documento se inició en el primer lote acotado de descubrimiento y se
amplió con el flujo de persistencia de Factura analizado en el segundo lote.
Describe estructuras físicas encontradas en el dump SQL, el código de acceso a
datos y los directorios persistentes. Los nombres funcionales no confirmados no
se presentan como entidades de negocio.

## Almacenes físicos confirmados

La aplicación utiliza PostgreSQL mediante un pool de `pg`. La conexión se
construye con host, puerto, base, usuario, contraseña y una opción SSL; si la
conexión inicial falla, la aplicación conserva el proceso activo sin pool y las
consultas posteriores responden con indisponibilidad.

**Evidence**
- File: `src/database/database.service.ts`
- Function / Method / Procedure: `DatabaseService.onModuleInit`, `DatabaseService.query`
- Line / Section: 22-67, 95-131
- Condition / Query / Statement: `new Pool(...)`; ante fallo se asigna `this.pool = undefined`; una consulta sin pool lanza `ServiceUnavailableException`
- Confidence: High

El artefacto DDL disponible es un dump de PostgreSQL 17.6 generado por
`pg_dump` 17.9. Crea el esquema `public` y el tipo enumerado físico
`public.user_role` con los valores `SUPERADMIN`, `ADMIN` y `USER`.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: encabezado del dump, creación de esquema y tipo
- Line / Section: 1-43
- Condition / Query / Statement: `CREATE SCHEMA IF NOT EXISTS public` y `CREATE TYPE public.user_role AS ENUM (...)`
- Confidence: High

## Inventario de tablas físicas

El dump contiene 29 tablas. La columna "uso físico observado" resume columnas,
comentarios SQL y relaciones declaradas; no reemplaza una definición funcional.

| Tabla física | Línea DDL | Uso físico observado | Clave o relación principal |
|---|---:|---|---|
| `auditoria` | 54 | Registra usuario, tenant, origen HTTP, acción, recurso, cambios, resultado y duración. | PK `id`; `usuario_id` referencia `usuarios`. |
| `catalogo_documentos_sustento` | 100 | Almacena código, descripción y estado activo de documentos de sustento. | PK `id`; `codigo` único. |
| `catalogo_formas_pago` | 113 | Almacena código, descripción y estado activo de formas de pago. | PK `id`; `codigo` único. |
| `catalogo_impuestos` | 126 | Almacena código, nombre, descripción y estado de impuestos. | PK `id`; `codigo` único. |
| `catalogo_motivos_traslado` | 141 | Almacena código, descripción y estado de motivos de traslado. | PK `id`; `codigo` único. |
| `catalogo_retenciones` | 154 | Almacena tipo, código, porcentaje, vigencia y estado de retenciones. | PK `id`; combinación `tipo`, `codigo`, `vigente_desde` única. |
| `catalogo_tarifas_impuesto` | 172 | Almacena tarifas y vigencia vinculadas a un impuesto. | PK `id`; FK `impuesto_id`. |
| `catalogo_tipos_identificacion` | 190 | Almacena código, longitud y expresión de validación por tipo de identificación. | PK `id`; `codigo` único. |
| `comprobante_detalles` | 205 | Almacena líneas con códigos, descripción, cantidad, precio, descuento y orden. | PK `id`; FK `comprobante_id`. |
| `comprobante_impuestos` | 232 | Almacena impuesto, tarifa, base y valor por línea de detalle. | PK `id`; FK `comprobante_detalle_id`. |
| `comprobante_pagos` | 254 | Almacena forma, total, plazo y unidad de tiempo por comprobante. | PK `id`; FK `comprobante_id`. |
| `comprobante_retenciones` | 275 | Almacena códigos, base, porcentaje, valor y datos de documento de sustento. | PK `id`; FK `comprobante_id`. |
| `comprobante_totales` | 303 | Almacena agregados de impuestos por comprobante. | PK `id`; FK `comprobante_id`. |
| `comprobante_xmls` | 327 | Almacena rutas de XML firmado y autorizado, no el contenido XML. | PK `id`; `comprobante_id` único y FK. |
| `comprobantes` | 354 | Cabecera física común con emisor, punto, receptor, tipo, clave, estados, importes y campos específicos de documentos. | PK `id`; FKs `emisor_id`, `punto_emision_id`; `clave_acceso` única. |
| `detalles_adicionales` | 413 | Almacena pares nombre/valor por línea de comprobante. | PK `id`; FK `comprobante_detalle_id`. |
| `emisores` | 432 | Almacena identificación, datos tributarios, tenant, estado y material/metadatos de certificado. | PK `id`; FK `tenant_id`; combinación `tenant_id`, `ruc` única. |
| `establecimientos` | 517 | Almacena código, dirección y estado bajo un emisor. | PK `id`; FK `emisor_id`; combinación `emisor_id`, `codigo` única. |
| `guia_destinatarios` | 538 | Almacena destinatario, traslado, ruta y documento de sustento de una guía. | PK `id`; FK `comprobante_id`. |
| `guia_detalles` | 576 | Almacena código, descripción y cantidad bajo un destinatario de guía. | PK `id`; FK `destinatario_id`. |
| `info_adicional` | 591 | Almacena pares nombre/valor a nivel de comprobante. | PK `id`; FK `comprobante_id`. |
| `motivos_nota_debito` | 610 | Almacena razón y valor por comprobante. | PK `id`; FK `comprobante_id`. |
| `puntos_emision` | 629 | Almacena código, descripción y estado bajo un establecimiento. | PK `id`; FK `establecimiento_id`; código único por establecimiento. |
| `secuenciales` | 650 | Almacena último secuencial por punto y tipo de comprobante. | PK `id`; FK `punto_emision_id`; combinación punto/tipo única. |
| `sistema_config` | 670 | Almacena pares `clave`/`valor`, descripción y marcas de tiempo. | PK `id`; `clave` única. |
| `tenants` | 684 | Almacena nombre, plan, estado y marcas de tiempo. | PK `id`. |
| `usuarios` | 705 | Almacena email, hash de contraseña, rol, tenant, estado y último acceso. | PK `id`; `email` único; FK `tenant_id`. |
| `webhook_configs` | 722 | Almacena URL, eventos, secreto, alcance, estado y reintentos de webhook. | PK `id`; FKs `tenant_id`, `emisor_id`. |
| `webhook_logs` | 748 | Almacena payload, respuesta, intento, resultado, error y duración de entregas. | PK `id`; FK `config_id`. |

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: sentencias `CREATE TABLE`
- Line / Section: 50-72, 96-198, 201-399, 409-524, 534-760
- Condition / Query / Statement: 29 sentencias `CREATE TABLE public.<nombre>` con sus columnas físicas
- Confidence: High

## Relaciones físicas principales

### Tenancy, usuarios y webhooks

`emisores` depende de `tenants` con borrado en cascada; `usuarios` depende de
`tenants` con `ON DELETE SET NULL`. Las configuraciones de webhook dependen de
tenant y emisor con cascada, y sus logs dependen de la configuración con
cascada. `auditoria.usuario_id` referencia `usuarios` sin acción de borrado
explícita en el dump.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: restricciones FK de tenancy, auditoría y webhooks
- Line / Section: 1698-1702, 1786-1790, 1850-1878
- Condition / Query / Statement: `FOREIGN KEY` hacia `usuarios`, `tenants`, `emisores` y `webhook_configs`
- Confidence: High

La unicidad física del emisor es `(tenant_id, ruc)`, no RUC global, y
`tenant_id` admite nulo. El servicio de creación sí rechaza un RUC ya encontrado
de forma global. La validación de acceso, la resolución
para emisión, la carga del certificado firmante y sus cachés consultan o se
identifican solo por RUC. La guarda permite una fila con tenant nulo a un usuario
no `SUPERADMIN` que sí tenga tenant. La política de unicidad, selección y
aislamiento permanece en `PFV-036`.

**Evidence**
- File: `database/init.sql`; `src/modules/emisores/emisores.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: DDL/constraint de `emisores`; `findByRuc`; `validateRucAccess`; `create`; `findEmisorByRuc`; `loadEmisorCertificate`
- Line / Section: dump 432-454, 1239-1243; emisores 227-291; repositorio 142-160; firmador 285-385
- Condition / Query / Statement: la restricción incluye tenant y create rechaza globalmente; consultas/caches usan RUC y la comparación de tenant solo opera cuando la fila tiene tenant no nulo.
- Confidence: High

### Jerarquía física de emisión

La cadena declarada es `emisores` -> `establecimientos` -> `puntos_emision` ->
`secuenciales`. Los tres enlaces aplican `ON DELETE CASCADE`. `comprobantes`
referencia directamente a `emisores` con cascada y a `puntos_emision` sin una
acción de borrado explícita.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: restricciones FK de emisor y punto de emisión
- Line / Section: 1762-1774, 1794-1798, 1834-1846
- Condition / Query / Statement: FKs `comprobantes_emisor_id_fkey`, `comprobantes_punto_emision_id_fkey`, `establecimientos_emisor_id_fkey`, `puntos_emision_establecimiento_id_fkey`, `secuenciales_punto_emision_id_fkey`
- Confidence: High

### Grafo físico del comprobante

`comprobante_detalles`, `comprobante_pagos`, `comprobante_retenciones`,
`comprobante_totales`, `comprobante_xmls`, `info_adicional`,
`motivos_nota_debito` y `guia_destinatarios` dependen de `comprobantes` con
borrado en cascada. `comprobante_impuestos` y `detalles_adicionales` dependen de
`comprobante_detalles`; `guia_detalles` depende de `guia_destinatarios`, también
con cascada.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: restricciones FK del grafo de comprobantes
- Line / Section: 1713-1758, 1777-1782, 1801-1830
- Condition / Query / Statement: restricciones `FOREIGN KEY ... ON DELETE CASCADE`
- Confidence: High

### Catálogos y unicidad operativa

`catalogo_tarifas_impuesto.impuesto_id` referencia a
`catalogo_impuestos.id`. El dump también impone unicidad a la clave de acceso,
al registro XML por comprobante, a los códigos dentro de la jerarquía de
emisión y al secuencial por punto/tipo.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: restricciones de catálogo y unicidad
- Line / Section: 1111-1147, 1191-1219, 1239-1251, 1295-1323, 1705-1710
- Condition / Query / Statement: restricciones `UNIQUE` y FK de tarifa a impuesto
- Confidence: High

## Uso CRUD observado en la emisión de Factura

La persistencia de una Factura crea una cabecera en `comprobantes` y utiliza su
`id` para insertar el grafo dependiente. Las operaciones PostgreSQL reciben el
mismo `PoolClient`: por cada línea crea `comprobante_detalles` y, cuando están
presentes, `comprobante_impuestos` y `detalles_adicionales`; después crea
`comprobante_totales`, `comprobante_pagos`, el registro de rutas en
`comprobante_xmls` e `info_adicional`. No se observan operaciones `DELETE` en
`persistirFactura`; la actualización posible dentro de esta rutina es el
`ON CONFLICT DO UPDATE` de `comprobante_xmls`.

| Estructura | Operación observada en Factura | Condición o relación |
|---|---|---|
| `comprobantes` | `INSERT ... RETURNING *` | Cabecera y resultado SRI; origina `comprobante.id`. |
| `comprobante_detalles` | `INSERT` por cada detalle | FK física y campo de enlace `comprobante_id`. |
| `comprobante_impuestos` | `INSERT` por lote para cada detalle | Solo cuando el detalle tiene impuestos; enlace por `comprobante_detalle_id`. |
| `detalles_adicionales` | `INSERT` por lote para cada detalle | Solo cuando el detalle tiene datos adicionales; enlace por `comprobante_detalle_id`. |
| `comprobante_totales` | `INSERT` por lote | Solo cuando `totalConImpuestos` está presente; enlace por `comprobante_id`. |
| `comprobante_pagos` | `INSERT` por lote | Solo cuando `pagos` está presente; enlace por `comprobante_id`. |
| `comprobante_xmls` | `INSERT` o `UPDATE` ante conflicto por comprobante | Conserva las rutas del XML firmado y, si existe, autorizado. |
| `info_adicional` | `INSERT` por lote | Solo cuando el DTO contiene información adicional; enlace por `comprobante_id`. |

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `database/init.sql`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `createComprobante`, métodos `create*` y `saveXml`; FKs del grafo de comprobantes
- Line / Section: factura.service.ts 340-503; sri-repository.service.ts 225-251, 296-333, 358-390; init.sql 1713-1758, 1777-1782, 1817-1822
- Condition / Query / Statement: la cabecera, sus dependencias y las rutas XML se escriben mediante el `client` recibido por `persistirFactura`
- Confidence: High

La cabecera almacena `numero_autorizacion` con el número retornado por el SRI y
usa la propia `clave_acceso` cuando ese valor no está presente. El mapeo técnico
está confirmado; si esa sustitución representa un número de autorización válido
para todos los estados queda pendiente en `PFV-024`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`
- Line / Section: 355-384
- Condition / Query / Statement: `numero_autorizacion: resultado.numeroAutorizacion || claveAcceso`
- Confidence: High

### Límites transaccionales del grafo

Cuando están disponibles el emisor y el punto de emisión, el flujo ejecuta la
persistencia del grafo en una transacción PostgreSQL corta, tanto para el
resultado normal como para el registro `PENDIENTE` posterior a una excepción
del SRI. `DatabaseService.transaction` aplica `BEGIN`, `COMMIT` y `ROLLBACK`
alrededor del callback, por lo que las escrituras PostgreSQL que usan ese
`PoolClient` comparten el mismo resultado transaccional.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `DatabaseService.transaction`
- Line / Section: factura.service.ts 153-205; database.service.ts 168-185
- Condition / Query / Statement: ambos caminos invocan `executeInTransaction(async (client) => persistirFactura(..., client))`; el helper confirma o revierte el callback
- Confidence: High

Dentro de ese callback, `saveAllXmls` escribe de forma síncrona los archivos en
el filesystem antes de insertar sus rutas en `comprobante_xmls`. El rollback de
PostgreSQL no contiene una operación que elimine o revierta esos archivos; por
tanto, una falla posterior puede dejar archivos sin su grafo confirmado en la
base. La política funcional de detección y reconciliación entre SRI,
PostgreSQL y filesystem permanece en `PFV-035`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.persistirFactura`; `XmlStorageService.saveAllXmls`, `saveXml`; `DatabaseService.transaction`
- Line / Section: factura.service.ts 470-503; xml-storage.service.ts 49-129; database.service.ts 168-185
- Condition / Query / Statement: `writeFileSync` ocurre antes de `repository.saveXml`; el manejo de error ejecuta únicamente `ROLLBACK` de PostgreSQL
- Confidence: High

### Reserva y unicidad del secuencial de Factura

Cuando el DTO no aporta secuencial, el flujo reserva el siguiente valor mediante
un `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` sobre `secuenciales`. Esta
reserva se ejecuta y confirma en una transacción propia antes de generar la
clave, firmar, invocar al SRI y abrir la transacción posterior que persiste el
grafo de Factura.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/database/database.service.ts`
- Function / Method / Procedure: `FacturaService.emitirFactura`; `SriRepositoryService.getNextSecuencial`; `DatabaseService.transaction`
- Line / Section: factura.service.ts 85-126, 187-205; sri-repository.service.ts 199-219; database.service.ts 168-185
- Condition / Query / Statement: la fase 1 llama a `executeInTransaction` para incrementar `secuenciales`; la fase 3 abre otra transacción para `persistirFactura`
- Confidence: High

Cuando el DTO sí aporta secuencial, el servicio solo lo completa a nueve
dígitos y no invoca `getNextSecuencial`; por ello ese camino no actualiza el
contador de `secuenciales`. Esa rama tampoco detiene la fase de firma y envío
cuando el emisor existe pero no se encontró su punto de emisión, mientras que
la persistencia posterior exige `puntoEmisionInfo`; en esa condición el grafo
no se crea. Además, en el dump analizado `comprobantes` solo declara unicidad
para `clave_acceso`: no se observa restricción ni índice único sobre la
combinación `punto_emision_id`, `tipo_comprobante`, `secuencial`. La política
esperada para secuenciales manuales, punto activo, sincronización del contador
y duplicados permanece en `PFV-023`.

**Evidence**
- File: `src/modules/sri/services/factura.service.ts`; `database/init.sql`
- Function / Method / Procedure: `FacturaService.emitirFactura`; restricciones e índices de `comprobantes` y `secuenciales`
- Line / Section: factura.service.ts 76-153, 187-205; init.sql 1207-1219, 1311-1323, 1460-1498
- Condition / Query / Statement: la rama `if (dto.secuencial)` no actualiza `secuenciales` ni exige el punto antes del envío; `if (emisor && puntoEmisionInfo)` condiciona la persistencia; el DDL no declara unicidad punto/tipo/secuencial en `comprobantes`
- Confidence: High

## Persistencia híbrida

### XML

PostgreSQL conserva rutas relativas en `comprobante_xmls`. El contenido se
escribe de forma síncrona en el filesystem bajo la estructura
`{ruc}/{año}/{mes}/{sin_firmar|firmados|autorizados}/{claveAcceso}.xml`.

**Evidence**
- File: `database/init.sql`; `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: DDL `comprobante_xmls`; `XmlStorageService.saveXml`, `readXml`
- Line / Section: init.sql 323-347; xml-storage.service.ts 30-81, 132-148
- Condition / Query / Statement: columnas de ruta; `writeFileSync` y `readFileSync`
- Confidence: High

La retención de siete años aparece en un comentario, pero en este lote no se
encontró una operación de expiración o depuración. La responsabilidad operativa
permanece en `PFV-015`.

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: comentario de clase y métodos de persistencia
- Line / Section: 6-9, 20-148
- Condition / Query / Statement: se declara retención; el servicio observado solo crea, escribe y lee
- Confidence: Medium

### Certificados

La carga conserva el P12 recibido en `CERTS_DIR`. Cuando la solicitud incluye
RUC y la vinculación termina correctamente, también actualiza
`emisores.certificado_p12`; sin RUC queda solo el archivo. El firmador consulta
nombre y contraseña cifrada en PostgreSQL, pero abre el binario desde el
filesystem. La fuente autoritativa por preservar está pendiente en `PFV-012`.

**Evidence**
- File: `src/modules/certificate/certificate.controller.ts`; `src/modules/sri/services/xml-signer.service.ts`
- Function / Method / Procedure: carga y vinculación de certificado; `loadEmisorCertificate`
- Line / Section: certificate.controller.ts 165-173, 255-287, 323-363; xml-signer.service.ts 303-380
- Condition / Query / Statement: archivo de upload y vinculación condicional de `certificado_p12`; lectura posterior mediante `readFileSync(certPath)`
- Confidence: High

### Archivos de documentos

Las rutas requeridas son plantillas, PDFs, certificados y XML. Los PDFs se
subdividen en `con_firma`, `others`, `documents` e `images`; el despliegue Docker
monta los cuatro directorios raíz como volúmenes persistentes.

**Evidence**
- File: `src/config/configuration.ts`; `src/common/utils/storage-paths.ts`; `docker-compose.yml`; `docker-compose.prod.yml`
- Function / Method / Procedure: configuración de directorios, getters de almacenamiento y volúmenes
- Line / Section: configuration.ts 111-117; storage-paths.ts 8-43; docker-compose.yml 57-62; docker-compose.prod.yml 68-73
- Condition / Query / Statement: variables de directorio obligatorias y montajes `/data/templates`, `/data/pdfs`, `/data/certs`, `/data/xmls`
- Confidence: High

## Datos iniciales y límites de evidencia

El dump incluye filas iniciales para catálogos y `sistema_config`, además de una
identidad administrativa fija con hash de contraseña. Ningún valor de
credencial se reproduce en esta documentación. El estado operacional de esa
identidad se mantiene en `PFV-018`.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: sentencias de carga inicial
- Line / Section: 770-895, 990-1015
- Condition / Query / Statement: `INSERT INTO` de catálogos, configuración e identidad `SUPERADMIN`
- Confidence: High

El dump invoca `extensions.uuid_generate_v4()` en múltiples defaults, pero la
creación del esquema y extensión está separada en una guía manual. El
procedimiento autoritativo de bootstrap y evolución se mantiene en `PFV-011`.

**Evidence**
- File: `database/init.sql`; `database/Install BD.txt`
- Function / Method / Procedure: defaults UUID y pasos de instalación
- Line / Section: init.sql 205-206 y usos repetidos hasta 685; Install BD.txt 18-23, 30-37
- Condition / Query / Statement: dependencia de `extensions.uuid_generate_v4()` y creación manual de `uuid-ossp`
- Confidence: High

Los desajustes entre consultas y DDL se documentan como deuda confirmada en
`13-technical-debt.md`; su relación con el esquema desplegado permanece en
`PFV-013` y `PFV-014`.
