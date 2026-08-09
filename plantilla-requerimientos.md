# Definición de Requerimientos — FloatOps

> Versión: 0.2
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
| A06 | Cliente | Humano / Organización | Empresa (con flota) o persona natural que solicita cotización y trae su vehículo al taller para reparación o mantenimiento. No hay evidencia de que tenga acceso directo al sistema — se asume que la interacción es presencial/telefónica y el despachador registra sus datos, no un login propio. ❓ A confirmar si en una fase futura se requiere un portal de autoservicio para el cliente. |
| A07 | Facturación | Externo | API de facturación electrónica (DIAN).

**Notas:**
- Administrador y Despachador son actores primarios del panel; Técnico es actor primario de la vista móvil.
- Job programado y Keycloak son actores secundarios (soporte).
- La lista definitiva de roles y su matriz de permisos está pendiente de confirmación (ver sección 5, restricciones y supuestos).

---

## 3. Requerimientos funcionales

| ID | Nombre | Descripción | Actor(es) | Prioridad | Criterios de aceptación |
|----|--------|-------------|-----------|-----------|--------------------------|
| RF-001 | Inicio de sesión con roles | El sistema debe permitir iniciar sesión y restringir el acceso a módulos/acciones según el rol del usuario. | Administrador, Despachador, Técnico | Alta | - Login vía Keycloak/OIDC<br>- El técnico solo ve sus órdenes asignadas en la vista móvil |
| RF-002 | Consultar panel de KPIs | El sistema debe mostrar en el panel principal los indicadores de vehículos pendientes, órdenes abiertas y total de flota. | Despachador, Administrador | Alta | - Muestra conteos actualizados<br>- Incluye tabla de "vehículos que requieren servicio" |
| RF-003 | Navegar a detalle desde el panel | El sistema debe permitir navegar al detalle de un vehículo o de una orden haciendo clic desde el panel. | Despachador, Administrador | Alta | - Clic en fila de vehículo abre su perfil<br>- Clic en fila de orden abre su detalle |
| RF-004 | Tablero kanban de órdenes | El sistema debe mostrar las órdenes de trabajo agrupadas por etapa: Requiere atención, Programado, En taller, Completados. | Despachador, Administrador | Alta | - Cada columna muestra el conteo de órdenes |
| RF-005 | Mover orden entre etapas (drag & drop) | El sistema debe permitir cambiar la etapa de una orden arrastrándola entre columnas del kanban. | Despachador, Administrador | Alta | - El cambio de etapa se persiste al soltar la tarjeta<br>- Se dispara notificación (RF-026) y auditoría (RF-027) |
| RF-006 | Crear orden de trabajo (modal rápido) | El sistema debe permitir crear una orden desde un modal con vehículo, tipo de servicio, descripción, prioridad y fecha programada. | Despachador, Administrador | Alta | - Todos los campos marcados obligatorios se validan antes de guardar |
| RF-007 | Crear orden de trabajo (formulario completo) | El sistema debe ofrecer un formulario completo de creación de orden que incluya selección de repuestos desde el catálogo. | Despachador, Administrador | Alta | - Muestra catálogo de repuestos con precio<br>- Calcula presupuesto en tiempo real |
| RF-008 | Asignar técnico al crear la orden | El sistema debe permitir asignar un técnico a la orden en el momento de su creación. | Despachador, Administrador | Alta | - El campo "técnico asignado" queda disponible en el formulario de creación |
| RF-009 | Adjuntar fotos a la orden | El sistema debe permitir adjuntar una o más fotos del problema al crear o editar una orden. | Despachador, Técnico | Media | - Soporta al menos 3 fotos por orden |
| RF-010 | Calcular presupuesto de la orden | El sistema debe calcular automáticamente el presupuesto (estimado, repuestos seleccionados, restante) al seleccionar repuestos. | Despachador, Administrador | Alta | - El total se recalcula al marcar/desmarcar un repuesto |
| RF-011 | Consultar detalle de orden | El sistema debe mostrar el detalle completo de una orden: descripción, checklist, técnico, vehículo, fechas, historial de servicio. | Despachador, Administrador, Técnico | Alta | - Incluye estado (ej. atrasado) y fechas de creación/vencimiento |
| RF-012 | Actualizar checklist de la orden | El sistema debe permitir marcar como completado cada ítem del checklist de tareas de una orden. | Despachador, Técnico | Alta | - El estado de cada ítem se persiste individualmente |
| RF-013 | Consultar orden asignada (vista móvil) | El sistema debe mostrar al técnico, en la vista móvil, el detalle de la orden que tiene asignada. | Técnico | Alta | - Incluye checklist, campo de foto y notas |
| RF-014 | Agregar foto y notas desde móvil | El sistema debe permitir al técnico adjuntar fotos y escribir notas sobre la orden desde la vista móvil. | Técnico | Media | — |
| RF-015 | Completar orden desde móvil | El sistema debe permitir al técnico marcar la orden como finalizada desde la vista móvil. | Técnico | Baja | ⚠️ Flujo exacto pendiente de definir — no bloquea el MVP |
| RF-016 | Consultar listado de flota | El sistema debe mostrar el listado completo de vehículos de la flota. | Despachador, Administrador | Alta | - Incluye filtros por estado (atrasado, próximo a vencer, programado) |
| RF-017 | Consultar perfil de vehículo | El sistema debe mostrar identificación (placa, VIN, marca/modelo, tipo, kilometraje) e historial de órdenes de un vehículo. | Despachador, Administrador | Alta | — |
| RF-018 | Registrar / editar vehículo | El sistema debe permitir crear y editar el registro de un vehículo. | Despachador, Administrador | Alta | - Campos mínimos: placa, VIN, marca, modelo, tipo, kilometraje |
| RF-019 | Dar de baja un vehículo | El sistema debe permitir cambiar el estado de un vehículo a "dado de baja". | Administrador | Alta | - Un vehículo dado de baja no aparece disponible para nuevas órdenes |
| RF-028 | Asociar vehículo a un cliente | El sistema debe permitir registrar el cliente dueño de un vehículo, ya sea una empresa (con posiblemente varios vehículos) o una persona natural (típicamente un vehículo). | Despachador, Administrador | Alta | - El registro distingue tipo de cliente: empresa o persona natural |
| RF-029 | Registrar cotización de un cliente | El sistema debe permitir registrar la solicitud de cotización de un cliente (empresa o persona natural) al crear la orden de trabajo, reutilizando el presupuesto ya calculado (RF-010). | Despachador, Administrador | Alta | ⚠️ Se asume que la cotización es equivalente al presupuesto de la orden (RF-010); a confirmar si debe existir como una etapa previa independiente a la creación de la orden (ej. "cotización pendiente de aprobación" antes de convertirse en orden) |
| RF-020 | Consultar listado/perfil de técnicos | El sistema debe mostrar, en un modal, el listado de técnicos con su disponibilidad y especialidad. | Despachador, Administrador | Media | — |
| RF-021 | Registrar / editar técnico | El sistema debe permitir crear y editar el registro de un técnico. | Administrador | Media | — |
| RF-022 | Gestionar catálogo de repuestos | El sistema debe permitir dar de alta repuestos y gestionar su stock. | Administrador | Alta | - Incluye código, nombre, precio y cantidad en stock |
| RF-023 | Generar informes | El sistema debe generar informes a partir de los datos almacenados (órdenes, técnicos, repuestos, vehículos). | Despachador, Administrador | Media | ⚠️ Alcance específico de reportes pendiente de definir |
| RF-024 | Gestionar usuarios | El sistema debe permitir crear, editar, asignar rol y desactivar usuarios. | Administrador | Alta | — |
| RF-025 | Gestionar catálogos base | El sistema debe permitir administrar los catálogos de tipos de servicio y categorías de repuestos. | Administrador | Media | — |
| RF-026 | Notificar acciones relevantes | El sistema debe enviar una notificación ante acciones relevantes (orden creada/asignada, cambio de etapa, orden atrasada, stock bajo, vehículo dado de baja). | Todos | Media | ⚠️ Canal de entrega (in-app/email/push) pendiente de definir |
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
- ❓ **Abierto:** el taller atiende tanto empresas (flota) como personas naturales (vehículo particular). Se asume, mientras no se confirme lo contrario, que el `Cliente` interactúa de forma presencial/telefónica (sin login propio) y que la "cotización" es equivalente al presupuesto ya modelado en la orden de trabajo (RF-010). Esto afecta el modelo de datos de `Client` y `Vehicle`, y debería confirmarse antes de avanzar al diagrama de clases.

---

## 6. Fuera de alcance

- Reasignación de técnico **después** de creada la orden (por ahora la asignación solo ocurre al crear la orden).
- Flujo detallado de "completar orden" desde la vista móvil (definido como pendiente para una siguiente iteración).
- Pantalla dedicada de consulta de historial de auditoría (por ahora solo se confirma el registro, no la consulta).
- Integración con facturación electrónica (DIAN) — ❓ estaba fuera de alcance para FloatOps, pero al absorber a TallerFlow (cuyo dominio sí consideraba facturación electrónica vía terceros como Alegra, Siigo o Factus) esto queda abierto: a confirmar si entra al MVP o a una siguiente iteración.
- Definición de canal específico de notificaciones (in-app, email o push) — se define el requerimiento funcional, no el canal.

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