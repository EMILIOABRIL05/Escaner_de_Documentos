# Auditoría de Usabilidad (HCI) — Pantalla vacía de la Fase 0

Fecha: 2026-10-12 · Entrega F0-04

## 1. Alcance y contexto

- **Qué se auditó:**
  - `MainActivity` con `FolioEmptyState`.
  - Los componentes base `FolioPrimaryButton` y `FolioCard`.
  - El tema `FolioTheme`.
  - Evidencia: código, pruebas automáticas y 9 capturas Roborazzi.
- **Usuario:** estudiante con un Android de gama media, con prisa y el teléfono en una mano (plan §10.1).
- **Tarea:** abrir la app y empezar a escanear.
- **Supuesto:** es una pantalla de marcador. El escáner llega en RF-01, así que la acción todavía no hace nada.

## 2. Resumen ejecutivo

- **Hallazgos:** 0 críticos, 2 altos, 2 medios y 1 bajo.
- **Veredicto:** el sistema de diseño cumple WCAG 2.2 AA en lo que se puede verificar automáticamente:
  - contraste;
  - objetivos táctiles de 48 y 56 dp;
  - TalkBack;
  - letra al 200 %;
  - "Quitar animaciones".

  Lo que falta es propio de una pantalla de marcador.
- **Acciones más urgentes:**
  1. Conectar "Escanear" al escáner (RF-01).
  2. Llevar la acción principal a la zona del pulgar en la pantalla Documentos (RF-10).
  3. Definir el ícono de la app.

## 3. Tabla de cumplimiento

| Criterio | ¿Cumple? | Evidencia | Observación |
| --- | --- | --- | --- |
| N1 · Visibilidad del estado | Parcial | El botón admite una segunda línea de estado ("3 pág. · ~420 KB") y un estado de carga | La pantalla vacía no tiene estado que mostrar; "Escanear" aún no responde |
| N2 · Correspondencia con el mundo real | Sí | "Escanea tu primera hoja", con un ícono de hoja | Lenguaje del usuario, sin tecnicismos |
| N3 · Control y libertad | Sí | Sin flujos; usa el gesto atrás del sistema y atrás predictivo (`enableOnBackInvokedCallback`) | — |
| N4 · Consistencia y estándares | Parcial | Material 3 con tema propio y comportamiento Android | La app no tiene ícono propio (lint `MissingApplicationIcon`) |
| N5 · Prevención de errores | Parcial | No hay entradas posibles | Un botón que no hace nada puede hacer pensar que la app falla |
| N6 · Reconocer antes que recordar | Sí | Acción visible y rotulada | — |
| N7 · Flexibilidad y eficiencia | N/A | Los atajos llegan en v1 (RF-20) | — |
| N8 · Estético y minimalista | Sí | Una sola acción principal, mucho espacio en blanco, un solo acento | Ver capturas `estado_vacio_*.png` |
| N9 · Recuperación de errores | N/A | Todavía no hay errores posibles | — |
| N10 · Ayuda | N/A | Los consejos de primer uso se planifican con RF-01 | — |
| POUR · Perceptible | Sí | `ContrastTest`: todos los pares ≥ 4,5:1 y el acento ≥ 3:1; ícono decorativo con `contentDescription = null` | El modo oscuro aclara el acento a `#60A5FA` (con `#2563EB` quedaba en 4,06:1) |
| POUR · Operable | Parcial | Botón ≥ 56 dp y acción ≥ 48 dp (pruebas) | El indicador de foco con teclado es el de Material 3 por defecto; no se probó con teclado físico |
| POUR · Comprensible | Sí | Textos en español e inglés (`MainActivityTest`), predecible | — |
| POUR · Robusto | Parcial | El título se marca como `heading()`; el botón anuncia texto y peso juntos | El estado "cargando" no se anuncia a TalkBack |
| Gestalt · Proximidad / similitud / región común | Sí | Ícono, título y mensaje agrupados; la acción se separa 16 dp | `FolioCard` aún no tiene separadores finos entre filas |
| Fitts · Objetivos clave | Parcial | Acción a ancho completo y 56 dp | Está centrada en vertical, no abajo en la zona del pulgar (plan §10.5) |
| Modelo conceptual ↔ mental | Sí | La metáfora de "hoja" se mantiene en el texto y el ícono | — |

## 4. Matriz de hallazgos priorizada

| # | Hallazgo | Criterio | S | F | P | Recomendación |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | "Escanear" no hace nada | N1, N5 | 3 | 4 | 12 | Conectarlo al escáner en RF-01; no publicar ninguna versión antes |
| 2 | La acción principal no está en la zona del pulgar | Fitts | 2 | 5 | 10 | En la pantalla Documentos (RF-10), botón fijo abajo al centro con 16 dp de margen |
| 3 | Sin ícono de app | N4 | 2 | 3 | 6 | Decidir nombre e ícono (plan §14.3) antes de la prueba cerrada |
| 4 | El estado "cargando" no se anuncia a TalkBack | POUR Robusto | 2 | 2 | 4 | Añadir `stateDescription` ("Generando PDF…") cuando `loading` sea verdadero, con su prueba |
| 5 | `FolioCard` sin separadores entre filas | Gestalt | 1 | 2 | 2 | Añadirlos cuando se use en Ajustes (agrupación estilo iOS) |

## 5. Historias de Usuario

- **HU-1 (hallazgo 1).** Como estudiante, quiero que "Escanear" abra el escáner en un toque para empezar sin pasos intermedios.
  - Criterio de aceptación: el toque abre ML Kit en menos de 1,5 s.
  - Métrica ISO 9241-11: eficiencia. Del ícono a la hoja de compartir en 3 toques más las capturas.
- **HU-2 (hallazgo 2).** Como usuario que sostiene el teléfono con una mano, quiero la acción principal abajo para alcanzarla con el pulgar.
  - Criterio de aceptación: el botón queda a ≤ 16 dp del borde inferior seguro, en cualquier tamaño de pantalla.
  - Métrica: eficiencia, medida como tiempo al primer toque en la prueba con 5 usuarios.
- **HU-4 (hallazgo 4).** Como usuario de TalkBack, quiero oír que el PDF se está generando para no tocar dos veces.
  - Criterio de aceptación: TalkBack lee "Enviar PDF, generando" mientras `loading` es verdadero.
  - Métrica: efectividad, medida como tasa de tareas completadas con TalkBack.

## 6. Revisión de código

| Práctica | ¿Cumple? | Evidencia |
| --- | --- | --- |
| Textos visibles en recursos (es/en) | Sí | `values/strings.xml`, `values-en/strings.xml` |
| Tamaños en `sp` / `dp` | Sí | `Type.kt` y `FolioPrimaryButton.kt` |
| Semántica para lector de pantalla | Sí | `contentDescription` combinado y `heading()` |
| El color no es el único indicador | Sí | Los estados usan texto o indicador de progreso |
| Respeta "Quitar animaciones" | Sí | `LocalReducedMotion` (`FolioThemeTest`) |
| Modo claro y oscuro | Sí | `FolioThemeTest` y capturas en oscuro |
