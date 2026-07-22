# GappleUtils

Librería de utilidades compartidas para los plugins de Paper de GappleClub.
Centraliza el formateo de texto, la paleta de colores de la marca y el parseo
de argumentos de comandos, para no reescribir lo mismo en cada plugin.

- **Java 21** · **Paper 1.21.4+** (compilada contra 1.21.4, compatible con 26.x)
- PlaceholderAPI es **opcional**: si no está instalado, se degrada sin fallar.

---

## Instalación (JitPack)

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.nolberh2</groupId>
        <artifactId>gapple-utils</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.nolberh2:gapple-utils:v1.0.0'
}
```

---

## ⚠️ Hay que shadear la librería (y relocalizarla)

GappleUtils **no se instala como plugin en el servidor**. Cada plugin que la use
tiene que empaquetarla dentro de su propio `.jar`. Si no lo hacés, el plugin
compila bien y luego revienta en runtime con `ClassNotFoundException`.

Y hay que **relocalizar** el paquete. Si dos plugins tuyos incluyen GappleUtils
sin relocalizar, comparten las mismas clases estáticas: el segundo plugin en
cargar pisa los mensajes configurados por el primero.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>com.gappleclub.gappleutils</pattern>
                        <shadedPattern>TU.PAQUETE.libs.gappleutils</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**No shadees Adventure ni MiniMessage.** Paper ya los trae en el classpath del
servidor; incluirlos causa clases duplicadas y errores raros de carga.

---

## Contenido

| Paquete | Clase | Para qué |
|---|---|---|
| `text` | `TextUtils` | Parseo MiniMessage + legacy `&` mezclados, PlaceholderAPI, strip, centrado de chat |
| `color` | `ColorUtils` | Paleta de marca, gradientes multilínea y con fase, mezcla de colores |
| `command` | `CommandUtils` | Parseo de argumentos con errores uniformes, duraciones, tab-complete |

### Paleta

Los colores de marca son los mismos que usa la web de GappleClub, para que el
chat del servidor y la web hablen el mismo idioma.

| Constante | Hex |
|---|---|
| `GOLD` | `#F3C14A` |
| `GOLD_SOFT` | `#E9C877` |
| `GOLD_DEEP` | `#C9962F` |
| `TEXT` | `#F4F5F7` |
| `MUTED` | `#9AA3B2` |
| `MUTED_2` | `#6B7482` |
| `SUCCESS` | `#5FD98A` |
| `ERROR` | `#E5534B` |
| `WARNING` | `#E8A33D` |
| `INFO` | `#6BA8E5` |

### Mensajes de error de comandos

Los `require*` de `CommandUtils` emiten mensajes en castellano por defecto.
Para usar los tuyos (desde el YAML de tu plugin), llamá a `setMessages()` en el
`onEnable()` **y en tu `/reload`**, o quedarán cacheados los viejos.

---

## Publicar una versión nueva

1. Subir la versión en el `pom.xml`.
2. Commit y push a `main`.
3. Crear el tag/release: `gh release create vX.Y.Z`.
4. JitPack compila la primera vez que alguien pide esa versión.
