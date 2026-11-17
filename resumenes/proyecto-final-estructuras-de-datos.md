# Proyecto Final: Simulador de un "Monedero Virtual con sistema de Puntos"

**Objetivo:** Desarrollar una aplicación (con interfaz grafica) que simule un monedero virtual avanzado, permitiendo realizar transacciones entre clientes como depósitos, retiros, transferencias, consultas, reportes, gastos, además de incorporar funcionalidades complejas como transacciones programadas, análisis de patrones de gasto, sistema de puntos por transacciones y optimización de algoritmos según el tipo de estructura de datos utilizada.

## Descripción

El sistema debe permitir la gestión de cuentas virtuales de los clientes, donde cada cliente puede realizar diversas transacciones y consultar distintos tipos de información relacionada con sus cuentas y gastos. Además de las funcionalidades básicas, el sistema debe incluir un componente de gamificación con un sistema de puntos, incentivando a los clientes a usar el monedero virtual.

## Funcionalidades Básicas

1. **Depósitos:** Los clientes pueden agregar dinero a su cuenta.
2. **Retiros:** Los clientes pueden retirar dinero de su cuenta, siempre que tengan saldo suficiente.
3. **Transferencias:** Los clientes pueden transferir dinero entre diferentes cuentas de otros clientes.
4. **Consultas de Saldo:** Los clientes pueden consultar su saldo actual.
5. **Consultas de Historial de Transacciones:** Los clientes pueden ver todas las transacciones realizadas.

## Funcionalidades Avanzadas

1. **Sistema de Puntos por Transacciones:**
   - Cada transacción realizada por un cliente (depósito, retiro, transferencia) genera una cantidad específica de puntos dependiendo del tipo y el monto de la transacción. Por ejemplo:
     - Depósitos: 1 punto por cada 100 unidades de moneda depositadas.
     - Retiros: 2 puntos por cada 100 unidades retiradas.
     - Transferencias: 3 puntos por cada 100 unidades transferidas.
   - Los puntos acumulados pueden ser canjeados por beneficios como descuentos en transacciones futuras o privilegios adicionales en el sistema.
   - Implementa una estructura eficiente como árboles binarios de búsqueda para gestionar los puntos acumulados por cada cliente.

2. **Rangos de Clientes:**
   - Con base en los puntos acumulados, los clientes serán clasificados en diferentes rangos (por ejemplo, Bronce, Plata, Oro, Platino). Cada rango puede tener beneficios adicionales, como tarifas reducidas para transferencias o bonos de puntos adicionales por ciertas transacciones.
   - Usa árboles balanceados para mantener actualizados los rangos de los clientes en función de sus puntos acumulados.

3. **Transacciones Programadas:**
   - Los clientes pueden programar transacciones recurrentes (depósitos automáticos, transferencias periódicas).
   - Usa colas de prioridad para procesar estas transacciones de manera ordenada y eficiente según la fecha de ejecución.

4. **Análisis de Patrones de Gasto:**
   - Genera informes avanzados sobre los patrones de gasto de los clientes. Usa grafos dirigidos para analizar las relaciones entre categorías de gasto y transferencias entre clientes.

5. **Alertas y Notificaciones:**
   - Envía notificaciones automáticas al cliente, como alertas de saldo bajo o recordatorios de transacciones programadas.
   - Usa listas circulares para gestionar las notificaciones de manera eficiente.

6. **Multimonedero:**
   - Los clientes pueden manejar múltiples monederos dentro de su cuenta (uno para ahorros, otro para gastos diarios).
   - Utiliza grafos dirigidos para representar las relaciones entre los monederos y las transacciones realizadas entre ellos.

7. **Verificación de Integridad de las Transacciones:**
   - Garantiza la validez de todas las transacciones realizadas utilizando algoritmos recursivos.

8. **Interfaz de Usuario Avanzada:**
   - Proporciona una interfaz para realizar transacciones, consultar puntos acumulados, ver rangos, y canjear beneficios.

## Requisitos del Sistema de Puntos

1. **Acumulación de Puntos:**
   - Cada cliente tiene un registro de los puntos acumulados y un historial de transacciones relacionadas con estos puntos.
   - Los puntos deben ser recalculados dinámicamente en caso de que se revierta alguna transacción.

2. **Canje de Puntos:**
   - Los clientes pueden canjear puntos por beneficios específicos. Ejemplo:
     - 100 puntos: Reducción del 10% en la comisión por transferencias.
     - 500 puntos: Un mes sin cargos por retiros.
     - 1000 puntos: Bono de saldo de 50 unidades en el monedero.

3. **Clasificación de Rangos:**
   - Implementar una función que calcule el rango de cada cliente con base en sus puntos acumulados:
     - Bronce: 0–500 puntos.
     - Plata: 501–1000 puntos.
     - Oro: 1001–5000 puntos.
     - Platino: Más de 5000 puntos.

## Requisitos del Proyecto

- **Estructuras de Datos:**
  - Listas: Para gestionar las transacciones por cliente.
  - Pilas: Para manejar transacciones reversibles.
  - Colas: Para gestionar las transacciones programadas y las notificaciones.
  - Árboles: Para almacenar puntos acumulados y rangos de los clientes.
  - Grafos: Para analizar transferencias entre clientes y patrones de gasto.
  - Tablas Hash: Para acceder rápidamente a los datos de los puntos acumulados por cliente.

- **Optimización de Algoritmos:**
  - Usa algoritmos eficientes para calcular y actualizar puntos, manejar rangos, y generar reportes.

## Informe Final

