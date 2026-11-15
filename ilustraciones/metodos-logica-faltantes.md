# Métodos de lógica faltantes

**Propósito:** dejar por escrito qué lógica falta antes de continuar con las pruebas o con la interfaz JavaFX.

**Base:** `resumenes/proyecto-final-estructuras-de-datos.md` y el código real del repositorio.

**Regla de avance:** primero se completa la lógica; después se prueban esos métodos; al final se empieza la interfaz. Las firmas son propuestas y deben confirmarse antes de implementarlas.

## Estado actual

- Implementado: depósitos, retiros, transferencias, consultas, historial de transacciones, puntos, canje, rangos, ranking, comisiones y reloj simulado.
- Implementado con límite: el ranking usa un BST sin balanceo (`src/main/java/estructuras/Arbol.java:4-8`); el rango ya se calcula, pero no hay beneficios automáticos por rango.
- No implementado: reversión, transacciones programadas, alertas, patrones de gasto, varios monederos, integridad y UI.
- Se agregaron `ServicioPuntos.historial`, `ServicioPuntos.buscarMovimientoPorTransaccion`, `Banco.consultarHistorialPuntos`, `Banco.buscarMovimientoPuntos` y `Banco.rankingCompleto`.
- Se agregaron `Reloj.avanzarHoras` y `Reloj.avanzarMinutos` como apoyo para las programadas.
- Hay 49 pruebas actuales. No se amplía la suite hasta cerrar la lógica.

## Estructuras que faltan (prerrequisito)

No reemplazar las estructuras propias por `java.util`.

- [ ] `Pila<T>` para transacciones reversibles: `apilar`, `desapilar`, `cima`, `estaVacia`, `tamano`.
- [ ] `ColaPrioridad<T>` o montículo para transacciones programadas: `insertar`, `extraerMinimo`, `verMinimo`, `estaVacia`, `tamano`.
- [ ] `ListaCircular<T>` para notificaciones: `agregar`, `siguiente`, `actual`, `eliminar`, `tamano`.
- [ ] `GrafoDirigido<T>` para patrones y monederos: `agregarVertice`, `agregarArista`, `vecinos`, `recorrer`, `tieneCiclo`, `camino`.
- [ ] Árbol balanceado (AVL o similar) para el ranking: `insertar`, `eliminar`, `buscar`, `rotarIzquierda`, `rotarDerecha`, `factorEquilibrio`, `balancear`, `altura`.

## 1. Historial de puntos

**Requisito:** sistema de puntos, acumulación e historial.

- [x] `ServicioPuntos.historial(String id)` → `ListaEnlazada<MovimientoPuntos>`.
- [x] `Banco.consultarHistorialPuntos(String id)` → `ListaEnlazada<MovimientoPuntos>`.
- [x] `ServicioPuntos.buscarMovimientoPorTransaccion(String id, int transaccionId)` → `MovimientoPuntos` o `null`.
- [x] `Banco.buscarMovimientoPuntos(String id, int transaccionId)` → `MovimientoPuntos` o `null`.

**Criterio:** devuelve el historial sin modificarlo; si el cliente no existe, lanza `OperacionInvalidaException`. Es el cierre más pequeño y se puede hacer antes que el resto.

**Estado:** implementado; pendiente de pruebas y de la reversión que lo usará.

## 2. Reversión de transacciones

**Requisito:** los puntos deben recalcularse si se revierte una transacción; el proyecto pide pilas para transacciones reversibles.

**Estado actual:** `Transaccion` es inmutable y no tiene estado de reversión (`src/main/java/modelo/Transaccion.java:7-19`); `Cliente` solo tiene `acreditar` y `debitar` (`src/main/java/modelo/Cliente.java:23-24`); `CuentaPuntos` solo tiene `sumar` y `gastar` (`src/main/java/modelo/CuentaPuntos.java:38-39`).

- [ ] Agregar estado de reversión a `Transaccion` o un registro de IDs revertidos en el servicio.
- [ ] `Banco.revertirUltimaTransaccion(String idCliente)` → `Transaccion`.
- [ ] `Banco.revertirTransaccion(String idCliente, int idTransaccion)` → `Transaccion`.
- [ ] `ServicioPuntos.revertir(Transaccion t)` → `int` con los puntos descontados.
- [ ] `CuentaPuntos.revertir(int puntos)` o `descontar(int puntos)`.
- [ ] Agregar `TipoMovimientoPuntos.REVERSION` y ajustar `MovimientoPuntos.toString`.
- [ ] Actualizar el ranking al revertir y agregar el movimiento de reversión al historial de puntos.

