package modelo;

import modelo.tipo.TipoMovimientoPuntos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MovimientoPuntos {


    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TipoMovimientoPuntos tipo;
    private final int puntos;          // siempre positivo; el tipo indica si suma o resta
    private final int transaccionId;   // transaccion que origino los puntos (0 si es un canje)
    private final String descripcion;
    private final LocalDateTime fecha;

    public MovimientoPuntos(TipoMovimientoPuntos tipo, int puntos, int transaccionId,
                            String descripcion, LocalDateTime fecha) {
        this.tipo = tipo;
        this.puntos = puntos;
        this.transaccionId = transaccionId;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public TipoMovimientoPuntos getTipo() { return tipo; }
    public int getPuntos() { return puntos; }
    public int getTransaccionId() { return transaccionId; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        String signo = tipo == TipoMovimientoPuntos.GANADOS ? "+" : "-";
        return String.format("[%s] %s%d pts - %s", fecha.format(FORMATO), signo, puntos, descripcion);
    }
}
