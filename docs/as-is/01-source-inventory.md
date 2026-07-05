# AS-IS 01 - Source Inventory

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Inventario liviano basado en `git ls-files`, `rg --files`, `find`, lectura de archivos de bootstrap/configuracion/controladores y SQL inicial. Se excluye `node_modules` por ser dependencia instalada/no fuente.

## Inventario Inicial

| ID | Artifact | Type | Path | Purpose / Observed Role | Related Module | Evidence | Confidence |
|---|---|---|---|---|---|---|---|
| SRC-001 | Metadata npm | Config | `package.json` | Nombre, descripcion, scripts y dependencias runtime/dev. | Global | `package.json:2-88` | High |
| SRC-002 | README principal | Documentacion | `README.md` | Describe proposito, arquitectura declarada, endpoints y variables. | Global | `README.md:17-103`, `README.md:282-424` | Medium |
| SRC-003 | Bootstrap NestJS | Codigo | `src/main.ts` | Configura Helmet, CORS, ValidationPipe, filtro global, Swagger y listen. | MOD-001 | `src/main.ts:21-170` | High |
| SRC-004 | Root module | Codigo | `src/app.module.ts` | Ensambla modulos globales y guards/interceptor globales. | MOD-001 | `src/app.module.ts:41-143` | High |
| SRC-005 | Configuracion central | Codigo | `src/config/configuration.ts` | Lee variables requeridas/opcionales de servidor, SRI, JWT, DB, Redis, directorios, colas. | MOD-001 | `src/config/configuration.ts:3-153` | High |
| SRC-006 | Servicio DB | Codigo | `src/database/database.service.ts` | Pool PostgreSQL, consultas parametrizadas, transacciones y sanitizacion de identificadores. | MOD-012 | `src/database/database.service.ts:12-258` | High |
| SRC-007 | SQL inicial | SQL | `database/init.sql` | Esquema PostgreSQL, catalogos semilla, constraints e indices. | MOD-012 | `database/init.sql:39-1878` | High |
| SRC-008 | Modulos NestJS | Codigo | `src/modules/**` | Modulos funcionales por dominio. | MOD-002..MOD-011 | `src/app.module.ts:20-38`, `src/app.module.ts:103-118` | High |
| SRC-009 | Controladores HTTP | Codigo | `src/modules/**/*.controller.ts` | Entry points REST. | EP-* | `rg` de decoradores controller/get/post/patch/delete en `src/modules` | High |
| SRC-010 | Procesadores BullMQ | Codigo | `src/modules/sri/processors/sri-emision.processor.ts`, `src/modules/webhooks/webhook.processor.ts` | Jobs `sri-emision` y `webhook-dispatch`. | EP-034, EP-035 | `src/modules/sri/processors/sri-emision.processor.ts:10-47`, `src/modules/webhooks/webhook.processor.ts:20-144` | High |
| SRC-011 | Coleccion Postman | Contract sample | `Collection/Api_Facturacion_Sri.json` | Coleccion API disponible; no revisada en detalle en lote 1. | Pendiente | `git ls-files` | Low |
| SRC-012 | Documentacion existente | Documentacion | `docs/*.md` | Docs tecnicas por modulo existentes; no revisadas en detalle en lote 1. | Pendiente | `git ls-files` | Low |
| SRC-013 | Docker/deploy | Ops config | `Dockerfile`, `docker-compose*.yml`, `DEPLOYMENT.md` | Artefactos de despliegue; solo se revisaron senales de env/Redis/puerto. | Pendiente | `rg` env/config en docker files | Medium |

## Hallazgos De Inventario

INV-001: El repositorio contiene codigo fuente NestJS/TypeScript, SQL inicial, documentacion tecnica, coleccion API y artefactos Docker.

**Evidence**
- File: repository root
- Function / Method / Procedure: `git ls-files`
- Line / Section: output inventory
- Condition / Query / Statement: lista archivos bajo `src/`, `database/`, `docs/`, `Collection/`, Docker y configuracion Node/NestJS.
- Confidence: High

INV-002: Existe `node_modules` en el workspace, pero no esta rastreado por Git y se excluye del analisis AS-IS por ser dependencia instalada.

**Evidence**
- File: repository root
- Function / Method / Procedure: `find . -maxdepth 3 -type d`
- Line / Section: output inventory
- Condition / Query / Statement: `node_modules` aparece en filesystem; no aparece en `git ls-files`.
- Confidence: High

INV-003: El arbol de trabajo ya tenia cambios no relacionados antes de escribir docs AS-IS.

**Evidence**
- File: repository root
- Function / Method / Procedure: `git status --short`
- Line / Section: command output
- Condition / Query / Statement: `M package-lock.json`, `?? .codex/`, `?? AGENTS.md` estaban presentes.
- Confidence: High
