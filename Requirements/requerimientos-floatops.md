# Definición de Requerimientos — FloatOps

> Versión: 0.15
> Fecha: 07/08/2026
> Autor: Edwin Suares
> Estado: Borrador

---

## 1. Descripción general

FloatOps es un sistema de gestión para **talleres de reparación y mantenimiento vehicular** (inicialmente con foco en talleres en Bogotá). Este proyecto absorbe el alcance que antes se exploraba por separado bajo el nombre **TallerFlow** — ya no son proyectos distintos; FloatOps es el único proyecto activo y cubre ese dominio. Al taller llegan dos tipos de clientes: **empresas** que solicitan cotizaciones y llevan su flota a mantenimiento o reparación, y **personas naturales** que solicitan cotización y reparación para su vehículo particular. En ambos casos, el flujo central es el mismo: se cotiza, se crea una orden de trabajo, se repara en el taller y se hace seguimiento hasta completarla.

El sistema resuelve la falta de trazabilidad digital sobre el estado de cada vehículo que pasa por el taller: hoy no hay una forma centralizada de saber qué vehículos requieren atención, en qué etapa está cada orden de trabajo, quién la tiene asignada, ni qué repuestos y presupuesto implica. FloatOps centraliza esta operación en un panel para administradores/despachadores y una vista móvil para los técnicos que ejecutan el trabajo en taller.

El proyecto está en etapa temprana: se cuenta con wireframes validados (11 pantallas) y las decisiones de alcance descritas en este documento, pero varias pantallas administrativas (gestión de vehículos, técnicos, repuestos, informes, login, administración, gestión de clientes) aún no tienen mockup. Los mockups actuales no distinguen todavía si un vehículo pertenece a una empresa o a una persona natural — ver nota en sección 5. El desarrollo es liderado por un único desarrollador (sin equipo), lo cual condiciona la priorización del MVP.

---

## 2. Actores

| ID | Actor | Tipo | Descripción |
|----|-------|------|-------------|
| A01 | Administrador | Humano | Acceso total: gestiona usuarios, catálogos base (tipos de servicio, categorías de repuestos) y configuración general. ⚠️ Rol propuesto, pendiente de confirmar como definitivo. |
| A02 | Despachador | Humano | Usuario principal del panel: crea y gestiona órdenes de trabajo, asigna técnicos, gestiona flota y repuestos. ⚠️ Rol propuesto, pendiente de confirmar como definitivo. |
| A03 | Técnico | Humano | Acceso limitado desde la vista móvil: consulta su orden asignada, actualiza checklist, adjunta fotos y notas. |
| A04 | Job programado | Automático | Proceso interno que evalúa condiciones (orden atrasada, stock bajo) y dispara notificaciones. |
| A05 | Proveedor de identidad (Keycloak) | Externo | Servicio de autenticación y autorización (OIDC) que valida credenciales y roles. |
| A06 | Cliente | Humano / Organización | Empresa (con su flota) o persona natural que solicita cotización y trae su vehículo al taller para reparación o mantenimiento, ya sea acercándose al establecimiento o por otro medio de comunicación (ej. teléfono). ✅ Confirmado: no requiere login ni acceso directo al sistema — sus datos son registrados por el despachador. |
| A07 | Facturación | Externo | API de facturación electrónica (DIAN). |
| A08 | Proveedor de notificaciones | Externo | Servicios de envío de email y WhatsApp (ej. WhatsApp Business API) usados para notificar a los usuarios. |
| A09 | Supervisor / Gerente | Humano | Rol confirmado, pendiente de definir su matriz de permisos específica frente a Administrador y Despachador. |
| A10 | Contador / Facturación | Humano | Rol confirmado. Además de informes y datos de costos, gestionará directamente la futura facturación electrónica DIAN (A07) cuando se implemente — confirmado que DIAN queda fuera del MVP, por lo que este alcance aplica a una siguiente iteración. Matriz de permisos aún por afinar. |

