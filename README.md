# RAM Saver — mod de Fabric para Minecraft 1.21.1

Mod cliente que vigila el uso de memoria (heap) de Java y, cuando detecta presión
de memoria, baja automáticamente:

- Distancia de renderizado (`viewDistance`)
- Distancia de simulación (`simulationDistance`)
- Escalado de distancia de entidades (`entityDistanceScaling`)
- Modo de partículas (a `MINIMAL`)

Cuando la memoria se recupera, restaura tus valores originales. También puede
sugerir al JVM liberar memoria no usada (`System.gc()`) con un cooldown para no
abusar de esa llamada (puede causar pausas si se invoca muy seguido).

## Comandos en el juego

- `/ramsaver` — muestra el uso de memoria actual y si los ajustes están bajados.
- `/ramsaver gc` — fuerza una sugerencia de recolección de basura.
- `/ramsaver toggle` — activa/desactiva el mod.

## Configuración

Se genera automáticamente en `config/ramsaver.json` la primera vez que cargas
el juego con el mod instalado. Puedes ajustar ahí los umbrales de presión de
memoria, los mínimos de render/simulación, etc.

## Perfil ajustado para equipos de gama baja (4 GB RAM)

Los valores por defecto de este mod ya vienen ajustados pensando en equipos con
4 GB de RAM total (ej. Intel i3-N305): umbral de presión más bajo (70%),
reacción más frecuente (cada 3s) y distancias mínimas de render/simulación más
agresivas (4 chunks).

**Importante:** el mod por sí solo ajusta lo que puede ajustar *en tiempo de
ejecución* (render distance, simulation distance, partículas, GC hints). Pero
con 4 GB de RAM totales, el factor que más impacto real tiene es **cuánta RAM
le asignas al JVM de Minecraft** y **qué más corre en segundo plano**. Recomendaciones:

### 1. Memoria asignada al juego (Xmx/Xms)

Con 4 GB totales, el sistema operativo y el lanzador (Prism Launcher, MultiMC,
etc.) ya consumen entre 1 y 1.5 GB. No le asignes más de **2 GB - 2.5 GB** al
JVM, o el sistema empezará a paginar a disco y todo se sentirá peor, no mejor:

```
-Xms1536M -Xmx2304M
```

### 2. Flags de JVM recomendados (G1GC, tipo "Aikar's flags" simplificados)

Estos flags reducen pausas de GC y son razonablemente livianos en CPU, algo
importante en el i3-N305 porque solo tiene núcleos eficientes (sin núcleos de
alto rendimiento), no de gama alta:

```
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20
```

> Nota: este mod sí llama a `System.gc()` deliberadamente para liberar memoria
> bajo presión sostenida; si usas `-XX:+DisableExplicitGC` esa llamada será
> ignorada por la JVM. Si quieres que el mod pueda forzar GC, no incluyas ese
> flag.

### 3. Mods complementarios (no sustituyen a este, lo complementan)

Para optimización real de uso de RAM y CPU a nivel del propio juego (más allá
de lo que un mod puede hacer ajustando opciones públicas), la comunidad usa:

- **FerriteCore** — reduce el uso de memoria de Minecraft a nivel de motor (muy
  efectivo, cambios reales de cuánta RAM ocupan chunks/bloques en memoria).
- **Lithium** — optimiza lógica de juego (CPU), ayuda mucho en CPUs de núcleos
  eficientes como el N305.
- **Starlight** — reescribe el motor de iluminación, reduce carga de CPU y algo
  de memoria al cargar chunks.

Estos son mods independientes de Fabric/terceros que puedes instalar junto con
RAM Saver sin conflicto.

### 4. Otras cosas a revisar en tu equipo

- Cierra Chrome/Discord/etc. mientras juegas: con 4 GB totales, cada GB que
  consume otra app es un GB menos para Minecraft.
- Revisa que el juego no esté corriendo con resolución 4K/Render Scale alto
  innecesariamente; eso afecta más a GPU que a RAM, pero en un equipo integrado
  (el N305 normalmente usa gráficos integrados) también compite por memoria
  compartida del sistema.



- JDK 21 instalado y configurado (`JAVA_HOME`).
- Conexión a internet (Gradle descargará Minecraft, Yarn mappings, Fabric Loader
  y Fabric API automáticamente la primera vez).
- No necesitas tener Minecraft Java comprado para *compilar* el mod, pero sí para
  ejecutarlo y probarlo con tu cuenta.

## Compilar desde una Chromebook (sin instalar nada) usando GitHub Actions