Incluye un análisis técnico detallado de cómo se implementaron las estructuras de datos y los algoritmos optimizados, además de ejemplos prácticos del sistema de puntos y su impacto en el uso del monedero virtual.

---

## Extra implementado: persistencia sin base de datos + login con contraseña

Esto **no es un requerimiento del enunciado**: es un agregado aceptado por el usuario. No sustituye ningún TAD propio y no añade dependencias (I/O de archivos y `MessageDigest` son Java estándar).

### Paquete `persistencia` (nuevo, primer nivel)

- **`RegistroOperaciones`** — Log de operaciones en `monedero.log` (texto plano, una línea por operación, campos separados por `;`) + **replay**: `repetir(banco)` lee el archivo, lo trunca y vuelve a ejecutar cada línea con los métodos públicos de `Banco`, de modo que el estado derivado (puntos, ranking AVL, grafo de reportes, historiales y avisos) se reconstruye solo. El propio replay re-escribe las líneas (archivo auto-reparable).
- **`Credenciales`** — Contraseñas como hash SHA-256 con sal aleatoria por usuario (`MessageDigest.isEqual` para comparar en tiempo constante). Nunca se guarda texto plano.

### Formato del log

`CREAR;id;nombre;sal:hash` · `DEPOSITO;id;monedero;monto;fecha` · `RETIRO;...` · `TRANSFERENCIA;origen;monO;destino;monD;monto;fecha` · `MOVIMIENTO;id;monO;monD;monto;fecha` · `MONEDERO;id;idM;nombre;tipo` · `CANJE;id;beneficio;fecha` · `PROGRAMAR;tipo;id;destino;monto;fecha;rec;ahora` · `CANCELAR_PROGRAMADA;id` · `EJECUTADA;id` · `FALLIDA;id;fechaAviso;mensaje` · `REPROGRAMAR;id;fecha` · `REVERSION;idCliente;idT;fecha` · `LEIDA;idAviso` · `UMBRAL;monto`.

### Cambios en `servicio`

- `Banco` tiene dos constructores: `Banco()` sin persistencia (pruebas) y `Banco(RegistroOperaciones)`. Cada método que muta estado escribe una línea vía el helper privado `persistir`; las operaciones que fallan no dejan línea.
- `registrarCliente(id, nombre, password)`, `autenticar(id, password)` y tres restauradores de replay: `restaurarCredencial`, `restaurarFallaProgramada`, `reprogramarProgramada` (no re-inserta en la `ColaPrioridad`; reordena el montículo vaciándola y re-insertando), `marcarProgramadaEjecutada`.
- `Reloj.establecer(LocalDateTime)` permite poner el reloj simulado en la fecha histórica de cada línea para conservar fechas y comisiones.

### Arranque e interfaz

- `Main.crearBanco()`: si `monedero.log` existe → replay; si no → carga demo (ana/bruno/carla, contraseña `demo`). El archivo de datos está en `.gitignore`.
- `LoginView` con dos momentos: **Acceder** (ID + contraseña) y **Crear cuenta** (ID + nombre + contraseña); las chips demo rellenan ID y contraseña `demo`.

### Pruebas

- `persistencia.RegistroOperacionesTest` (7 casos, `@TempDir`): replay de saldos/historiales/puntos, contraseña sin texto plano y re-alineación de ids (contadores estáticos de `modelo` reiniciados por reflexión para simular una JVM nueva), reversiones, programada ejecutada no se duplica / cancelada no se ejecuta, umbral + aviso leído, operaciones fallidas no registradas.

### Limitaciones conocidas

- El replay asume un `Banco` recién creado (en la app real es lo primero que ocurre al arrancar); los ids de transacciones/programadas/avisos provienen de contadores estáticos y se alinean solo si el orden se conserva.
- El cursor circular de `siguiente()` no se persiste: los avisos arrancan de nuevo al abrir la app.
- **Fallo preexistente** (sin relación con este módulo): `estructuras.ListaCircular.eliminar` no desengancha el nodo cuando el cursor nunca avanzó (`previoActual == actual`, línea 67) — lo detecta `ListaCircularTest.eliminarActualAvanzaAlSiguiente:44`.

## Organización de carpetas (refactor de estructura, sin cambios de comportamiento)

- `interfaz/` queda con el arranque y el soporte visual: `Launcher`, `Main`, `Ui`, `PassCard`, `RangeLadder`, `LoginView`, `DashboardView`.
- **`interfaz/vistas/`** (nuevo): la interfaz `Vista`, `MovimientoCell` y las 11 pantallas de contenido (Resumen, Movimientos, Operaciones, Puntos, Ranking, Reversión, Monederos, Programadas, Notificaciones, Reportes, Integridad). Clases y constructores ahora `public` (cruzan de paquete); `DashboardView` las importa con `interfaz.vistas.*`.
- **`modelo/tipo/`** (nuevo): los 8 enums (`Beneficio`, `EstadoProgramada`, `Rango`, `Recurrencia`, `TipoMonedero`, `TipoMovimientoPuntos`, `TipoNotificacion`, `TipoTransaccion`). `modelo/` queda con las 9 entidades. `Banco` y `ServicioPuntos` importan `modelo.tipo.*`; el resto, imports específicos.
- **`src/main/resources/interfaz/estilos/`** (nuevo): los 6 CSS. `Main.ESTILOS` carga desde `/interfaz/estilos/`; `pass-card.css` ajustó la ruta de su imagen de fondo a `../images/`.
- `estructuras/`, `servicio/`, `persistencia/` y `servicio/dto/` no cambian. Verificado: `mvn clean compile` OK y `mvn test` 95/96 (el único fallo es el preexistente de `ListaCircularTest`).