**Notas:**
- Administrador y Despachador son actores primarios del panel; Técnico es actor primario de la vista móvil.
- Job programado y Keycloak son actores secundarios (soporte).
- La lista definitiva de roles y su matriz de permisos está pendiente de confirmación (ver sección 5, restricciones y supuestos).

---

### 2.1 Matriz de permisos por rol

Para Administrador, Despachador y Técnico, esta matriz se deriva directamente de la columna "Actor(es)" de la tabla de RF (sección 3) — no son decisiones nuevas. Para Supervisor/Gerente y Contador/Facturación no hay RF que los mencione todavía, así que las celdas marcadas 🔶 son una **propuesta a validar**, no una confirmación.

| Módulo | Administrador | Despachador | Técnico | Supervisor / Gerente | Contador / Facturación |
|---|---|---|---|---|---|
| Panel | Total | Total | Ninguno | ✅ Solo lectura | ✅ Solo lectura |
| Órdenes de trabajo | Total | Total (crear, kanban, reasignar, aprobar cotización) | Parcial: solo su orden asignada — ver detalle, checklist, fotos, marcar lista para revisión (RF-009, 011-015) | ✅ Aprueba cotizaciones de alto valor (umbral por definir) — solo lectura del resto | 🔶 Solo lectura del presupuesto/cotización (propuesto) |
| Vehículos | Total | Total | Ninguno | ✅ Solo lectura | ✅ Solo lectura |
| Clientes | Total | Total | Ninguno | ✅ Solo lectura | ✅ Solo lectura, datos de facturación |
| Técnicos | Total (CRUD, RF-021) | Solo lectura (RF-020) | Ninguno | ✅ Solo lectura | Ninguno |
| Repuestos | Total (RF-022) | Ninguno (solo selecciona catálogo dentro de una orden) | Ninguno | ✅ Solo lectura | ✅ Solo lectura, costos |
| Informes | Total | Total | Ninguno | ✅ Total | ✅ Total |
| Autenticación | Propio (login/logout/contraseña) | Propio | Propio | Propio | Propio |
| Administración | Total (RF-024, 025, 035) | Ninguno | Ninguno | Ninguno | ✅ Gestión de facturación DIAN cuando se implemente (fuera del MVP) |
| Notificaciones | Recibe | Recibe | Recibe | ✅ Recibe | ✅ Recibe |
| Auditoría | Total (registro automático + consulta) | Ninguno | Ninguno | 🔶 Solo lectura (propuesto) | Ninguno |

**Notas:**
- ✅ **Matriz completa — sin celdas 🔶 pendientes.**
- El acceso "Parcial" del Técnico en Órdenes de trabajo está limitado a la orden que tiene asignada — no ve el listado completo ni el kanban.
- Supervisor/Gerente aprueba cotizaciones de alto valor (RF-039) — ⚠️ el umbral exacto de valor sigue pendiente.
- Falta actualizar los actores de RF-023 (Informes) y RF-026 (Notificaciones) para listar explícitamente a Supervisor/Gerente y Contador/Facturación.



Agrupados por módulo, en el mismo orden usado en `README.md`. Los IDs (`RF-XXX`) se conservan sin cambios para no romper referencias cruzadas entre documentos.

### 3.1 Panel

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-002 | Consultar panel de KPIs | El sistema debe mostrar en el panel principal los indicadores de vehículos pendientes, órdenes abiertas y total de flota. | Despachador, Administrador | Alta | - Muestra conteos actualizados<br>- Incluye tabla de "vehículos que requieren servicio" |
| RF-003 | Navegar a detalle desde el panel | El sistema debe permitir navegar al detalle de un vehículo o de una orden haciendo clic desde el panel. | Despachador, Administrador | Alta | - Clic en fila de vehículo abre su perfil<br>- Clic en fila de orden abre su detalle |

