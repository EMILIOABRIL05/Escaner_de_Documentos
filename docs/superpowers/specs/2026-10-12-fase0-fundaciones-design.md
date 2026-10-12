# Fase 0 · Fundaciones — Diseño

Fecha: 2026-10-12 · Fuente de verdad: `docs/plan.md` (§6, §11, §13.1) y `CLAUDE.md`.

## 1. Objetivo y criterio de éxito

Dejar un repositorio que compila una app vacía con la arquitectura del MVP ya cableada, la puerta de calidad funcionando en local y en CI, el sistema de diseño base y las decisiones iniciales registradas en ADR.

La Fase 0 está terminada cuando:

1. `./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify` pasa en local.
2. El workflow de CI ejecuta la misma puerta, más `verifyRoborazziDebug` y `assembleRelease` con R8, y queda en verde.
3. Las pruebas de arquitectura (Konsist) fallan si se viola una regla de dependencia (se demuestra con una prueba que detecta una violación).
4. El tema claro y oscuro y los 3 componentes base tienen capturas Roborazzi en claro, oscuro y fuente al 200 %.
5. Existen ADR-001, ADR-002, `docs/privacidad.md`, `LICENSE` (Apache 2.0), `README.md` y `CHANGELOG.md`.

Fuera de alcance: cualquier RF-xx, pantallas reales, Room, DataStore, Coil, ML Kit y módulos de v1/v2.

## 2. Decisiones tomadas

| Tema | Decisión | Origen |
| --- | --- | --- |
| Motor de escaneo del MVP | ML Kit Document Scanner, con alternativa de cámara del sistema + recorte manual | Usuario, 2026-10-12 (ADR-002) |
| Licencia | Apache 2.0 | Usuario, 2026-10-12 |
| Módulos creados en F0 | Solo los del MVP, más `:pdf` (opción A) | Usuario, 2026-10-12 (ADR-001) |
| Persistencia | Room 2.x (no Room 3, que cambia de paquete) cuando llegue RF-10 | Conservador; se revisa en RF-10 |
| Fuente Inter | Empaquetada en `core:designsystem` (no fuente descargable), por funcionar offline | Plan §10.6, offline-first |

## 3. Módulos

Se crean en F0 (cada uno con su convention plugin y el mínimo para compilar):

```
app/
build-logic/convention/
core/model        (Kotlin JVM)
core/domain       (Kotlin JVM)
core/data  core/database  core/storage  core/designsystem  core/testing   (Android library)
feature/capture  feature/review  feature/library  feature/export  feature/settings   (Android library + Compose)
engine/api  engine/mlkit
pdf/
```

Se crean al empezar su fase: `feature/desktop`, `engine/opencv`, `server`, `sync/drive`, `web/`, `testdata/hojas/`.

Grafo de dependencias de F0 (plan §6.2):

- `core:model` → nada. `core:domain` → `core:model`.
- `core:data` → `core:domain`, `core:database`, `core:storage`.
- `core:database`, `core:storage`, `engine:api` → `core:model`. `engine:mlkit` → `engine:api`.
- `pdf` → `core:domain`. `core:testing` → `core:domain`. `core:designsystem` → nada.
- `feature:*` → `core:domain`, `core:designsystem`; `feature:capture` además → `engine:api`.
- `app` → todos.

## 4. Build

- Gradle Kotlin DSL, `gradle/libs.versions.toml` como única fuente de versiones, `build-logic/convention` como included build.
- Convention plugins:
  - `folio.android.application`: AGP application, compileSdk/targetSdk 37, minSdk 26, Java/JVM 17, R8 en release.
  - `folio.android.library` y `folio.android.library.compose`.
  - `folio.android.feature`: library + compose + hilt + dependencias comunes de feature.
  - `folio.jvm.library`: módulos Kotlin puros; registra la tarea `testDebugUnitTest` como alias de `test`, para que la puerta de calidad también ejecute sus pruebas.
  - `folio.hilt`: Hilt + KSP (nunca kapt).
  - `folio.quality`: ktlint, detekt y Kover en cada módulo.
- Ninguna versión en un `build.gradle.kts`.

### Versiones (verificadas el 2026-10-12, solo estables)

