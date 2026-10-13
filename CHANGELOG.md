# Registro de cambios

Todos los cambios importantes de Folio se anotan aquí, en lenguaje de usuario.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Añadido

- Escanea varias hojas seguidas: el escáner detecta los bordes, recorta y puede disparar solo. Folio no pide permiso de cámara.
- Las páginas se guardan en el teléfono antes de confirmarlas: si la app se cierra, no se pierde ninguna.
- Cada escaneo recibe un nombre automático con la fecha, por ejemplo "Escaneo 2026-10-12 22.30".
- Ajusta a mano las 4 esquinas de una hoja, con una lupa para ser preciso, y deshaz o rehaz cada cambio. La foto original nunca se modifica.
- Con TalkBack, cada esquina se mueve con acciones de accesibilidad, sin necesidad de arrastrar.
- Si el teléfono no tiene el escáner, puedes usar la cámara y recortar la hoja a mano.
- Si algo falla, Folio explica la causa y cómo resolverla, por ejemplo cuando falta espacio o el teléfono no tiene el escáner.
- Diseño propio con tipografía Inter, en modo claro y oscuro, legible con la letra del sistema al 200 % y con TalkBack.
- Respeta el ajuste "Quitar animaciones" del sistema.
- Disponible en español e inglés.

### Seguridad

- Folio bloquea los datos de uso que el escáner de Google intentaba enviar.
- Folio no pide el identificador de publicidad, no envía tus datos a la copia en la nube y no permite conexiones sin cifrar.
