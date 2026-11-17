package servicio;

import estructuras.ArbolAVL;
import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import modelo.*;
import modelo.tipo.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestiona los puntos de los clientes con dos estructuras:
 *  - TablaHash (id -> CuentaPuntos): acceso O(1) a los datos de puntos de un cliente.
 *  - ArbolAVL de EntradaPuntos: clientes ordenados por puntos acumulados (ranking).
 */
public class ServicioPuntos {

    private static final BigDecimal CIEN = new BigDecimal(100);
    private static final int DIAS_SIN_CARGOS = 30;

    private final TablaHash<String, CuentaPuntos> cuentas = new TablaHash<>();
    private final ArbolAVL<EntradaPuntos> ranking = new ArbolAVL<>();

    public void registrarCliente(String id) {
        cuentas.poner(id, new CuentaPuntos(id));
        ranking.insertar(new EntradaPuntos(id, 0));
    }

    public CuentaPuntos consultar(String id) {
        CuentaPuntos c = cuentas.obtener(id);
        if (c == null) throw new OperacionInvalidaException("No hay cuenta de puntos para " + id);
        return c;
    }

    /** Historial de movimientos de puntos del cliente. */
    public ListaEnlazada<MovimientoPuntos> historial(String id) {
        return consultar(id).getHistorial();
    }

    /** Movimiento de puntos generado por una transaccion, o null si no existe. */
    public MovimientoPuntos buscarMovimientoPorTransaccion(String id, int transaccionId) {
        for (MovimientoPuntos m : consultar(id).getHistorial()) {
            if (m.getTransaccionId() == transaccionId) return m;
        }
        return null;
    }

    /** Rango que corresponde a una cantidad de puntos acumulados. */
    public static Rango calcularRango(int puntosAcumulados) {
        return Rango.desdePuntos(puntosAcumulados);
    }

    /** Rango actual del cliente. */
    public Rango consultarRango(String id) {
        return consultar(id).getRango();
    }

    /** Puntos que genera una operacion: division entera por cada 100 unidades. */
    public static int calcularPuntos(TipoTransaccion tipo, BigDecimal monto) {
        int bloques = monto.divideToIntegralValue(CIEN).intValue();
        return switch (tipo) {
            case DEPOSITO -> bloques;                  // 1 punto por cada 100
            case RETIRO -> bloques * 2;                // 2 puntos por cada 100
            case TRANSFERENCIA_ENVIADA -> bloques * 3; // 3 puntos por cada 100
            default -> 0;                              // recibir, mover entre monederos o un bono no da puntos
        };
    }

    /** Otorga los puntos de una transaccion y actualiza el ranking. Devuelve los puntos ganados. */
    public int otorgar(Transaccion t) {
        int puntos = calcularPuntos(t.getTipo(), t.getMonto());
        if (puntos == 0) return 0;
        CuentaPuntos cuenta = consultar(t.getClienteId());
        int anterior = cuenta.getAcumulados();
        cuenta.sumar(puntos);
        cuenta.getHistorial().agregar(new MovimientoPuntos(
                TipoMovimientoPuntos.GANADOS, puntos, t.getId(),
                t.getTipo() + " de " + t.getMonto().toPlainString(), t.getFecha()));
        actualizarRanking(cuenta.getClienteId(), anterior, cuenta.getAcumulados());
        return puntos;
    }

    /** Descuenta los puntos generados por una transaccion revertida. */
    public int revertir(Transaccion t) {
        return revertir(t, LocalDateTime.now());
    }

    /** Descuenta los puntos de una transaccion y registra el movimiento de reversion. */
    public int revertir(Transaccion t, LocalDateTime fecha) {
        int puntos = calcularPuntos(t.getTipo(), t.getMonto());
        if (puntos == 0) return 0;
        CuentaPuntos cuenta = consultar(t.getClienteId());
        if (cuenta.getDisponibles() < puntos)
            throw new OperacionInvalidaException("No se puede revertir: el cliente ya gasto los puntos generados.");
        int anterior = cuenta.getAcumulados();
        cuenta.revertir(puntos);
        cuenta.getHistorial().agregar(new MovimientoPuntos(
                TipoMovimientoPuntos.REVERSION, puntos, t.getId(),
                "Reversion de " + t.getTipo() + " de " + t.getMonto().toPlainString(), fecha));
        actualizarRanking(cuenta.getClienteId(), anterior, cuenta.getAcumulados());
        return puntos;
    }

    /** Canjea un beneficio. Valida todo antes de modificar nada. */
    public MovimientoPuntos canjear(String id, Beneficio beneficio, LocalDateTime ahora) {
        CuentaPuntos cuenta = consultar(id);
        if (cuenta.getDisponibles() < beneficio.getCosto())
            throw new OperacionInvalidaException("Puntos insuficientes: necesitas " + beneficio.getCosto()
                    + " y tienes " + cuenta.getDisponibles());
        if (beneficio == Beneficio.DESCUENTO_TRANSFERENCIA && cuenta.tieneDescuentoTransferencia())
            throw new OperacionInvalidaException("Ya tienes activo el descuento en transferencias.");
        if (beneficio == Beneficio.SIN_CARGOS_RETIRO && cuenta.tieneRetiroGratis(ahora))
            throw new OperacionInvalidaException("Ya tienes activo el mes sin cargos por retiro.");

        cuenta.gastar(beneficio.getCosto());
        switch (beneficio) {
            case DESCUENTO_TRANSFERENCIA -> cuenta.activarDescuentoTransferencia();
            case SIN_CARGOS_RETIRO -> cuenta.activarRetiroGratisHasta(ahora.plusDays(DIAS_SIN_CARGOS));
            case BONO_SALDO -> { /* el saldo lo acredita Banco */ }
        }
        MovimientoPuntos m = new MovimientoPuntos(
                TipoMovimientoPuntos.CANJE, beneficio.getCosto(), 0, "Canje: " + beneficio.getDescripcion(), ahora);
        cuenta.getHistorial().agregar(m);
        return m;
    }

    /** Los n clientes con mas puntos acumulados, de mayor a menor. */
    public ListaEnlazada<EntradaPuntos> top(int n) { return ranking.mayores(n); }

    public ListaEnlazada<EntradaPuntos> rankingCompleto() { return ranking.mayores(ranking.tamano()); }

    public int alturaArbol() { return ranking.altura(); }

    private void actualizarRanking(String id, int anterior, int nuevo) {
        ranking.eliminar(new EntradaPuntos(id, anterior));
        ranking.insertar(new EntradaPuntos(id, nuevo));
    }
}