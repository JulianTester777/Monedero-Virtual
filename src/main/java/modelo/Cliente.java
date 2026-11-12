package modelo;

import estructuras.ListaEnlazada;
import java.math.BigDecimal;

public class Cliente {

    private final String id;
    private final String nombre;
    private BigDecimal saldo = BigDecimal.ZERO;
    private final ListaEnlazada<Transaccion> historial = new ListaEnlazada<>();

    public Cliente(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getSaldo() { return saldo; }
    public ListaEnlazada<Transaccion> getHistorial() { return historial; }

    public void acreditar(BigDecimal monto) { saldo = saldo.add(monto); }
    public void debitar(BigDecimal monto) { saldo = saldo.subtract(monto); }

    @Override
    public String toString() { return nombre + " (" + id + ") - Saldo: " + saldo.toPlainString(); }
}