# Fase 0 · Fundaciones — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** repositorio con la app vacía que compila sobre los módulos del MVP, puerta de calidad local y en CI, sistema de diseño base y ADR iniciales.

**Architecture:** build multi-módulo con `build-logic/convention` (included build) y `gradle/libs.versions.toml`. Módulos Kotlin puros para `core:model`/`core:domain`, librerías Android para el resto, y `app` cablea Hilt. Konsist valida las reglas de dependencia por imports.

**Tech Stack:** Gradle 9.8.1, AGP 9.4.x, Kotlin 2.4.21, KSP 2.3.12, Compose BOM 2026.09.00, Hilt 2.60.1, ktlint 1.8.0 (plugin 14.2.0), detekt 1.23.8, Kover 0.9.11, Konsist 0.18.1, Robolectric 4.17, Roborazzi 1.76.0, JUnit 4.13.2, Turbine 1.2.1.

**Spec:** `docs/superpowers/specs/2026-10-12-fase0-fundaciones-design.md`

## Global Constraints

- `minSdk = 26`, `compileSdk = targetSdk = 37`, bytecode JVM 17; Gradle se ejecuta con JDK 21: `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'`.
- SDK en `C:\Users\emilio\AppData\Local\Android\Sdk` (va en `local.properties`, que nunca se commitea).
- Solo versiones estables; todas en `libs.versions.toml`.
- Hilt con KSP, nunca kapt. Prohibidos `!!`, `Manager`/`Helper`/`Utils`, `TODO` sin issue y textos visibles fuera de `strings.xml`.
- Paquete base: `io.github.emilioabril05.folio` (applicationId igual). Módulo `core:model` → `…folio.core.model`, `feature:review` → `…folio.feature.review`, etc.
- Textos: `values/strings.xml` en español (por defecto) y `values-en/strings.xml` en inglés.
- Colores (spec §7):
  - claro: fondo `#F5F5F7`, superficie `#FFFFFF`, texto `#111827`, secundario `#4B5563`, acento `#2563EB` con texto `#FFFFFF` encima.
  - oscuro: fondo `#000000`, superficie `#1C1C1E`, texto `#F3F4F6`, secundario `#9CA3AF`, acento `#60A5FA` con texto `#0B1220` encima.
- Tipografía Inter en 34/22/17/13 sp. Esquinas de 12 y 16 dp. Rejilla de 8 dp. Animaciones de 200 y 300 ms.
- Puerta de calidad: `./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify`.
- Cada tarea:
  - vive en `feature/F0-0N-…`.
  - usa commits en español con Conventional Commits y pie `Refs: F0-0N` + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - se mergea con `git merge --no-ff` a `develop` y luego se borra la rama.
  - al terminar, se informa al usuario según CLAUDE.md §1.5.
- No se hace `push` a GitHub sin aprobación explícita del usuario.

## Review Focus

1. **Finales de línea CRLF en Windows:** ktlint y las capturas fallarían en CI aunque pasen en local. Se espera que `.gitattributes` fuerce LF y que `git ls-files --eol` lo confirme (Task 1).
2. **Módulos JVM que la puerta salta en silencio:** `testDebugUnitTest` no existe en `core:model`/`core:domain`. Se espera que `--dry-run` liste `:core:domain:testDebugUnitTest` (Task 2).
3. **Texto al 200 %:** el botón principal no debe cortar su texto. Se espera que la altura de `FolioPrimaryButton` crezca con la escala, siempre ≥ 56 dp (Task 5).
4. **"Quitar animaciones" activo:** se espera que la duración de movimiento sea 0 (Task 5).
5. **Capturas generadas en Windows y verificadas en Linux:** se espera que `verifyRoborazziDebug` pase en CI. Para eso la fuente va empaquetada y las capturas usan `RoborazziOptions` con tolerancia `changeThreshold = 0.01F`; si CI difiere, las capturas de referencia se regeneran desde CI (Task 4 y Task 5).

---

### Task 1: F0-00 Higiene del repositorio

Rama: `feature/F0-00-documentacion-inicial`, que ya existe con `CLAUDE.md`, `docs/plan.md` y la spec.

