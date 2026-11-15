package servicio;

import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import modelo.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fachada con la logica de negocio. Los clientes se guardan en una tabla hash por id. */
public class Banco {

    private static final BigDecimal BONO_SALDO = new BigDecimal("50");

    private final TablaHash<String, Cliente> clientes = new TablaHash<>();
    private final ServicioPuntos puntos = new ServicioPuntos();
    private final Reloj reloj = new Reloj();

    public Cliente registrarCliente(String id, String nombre) {
        if (id == null || id.isBlank()) throw new OperacionInvalidaException("El id es obligatorio.");
        if (nombre == null || nombre.isBlank()) throw new OperacionInvalidaException("El nombre es obligatorio.");
        if (clientes.contiene(id)) throw new OperacionInvalidaException("Ya existe un cliente con id " + id);
        Cliente c = new Cliente(id, nombre);
        clientes.poner(id, c);
        puntos.registrarCliente(id);
        return c;
    }

    public Cliente buscarCliente(String id) {
        Cliente c = clientes.obtener(id);
        if (c == null) throw new OperacionInvalidaException("No existe el cliente " + id);
        return c;
    }

    public ListaEnlazada<Cliente> listarClientes() {
        ListaEnlazada<Cliente> lista = new ListaEnlazada<>();
        for (TablaHash.Entrada<String, Cliente> e : clientes) lista.agregar(e.getValor());
        return lista;
    }

    public Transaccion depositar(String id, BigDecimal monto) {
        validarMonto(monto);
        Cliente c = buscarCliente(id);
        c.acreditar(monto);
        Transaccion t = new Transaccion(TipoTransaccion.DEPOSITO, monto, BigDecimal.ZERO, reloj.ahora(), id, null);
        c.getHistorial().agregar(t);
        puntos.otorgar(t);
        return t;
    }

    public Transaccion retirar(String id, BigDecimal monto) {
        validarMonto(monto);
        Cliente c = buscarCliente(id);
        LocalDateTime ahora = reloj.ahora();
        BigDecimal comision = Comisiones.retiro(monto, puntos.consultar(id).tieneRetiroGratis(ahora));
        BigDecimal total = monto.add(comision);
        validarSaldo(c, total, comision);
        c.debitar(total);
        Transaccion t = new Transaccion(TipoTransaccion.RETIRO, monto, comision, ahora, id, null);
        c.getHistorial().agregar(t);
        puntos.otorgar(t);
        return t;
    }

    /** Devuelve la transaccion registrada en el origen. La comision la paga quien envia. */
    public Transaccion transferir(String idOrigen, String idDestino, BigDecimal monto) {
        validarMonto(monto);
        if (idOrigen.equals(idDestino))
            throw new OperacionInvalidaException("No se puede transferir a la misma cuenta.");
        Cliente origen = buscarCliente(idOrigen);
        Cliente destino = buscarCliente(idDestino);
        LocalDateTime ahora = reloj.ahora();
        BigDecimal comision = Comisiones.transferencia(monto, puntos.consultar(idOrigen).tieneDescuentoTransferencia());
        BigDecimal total = monto.add(comision);
        validarSaldo(origen, total, comision);
        origen.debitar(total);
        destino.acreditar(monto);
        Transaccion enviada = new Transaccion(TipoTransaccion.TRANSFERENCIA_ENVIADA, monto, comision, ahora, idOrigen, idDestino);
        Transaccion recibida = new Transaccion(TipoTransaccion.TRANSFERENCIA_RECIBIDA, monto, BigDecimal.ZERO, ahora, idDestino, idOrigen);
        origen.getHistorial().agregar(enviada);
        destino.getHistorial().agregar(recibida);
        puntos.otorgar(enviada); // solo quien envia gana puntos
        return enviada;
    }

    /** Canjea un beneficio con los puntos disponibles del cliente. */
    public MovimientoPuntos canjearBeneficio(String id, Beneficio beneficio) {
        Cliente c = buscarCliente(id);
        LocalDateTime ahora = reloj.ahora();
        MovimientoPuntos m = puntos.canjear(id, beneficio, ahora);
        if (beneficio == Beneficio.BONO_SALDO) {
            c.acreditar(BONO_SALDO);
            c.getHistorial().agregar(new Transaccion(TipoTransaccion.BONO, BONO_SALDO, BigDecimal.ZERO, ahora, id, null));
        }
        return m;
    }

    public BigDecimal consultarSaldo(String id) { return buscarCliente(id).getSaldo(); }

    public ListaEnlazada<Transaccion> consultarHistorial(String id) { return buscarCliente(id).getHistorial(); }

    public CuentaPuntos consultarPuntos(String id) { buscarCliente(id); return puntos.consultar(id); }

    /** Historial de movimientos de puntos del cliente. */
    public ListaEnlazada<MovimientoPuntos> consultarHistorialPuntos(String id) {
        buscarCliente(id);
        return puntos.historial(id);
    }

    /** Movimiento de puntos generado por una transaccion, o null si no existe. */
    public MovimientoPuntos buscarMovimientoPuntos(String id, int transaccionId) {
        buscarCliente(id);
        return puntos.buscarMovimientoPorTransaccion(id, transaccionId);
    }

    /** Rango actual del cliente segun sus puntos acumulados. */
    public Rango consultarRango(String id) { buscarCliente(id); return puntos.consultarRango(id); }

    public ListaEnlazada<EntradaPuntos> ranking(int n) { return puntos.top(n); }

    public ListaEnlazada<EntradaPuntos> rankingCompleto() { return puntos.rankingCompleto(); }

    public Reloj getReloj() { return reloj; }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new OperacionInvalidaException("El monto debe ser mayor que cero.");
    }

    private void validarSaldo(Cliente c, BigDecimal total, BigDecimal comision) {
        if (c.getSaldo().compareTo(total) < 0)
            throw new OperacionInvalidaException("Saldo insuficiente. Se necesitan " + total.toPlainString()
                    + " (incluye comision de " + comision.toPlainString() + "). Saldo actual: " + c.getSaldo().toPlainString());
    }
}
