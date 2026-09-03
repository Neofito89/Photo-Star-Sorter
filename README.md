# Photo Star Sorter

**Photo Star Sorter** es una aplicación nativa para Android desarrollada en Kotlin y Jetpack Compose con Material Design 3. Diseñada específicamente para fotógrafos que conectan la tarjeta SD de su cámara Canon al teléfono o tableta mediante un adaptador USB-OTG, permitiendo leer las calificaciones por estrellas asignadas en cámara, filtrar fotos, seleccionarlas de forma individual o por rangos, y copiarlas, compartirlas o eliminarlas.

---

## 🌟 Características Principales

1. **Apertura y Persistencia mediante Storage Access Framework (SAF):**
   - Al iniciar, abre directamente el selector de carpetas de Android (`ACTION_OPEN_DOCUMENT_TREE`) para elegir la tarjeta SD conectada o la carpeta `DCIM/100CANON`.
   - Persiste los permisos de URI (`takePersistableUriPermission`) para reabrir automáticamente la tarjeta en sesiones futuras sin pedir permisos nuevamente.
   - Cumple estrictamente con el almacenamiento con ámbito (Scoped Storage) de Android 10+ sin requerir el permiso invasivo `MANAGE_EXTERNAL_STORAGE`.

2. **Extracción y Clasificación por Estrellas:**
   - Lee prioritariamente las calificaciones Adobe XMP (`xmp:Rating`, `Rating`, `RatingPercent`) de 0 a 5 estrellas grabadas por cámaras Canon.
   - Soporte para formatos de imagen estándar: **JPEG, HEIC, TIFF y DNG**.
   - Soporte para archivos Canon RAW: **CR2 y CR3**.
   - Muestra la calificación de forma prominente como número (0 a 5) y estrellas visuales doradas.

3. **Filtrado y Ordenamiento Global:**
   - Filtro horizontal rápido: **Todas**, **5 ★**, **4 ★**, **3 ★**, **2 ★**, **1 ★**, **0 ★ (Sin calificar)** y **No disponible**.
   - El filtro se aplica sobre el total del escaneo, no sólo sobre los elementos visibles.
   - Ordenación interactiva pulsando en el encabezado de las columnas (por Calificación, Nombre, Fecha o Tamaño).

4. **Selección Flexible y Acciones en Bloque:**
   - Selección de archivos individuales mediante casillas de verificación.
   - Botón de **"Seleccionar todo el resultado filtrado"** con un solo toque.
   - **Selección por rango:** Marca el primer y último archivo para seleccionar automáticamente todas las fotos intermedias en el orden actual.
   - Barra contextual inferior con acciones inmediatas:
     - **Copiar a almacenamiento interno:** Selector de carpeta de destino con barra de progreso en segundo plano, protección contra sobreescritura (renombrado seguro con sufijo numérico) y resumen final de resultados.
     - **Compartir:** Utiliza la hoja nativa de Android (`ACTION_SEND_MULTIPLE`) preservando los tipos MIME y otorgando permisos de lectura seguros.
     - **Eliminar de la tarjeta SD:** Diálogo de confirmación explícito con conteo de archivos, eliminación segura con permisos SAF y resumen de errores individuales.

5. **Columnas Configurables:**
   - Vista por defecto ultra limpia: **Nombre del archivo** y **Calificación**.
   - Menú de columnas opcional para activar: **Fecha de captura**, **Tamaño de archivo**, **Tipo/Extensión** y **Ruta relativa**.

6. **Rendimiento y Caché Local con Room:**
   - Base de datos SQLite local (Room) que almacena en caché los metadatos indexados por `(uri, lastModified, fileSize)`.
   - Escaneo progresivo en segundo plano que mantiene la interfaz rápida y reactiva incluso con miles de fotos en la tarjeta.

---

## 📷 Compatibilidad y Limitaciones de Canon RAW (CR2 y CR3) en Android

### Formato Canon CR2 (RAW basado en TIFF)
- **Soporte completo:** El formato CR2 utiliza una estructura TIFF estándar de 32 bits. Canon almacena las calificaciones en la etiqueta de metadatos Adobe XMP (tag 700 / `0x02BC`) en el IFD0 y en etiquetas Exif de calificación.
- **Implementación:** La aplicación examina los encabezados TIFF y extrae el paquete XMP directamente del flujo de bytes sin requerir códecs nativos pesados.

