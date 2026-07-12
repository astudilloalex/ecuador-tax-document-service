# 01 - Inventario de fuentes AS-IS

## Alcance del inventario

El indice Git contiene 151 archivos rastreados. En el snapshot analizado, 142 estan disponibles y nueve documentos rastreados bajo `docs/` estan eliminados. Este inventario excluye dependencias, salidas de compilacion, cobertura, logs, datos runtime y metadatos de agentes.

**Evidence**
- File: Indice Git y worktree del repositorio
- Function / Method / Procedure: `git ls-files`; `git status --short`
- Line / Section: Snapshot del 2026-07-10
- Condition / Query / Statement: El conteo del indice es 151; nueve rutas `docs/*.md` aparecen eliminadas y no son legibles.
- Confidence: High

## Resumen cuantitativo

| Area | Archivos rastreados | Disponibles en el snapshot | Observacion |
|---|---:|---:|---|
| Raiz del repositorio | 18 | 18 | Manifiestos, configuracion, documentacion y despliegue |
| `Collection/` | 1 | 1 | Coleccion Postman |
| `database/` | 2 | 2 | Dump SQL y guia manual |
| `docs/` historico | 9 | 0 | Eliminados; relacionado con `PFV-003` |
| `src/` | 119 | 119 | Codigo TypeScript de aplicacion |
| `test/` | 2 | 2 | Prueba E2E y configuracion Jest |

**Evidence**
- File: Indice Git del repositorio
- Function / Method / Procedure: `git ls-files` agrupado por primer segmento de ruta
- Line / Section: Snapshot del lote 1
- Condition / Query / Statement: La suma por area produce 151 archivos y coincide con el conteo total del indice.
- Confidence: High

## Artefactos revisados

### 1. Manifiesto y toolchain

| Artefacto | Tipo | Proposito observado |
|---|---|---|
| `package.json` | Manifiesto NPM | Identidad, scripts, dependencias y configuracion Jest |
| `package-lock.json` | Lockfile NPM | Resolucion concreta de dependencias; modificado localmente antes de este lote |
| `nest-cli.json` | Configuracion Nest CLI | Raiz de fuentes y limpieza de salida |
| `tsconfig.json` | Configuracion TypeScript | Compilacion NodeNext a `dist` |
| `tsconfig.build.json` | Configuracion de build | Exclusion de pruebas y artefactos no productivos |
| `eslint.config.mjs`, `.prettierrc` | Calidad de codigo | Reglas estaticas y formato |

**Evidence**
- File: `package.json`; `package-lock.json`; `nest-cli.json`; `tsconfig.json`; `tsconfig.build.json`; `eslint.config.mjs`; `.prettierrc`
- Function / Method / Procedure: Manifiestos y configuraciones de herramienta
- Line / Section: `package.json:2-4,23-43,45-139`; `package-lock.json:1-22`; `nest-cli.json:2-7`; `tsconfig.json:2-23`; `tsconfig.build.json:2-3`; `eslint.config.mjs:7-40`; `.prettierrc:1-4`
- Condition / Query / Statement: Los archivos declaran NestJS 11, TypeScript, scripts de build/test y las reglas de compilacion y calidad.
- Confidence: High

### 2. Bootstrap y composicion

| Artefacto | Tipo | Proposito observado |
|---|---|---|
| `src/main.ts` | Entrada de proceso | Construccion de la aplicacion HTTP y registro de comportamiento global |
| `src/app.module.ts` | Modulo raiz NestJS | Composicion de infraestructura y modulos funcionales |

**Evidence**
- File: `src/main.ts`; `src/app.module.ts`
- Function / Method / Procedure: `bootstrap`; `AppModule`
- Line / Section: `src/main.ts:21-170`; `src/app.module.ts:41-143`
- Condition / Query / Statement: `main.ts` crea y escucha la aplicacion; `AppModule` declara todos los imports y proveedores globales.
- Confidence: High

### 3. Codigo transversal, configuracion y datos

| Ruta | Cantidad | Proposito observado |
|---|---:|---|
| `src/common/` | 12 | Cache, colas, auditoria, cifrado, filtros, interceptor, interfaces y utilidades |
| `src/config/` | 1 | Configuracion centralizada desde variables de entorno |
| `src/database/` | 3 | Modulo global y acceso PostgreSQL |

