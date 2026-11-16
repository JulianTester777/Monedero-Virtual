# Métodos de lógica faltantes

**Propósito:** dejar por escrito qué lógica estaba pendiente y cómo quedó cubierta.

**Base:** `resumenes/proyecto-final-estructuras-de-datos.md` y el código real del repositorio.

**Estado:** la lógica de los bloques 1 a 8 está implementada. La interfaz JavaFX también
existe. No se ejecutaron `mvn test` ni `mvn javafx:run` en esta revisión; la suite actual
tiene 89 pruebas JUnit.

## Estado actual

- Implementado: depósitos, retiros, transferencias, consultas, historial, puntos, canje,
  rangos, ranking, comisiones, reloj simulado, reversión, transacciones programadas,
  notificaciones, reportes de patrones, multimonedero e integridad.
- El ranking usa `ArbolAVL` (`src/main/java/servicio/ServicioPuntos.java`).
- Estructuras propias conectadas: `ListaEnlazada`, `TablaHash`, `Arbol`, `ArbolAVL`,
  `Pila`, `ColaPrioridad`, `ListaCircular` y `GrafoDirigido`.
- La UI está en `src/main/java/interfaz` y los estilos en `src/main/resources/interfaz`.

## 1. Historial de puntos

- [x] `ServicioPuntos.historial(String id)`.
- [x] `ServicioPuntos.buscarMovimientoPorTransaccion(String id, int transaccionId)`.
- [x] `Banco.consultarHistorialPuntos(String id)`.
- [x] `Banco.buscarMovimientoPuntos(String id, int transaccionId)`.

## 2. Reversión de transacciones

- [x] `Transaccion` conserva su id y ahora registra `revertida`, `parId`, `monederoId` y
  `monederoDestinoId`.
- [x] `Banco.revertirUltimaTransaccion(String idCliente)`.
- [x] `Banco.revertirTransaccion(String idCliente, int idTransaccion)`.
- [x] `ServicioPuntos.revertir(Transaccion t)` y sobrecarga con fecha.
- [x] `CuentaPuntos.revertir(int puntos)`.
- [x] `TipoMovimientoPuntos.REVERSION`.
- [x] El ranking se actualiza y se registra el movimiento de reversión.

**Decisiones aplicadas:** se rechaza la reversión si el saldo o los puntos ya fueron
usados; una transferencia revierte ambos registros y devuelve la comisión; el bono de
saldo no se revierte; una transacción no puede revertirse dos veces.

## 3. Árbol balanceado y rangos

- [x] `ServicioPuntos` usa `ArbolAVL<EntradaPuntos>`.
- [x] `actualizarRanking` elimina e inserta entradas en O(log n).
- [x] `ranking`, `alturaArbol` y `Rango` siguen funcionando.
- [ ] Beneficios automáticos por rango: quedan como mejora opcional.

## 4. Transacciones programadas

- [x] `TransaccionProgramada`.
- [x] `Recurrencia` (`NINGUNA`, `DIARIA`, `SEMANAL`, `MENSUAL`) con `siguiente`.
- [x] `EstadoProgramada` (`PENDIENTE`, `EJECUTADA`, `FALLIDA`, `CANCELADA`).
- [x] `ColaPrioridad<TransaccionProgramada>` ordenada por fecha.
- [x] `Banco.programarDeposito`, `programarRetiro` y `programarTransferencia`.
- [x] `Banco.procesarProgramadas()` y `Banco.procesarHasta(LocalDateTime)`.
- [x] `Banco.cancelarProgramada(int id)`.
- [x] `Banco.consultarProgramadas(String idCliente)`.

**Criterio aplicado:** cada programada usa el saldo del momento; la recurrente se
reprograma; si falla, se marca `FALLIDA` y genera una notificación.

## 5. Alertas y notificaciones

