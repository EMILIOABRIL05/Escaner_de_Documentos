# Folio — Plan del proyecto

Oct 6, 2026 · @Emilio

## 1. Resumen ejecutivo

Folio (nombre provisional) es una app Android nativa en Kotlin y Jetpack Compose, gratuita, sin publicidad y sin cuenta obligatoria, que convierte fotos de documentos en un PDF listo para compartir en 3 toques y menos de 30 segundos para 5 páginas.

**Problema.** Las apps de escaneo populares interrumpen cada captura con anuncios, ponen marcas de agua, empujan suscripciones y piden registrarse. Un estudiante al que el docente le pide "escanéelo y envíelo" solo quiere fotos → PDF → WhatsApp.

**Propuesta de valor.**

- Cero anuncios, cero rastreadores, cero cuenta. Nada que monetizar significa nada que interrumpa.
- Offline-first: detección de bordes, filtros, OCR y PDF se procesan en el teléfono.
- Flujo directo: abrir → escanear (lote) → compartir. La cámara es la pantalla de inicio de la acción principal.
- Mejor que la competencia en cinco puntos: captura en lote sin pausas, PDF ligero preajustado para WhatsApp y correo, Modo Escritorio por Wi-Fi con QR, OCR para buscar dentro de tus PDFs y exportación PDF/A para archivo.

**Principios rectores.**

1. Velocidad antes que funciones: cada función nueva se justifica si no agrega pasos al flujo principal.
2. Privacidad por diseño: los documentos nunca salen del teléfono salvo que el usuario lo decida.
3. Costo cero: solo herramientas y bibliotecas de licencia libre o gratuita.
4. Calidad medible: ISO/IEC 25010 para el producto, ISO 9241-11 para usabilidad y WCAG 2.2 nivel AA para accesibilidad.

**Decisiones de base.**

| Tema | Decisión | Por qué |
| --- | --- | --- |
| Plataforma | Android nativo (Kotlin), sin Flutter | Pediste Kotlin; acceso directo a cámara, ML Kit y almacenamiento |
| Interfaz | Jetpack Compose con estética inspirada en Apple | Mucho espacio en blanco, tipografía clara, sin tocar convenciones de Android |
| Web de escritorio | Página ligera servida por el propio teléfono | No requiere servidor ni hosting pagado |
| React | No se usa en la app; opcional (Preact) en la web si crece | Una sola pantalla web no justifica React |
| Nube | Sin backend propio; Google Drive y compartir del sistema | Cero costos de servidor y cero datos personales que custodiar |

## 2. Competencia y lógica de negocio

Todo escáner de documentos es la misma cadena de seis pasos: capturar, detectar bordes, corregir perspectiva, mejorar la imagen, armar el PDF y entregarlo. La competencia monetiza interrumpiendo esa cadena; Folio la deja intacta.

### 2.1 Qué hace la competencia