**Files:**
- Create: `.gitattributes` (`* text=auto eol=lf`, `*.png binary`, `*.ttf binary`, `*.jar binary`, `*.bat eol=crlf`)
- Create: `.gitignore` (Android y Gradle: `build/`, `.gradle/`, `local.properties`, `.idea/` salvo `codeStyles`, `*.jks`, `*.keystore`, `google-services.json`, `captures/`, `.kotlin/`)
- Create: `LICENSE` con el texto íntegro de Apache 2.0 y `README.md` con propósito, cómo compilar, puerta de calidad y licencia, en español.

- [ ] **Step 1:** crear los archivos y ejecutar `git add --renormalize .`.
- [ ] **Step 2:** verificar con `git ls-files --eol`. Esperado: todos los archivos de texto en `i/lf`.
- [ ] **Step 3:** hacer el commit `chore: agregar gitignore, gitattributes, licencia y readme`. Después hacer merge `--no-ff` a `develop` y borrar la rama.

### Task 2: F0-01 Esqueleto Gradle, módulos MVP y app vacía

Rama: `feature/F0-01-esqueleto-gradle`.

**Files:**
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, generados con `C:\Users\emilio\tools\gradle-9.8.1\bin\gradle wrapper --gradle-version 9.8.1 --distribution-type bin`.
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` (`org.gradle.configuration-cache=true`, `org.gradle.caching=true`, `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx4g`) y `gradle/libs.versions.toml`.
- Create: `build-logic/settings.gradle.kts`, `build-logic/convention/build.gradle.kts` y plugins en `build-logic/convention/src/main/kotlin/`:
  - `AndroidApplicationConventionPlugin.kt`, `AndroidLibraryConventionPlugin.kt`, `AndroidLibraryComposeConventionPlugin.kt`, `AndroidFeatureConventionPlugin.kt`, `JvmLibraryConventionPlugin.kt`, `HiltConventionPlugin.kt`.
  - utilidades internas: `KotlinAndroid.kt` (configura SDK y JVM target) y `ProjectExtensions.kt` (acceso a `libs`).
- Create: un `build.gradle.kts` por módulo de spec §3, con sus dependencias del grafo de spec §3 (`implementation(projects.core.domain)`).
- Create:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/xml/data_extraction_rules.xml` y `backup_rules.xml`, que excluyen todo.
  - `app/src/main/res/xml/network_security_config.xml`
  - `app/src/main/res/values/strings.xml` y `values-en/strings.xml` con `app_name` = "Folio".
  - tema XML mínimo de ventana (`Theme.Folio`, que hereda de `android:Theme.Material.Light.NoActionBar`) con `values-night`.
- Create: `app/src/main/kotlin/io/github/emilioabril05/folio/FolioApplication.kt` (`@HiltAndroidApp`) y `MainActivity.kt` (`@AndroidEntryPoint`, `enableEdgeToEdge()`, `setContent` muestra `Text(stringResource(R.string.app_name))` dentro de `MaterialTheme`; Task 5 lo reemplaza).
- Test: `app/src/test/kotlin/io/github/emilioabril05/folio/AppManifestTest.kt` y `MainActivityTest.kt` (Robolectric, `@Config(sdk = [36])`, o 37 si Robolectric 4.17 lo soporta).

**Interfaces:**
- Produces: plugin ids `folio.android.application`, `folio.android.library`, `folio.android.library.compose`, `folio.android.feature`, `folio.jvm.library`, `folio.hilt`.
- Produces: accesores type-safe `projects.*` (`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`).
- Produces: `folio.jvm.library` registra `tasks.register("testDebugUnitTest") { dependsOn("test") }`.

- [ ] **Step 1:** generar el wrapper, crear `settings`, el catálogo y `build-logic`. Ejecutar `./gradlew help`. Esperado: `BUILD SUCCESSFUL`. Si KSP no acepta Kotlin 2.4.21, aplicar la regla de salida de spec §4 y anotarla para el ADR-001.
- [ ] **Step 2: escribir las pruebas que fallan.**
  - `AppManifestTest`:
    - `` `no declara el permiso de identificador de publicidad`() ``: `requestedPermissions` no contiene `com.google.android.gms.permission.AD_ID`.
    - `` `desactiva la copia de seguridad en la nube`() ``: `applicationInfo.flags and FLAG_ALLOW_BACKUP == 0`.
    - `` `no permite tráfico sin cifrar`() ``: `NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted` es `false`.
  - `MainActivityTest`:
    - `` `muestra el nombre de la app al abrir`() ``: `onNodeWithText("Folio").assertIsDisplayed()`.
