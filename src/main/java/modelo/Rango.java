package modelo;

/**
 * Rango de un cliente segun sus puntos acumulados.
 * Bronce: 0-500, Plata: 501-1000, Oro: 1001-5000, Platino: mas de 5000.
 */
public enum Rango {

    BRONCE(0, 500, "Bronce"),
    PLATA(501, 1000, "Plata"),
    ORO(1001, 5000, "Oro"),
    PLATINO(5001, Integer.MAX_VALUE, "Platino");

    private final int desde;
    private final int hasta;
    private final String nombre;

    Rango(int desde, int hasta, String nombre) {
        this.desde = desde;
        this.hasta = hasta;
        this.nombre = nombre;
    }

    /** Devuelve el rango que corresponde a los puntos acumulados. */
    public static Rango desdePuntos(int puntos) {
        if (puntos < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos.");
        for (Rango rango : values()) {
            if (puntos <= rango.hasta) return rango;
        }
        return PLATINO;
    }

    public int getDesde() { return desde; }
    public int getHasta() { return hasta; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() { return nombre; }
}
