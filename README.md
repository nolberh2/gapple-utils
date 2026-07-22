# GappleUtils

Librería de utilidades compartidas para los plugins de Paper de GappleClub.
Centraliza el formateo de texto, la paleta de colores de la marca y el parseo
de argumentos de comandos, para no reescribir lo mismo en cada plugin.

- **Java 17** · **Paper 1.20.4+** (compilada contra la API más vieja soportada, así corre
  igual en 1.20.x, 1.21.x y 26.x)
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
        <version>v1.0.1</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.nolberh2:gapple-utils:v1.0.1'
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
| `command` | `CommandUtils` | Envoltorio de ejecución con traza completa, y filtro de tab-complete |

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

### Migrar un plugin que trabaja con `String`

Si el plugin todavía usa `player.sendMessage(String)` y códigos `§`, no hace falta
reescribirlo para empezar a aprovechar MiniMessage. `TextUtils.colorize(String)` entra y
sale en `String`:

```java
// antes
public static String color(String text) { /* 60 líneas de regex */ }

// después
public static String color(String text) { return TextUtils.colorize(text); }
```

A partir de ahí, las configs del plugin aceptan MiniMessage y gradientes sin tocar una sola
firma. Para texto nuevo, mejor `parse()` y trabajar con `Component`: `colorize()` pierde por
el camino los gradientes (los aproxima al color más cercano) y los eventos de click/hover,
porque legacy no sabe representarlos.

### Comandos

`CommandUtils` es deliberadamente pequeño: un envoltorio que captura los errores y un
filtro de tab-complete. **No trae validación de argumentos ni mensajes de error propios.**

Llegó a tenerlos —una familia de `require*` con su sistema de mensajes— y no los usó nadie:
cada plugin ya tiene sus textos en el YAML, y unos mensajes fijos en la librería compiten
con eso en vez de ayudar.

Para validar, hazlo en el comando y corta con `CommandException`, que es lo que permite
escribir el cuerpo en línea recta:

```java
public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    return CommandUtils.execute(sender, () -> {
        Player player = requireJugador(sender);   // tu guard, con tus mensajes
        // ... lógica, sin un if de validación por argumento
    });
}

private Player requireJugador(CommandSender sender) {
    if (sender instanceof Player player) return player;
    Mensajes.soloJugadores(sender);          // tu sistema de mensajes
    throw CommandException.silent();          // corta sin enviar nada más
}
```

`execute()` registra cualquier excepción inesperada **con su traza completa** en nivel
`SEVERE` y responde al jugador con un aviso genérico.

---

## Publicar una versión nueva

1. Subir la versión en el `pom.xml`.
2. Commit y push a `main`.
3. Crear el tag/release: `gh release create vX.Y.Z`.
4. JitPack compila la primera vez que alguien pide esa versión.
