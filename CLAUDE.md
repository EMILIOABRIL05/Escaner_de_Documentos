# CLAUDE.md — Folio

Folio es un escáner de documentos para Android: fotos de hojas → PDF liviano → compartir.
Sin anuncios, sin cuentas, sin rastreadores, offline-first.
El plan completo (requisitos, arquitectura, UX, roadmap) está en `docs/plan.md`. Léelo antes de empezar cualquier tarea y trátalo como la fuente de verdad.

Estas reglas son obligatorias. Si una tarea choca con alguna, detente y pregunta antes de seguir.

---

## 1. Forma de trabajo: paso a paso

1. **Una fase a la vez** (Fase 0 → 1 → 2 → 3, ver `docs/plan.md` §13). Nunca adelantes trabajo de una fase posterior.
2. **Un requisito a la vez** (RF-xx / RNF-xx). Cada requisito vive en su propia rama `feature/`.
3. **Antes de escribir código**, presenta un plan corto: archivos que vas a tocar, pruebas que vas a escribir, riesgos. Espera mi aprobación si el cambio toca más de un módulo o cambia una decisión de arquitectura.
4. **Ciclo por requisito:** prueba que falla → código mínimo que la pasa → refactor → puerta de calidad local → commit.
5. **Al terminar cada requisito**, informa: qué hiciste, qué pruebas lo cubren, resultado de la puerta de calidad y qué sigue. Luego espera.
6. **Al cerrar una fase**, detente. No empieces la siguiente sin mi confirmación.
7. Si algo es ambiguo, pregunta una sola cosa concreta. No inventes requisitos.
8. Nunca digas que algo "funciona" o "está listo" sin haber ejecutado las pruebas y mostrado el resultado.

---

## 2. Stack (no negociable)

Android nativo en Kotlin. **Prohibido:** Flutter, React Native, Java para código nuevo, XML de vistas para UI nueva, iText (AGPL), cualquier SDK de anuncios, analítica o rastreo (Firebase Analytics, Crashlytics, etc.).

| Capa | Herramienta | Fase |
| --- | --- | --- |
| Lenguaje | Kotlin + Coroutines + Flow | MVP |
| UI | Jetpack Compose + Material 3 con tema propio | MVP |
| Navegación | Navigation Compose con rutas tipadas (`@Serializable`) | MVP |
| DI | Hilt (KSP, nunca kapt) | MVP |
| Persistencia | Room (KSP) + DataStore Preferences | MVP |
| Imágenes | Coil | MVP |
| Escáner motor 1 | ML Kit Document Scanner (`play-services-mlkit-document-scanner`) | MVP |
| PDF básico | `android.graphics.pdf.PdfDocument` | MVP |
| OCR | ML Kit Text Recognition v2 | v1 |
| PDF avanzado | PdfBox-Android (`com.tom-roush:pdfbox-android`) | v1 |
| Segundo plano | WorkManager | v1 |
| Google Drive | Drive API v3 + `AuthorizationClient` (alcance `drive.file`) | v1 |
| Servidor local | Ktor Server, motor CIO | v1 |
| QR | ZXing core | v1 |
| Escáner motor 2 | CameraX + OpenCV | v2 |
| Build | Gradle Kotlin DSL, `gradle/libs.versions.toml`, convention plugins en `build-logic/` | F0 |

### Versiones: solo estables

- Usa **la última versión estable** de cada dependencia, verificada en su fuente oficial en el momento de agregarla:
  - AndroidX y Compose: https://developer.android.com/jetpack/androidx/versions
  - Compose BOM: https://developer.android.com/develop/ui/compose/bom
  - Kotlin: https://kotlinlang.org/docs/releases.html
  - AGP: https://developer.android.com/build/releases/gradle-plugin
  - Resto: Maven Central o la página de releases del proyecto.
- **Prohibido:** `alpha`, `beta`, `rc`, `dev`, `eap`, `SNAPSHOT`, `+` o rangos dinámicos.
- Todas las versiones van en `libs.versions.toml`; nunca en un `build.gradle.kts`.
- Mantén alineados Kotlin, KSP, AGP y Compose Compiler según la tabla de compatibilidad oficial.
- Si la única versión disponible de algo no es estable, detente y pregúntame.
- `minSdk = 26`. `targetSdk` y `compileSdk` = el último nivel estable que soporte el AGP estable.
- Código nativo (OpenCV en v2): debe estar alineado a páginas de 16 KB. Verifícalo antes de mergear.
- Si en algún momento apuntas a API 37+, el Modo Escritorio necesita `ACCESS_LOCAL_NETWORK` pedido en tiempo de ejecución; antes de API 37 **no** lo declares.

---

## 3. Arquitectura

Sigue la guía oficial de arquitectura de Android: capas UI / dominio / datos, flujo de datos unidireccional.

