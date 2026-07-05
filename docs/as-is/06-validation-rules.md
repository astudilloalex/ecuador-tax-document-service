# AS-IS 06 - Validation Rules

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Validaciones profundas de DTOs SRI quedan para lote 2.

| ID | Field / Object | Validation | Failure Behavior | Evidence | Confidence | Related PFV |
|---|---|---|---|---|---|---|
| VR-001 | Todos los DTOs HTTP | `ValidationPipe` global transforma, elimina propiedades no declaradas y rechaza no-whitelisted. | Error de validacion NestJS filtrado por filtro global. | `src/main.ts:53-60` | High |  |
| VR-002 | CORS origin | Solo origen sin header, o dentro de `allowedOrigins`; otros origenes producen error CORS. | callback con `Error("CORS: Origen no permitido")`. | `src/main.ts:34-51` | High |  |
| VR-003 | `LoginDto.email/password` | email valido y no vacio; password string no vacio. | class-validator via global pipe. | `src/modules/auth/dto/auth.dto.ts:18-28` | High |  |
| VR-004 | `RegisterUserDto` | email valido, password minimo 8, rol enum opcional, tenantId UUID opcional. | class-validator via global pipe. | `src/modules/auth/dto/auth.dto.ts:30-53` | High |  |
| VR-005 | `ChangePasswordDto` | currentPassword requerido; newPassword requerido, string, minimo 8. | class-validator via global pipe. | `src/modules/auth/dto/auth.dto.ts:55-65` | High |  |
| VR-006 | XML upload SRI | Archivo `file` requerido; XML parseable; tag de comprobante valido; firma `ds:Signature`; claveAcceso de 49 digitos. | Retorna `{ valido: false, errores: [...] }` para validacion de XML o falta archivo. | `src/modules/sri/sri.controller.ts:287-303`, `src/modules/sri/sri.service.ts:211-258` | High |  |
| VR-007 | `verificarEnSri.claveAcceso` | claveAcceso debe tener longitud 49. | `BadRequestException`. | `src/modules/sri/sri.service.ts:593-605` | High |  |
| VR-008 | Identificadores SQL internos | Nombres de tablas/columnas deben cumplir regex `^[a-zA-Z_][a-zA-Z0-9_.]*$`. | `Error` antes de construir SQL helper. | `src/database/database.service.ts:17-18`, `src/database/database.service.ts:81-88` | High |  |

## Hallazgo De Validacion Global

VR-FIND-001: La aplicacion aplica validacion global a todos los controladores, por lo que las reglas de DTO tienen efecto transversal en runtime NestJS.

**Evidence**
- File: `src/main.ts`
- Function / Method / Procedure: `bootstrap`
- Line / Section: lines 53-60
- Condition / Query / Statement: `app.useGlobalPipes(new ValidationPipe({ transform: true, whitelist: true, forbidNonWhitelisted: true }))`.
- Confidence: High

