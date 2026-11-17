package modelo.tipo;

public enum Beneficio {

    DESCUENTO_TRANSFERENCIA(100, "10% menos de comision en transferencias"),
    SIN_CARGOS_RETIRO(500, "Un mes sin cargos por retiros"),
    BONO_SALDO(1000, "Bono de 50 unidades en el monedero");

    private final int costo;
    private final String descripcion;

    Beneficio(int costo, String descripcion) {
        this.costo = costo;
        this.descripcion = descripcion;
    }

    public int getCosto() { return costo; }
    public String getDescripcion() { return descripcion; }
}
