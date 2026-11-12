package servicio;

import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import modelo.*;
import java.math.BigDecimal;

/** Fachada con la logica de negocio. Los clientes se guardan en una tabla hash por id. */
public class Banco {

    private final TablaHash<String, Cliente> clientes = new TablaHash<>();

    public Cliente registrarCliente(String id, String nombre) {
        if (id == null || id.isBlank()) throw new OperacionInvalidaException("El id es obligatorio.");
        if (nombre == null || nombre.isBlank()) throw new OperacionInvalidaException("El nombre es obligatorio.");
        if (clientes.contiene(id)) throw new OperacionInvalidaException("Ya existe un cliente con id " + id);
        Cliente c = new Cliente(id, nombre);
        clientes.poner(id, c);
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
        Transaccion t = new Transaccion(TipoTransaccion.DEPOSITO, monto, id, null);
        c.getHistorial().agregar(t);
        return t;
    }

    public Transaccion retirar(String id, BigDecimal monto) {
        validarMonto(monto);
        Cliente c = buscarCliente(id);
        if (c.getSaldo().compareTo(monto) < 0)
            throw new OperacionInvalidaException("Saldo insuficiente. Saldo actual: " + c.getSaldo().toPlainString());
        c.debitar(monto);
        Transaccion t = new Transaccion(TipoTransaccion.RETIRO, monto, id, null);
        c.getHistorial().agregar(t);
        return t;
    }

    /** Devuelve la transaccion registrada en el origen. */
    public Transaccion transferir(String idOrigen, String idDestino, BigDecimal monto) {
        validarMonto(monto);
        if (idOrigen.equals(idDestino))
            throw new OperacionInvalidaException("No se puede transferir a la misma cuenta.");
        Cliente origen = buscarCliente(idOrigen);
        Cliente destino = buscarCliente(idDestino);
        if (origen.getSaldo().compareTo(monto) < 0)
            throw new OperacionInvalidaException("Saldo insuficiente. Saldo actual: " + origen.getSaldo().toPlainString());
        origen.debitar(monto);
        destino.acreditar(monto);
        Transaccion enviada = new Transaccion(TipoTransaccion.TRANSFERENCIA_ENVIADA, monto, idOrigen, idDestino);
        Transaccion recibida = new Transaccion(TipoTransaccion.TRANSFERENCIA_RECIBIDA, monto, idDestino, idOrigen);
        origen.getHistorial().agregar(enviada);
        destino.getHistorial().agregar(recibida);
        return enviada;
    }

    public BigDecimal consultarSaldo(String id) { return buscarCliente(id).getSaldo(); }

    public ListaEnlazada<Transaccion> consultarHistorial(String id) { return buscarCliente(id).getHistorial(); }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new OperacionInvalidaException("El monto debe ser mayor que cero.");
    }
}