**Evidence**
- File: `src/common/cache/redis-cache.module.ts`; `src/common/queues/queue.module.ts`; `src/common/services/audit.module.ts`; `src/common/services/encryption.module.ts`; `src/config/configuration.ts`; `src/database/database.module.ts`
- Function / Method / Procedure: Declaraciones de modulos transversales y funcion de configuracion
- Line / Section: `src/common/cache/redis-cache.module.ts:12-38`; `src/common/queues/queue.module.ts:11-74`; `src/common/services/audit.module.ts:4-8`; `src/common/services/encryption.module.ts:4-8`; `src/config/configuration.ts:3-153`; `src/database/database.module.ts:4-8`
- Condition / Query / Statement: Las rutas contienen servicios globales compartidos y la configuracion de sus dependencias.
- Confidence: High

### 4. Modulos funcionales

`src/modules/` contiene 101 archivos distribuidos entre 13 directorios: `auth`, `certificate`, `document`, `emisores`, `image`, `pdf`, `puntos-emision`, `signature`, `sri`, `status`, `template`, `tenants` y `webhooks`. Su mapa estable se documenta en `02-module-inventory.md`.

**Evidence**
- File: `src/modules/*`; `src/app.module.ts`
- Function / Method / Procedure: Inventario `git ls-files src/modules/**`; imports de `AppModule`
- Line / Section: `src/app.module.ts:20-37,103-119`; 101 rutas bajo `src/modules/`
- Condition / Query / Statement: Los trece directorios tienen declaracion de modulo y aparecen importados por el modulo raiz.
- Confidence: High

### 5. Esquema y bootstrap PostgreSQL

| Artefacto | Tipo | Proposito observado |
|---|---|---|
| `database/init.sql` | Dump PostgreSQL | Tipo enumerado, 29 tablas, relaciones, indices y datos iniciales |
| `database/Install BD.txt` | Guia operativa | Diagnostico e instalacion manual de `uuid-ossp` |

La guia contiene comandos de parada, compilacion y eliminacion de esquema. Se trato unicamente como evidencia textual y no fue ejecutada. La autoridad del procedimiento se mantiene en `PFV-011`; el estado operativo del usuario inicial, en `PFV-018`.

**Evidence**
- File: `database/init.sql`; `database/Install BD.txt`
- Function / Method / Procedure: Dump PostgreSQL; pasos manuales de instalacion y recuperacion
- Line / Section: `database/init.sql:1-7,35-47,51-748,1012-1015`; `database/Install BD.txt:1-23,30-44`
- Condition / Query / Statement: El dump contiene objetos y datos; la guia alterna nombres de base de datos y termina con una operacion destructiva de recuperacion.
- Confidence: High

### 6. Configuracion de entorno

`.env.example` enumera variables de servidor, almacenamiento, renderizado, firma, SRI, PostgreSQL, Redis, JWT, CORS, throttling, health checks y colas. Solo se inventariaron nombres y categorias; ningun valor sensible se reproduce.

**Evidence**
- File: `.env.example`; `.gitignore`; `src/config/configuration.ts`
- Function / Method / Procedure: Plantilla de entorno; reglas de exclusion; carga de configuracion
- Line / Section: `.env.example:7-111`; `.gitignore:38-41`; `src/config/configuration.ts:3-153`
- Condition / Query / Statement: Los archivos `.env` reales estan excluidos, mientras `.env.example` y `configuration.ts` describen el contrato de configuracion.
- Confidence: High

### 7. Empaquetado y despliegue

| Artefacto | Tipo | Proposito observado |
|---|---|---|
| `Dockerfile` | Build y runtime | Imagen multi-stage Node 22 |
| `docker-compose.yml` | Orquestacion local | Redis y API construida localmente |
| `docker-compose.prod.yml` | Orquestacion de servidor | Redis, imagen publicada, volumenes y health check |
| `DEPLOYMENT.md` | Guia operativa | Docker, Nginx, variables, actualizacion y troubleshooting |