### 3.2 Órdenes de trabajo

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-004 | Tablero kanban de órdenes | El sistema debe mostrar las órdenes de trabajo agrupadas por etapa. Las etapas son configurables (ver RF-035) — no son fijas. | Despachador, Administrador | Alta | - Cada columna muestra el conteo de órdenes<br>- Refleja las etapas definidas en la configuración vigente |
| RF-005 | Mover orden entre etapas (drag & drop) | El sistema debe permitir cambiar la etapa de una orden arrastrándola entre columnas del kanban. | Despachador, Administrador | Alta | - El cambio de etapa se persiste al soltar la tarjeta<br>- Se dispara notificación (RF-026) y auditoría (RF-027) |
| RF-031 | Actualizar etapa desde el detalle de la orden | El sistema debe permitir cambiar la etapa de una orden mediante un botón/selector en la pantalla de detalle, como alternativa al drag & drop del kanban. | Despachador, Administrador | Alta | - Dispara la misma notificación (RF-026) y auditoría (RF-027) que el cambio vía kanban |
| RF-032 | Reasignar técnico en cualquier etapa | El sistema debe permitir reasignar el técnico de una orden en cualquier etapa de su ciclo de vida, no solo al crearla. | Despachador, Administrador | Alta | - Restringido por rol: solo Despachador o Administrador pueden reasignar; el Técnico no<br>- Dispara notificación (RF-026) y auditoría (RF-027) |
| RF-033 | Registrar aprobación del cliente sobre la cotización | El sistema debe permitir registrar si el cliente aprobó la cotización (presupuesto) de la orden. | Despachador, Administrador | Alta | - Como el cliente no tiene acceso al sistema, la aprobación se registra manualmente por el despachador tras confirmarla con el cliente (presencial/telefónica)<br>- ✅ Confirmado: el paso a la etapa "En taller" se bloquea si la cotización supera el umbral de alto valor (RF-039) y no cuenta con la aprobación adicional de Supervisor/Gerente. Por debajo del umbral, solo requiere esta aprobación del cliente |
| RF-039 | Aprobar cotización de alto valor | El sistema debe permitir a un Supervisor/Gerente aprobar cotizaciones que superen un umbral de valor, como paso adicional a la aprobación del cliente (RF-033). Una orden con cotización de alto valor no avanza a "En taller" sin esta aprobación. | Supervisor/Gerente | Alta | ✅ Confirmado: bloquea el avance de etapa si no está aprobada. ⚠️ Pendiente: el umbral exacto de valor |
| RF-037 | Consultar cola de órdenes abiertas | El sistema debe mostrar la cola de órdenes de trabajo abiertas (no completadas). | Despachador, Administrador | Alta | ✅ Confirmado en el mockup (slide 4, vista dividida flota + cola de órdenes) — se le había asignado descripción pero nunca un ID de RF, corregido en esta revisión |
| RF-006 | Crear orden de trabajo (modal rápido) | El sistema debe permitir crear una orden desde un modal con vehículo, tipo de servicio, descripción, prioridad y fecha programada. | Despachador, Administrador | Alta | - Todos los campos marcados obligatorios se validan antes de guardar |
| RF-007 | Crear orden de trabajo (formulario completo) | El sistema debe ofrecer un formulario completo de creación de orden que incluya selección de repuestos desde el catálogo. | Despachador, Administrador | Alta | - Muestra catálogo de repuestos con precio<br>- Calcula presupuesto en tiempo real |
| RF-008 | Asignar técnico al crear la orden | El sistema debe permitir asignar un técnico a la orden en el momento de su creación. | Despachador, Administrador | Alta | - El campo "técnico asignado" queda disponible en el formulario de creación |
| RF-009 | Adjuntar fotos a la orden | El sistema debe permitir adjuntar una o más fotos del problema al crear o editar una orden. | Despachador, Técnico | Media | - Soporta al menos 3 fotos por orden |
| RF-010 | Calcular presupuesto de la orden | El sistema debe calcular automáticamente el presupuesto (estimado, repuestos seleccionados, restante) al seleccionar repuestos. | Despachador, Administrador | Alta | - El total se recalcula al marcar/desmarcar un repuesto |
| RF-011 | Consultar detalle de orden | El sistema debe mostrar el detalle completo de una orden: descripción, checklist, técnico, vehículo, fechas, historial de servicio. | Despachador, Administrador, Técnico | Alta | - Incluye estado (ej. atrasado) y fechas de creación/vencimiento |
| RF-012 | Actualizar checklist de la orden | El sistema debe permitir marcar como completado cada ítem del checklist de tareas de una orden. | Despachador, Técnico | Alta | - El estado de cada ítem se persiste individualmente |
| RF-013 | Consultar orden asignada (vista móvil) | El sistema debe mostrar al técnico, en la vista móvil, el detalle de la orden que tiene asignada. | Técnico | Alta | - Incluye checklist, campo de foto y notas |
| RF-014 | Agregar foto y notas desde móvil | El sistema debe permitir al técnico adjuntar fotos y escribir notas sobre la orden desde la vista móvil. | Técnico | Media | — |
| RF-015 | Marcar orden lista para revisión (vista móvil) | El sistema debe permitir al técnico marcar la orden como "lista para revisión" desde la vista móvil una vez termina el trabajo. La finalización real (mover la orden a la etapa completada) es una acción separada del despachador, ya cubierta por RF-031 (actualizar etapa desde el detalle). | Técnico | Alta | ✅ Flujo confirmado: el técnico no completa la orden directamente, solo la marca lista; el despachador revisa y la completa |

