# AGENTS.md

## Propósito
Proyecto universitario (Java) de un monedero virtual con sistema de puntos.
Quiero LEER y ENTENDER el código. **Responde siempre en español.**

## Regla de inicio (obligatoria)
- Al empezar **cada sesión** o antes de **cualquier cambio**, lee
  `resumenes/proyecto-final-estructuras-de-datos.md`.
- Úsalo como contexto, pero contrasta siempre con el código real y con
  `git status` / `git diff`: el repositorio es la fuente de verdad.
- Si un cambio de código afecta las guías de `ilustraciones/` (HTML/CSS), actualízalas
  después de cada `git pull` o `git push`.

## Estado real del repo (verificado)
- Java 21 + Maven. Dependencias: `javafx-controls` 21.0.5, `junit-jupiter` 5.10.2 e
  Ikonli 12.4.0 (`ikonli-javafx` + `ikonli-feather-pack`, iconos Feather).
- **La UI JavaFX ya existe**: `interfaz.Main` extiende `Application` y es el `mainClass`
  del `pom.xml`; `interfaz.Launcher` solo delega en `Main`. No cambies el entrypoint
  ni el `pom.xml` sin pedirlo.
- `mvn javafx:run` abre la app; `mvn test` corre las pruebas JUnit 5 (~96; hoy falla 1
  preexistente: `ListaCircularTest.eliminarActualAvanzaAlSiguiente`, bug de
  `ListaCircular.eliminar` cuando el cursor nunca avanzó).
  No ejecutes compilaciones ni pruebas salvo que se pida.
- No hay `docs/` ni README; la documentación vive en `resumenes/` e `ilustraciones/`.