La guia no se considera automaticamente la fuente operativa vigente; su autoridad y divergencias estan centralizadas en `PFV-017`.

**Evidence**
- File: `Dockerfile`; `docker-compose.yml`; `docker-compose.prod.yml`; `DEPLOYMENT.md`
- Function / Method / Procedure: Etapas Docker; servicios Compose; secciones de despliegue
- Line / Section: `Dockerfile:8-64`; `docker-compose.yml:9-92`; `docker-compose.prod.yml:20-103`; `DEPLOYMENT.md:16-60,91-157,236-257,321-387`
- Condition / Query / Statement: Los artefactos describen variantes de construccion y operacion; la guia contiene contratos de endpoint distintos del controlador actual.
- Confidence: Medium

### 8. Contrato API de ejemplo

`Collection/Api_Facturacion_Sri.json` es una coleccion Postman con 17 grupos que cubren estado, autenticacion, tenants, emisores, certificados, puntos de emision, secuenciales, SRI, catalogos, webhooks y documentos. Sus cuerpos de ejemplo no se trataron como datos productivos.

El encabezado contiene material de credenciales de ejemplo. Los valores fueron deliberadamente omitidos de toda documentacion AS-IS; su posible relacion con el usuario sembrado se mantiene en `PFV-018`.

**Evidence**
- File: `Collection/Api_Facturacion_Sri.json`
- Function / Method / Procedure: Informacion y grupos de la coleccion Postman
- Line / Section: `Collection/Api_Facturacion_Sri.json:2-10,12-2286`
- Condition / Query / Statement: La coleccion declara los grupos y solicitudes; el encabezado incluye material sensible de ejemplo que no se reproduce.
- Confidence: High

### 9. Pruebas

Se encontraron tres suites unitarias dentro de `src/modules/sri/` y una prueba E2E bajo `test/`. No se encontraron suites rastreadas para los otros doce directorios funcionales en este inventario. No se ejecutaron pruebas en este lote.

**Evidence**
- File: `src/modules/sri/services/clave-acceso.service.spec.ts`; `src/modules/sri/services/factura-totales.spec.ts`; `src/modules/sri/sri-controller-tenant.spec.ts`; `test/app.e2e-spec.ts`; `test/jest-e2e.json`
- Function / Method / Procedure: Suites `describe`; configuracion Jest E2E
- Line / Section: `src/modules/sri/services/clave-acceso.service.spec.ts:4`; `src/modules/sri/services/factura-totales.spec.ts:7`; `src/modules/sri/sri-controller-tenant.spec.ts:7`; `test/app.e2e-spec.ts:7-24`; `test/jest-e2e.json:1-9`
- Condition / Query / Statement: El indice contiene esas cuatro suites y ninguna otra ruta `*.spec.ts`; su resultado runtime no fue verificado.
- Confidence: High

### 10. Documentacion y gobierno del repositorio

| Artefacto | Proposito observado | Tratamiento AS-IS |
|---|---|---|
| `README.md` | Proposito, capacidades, ejemplos y estructura | Evidencia secundaria; contrastar con codigo |
| `DEPLOYMENT.md` | Operacion declarada | Evidencia secundaria; `PFV-017` |
| `CHANGELOG.md` | Historial y propuestas | Las secciones marcadas "Propuesta" se excluyen del AS-IS |
| `CONTRIBUTING.md` | Convenciones de desarrollo | Contexto de ingenieria, no comportamiento funcional |
| Nueve `docs/*.md` | Documentacion historica | No disponible; `PFV-003` |

**Evidence**
- File: `README.md`; `CHANGELOG.md`; `CONTRIBUTING.md`; rutas historicas `docs/*.md`
- Function / Method / Procedure: Secciones de documentacion; estado Git
- Line / Section: `README.md:17-103,483-524`; `CHANGELOG.md:9-17,40-42`; `CONTRIBUTING.md:49-103,207-273`; archivos `docs/*.md` completos no disponibles
- Condition / Query / Statement: El README describe el sistema; el changelog etiqueta trabajo futuro como propuesta; Git marca los nueve documentos historicos como eliminados.
- Confidence: Medium

## Exclusiones del lote

