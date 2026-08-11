# FloatOps API

Backend REST de FloatOps — sistema de gestión para talleres de reparación vehicular. Construido con [Quarkus](https://quarkus.io/), el Supersonic Subatomic Java Framework.

Este proyecto es parte del monorepo `FloatOps/` y vive en `backend/`. Para el contexto completo del producto (requerimientos, casos de uso, diagramas), ver [`../docs/`](../docs/).

## Stack

- **Java 21** + **Quarkus 3.38.1**
- **Hibernate ORM with Panache** — persistencia, entidades como active record
- **PostgreSQL** — base de datos relacional
- **Flyway** — migraciones de esquema versionadas
- **Keycloak / OIDC** — autenticación (extensión aún no agregada al proyecto — ver [Próximos pasos](#próximos-pasos))

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
   docker compose up -d postgres keycloak
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

El contrato OpenAPI de este módulo (Órdenes de trabajo) está versionado por separado en [`../openapi/openapi-ordenes-trabajo.yaml`](../openapi/openapi-ordenes-trabajo.yaml), junto con los otros 10 módulos del sistema. Todavía no se agregó la extensión `quarkus-smallrye-openapi` a este proyecto — cuando se agregue, hay que decidir si el spec se genera desde las anotaciones `@Path` o si se sirve el YAML ya escrito como documento estático (`src/main/resources/META-INF/openapi.yaml`).

## Próximos pasos

- [ ] Agregar `quarkus-oidc` y conectar con el realm de Keycloak (`../keycloak/floatops-realm.json`)
- [ ] Agregar `quarkus-smallrye-openapi` y decidir generación vs. documento estático
- [ ] Implementar `repository/`, `dto/`, `service/`, `resource/` para `WorkOrder` (las entidades ya existen)
- [ ] Definir estrategia de pruebas (unitarias vs. Testcontainers)
- [ ] Migración `V2` para completar `Vehicle`/`Technician` cuando esos módulos se implementen

## Documentación relacionada

- [`../docs/README.md`](../docs/README.md) — arquitectura, módulos, modelo de datos completo
- [`../docs/requerimientos-floatops.md`](../docs/requerimientos-floatops.md) — los 40 RF, matriz de permisos, RNF
- [`../diagrams/`](../diagrams/) — diagramas de clases, casos de uso, secuencia, despliegue
- [`../openapi/`](../openapi/) — los 11 contratos de API, uno por módulo