# ADR-002 · Motor de escaneo inicial

- Estado: aceptado
- Fecha: 2026-10-12
- Referencias: `docs/plan.md` §5, §6.3, §7.1 y §14

## Contexto

El MVP necesita:

- capturar varias hojas;
- detectar bordes;
- corregir la perspectiva;
- aplicar filtros.

Hay dos caminos (plan §14.3):

- **ML Kit Document Scanner:** gratuito y en el dispositivo, con detección, recorte, filtros y limpieza de manchas. Usa la cámara de Play services, así que la app no pide permiso de cámara. A cambio, su interfaz es de Google, requiere Play services y 1,7 GB de RAM.
- **Cámara propia con CameraX + OpenCV:** control total del diseño, pero añade de 3 a 4 semanas y código nativo que debe alinearse a páginas de 16 KB.

## Decisión

El MVP usa **ML Kit Document Scanner** (`play-services-mlkit-document-scanner`) en el módulo `engine:mlkit`. Decidido por el usuario el 2026-10-12.

El escáner queda detrás de un contrato intercambiable en `engine:api`, el patrón Strategy del plan §6.3:

```kotlin
interface DocumentScanner {
    fun contract(options: ScanOptions): ActivityResultContract<Unit, ScanOutcome>
}

sealed interface ScanOutcome {
    data class Success(val pages: List<Uri>) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Failed(val reason: ScanError) : ScanOutcome
}
```

**Alternativa sin ML Kit** (plan §14.1): en equipos sin Play services o con menos RAM, se toma la foto con la cámara del sistema (`ACTION_IMAGE_CAPTURE`, sin permiso) y se recorta a mano con 4 esquinas usando `Matrix.setPolyToPoly`. No hace falta OpenCV.

## Consecuencias

- **Lo que se gana:**
  - Se ahorran semanas en el MVP.
  - La app no pide permiso de cámara.
  - El procesamiento ocurre en el teléfono, sin conexión.
- **Lo que se pierde:**
  - La interfaz de captura es de Google y casi no se puede personalizar.
  - La primera vez puede esperar la descarga del modelo, así que hay que mostrar progreso y nunca una pantalla congelada.
- **Ya aplicado en F0:** ML Kit podría añadir el permiso `AD_ID`. El manifiesto lo elimina explícitamente y `AppManifestTest` lo vigila.
- **En v2:** el motor CameraX + OpenCV entra como segunda implementación de `DocumentScanner` (`engine:opencv`), en la variante `libre`, sin tocar las pantallas.
- Implementar el contrato es parte de RF-01; en F0 `engine:api` y `engine:mlkit` están vacíos.
