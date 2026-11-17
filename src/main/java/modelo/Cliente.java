package modelo;

import modelo.tipo.TipoMonedero;

import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import java.math.BigDecimal;

public class Cliente {

    public static final String MONEDERO_PRINCIPAL = "principal";

    private final String id;
    private final String nombre;
    private final Monedero principal;
    private final TablaHash<String, Monedero> monederos = new TablaHash<>();
    private final ListaEnlazada<Transaccion> historial = new ListaEnlazada<>();

    public Cliente(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.principal = new Monedero(MONEDERO_PRINCIPAL, "Principal", TipoMonedero.DIARIO);
        monederos.poner(principal.getId(), principal);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getSaldo() { return principal.getSaldo(); }
    public ListaEnlazada<Transaccion> getHistorial() { return historial; }
    public Monedero getMonederoPrincipal() { return principal; }

    public void acreditar(BigDecimal monto) { principal.acreditar(monto); }
    public void debitar(BigDecimal monto) { principal.debitar(monto); }

    public Monedero crearMonedero(String id, String nombre, TipoMonedero tipo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del monedero es obligatorio.");
        }
        if (monederos.contiene(id)) {
            throw new IllegalArgumentException("Ya existe el monedero " + id);
        }
        Monedero monedero = new Monedero(id, nombre, tipo);
        monederos.poner(id, monedero);
        return monedero;
    }

    public Monedero buscarMonedero(String id) {
        return monederos.obtener(id == null ? MONEDERO_PRINCIPAL : id);
    }

    public ListaEnlazada<Monedero> listarMonederos() {
        ListaEnlazada<Monedero> lista = new ListaEnlazada<>();
        for (TablaHash.Entrada<String, Monedero> e : monederos) lista.agregar(e.getValor());
        return lista;
    }

    public BigDecimal getSaldoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (TablaHash.Entrada<String, Monedero> e : monederos) total = total.add(e.getValor().getSaldo());
        return total;
    }

    @Override
    public String toString() { return nombre + " (" + id + ") - Saldo: " + getSaldo().toPlainString(); }
}