| App | Modelo | Lo que hace bien | Lo que falla para nuestro caso |
| --- | --- | --- | --- |
| [CamScanner](https://www.getapp.com/all-software/a/camscanner/) | Freemium con anuncios | Recorte, lote, OCR, muchas funciones | Anuncios frecuentes y marca de agua obligatoria en la versión gratis; quitarla exige suscripción |
| [Adobe Scan](https://softpicker.com/how-to-scan-to-pdf-free/) | Gratis con cuenta | OCR automático, buena calidad de PDF | Exige cuenta Adobe y sube los escaneos a su nube |
| [Genius Scan](https://help.geniusscan.com/) | Freemium sin anuncios | Plan básico gratis sin límite ni marca de agua, 100 % offline | OCR y exportación automática a la nube solo en el plan pagado |
| [Microsoft Lens](https://support.microsoft.com/en-gb/topic/retirement-of-microsoft-lens-fc965de7-499d-4d38-aeae-f6e48271652d) | Gratis (retirada) | Era gratis y sin anuncios | Retirada en 2026: salió de las tiendas el 9 de febrero y desde el 9 de marzo de 2026 ya no permite escanear; Microsoft recomienda OneDrive |
| [ML Kit Document Scanner](https://developers.google.com/ml-kit/vision/doc-scanner/android) (Google) | API gratuita | Detección automática, recorte, filtros y limpieza de manchas con ML, en el dispositivo | Interfaz de Google poco personalizable; requiere Google Play services y 1,7 GB de RAM |
| [FairScan](https://github.com/pynicolas/FairScan) | Libre (GPLv3) | Sin anuncios ni rastreo, sin permiso de internet, modelo propio de detección | No ofrece control fino del procesamiento; sin OCR ni nube |
| [OSS Document Scanner](https://github.com/ossappscollective/OSS-DocumentScanner) | Libre (MIT) | Detección y recorte automáticos con OpenCV, OCR con Tesseract | Hecho con NativeScript, no Kotlin nativo; útil como referencia de su canal OpenCV + Tesseract |

Lectura: el hueco que dejó Microsoft Lens (gratis, sin anuncios, con OCR) no lo cubre nadie con una interfaz cuidada. Ese es el lugar de Folio.

### 2.2 Modelo de dominio

- **Documento**: id, título, fecha de creación y modificación, etiquetas, estado (activo o en papelera).
- **Página**: orden, imagen original, cuadrilátero de recorte (4 esquinas), rotación, filtro, texto OCR.
- **Exportación**: PDF derivado del documento, perfil de calidad usado, tamaño en bytes, fecha.
- **Perfil de exportación**: resolución, calidad JPEG, color o escala de grises, tamaño de hoja.

### 2.3 Reglas de negocio

1. **Edición no destructiva.** La foto original nunca se modifica; recorte, rotación y filtro se guardan como parámetros y la imagen final se recalcula. Permite deshacer siempre.
2. **El PDF es derivado.** Se regenera cuando cambia una página y se guarda en caché mientras no cambie.
3. **Nombre automático.** "Escaneo 2026-10-06 22.30" por defecto; con OCR activo se sugiere la primera línea del texto.
4. **Perfiles de exportación** (valores iniciales, a validar con pruebas):
   - Alta: 300 ppp, JPEG calidad 90, color.
   - Equilibrado (por defecto): 200 ppp, JPEG calidad 75.
   - Ligero para WhatsApp y correo: 150 ppp, JPEG calidad 60, escala de grises si la página es solo texto.
5. **Tamaño de hoja.** "Ajustar a la imagen" por defecto; A4 y Carta como opciones.
6. **Papelera de 30 días.** Borrar mueve a la papelera; solo la papelera borra de verdad.
7. **Páginas editables después.** Se pueden añadir, reordenar, rotar o quitar páginas de un documento ya guardado.

### 2.4 Qué copiar, mejorar y evitar

- **Copiar:** captura automática al detectar la hoja, lote de varias páginas, filtros de un toque, OCR.
- **Mejorar:** cero pantallas entre la captura y el PDF, perfil "Ligero" pensado para WhatsApp, Modo Escritorio por Wi-Fi, búsqueda por texto OCR, edición no destructiva.
- **Evitar:** anuncios, marcas de agua, cuentas obligatorias, nube propia, suscripciones, analítica de terceros y avisos para "pasarse a Pro".

## 3. Requisitos funcionales

El MVP son 12 requisitos Must que resuelven el caso del docente: escanear varias hojas, ajustarlas, filtrarlas y enviar el PDF por WhatsApp. La columna Prioridad es editable si cambias de opinión.

| ID | Área | Requisito | Prioridad |
| --- | --- | --- | --- |
| RF-01 | Captura | Escanear varias páginas seguidas con detección de bordes y disparo automático opcional | Must · MVP |
| RF-02 | Captura | Ajustar a mano las 4 esquinas con lupa de precisión | Must · MVP |
| RF-03 | Captura | Importar fotos de la galería con el selector de fotos del sistema, sin permiso de almacenamiento | Must · MVP |
| RF-04 | Edición | Rotar, reordenar arrastrando, eliminar y añadir páginas | Must · MVP |
| RF-05 | Edición | Filtros de un toque: Original, Documento (blanco y negro adaptativo), Grises, Color realzado; "aplicar a todas" | Must · MVP |
| RF-06 | Exportación | Generar PDF multipágina con perfil Alta, Equilibrado o Ligero | Must · MVP |
| RF-07 | Exportación | Compartir con la hoja nativa de Android (WhatsApp, Gmail, Drive, Quick Share) | Must · MVP |
| RF-08 | Exportación | Mostrar el tamaño estimado del PDF antes de compartir | Must · MVP |
| RF-09 | Exportación | Guardar en Descargas o en una carpeta que elija el usuario | Must · MVP |
| RF-10 | Biblioteca | Lista de documentos recientes con miniatura, título, fecha y número de páginas | Must · MVP |
| RF-11 | Biblioteca | Renombrar, duplicar, enviar a la papelera y restaurar | Must · MVP |
| RF-12 | Biblioteca | Buscar por título | Must · MVP |
| RF-13 | Inteligencia | OCR en el dispositivo y búsqueda por el texto de las páginas | Should · v1 |
| RF-14 | Exportación | PDF con capa de texto invisible (seleccionable y buscable) | Should · v1 |
| RF-15 | Exportación | Exportar páginas como JPG o PNG, o todo en un ZIP | Should · v1 |
| RF-16 | Biblioteca | Unir documentos y dividir uno en varios | Should · v1 |
| RF-17 | Integración | Subir a Google Drive a una carpeta elegida, sin salir de la app | Should · v1 |
| RF-18 | Integración | Modo Escritorio: servidor web temporal en la red local, con QR, para ver y descargar PDFs desde la PC | Should · v1 |
| RF-19 | Edición | Ajustar brillo, contraste y nitidez por página | Should · v1 |
| RF-20 | Acceso | Atajo "Escanear" en el ícono de la app y en Ajustes rápidos | Should · v1 |
| RF-21 | Acceso | Recibir imágenes compartidas desde otras apps (por ejemplo, fotos de WhatsApp) y convertirlas en PDF | Should · v1 |
| RF-22 | Exportación | Exportar en PDF/A para archivo de largo plazo | Could · v2 |
| RF-23 | Exportación | Proteger el PDF con contraseña | Could · v2 |
| RF-24 | Edición | Firma manuscrita y anotaciones simples | Could · v2 |
| RF-25 | Captura | Modo libro: dos páginas por foto y corrección de curvatura | Could · v2 |
| RF-26 | Captura | Modo cédula: anverso y reverso en una sola hoja | Could · v2 |
| RF-27 | Integración | Subir archivos de la PC al teléfono desde el Modo Escritorio | Could · v2 |
| RF-28 | Fuera de alcance | Cuentas de usuario y nube propia | Won't |
| RF-29 | Fuera de alcance | Anuncios, suscripciones, marcas de agua | Won't |
| RF-30 | Fuera de alcance | Editar el texto de un PDF existente | Won't |

**Criterio de aceptación del flujo principal** (formato Gherkin, base para pruebas automáticas):

```gherkin
Escenario: Enviar una prueba de 3 hojas por WhatsApp
  Dado que abro Folio con la cámara lista
  Cuando capturo 3 hojas y toco "Listo"
  Y elijo "Compartir" con el perfil "Ligero"
  Entonces se abre la hoja de compartir con un PDF de 3 páginas
  Y el PDF pesa menos de 1,5 MB
  Y no apareció ninguna pantalla intermedia distinta de la revisión
```

## 4. Requisitos no funcionales

Cada requisito no funcional cuelga de una de las nueve características de [ISO/IEC 25010:2023](https://quality.arc42.org/standards/iso-25010) y tiene un número que se puede comprobar en CI o en pruebas con usuarios. La versión 2023 renombró Usabilidad como Capacidad de interacción y Portabilidad como Flexibilidad, y añadió Protección (Safety).

| ID | Característica ISO/IEC 25010 | Requisito medible | Cómo se verifica |
| --- | --- | --- | --- |
| RNF-01 | Eficiencia de desempeño | Del ícono al visor de cámara en menos de 1,5 s (arranque en frío, gama media) | Macrobenchmark en CI |
| RNF-02 | Eficiencia de desempeño | Recorte + filtro de una página en menos de 700 ms; PDF de 10 páginas en menos de 3 s | Pruebas de rendimiento instrumentadas |
| RNF-03 | Eficiencia de desempeño | Perfil Ligero: 150 KB o menos por página A4 de texto; descarga de la app de 20 MB o menos | Corpus de 30 hojas de prueba; informe de tamaño del AAB |
| RNF-04 | Eficiencia de desempeño | Nunca más de un bitmap a resolución completa en memoria; sin errores de memoria en equipos de 2 GB de RAM | LeakCanary en debug; prueba en emulador de 2 GB |
| RNF-05 | Fiabilidad | Cierres percibidos por debajo de 0,5 % y ANR por debajo de 0,2 % de usuarios diarios (Google Play penaliza desde 1,09 % y 0,47 %) | Android vitals |
| RNF-06 | Fiabilidad | Ninguna página capturada se pierde si el sistema cierra la app: cada captura se guarda en disco antes de mostrarse | Prueba de "muerte de proceso" con ADB |
| RNF-07 | Capacidad de interacción | Del ícono a la hoja de compartir en 3 toques más las capturas; éxito de tarea de 95 % o más; SUS de 80 o más | Pruebas con 5 usuarios por iteración |
| RNF-08 | Capacidad de interacción | WCAG 2.2 AA: contraste de texto 4,5:1, objetivos táctiles de 48 dp, todo operable con TalkBack, texto al 200 % sin cortes | Accessibility Scanner y pruebas de Compose |
| RNF-09 | Seguridad | Sin SDK de anuncios ni analítica; documentos en el almacenamiento privado de la app; servidor local apagado por defecto | Revisión de dependencias en CI; MASVS (sección 12) |
| RNF-10 | Compatibilidad | Android 8.0 (API 26) o superior; los PDFs se abren en WhatsApp, Gmail, Drive y lectores de PC | Matriz de pruebas en Firebase Test Lab (cuota gratuita) o equipos propios |
| RNF-11 | Mantenibilidad | Cobertura de 80 % o más en dominio y datos; cero alertas de detekt y ktlint; funciones con complejidad ciclomática de 15 o menos | Puerta de calidad en CI |
| RNF-12 | Flexibilidad | Motor de escaneo intercambiable (ML Kit u OpenCV) sin tocar la interfaz; textos en español e inglés; tabletas y horizontal | Pruebas de contrato del motor; revisión de recursos |
| RNF-13 | Protección | Ninguna acción borra un documento sin poder deshacerse o sin confirmación explícita | Pruebas de interfaz de la papelera |
| RNF-14 | Idoneidad funcional | Todo PDF generado es válido según ISO 32000; el PDF/A pasa la validación | qpdf --check y veraPDF en CI |

Los umbrales de Google Play vienen de [Android vitals](https://developer.android.com/vitals): por encima de ellos Play puede reducir la visibilidad de la app. Los demás números son metas iniciales para ajustar tras la primera medición.

## 5. Stack tecnológico

Todo el stack es Kotlin y bibliotecas de licencia Apache 2.0 o APIs gratuitas de Google; el único gasto posible es la cuenta de Google Play, USD 25 una sola vez, y es opcional.

| Capa | Herramienta | Licencia | Para qué | Fase |
| --- | --- | --- | --- | --- |
| Lenguaje | Kotlin + Coroutines + Flow | Apache 2.0 | Todo el código; concurrencia estructurada para no bloquear la interfaz | MVP |
| Interfaz | Jetpack Compose + Material 3 con tema propio | Apache 2.0 | Interfaz declarativa; el tema propio da la estética tipo Apple | MVP |
| Navegación | Navigation Compose con rutas tipadas | Apache 2.0 | Pila de pantallas y botón atrás del sistema | MVP |
| Escaneo (motor 1) | [ML Kit Document Scanner](https://developers.google.com/ml-kit/vision/doc-scanner/android) | Gratis (términos de Google) | Captura, bordes, recorte, filtros y limpieza con ML; usa la cámara de Play services, así la app no pide permiso de cámara | MVP |
| Escaneo (motor 2) | CameraX + OpenCV | Apache 2.0 | Cámara y procesamiento propios, control total del flujo y versión sin Google | v2 |
| OCR | ML Kit Text Recognition v2 | Gratis (términos de Google) | Texto de cada página, en el dispositivo | v1 |
| PDF básico | `android.graphics.pdf.PdfDocument` | Incluido en Android | Armar PDFs de imágenes sin dependencias | MVP |
| PDF avanzado | [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) | Apache 2.0 | Capa de texto OCR, contraseña, unir y dividir, metadatos PDF/A | v1 |
| Datos | Room (SQLite) + DataStore | Apache 2.0 | Documentos y páginas; preferencias del usuario | MVP |
| Inyección | Hilt | Apache 2.0 | Dependencias verificadas al compilar; facilita pruebas | MVP |
| Trabajo en segundo plano | WorkManager | Apache 2.0 | OCR y subidas a Drive que sobreviven a cierres de la app | v1 |
| Imágenes | Coil | Apache 2.0 | Miniaturas con caché y muestreo para ahorrar memoria | MVP |
| Servidor local | Ktor Server (motor CIO) | Apache 2.0 | Modo Escritorio por Wi-Fi | v1 |
| Código QR | ZXing core | Apache 2.0 | QR con la dirección del Modo Escritorio | v1 |
| Web de escritorio | HTML + CSS + JavaScript sin framework (Preact si crece) | MIT | Página de unos 20 KB servida por el teléfono | v1 |
| Nube | Google Drive API v3 + AuthorizationClient | Gratis | Subir PDFs con el alcance `drive.file` | v1 |
| Errores en producción | Android vitals de Play Console | Gratis | Cierres y ANR sin meter ningún SDK de terceros | MVP |

**Qué evitar y por qué.**

- **iText:** su licencia AGPL obliga a publicar tu código bajo AGPL o a pagar licencia comercial. PdfBox-Android hace lo necesario con Apache 2.0.
- **Google Sign-In clásico:** está obsoleto; para Drive se usa [AuthorizationClient](https://developer.android.com/identity/sign-in/legacy-gsi-migration), que pide permiso solo cuando el usuario toca "Subir a Drive".
- **Firebase Analytics, Crashlytics o cualquier SDK de anuncios:** contradicen la promesa de cero rastreadores.
- **Bibliotecas nativas sin alinear a 16 KB:** desde el 1 de noviembre de 2025 Google Play exige que el código nativo soporte páginas de memoria de 16 KB en apps que apuntan a Android 15 o superior. Afecta a OpenCV en v2; se verifica con APK Analyzer.

**Dos variantes de compilación (product flavors).**

- `play`: ML Kit + Google Drive. Se publica en Google Play.
- `libre`: OpenCV + Tesseract, sin servicios de Google. Se publica en GitHub Releases e IzzyOnDroid, gratis. Llega en v2, cuando exista el motor 2.

**Distribución.** GitHub Releases no cuesta nada. Google Play cuesta USD 25 una vez; las cuentas personales creadas después del 13 de noviembre de 2023 deben hacer una prueba cerrada con 12 testers durante 14 días seguidos antes de publicar ([detalle](https://extendsclass.com/blog/?p=2302)). Conviene reclutar compañeros de curso desde el inicio.

**Herramientas de desarrollo** (todas gratuitas): Android Studio, Gradle con Kotlin DSL y catálogo de versiones, GitHub Actions, detekt, ktlint, Android Lint, Kover, Roborazzi, Macrobenchmark, LeakCanary y Renovate.

## 6. Arquitectura

Folio sigue la [guía oficial de arquitectura de Android](https://developer.android.com/topic/architecture): tres capas (interfaz, dominio, datos), flujo de datos unidireccional y módulos Gradle cuyas dependencias apuntan siempre hacia el dominio. Así el motor de escaneo, el generador de PDF o la nube se cambian sin tocar las pantallas.

&#91;embedded content: arquitectura · 3 capas y sus módulos\]

Interfaz y datos dependen del dominio, nunca al revés; por eso ML Kit se cambia por OpenCV sin tocar una pantalla. Solo `:app` ve todos los módulos, para cablearlos con Hilt.

### 6.1 Capas

- **Interfaz:** pantallas Compose sin lógica + un ViewModel por pantalla que expone un único `StateFlow<UiState>` y recibe eventos. Patrón UDF: el estado baja, los eventos suben.
- **Dominio:** Kotlin puro, sin Android. Entidades, casos de uso (`ExportDocument`, `ReorderPages`, `ApplyFilter`) e interfaces que la capa de datos implementa (inversión de dependencias).
- **Datos:** repositorios y fuentes concretas: Room, archivos, ML Kit, PdfBox, Drive, servidor Ktor.

### 6.2 Módulos Gradle

| Módulo | Contiene | Depende de |
| --- | --- | --- |
| `:app` | Punto de entrada, navegación, cableado de Hilt | Todos los `:feature` |
| `:feature:capture` | Lanzar el escáner y recibir páginas | `:core:domain`, `:core:designsystem`, `:engine:api` |
| `:feature:review` | Revisar, recortar, filtrar, reordenar | `:core:domain`, `:core:designsystem` |
| `:feature:library` | Lista, búsqueda, papelera | `:core:domain`, `:core:designsystem` |
| `:feature:export` | Perfiles, tamaño estimado, compartir | `:core:domain`, `:core:designsystem` |
| `:feature:desktop` | Modo Escritorio (pantalla + servicio) | `:core:domain`, `:server` |
| `:feature:settings` | Preferencias | `:core:domain` |
| `:core:model` | Entidades puras (`Document`, `Page`, `ExportProfile`) | Nada |
| `:core:domain` | Casos de uso e interfaces | `:core:model` |
| `:core:data` | Repositorios | `:core:domain`, `:core:database`, `:core:storage` |
| `:core:database` | Room: entidades, DAOs, migraciones | `:core:model` |
| `:core:storage` | Archivos, FileProvider, carpetas del usuario | `:core:model` |
| `:core:designsystem` | Tema, tokens, componentes reutilizables | Nada |
| `:engine:api` / `:engine:mlkit` / `:engine:opencv` | Contrato del escáner y sus dos implementaciones | `:core:model` |
| `:pdf` | Escritura de PDF (Android nativo y PdfBox) | `:core:domain` |
| `:server` | Ktor + página web empaquetada | `:core:domain` |
| `:sync:drive` | Subida a Google Drive | `:core:domain` |
| `:core:testing` | Dobles de prueba y reglas compartidas | `:core:domain` |

Regla: un `:feature` nunca depende de otro `:feature`. Se valida en CI con una prueba de arquitectura (Konsist).

### 6.3 Contratos clave

```kotlin
// :engine:api — el escáner es intercambiable
interface DocumentScanner {
    fun contract(options: ScanOptions): ActivityResultContract<Unit, ScanOutcome>
}

sealed interface ScanOutcome {
    data class Success(val pages: List<Uri>) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Failed(val reason: ScanError) : ScanOutcome
}

// :core:domain — caso de uso puro, se prueba sin Android
class ExportDocument(
    private val documents: DocumentRepository,
    private val pages: PageRenderer,
    private val pdf: PdfWriter,
) {
    suspend operator fun invoke(id: DocumentId, profile: ExportProfile): Result<ExportedPdf> =
        runCatching {
            val doc = documents.get(id)
            val rendered = doc.pages.map { pages.render(it, profile) }
            pdf.write(doc.title, rendered, profile)
        }
}

// :feature:review — un solo estado por pantalla
data class ReviewUiState(
    val pages: List<PageUi> = emptyList(),
    val selectedFilter: Filter = Filter.Document,
    val estimatedSize: Bytes? = null,
    val isExporting: Boolean = false,
)
```

### 6.4 Almacenamiento en disco

- `files/documents/{docId}/original/{pageId}.jpg`: fotos originales, nunca se modifican.
- `cache/rendered/{pageId}_{hash}.jpg`: páginas procesadas; el sistema puede borrarlas y se regeneran.
- `files/exports/{docId}.pdf`: último PDF generado, compartido como `content://` mediante FileProvider.
- Base Room: metadatos, orden de páginas, parámetros de edición y texto OCR (tabla FTS4 para buscar).

## 7. Procesamiento de imagen y PDF

La vista previa de filtros es instantánea porque se pinta con una matriz de color en la GPU; el procesamiento pesado a resolución completa solo ocurre al exportar, en segundo plano.

### 7.1 Canal de procesamiento (motor 2, OpenCV)

En el MVP, ML Kit hace los pasos 1 a 4 por nosotros. Este es el canal propio para v2, en la línea de lo que hace [OSS Document Scanner](https://github.com/ossappscollective/OSS-DocumentScanner):

1. **Captura:** CameraX con `ImageAnalysis` a baja resolución para detectar, e `ImageCapture` a resolución completa para la foto.
2. **Detección de bordes:** escala de grises → desenfoque gaussiano → Canny → `findContours` → `approxPolyDP` → el cuadrilátero convexo más grande. Disparo automático cuando el cuadrilátero se mantiene estable unos 600 ms.
3. **Ajuste manual:** 4 esquinas arrastrables con lupa; se guardan como coordenadas normalizadas de 0 a 1.
4. **Corrección de perspectiva:** `getPerspectiveTransform` + `warpPerspective` al tamaño de la hoja detectada.
5. **Filtro:**
   - Documento: normalizar iluminación (dividir por el fondo desenfocado, quita sombras) + umbral adaptativo.
   - Grises: escala de grises + CLAHE para contraste local.
   - Color realzado: balance de blancos + saturación leve + máscara de enfoque.
6. **Reescalado:** a la resolución del perfil (150, 200 o 300 ppp) según el tamaño físico de la hoja.
7. **Codificación:** JPEG con la calidad del perfil; las páginas en blanco y negro puro van como imagen de 1 bit con compresión CCITT G4, mucho más ligera.

### 7.2 Generación del PDF

- **MVP:** `PdfDocument` de Android dibuja cada imagen en una página del tamaño elegido. Sin dependencias.
- **v1:** PdfBox-Android para:
  - Capa de texto invisible con el OCR, palabra por palabra en su posición, para que el PDF sea buscable y seleccionable.
  - Imágenes CCITT G4 para páginas en blanco y negro.
  - Contraseña (cifrado AES de 256 bits), unir y dividir.
- **v2:** PDF/A-2b ([ISO 19005-2](https://www.iso.org/standard/50655.html)) para archivo: perfil de color sRGB incrustado, metadatos XMP y fuentes incrustadas. Se valida con veraPDF en CI.

### 7.3 Tamaño estimado antes de compartir

Se codifica la primera página con el perfil elegido y se multiplica por el número de páginas, con un margen del 10 %. El cálculo tarda menos de 300 ms y se recalcula al cambiar de perfil.

### 7.4 Memoria y rendimiento

- Decodificar siempre con `inSampleSize` al tamaño de pantalla para miniaturas y vista previa.
- Procesar página por página en `Dispatchers.Default`, liberando cada bitmap antes de pasar al siguiente.
- Cancelar el trabajo si el usuario sale de la pantalla (corrutinas atadas al `viewModelScope`).
- Baseline Profile para que las pantallas principales arranquen compiladas.

## 8. Modo Escritorio por red local

Sí vale la pena, pero en v1 y no en el MVP: la hoja de compartir ya cubre WhatsApp y Quick Share. Su ventaja es que en la PC no hay que instalar nada ni iniciar sesión; basta un navegador y escanear un QR.

### 8.1 Cómo se usa

1. En el teléfono, el usuario toca "Modo Escritorio". La app pide el permiso de red local solo en ese momento.
2. El teléfono muestra un QR y debajo la dirección escrita, por ejemplo `http://192.168.1.23:8765`.
3. En la PC, el usuario abre esa dirección (o escanea el QR con la cámara de la laptop).
4. El teléfono pregunta "¿Permitir a este navegador?" y el usuario acepta.
5. La PC ve la lista de documentos con miniaturas y descarga cualquier PDF con un clic.
6. Si el usuario escanea algo nuevo mientras tanto, aparece en la PC al instante.
7. Al salir de la pantalla, o tras 10 minutos sin uso, el servidor se apaga solo.

### 8.2 Diseño técnico

- **Servidor:** Ktor con motor CIO, enlazado solo a la IP Wi-Fi del teléfono (nunca a datos móviles), en un puerto alto.
- **Ciclo de vida:** vive mientras la pantalla de Modo Escritorio está abierta, con la pantalla encendida. Así no se necesita un servicio en primer plano, que en Android 14 y posteriores exige justificar su tipo ante Google Play.
- **Rutas:**
  - `GET /`: página web empaquetada en la app (HTML, CSS y JS, unos 20 KB).
  - `GET /api/documents`: lista en JSON.
  - `GET /api/documents/{id}/thumb` y `GET /api/documents/{id}/pdf`: miniatura y descarga.
  - `GET /api/events`: eventos del servidor (SSE) para avisar de escaneos nuevos.
  - `POST /api/upload`: subir archivos de la PC al teléfono (RF-27, v2).
- **Permisos:** `INTERNET` y `ACCESS_NETWORK_STATE`. Desde Android 17, las apps que apuntan a API 37 tienen la red local bloqueada por defecto y deben pedir `ACCESS_LOCAL_NETWORK` en tiempo de ejecución ([guía oficial](https://developer.android.com/privacy-and-security/local-network-permission)).

### 8.3 Seguridad

Una red Wi-Fi compartida (universidad, cafetería) es el riesgo principal, así que el servidor asume que cualquiera en la red puede intentar entrar.

- **Token de sesión** de 128 bits aleatorios, generado cada vez que se abre el modo. Va en el fragmento de la URL del QR (`#t=…`), que el navegador no envía al servidor ni guarda en registros; la página lo lee y lo manda en una cabecera.
- **Aprobación en el teléfono** de cada navegador nuevo, con su nombre y sistema operativo.
- **Solo lectura por defecto**; la subida desde la PC se activa aparte.
- **Límite de intentos:** 5 tokens inválidos bloquean esa IP durante la sesión.
- **Sin HTTPS:** un certificado autofirmado haría que el navegador muestre una advertencia que asusta. Se acepta el riesgo porque la sesión es corta, aprobada y temporal; se documenta y se revisa en v2.

### 8.4 Alternativas que ya existen

| Opción | Ventaja | Limitación |
| --- | --- | --- |
| Hoja de compartir → WhatsApp | Es lo que pide el docente; cero configuración | Pasa por la nube de Meta |
| [Quick Share para Windows](https://www.android.com/intl/us_en/filetransfer/) | Rápido, directo entre dispositivos | Hay que instalar la app de Google en Windows 10 u 11 de 64 bits |
| Google Drive | Queda respaldado | Requiere internet y cuenta |
| Modo Escritorio de Folio | Sin instalar nada en la PC; ve toda la biblioteca | Ambos equipos en la misma red Wi-Fi |

## 9. Integraciones

El MVP no integra ninguna API externa: todo sale por la hoja de compartir de Android, que ya llega a WhatsApp, Gmail, Drive y Quick Share. Las integraciones directas llegan en v1 para ahorrar toques, no para habilitar funciones.

### 9.1 Compartir (MVP)

- El PDF se entrega como URI `content://` mediante FileProvider, con permiso de lectura temporal. Nunca se expone una ruta de archivo.
- **Botón directo "WhatsApp"** junto a "Compartir": abre WhatsApp con el PDF adjunto en un toque. Si WhatsApp no está instalado, el botón no aparece. Requiere declarar el paquete en `<queries>` por la visibilidad de paquetes de Android 11+.
- Nombre del archivo legible: `Prueba-Calculo-2026-10-06.pdf`, nunca `IMG_8823.pdf`.

### 9.2 Recibir de otras apps (v1)

- Filtros de intent `ACTION_SEND` y `ACTION_SEND_MULTIPLE` para imágenes: desde la galería o WhatsApp, "Compartir → Folio" convierte esas fotos en un PDF.
- El selector de fotos del sistema (`PickMultipleVisualMedia`) importa sin pedir permiso de almacenamiento.

### 9.3 Guardar en el teléfono

- **Descargas:** MediaStore en Android 10+; en Android 8 y 9 se pide el permiso de escritura solo para esas versiones (`maxSdkVersion="28"`).
- **Carpeta elegida:** Storage Access Framework (`OpenDocumentTree`). Funciona también con carpetas de Drive, OneDrive o una memoria SD si el usuario las elige.

### 9.4 Google Drive directo (v1)

1. El usuario toca "Subir a Drive" por primera vez.
2. AuthorizationClient pide solo el alcance `drive.file`: la app ve únicamente los archivos que ella misma crea. Google lo clasifica como no sensible, así que solo requiere [verificación básica](https://developers.google.com/workspace/drive/api/guides/api-specific-auth).
3. La app crea una carpeta "Folio" en Drive (o una subcarpeta por materia) y sube ahí.
4. La subida es reanudable y corre en WorkManager con la condición "hay red" y reintentos con espera exponencial. Si el usuario cierra la app, la subida continúa.
5. Al terminar, una notificación ofrece "Abrir en Drive" o "Copiar enlace".

Configuración única y gratuita: un proyecto en Google Cloud con la Drive API activada y un cliente OAuth de tipo Android (nombre del paquete + huella SHA-1 de la firma).

## 10. Diseño UX y HCI

La app tiene una sola pantalla principal y un botón grande "Escanear" al alcance del pulgar; todo lo demás aparece solo cuando hace falta. La estética es de Apple; el comportamiento es de Android.

### 10.1 Usuario y contexto

- **Persona principal:** estudiante universitario con un Android de gama media. El docente pide "escánelo y envíelo" y hay que hacerlo en el aula, con prisa, luz irregular y el teléfono en una mano.
- **Personas secundarias:** docente que digitaliza pruebas en lote; persona que archiva recibos y trámites.
- **Tarea principal:** fotos de varias hojas → un PDF legible y liviano → WhatsApp. Menos de 45 s para 3 hojas.
- **Tareas secundarias:** encontrar un documento viejo, pasarlo a la PC, subirlo a Drive, corregir una página mal recortada.
- **Experiencia previa:** ya usa CamScanner o la cámara; espera bordes automáticos y la hoja de compartir de Android.

### 10.2 Modelo conceptual y metáfora

**Metáfora: la pila de hojas y la fotocopia.** Un documento es una pila de hojas que puedes reordenar, girar o recortar como tarjetas sobre la mesa. El PDF es la fotocopia que entregas: sale de la pila, pero la pila sigue intacta para corregirla y sacar otra copia.

| Modelo conceptual (sistema) | Modelo mental (usuario) | En la interfaz |
| --- | --- | --- |
| Documento con páginas ordenadas | Una pila de hojas | Cuadrícula de miniaturas arrastrables |
| Parámetros de edición no destructivos | "Arreglo la hoja" | Recortar y filtrar siempre se puede deshacer |
| PDF derivado y en caché | "Saco la copia y la envío" | Botón "Enviar PDF" con páginas y peso |
| Perfil de exportación | "Calidad de la copia" | Ligero, Equilibrado, Alta, con su peso en KB |
| Papelera con 30 días | "La tiré, pero puedo sacarla" | Deshacer al borrar; Papelera en el menú |

**Reducción de carga cognitiva:** buenos valores por defecto (filtro Documento, nombre automático, perfil Equilibrado), cero formularios en el flujo principal y ninguna decisión que no cambie el resultado.

### 10.3 Arquitectura de información

- **Documentos** (inicio)
  - Escanear (pantalla completa) → Revisión → Editor de página
  - Revisión → Hoja "Enviar" (WhatsApp, Compartir, Guardar, Drive)
  - Buscar (campo en la parte superior)
  - Modo Escritorio (ícono en la barra)
  - Menú: Papelera, Ajustes, Acerca de y licencias

Sin barra de pestañas: con un solo destino frecuente, las pestañas añadirían decisiones sin aportar nada.

### 10.4 Flujos

&#91;embedded content: flujo principal · 5 pasos, 1 decisión, 2 caminos alternos\]

Ningún camino termina en un callejón: si el escáner no está disponible se usa la cámara del sistema, y si el usuario cancela, lo capturado queda como borrador.

**Atajos (Nielsen 7):** mantener pulsado el ícono de la app o el botón de Ajustes rápidos abre el escáner directamente; en la hoja "Enviar", el último destino usado aparece primero.

### 10.5 Pantallas (especificación)

**Documentos (inicio)**

- Propósito: retomar algo reciente o empezar a escanear.
- Regiones: título grande "Documentos" que se reduce al desplazar → campo de búsqueda → lista agrupada por fecha (Hoy, Esta semana, Anteriores) → botón flotante "Escanear" abajo al centro.
- Fila: miniatura de 56 dp, título, "3 páginas · 22:30". Deslizar a la izquierda envía a la papelera con "Deshacer"; a la derecha, comparte.
- Botón Escanear: 56 dp de alto, ancho completo menos 32 dp de margen, en la zona del pulgar.
- Estado vacío: "Escanea tu primera hoja" con el mismo botón.

**Revisión**

- Propósito: comprobar que las hojas salieron bien y enviar.
- Regiones: barra superior (atrás, título editable con un toque, menú) → cuadrícula de 2 columnas con número de página, arrastrable, y una celda final "+ Añadir" → barra inferior.
- Barra inferior: chips de filtro (Original, Documento, Grises, Color) con "Aplicar a todas"; Rotar; Recortar; Eliminar.
- Botón principal: "Enviar PDF · 3 pág. · \~420 KB", con el peso recalculado al cambiar el filtro.

**Editor de página**

- Imagen completa con 4 esquinas de 48 dp y lupa al arrastrar; detecta y se pega al borde más cercano.
- Abajo: Rotar, Restablecer, Hecho. Deshacer y rehacer arriba.

**Hoja "Enviar"** (panel inferior)

- Selector segmentado Ligero, Equilibrado, Alta, cada uno con su peso.
- Fila de destinos grandes: WhatsApp (si existe), Compartir…, Guardar en el teléfono, Google Drive.
- Interruptor "Texto buscable (OCR)".

**Modo Escritorio**

- QR de 240 dp al centro, dirección en fuente monoespaciada debajo, estado ("Esperando conexión…", "Conectado: Chrome en Windows").
- Lista de navegadores conectados con "Quitar"; botón "Detener".

**Ajustes:** lista agrupada en tarjetas (estilo iOS): perfil por defecto, tamaño de hoja, nombre automático, OCR, tema, licencias.

### 10.6 Estilo visual: estética Apple, comportamiento Android

Apple organiza hoy su guía de diseño en tres principios: [jerarquía, armonía y consistencia](https://developer.apple.com/videos/play/wwdc2025/102/). Se toman la jerarquía (el contenido manda y los controles flotan encima) y la sobriedad visual. El comportamiento sigue a Android: gesto atrás del sistema, atrás predictivo, hoja de compartir nativa y tamaño de letra del sistema (Nielsen 4).

- **Tipografía:** Inter (licencia OFL, gratuita). San Francisco solo puede usarse en plataformas de Apple. Escala en sp: título grande 34, título 22, cuerpo 17, nota 13.
- **Color:** fondo gris muy claro con tarjetas blancas (modo claro); negro y gris carbón (modo oscuro). Un solo color de acento, azul `#2563EB`, con contraste de 5,2:1 sobre blanco. El color nunca es el único indicador de estado.
- **Forma:** esquinas de 12 a 16 dp, sombras muy suaves, separadores finos, mucho espacio en blanco (rejilla de 8 dp).
- **Materiales:** barras translúcidas con desenfoque en Android 12+; color sólido en versiones anteriores.
- **Movimiento:** 200 a 300 ms, salida suave; paneles con efecto resorte. Se respeta "Quitar animaciones" del sistema.
- **Íconos:** Material Symbols en estilo redondeado (Apache 2.0); los SF Symbols de Apple no están permitidos en Android.
- **Háptica:** vibración ligera al capturar y al soltar una página reordenada.

### 10.7 Justificación HCI

| Heurística de Nielsen | Cómo se aplica |
| --- | --- |
| 1. Visibilidad del estado | Contador de páginas al capturar; peso estimado en el botón Enviar; progreso de OCR y Drive en notificación |
| 2. Correspondencia con el mundo real | Metáfora de pila y fotocopia; "Ligero" en vez de "150 ppp, JPEG 60" |
| 3. Control y libertad | Deshacer en edición y borrado; cancelar el escáner guarda lo capturado como borrador |
| 4. Consistencia y estándares | Gestos y hoja de compartir de Android; mismos verbos en toda la app |
| 5. Prevención de errores | Aviso si una página salió borrosa; sugerir Ligero si el PDF supera 20 MB (Gmail adjunta hasta 25 MB) |
| 6. Reconocer antes que recordar | Miniaturas siempre visibles; último destino arriba |
| 7. Flexibilidad y eficiencia | Atajo al escáner, "Aplicar a todas", selección múltiple |
| 8. Estética minimalista | Una acción principal por pantalla; sin banners ni avisos de Pro |
| 9. Recuperarse de errores | Mensajes con causa y salida: "No hay espacio. Libera 50 MB o usa Ligero" |
| 10. Ayuda | Tres consejos breves la primera vez (luz, fondo oscuro, esquinas); ayuda en Ajustes |

- **Gestalt:** proximidad y región común agrupan los filtros en una barra y los destinos en el panel Enviar; similitud: todas las acciones secundarias comparten estilo y solo "Escanear" y "Enviar PDF" llevan el acento; figura y fondo: la hoja escaneada sobre fondo neutro.
- **Ley de Fitts:** Escanear y Enviar son los objetivos más grandes y están abajo, en la zona del pulgar; Eliminar queda lejos de Enviar y pide deshacer, no confirmación.
- **WCAG 2.2 AA (POUR):** contraste de 4,5:1 en texto y 3:1 en íconos; objetivos de 48 dp (más que los 24 px mínimos del criterio 2.5.8); descripciones para TalkBack en cada miniatura ("Página 2 de 3, filtro Documento"); reordenar también con acciones de accesibilidad, no solo arrastrando; texto hasta 200 % sin cortes.
- **ISO 9241-11:** efectividad = 95 % de tareas completadas sin ayuda; eficiencia = menos de 45 s y 3 toques más capturas para 3 hojas; satisfacción = SUS de 80 o más.
- **ISO 9241-110:2020:** sus siete principios de interacción (adecuación a la tarea, autodescripción, conformidad con expectativas, aprendizaje, controlabilidad, robustez ante errores de uso, compromiso del usuario) se revisan en cada pantalla nueva.
- **ISO 9241-210:2019:** proceso iterativo centrado en el usuario: contexto → requisitos → diseño → evaluación con 5 usuarios → repetir en cada fase del roadmap.

## 11. Estándares y calidad del código

Para un proyecto de una persona, la norma marco es [ISO/IEC 29110](https://store.sfs.fi/en/sfs-isoiec-29110-4-1-2020): está pensada para equipos de hasta 25 personas y su perfil Básico toma de ISO/IEC/IEEE 12207 solo dos procesos, gestión del proyecto e implementación. Las demás normas se aplican donde aportan un criterio verificable.

### 11.1 Normas aplicables

| Norma | Qué regula | Cómo se aplica en Folio |
| --- | --- | --- |
| ISO/IEC 29110-4-1 y 29110-5-1-2:2025 | Ciclo de vida para entidades muy pequeñas | Plan de proyecto ligero, registro de cambios, revisión al cerrar cada fase |
| ISO/IEC/IEEE 12207:2017 | Procesos del ciclo de vida del software | Referencia de 29110; gestión de configuración con Git |
| ISO/IEC/IEEE 29148:2018 | Ingeniería de requisitos | Requisitos con ID, verificables y trazados a pruebas (secciones 3 y 4) |
| ISO/IEC 25010:2023 | Modelo de calidad de producto | Requisitos no funcionales (sección 4) |
| [ISO/IEC 5055:2021](https://www.it-cisq.org/standards/code-quality-standards/) | Calidad del código fuente medida por debilidades CWE | Reglas de detekt y Android Lint ligadas a fiabilidad, seguridad, eficiencia y mantenibilidad; la norma es gratuita en CISQ |
| ISO/IEC/IEEE 42010:2022 | Descripción de arquitectura | Diagrama de capas + registros de decisión (ADR) en `docs/adr/` |
| ISO/IEC/IEEE 29119 | Pruebas de software | Plan de pruebas por niveles (11.4) |
| ISO/IEC 14764:2022 | Mantenimiento | Etiquetar cada issue como correctivo, adaptativo, perfectivo o preventivo |
| ISO 9241-11, -110 y -210 | Usabilidad y diseño centrado en el usuario | Sección 10 |
| WCAG 2.2 (W3C; su versión 2.0 es ISO/IEC 40500) | Accesibilidad | Nivel AA (RNF-08) |
| ISO 32000-2:2020 e ISO 19005 | PDF 2.0 y PDF/A | Validación de PDFs en CI (RNF-14) |
| ISO 8601 | Formato de fechas | Nombres de archivo `AAAA-MM-DD` que se ordenan solos |
| OWASP MASVS | Seguridad de apps móviles | Sección 12 (ISO/IEC 27001 aplica a organizaciones, no a una app personal) |

### 11.2 Reglas de código limpio

- **Convenciones oficiales de Kotlin**, aplicadas automáticamente por ktlint; nadie discute formato en una revisión.
- **Funciones pequeñas y con un propósito:** detekt bloquea complejidad ciclomática mayor a 15, funciones de más de 60 líneas y clases de más de 600.
- **Inmutabilidad por defecto:** `val`, `data class`, listas inmutables en el estado de la interfaz.
- **Nulos seguros:** prohibido `!!` (regla de detekt); los casos posibles se modelan con `sealed interface`.
- **Errores como valores:** `Result` o tipos de error propios entre capas; las excepciones no cruzan del dominio a la interfaz.
- **Corrutinas estructuradas:** dispatchers inyectados (así se prueban), nada de `GlobalScope`, cancelación respetada.
- **SOLID en la práctica:** dependencias por constructor; el dominio define interfaces y los datos las implementan.
- **Nombres del dominio:** `Document`, `Page`, `ExportProfile`; nada de `Manager`, `Helper` o `Utils`.
- **KDoc** en toda API pública de `:core:domain` y `:engine:api`.
- **Commits convencionales** (`feat:`, `fix:`, `refactor:`), versionado semántico y `CHANGELOG.md` generado.

### 11.3 Puerta de calidad en CI (GitHub Actions)

Cada pull request debe pasar, en este orden:

1. ktlint y detekt sin alertas.
2. Android Lint sin errores (advertencias registradas en una línea base).
3. Pruebas de arquitectura con Konsist (reglas de dependencia de la sección 6).
4. Pruebas unitarias e integración; cobertura de Kover de 80 % o más en `:core`.
5. Pruebas de capturas de pantalla con Roborazzi (modo claro, oscuro y texto al 200 %).
6. Compilación de release con R8 y verificación de alineación de 16 KB.

En `main` se agregan: pruebas instrumentadas en emulador (Gradle Managed Devices), Macrobenchmark de arranque y publicación firmada en GitHub Releases. Renovate mantiene las dependencias al día con PRs automáticos.

### 11.4 Estrategia de pruebas

| Nivel | Herramientas | Qué cubre | Proporción aproximada |
| --- | --- | --- | --- |
| Unitarias | JUnit, Turbine, kotlinx-coroutines-test, dobles de prueba propios | Casos de uso, ViewModels, reglas de negocio | 70 % |
| Integración | Room en memoria, Robolectric | Repositorios, migraciones, generación de PDF | 20 % |
| Interfaz | Compose UI Test con comprobaciones de accesibilidad, Roborazzi | Flujo principal, estados vacíos y de error | 10 % |
| Rendimiento | Macrobenchmark, Baseline Profiles | RNF-01 y RNF-02 | Por versión |
| Regresión de imagen | Corpus de 30 hojas reales + comparación SSIM | Que un cambio de filtro no empeore la legibilidad | Por cambio en `:engine` |
| Usabilidad | 5 usuarios, tarea cronometrada + SUS | ISO 9241-11 | Por fase |

### 11.5 Definición de terminado

- [ ] El requisito tiene criterio de aceptación y prueba automática que lo cubre.
- [ ] La puerta de calidad de CI está en verde.
- [ ] La pantalla funciona con TalkBack, en modo oscuro y con texto al 200 %.
- [ ] Los textos están en `strings.xml` (español e inglés).
- [ ] Si hubo una decisión de arquitectura, hay un ADR.
- [ ] El CHANGELOG describe el cambio en lenguaje de usuario.

## 12. Seguridad y privacidad

Folio no recolecta ningún dato: no hay cuentas, servidores propios, analítica ni anuncios. La ficha de "Seguridad de los datos" de Google Play puede decir "No se recopilan datos". La guía de verificación es OWASP MASVS.

### 12.1 Permisos mínimos

| Permiso | Para qué | Cuándo se pide |
| --- | --- | --- |
| Ninguno para cámara (motor ML Kit) | ML Kit usa la cámara de Play services | Nunca |
| `CAMERA` | Solo el motor 2 (OpenCV), variante `libre` | Al abrir el escáner por primera vez |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Drive y Modo Escritorio | Se conceden al instalar |
| `ACCESS_LOCAL_NETWORK` | Modo Escritorio cuando la app apunte a Android 17 | Al tocar "Modo Escritorio" |
| `POST_NOTIFICATIONS` | Progreso de OCR y subidas (Android 13+) | La primera vez que se sube a Drive |
| `WRITE_EXTERNAL_STORAGE` con `maxSdkVersion="28"` | Guardar en Descargas en Android 8 y 9 | Al guardar por primera vez |

No se pide acceso a fotos: el selector del sistema entrega solo las imágenes elegidas. Se elimina explícitamente `AD_ID` del manifiesto por si alguna dependencia la añade.

### 12.2 Controles por categoría MASVS

| Categoría | Medida |
| --- | --- |
| STORAGE | Documentos en el almacenamiento privado de la app; excluidos de la copia automática en la nube salvo que el usuario la active; ningún contenido de documentos en los registros |
| CRYPTO | PDF con contraseña usa AES de 256 bits; tokens de sesión con `SecureRandom`; los tokens de Drive los guarda Play services, no la app |
| AUTH | Sin cuentas; bloqueo opcional de la app con huella o rostro (BiometricPrompt, v2) |
| NETWORK | Todo tráfico saliente por HTTPS; `cleartextTrafficPermitted="false"`; el servidor local solo escucha en la interfaz Wi-Fi |
| PLATFORM | Solo se exportan la actividad principal y los filtros de compartir; FileProvider no exportado con permisos por URI; las imágenes recibidas se validan por tipo y tamaño |
| CODE | R8 en release; dependencias vigiladas con Renovate y alertas de GitHub; el servidor local acepta IDs, nunca rutas de archivo |
| PRIVACY | Sin SDK de terceros que recolecten datos; política de privacidad de una página publicada en GitHub Pages |

### 12.3 Datos del usuario

- Borrar desde la papelera elimina fotos originales, caché y PDFs del documento.
- "Borrar todo" en Ajustes deja la app como recién instalada.
- Desinstalar la app borra todo lo que no se haya guardado fuera (Descargas, carpeta elegida o Drive). Se avisa en Ajustes.

## 13. Roadmap y repositorio

Cuatro fases en unas 26 semanas a tiempo parcial; cada una termina en una puerta de paso medible y entrega algo usable. El MVP sirve para el caso del docente desde la semana 8.

&#91;embedded content: roadmap · 4 fases y sus puertas (bandas no a escala)\]

La puerta del MVP es la que importa: si 5 usuarios no envían una prueba de 3 hojas en menos de 45 s, se corrige antes de sumar funciones.

### 13.1 Entregables por fase

**Fase 0 · Fundaciones (semanas 1 y 2)**

- Repositorio con módulos, plugins de convención y catálogo de versiones.
- Puerta de calidad de CI funcionando con una app vacía.
- Tema de diseño (tokens de color, tipografía, espaciado) y 3 componentes base.
- ADR-001 Arquitectura, ADR-002 Motor de escaneo inicial, borrador de política de privacidad.

**Fase 1 · MVP (semanas 3 a 8)**

- RF-01 a RF-12: escanear, editar, filtrar, PDF, compartir, biblioteca.
- RNF-01 a RNF-06 medidos y en meta.
- Publicación en GitHub Releases; se abre la prueba cerrada de Google Play con 12 testers.

**Fase 2 · v1 (semanas 9 a 16)**

- RF-13 a RF-21: OCR, PDF buscable, ZIP, unir y dividir, Google Drive, Modo Escritorio, ajustes finos, atajos, recibir imágenes.
- Publicación en producción de Google Play.

**Fase 3 · v2 (semanas 17 a 26)**

- Motor 2 con CameraX + OpenCV y variante `libre` sin servicios de Google.
- RF-22 a RF-27: PDF/A, contraseña, firma, modo libro, modo cédula, subida desde la PC.
- Publicación en IzzyOnDroid.

### 13.2 Estructura del repositorio

```text
folio/
├── app/                      # punto de entrada, navegación, Hilt
├── build-logic/convention/   # plugins Gradle compartidos
├── core/
│   ├── model/  domain/  data/  database/  storage/
│   ├── designsystem/         # tema, tokens, componentes
│   └── testing/
├── feature/
│   ├── capture/  review/  library/  export/  desktop/  settings/
├── engine/
│   ├── api/  mlkit/  opencv/
├── pdf/   server/   sync/drive/
├── web/                      # fuente de la página del Modo Escritorio
├── testdata/hojas/           # corpus de 30 hojas para regresión
├── docs/
│   ├── adr/  requisitos/  diseno/  privacidad.md
├── gradle/libs.versions.toml
├── .github/workflows/        # ci.yml, release.yml
├── CHANGELOG.md
└── README.md
```

### 13.3 Forma de trabajo

- Iteraciones de 2 semanas con un tablero simple (GitHub Projects): Por hacer, En curso, En revisión, Hecho.
- Una rama corta por requisito, PR revisado contra la definición de terminado (11.5) aunque trabajes solo.
- Al cerrar cada fase: retrospectiva de una página, prueba con 5 usuarios y actualización de este documento.

## 14. Riesgos, supuestos y decisiones abiertas

El riesgo técnico más serio es que el escáner de ML Kit no esté disponible en algunos teléfonos; el riesgo de proyecto es el de todo proyecto personal: que el alcance crezca y nunca se publique.

### 14.1 Riesgos y mitigación

| Riesgo | Mitigación |
| --- | --- |
| ML Kit no funciona en el equipo (sin Play services, menos de 1,7 GB de RAM, teléfonos Huawei recientes) | Camino alterno desde el MVP: foto con la cámara del sistema (sin permiso) + recorte manual de 4 esquinas con `Matrix.setPolyToPoly` de Android, sin OpenCV |
| El primer escaneo espera la descarga del modelo de ML Kit | Pedir la descarga anticipada al abrir la app por primera vez si la API de módulos de Play services lo permite; mostrar progreso, nunca una pantalla congelada |
| La prueba cerrada de Play (12 testers, 14 días) retrasa la publicación | Reclutar compañeros de curso desde la fase 1; mientras tanto, distribuir por GitHub Releases |
| El alcance crece y el MVP no sale | La lista "Won't" (RF-28 a RF-30) y la regla: nada de v1 entra antes de cerrar la puerta del MVP |
| Cambios de políticas de Android y Play (red local en Android 17, tipos de servicio en primer plano, páginas de 16 KB) | Diseño sin servicio en primer plano, permisos contextuales y verificación de 16 KB en CI |
| PDFs pesados o falta de memoria en gama baja | Procesamiento página por página; perfil Ligero sugerido automáticamente |
| Copiar código GPL de otro escáner obliga a licenciar Folio como GPL | Estudiar FairScan y OSS Document Scanner sin copiar; informe de licencias automático con AboutLibraries (Apache 2.0) |
| Modo Escritorio expuesto en una Wi-Fi pública | Token, aprobación en el teléfono, solo lectura y apagado automático (8.3) |

### 14.2 Supuestos

- La mayoría de usuarios tiene Android 8 o superior con Google Play services.
- WhatsApp es el destino principal; el perfil Ligero se calibra para él.
- La app arranca en español e inglés.
- "Folio" es un nombre provisional; hay que verificar que esté libre en Google Play antes de la fase 1.

### 14.3 Decisiones abiertas

- [ ] **Motor del MVP:** ML Kit (recomendado, ahorra 3 a 4 semanas, interfaz de Google) o cámara propia desde el inicio (control total del diseño).
- [ ] **Licencia del código:** Apache 2.0 (cualquiera puede reutilizarlo, incluso en apps cerradas) o GPLv3 (todo derivado debe seguir siendo abierto).
- [ ] **Publicación:** Google Play desde v1 o solo GitHub e IzzyOnDroid.
- [ ] **Subida desde la PC (RF-27):** mantenerla en v2 o descartarla para que el Modo Escritorio sea solo de lectura.
- [ ] **Nombre definitivo** y ícono.

## 15. Fuentes

| Fuente | Qué respalda |
| --- | --- |
| [ML Kit Document Scanner (Google)](https://developers.google.com/ml-kit/vision/doc-scanner/android) | Funciones, modos, RAM mínima de 1,7 GB, salida JPEG y PDF |
| [Android vitals](https://developer.android.com/vitals) | Umbrales de cierres (1,09 %) y ANR (0,47 %) |
| [Permiso de red local (Android)](https://developer.android.com/privacy-and-security/local-network-permission) | Bloqueo de red local para apps que apuntan a Android 17 |
| [Retiro de Microsoft Lens](https://support.microsoft.com/en-gb/topic/retirement-of-microsoft-lens-fc965de7-499d-4d38-aeae-f6e48271652d) | Fechas de retiro y alternativa recomendada |
| [OSS Document Scanner](https://github.com/ossappscollective/OSS-DocumentScanner) | Licencia MIT, NativeScript, OpenCV y Tesseract |
| [FairScan](https://github.com/pynicolas/FairScan) | Escáner libre GPLv3, sin anuncios ni permiso de internet |
| [CamScanner en GetApp](https://www.getapp.com/all-software/a/camscanner/) | Anuncios y marca de agua en la versión gratis |
| [Genius Scan, ayuda](https://help.geniusscan.com/) | Plan básico gratis, sin marca de agua, offline |
| [Comparativa de escáneres gratuitos 2026](https://softpicker.com/how-to-scan-to-pdf-free/) | OCR de Adobe Scan y límites de otros planes gratis |
| [Guía de arquitectura de apps Android](https://developer.android.com/topic/architecture) | Capas y flujo de datos unidireccional |
| [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) | Licencia Apache 2.0 y funciones de PDF |
| [Migración desde Google Sign-In](https://developer.android.com/identity/sign-in/legacy-gsi-migration) | AuthorizationClient para acceder a Drive |
| [Alcances de la Drive API](https://developers.google.com/workspace/drive/api/guides/api-specific-auth) | `drive.file` como alcance no sensible |
| [Tamaños de página de 16 KB](https://developer.android.com/guide/practices/page-sizes) | Requisito para código nativo en Android 15+ |
| [Prueba cerrada de Google Play](https://extendsclass.com/blog/?p=2302) | 12 testers durante 14 días para cuentas personales nuevas |
| [Quick Share para Windows](https://www.android.com/intl/us_en/filetransfer/) | Alternativa de transferencia a PC |
| [Servidor Ktor en Android (ejemplo)](https://github.com/zahidaz/android-http-server) | Viabilidad del servidor embebido con motor CIO |
| [ISO/IEC 25010 en arc42](https://quality.arc42.org/standards/iso-25010) | Las nueve características de la versión 2023 |
| [ISO/IEC 29110-4-1](https://store.sfs.fi/en/sfs-isoiec-29110-4-1-2020) | Perfil Básico para entidades muy pequeñas |
| [ISO/IEC 5055 en CISQ](https://www.it-cisq.org/standards/code-quality-standards/) | Medidas de calidad del código basadas en CWE |
| [ISO 19005-2 (PDF/A-2)](https://www.iso.org/standard/50655.html) | Formato PDF para archivo |
| [Apple WWDC25: diseño con Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/102/) | Principios de jerarquía, armonía y consistencia |