| Ruta o clase | Motivo |
|---|---|
| `node_modules/`, `dist/`, `build/` | Dependencias o salida generada |
| `coverage/`, `.nyc_output/` | Salida de pruebas |
| Logs y reportes diagnosticos | Datos runtime no rastreados |
| `.env`, `.env.*` salvo `.env.example` | Potenciales secretos |
| `data/`, plantillas, PDF, certificados y XML runtime | Datos persistentes no incluidos en Git |
| `.agents/`, `.codex/`, `AGENTS.md` | Metadatos de agentes, no producto |
| Secciones futuras de `CHANGELOG.md` | Propuestas, no comportamiento actual |

**Evidence**
- File: `.gitignore`; `.dockerignore`; `CHANGELOG.md`
- Function / Method / Procedure: Patrones de exclusion y marcadores de version
- Line / Section: `.gitignore:1-20,38-65`; `.dockerignore:5-57`; `CHANGELOG.md:9-17,40-42`
- Condition / Query / Statement: Los patrones excluyen dependencias, secretos y datos runtime; el changelog identifica explicitamente varias versiones como propuestas.
- Confidence: High

## Estado de cobertura

El lote 1 inventario fuentes, entry points, modulos, datos e integraciones. El lote 2 profundizo los servicios SRI comunes utilizados por Factura y el flujo de emision de ese comprobante, con reglas, validaciones, flujos, acceso, salidas, errores y backlog de preservacion documentados. Esta cobertura no se extiende automaticamente a los otros comprobantes ni a los demas modulos.

