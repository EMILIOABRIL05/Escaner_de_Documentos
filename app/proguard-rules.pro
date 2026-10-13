# ML Kit (escáner de documentos) crea sus componentes internos por reflexión; R8 no debe quitarlos ni renombrarlos.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_document_scanner.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class com.google.android.datatransport.** { *; }
