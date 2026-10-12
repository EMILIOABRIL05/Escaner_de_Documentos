# Folio

Escáner de documentos para Android: fotos de hojas → PDF liviano → compartir.
Sin anuncios, sin cuentas, sin rastreadores y sin conexión obligatoria.

> Estado: Fase 0 (fundaciones). Todavía no hay una versión publicada.

## Documentación

- [Plan del proyecto](docs/plan.md): requisitos, arquitectura, UX y roadmap. Es la fuente de verdad.
- [Reglas de trabajo](CLAUDE.md): stack, arquitectura, código limpio, pruebas y Git.
- [Decisiones de arquitectura](docs/adr/)

## Requisitos para compilar

- JDK 21 para ejecutar Gradle (el código se compila a bytecode 17).
- Android SDK con la plataforma 37 y build-tools 36.0.0.
- Un archivo `local.properties` con `sdk.dir=<ruta del SDK>`. Android Studio lo crea solo.

## Puerta de calidad

Antes de cada commit:

```bash
./gradlew ktlintCheck detekt lint testDebugUnitTest koverVerify
```

## Licencia

[Apache 2.0](LICENSE).
