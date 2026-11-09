# Peakselect: spec resumido

App Android para limpiar la galería revisando fotos y videos uno por uno. Funciona sin Internet, sin cuentas y sin
recopilar datos ([política de privacidad](PRIVACY.md)).

**Plataforma:** Android 10 o superior (minSdk 29, targetSdk 36). Kotlin, Jetpack Compose, Room y Coil.

## Flujo

1. El usuario concede acceso a fotos y videos.
2. Elige qué revisar: toda la galería o algunas carpetas.
3. Decide en un mazo, deslizando o tocando: **borrar** o **conservar**. También puede mandar a **Favoritos** o **Liked**.
   "Deshacer" revierte la última decisión.
4. Toca "Confirmar (N)" y confirma dos veces. Recién ahí se aplican los cambios.

## Reglas de seguridad

- Revisar no modifica nada: las decisiones quedan guardadas y sobreviven a que se cierre la app.
- Android 11 o superior: borrar manda a la papelera del sistema (unos 30 días). El borrado definitivo se hace desde la
  pantalla de papelera, con doble confirmación.
- Android 10 no tiene papelera: borrar es definitivo y la app lo avisa antes.
- Después de cada operación se verifica el resultado de cada archivo.
- Archivos en carpetas de otras apps: se copian, se compara el SHA-256 de la copia con el original y solo entonces se
  quita el original. Si la copia falla, el original queda intacto.

## Otras funciones

- Favoritos y Liked viven en `Pictures/Favoritos` y `Pictures/Liked`.
- Las carpetas revisadas se pueden archivar; reaparecen solo si llegan archivos nuevos.
- Ajustes: distancia para deslizar, invertir lados, decidir tocando o solo deslizando, vibración, orden, incluir
  videos, tema y reiniciar revisadas.
