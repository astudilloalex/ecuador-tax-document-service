# AS-IS 04 - Data Model

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Modelo fisico inicial basado en `database/init.sql` y uso observado desde servicios. No se documentan todavia todas las queries CRUD ni cardinalidades funcionales completas.

## Persistencia Confirmada

DM-001: El sistema usa PostgreSQL mediante `pg.Pool`; la configuracion viene de `database.*` y el servicio provee consultas parametrizadas, transacciones e inserts/updates auxiliares.

**Evidence**
- File: `src/database/database.service.ts`
- Function / Method / Procedure: `DatabaseService.onModuleInit`, `query`, `transaction`, `insert`, `update`
- Line / Section: lines 22-45, 95-131, 171-186, 192-257
- Condition / Query / Statement: inicializa `Pool`, ejecuta query con params, maneja transacciones y helpers SQL.
- Confidence: High

DM-002: El esquema inicial define roles de usuario `SUPERADMIN`, `ADMIN`, `USER`.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: DDL `CREATE TYPE public.user_role`
- Line / Section: lines 39-43
- Condition / Query / Statement: enum `user_role` con los tres roles.
- Confidence: High

## Grupos De Tablas

| Area | Tablas / Estructuras | Observacion AS-IS | Evidence | Confidence |
|---|---|---|---|---|
| Auditoria | `auditoria` | Trazabilidad con usuario, tenant, accion, recurso, metadata, exito/error y duracion. | `database/init.sql:54-79` | High |
| Catalogos SRI | `catalogo_documentos_sustento`, `catalogo_formas_pago`, `catalogo_impuestos`, `catalogo_motivos_traslado`, `catalogo_retenciones`, `catalogo_tarifas_impuesto`, `catalogo_tipos_identificacion` | Catalogos y semillas iniciales para codigos SRI, formas de pago, impuestos, motivos, retenciones e identificacion. | `database/init.sql:100-198`, `database/init.sql:780-895` | High |
| Comprobantes | `comprobantes`, `comprobante_detalles`, `comprobante_impuestos`, `comprobante_pagos`, `comprobante_retenciones`, `comprobante_totales`, `comprobante_xmls`, `detalles_adicionales`, `info_adicional`, `motivos_nota_debito`, `guia_destinatarios`, `guia_detalles` | Modelo central de comprobantes electronicos y estructuras dependientes por tipo/detalle/XML. | `database/init.sql:205-406`, `database/init.sql:538-610`, `database/init.sql:1757-1830` | High |
| Emisores y puntos | `emisores`, `establecimientos`, `puntos_emision`, `secuenciales` | Emisores por tenant, establecimientos, puntos y control de secuenciales por punto/tipo. | `database/init.sql:432-531`, `database/init.sql:629-650`, `database/init.sql:1845-1846` | High |
| Multi-tenant y usuarios | `tenants`, `usuarios` | Tenants, usuarios con hash de password, rol, tenant y estado activo. Valor sensible de hash no reproducido. | `database/init.sql:684-705`, `database/init.sql:1015` | High |
| Configuracion sistema | `sistema_config` | Configuracion clave/valor como limite sync y TTL cache sembrados. | `database/init.sql:670-683`, `database/init.sql:998-1002` | High |
| Webhooks | `webhook_configs`, `webhook_logs` | Configuracion de webhooks por tenant/emisor/eventos/secreto y bitacora de intentos. | `database/init.sql:722-760`, `src/modules/webhooks/webhook.processor.ts:111-137` | High |

## Constraints E Indices Relevantes

DM-003: Existen unicidades para `usuarios.email`, `comprobantes.clave_acceso`, `emisores(tenant_id,ruc)`, `puntos_emision(establecimiento_id,codigo)` y `secuenciales(punto_emision_id,tipo_comprobante)`.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: DDL constraints
- Line / Section: lines 1218-1226, 1242-1250, 1298-1322, 1354-1362
- Condition / Query / Statement: `UNIQUE` y `PRIMARY KEY` sobre claves funcionales principales.
- Confidence: High

DM-004: Existen indices para busquedas por comprobantes, catalogos, auditoria, emisores, usuarios y webhooks.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: DDL indexes
- Line / Section: lines 1386-1694
- Condition / Query / Statement: crea indices `idx_comprobantes_*`, `idx_emisores_*`, `idx_usuarios_*`, `idx_webhook_*`, entre otros.
- Confidence: High

## Archivos Como Persistencia

DM-005: Los XML se almacenan en filesystem y la BD guarda rutas relativas de XML firmado/autorizado.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: table/comment `comprobante_xmls`
- Line / Section: lines 327-347
- Condition / Query / Statement: columnas `xml_autorizado_path`, `xml_firmado_path`; comentario describe estructura `{ruc}/{year}/{month}/{claveAcceso}_autorizado.xml`.
- Confidence: High

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `saveXml`, `readXml`
- Line / Section: lines 53-78, 135-138
- Condition / Query / Statement: escribe y lee XML desde filesystem.
- Confidence: High

