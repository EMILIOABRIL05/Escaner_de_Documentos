# ADR-004 · Recorte no destructivo y navegación entre pantallas

- Estado: aceptado
- Fecha: 2026-10-12
- Requisito: RF-02
- Referencias: ADR-003, `docs/plan.md` §2.3 (regla 1), §6.2, §7 y §10.5

## Contexto

RF-02 agrega la primera pantalla además de Captura (el editor de recorte), la primera migración de Room y la primera transformación de imagen.

## Decisiones

1. **Recorte como parámetro.**
   - Cada página guarda 4 esquinas normalizadas (`CropQuad`, de 0 a 1) en 8 columnas de `pages`, con la base de datos en versión 2 y la migración `1 → 2`.
   - La foto original nunca se modifica.
   - `CropQuad.validate()`, en el dominio, rechaza recortes fuera de la foto, cruzados o cóncavos, y los menores al 1 % del área.
2. **Corrección de perspectiva sin OpenCV.**
   - `BitmapPageRenderer` (`core:storage`) usa `Matrix.setPolyToPoly` para llevar las 4 esquinas a un rectángulo.
   - Decodifica con `inSampleSize` al tamaño pedido (RNF-04) y devuelve JPEG a través de la interfaz de dominio `PageRenderer`. Así los features no dependen de `core:storage`.
3. **Editor en `feature:review`**, donde el plan ubica la edición de páginas:
   - Deshacer y rehacer. Un arrastre completo cuenta como un solo cambio (`CropHistory`).
   - Lupa propia, porque la de Compose solo existe desde Android 9 y el mínimo es Android 8.
   - Acciones de TalkBack para mover cada esquina sin arrastrar (WCAG 2.5.7).
4. **Navegación Compose con rutas tipadas.**
   - Cada feature declara su destino `@Serializable` (`CaptureDestination`, `CropEditorDestination`).
   - Solo `app` los une en `FolioNavHost`, porque un feature no conoce a otro.
   - El `ViewModel` del editor lee su ruta con `SavedStateHandle.toRoute()`.
5. **Alternativa sin ML Kit.**
   - Si el escáner no existe, "Usar la cámara" abre la cámara del sistema (`TakePicture`), sin permiso de cámara.
   - La foto se escribe en una URI de `FileProvider` que solo expone `cache/captures/`.
   - La página se guarda como cualquier otra y se abre el editor de recorte.

## Consecuencias

- **Pegarse al borde:** el plan §10.5 pide que la esquina se pegue al borde más cercano. Necesita detección de bordes y se pospone al motor OpenCV (Fase 3).
- **Lo que no se prueba en local:**
  - La cámara del sistema y la lupa a resolución real se verifican en un dispositivo.
  - Robolectric en Windows no resuelve rutas de `FileProvider`. Esa prueba se omite en Windows y corre en la CI de Linux.
- **Vista previa:** la hoja corregida se ve como miniatura en el editor. El PDF (RF-06) usará el mismo `PageRenderer` a resolución completa.