- [x] `Notificacion` y `TipoNotificacion`.
- [x] `ListaCircular<Notificacion>` por cliente y una lista enlazada para consultarlas.
- [x] `ServicioNotificaciones.encolar`, `siguiente`, `listar`, `marcarLeida` y
  `contarNoLeidas`.
- [x] `Banco.consultarNotificaciones`, `siguienteNotificacion`,
  `marcarNotificacionLeida`, `notificacionesNoLeidas` y `configurarUmbralSaldoBajo`.
- [x] Se dispara saldo bajo después de depósitos, retiros, transferencias, bonos y
  reversiones; las programadas fallidas también avisan.

## 6. Patrones de gasto

- [x] `GrafoDirigido<String>` para clientes, categorías y monederos.
- [x] `ServicioReportes.registrarTransaccion`.
- [x] `reportePatrones`, `categoriasMasUsadas`, `clientesMasConectados`,
  `detectarCiclos`, `flujoEntreClientes` y `caminoEntreClientes`.
- [x] `Banco.generarReportePatrones` y métodos equivalentes de consulta.
- Se usan los `TipoTransaccion` existentes como categorías; no se agregó
  `CategoriaGasto`.

## 7. Varios monederos (multimonedero)

- [x] `Monedero` y `TipoMonedero` (`AHORRO`, `DIARIO`, `OTRO`).
- [x] `Cliente` conserva un monedero principal y guarda los demás en `TablaHash`.
- [x] `Cliente.getSaldoTotal()` y `Cliente.listarMonederos()`.
- [x] `Banco.crearMonedero`, `listarMonederos` y `consultarSaldo(id, idMonedero)`.
- [x] `Banco.depositar(id, idMonedero, monto)` y `retirar(id, idMonedero, monto)`.
- [x] `Banco.transferirEntreMonederos`.
- [x] `Banco.transferir` con monederos de origen y destino.
- [x] `TipoTransaccion.TRANSFERENCIA_ENTRE_MONEDEROS`.
- Las firmas anteriores siguen funcionando y usan el monedero principal.

## 8. Verificación de integridad

- [x] `ResultadoIntegridad`.
- [x] `Banco.verificarIntegridad()`.
- [x] `ServicioIntegridad.verificarCliente`, `verificarCuentaPuntos`,
  `verificarTransacciones` y `verificarGlobal`.
- [x] Se reconstruyen saldos por monedero, se revisan puntos acumulados/disponibles,
  IDs duplicados, contrapartes, comisiones, saldos negativos y reversiones.

## 9. Interfaz JavaFX

- [x] `interfaz.Main` abre la aplicación con `mvn javafx:run`.
- [x] `DashboardView` y las vistas `ResumenView`, `MovimientosView`, `OperacionesView`,
  `PuntosView` y `RankingView`.
- [x] Pantallas dedicadas: `ReversionView`, `MonederosView`, `ProgramadasView`,
  `NotificacionesView`, `ReportesView` e `IntegridadView`.
- [x] La UI consume DTOs de `servicio.dto` (`ResumenDto`, `MovimientoDto`,
  `MovimientoPuntosDto`, `BeneficioDto`, `RankingDto`, `MonederoDto`, `ProgramadaDto`,
  `NotificacionDto`, `ReporteDto` e `IntegridadDto`).
- [x] `Banco` expone consultas de vista (`consultarResumenVista`,
  `consultarMovimientosVista`, `consultarReversiblesVista`, etc.).
- [x] `MovimientoCell` ya no consulta el banco ni interpreta tipos; pinta un
  `MovimientoDto`. `PuntosView` ya no contiene reglas de negocio.
- [ ] Verificar visualmente la UI con `mvn javafx:run`.

## Orden recomendado para continuar

1. Ejecutar `mvn test` y corregir cualquier regresión.
2. Ejecutar `mvn javafx:run` y revisar las pantallas nuevas en 320px y escritorio.
3. Actualizar las guías HTML de `ilustraciones/` con las ocho estructuras y las nuevas
   clases de modelo/servicio/DTO.