### 3.3 Vehículos

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-016 | Consultar listado de flota | El sistema debe mostrar el listado completo de vehículos de la flota. | Despachador, Administrador | Alta | - Incluye filtros por estado (atrasado, próximo a vencer, programado) |
| RF-017 | Consultar perfil de vehículo | El sistema debe mostrar identificación (placa, VIN, marca/modelo, tipo, kilometraje) e historial de órdenes de un vehículo. | Despachador, Administrador | Alta | - Incluye contador de órdenes abiertas del vehículo |
| RF-018 | Registrar / editar vehículo | El sistema debe permitir crear y editar el registro de un vehículo, incluyendo la selección o creación inline de su cliente dueño (empresa o persona natural) como atajo. | Despachador, Administrador | Alta | - Campos mínimos: placa, VIN, marca, modelo, tipo, kilometraje<br>- Permite seleccionar un cliente existente o crear uno nuevo (RF-030) sin salir del formulario<br>- ✅ Confirmado: coexiste con la pantalla dedicada de gestión de clientes (RF-030), el inline es un atajo, no un reemplazo |
| RF-019 | Dar de baja un vehículo | El sistema debe permitir cambiar el estado de un vehículo a "dado de baja". | Administrador | Alta | - Un vehículo dado de baja no aparece disponible para nuevas órdenes |
| RF-034 | Filtrar listado de vehículos (filtros avanzados) | El sistema debe permitir filtrar el listado de vehículos por cliente, tipo, estado y otros criterios. | Despachador, Administrador | Media | ⚠️ Lista completa de criterios de filtro pendiente de definir — mínimo: cliente, tipo, estado |

### 3.4 Clientes

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-028 | Asociar vehículo a un cliente | El sistema debe permitir registrar el cliente dueño de un vehículo, ya sea una empresa (con posiblemente varios vehículos) o una persona natural (típicamente un vehículo). | Despachador, Administrador | Alta | - El registro distingue tipo de cliente: empresa o persona natural |
| RF-029 | Registrar cotización de un cliente | El sistema debe permitir registrar la solicitud de cotización de un cliente (empresa o persona natural) al crear la orden de trabajo, reutilizando el presupuesto ya calculado (RF-010). | Despachador, Administrador | Alta | ⚠️ Se asume que la cotización es equivalente al presupuesto de la orden (RF-010); a confirmar si debe existir como una etapa previa independiente a la creación de la orden (ej. "cotización pendiente de aprobación" antes de convertirse en orden) |
| RF-030 | Registrar / editar cliente | El sistema debe permitir crear y editar el registro de un cliente (empresa o persona natural), incluyendo clientes de paso (no recurrentes). | Despachador, Administrador | Alta | ✅ Todo cliente, recurrente o de paso, se registra con los mismos campos obligatorios — no existe un flujo simplificado alterno |

