package servicio;

import estructuras.Arbol;
import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import modelo.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestiona los puntos de los clientes con dos estructuras:
 *  - TablaHash (id -> CuentaPuntos): acceso O(1) a los datos de puntos de un cliente.
 *  - ArbolBST de EntradaPuntos: clientes ordenados por puntos acumulados (ranking).
 */
public class ServicioPuntos {

    private static final BigDecimal CIEN = new BigDecimal(100);
    private static final int DIAS_SIN_CARGOS = 30;

    private final TablaHash<String, CuentaPuntos> cuentas = new TablaHash<>();
    private final Arbol<EntradaPuntos> ranking = new Arbol<>();

    public void registrarCliente(String id) {
        cuentas.poner(id, new CuentaPuntos(id));
        ranking.insertar(new EntradaPuntos(id, 0));
    }

    public CuentaPuntos consultar(String id) {
        CuentaPuntos c = cuentas.obtener(id);
        if (c == null) throw new OperacionInvalidaException("No hay cuenta de puntos para " + id);
        return c;
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
            default -> 0;                              // recibir una transferencia o un bono no da puntos
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