# DigitalFix BFF — Caso 6

BFF Spring Boot 4 + Java 17. Valida JWT de Microsoft Entra ID y enruta a microservicios. No toca DB.

## Arquitectura

Angular + MSAL → API Gateway AWS (`Stage-API-Prueba`) → BFF `:8080` → `ms-workorders:8082` + `ms-catalog:8081`

## Validación JWT

`config/SecurityConfig.java` + `application.yml`:

- Issuer: `https://login.microsoftonline.com/0c1f677f-ce2e-4fd2-8562-f5052fab4086/v2.0`
- Audience: `API_AUDIENCE=api://2cd82242-bced-4d52-974a-09fbbf1a9e4d/access_as_user` (normaliza v1/v2 a GUID)
- Firma: JWKS vía `JwtDecoders.fromIssuerLocation`
- Vigencia: `JwtTimestampValidator` (exp/nbf)
- Errores: `401 Token requerido o invalido`, `403 Permiso insuficiente para el rol`

## Autorización por roles

Mapeo claim `roles` → `ROLE_*` (case-insensitive). Reglas:

- `GET /api/catalog/**`: Admin, Supervisor, Cliente
- `POST/PUT/DELETE/PATCH /api/catalog/**`: Admin, Supervisor
- `PATCH /api/workorders/*/status`: Admin, Supervisor (Cliente crea y sigue, no cambia estado)
- `DELETE /api/workorders/**`: Admin, Supervisor
- `GET/POST /api/workorders/**`: autenticado (el MS filtra por email si es Cliente)

Propaga `X-User-Email` (preferred_username) y `X-User-Role` al MS.

## Regla de negocio

Al `PATCH .../status {ASIGNADA}`: pre-check `GET orden + GET repuesto stock>0`, luego `PATCH`, luego `POST catalog/repuestos/{id}/consumir` (-1). Si no hay stock → `409` sin asignar.

## Env

```
WORKORDERS_URL=http://ms-workorders:8082
CATALOG_URL=http://ms-catalog:8081
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
API_AUDIENCE=api://2cd82242-bced-4d52-974a-09fbbf1a9e4d/access_as_user
```

## Run

```
./mvnw -DskipTests package
docker compose up -d --build
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/catalog/servicios # 401 sin token OK
```

## Colección Postman EP1

https://benjamin-1874968.postman.co/workspace/Benjamin's-Workspace~edccb9a7-d9b0-4dac-8429-645e20bcb7f8/collection/45505473-ff1f78c6-e53c-4f7b-b861-e5387a510074?action=share&creator=45505473
