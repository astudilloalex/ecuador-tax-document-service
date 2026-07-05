# AS-IS 10 - Security Access Control

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. No se auditaron todos los endpoints administrativos en profundidad.

## Reglas Confirmadas

### SEC-001 - JWT global por defecto

Todos los endpoints quedan protegidos por guard JWT global salvo los marcados con `@Public()`.

**Evidence**
- File: `src/app.module.ts`
- Function / Method / Procedure: `AppModule.providers`
- Line / Section: lines 120-125
- Condition / Query / Statement: registra `JwtAuthGuard` como `APP_GUARD`.
- Confidence: High

**Evidence**
- File: `src/modules/auth/guards/jwt-auth.guard.ts`
- Function / Method / Procedure: `JwtAuthGuard.canActivate`
- Line / Section: lines 16-28
- Condition / Query / Statement: si metadata `IS_PUBLIC_KEY` existe retorna true; en caso contrario ejecuta `AuthGuard('jwt')`.
- Confidence: High

### SEC-002 - Endpoints publicos identificados en lote 1

`/auth/login`, `/auth/refresh` y `/status` estan marcados publicos; otros endpoints dependen del guard global salvo evidencia de `@Public()` no revisada.

**Evidence**
- File: `src/modules/auth/auth.controller.ts`
- Function / Method / Procedure: `login`, `refresh`
- Line / Section: lines 42-63
- Condition / Query / Statement: decorador `@Public()` en login y refresh.
- Confidence: High

**Evidence**
- File: `src/modules/status/status.controller.ts`
- Function / Method / Procedure: `StatusController`
- Line / Section: lines 15-17
- Condition / Query / Statement: `@Public()` a nivel de controller.
- Confidence: High

### SEC-003 - Roles globales por metadata

El guard de roles global permite acceso si no hay roles requeridos; si hay metadata `@Roles`, exige que `user.rol` coincida.

**Evidence**
- File: `src/app.module.ts`
- Function / Method / Procedure: `AppModule.providers`
- Line / Section: lines 126-130
- Condition / Query / Statement: registra `RolesGuard` como `APP_GUARD`.
- Confidence: High

**Evidence**
- File: `src/modules/auth/guards/roles.guard.ts`
- Function / Method / Procedure: `RolesGuard.canActivate`
- Line / Section: lines 19-48
- Condition / Query / Statement: lee `ROLES_KEY`, verifica usuario y rol; lanza `ForbiddenException` si no cumple.
- Confidence: High

### SEC-004 - Registro de usuarios requiere SUPERADMIN

`POST /auth/register` exige `@Roles(UserRole.SUPERADMIN)`.

**Evidence**
- File: `src/modules/auth/auth.controller.ts`
- Function / Method / Procedure: `register`
- Line / Section: lines 75-90
- Condition / Query / Statement: decorador `@Roles(UserRole.SUPERADMIN)`.
- Confidence: High

### SEC-005 - Refresh tokens no son aceptados para acceder a recursos protegidos

`AuthService.validatePayload` rechaza payloads con `type === 'refresh'`.

**Evidence**
- File: `src/modules/auth/auth.service.ts`
- Function / Method / Procedure: `validatePayload`
- Line / Section: lines 223-239
- Condition / Query / Statement: si `payload.type === 'refresh'`, lanza `UnauthorizedException`.
- Confidence: High

### SEC-006 - Passwords de usuario se comparan/guardan con bcrypt

Login usa `bcrypt.compare`; registro y cambio de password usan `bcrypt.hash` con 12 rounds.

**Evidence**
- File: `src/modules/auth/auth.service.ts`
- Function / Method / Procedure: `login`, `register`, `changePassword`
- Line / Section: lines 24, 53-60, 169-175, 206-214
- Condition / Query / Statement: `BCRYPT_ROUNDS = 12`, compare/hash de password.
- Confidence: High

### SEC-007 - Secretos y credenciales estan parametrizados por env/config y no deben exponerse

La configuracion requiere JWT/encryption/directorios y define passwords/certs como variables; los valores sensibles encontrados en plantillas/SQL se omiten/redactan en esta documentacion.

**Evidence**
- File: `src/config/configuration.ts`
- Function / Method / Procedure: configuration factory
- Line / Section: lines 69-72, 89-117
- Condition / Query / Statement: `JWT_SECRET`, `ENCRYPTION_KEY`, `ENCRYPTION_SALT`, DB password, Redis password, cert password/paths.
- Confidence: High

### SEC-008 - Headers HTTP y CORS se configuran en bootstrap

Helmet se instala con CSP desactivado y CORS restringe origenes contra `cors.allowedOrigins`.

**Evidence**
- File: `src/main.ts`
- Function / Method / Procedure: `bootstrap`
- Line / Section: lines 26-51
- Condition / Query / Statement: `helmet({ crossOriginEmbedderPolicy: false, contentSecurityPolicy: false })` y `app.enableCors`.
- Confidence: High

