# AS-IS 12 - Error Handling

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. No se completo catalogo de errores por todos los modulos.

## Manejo De Errores Confirmado

### ERR-001 - Filtro global normaliza respuestas de error

El filtro global captura excepciones, deriva status/mensaje, loguea metodo/URL/status/mensaje y responde `{ success: false, error }`; en desarrollo incluye stack.

**Evidence**
- File: `src/common/filters/http-exception.filter.ts`
- Function / Method / Procedure: `AllExceptionsFilter.catch`
- Line / Section: lines 15-45
- Condition / Query / Statement: procesa `HttpException`/`Error`, loguea y responde JSON.
- Confidence: High

### ERR-002 - DB no disponible retorna ServiceUnavailable

Si el pool PostgreSQL no esta inicializado, `query` y `getClient` lanzan `ServiceUnavailableException`.

**Evidence**
- File: `src/database/database.service.ts`
- Function / Method / Procedure: `query`, `getClient`
- Line / Section: lines 99-103, 159-165
- Condition / Query / Statement: guard sobre `!this.pool`.
- Confidence: High

### ERR-003 - Transacciones DB hacen rollback ante error

El helper `transaction` ejecuta `BEGIN`, `COMMIT`, y `ROLLBACK` si el callback falla.

**Evidence**
- File: `src/database/database.service.ts`
- Function / Method / Procedure: `transaction`
- Line / Section: lines 171-185
- Condition / Query / Statement: try/catch con `ROLLBACK` y `client.release`.
- Confidence: High

### ERR-004 - Auth devuelve Unauthorized/Conflict/NotFound segun escenario

Login falla con credenciales invalidas o usuario inactivo; refresh invalido expira; registro duplica email como conflicto; tenant inexistente como not found.

**Evidence**
- File: `src/modules/auth/auth.service.ts`
- Function / Method / Procedure: `login`, `refreshToken`, `register`, `changePassword`, `validatePayload`
- Line / Section: lines 44-60, 76-91, 150-166, 202-208, 229-235
- Condition / Query / Statement: lanza excepciones especificas NestJS.
- Confidence: High

### ERR-005 - Webhook no exitoso provoca retry de BullMQ

Si el POST de webhook responde no-OK o falla, el processor registra intento fallido, loguea y relanza error.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `process`
- Line / Section: lines 77-107
- Condition / Query / Statement: `if (!response.ok) throw new Error(...)`; catch loguea y `throw error`.
- Confidence: High

### ERR-006 - Reintento/anulacion SRI rechazan estados o artefactos faltantes

Anulacion y reintento lanzan `BadRequestException` ante comprobante inexistente, estado no permitido, XML faltante o archivo no encontrado.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `anularComprobante`, `reintentarComprobante`
- Line / Section: lines 431-453, 477-528
- Condition / Query / Statement: validaciones y `BadRequestException`.
- Confidence: High