| Pieza | Versión |
| --- | --- |
| Gradle | 9.8.1 (AGP pide como mínimo 9.6.0) |
| AGP | 9.4.1 (la última final en Google Maven); si la nota oficial no la respalda, 9.4.0 |
| Kotlin (compose y serialization plugin) | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| core-ktx / activity-compose / lifecycle | 1.19.1 / 1.13.0 / 2.11.0 |
| Hilt | 2.60.1 |
| kotlinx-coroutines | 1.11.0 |
| detekt + compose rules | 1.23.8 + 0.6.8 |
| ktlint Gradle plugin / ktlint | 14.2.0 / 1.8.0 |
| Kover / Konsist | 0.9.11 / 0.18.1 |
| JUnit 4 / Turbine / Robolectric / Roborazzi | 4.13.2 / 1.2.1 / 4.17 / 1.76.0 |
| Inter | 4.1 (OFL 1.1) |
| JDK | 21 (Temurin) para ejecutar Gradle; bytecode 17 |

Las bibliotecas de fases posteriores (Room, DataStore, Coil, ML Kit, Navigation, serialization) se agregan al catálogo en el requisito que las usa, verificando de nuevo su última versión estable.

### Riesgo de compatibilidad y regla de salida

KSP 2.3.12 no declara soporte de Kotlin 2.4.21, y detekt 1.23.8 se compiló con un Kotlin anterior. F0-01 y F0-02 hacen una compilación de humo. Si alguno falla con Kotlin 2.4.21, **bajo Kotlin a la última 2.3.x estable** que sí soporten, y lo registro en ADR-001. Nunca uso detekt 2.x alpha ni quito detekt de la puerta sin preguntar.

## 5. Puerta de calidad

- **ktlint** con `.editorconfig` (estilo oficial de Kotlin y reglas de Compose).
- **detekt** con `config/detekt/detekt.yml`:
  - `CyclomaticComplexMethod` ≤ 15, `LongMethod` ≤ 60, `LargeClass` ≤ 600, `LongParameterList` ≤ 6.
  - `UnsafeCallOnNullableType` activa (prohíbe `!!`).
  - `ForbiddenClassName` para `*Manager`, `*Helper` y `*Utils`.
  - `ForbiddenComment` para `TODO` sin número de issue.
  - Reglas de Compose activas.
- **Android Lint**: `warningsAsErrors` en falso, errores bloquean; la línea base solo se crea si hay advertencias heredadas (en F0 no debería haberlas).
- **Kover**: verificación de ≥ 80 % de líneas sobre los módulos `core:*`. Si un módulo no tiene código medible, la regla no puede fallar por división entre cero; esto se comprueba en F0-02.
- **Konsist** (pruebas en `app/src/test`, porque `app` es el único módulo que ve todo):
  - `core.model` y `core.domain` no importan `android.*` ni `androidx.*`.
  - Ningún `feature.X` importa de `feature.Y`.
  - `core.domain` no importa `core.data`, `core.database`, `core.storage`, `engine.*` ni `pdf`.
  - Las clases que terminan en `ViewModel` viven en `feature.*`.
  - Se agrega una prueba de contrato que verifica que las reglas detectan una violación sembrada en un fixture de texto.

## 6. CI (GitHub Actions)

`.github/workflows/ci.yml`, que corre en `pull_request` y en `push` a `develop` y `main`, sobre `ubuntu-latest`:

1. `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 21), `gradle/actions/setup-gradle@v6`.
2. `./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify`.
3. `./gradlew verifyRoborazziDebug`.
4. `./gradlew assembleRelease` (R8, sin firmar).
5. Si falla, se suben como artefactos los reportes de detekt, lint, pruebas y el diff de Roborazzi.

La verificación de alineación a 16 KB se agrega en v2, cuando entre OpenCV. En F0 no hay código nativo, y así queda documentado en ADR-001.

## 7. Sistema de diseño (`core:designsystem`)

**Tokens**

- Color en modo claro:
  - fondo `#F5F5F7`, superficie/tarjeta `#FFFFFF`, texto `#111827`, texto secundario `#4B5563`.
  - acento `#2563EB` con texto `#FFFFFF` encima (5,2:1).
- Color en modo oscuro:
  - fondo `#000000`, superficie `#1C1C1E`, texto `#F3F4F6`, texto secundario `#9CA3AF`.
  - acento `#60A5FA` con texto `#0B1220` encima, porque `#2563EB` sobre negro da 4,06:1 y no llega a 4,5:1.