```
app/                        # punto de entrada, navegación, cableado Hilt
build-logic/convention/     # plugins Gradle compartidos
core/model                  # entidades puras (Kotlin, sin Android)
core/domain                 # casos de uso + interfaces (Kotlin puro)
core/data                   # repositorios (implementan interfaces del dominio)
core/database               # Room
core/storage                # archivos, FileProvider, SAF
core/designsystem           # tema, tokens, componentes
core/testing                # fakes y reglas compartidas
feature/{capture,review,library,export,desktop,settings}
engine/{api,mlkit,opencv}
pdf/  server/  sync/drive/  web/
docs/{plan.md,adr/}
testdata/hojas/
```

Reglas de dependencia (se validan con Konsist en CI):
- `core:model` y `core:domain` no dependen de Android ni de ningún otro módulo salvo `core:model`.
- Un `feature` **nunca** depende de otro `feature`.
- Las implementaciones (`core:data`, `engine:*`, `pdf`, `server`, `sync:*`) dependen del dominio, nunca al revés.
- Solo `app` conoce todos los módulos.

Patrones esperados:
- **MVVM + UDF:** un `ViewModel` por pantalla que expone un único `StateFlow<XxxUiState>` y recibe eventos (`onEvent(XxxEvent)`). Efectos de una sola vez por `Channel`/`SharedFlow`.
- **Repository** para acceso a datos; **Use case** (`operator fun invoke`) para cada regla de negocio.
- **Strategy** para piezas intercambiables: `DocumentScanner`, `PdfWriter`, `OcrEngine`.
- **Inversión de dependencias:** el dominio define la interfaz, los datos la implementan, Hilt las une.
- Edición **no destructiva**: la foto original nunca se modifica; recorte, rotación y filtro se guardan como parámetros.
- Cada decisión de arquitectura nueva lleva un ADR en `docs/adr/NNN-titulo.md` (contexto, decisión, consecuencias).

---

## 4. Código limpio

- Convenciones oficiales de Kotlin. ktlint decide el formato.
- Nombres del dominio (`Document`, `Page`, `ExportProfile`). Prohibido: `Manager`, `Helper`, `Utils`, `Data2`, abreviaturas crípticas.
- Funciones pequeñas con un solo propósito. Límites de detekt: complejidad ciclomática ≤ 15, función ≤ 60 líneas, clase ≤ 600 líneas, ≤ 6 parámetros.
- Inmutabilidad por defecto: `val`, `data class`, colecciones inmutables en el estado de UI.
- Prohibido `!!`. Modela los casos posibles con `sealed interface`.
- Errores como valores (`Result` o tipos de error propios). Las excepciones no cruzan del dominio a la UI.
- Corrutinas estructuradas: dispatchers inyectados, nada de `GlobalScope`, respeta la cancelación. Trabajo pesado en `Dispatchers.Default`/`IO`, nunca en el hilo principal.
- Sin números ni textos mágicos: constantes con nombre; todo texto visible en `strings.xml` (español e inglés).
- KDoc en toda API pública de `core:domain` y `engine:api`.
- Nada de código comentado ni código muerto. Nada de `TODO` sin un issue asociado.
- Nunca registres en logs contenido de documentos, rutas de archivos del usuario ni tokens.

---

## 5. HCI y accesibilidad (obligatorio en cada pantalla)

Usuario principal: estudiante con Android de gama media que debe escanear y mandar una prueba por WhatsApp, con prisa. Ver `docs/plan.md` §10.

- **Flujo principal:** del ícono a la hoja de compartir en 3 toques más las capturas. Ninguna pantalla intermedia innecesaria.
- **Una acción principal por pantalla**, grande y en la zona del pulgar (ley de Fitts). Acciones destructivas lejos de las frecuentes.
- **Nielsen:** estado visible (contador de páginas, peso estimado, progreso); deshacer en edición y borrado; lenguaje del usuario ("Ligero", no "150 ppp"); mensajes de error con causa y solución.
- **Convenciones de Android:** gesto atrás del sistema, atrás predictivo, hoja de compartir nativa. Estética tipo Apple (tipografía Inter, espacio en blanco, esquinas 12–16 dp, un solo color de acento), comportamiento Android.
- **WCAG 2.2 AA:** contraste ≥ 4,5:1 en texto y ≥ 3:1 en íconos; objetivos táctiles ≥ 48 dp; `contentDescription` útil en todo elemento interactivo o informativo; acciones de accesibilidad para lo que se hace arrastrando; texto al 200 % sin cortes; el color nunca es el único indicador.
- Respeta "Quitar animaciones" y el tamaño de letra del sistema (usa `sp`).
- Modo claro y oscuro desde el primer día.
- Al terminar cada pantalla, autoevalúala contra las 10 heurísticas de Nielsen y POUR, e incluye el resultado en tu informe.

---

## 6. Pruebas

Ninguna funcionalidad se da por terminada sin pruebas automáticas que la cubran.