### 3.5 Técnicos

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-020 | Consultar listado/perfil de técnicos | El sistema debe mostrar, en un modal, el listado de técnicos con su disponibilidad y especialidad. | Despachador, Administrador | Media | — |
| RF-021 | Registrar / editar técnico | El sistema debe permitir crear y editar el registro de un técnico. | Administrador | Media | — |

### 3.6 Repuestos

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-022 | Gestionar catálogo de repuestos | El sistema debe permitir dar de alta repuestos y gestionar su stock. | Administrador | Alta | - Incluye código, nombre, precio y cantidad en stock |

### 3.7 Informes

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-023 | Generar informes | El sistema debe generar informes a partir de los datos almacenados (órdenes, técnicos, repuestos, vehículos). Para el MVP, el alcance se limita a: (1) órdenes por técnico o por período, y (2) tiempo promedio de resolución por tipo de servicio. | Despachador, Administrador, Supervisor/Gerente, Contador/Facturación | Alta | ✅ Alcance de MVP confirmado (2 informes). Los candidatos restantes (gasto en repuestos, vehículos con mayor frecuencia de incidencias, cumplimiento de mantenimiento programado) quedan para una siguiente iteración. Filtros y formato de exportación aún por definir |

### 3.8 Autenticación y roles

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-001 | Inicio de sesión con roles | El sistema debe permitir iniciar sesión y restringir el acceso a módulos/acciones según el rol del usuario. | Administrador, Despachador, Técnico | Alta | - Login vía Keycloak/OIDC<br>- El técnico solo ve sus órdenes asignadas en la vista móvil |
| RF-036 | Forzar actualización periódica de contraseña | El sistema debe exigir a los usuarios actualizar su contraseña cada 30 días. Si no la actualizan, el usuario queda inhabilitado hasta que la actualice. | Administrador, Despachador, Técnico | Alta | - Si han pasado 30 días desde la última actualización, el usuario queda inhabilitado<br>- Al intentar iniciar sesión con contraseña vencida, el sistema redirige directamente a la pantalla de actualización de contraseña, sin permitir acceso al resto del sistema |
| RF-038 | Cerrar sesión | El sistema debe permitir a cualquier usuario autenticado cerrar su sesión. | Administrador, Despachador, Técnico | Alta | ✅ Caso de uso implícito de todo sistema con login; no tenía RF asignado hasta esta revisión |

### 3.9 Administración

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-024 | Gestionar usuarios | El sistema debe permitir crear, editar, asignar rol y desactivar usuarios. | Administrador | Alta | — |
| RF-025 | Gestionar catálogos base | El sistema debe permitir administrar los catálogos de tipos de servicio y categorías de repuestos. | Administrador | Media | — |
| RF-035 | Configurar etapas del tablero kanban | El sistema debe permitir crear, editar, reordenar y desactivar las etapas del tablero kanban de órdenes de trabajo. | Administrador | Alta | - Las etapas no son fijas en código; viven como catálogo configurable<br>- Los valores actuales (Requiere atención, Programado, En taller, Completados) son la configuración inicial por defecto, no un límite |

### 3.10 Notificaciones

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-026 | Notificar acciones relevantes | El sistema debe enviar una notificación ante acciones relevantes (orden creada/asignada, cambio de etapa, orden atrasada, stock bajo, vehículo dado de baja, contraseña próxima a vencer). | Administrador, Despachador, Técnico, Supervisor/Gerente, Contador/Facturación | Alta | ✅ Canales confirmados: in-app, email y WhatsApp (requiere integración con proveedor de mensajería, ver A08) |

### 3.11 Auditoría

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-027 | Registrar auditoría de cambios | El sistema debe registrar automáticamente quién, cuándo y qué cambió en una orden, un presupuesto o el estado de un vehículo. | Sistema | Alta | - Cada registro incluye entidad, campo, valor anterior, valor nuevo, usuario y timestamp |

**Prioridad sugerida:** Alta (MVP) / Media (siguiente iteración) / Baja (nice to have)

---

## 4. Requerimientos no funcionales

