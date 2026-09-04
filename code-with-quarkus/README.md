# FloatOps API

Backend REST de FloatOps — sistema de gestión para talleres de reparación vehicular. Construido con [Quarkus](https://quarkus.io/), el Supersonic Subatomic Java Framework.

Este proyecto es parte del monorepo `FloatOps/` y vive en `backend/`. Para el contexto completo del producto (requerimientos, casos de uso, diagramas), ver [`../docs/`](../docs/).

## Stack

- **Java 21** + **Quarkus 3.38.1**
- **Hibernate ORM with Panache** — persistencia, entidades como active record
- **PostgreSQL** — base de datos relacional
- **Flyway** — migraciones de esquema versionadas
- **Keycloak / OIDC** — autenticación y autorización por rol (`quarkus-oidc` + `@RolesAllowed`), conectado al realm `floatops`
- **SmallRye OpenAPI** — expone Swagger UI y el spec generado desde las anotaciones `@Path`
- **MinIO / Amazon S3** (`quarkus-amazon-s3`) — almacenamiento de fotos de órdenes (RF-009/RF-014), MinIO en dev, S3 real en producción

## Estructura del proyecto

Organizado por capa (no por módulo de negocio) — ver `../docs/README.md` para la justificación de esta decisión:

```
src/main/java/com/floatops/
├── resource/        # endpoints REST (@Path)
├── service/          # logica de negocio
├── repository/        # PanacheRepository / metodos estaticos por entidad
├── entity/            # modelo de dominio (JPA/Panache)
├── dto/                # request/response, separados de entity/
└── shared/             # excepciones, seguridad

src/main/resources/
├── application.properties
└── db/migration/       # scripts de Flyway (V1__init.sql, V2__..., etc.)
```

Por ahora solo existe `entity/` con las 10 entidades del núcleo de Órdenes de trabajo (`WorkOrder`, `Stage`, `ChecklistItem`, `Photo`, `WorkOrderPart`, `ServiceType`, `PartCategory`, `Part`, y los stubs `Vehicle`/`Technician`).

## Requisitos previos

- Java 21
- Docker (para PostgreSQL y Keycloak — no hace falta instalarlos localmente)
- El Maven wrapper (`./mvnw`) ya está incluido, no requiere instalación separada de Maven

## Ejecutar en modo desarrollo

Quarkus dev mode da recarga en caliente: los cambios en el código se aplican sin reiniciar.

1. Desde la **raíz del monorepo** (no desde `backend/`), levanta solo las dependencias, no el propio backend:
   ```shell script
   docker compose up -d postgres keycloak minio
   ```
2. Confirma que Postgres está listo (no solo "running", sino "healthy"):
   ```shell script
   docker compose ps
   ```
3. Desde `backend/` (donde está este `pom.xml`), arranca Quarkus:
   ```shell script
   ./mvnw quarkus:dev
   ```
4. Dev UI disponible en <http://localhost:8080/q/dev/> — incluye una consola de base de datos para verificar tablas y datos sin salir del navegador.

Al arrancar, Flyway corre automáticamente las migraciones pendientes (ver `quarkus.flyway.migrate-at-start=true` en `application.properties`). Busca en el log una línea confirmando la migración a la versión más reciente.

## Migraciones de base de datos

Los scripts viven en `src/main/resources/db/migration/`, con el naming de Flyway: `V{numero}__descripcion.sql`.

- **`V1__init.sql`**: crea las tablas del núcleo de Órdenes de trabajo, más tablas stub mínimas de `vehicles` y `technicians` (solo lo necesario para las foreign keys).
- Los módulos de Vehículos y Técnicos deben **ampliar** esas tablas stub con una migración nueva (`V2__vehicle_fields.sql`, por ejemplo, con `ALTER TABLE`) — nunca editar `V1__init.sql` una vez aplicado, Flyway no permite modificar migraciones ya corridas.
- Nunca se usa `quarkus.hibernate-orm.database.generation=update` ni `drop-and-create` — el esquema vive únicamente en estos scripts SQL versionados. Hibernate solo **valida** que las entidades coincidan (`database.generation=validate`).

## Pruebas

```shell script
./mvnw test
```

Framework: JUnit 5 (incluido por defecto en el arquetipo de Quarkus). La estrategia de cobertura (¿pruebas de integración con Testcontainers contra Postgres real, o solo unitarias?) sigue como decisión abierta — ver `../docs/README.md`, sección de próximos pasos. El objetivo de cobertura acordado es ≥ 80% (RNF-007 en `../docs/requerimientos-floatops.md`).

## Pruebas manuales contra Keycloak

Los tests automatizados (`WorkOrderResourceTest`) usan `@TestSecurity` para simular roles sin necesitar un Keycloak real corriendo. Para probar la integración real de punta a punta (token real, emitido por Keycloak real, validado por `quarkus-oidc` real), hazlo a mano así:

### 1. Prerrequisitos

- `docker compose up -d postgres keycloak` desde la raíz del monorepo
- `./mvnw quarkus:dev` desde `backend/`
- El realm `floatops` debe tener los 4 usuarios de prueba definidos en `../keycloak/floatops-realm.json` (uno por rol: `admin.prueba`, `despachador.prueba`, `tecnico.prueba`, `supervisor.prueba`, todos con password `prueba123`)

### 2. Obtener un token

Usa el client `floatops-web` (público, no necesita `client_secret`) con el grant de contraseña directa:

```shell script
curl -X POST http://localhost:8081/realms/floatops/protocol/openid-connect/token \
  -d 'client_id=floatops-web' -d 'grant_type=password' \
  -d 'username=despachador.prueba' -d 'password=prueba123'
```

Para no copiar el token gigante a mano en cada prueba, guárdalo en una variable:

```shell script
TOKEN=$(curl -s -X POST http://localhost:8081/realms/floatops/protocol/openid-connect/token \
  -d 'client_id=floatops-web' -d 'grant_type=password' \
  -d 'username=despachador.prueba' -d 'password=prueba123' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### 3. Probar un endpoint

```shell script
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/v1/work-orders
```

Nota el prefijo `/v1` — viene de la sección `servers:` de cada contrato OpenAPI, no lo olvides al probar cualquier endpoint.

### 4. Confirmar que la restricción de rol bloquea de verdad

La prueba que realmente importa no es que un endpoint funcione, sino que **rechace a quien no debería tener acceso**. Ejemplo con `registerSupervisorQuoteApproval` (`@RolesAllowed({"supervisor-gerente"})`, RF-039):

```shell script
# Con token de administrador.prueba (rol "administrador", NO autorizado aqui)
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v1/work-orders/00000000-0000-0000-0000-000000000000/supervisor-approval
```

Debe devolver `403 Forbidden`. Si en cambio usas un token de `supervisor.prueba`, debe devolver `404 Not Found` (el UUID de prueba no existe, pero eso confirma que la autorización sí lo dejó pasar).

### 5. Crear una nueva orden de reparación
```shell script
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'accept: application/json' 'http://localhost:8080/v1/work-orders' -d '{"vehicleId": "f585c483-80d1-4fbf-a8c7-3e438432d290", "tipoServicio": "Cambio de aceite", "descripcion": "Cambio de aceite camioneta", "prioridad": "BAJA", "fechaProgramada": "2026-08-14", "technicianId": "913347f3-ba6a-4413-8bef-95bcf3fe468b", "partIds": ["C72aFC59-E1dC-A6C3-f7a9-30D926AcCB16"]}'
```

### Tabla de usuarios de prueba

| Usuario | Password | Rol |
|---|---|---|
| `admin.prueba` | `prueba123` | administrador |
| `despachador.prueba` | `prueba123` | despachador |
| `tecnico.prueba` | `prueba123` | tecnico |
| `supervisor.prueba` | `prueba123` | supervisor-gerente |

> ⚠️ Estas credenciales son de **desarrollo local únicamente** — viven en texto plano en `floatops-realm.json`, nunca deben existir así en un ambiente real.

## Almacenamiento de fotos (MinIO/S3)

`POST /work-orders/{id}/photos` (RF-009/RF-014) sube el archivo a **MinIO** en desarrollo, o a **S3 real** en producción — mismo código (`quarkus-amazon-s3`), solo cambia el endpoint configurado.

- **Consola web de MinIO:** <http://localhost:9001> (usuario `floatops` / password `floatops123`)
- **Bucket:** `floatops-photos` — se crea automáticamente al arrancar Quarkus (`S3BucketInitializer`), MinIO no lo crea solo como sí haría S3 real con algunas configuraciones
- **`CI_CD=true`** en el servicio de MinIO del `docker-compose.yml` reduce su consumo de RAM de ~1GB a ~256MB — necesario mantenerlo así en desarrollo, ver discusión de footprint en el historial de decisiones

⚠️ **Pendiente:** `Photo.url` guarda la *key* interna del objeto en S3, no una URL accesible desde el navegador. Falta implementar generación de URLs firmadas temporales (`S3Presigner`) — no se confirmó si `quarkus-amazon-s3` lo expone como bean inyectable automáticamente.

### Pruebas manuales de Note y almacenamiento de fotos — ✅ verificado de punta a punta

Con un token de `tecnico.prueba` (`@RolesAllowed` de ambos endpoints) y una orden real ya creada:

```shell script
# Agregar nota (RF-014) - devuelve 204 No Content
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/v1/work-orders/{workOrderId}/notes" \
  -H 'Content-Type: application/json' \
  -d '{"texto": "Cliente reporta ruido al frenar"}'

# Confirmar que quedo guardada - debe aparecer dentro de "notes"
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/v1/work-orders/{workOrderId}"

# Adjuntar foto (RF-009/RF-014) - sube de verdad a MinIO
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/v1/work-orders/{workOrderId}/photos" \
  -F 'file=@/ruta/a/una-imagen.jpg'
```

Confirmado el 12/08/2026: la nota aparece en el `GET` con el texto correcto, y la foto aparece físicamente en el bucket `floatops-photos` de la consola de MinIO con la key `{workOrderId}/{uuid}` esperada.

## Empaquetado

```shell script
./mvnw package
```

Produce `target/quarkus-app/quarkus-run.jar` (no es über-jar; las dependencias quedan en `target/quarkus-app/lib/`). Se ejecuta con:

```shell script
java -jar target/quarkus-app/quarkus-run.jar
```

Para generar un über-jar en su lugar:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

## Ejecutable nativo

```shell script
./mvnw package -Dnative
```

O, sin GraalVM instalado localmente, compilando dentro de un contenedor:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Ejecutable resultante: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`. Más detalles: <https://quarkus.io/guides/maven-tooling>.

> No es una prioridad para el MVP — se deja documentado por si en algún momento se necesita reducir tiempos de arranque o consumo de memoria en producción.

## Despliegue con Docker

El `docker-compose.yml` en la **raíz del monorepo** define el servicio `quarkus-api`, que construye la imagen desde este directorio (`context: ./backend`) usando `src/main/docker/Dockerfile.jvm`.

Para levantar la aplicación empaquetada (no modo desarrollo) junto con sus dependencias:

```shell script
# desde la raiz del monorepo
docker compose up -d --build
```

Dentro de la red de Docker, el backend se conecta a `postgres:5432` y `keycloak:8080` (nombres de servicio, no `localhost`) — ver `../docker-compose.yml` para las variables de entorno exactas. Esto es distinto a la configuración de `application.properties` usada en modo desarrollo, que apunta a `localhost:5433` porque en ese caso Quarkus corre fuera de Docker.

## Contrato de API

El contrato OpenAPI de este módulo (Órdenes de trabajo) está versionado por separado en [`../ApiContract/openapi-ordenes-trabajo.yaml`](../ApiContract/openapi-ordenes-trabajo.yaml), junto con los otros 10 módulos del sistema. `WorkOrdersApi` (la interfaz que implementa `WorkOrderResource`) se **genera automáticamente** de ese `.yaml` en cada build vía `openapi-generator-maven-plugin` — ver la fase `generate-sources` en `pom.xml`. Con `quarkus-smallrye-openapi` agregado, Swagger UI (`/q/swagger-ui`) muestra el spec derivado de esas mismas anotaciones — el círculo completo: contrato → código generado → código real → contrato de vuelta.

## Próximos pasos

- [x] Agregar `quarkus-oidc` y aplicar `@RolesAllowed` según la matriz de permisos — verificado con `curl` real (403/404 correctos)
- [x] Agregar `quarkus-smallrye-openapi`
- [x] Implementar `dto/`, `service/`, `resource/` para `WorkOrder` vía `openapi-generator-maven-plugin`
- [x] `RolesResources.java` — era una prueba del propio Edwin, ya eliminada
- [x] Entidad `Note` agregada (RF-014) — migración `V2__add_notes.sql`, **verificado con pruebas manuales el 12/08/2026**
- [x] Almacenamiento real de fotos vía MinIO/S3 (`quarkus-amazon-s3`) — **verificado con pruebas manuales el 12/08/2026**, ver [Almacenamiento de fotos](#almacenamiento-de-fotos-minios3)
- [ ] Generar URLs firmadas temporales (`S3Presigner`) para que el frontend pueda mostrar las fotos — no verificado si se inyecta automático con `quarkus-amazon-s3`
- [ ] **Bloqueante real para seguir probando manualmente:** no existen endpoints para crear Vehículos, Técnicos ni Repuestos — actualmente se siembran a mano vía `V900__dev_seed_data.sql` (solo vehículo y tipo de servicio). El módulo de Vehículos es el siguiente a implementar, siguiendo exactamente el mismo patrón ya probado en Órdenes de trabajo (entidad → migración → contrato generado → service → resource → `@RolesAllowed` → tests)
- [ ] `tipoServicio` se busca por nombre de texto en vez de por `serviceTypeId` (UUID) — frágil
- [ ] Definir estrategia de pruebas de integración con base de datos (Dev Services ya configurado para tests, ver `application.properties`)
- [ ] Migración `V2` para completar `Vehicle`/`Technician` cuando esos módulos se implementen
- [ ] Repetir el patrón completo (entidad → migración → generador → seguridad) para los otros 10 módulos

## Documentación relacionada

- [`../docs/README.md`](../docs/README.md) — arquitectura, módulos, modelo de datos completo
- [`../docs/requerimientos-floatops.md`](../docs/requerimientos-floatops.md) — los 40 RF, matriz de permisos, RNF
- [`../diagrams/`](../diagrams/) — diagramas de clases, casos de uso, secuencia, despliegue
- [`../openapi/`](../openapi/) — los 11 contratos de API, uno por módulo