- [ ] **Step 3:** ejecutar `./gradlew :app:testDebugUnitTest`. Esperado: FAIL, porque aún no hay manifiesto, actividad ni reglas.
- [ ] **Step 4:** crear los módulos, el manifiesto, los XML y las clases de la lista de archivos.
- [ ] **Step 5:** ejecutar `./gradlew assembleDebug :app:testDebugUnitTest`. Esperado: PASS, 4 pruebas.
- [ ] **Step 6:** ejecutar `./gradlew testDebugUnitTest --dry-run`. Esperado: la salida incluye `:core:model:testDebugUnitTest` y `:core:domain:testDebugUnitTest`.
- [ ] **Step 7:** hacer dos commits atómicos: `build: agregar wrapper, catálogo de versiones y plugins de convención` y `feat(app): crear app vacía con módulos del MVP y manifiesto seguro`. Después merge a `develop`.

### Task 3: F0-02 Puerta de calidad local

Rama: `feature/F0-02-puerta-de-calidad`.

**Files:**
- Create: `build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt` (id `folio.quality`). Se aplica desde los 6 plugins anteriores, y la raíz aplica Kover para la agregación.
- Create: `.editorconfig` (`ktlint_code_style = ktlint_official`, `max_line_length = 120`, `ktlint_function_naming_ignore_when_annotated_with = Composable`).
- Create: `config/detekt/detekt.yml` (`buildUponDefaultConfig = true`) con los umbrales de spec §5, `ForbiddenClassName: ['*Manager', '*Helper', '*Utils']`, `ForbiddenComment` con `allowedPatterns: 'TODO\(#\d+\)'` y el ruleset de compose rules.
- Modify: el `build.gradle.kts` raíz con `kover { reports { verify { rule { filters { includes { projects.add(":core:*") } }; minBound(80) } } } }`, ajustado a la API de Kover 0.9.11.
- Create: `app/src/test/kotlin/io/github/emilioabril05/folio/architecture/ArchitectureRules.kt` y `ArchitectureTest.kt`.
- Create: `app/src/test/resources/konsist-fixtures/feature/review/ViolatesFeatureIsolation.kt.txt`, que contiene `import io.github.emilioabril05.folio.feature.library.Something`.

**Interfaces:**
- Produces, en `ArchitectureRules.kt`:
  - `fun KoScope.importsOf(packagePrefix: String): List<String>`
  - `fun KoScope.featureCrossImports(): List<String>`
  - `fun KoScope.androidImportsInPureModules(): List<String>`
  - `fun KoScope.domainImportsOfImplementations(): List<String>`
  - Cada una devuelve las violaciones legibles (`"archivo -> import"`), vacío si no hay.

- [ ] **Step 1: escribir las pruebas que fallan** en `ArchitectureTest`:
  - `` `ningún feature importa otro feature`() ``: `Konsist.scopeFromProject().featureCrossImports()` es vacío.
  - `` `model y domain no dependen de Android`() ``: igual con `androidImportsInPureModules()`.
  - `` `domain no conoce las implementaciones`() ``: igual con `domainImportsOfImplementations()`.
  - `` `la regla de aislamiento detecta una violación sembrada`() ``: con el scope del fixture, `featureCrossImports()` tiene tamaño 1.
- [ ] **Step 2:** ejecutar `./gradlew :app:testDebugUnitTest`. Esperado: FAIL de compilación, porque las reglas aún no existen.
- [ ] **Step 3:** implementar las reglas, `QualityConventionPlugin`, `.editorconfig`, `detekt.yml` y Kover.
- [ ] **Step 4:** ejecutar la puerta completa. Esperado: `BUILD SUCCESSFUL`.
  - Si detekt 1.23.8 falla con Kotlin 2.4.21, aplicar la regla de salida de spec §4.
  - Si `koverVerify` falla por módulos `core` sin código medible, limitar la regla a los módulos con clases. Es un ajuste de alcance y no baja el umbral; se informa al usuario.
- [ ] **Step 5:** añadir temporalmente `!!` en `MainActivity` y ejecutar `./gradlew detekt`. Esperado: FAIL con `UnsafeCallOnNullableType`. Revertir el cambio.
- [ ] **Step 6:** hacer dos commits: `build: agregar puerta de calidad con ktlint, detekt y kover` y `test(app): validar reglas de dependencia con konsist`. Después merge a `develop`.