## Cómo ejecutar
- UI: `mvn javafx:run` (no usar `java -cp target/classes`: JavaFX exige módulos).
- Todas las pruebas: `mvn test`.
- Una clase: `mvn -Dtest=ServicioPuntosTest test`.
- Un método: `mvn -Dtest=ServicioPuntosTest#calcularPuntosPorTipoYDivisionEntera test`.
- Login con **ID + contraseña** (dos momentos en `LoginView`: "Acceder" y "Crear
  cuenta"). Las cuentas demo (`ana`, `bruno`, `carla`) usan la contraseña `demo`;
  cualquier usuario nuevo se crea desde la vista y aparece como chip en el login.
- **Persistencia sin base de datos** (extra aceptado, no es requerimiento):
  `Main.crearBanco()` carga el registro `monedero.log` (en la raíz del proyecto,
  ignorado por git) si existe y lo reproduce; si no, precarga la demo. Las
  contraseñas se guardan como hash SHA-256 con sal en el propio log.

## Arquitectura (lo no obvio)
- Paquetes de primer nivel: `estructuras`, `modelo` (+ `modelo.tipo` con los 8 enums),
  `servicio` (+ `servicio.dto` con los DTOs), `persistencia` (extra, sin base de datos)
  e `interfaz` (con `interfaz.vistas` para las pantallas).
- `estructuras/` tiene 8 TADs propios: `ListaEnlazada`, `TablaHash`, `Arbol` (BST),
  `ArbolAVL`, `Pila`, `ColaPrioridad`, `ListaCircular`, `GrafoDirigido`.
  **Prohibido sustituirlos por `java.util`** (solo se permite `Iterator`).
- Ya están conectados a `servicio`: `ListaEnlazada` (historiales), `TablaHash` (clientes,
  cuentas, transacciones), `ArbolAVL` (ranking), `Pila` (reversión), `ColaPrioridad`
  (programadas), `ListaCircular` (notificaciones) y `GrafoDirigido` (reportes y
  monederos). `Arbol` (BST) sigue existiendo con pruebas, pero el ranking usa AVL.
- `servicio.Banco` es la fachada; `ServicioPuntos` usa `TablaHash` (cuentas) +
  `ArbolAVL` (ranking); `ServicioNotificaciones`, `ServicioReportes` y
  `ServicioIntegridad` cubren las funciones avanzadas; `Comisiones` centraliza tasas;
  `Reloj` permite avanzar tiempo simulado (`avanzarDias`/`avanzarHoras`) para probar
  vencimientos y programadas.
- Dinero con `BigDecimal`; puntos = división entera por cada 100 según
  `TipoTransaccion` (enum en `modelo.tipo`; transferencia recibida y bono no otorgan
  puntos).
- UI: `Main` carga en orden `base.css`, `components.css`, `login.css`, `pass-card.css`,
  `dashboard.css` y `views.css` desde `src/main/resources/interfaz/estilos/`; si mueves
  estilos, ajusta `Main.ESTILOS`. `DashboardView` (en `interfaz`) es el shell con
  navegación; cada pantalla implementa `Vista` y vive en `interfaz.vistas`
  (`ResumenView`, `MovimientosView`, `OperacionesView`, `PuntosView`, `RankingView`,
  `ReversionView`, `MonederosView`, `ProgramadasView`, `NotificacionesView`,
  `ReportesView`, `IntegridadView`). Las vistas de consulta usan DTOs de
  `servicio.dto` y `MovimientoCell` pinta `MovimientoDto` sin consultar `Banco`.
  `LoginView`, `PassCard`, `RangeLadder` y `Ui` (apoyo visual) viven en `interfaz`.
- **Persistencia (`persistencia/`, extra sin BD)**: `RegistroOperaciones` guarda cada
  operación como una línea de `monedero.log` y al arrancar la reproduce con los
  métodos públicos de `Banco` (los hooks escribieron las líneas). `Credenciales`
  maneja el hash de contraseñas. Los ids de transacciones/programadas/avisos vienen
  de contadores estáticos en `modelo`; el replay asume un `Banco` recién creado
  (en pruebas se reinician por reflexión).
- Las guías de `ilustraciones/` son HTML/CSS estáticos escritos a mano; sus cifras
  (nº de pruebas/TADs) pueden estar desactualizadas: verifica antes de repetirlas.

## Modo de trabajo
- Por defecto, NO edites, crees, borres ni muevas archivos del proyecto: explica en texto
  lo que debería cambiarse. Sí puedes hacerlo cuando se pida explícitamente.
- Puedes crear archivos en `ilustraciones/` (scripts de Python e imágenes),
  `resumenes/` (Markdown) y `src/test/java/` (pruebas) cuando corresponda.
- Git de solo lectura: `git status`, `git log`, `git diff`, `git fetch`.
- `git commit` / `git push` solo si se piden; si el push se rechaza por cambios remotos,
  avisa antes de merge/rebase. No fuerces el push.

## Cómo leer (ahorrar contexto)
- Lista/busca carpetas antes de abrir archivos; abre solo lo necesario.
- No releas archivos ya leídos en la sesión.
- Los requerimientos están en `resumenes/proyecto-final-estructuras-de-datos.md`;
  verifica contra el código y no inventes requerimientos.
- Respuestas breves; extiéndete solo si se pide.

## Cuando diga "hice pull" o "hice push"
1. Mira qué cambió con `git log` y `git diff` contra el estado anterior.
2. Explica en pocas líneas: archivos cambiados, métodos agregados/modificados/borrados
   y con qué clases se relacionan.
3. Indica qué requerimiento toca cada cambio.

## Al explicar código
- Di qué requerimiento cubre cada clase/método y con qué otros se relaciona.
- Marca lo que no corresponda a ningún requerimiento.

## Revisión de calidad
Avisa (`archivo:línea`) si ves métodos de más de ~40 líneas, código ajeno a los
requerimientos, código repetido o comentado, o nombres poco claros. No lo corrijas.

## Advertencias
- No leas ni muestres claves, contraseñas ni configuración privada (incluido `.idea/`).
- No inventes; si no estás seguro, dilo.
- No asumas que el código funciona: verifica con `mvn test` cuando se pida y di qué
  no pudiste verificar.


## UI
Lee DESIGN.md antes de cualquier cambio visual.
Mobile-first: escribe estilos base para 320px, amplía con min-width.
Verifica ARIA y contraste en cada componente nuevo.