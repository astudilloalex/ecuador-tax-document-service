# AS-IS 00 - System Overview

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Este documento resume solo evidencia revisada en inventario inicial, bootstrap, modulos NestJS, entry points principales, seguridad transversal, colas, persistencia principal y webhooks. No representa cobertura completa del sistema.

## Proposito Confirmado

El sistema `open-api-facturacion-sri` se declara como una API REST NestJS para emitir, firmar y gestionar comprobantes electronicos ante el SRI Ecuador, incluyendo facturas, notas de credito/debito, retenciones y guias de remision.

**Evidence**
- File: `package.json`
- Function / Method / Procedure: package metadata
- Line / Section: lines 2-10
- Condition / Query / Statement: `name`, `description` y `keywords` identifican API de facturacion electronica SRI Ecuador compatible con XAdES-BES.
- Confidence: High

**Evidence**
- File: `README.md`
- Function / Method / Procedure: seccion "Que hace esta API?"
- Line / Section: lines 17-27
- Condition / Query / Statement: describe flujo REST JSON -> XML -> firma XAdES-BES -> SRI SOAP -> autorizacion -> RIDE/Webhook/DB.
- Confidence: Medium

## Capacidades Observadas

- Emision de comprobantes SRI mediante endpoints HTTP bajo `/sri`.
- Generacion y validacion de XML, firma XAdES-BES y consulta SOAP al SRI.
- Procesamiento asincrono configurable mediante BullMQ/Redis para emision SRI.
- Gestion multi-tenant con emisores, puntos de emision y secuenciales.
- Autenticacion JWT global con excepciones `@Public()` y roles `SUPERADMIN`, `ADMIN`, `USER`.
- Webhooks de salida para eventos `comprobante.autorizado` y `comprobante.rechazado`.
- Generacion, guardado y descarga de PDFs/documentos, templates, imagenes y firma digital de PDFs.
- Persistencia PostgreSQL y almacenamiento filesystem para XML/PDF/certificados/templates.

**Evidence**
- File: `src/app.module.ts`
- Function / Method / Procedure: `AppModule`
- Line / Section: lines 41-143
- Condition / Query / Statement: importa modulos de configuracion, throttling, archivos estaticos, servicios comunes, base de datos, auth y modulos funcionales.
- Confidence: High

**Evidence**
- File: `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `SriModule`
- Line / Section: lines 27-69
- Condition / Query / Statement: registra controladores SRI/catalogos, servicios de comprobantes, XML, SOAP, firma, repositorio y processor `sri-emision`.
- Confidence: High

## Limites del Lote 1

No se profundizo todavia en reglas internas completas de cada comprobante, calculo tributario, construccion XML por tipo, validez de catalogos contra normativa SRI, servicios CRUD administrativos ni generacion PDF detallada. Esos temas quedan para lotes posteriores.

## Riesgos / Brechas Iniciales

- RISK-001: contrato de rutas SRI documentado en README difiere de rutas implementadas en `SriController`.
- RISK-002: existen credenciales iniciales documentadas/sembradas para superadmin; los valores sensibles no se reproducen en esta documentacion.
- RISK-003: `package-lock.json` ya estaba modificado antes de este lote; no se altero codigo ni configuracion de aplicacion.

**Evidence**
- File: `README.md`
- Function / Method / Procedure: API Endpoints / Ejemplo Real
- Line / Section: lines 171-176, 295-310
- Condition / Query / Statement: README muestra rutas como `/sri/factura/emitir`.
- Confidence: Medium

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `SriController.emitirFactura`
- Line / Section: lines 84-110
- Condition / Query / Statement: codigo implementa `@Controller('sri')` + `@Post('emitir/factura')`, es decir `POST /sri/emitir/factura`.
- Confidence: High

