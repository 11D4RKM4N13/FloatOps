# Cómo Construir Casos de Uso

* **Actor**: quién o qué interactua con el sistema (persona o sistema externo). En FloatOps: Despachador, Administrador, Ténico, etc.

* **Caso de uso**: una acción completa que el actor logra con el sistema (verbo + objeto). Ej: "Mover orden entre etapas".

* **Límites del sistema** (system boundary): el rectángulo que encierra todos los casos de uso - representa FloatOps.

* **Relaciones**: línea simple (asociación actor <---> caso de uso), <<include>> (un caso de uso siempre utiliza otro, ej: "Crear orden" incluye "Calcular presupuesto"), <<extend>> (un caso de uso opcionalmente extiendo otro, ej: "Adjuntar foto" extiende "Actualizar checklist"), y generalización (un actor especializa a otro, ej: **Administrador** hereda de **Usuario del panel**)

# Pasos

## Identifica los actores
Lista quién interactúa con el sistema, sin repetir roles. Para FloatOps ya están definidos en el documento [plantilla-requerimientos.md](./plantilla-requerimientos.md).

## Agrupa los casos de uso por módulo
No intentes un solo diagrama gigante con todos los casos de uso. Haz un diagrama por módulo y opcoinalmente uno general de alto nivel que solo muestre los módulos como cajas.

## Dibuja el límite del sisitema
Un rectángulo grande con el nombre del módulo o del sistema arriba. Todos los óvalos de casos de uso van dentro; los actores van fuera, a los lados.

## Conecta actor con casos de uso
Una línea simple del actor al óvalo si ese actor puede iniciar esa acción. Si dos actores comparten un caso de uso (ej. Despachador y Técnico pueden 'Actualizar checklist'), ambos se conectan al mismo óvalo.

## Aplica include/extend solo si aporta calidad
Úsalos con moderación. Ejemplo real de FloatOps: 'Crear orden de trabajo' <<include>> 'Calcular presupuesto' (siempre ocurre), y 'Actualizar Checklist' <<extend>> 'Adjuntar foto'(ocurre solo a veces)

## Revisa que cada caso de uso sea una meta completa
Evita casos de uso demasiado técnicos ('Hacer clic en botón guardar') o demasiado amplios ('Gestionar órdenes'). El punto medio correcto es el nivel que ya usamos en el [README](./README.md) 'Crear una orden de trabajo (formulario completo)'.