**Decisiones pendientes:**

- Si los puntos ya se gastaron, ¿se rechaza la reversión o se permite que `disponibles` quede negativo?
- En una transferencia, revertir ambos lados y devolver la comisión.
- Evitar doble reversión de la misma transacción.
- El rango se actualiza solo porque `CuentaPuntos.getRango` se deriva de `acumulados`.

## 3. Árbol balanceado y rangos

**Requisito:** usar árboles balanceados para mantener actualizados los rangos y el ranking.

- [ ] Implementar `ArbolAVL` o agregar balanceo al `Arbol` actual.
- [ ] Mantener `ServicioPuntos.actualizarRanking` con operaciones O(log n).
- [ ] Verificar que `ranking`, `alturaArbol` y `Rango` sigan funcionando después del cambio.

**Opcional:** los rangos "pueden" tener beneficios adicionales. Si se implementan:

- [ ] `Comisiones.transferencia(BigDecimal monto, Rango rango)` o equivalente.
- [ ] `ServicioPuntos.bonoPorRango(Rango rango, Transaccion t)` o equivalente.

## 4. Transacciones programadas

**Requisito:** transacciones recurrentes con cola de prioridad por fecha de ejecución.

- [ ] Modelo `TransaccionProgramada`: id, cliente, tipo, monto, destino, fecha de ejecución, recurrencia y estado.
- [ ] Enum `Recurrencia`: `NINGUNA`, `DIARIA`, `SEMANAL`, `MENSUAL`, con `siguiente(LocalDateTime)`.
- [ ] `ColaPrioridad<TransaccionProgramada>` ordenada por fecha.
- [ ] `Banco.programarDeposito(...)`.
- [ ] `Banco.programarRetiro(...)`.
- [ ] `Banco.programarTransferencia(...)`.
- [ ] `Banco.procesarProgramadas()` o `Banco.procesarHasta(LocalDateTime)`.
- [ ] `Banco.cancelarProgramada(int id)`.
- [ ] `Banco.consultarProgramadas(String idCliente)`.
- [x] `Reloj.avanzarHoras` y `Reloj.avanzarMinutos` (apoyo listo; las programadas siguen pendientes).

**Criterio:** cada programada se ejecuta una sola vez cuando vence, usa el saldo disponible en ese momento, respeta la recurrencia y no se duplica. Si falla, debe quedar registrada para una alerta.

## 5. Alertas y notificaciones

**Requisito:** notificaciones automáticas con listas circulares, por ejemplo saldo bajo y recordatorios de programadas.

- [ ] Modelo `Notificacion`: id, cliente, tipo, mensaje, fecha y estado de lectura.
- [ ] Enum `TipoNotificacion`: `SALDO_BAJO`, `PROGRAMADA`, `RANGO`, `BENEFICIO`, `GENERAL`.
- [ ] `ListaCircular<Notificacion>` por cliente o una lista circular global.
- [ ] `ServicioNotificaciones.encolar(String idCliente, Notificacion n)`.
- [ ] `ServicioNotificaciones.siguiente(String idCliente)`.
- [ ] `ServicioNotificaciones.listar(String idCliente)`.
- [ ] `ServicioNotificaciones.marcarLeida(int idNotificacion)`.
- [ ] `ServicioNotificaciones.contarNoLeidas(String idCliente)`.
- [ ] `Banco.consultarNotificaciones(String idCliente)`.
- [ ] `Banco.marcarNotificacionLeida(int idNotificacion)`.
- [ ] `Banco.configurarUmbralSaldoBajo(BigDecimal umbral)`.
- [ ] Disparar la alerta después de depósitos, retiros, transferencias y programadas fallidas.

**Decisión pendiente:** el resumen menciona colas para notificaciones y listas circulares; definir si se usan ambas o solo la lista circular.

## 6. Patrones de gasto

**Requisito:** informes con grafos dirigidos sobre categorías de gasto y transferencias entre clientes.

- [ ] Decidir si se agrega `CategoriaGasto` o si se usan los `TipoTransaccion` existentes.
- [ ] `GrafoDirigido<String>` para clientes y transferencias.
- [ ] `GrafoDirigido<CategoriaGasto>` para categorías, si se agrega el modelo.
- [ ] `ServicioReportes.registrarTransaccion(Transaccion t)`.
- [ ] `ServicioReportes.reportePatrones(String idCliente)`.
- [ ] `ServicioReportes.categoriasMasUsadas(String idCliente)`.
- [ ] `ServicioReportes.clientesMasConectados()`.
- [ ] `ServicioReportes.detectarCiclos()`.
- [ ] `ServicioReportes.flujoEntreClientes()`.
- [ ] `Banco.generarReportePatrones(String idCliente)`.