| ID | Categoría | Descripción | Métrica / criterio de cumplimiento |
|----|-----------|-------------|--------------------------------------|
| RNF-001 | Rendimiento | El sistema debe responder a solicitudes de consulta del panel y del kanban con baja latencia. | [por definir — ej. p95 < X ms] |
| RNF-002 | Seguridad | Las credenciales deben gestionarse a través de Keycloak/OIDC; las contraseñas nunca se almacenan en texto plano en la aplicación. | Autenticación delegada a Keycloak (OIDC) |
| RNF-003 | Cumplimiento normativo | Al operar en Colombia y almacenar datos personales (usuarios, técnicos), el sistema debe respetar la Ley de Habeas Data (Ley 1581 de 2012). | Política de tratamiento de datos personales definida antes de producción |
| RNF-004 | Disponibilidad | El sistema debe estar disponible para consulta durante el horario operativo del taller. | [por definir — ej. uptime ≥ X% mensual] |
| RNF-005 | Escalabilidad | El backend (Quarkus) debe soportar el crecimiento de la flota y del número de órdenes sin rediseño mayor. | [por definir — ej. soporta X usuarios concurrentes] |
| RNF-006 | Usabilidad | La vista móvil del técnico debe ser operable en taller sin capacitación previa. | [por definir — ej. tasa de éxito de tarea ≥ X% en prueba de usabilidad] |
| RNF-007 | Mantenibilidad | El código (Kotlin, Quarkus, Compose Multiplatform) debe seguir estándares que faciliten el mantenimiento por un equipo pequeño. | [por definir — ej. cobertura de pruebas ≥ X%] |
| RNF-008 | Compatibilidad | El sistema debe funcionar en Web (dashboard) y en Android/iOS (vista móvil de técnicos) a partir de un mismo código compartido. | Kotlin Multiplatform + Compose Multiplatform (targets Web Wasm/JS y Android/iOS) |
| RNF-009 | Persistencia | Los datos deben almacenarse en un motor relacional que soporte las relaciones del modelo de datos definido. | PostgreSQL |
| RNF-010 | Seguridad | Las contraseñas deben rotarse obligatoriamente cada 30 días; vencido ese plazo, la cuenta queda inhabilitada hasta la actualización. | Configuración de política de contraseñas en Keycloak (max password age = 30 días) |

**Categorías comunes de referencia:** rendimiento, seguridad, disponibilidad, escalabilidad, usabilidad, mantenibilidad, portabilidad, compatibilidad, cumplimiento normativo (legal/regulatorio), respaldo y recuperación ante desastres.

---

## 5. Restricciones y supuestos

**Restricciones:**
- El backend debe construirse en Java/Kotlin sobre **Quarkus**, con persistencia en **PostgreSQL**.
- El frontend debe construirse en **Kotlin Multiplatform + Compose Multiplatform**, compartiendo lógica entre el dashboard web y la app móvil de técnicos.
- La autenticación debe delegarse a **Keycloak** (OIDC).
- El desarrollo es realizado por un único desarrollador, sin equipo — esto condiciona el ritmo de entrega y obliga a priorizar el MVP de forma estricta.
- No hay presupuesto ni fecha límite definidos aún.

**Supuestos:**
- Los usuarios (administradores, despachadores, técnicos) cuentan con conexión a internet estable en el taller.
- El volumen de datos inicial (flota, órdenes, técnicos, repuestos) es bajo, propio de una etapa temprana del producto.
- La lista definitiva de roles (más allá de la propuesta Administrador/Despachador/Técnico) aún no está confirmada.
- El canal de entrega de notificaciones y el alcance exacto de los informes se definirán en una siguiente iteración, sin bloquear el desarrollo del MVP.
- El software debe contar con un porcentje mínimo de 80% de covertura de pruebas unitarias, y de integración, para garantizar la calidad operativa del sistema.
- ✅ **Confirmado:** el taller atiende tanto empresas (flota) como personas naturales (vehículo particular). El `Cliente` no tiene login ni acceso directo al sistema — interactúa de forma presencial en el establecimiento o por otro medio de comunicación (ej. teléfono), y es el despachador quien registra sus datos y la cotización (equivalente al presupuesto ya modelado en la orden de trabajo, RF-010).
- ✅ **Confirmado:** un cliente de paso (no recurrente) se registra con los **mismos campos obligatorios** que cualquier otro cliente — no hay un flujo simplificado ni un tipo de registro distinto (ver RF-030).
- ✅ **Confirmado:** la asociación vehículo-cliente tiene dos caminos: una pantalla dedicada de gestión de clientes (RF-030) y un atajo inline dentro del formulario de vehículo (RF-018). No es uno u otro, coexisten.

