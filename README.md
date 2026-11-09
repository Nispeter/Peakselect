# Peakselect

App Android para limpiar la galería: revisa tus fotos y videos uno por uno y decide qué borrar, conservar o destacar.
Nada cambia hasta que confirmas. Detalle del comportamiento en [SPEC.md](SPEC.md).

## Para usuarios

Necesitas un teléfono con **Android 10 o superior**. La app todavía no está disponible para todos en Google Play, así
que por ahora se instala desde un APK:

| Versión | Descarga | Estado |
|---|---|---|
| Peakselect (prueba) | [Peakselect-prueba.apk](https://github.com/Nispeter/Peakselect/releases/download/peakselect-prueba/Peakselect-prueba.apk) | Versión de prueba, con el nombre y las funciones actuales |
| ImageSorter v1.3.0 | [ImageSorter.apk](https://github.com/Nispeter/Peakselect/releases/latest/download/ImageSorter.apk) | Última versión estable, con el nombre anterior |

1. Descarga el APK desde el teléfono y ábrelo.
2. Si Android lo pide, permite instalar apps desde ese navegador.
3. Abre la app y dale acceso a tus fotos y videos.

Antes de usarla, ten en cuenta:

- **En Android 10 no hay papelera:** lo que confirmes para borrar se elimina para siempre. La app lo avisa antes.
- Peakselect e ImageSorter son apps distintas: se instalan por separado y no comparten las decisiones guardadas.
- Cuando instales la versión de Google Play, primero tendrás que desinstalar la de prueba.

### Cómo se usa

Elige qué carpetas revisar y decide foto por foto, deslizando o tocando. Cuando termines, toca **Confirmar** y confirma
dos veces. En Android 11 o superior lo borrado queda unos 30 días en la papelera y se puede restaurar.

## Para desarrolladores

### Requisitos

- JDK 17 o superior (probado con JDK 23).
- Android SDK con la plataforma 36.
- Un emulador o teléfono con Android 10 o superior.

El wrapper de Gradle descarga la versión correcta; no hace falta instalar Gradle.

### Preparar el proyecto

```sh
git clone https://github.com/Nispeter/Peakselect.git
cd Peakselect
```

Indica dónde está el SDK con la variable `ANDROID_HOME` o con un archivo `local.properties` en la raíz:

```properties
sdk.dir=C\:\\ruta\\al\\Android\\Sdk
```

`scripts/env.ps1` configura el entorno para la sesión de PowerShell, pero sus rutas apuntan a `E:\Android`: ajústalas a
tu equipo antes de usarlo (`. .\scripts\env.ps1`).

### Compilar y ejecutar

En Windows usa `.\gradlew`; en Linux y macOS, `./gradlew`.

```sh
./gradlew installDebug          # compila e instala en el dispositivo conectado
./gradlew assembleDebug         # solo genera app/build/outputs/apk/debug/app-debug.apk
```

Después abre Peakselect en el dispositivo, o abre el proyecto en Android Studio y ejecútalo desde ahí.

### Tests

> **Usa un emulador, no tu teléfono:** los tests instrumentados crean fotos, las mandan a la papelera y las restauran.

```sh
./gradlew testDebugUnitTest          # unitarios, no necesitan dispositivo
./gradlew connectedDebugAndroidTest  # instrumentados, en el emulador conectado
```

Conviene correr los instrumentados en Android 10 y en una versión 11 o superior, porque cada caso cubre un modo de
borrado distinto. Con varios dispositivos conectados, elige uno con `ANDROID_SERIAL`.

Dos pruebas de ciclo de vida se ejecutan desde PowerShell y se niegan a correr fuera de un emulador:

```powershell
.\scripts\test-process-death.ps1     # las decisiones sin confirmar sobreviven a matar la app
.\scripts\test-uninstall-trash.ps1   # desinstalar no borra lo que está en la papelera
```

### Generar la versión de publicación

`./gradlew bundleRelease` requiere la clave de subida a Play, que no está en el repo. El build falla a propósito si
falta o si su huella no coincide con `publishedCertSha256` en `app/build.gradle.kts`. La clave se configura en el
`gradle.properties` del Gradle home:

```properties
imagesorter.keystore=/ruta/a/la/clave.jks
imagesorter.keyAlias=...
imagesorter.keystorePassword=...
imagesorter.keyPassword=...
```

### Estructura

El código está en `app/src/main/java/com/imagesorter/`:

- `ui/`: pantallas en Compose.
- `domain/`: sesión de revisión, planificación y verificación de los cambios.
- `data/`: consultas a MediaStore, base de datos Room y `MediaOps`, que ejecuta las operaciones sobre los archivos.

El paquete sigue siendo `com.imagesorter` a propósito; el `applicationId` publicado es `io.github.nispeter.peakselect`.