**Nota:** `Transaccion` no tiene categoría; hoy solo tiene tipo, monto, cliente y contraparte (`src/main/java/modelo/Transaccion.java:13-19`).

## 7. Varios monederos (multimonedero)

**Requisito:** un cliente puede tener varios monederos, por ejemplo ahorro y gasto diario. No significa necesariamente varias monedas.

**Estado actual:** `Cliente` tiene un solo `saldo` (`src/main/java/modelo/Cliente.java:10`).

- [ ] Modelo `Monedero`: id, nombre, saldo y dueño.
- [ ] Enum `TipoMonedero`: `AHORRO`, `DIARIO`, `OTRO`.
- [ ] `Cliente` con `TablaHash<String, Monedero>` o `ListaEnlazada<Monedero>`.
- [ ] `GrafoDirigido<Monedero>` para relaciones y transferencias entre monederos.
- [ ] `Banco.crearMonedero(String idCliente, String idMonedero, String nombre)`.
- [ ] `Banco.listarMonederos(String idCliente)`.
- [ ] `Banco.consultarSaldo(String idCliente, String idMonedero)`.
- [ ] `Banco.depositar(String idCliente, String idMonedero, BigDecimal monto)`.
- [ ] `Banco.transferirEntreMonederos(String idCliente, String idOrigen, String idDestino, BigDecimal monto)`.
- [ ] `Banco.transferir(String idClienteOrigen, String idMonederoOrigen, String idClienteDestino, String idMonederoDestino, BigDecimal monto)`.
- [ ] Mantener sobrecargas de compatibilidad con las firmas actuales, asociadas a un monedero por defecto.

**Nota:** es el cambio más grande porque modifica saldos, historial, transferencias y pruebas existentes. Debe hacerse antes de la UI.

## 8. Verificación de integridad

**Requisito:** validar transacciones con algoritmos recursivos.

- [ ] Modelo `ResultadoIntegridad` con `esValido` y una lista de errores.
- [ ] `Banco.verificarIntegridad()` → `ResultadoIntegridad`.
- [ ] `ServicioIntegridad.verificarCliente(Cliente c)` recursivo.
- [ ] `ServicioIntegridad.verificarCuentaPuntos(CuentaPuntos c)`.
- [ ] `ServicioIntegridad.verificarTransacciones(ListaEnlazada<Transaccion> historial)`.
- [ ] Verificar saldo reconstruyendo el historial, puntos acumulados contra movimientos, IDs duplicados, contrapartes de transferencias, comisiones, saldos negativos y reversiones.
- [ ] Definir cómo recorrer `ListaEnlazada` de forma recursiva sin exponer sus nodos.

**Dependencia:** conviene hacerlo después de reversión y varios monederos, porque la verificación debe conocer esas reglas.

## 9. Interfaz JavaFX (después de la lógica)

**Requisito:** interfaz para operar, consultar puntos, ver rangos y canjear beneficios.

- [ ] No iniciar hasta cerrar los bloques 1-8 y probar sus métodos.
- [ ] Agregar `Launcher`, FXML/controladores y recursos cuando corresponda.
- [ ] Mantener `interfaz.Main` como demo de consola o reemplazarlo de forma controlada.

**Estado actual:** `pom.xml` ya declara JavaFX, pero no hay `Launcher`, FXML ni `src/main/resources`; `mvn javafx:run` ejecuta la demo de consola.

## Orden recomendado

1. Estructuras base: pila, cola de prioridad, lista circular, grafo y árbol balanceado.
2. Historial de puntos.
3. Reversión de transacciones.
4. Balanceo del ranking y, si se decide, beneficios por rango.
5. Transacciones programadas.
6. Alertas y notificaciones.
7. Patrones de gasto.
8. Varios monederos.
9. Verificación de integridad.
10. Pruebas de toda la lógica nueva.
11. Interfaz JavaFX.

## Decisiones que debe tomar el equipo

- Semántica exacta de una reversión y qué pasa con puntos ya gastados.
- Si las transacciones programadas fallidas se reintentan o solo generan alerta.
- Si las notificaciones son por cliente o globales.
- Si los reportes usan categorías nuevas o los tipos de transacción actuales.
- Cómo compatibilizar varios monederos con las firmas actuales de `Banco`.
- Si la integridad reconstruye saldos desde el historial o usa instantáneas guardadas.
- Si los beneficios por rango son obligatorios o quedan como mejora opcional.
