# AGENTS.md

## Propósito
Proyecto universitario (Java) de un monedero virtual, empezado por mi compañero.
Quiero LEER y ENTENDER lo que hizo. **Responde siempre en español.**

## Regla de inicio (obligatoria)
- Al empezar **cada sesión** o antes de **cualquier cambio**, lee
  `resumenes/proyecto-final-estructuras-de-datos.md`.
- Úsalo como contexto del proyecto, pero contrasta siempre con el código real y con
  `git status` / `git diff`: el repositorio es la fuente de verdad.
- Toda explicación, revisión o propuesta debe basarse en ese resumen y en los cambios
  reales del proyecto, no en suposiciones.
- Si es necesario cambiar todos los archivos HTML/CSS de ilustraciones por un cambio nuevo,
  hazlo después de cada `git pull` o `git push`.

## Estado real del repo (verificado)
- Java 21 + Maven (`pom.xml`). Dependencias: `javafx-controls` 21.0.5 (aún sin usar) y
  `junit-jupiter` 5.10.2 (solo test).
- **No hay UI todavía**: no hay `Launcher`, FXML ni `src/main/resources`.
  El paquete `interfaz` solo contiene `Main` (demo de consola). No escribas código JavaFX
  hasta que te lo pida.
- **Hay pruebas** JUnit 5 en `src/test/java` (`mvn test`). No hay README ni carpeta `docs/`.
- El entrypoint real es `interfaz.Main` (demo de consola de Fases 1 y 2).
- `pom.xml` declara `mainClass = interfaz.Main`: `mvn javafx:run` ejecuta la demo
  (Main no usa JavaFX). También: `mvn compile` y luego
  `java -cp target/classes interfaz.Main`.
  No corrijas el `pom.xml` sin pedirlo.

## Estructura de paquetes (no obvia)
Solo `Main` vive en `interfaz`; el resto son paquetes de primer nivel:
- `estructuras/`: TADs propios (ListaEnlazada, TablaHash, Arbol BST).
  **Sin colecciones de `java.util`** para las estructuras (solo `Iterator`/`NoSuchElementException`).
  No las reemplaces por `ArrayList`/`HashMap`; es el núcleo del proyecto.
- `modelo/`: dominio (Cliente, Transaccion, CuentaPuntos, Beneficio, enums…).
- `servicio/`: lógica de negocio. `Banco` es la fachada; `ServicioPuntos` (ranking con TablaHash+Árbol),
  `Comisiones` (tasas centralizadas), `Reloj` (tiempo simulado: `avanzarDias` para probar vencimientos),
  `OperacionInvalidaException`.
- Dinero con `BigDecimal`; puntos = división entera por cada 100 según tipo de transacción.

## Modo de trabajo
- Por defecto, NO edites, crees, borres ni muevas archivos del proyecto: explica en texto
  lo que debería cambiarse. Sí puedes hacerlo cuando te lo pida explícitamente.
- Puedes crear archivos en `ilustraciones/` (scripts de Python e imágenes),
  `resumenes/` (Markdown) y `src/test/java/` (pruebas) cuando corresponda.
- Comandos de git de lectura: `git status`, `git log`, `git diff`, `git fetch`.
- `git commit` / `git push` solo si te lo pido; si el push se rechaza por cambios remotos,
  avísame antes de hacer merge/rebase. No fuerces el push.
- Ejecuta compilaciones o `mvn test` solo cuando te lo pida.

## Cómo leer (ahorrar contexto)
- Lista/busca carpetas antes de abrir archivos; abre solo lo necesario.
- No releas archivos ya leídos en la sesión.
- Los requerimientos están resumidos en `resumenes/proyecto-final-estructuras-de-datos.md`;
  úsalo como punto de partida, pero verifica contra el código. No inventes requerimientos.
- Respuestas breves; extiéndete solo si te lo pido.

## Cuando te diga "hice pull" o "hice push"
1. Mira qué cambió con `git log` y `git diff` (contra el estado anterior).
2. Explica en pocas líneas: archivos cambiados, métodos agregados/modificados/borrados y con qué clases se relacionan.
3. Indica qué requerimiento toca cada cambio.

## Al explicar código
- Di qué requerimiento cubre cada clase/método y con qué otros se relaciona.
- Marca lo que no corresponda a ningún requerimiento.

## Revisión de calidad
Avísame (archivo:línea) si ves métodos de más de ~40 líneas, código ajeno a los
requerimientos, código repetido o comentado, o nombres poco claros. No lo corrijas.

## Advertencias
- No leas ni muestres claves, contraseñas ni configuración privada.
- No inventes; si no estás seguro, dilo.
- No asumas que el código funciona: verifica con `mvn test` cuando se te pida y di qué
  no pudiste verificar.
