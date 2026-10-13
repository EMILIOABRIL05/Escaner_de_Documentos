# ADR-001 · Arquitectura y fundaciones del proyecto

- Estado: aceptado
- Fecha: 2026-10-12
- Referencias: `docs/plan.md` §6, §11 y §13; `docs/superpowers/specs/2026-10-12-fase0-fundaciones-design.md`

## Contexto

Folio se desarrolla de forma incremental, una fase a la vez. La Fase 0 tiene que dejar listo:

- una base que compile;
- reglas de arquitectura verificables;
- una puerta de calidad que funcione igual en Windows y en la CI de Linux.

Todo esto sin adelantar trabajo de fases posteriores.

## Decisión

### Capas y módulos

- Se sigue la guía oficial de arquitectura de Android: interfaz, dominio y datos, con flujo de datos unidireccional.
- `core:model` y `core:domain` son módulos Kotlin puros, sin Android.
- **Solo se crean los módulos del MVP:**
  - `app`;
  - `core:{model, domain, data, database, storage, designsystem, testing}`;
  - `feature:{capture, review, library, export, settings}`;
  - `engine:{api, mlkit}`;
  - `pdf`.
- `feature:desktop`, `engine:opencv`, `server`, `sync:drive` y `web/` se crean al empezar la fase que los necesita.
- La build usa `build-logic/convention` con 7 plugins:
  - `folio.android.application`
  - `folio.android.library`
  - `folio.android.compose.library`
  - `folio.android.feature`
  - `folio.jvm.library`
  - `folio.hilt`
  - `folio.quality`
- Todas las versiones viven en `gradle/libs.versions.toml`.
- El plugin de librería Compose se llama `folio.android.compose.library` y no `folio.android.library.compose`. Con el segundo nombre, el alias `folio-android-library` del catálogo se convertiría en un grupo y dejaría de ser un plugin.

### Identidad

- El paquete base y el `applicationId` son `io.github.emilioabril05.folio`. Usan el dominio de GitHub del autor, así que no chocan con nadie.
- "Folio" sigue siendo un nombre provisional (plan §14.3). Cambiar el `applicationId` después de publicar no es posible sin perder las actualizaciones, así que se decide antes de la prueba cerrada.

### Versiones (todas estables, verificadas el 2026-10-12)

| Herramienta | Versión |
| --- | --- |
| Gradle | 9.8.1, con checksum fijado en el wrapper |
| AGP | 9.4.1 |
| Kotlin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Hilt | 2.60.1 |
| `compileSdk` / `targetSdk` | 37 |
| `minSdk` | 26 |
| JDK | 21 para ejecutar Gradle; bytecode 17 |

La regla de salida prevista (bajar Kotlin si KSP o detekt fallaban con 2.4.21) **no hizo falta**: los dos funcionan.

### Puerta de calidad

`./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify`, y en CI además `verifyRoborazziDebug` y `assembleRelease`.

- **ktlint 1.8.0** con el estilo `ktlint_official`.
- **detekt 1.23.8** con los umbrales de CLAUDE.md y las reglas de Compose 0.6.8.
  - `LongParameterList` usa el umbral 7 porque detekt avisa al *alcanzar* el umbral: así se permiten hasta 6 parámetros.
- **Kover:** ≥ 80 % de líneas en `core:*`, salvo `core:testing`, que contiene dobles de prueba.
- **Konsist** (en `app/src/test`, porque `app` es el único módulo que ve todo) valida:
  - aislamiento entre features;
  - `core:model` y `core:domain` sin Android;
  - el dominio sin dependencias de sus implementaciones;
  - los `ViewModel` dentro de `feature`;
  - **la prohibición de `!!`**.

  Cada regla tiene un fixture que demuestra que detecta su violación.
- `!!` se controla con Konsist y no con detekt. `UnsafeCallOnNullableType` necesita resolución de tipos, que la tarea `detekt` de la puerta no ejecuta, y la resolución de tipos de detekt 1.23 no es compatible con Kotlin 2.4. detekt 2.x solo existe en alpha.
- Los módulos Kotlin puros registran `testDebugUnitTest` como alias de `test`, para que la puerta también ejecute sus pruebas.
- Los módulos Android **sin** `src/test` toleran no descubrir pruebas: Hilt genera fuentes de prueba en los features vacíos y Gradle 9 fallaría. En cuanto un módulo tiene pruebas propias, la exigencia vuelve.
- Robolectric (SDK 36 sobre JDK 21) necesita `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`; lo aplica el convention plugin.
- `.gitattributes` fuerza LF para que ktlint y las capturas den el mismo resultado en Windows y en Linux.

### Seguridad desde el primer día (plan §12)

- Se elimina el permiso `AD_ID` del manifiesto.
- No hay copia de seguridad en la nube ni transferencia entre equipos.
- `cleartextTrafficPermitted="false"`.
- Solo `MainActivity` está exportada.

## Consecuencias

- **Páginas de 16 KB:** en F0 no hay código nativo. La verificación de alineación se agrega a la CI en v2, con OpenCV.
- **Modo Escritorio:** como `targetSdk = 37`, el Modo Escritorio (v1) necesitará pedir `ACCESS_LOCAL_NETWORK` en tiempo de ejecución.
- **Robolectric en las pruebas:**
  - Corre con el SDK 36. Su `NetworkSecurityPolicy` simulada no aplica la configuración de red, así que las pruebas verifican la configuración declarada (manifiesto + XML) y no su efecto en un dispositivo.
  - Las pruebas que miden texto usan `@GraphicsMode(NATIVE)`.
- **Deprecación de Gradle:** un plugin de terceros usa `Configuration.setVisible`, que Gradle 11 eliminará. Se revisa al actualizar plugins.
- **Lint:** deja dos advertencias:
  - `MissingApplicationIcon`: el ícono está pendiente, igual que el nombre.
  - `UnusedAttribute`: atrás predictivo en Android anterior al 13, inocuo.