### Task 4: F0-03 CI en GitHub Actions

Rama: `feature/F0-03-ci`.

**Files:**
- Create: `.github/workflows/ci.yml`, con lo que pide spec §6:
  - `concurrency` que cancela ejecuciones viejas de la misma rama.
  - `permissions: contents: read`.
  - `upload-artifact` de `**/build/reports/**` y `**/build/outputs/roborazzi/**` cuando falla.

- [ ] **Step 1:** escribir `ci.yml` y añadir `verifyRoborazziDebug`, que se ignora si aún no hay capturas.
- [ ] **Step 2:** validar la sintaxis con `actionlint` si está disponible; si no, revisar a mano contra la referencia de GitHub Actions.
- [ ] **Step 3:** ejecutar localmente `./gradlew assembleRelease`. Esperado: `BUILD SUCCESSFUL` con R8.
- [ ] **Step 4:** hacer el commit `ci: agregar flujo de integración continua con la puerta de calidad` y merge a `develop`.
- [ ] **Step 5:** pedir permiso al usuario para hacer push de `main` y `develop` a GitHub. Con su sí, comprobar que el workflow pasa en verde con `gh run watch`.

### Task 5: F0-04 Sistema de diseño

Rama: `feature/F0-04-sistema-de-diseno`.

**Files:**
- Create en `core/designsystem/src/main/kotlin/io/github/emilioabril05/folio/core/designsystem/`:
  - `theme/Color.kt`: `FolioColors` (claro y oscuro con los valores de Global Constraints).
  - `theme/Type.kt`: `FolioTypography`, con `FontFamily` Inter (Regular 400, Medium 500, SemiBold 600, Bold 700).
  - `theme/Shape.kt`: `FolioShapes` (12 y 16 dp).
  - `theme/Spacing.kt`: `object FolioSpacing { xs = 4.dp; s = 8.dp; m = 16.dp; l = 24.dp; xl = 32.dp }`.
  - `theme/Motion.kt`: `FolioMotion` y `LocalReducedMotion`.
  - `theme/Theme.kt`: `FolioTheme`.
  - `theme/Contrast.kt`: `contrastRatio`.
  - `component/FolioPrimaryButton.kt`, `component/FolioCard.kt`, `component/FolioEmptyState.kt`.
- Create: `core/designsystem/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`, copiados de `C:\Users\emilio\tools\dl-inter\inter\extras\ttf\Inter-{Regular,Medium,SemiBold,Bold}.ttf` (unos 410 KB cada uno), y `OFL.txt` en `core/designsystem/`.
- Create: `core/designsystem/src/main/res/drawable/ic_description.xml` (Material Symbols Rounded "description", Apache 2.0).
- Modify: `app/.../MainActivity.kt` para usar `FolioTheme` + `FolioEmptyState`, y los `strings.xml` de app con `empty_title` "Escanea tu primera hoja" / "Scan your first page", `empty_message` y `action_scan` "Escanear" / "Scan".
- Test en `core/designsystem/src/test/kotlin/.../`: `ContrastTest.kt`, `MotionTest.kt`, `FolioPrimaryButtonTest.kt`, `FolioEmptyStateTest.kt` y `ScreenshotTest.kt` (Roborazzi + Robolectric, `@GraphicsMode(NATIVE)`).