- Todos los pares texto/fondo se fijan con una prueba unitaria que calcula el contraste WCAG (≥ 4,5:1 en texto y ≥ 3:1 en íconos y bordes).
- Tipografía Inter: título grande 34 sp, título 22 sp, cuerpo 17 sp, nota 13 sp, siempre en `sp`.
- Forma: esquinas de 12 dp (pequeña) y 16 dp (grande).
- Espaciado: rejilla de 8 dp (4, 8, 16, 24, 32).
- Movimiento: duraciones de 200 y 300 ms. Un `LocalReducedMotion` lee el ajuste "Quitar animaciones" del sistema y, si está activo, deja la duración en 0.
- `FolioTheme` construye el `MaterialTheme` de Material 3 con estos tokens y sin color dinámico, y cambia entre claro y oscuro según el sistema.

**Componentes base**

1. **`FolioPrimaryButton`**: la acción principal de cada pantalla ("Escanear", "Enviar PDF").
   - Alto 56 dp, ancho completo, acento.
   - Línea secundaria opcional ("3 pág. · ~420 KB").
   - Estados de cargando y deshabilitado, con semántica de botón y `contentDescription` que combina ambas líneas.
2. **`FolioCard`**: tarjeta agrupada estilo iOS para listas y ajustes, con esquina de 16 dp, sombra suave y separadores finos.
3. **`FolioEmptyState`**: ícono, título, texto y una acción; es la base de "Escanea tu primera hoja".

Los componentes reciben sus textos por parámetro. Los textos visibles de la app viven en `strings.xml`, en `values/` (inglés, por defecto) y `values-es/`.

**Pruebas del sistema de diseño**

- Unitarias de contraste.
- Compose UI Test:
  - tamaño táctil ≥ 48 dp.
  - semántica para TalkBack.
  - estado deshabilitado no anunciado como clicable.
- Roborazzi en claro, oscuro y escala de fuente 2.0.

## 8. App vacía

- `FolioApplication` con `@HiltAndroidApp`.
- `MainActivity` con edge-to-edge y `FolioTheme`, que muestra un `FolioEmptyState` de marcador con textos en `strings.xml`.
- Valores seguros del manifiesto desde ahora (plan §12):
  - eliminar `com.google.android.gms.permission.AD_ID` (`tools:node="remove"`).
  - `android:allowBackup="false"` con `dataExtractionRules` que excluyen todo.
  - `networkSecurityConfig` con `cleartextTrafficPermitted="false"`.
  - solo `MainActivity` exportada.

## 9. Git y entregas

- `main` se crea con un único commit vacío de arranque, imprescindible para que exista la rama. `develop` sale de ahí. Ese es el único commit fuera de una rama `feature/`.
- Los documentos de arranque (`CLAUDE.md`, `docs/plan.md` y esta especificación) van en `feature/F0-00-documentacion-inicial`.
- Cada entrega va en su propia rama `feature/F0-0N-…` y se mergea a `develop` con `--no-ff`, tras rebase y puerta en verde. Los commits llevan `Refs: F0-0N`.

| ID | Entrega |
| --- | --- |
| F0-00 | Documentación inicial: `CLAUDE.md`, `docs/plan.md`, esta especificación, `README.md`, `LICENSE`, `.gitignore`, `.gitattributes` |
| F0-01 | Gradle wrapper, catálogo, convention plugins, módulos MVP y app vacía que compila |
| F0-02 | Puerta de calidad local: ktlint, detekt, lint, Kover, Konsist |
| F0-03 | CI en GitHub Actions |
| F0-04 | Tokens, `FolioTheme`, Inter y los 3 componentes base con sus pruebas y capturas |
| F0-05 | ADR-001 Arquitectura, ADR-002 Motor de escaneo, `docs/privacidad.md` y `CHANGELOG.md` |

El push a GitHub se hace solo con aprobación explícita del usuario.

## 10. Descargas necesarias

- Distribución de Gradle 9.8.1 desde `services.gradle.org`, unos 140 MB, una vez, para generar el wrapper.
- Inter 4.1 desde `github.com/rsms/inter/releases`, unos 30 MB en zip; solo se copian al repo los TTF necesarios.
- Dependencias de Maven Central, Google Maven y el portal de plugins de Gradle, que Gradle descarga en cada compilación.