| Nivel | Herramientas | Qué cubre |
| --- | --- | --- |
| Unitarias (~70 %) | JUnit, Turbine, kotlinx-coroutines-test, fakes propios de `core:testing` | Casos de uso, ViewModels, reglas de negocio |
| Integración (~20 %) | Room en memoria, Robolectric | Repositorios, migraciones, generación de PDF |
| UI (~10 %) | Compose UI Test + checks de accesibilidad, Roborazzi | Flujo principal, estados vacío, carga y error |
| Rendimiento | Macrobenchmark, Baseline Profiles | Arranque < 1,5 s, PDF de 10 págs < 3 s |

- **TDD** en `core:domain` y `core:data`: la prueba se escribe primero.
- Prefiere **fakes** a mocks. Si usas mocks, que sea en el borde (APIs externas).
- Nombres descriptivos: `` `exporta PDF con perfil Ligero bajo 150 KB por página`() ``.
- Cobertura con Kover ≥ 80 % en módulos `core`.
- Cada bug corregido lleva primero una prueba que lo reproduce.
- Capturas de Roborazzi en claro, oscuro y fuente al 200 % para cada pantalla.

---

## 7. Puerta de calidad local (antes de cada commit)

```bash
./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify
```

Si algo falla, se arregla antes de commitear. Nunca desactives una regla, subas un umbral ni agregues `@Suppress` para pasar la puerta sin preguntarme.

---

## 8. Git: GitFlow

Ramas:
- `main`: solo versiones publicadas. Cada merge lleva tag `vX.Y.Z`.
- `develop`: integración. Siempre compila y pasa las pruebas.
- `feature/RF-xx-descripcion-corta`: sale de `develop`, vuelve a `develop`.
- `release/X.Y.0`: sale de `develop` al cerrar una fase; solo correcciones, versión y CHANGELOG; se mergea a `main` y a `develop`.
- `hotfix/X.Y.Z`: sale de `main`; se mergea a `main` y a `develop`.

Reglas:
- Nunca commits directos a `main` ni a `develop`.
- Merges con `--no-ff` para conservar la historia de cada rama.
- Antes de mergear: rebase de la `feature` sobre `develop` actualizado y puerta de calidad en verde.
- Nunca `push --force` a ramas compartidas. Nunca reescribas historia publicada.
- Borra la rama `feature` después de mergearla.
- Versionado semántico: Fase 1 → `0.1.0`, Fase 2 → `0.2.0`, Fase 3 → `1.0.0`.

---

## 9. Commits

**Atómicos:** un commit = un cambio lógico. El proyecto compila y las pruebas pasan en cada commit. No mezcles refactor con funcionalidad, ni formato con lógica.

Formato **Conventional Commits**:

```
<tipo>(<alcance>): <descripción>

[cuerpo opcional: el porqué, no el qué]

[pie opcional: Refs: RF-05 | BREAKING CHANGE: ...]
```

- **Tipos:** `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `perf`, `build`, `ci`, `chore`.
- **Alcance:** el módulo (`domain`, `data`, `review`, `export`, `engine-mlkit`, `designsystem`, `build`…).
- **Descripción:** en español, modo imperativo, minúscula inicial, sin punto final, ≤ 72 caracteres.
- Cuerpo envuelto a 72 columnas. Pie con el requisito: `Refs: RF-05`.

Ejemplos:
```
feat(review): reordenar páginas arrastrando la miniatura
test(domain): cubrir ExportDocument con perfil Ligero
fix(export): calcular peso estimado tras cambiar el filtro
refactor(data): extraer PageRenderer del repositorio
build: agregar Room y KSP al catálogo de versiones
```

Nunca commitees: secretos, `local.properties`, keystores, `google-services.json` con claves, archivos generados ni carpetas `build/`.

---

## 10. Skills y herramientas del agente

Si están instaladas en esta sesión, úsalas:
- **superpowers** (`brainstorming`, `writing-plans`, `test-driven-development`, `systematic-debugging`, `verification-before-completion`, `finishing-a-development-branch`): planificar cada requisito, TDD, depurar con método y verificar antes de declarar algo terminado.
- **hci-design-architect** antes de construir una pantalla nueva; **hci-usability-audit** al terminarla.
- **claude-mem**: memoria persistente entre sesiones. Al iniciar, revisa el contexto recuperado para saber en qué fase y requisito quedamos. Aun así, `docs/plan.md` y este archivo mandan sobre cualquier recuerdo.

Si una skill contradice este archivo, gana este archivo.

---

## 11. Definición de terminado

- [ ] El requisito tiene criterio de aceptación y prueba automática que lo cubre.
- [ ] La puerta de calidad local está en verde (y CI cuando exista).
- [ ] La pantalla funciona con TalkBack, en modo oscuro y con texto al 200 %.
- [ ] Textos en `strings.xml` (es y en).
- [ ] Commits atómicos con Conventional Commits y `Refs: RF-xx`.
- [ ] ADR si hubo una decisión de arquitectura.
- [ ] `CHANGELOG.md` actualizado en lenguaje de usuario.
- [ ] Informe al usuario con pruebas ejecutadas y su resultado.
