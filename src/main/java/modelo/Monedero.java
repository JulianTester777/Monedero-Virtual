package modelo;

import modelo.tipo.TipoMonedero;

import java.math.BigDecimal;

/** Saldo independiente dentro de la cuenta de un cliente. */
public class Monedero {

    private final String id;
    private final String nombre;
    private final TipoMonedero tipo;
    private BigDecimal saldo = BigDecimal.ZERO;

    public Monedero(String id, String nombre, TipoMonedero tipo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del monedero es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del monedero es obligatorio.");
        }
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo == null ? TipoMonedero.OTRO : tipo;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoMonedero getTipo() { return tipo; }
    public BigDecimal getSaldo() { return saldo; }

    public void acreditar(BigDecimal monto) { saldo = saldo.add(monto); }
    public void debitar(BigDecimal monto) { saldo = saldo.subtract(monto); }

    @Override
    public String toString() {
        return nombre + " (" + id + ") - Saldo: " + saldo.toPlainString();
    }
}