Si solo tienes una Chromebook, esta es la forma más práctica: GitHub compila
el mod gratis en sus servidores, tú solo usas el navegador.

### Paso 1: Crea una cuenta de GitHub
Ve a [github.com](https://github.com) y crea una cuenta gratuita si no tienes.

### Paso 2: Crea un repositorio nuevo
- Click en el botón verde **"New"** (o el "+" arriba a la derecha → "New repository").
- Ponle un nombre, por ejemplo `ramsaver-mod`.
- Que sea **público** o **privado**, ambos funcionan con GitHub Actions gratis.
- Click en **"Create repository"**.

### Paso 3: Sube los archivos del proyecto
- En la página del repositorio recién creado, busca el link **"uploading an existing file"**.
- Descomprime el zip `ramsaver-fabric-mod.zip` en tu Chromebook (el Files app de ChromeOS puede descomprimir zips con clic derecho → Extraer).
- Arrastra **todo el contenido de la carpeta `ramsaver`** (no la carpeta en sí, sino lo que está adentro: `build.gradle`, `settings.gradle`, `src/`, `.github/`, etc.) a la zona de subida de GitHub.
- Espera a que termine de subir y dale **"Commit changes"**.

> Importante: la carpeta `.github/workflows/build.yml` tiene que quedar en esa
> ruta exacta dentro del repo. Si el drag-and-drop no respeta la estructura de
> carpetas, puedes crear el archivo manualmente desde GitHub: botón "Add file"
> → "Create new file", escribe `.github/workflows/build.yml` como nombre (GitHub
> crea las carpetas automáticamente) y pega el contenido de ese archivo.

### Paso 4: Espera a que compile solo
- Ve a la pestaña **"Actions"** del repositorio (arriba).
- Verás un workflow corriendo llamado "Build RAM Saver mod" (se dispara solo al subir los archivos).
- Tarda unos 2-5 minutos. Cuando el ícono se ponga ✅ verde, terminó.

### Paso 5: Descarga el .jar ya compilado
- Click en el workflow que terminó (✅).
- Abajo, en la sección **"Artifacts"**, verás `ramsaver-jar`.
- Click ahí para descargarlo — te baja un .zip con el `.jar` adentro a tu Chromebook.

### Paso 6: Instala en MJLauncher
- Descomprime ese zip, te queda `ramsaver-1.0.0.jar`.
- En MJLauncher, instala el perfil de **Fabric Loader 1.21.1** si aún no lo tienes (suele tener un instalador integrado o instrucciones en su propia app/Discord).
- Descarga **Fabric API** para 1.21.1 desde [Modrinth](https://modrinth.com/mod/fabric-api) (el navegador de la Chromebook puede descargarlo directo).
- Copia tanto `fabric-api-*.jar` como `ramsaver-1.0.0.jar` a la carpeta `mods` que usa MJLauncher (normalmente accesible desde el explorador de archivos de la app, o vía la app Files de ChromeOS si MJLauncher guarda sus datos en una carpeta visible).

Si en algún paso no encuentras el botón o la carpeta exacta que menciono (la
interfaz de MJLauncher varía bastante), dime exactamente qué ves en pantalla y
te guío con eso.

## Cómo compilar (en una PC normal, no Chromebook)

Desde la carpeta del proyecto (donde está `build.gradle`):

```bash
# Linux / macOS
./gradlew build

# Windows (PowerShell o CMD)
gradlew.bat build
```

> Nota: este proyecto no incluye el wrapper de Gradle (`gradlew`/`gradlew.bat`
> y la carpeta `gradle/`). Si no los tienes, instala Gradle 8.8+ y genera el
> wrapper con:
> ```bash
> gradle wrapper --gradle-version 8.8
> ```
> o simplemente ejecuta `gradle build` directamente si tienes Gradle instalado.

El primer build tardará varios minutos porque descarga y remapea Minecraft.
Builds posteriores son mucho más rápidos.

## Dónde queda el archivo final

Tras compilar, el `.jar` listo para usar aparece en:

```
build/libs/ramsaver-1.0.0.jar
```

Cópialo a la carpeta `mods/` de tu instalación de Fabric (Fabric Loader 0.16.9+
y Fabric API instalados, Minecraft 1.21.1).

## Ideas para extender el mod

- Agregar una pantalla de configuración con Mod Menu + Cloth Config.
- Sumar control sobre el caché de chunks renderizados (requiere mixins más
  específicos, hay que validar nombres exactos de Yarn para 1.21.1 antes de
  escribirlos).
- Registrar en un archivo de log el historial de presión de memoria para
  analizar patrones de uso.