### Formato Canon CR3 (RAW basado en ISO-BMFF / MP4)
- **Estructura:** Introducido a partir de cámaras como la Canon EOS M50, EOS R, R5, R6, 90D, etc. Los archivos `.CR3` son contenedores MPEG-4 / ISO Base Media File Format (`ftyp crx `).
- **Soporte de metadatos:**
  - **Soportado plenamente cuando incluye paquete XMP estándar:** La cámara Canon suele incrustar una caja `uuid` de nivel raíz o dentro del contenedor `moov` con el identificador único universal de Adobe (`BE 7A CF CB 97 A9 42 E8 9C 71 99 94 91 E3 AF AC`). `Cr3BoxParser` localiza esta caja en streaming y extrae la calificación `xmp:Rating`.
  - **Fallback elegante ("Rating no disponible"):** Ciertas versiones de firmware o perfiles de compresión de Canon (C-RAW) almacenan información de disparo en bloques propietarios binarios (`CMT1`, `PRVW`) donde la calificación no se expone en un XML XMP estándar abierto.
  - **Comportamiento ante metadatos no legibles:** Para evitar conjeturas o falsos positivos que arruinen la selección del fotógrafo, la aplicación clasifica estos archivos bajo el estado **"Rating no disponible"** en lugar de asignarles 0 estrellas de manera errónea.
- **Arquitectura desacoplada:** La extracción está encapsulada tras la interfaz `MetadataExtractor`. Esto permite incorporar parsers adicionales o librerías C++ nativas (como LibRaw o Exiv2) a futuro sin modificar la interfaz de usuario ni la base de datos.

---

## 🛠️ Instrucciones de Configuración y Compilación

### Requisitos del Sistema
- **Android Studio:** Ladybug (2024.2.1) o superior.
- **JDK:** Java 11 o Java 17.
- **SDK de Android:** `compileSdk 36`, `minSdk 24` (Android 7.0+; optimizado para Android 10+).

### Dependencias Utilizadas
- **Jetpack Compose & Material 3:** Interfaz declarativa, temas dinámicos e iconos Material Symbols.
- **AndroidX Room (`room-runtime`, `room-ktx`, `room-compiler` con KSP):** Caché local de metadatos.
- **AndroidX DocumentFile (`documentfile`):** Acceso seguro y recursivo a tarjetas SD por USB-OTG mediante SAF.
- **AndroidX ExifInterface (`exifinterface`):** Decodificación de metadatos EXIF de alta fidelidad.
- **Kotlin Coroutines & Flow:** Operaciones de I/O, escaneo y copia fuera del hilo principal (`Dispatchers.IO`).
- **JUnit 4, Robolectric & Roborazzi:** Suite de pruebas unitarias y de captura de pantalla.

### Pasos para Ejecutar
1. Clona o abre el proyecto en Android Studio.
2. Deja que Gradle sincronice las dependencias del catálogo `gradle/libs.versions.toml`.
3. Conecta tu teléfono o tableta Android.
4. Conecta el lector de tarjetas SD mediante un cable o adaptador USB-OTG.
5. Ejecuta la aplicación (`Run 'app'`).
6. En el selector de carpetas SAF que se abre en el primer inicio, selecciona tu tarjeta SD (por ejemplo, el volumen USB o la carpeta `DCIM`).
7. ¡Ordena y filtra tus fotos por estrellas al instante!

---

## 🧪 Pruebas Unitarias

El proyecto incluye tests unitarios automatizados que verifican:
- `XmpRatingParserTest`: Comprueba el parseo de etiquetas `<xmp:Rating>`, `<Rating>`, atributos `xmp:Rating="5"`, mapeo de porcentajes `<RatingPercent>` a 1-5 estrellas, extracción de fechas y búsqueda de paquetes XMP en streams.
- `Cr3BoxParserTest`: Valida el escaneo de cajas ISO-BMFF sintéticas de Canon CR3 con cajas `uuid` de Adobe XMP.
- `FilteringAndSortingTest`: Comprueba el filtrado exacto por estrellas (0 a 5), filtrado de no disponibles, ordenamiento ascendente y descendente por calificación, nombre, fecha y tamaño, así como la selección por rango continuo (directa e inversa).
- `ExampleRobolectricTest`: Verifica la carga de recursos y configuración de la aplicación en el entorno local de JVM.
- `GreetingScreenshotTest`: Verifica la renderización de componentes visuales con Roborazzi.

Para ejecutar los tests desde la terminal:
```bash
gradle :app:testDebugUnitTest
```
