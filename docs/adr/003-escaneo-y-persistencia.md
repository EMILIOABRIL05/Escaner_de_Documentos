# ADR-003 · Contrato del escáner, persistencia y medición de cobertura

- Estado: aceptado
- Fecha: 2026-10-12
- Requisito: RF-01
- Referencias: ADR-001, ADR-002, `docs/plan.md` §6.3 y §6.4

## Contexto

RF-01 es el primer requisito con datos reales. Al implementarlo aparecieron cuatro decisiones de arquitectura.

## Decisiones

1. **Contrato del escáner en dos pasos.** ML Kit entrega su pantalla de forma asíncrona (`getStartScanIntent` devuelve una `Task<IntentSender>`), así que la firma del plan §6.3 (`contract(): ActivityResultContract`) no le sirve. `DocumentScanner` queda así:
   - `suspend fun prepare(activity, options): ScanLaunch`, que devuelve `Ready(IntentSenderRequest)` o `Failed(ScanError)`.
   - `fun parseResult(resultCode, data): ScanOutcome`, que devuelve `Success`, `Cancelled` o `Failed`.

   La pantalla lanza el escáner con `StartIntentSenderForResult`. El motor de OpenCV de v2 implementará el mismo contrato.
2. **Room desde RF-01** (decisión del usuario). Las tablas `documents` y `pages` se crean desde ya, con la versión 1 del esquema exportada en `core/database/schemas`. Room no garantiza el orden en `@Relation`, así que el DAO lee el documento y sus páginas ordenadas por `position` con dos consultas dentro de una transacción.
3. **Primero el disco, luego la base.** `CreateDocumentFromScan` copia cada original a `files/documents/{id}/original/{pageId}.jpg` antes de registrar el documento (RNF-06). Si algo falla, borra lo copiado y devuelve un error de dominio (`StorageError`), nunca una excepción.
4. **`core:testing` es Kotlin puro y la cobertura se mide en conjunto** (decisión del usuario).
   - `core:testing` pasa a ser JVM para que las pruebas de `core:domain` usen sus fakes.
   - Kover verifica ≥ 80 % sobre todos los módulos `core` juntos (salvo `core:testing`) desde la raíz. Así las pruebas del dominio también cuentan para las clases de datos de `core:model`.
   - En cada informe se muestra la cobertura por módulo, para detectar un módulo débil tapado por los demás.

## Consecuencias

- **Permisos que llegan con ML Kit:** `INTERNET` y `ACCESS_NETWORK_STATE`. No pide cámara ni `AD_ID` (lo vigila `AppManifestTest`). La política de privacidad lo explica.
- **Lo que no se prueba en local:** el lanzamiento real del escáner necesita Google Play services y un dispositivo. Las pruebas locales cubren el contrato con un escáner falso, la interpretación del resultado y los estados de la pantalla.
- **Nombre automático:** se forma en `app` con `strings.xml` ("Escaneo 2026-10-12 22.30" / "Scan …") y fecha ISO 8601, porque el dominio no conoce recursos de Android.
- **Deuda pendiente:**
  - Las pruebas de Compose usan `createComposeRule`, deprecada a favor de la API v2. Se migra en un refactor aparte.
  - La alternativa sin ML Kit (cámara del sistema + recorte manual) se resuelve junto con RF-02.