**Evidence**
- File: `src/modules/`; `database/init.sql`; `Collection/Api_Facturacion_Sri.json`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/sri-soap.client.ts`
- Function / Method / Procedure: Inventario estructural del lote 1; `FacturaService.emitirFactura`; `CreateFacturaDto`; validadores compartidos; `SriSoapClient.enviarYAutorizar`
- Line / Section: 101 archivos de modulos; dump completo; grupos de coleccion; `src/modules/sri/services/factura.service.ts:42-723`; `src/modules/sri/dto/factura.dto.ts:20-127`; `src/modules/sri/services/sri-base.service.ts:23-197`; `src/modules/sri/services/sri-soap.client.ts:84-212`
- Condition / Query / Statement: El inventario cubre el snapshot estructural y el analisis detallado agregado se limita al camino comun y de Factura seleccionado para el lote 2.
- Confidence: High

## Ledger de cobertura - Lote 1

| Ruta / artefacto | Cobertura de este lote | Motivo o limite | Modulos relacionados | Documentos actualizados | Confianza | PFV abiertos |
|---|---|---|---|---|---|---|
| Manifiestos y archivos raiz | Inventario y configuracion estructural | No se ejecutaron scripts ni despliegues | MOD-001, MOD-002 | 00, 01, 02, 03, 08, 13 | High | PFV-008, PFV-017 |
| `src/main.ts`, `src/app.module.ts` | Bootstrap, middleware global, composicion y montajes | Sin verificacion runtime | MOD-001 | 00, 02, 03 | High | PFV-002, PFV-008, PFV-009 |
| `src/modules/*/*.module.ts` y controllers | Modulos y 79 rutas controller inventariados | Cuerpos de servicios y DTO no analizados exhaustivamente | MOD-008 a MOD-020 | 02, 03 | High | PFV-001, PFV-002, PFV-004 a PFV-009 |
| `database/` y `src/database/` | 29 tablas, relaciones, pool y brechas de bootstrap | Sin acceso a una instancia desplegada ni historial de migraciones | MOD-003 y modulos persistentes | 01, 04, 08, 13 | High | PFV-011, PFV-013, PFV-014, PFV-018 |
| Redis, BullMQ y listeners | Dos workers, dos listeners y sincronizacion manual | Sin inspeccion de jobs o metricas runtime | MOD-004, MOD-005, MOD-012, MOD-013 | 02, 03, 08, 09, 13 | High | PFV-004, PFV-005, PFV-006, PFV-016 |
| SOAP SRI, Carbone, webhooks e imagenes remotas | Mecanismos y direcciones confirmados | Sin trafico, contratos remotos ni configuracion desplegada | MOD-012, MOD-013, MOD-016, MOD-017 | 00, 08, 09, 13 | High | PFV-010, PFV-017 |
| Filesystem de XML, P12, plantillas, PDF e imagenes | Rutas, montajes y persistencia hibrida | Sin datos runtime, backup ni politica operativa | MOD-012, MOD-014 a MOD-019 | 04, 08, 13 | High | PFV-012, PFV-015 |
| Servicios y DTO del nucleo SRI | Descubrimiento parcial para entry points, datos e integraciones | En el lote 1 quedaron fuera reglas, validaciones, calculos y alternativas; Factura fue profundizada en el lote 2 y los otros tipos siguen pendientes | MOD-012 | 00, 02, 03, 04, 08, 09, 13 | Medium | PFV-001, PFV-005 a PFV-007, PFV-010, PFV-013 a PFV-015 |
| Pruebas rastreadas | Inventario de cuatro suites | No se ejecutaron ni se usaron como unica confirmacion funcional | MOD-001, MOD-012 | 00, 01 | High | PFV-002 |
| Nueve `docs/*.md` historicos | Sin cobertura; archivos ausentes | Eliminados antes del lote y no restaurados | Por determinar | 00, 01, 14 | High | PFV-003 |

**Evidence**
- File: indice Git; `src/`; `database/`; `Collection/Api_Facturacion_Sri.json`; documentos del lote bajo `docs/as-is/`
- Function / Method / Procedure: inventario `git ls-files`, busquedas dirigidas y revision estatica descrita en cada documento
- Line / Section: 151 archivos rastreados; 142 legibles; referencias detalladas en 00, 02, 03, 04, 08, 09, 13 y 14
- Condition / Query / Statement: el ledger distingue descubrimiento completo, analisis parcial, evidencia ausente y trabajo no ejecutado para evitar afirmar cobertura funcional total.
- Confidence: High

## Ledger de cobertura - Lote 2

| Ruta / artefacto | Cobertura de este lote | Motivo o limite | Modulos relacionados | Documentos actualizados | Confianza | PFV abiertos |
|---|---|---|---|---|---|---|
| `sri.controller.ts`, `sri.service.ts`, `sri-emision.processor.ts` y registros de `sri-emision` | Acceso previo, despacho directo/asincrono, payload y delegacion del job de Factura | Sin inspeccion de jobs, Redis ni configuracion runtime; opciones, idempotencia, alcance RUC y retencion no confirmados | MOD-005, MOD-010, MOD-012 | 00, 02, 05, 07, 09, 10, 11, 12, 13, 14, 15 | High | PFV-001, PFV-005, PFV-007, PFV-031, PFV-032, PFV-036, PFV-038 |
| `dto/factura.dto.ts`, `dto/common.dto.ts`, `sri-base.service.ts`, validadores de identificacion y catalogos | Contrato DTO, validaciones estructurales, de identificacion y de catalogos aplicadas a Factura | Sin confirmar minimos funcionales, fecha calendaria, consistencia aritmetica, seleccion temporal ni actividad jerarquica | MOD-012 | 00, 05, 06, 14, 15 | High | PFV-019 a PFV-022, PFV-026 a PFV-028, PFV-037 |
| `factura.service.ts`, clave de acceso, XML builder/signer, certificados y cliente SOAP | Flujo estatico completo de Factura: preparar, secuenciar, construir, firmar, enviar, resolver estados, persistir, emitir eventos y responder | Sin certificado, trafico SRI, fallos inducidos ni ejecucion del flujo | MOD-012, MOD-014 | 00, 05, 06, 07, 08, 10, 11, 12, 13, 14, 15 | High | PFV-023 a PFV-025, PFV-029, PFV-030, PFV-032 a PFV-036, PFV-038, PFV-039 |
| `sri-repository.service.ts`, `xml-storage.service.ts`, `database.service.ts` y tablas de comprobantes | Reserva de secuencial, seleccion de emisor, grafo de persistencia de Factura y limite PostgreSQL/filesystem | Sin instancia desplegada, archivos runtime ni procedimiento confirmado de reconciliacion | MOD-003, MOD-012 | 00, 04, 05, 07, 10, 11, 12, 13, 14, 15 | High | PFV-023, PFV-024, PFV-033, PFV-035, PFV-036 |
| Servicios y DTO de Nota de Credito, Nota de Debito, Retencion y Guia de Remision | Solo delimitacion de dependencias compartidas y seleccion del siguiente lote | Sus reglas, calculos, persistencia y alternativas no fueron extraidos en profundidad | MOD-012 | 01, 02 | High | Pendiente de lotes posteriores |

**Evidence**
- File: `src/modules/sri/sri.controller.ts`; `src/modules/sri/sri.service.ts`; `src/modules/sri/processors/sri-emision.processor.ts`; `src/common/queues/queue.module.ts`; `src/modules/sri/sri.module.ts`; `src/config/configuration.ts`; `src/modules/sri/dto/factura.dto.ts`; `src/modules/sri/dto/common.dto.ts`; `src/modules/sri/services/sri-base.service.ts`; `src/modules/sri/services/catalogo-validator.service.ts`; `src/modules/sri/services/factura.service.ts`; `src/modules/sri/services/clave-acceso.service.ts`; `src/modules/sri/services/xml-builder.service.ts`; `src/modules/sri/services/xml-signer.service.ts`; `src/modules/sri/services/sri-soap.client.ts`; `src/modules/sri/services/sri-repository.service.ts`; `src/modules/sri/services/xml-storage.service.ts`; `src/database/database.service.ts`; `src/modules/certificate/certificate.controller.ts`; `database/init.sql`; `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/services/retencion.service.ts`; `src/modules/sri/services/guia-remision.service.ts`
- Function / Method / Procedure: despacho/processor; DTO y validadores; `FacturaService.emitirFactura`, calculos y persistencia; clave, XML, firma, SOAP, repositorio, almacenamiento y certificado; servicios restantes delimitados
- Line / Section: controller 84-110; SriService 52-80; processor 24-46; QueueModule 29-49; DTO Factura 20-127 y DTO comunes; base 23-197; catalogo 84-100, 321-407; Factura 42-723; colaboradores SRI citados completos; certificate controller 63-421; database/init y otros servicios en las secciones citadas por los documentos 00, 02 y 04 a 15
- Condition / Query / Statement: la revision profundizo Factura y sus colaboradores directos, incluidos acceso/certificado/cola; los otros cuatro comprobantes solo se delimitaron.
- Confidence: High

## Siguiente lote sugerido

Analizar Nota de Credito como siguiente lote acotado, incluyendo las reglas de documento modificado que comparte estructuralmente con Nota de Debito: codigo, numero y fecha del documento sustento, validacion de catalogo, construccion XML, persistencia, estados, eventos y errores. La coincidencia de campos no se promueve aun a una regla funcional comun; el lote debe contrastar ambos caminos y registrar cualquier diferencia o incertidumbre antes de reutilizar una conclusion.

**Evidence**
- File: `src/modules/sri/services/nota-credito.service.ts`; `src/modules/sri/dto/nota-credito.dto.ts`; `src/modules/sri/services/nota-debito.service.ts`; `src/modules/sri/dto/nota-debito.dto.ts`; `src/modules/sri/services/sri-base.service.ts`
- Function / Method / Procedure: `NotaCreditoService.emitirNotaCredito`; `CreateNotaCreditoDto`; `NotaDebitoService.emitirNotaDebito`; `CreateNotaDebitoDto`; `SriBaseService.validarDocumentoSustentoCatalogo`
- Line / Section: `src/modules/sri/services/nota-credito.service.ts:40-176,221-223,378-380`; `src/modules/sri/dto/nota-credito.dto.ts:80-176`; `src/modules/sri/services/nota-debito.service.ts:41-75,233-235,372-374`; `src/modules/sri/dto/nota-debito.dto.ts:78-112`; `src/modules/sri/services/sri-base.service.ts:177-197`
- Condition / Query / Statement: Nota de Credito es el siguiente servicio SRI no profundizado y ambos comprobantes modificatorios exponen los tres datos del documento sustento y llaman al validador compartido de su codigo.
- Confidence: High