**Interfaces:**
- Produces:
  - `fun contrastRatio(foreground: Color, background: Color): Double`, que sigue la fórmula WCAG 2.2 de luminancia relativa.
  - `@Composable fun FolioTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`
  - `@Composable fun FolioPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, supportingText: String? = null, enabled: Boolean = true, loading: Boolean = false)`
  - `@Composable fun FolioCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun FolioEmptyState(icon: Painter, title: String, message: String, actionText: String, onAction: () -> Unit, modifier: Modifier = Modifier)`
  - `fun FolioMotion.durationMillis(base: Int, reducedMotion: Boolean): Int`
  - `val LocalReducedMotion: ProvidableCompositionLocal<Boolean>`, que `FolioTheme` lee de `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.

- [ ] **Step 1: escribir `ContrastTest`.**
  - `` `blanco sobre negro da 21 a 1`() ``: el resultado es `21.0 ± 0.01`.
  - `` `texto sobre fondo cumple 4,5 a 1 en claro y oscuro`() ``: verifica texto/fondo, texto/superficie, secundario/fondo, secundario/superficie y sobre-acento/acento.
  - `` `acento sobre superficie cumple 3 a 1 para íconos`() ``.
- [ ] **Step 2:** ejecutar `./gradlew :core:designsystem:testDebugUnitTest`. Esperado: FAIL, porque `contrastRatio` aún no existe.
- [ ] **Step 3:** implementar `Contrast.kt` y `Color.kt`. Ejecutar de nuevo. Esperado: PASS.
- [ ] **Step 4: escribir `MotionTest`.**
  - `` `con quitar animaciones la duración es cero`() ``: `durationMillis(300, reducedMotion = true) == 0`.
  - Y con `false` el resultado es 300.
  - Verificar que falla, implementar `Motion.kt` y verificar que pasa.
- [ ] **Step 5: escribir las pruebas de componentes** (Robolectric + Compose UI Test).
  - `` `el botón principal mide al menos 56 dp de alto`() ``
  - `` `el botón anuncia texto y peso juntos a TalkBack`() ``: el `contentDescription` combinado contiene "Enviar PDF" y "3 pág.".
  - `` `el botón deshabilitado no es clicable`() ``: `assertIsNotEnabled()`.
  - `` `el botón cargando no dispara la acción`() ``
  - `` `con fuente al 200 % el botón crece y no corta el texto`() ``: con `Density(fontScale = 2f)`, la altura es > 56 dp y el texto aparece completo.
  - `` `el estado vacío ofrece su acción con objetivo táctil de 48 dp`() ``
  - Verificar que fallan, implementar `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt` y los componentes, y verificar que pasan.
- [ ] **Step 6: `ScreenshotTest`.**
  - Una captura por componente en claro, oscuro y `fontScale = 2f` (9 en total), con `changeThreshold = 0.01F`.
  - Grabar con `./gradlew recordRoborazziDebug` y revisar las imágenes a mano.
  - Ejecutar `verifyRoborazziDebug`. Esperado: PASS.
- [ ] **Step 7:** actualizar `MainActivity` y los `strings.xml`. Actualizar `MainActivityTest`: `` `muestra el estado vacío con la acción Escanear`() ``.
- [ ] **Step 8:** ejecutar la puerta completa. Esperado: verde, con Kover ≥ 80 % en `core:designsystem`.
- [ ] **Step 9:** autoevaluar la pantalla con las 10 heurísticas de Nielsen y POUR usando la skill `hci-usability-audit`, y ponerlo en el informe.
- [ ] **Step 10:** hacer commits atómicos: `feat(designsystem): agregar tokens de color con contraste verificado`, `feat(designsystem): agregar tipografía inter, formas y movimiento`, `feat(designsystem): agregar botón principal, tarjeta y estado vacío`, `test(designsystem): agregar capturas en claro, oscuro y 200 %` y `feat(app): mostrar estado vacío con el tema de folio`. Después merge a `develop`.

### Task 6: F0-05 Documentación de decisiones

Rama: `feature/F0-05-adr-y-privacidad`.

**Files:**
- Create: `docs/adr/001-arquitectura.md`: capas, módulos de spec §3, opción A, versiones y salida aplicada si hubo, paquete base, Konsist en `app`, 16 KB diferido a v2, y `ACCESS_LOCAL_NETWORK` pendiente para el Modo Escritorio porque `targetSdk = 37`.
- Create: `docs/adr/002-motor-de-escaneo.md`: ML Kit, alternativa de cámara del sistema + `Matrix.setPolyToPoly`, y contrato `DocumentScanner` del plan §6.3.
- Create: `docs/privacidad.md`, borrador de una página basado en plan §12.
- Create: `CHANGELOG.md` en formato Keep a Changelog, sección `[Sin publicar]` en lenguaje de usuario.
- [ ] **Step 1:** escribir los cuatro archivos, cada uno con contexto, decisión y consecuencias en los ADR.
- [ ] **Step 2:** ejecutar `git ls-files --eol docs`. Esperado: LF.
- [ ] **Step 3:** hacer el commit `docs: agregar adr de arquitectura y motor, privacidad y changelog`. Después merge a `develop`.
- [ ] **Step 4:** cierre de fase: ejecutar la puerta completa sobre `develop`, informar y **detenerse** hasta que el usuario confirme la Fase 1.
