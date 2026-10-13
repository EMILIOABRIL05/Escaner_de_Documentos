# Política de privacidad de Folio (borrador)

Última actualización: 2026-10-12 · Estado: borrador, se publicará en GitHub Pages antes de la primera versión.

## En resumen

Folio no recopila ningún dato. No tiene cuentas, servidores propios, analítica ni publicidad.

## Qué hace Folio con tus documentos

- **Fotos, documentos y PDFs:** se procesan y se guardan solo en tu teléfono, en el almacenamiento privado de la app.
- **Copia en la nube:** Folio no los sube a ninguna nube propia. Además, quedan fuera de la copia de seguridad automática de Android.
- **Cuándo salen del teléfono:** solo cuando tú lo decides:
  - al compartir con la hoja de compartir de Android (WhatsApp, Gmail, Quick Share…);
  - al guardar en una carpeta que elijas;
  - más adelante, al subir a tu Google Drive.

  A partir de ahí, se aplica la política de privacidad del servicio que elegiste.
- **Detección de bordes y texto:** el reconocimiento de bordes y, más adelante, de texto (OCR) se hace en el teléfono con ML Kit de Google, sin enviar tus documentos a internet.

## Permisos

| Permiso | Para qué | Cuándo |
| --- | --- | --- |
| Ninguno de cámara | El escáner usa la cámara de Google Play services | Nunca se pide |
| Fotos | No se pide: el selector de fotos del sistema solo entrega las imágenes que eliges | Nunca se pide |
| Internet y estado de la red | Los declara ML Kit de Google, que hace el escaneo en el teléfono; más adelante también Google Drive y Modo Escritorio. Folio no envía tus documentos a internet | Se concede al instalar |
| Almacenamiento (Android 8 y 9) | Guardar en Descargas | La primera vez que guardas |

Folio elimina el permiso de identificador de publicidad (`AD_ID`) aunque alguna biblioteca lo pida. También desactiva el envío de datos de uso que trae ML Kit de Google: esos registros no tienen a dónde salir del teléfono.

## Borrar tus datos

- **Papelera:** borrar un documento lo envía a la papelera durante 30 días. Al vaciarla se eliminan las fotos, la caché y los PDFs de ese documento.
- **"Borrar todo"** en Ajustes deja la app como recién instalada.
- **Desinstalar:** desinstalar Folio borra todo lo que no hayas guardado fuera de la app.

## Contacto

Puedes escribir a través de los issues del repositorio del proyecto en GitHub.
