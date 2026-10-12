# Plan de Diseño — Folio · RF-01 Captura

## 1. Usuario y contexto

- **Persona:** estudiante con un Android de gama media. El docente pide "escanéelo y envíelo".
- **Contexto:** en el aula, con prisa, con luz irregular y el teléfono en una mano.
- **Tarea principal:** escanear varias hojas seguidas. La captura y el recorte los hace la interfaz de ML Kit; Folio solo abre el escáner y guarda lo capturado.
- **Experiencia previa:** ya usó CamScanner o la cámara. Espera bordes automáticos.

## 2. Modelo conceptual y metáfora

- **Metáfora:** la pila de hojas (plan §10.2). Escanear agrega hojas a la pila; "3 páginas guardadas" confirma que la pila ya existe y está a salvo.
- **Correspondencia:** el sistema copia cada foto al almacenamiento privado antes de confirmar. Para el usuario eso significa "ya está guardado; si se cierra la app no pierdo nada".
- **Carga cognitiva:** una sola acción en cada estado. No hay formularios ni decisiones: el nombre es automático y no se pide nada.

## 3. Arquitectura de información

```
Captura (pantalla de inicio en RF-01)
└── Escáner de ML Kit (pantalla completa, de Google)
```

La pantalla Documentos (RF-10) y Revisión (RF-04) se agregan después. Hasta entonces, Captura es la pantalla de inicio.

## 4. Flujos

- **Camino feliz:**
  1. Listo → tocar **Escanear**.
  2. Se abre el escáner de ML Kit y el usuario captura N hojas.
  3. Al tocar **Guardar** en ML Kit, pasa a **Guardando N páginas**.
  4. Termina en **N páginas guardadas**.
- **Cancelar:** cerrar el escáner vuelve a Listo sin mensaje.
- **Errores**, siempre con causa, solución y una salida:

| Causa | Título | Solución mostrada | Acción |
| --- | --- | --- | --- |
| Escáner no disponible | "Este teléfono no tiene el escáner" | "Actualiza Google Play services y vuelve a intentarlo." | Reintentar |
| Sin espacio | "No hay espacio para guardar" | "Libera espacio en el teléfono y vuelve a escanear." | Reintentar |
| Página ilegible | "No se pudo leer una página" | "Vuelve a escanear las hojas." | Reintentar |
| Desconocido | "No se pudo escanear" | "Vuelve a intentarlo." | Reintentar |

"Reintentar" abre el escáner de nuevo directamente: un toque, no dos.

## 5. Wireframes (spec)

**Estructura común a todos los estados:**
- **Región de contenido:** ocupa el alto disponible y centra en vertical un ícono de 64 dp, un título (`titleLarge`, encabezado para TalkBack) y un mensaje (`bodyLarge`, color secundario).
- **Barra inferior:** `FolioPrimaryButton` a ancho completo, con 16 dp de margen y pegado al borde inferior seguro (zona del pulgar).
- **Desplazamiento:** con la letra al 200 % la región de contenido se desplaza y el botón sigue visible.

| Estado | Ícono | Título | Mensaje | Botón |
| --- | --- | --- | --- | --- |
| Listo | Hoja (acento) | "Escanea tu primera hoja" | "Pon la hoja sobre un fondo oscuro y con buena luz." | Escanear |
| Guardando | Indicador de progreso | "Guardando 3 páginas…" | — | Sin botón, para evitar un segundo escaneo |
| Guardado | Marca de verificación (acento) | "3 páginas guardadas" | "Puedes seguir escaneando." | Escanear otra vez |
| Error | Alerta (color de error) + texto | Según la tabla de §4 | Según la tabla de §4 | Reintentar |

Los cambios de estado se anuncian a TalkBack como región viva (*live region*) educada.

## 6. Justificación HCI

- **Nielsen:**
  - N1: el estado Guardando muestra cuántas páginas.
  - N2: lenguaje del usuario ("hoja", "espacio").
  - N3: cancelar no castiga.
  - N5: no hay botón mientras se guarda.
  - N9: errores con causa y solución.
  - N10: el consejo de luz y fondo aparece en Listo.
- **Gestalt:** el ícono, el título y el mensaje forman un grupo (proximidad). La acción va en su propia región (región común).
- **Fitts:** la única acción está abajo, a ancho completo y con 56 dp de alto.
- **POUR:**
  - Contraste de los tokens ya verificado.
  - El error usa ícono y texto, no solo color.
  - La acción mide 48 dp o más.
  - Encabezado y región viva para TalkBack.
  - Texto al 200 % con desplazamiento.
  - Respeta "Quitar animaciones".
- **ISO 9241-11:** eficiencia de un toque hasta el escáner; efectividad sin pérdida de páginas (RNF-06).

## 7. Handoff

La implementación vive en `feature:capture`:
- `CaptureScreen` sin estado, con capturas en claro, oscuro y 200 %.
- `CaptureRoute`, que conecta el escáner y el `ViewModel`.
- Textos en `values/strings.xml` (es) y `values-en/strings.xml` (en), con plurales.

## 8. Autoevaluación al terminar (Nielsen y POUR)

- **Evidencia:**
  - Capturas `feature/capture/src/test/screenshots/captura_*` en claro, oscuro y 200 %.
  - `CaptureScreenTest`: estados, región viva, acción por estado.
  - `CaptureRouteTest`: escáner no disponible.
- **Cumple:**
  - N1: Guardando N páginas.
  - N2: lenguaje del usuario.
  - N3: cancelar sin castigo.
  - N5: sin botón mientras guarda.
  - N9: causa y solución.
  - N10: consejo de luz y fondo.
  - Fitts: acción abajo, a ancho completo, de 56 dp; resuelve el hallazgo 2 de la auditoría F0.
  - POUR: contraste por tokens, error con ícono y texto, encabezado y región viva, 200 % con desplazamiento.
- **Parcial:**
  - N7, flexibilidad: los atajos al escáner llegan con RF-20.
  - Robusto: el indicador de progreso no tiene descripción propia; lo cubre el título "Guardando N páginas…" como región viva.
- **Pendiente para RF-04:** al guardar, ir directo a Revisión en lugar del estado Guardado, para cumplir el flujo de 3 toques.