---

## 6. Fuera de alcance

- Flujo detallado de "completar orden" desde la vista móvil (definido como pendiente para una siguiente iteración).
- Pantalla dedicada de consulta de historial de auditoría (por ahora solo se confirma el registro, no la consulta).
- Integración con facturación electrónica (DIAN) — ✅ confirmado que queda fuera del MVP, para una siguiente iteración.

---

## 7. Glosario

| Término | Definición |
|---------|------------|
| OT | Orden de trabajo (work order): solicitud de servicio o mantenimiento sobre un vehículo. |
| Kanban | Tablero visual que organiza las órdenes de trabajo por etapa (Requiere atención, Programado, En taller, Completados). |
| KPI | Indicador clave de desempeño mostrado en el panel principal. |
| Checklist | Lista de tareas asociadas a una orden de trabajo que el técnico debe completar. |
| Repuesto | Pieza o insumo utilizado en la ejecución de una orden de trabajo. |
| Habeas Data | Ley colombiana (1581 de 2012) que regula el tratamiento de datos personales. |

---

## 8. Historial de cambios

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 0.1 | 07/08/2026 | Edwin Suares | Versión inicial, construida a partir del análisis de los wireframes de FloatOps y las decisiones de alcance confirmadas hasta la fecha. |
| 0.2 | 07/08/2026 | Edwin Suares | Sincronizado con `README.md`: se incorpora la fusión con TallerFlow, el modelo de cliente (empresa / persona natural), el actor Cliente (A06), RF-028 y RF-029, y se abre la pregunta de facturación electrónica DIAN en vez de descartarla. |
| 0.3 | 07/08/2026 | Edwin Suares | Se agrega el actor A07 (Facturación) y el requisito de cobertura de pruebas. Se reorganiza la sección 3 (Requerimientos funcionales) en subsecciones por módulo (3.1 a 3.11), en el mismo orden usado en `README.md`, agregando el nuevo módulo "Clientes". Se corrige una fila de tabla mal formada en la sección de Actores. |
| 0.4 | 07/08/2026 | Edwin Suares | Se confirma que el actor Cliente (empresa o persona natural) no requiere login: interactúa presencialmente en el establecimiento o por otro medio de comunicación, y el despachador registra sus datos. |
| 0.5 | 07/08/2026 | Edwin Suares | Se agrega RF-030 (registrar/editar cliente). Se aclara que la asociación vehículo-cliente ocurre inline en el formulario de vehículo (RF-018). Se confirma que un cliente de paso (no recurrente) se registra con los mismos campos obligatorios que cualquier cliente, sin flujo simplificado. |
| 0.6 | 07/08/2026 | Edwin Suares | Se agrega el actor A08 (proveedor de notificaciones). Se agregan RF-031 (actualizar etapa desde el detalle), RF-032 (reasignar técnico en cualquier etapa, restringido por rol), RF-033 (aprobación de cotización por el cliente), RF-034 (filtros avanzados de vehículos), RF-035 (configurar etapas del kanban) y RF-036 (rotación obligatoria de contraseña cada 30 días). Se confirman los canales de notificación (in-app, email, WhatsApp) en RF-026. Se eliminan de "Fuera de alcance" la reasignación de técnico y la definición de canal de notificaciones, por quedar resueltas. |
| 0.7 | 07/08/2026 | Edwin Suares | Auditoría de coherencia entre `README.md` (casos de uso narrativos) y este documento (RF formales): se agrega RF-037 (consultar cola de órdenes abiertas), caso de uso confirmado en el mockup que nunca había recibido un ID de RF. Se corrige el README para eliminar dos líneas de Panel mal clasificadas/duplicadas y consolidar dos líneas de Órdenes de trabajo en RF-014. |
| 0.8 | 07/08/2026 | Edwin Suares | Se completa la auditoría de coherencia en los 8 módulos restantes (Vehículos, Clientes, Técnicos, Repuestos, Informes, Autenticación, Administración, Notificaciones, Auditoría). Se agrega RF-038 (cerrar sesión), caso de uso obvio que nunca tuvo RF asignado. Se agrega criterio de "contador de órdenes abiertas" a RF-017. Se agregan tags de RF a todas las líneas del README para trazabilidad. |
| 0.9 | 07/08/2026 | Edwin Suares | Se agregan al `README.md` los diagramas de estado (Mermaid) de asignación de técnico y aprobación de cotización, ambos del módulo Órdenes de trabajo, junto con el ya existente de la etapa del kanban, bajo una sección 5 renombrada a "Diagramas de estado" (5.1 a 5.3). El módulo Panel no requiere diagrama de estado (sin entidad propia con ciclo de vida). Se retira un diagrama de estado de vehículo agregado por error, fuera del alcance solicitado (Panel + Órdenes de trabajo). |
| 0.10 | 07/08/2026 | Edwin Suares | Se agrega el diagrama de estado del vehículo (sección 5.4). Se crean los diagramas de despliegue: desarrollo local (Docker Compose, según stack ya definido) y producción conceptual (Kubernetes, replicas y hosting — marcado explícitamente como no confirmado, es una propuesta razonable basada en el stack, no una decisión tomada). |
| 0.11 | 07/08/2026 | Edwin Suares | Ronda de preguntas abiertas: se agregan los actores A09 (Supervisor/Gerente) y A10 (Contador/Facturación) — matriz de permisos aún pendiente. Se confirma que auditoría sí requiere pantalla de consulta para administradores. Se redefine RF-015: el técnico marca la orden "lista para revisión" (campo booleano nuevo en WorkOrder, no cambio de etapa, para no chocar con la restricción de rol de RF-031); el despachador completa la orden por separado. Queda abierto el criterio exacto de bloqueo de cotización sin aprobar. |
| 0.12 | 07/08/2026 | Edwin Suares | Continuación de la ronda de preguntas: se confirma que la asociación vehículo-cliente coexiste por dos caminos (pantalla dedicada RF-030 + atajo inline RF-018). Se confirma que DIAN queda fuera del MVP. Se acota RF-023 (Informes) a 2 informes priorizados para MVP: órdenes por técnico/período y tiempo promedio de resolución; el resto de candidatos queda para una siguiente iteración. |
| 0.13 | 07/08/2026 | Edwin Suares | Se agrega la sección 2.1 (Matriz de permisos por rol) para los 5 roles, derivada de las columnas Actor(es) de la tabla de RF para los 3 roles originales. Se agrega RF-039 (Supervisor/Gerente aprueba cotizaciones de alto valor) — posible respuesta al criterio de bloqueo de RF-033, pendiente de confirmar. Se confirma que Contador/Facturación gestionará directamente la futura facturación DIAN. |
| 0.14 | 07/08/2026 | Edwin Suares | Se confirma el criterio de bloqueo de RF-033: cotizaciones de alto valor bloquean el avance a "En taller" sin la aprobación de Supervisor/Gerente (RF-039) — umbral exacto aún pendiente. Se confirman los permisos de Supervisor/Gerente y Contador/Facturación en Panel, Vehículos, Clientes, Técnicos y Repuestos (todos solo lectura, con foco financiero para Contador) y en Administración (Contador gestionará DIAN a futuro). Quedan pendientes solo Informes y Notificaciones para estos 2 roles. |
| 0.15 | 07/08/2026 | Edwin Suares | Se cierra la matriz de permisos por rol (sección 2.1): Supervisor/Gerente y Contador/Facturación confirmados con acceso Total a Informes y como destinatarios de Notificaciones. Se actualizan los actores de RF-023 y RF-026 para listar explícitamente a los 5 roles. Única pregunta abierta restante de esta ronda: el umbral de valor exacto para RF-039. |