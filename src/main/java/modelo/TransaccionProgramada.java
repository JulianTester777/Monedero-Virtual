package modelo;

import modelo.tipo.EstadoProgramada;
import modelo.tipo.Recurrencia;
import modelo.tipo.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Operacion futura que se ejecutara cuando el reloj alcance su fecha. */
public class TransaccionProgramada implements Comparable<TransaccionProgramada> {

    private static int contador = 0;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final String clienteId;
    private final TipoTransaccion tipo;
    private final BigDecimal monto;
    private final String destinoId;
    private LocalDateTime fecha;
    private final Recurrencia recurrencia;
    private EstadoProgramada estado = EstadoProgramada.PENDIENTE;

    public TransaccionProgramada(String clienteId, TipoTransaccion tipo, BigDecimal monto,
                                 String destinoId, LocalDateTime fecha, Recurrencia recurrencia) {
        this.id = ++contador;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.monto = monto;
        this.destinoId = destinoId;
        this.fecha = fecha;
        this.recurrencia = recurrencia == null ? Recurrencia.NINGUNA : recurrencia;
    }

    public int getId() { return id; }
    public String getClienteId() { return clienteId; }
    public TipoTransaccion getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public String getDestinoId() { return destinoId; }
    public LocalDateTime getFecha() { return fecha; }
    public Recurrencia getRecurrencia() { return recurrencia; }
    public EstadoProgramada getEstado() { return estado; }

    public boolean esPendiente() { return estado == EstadoProgramada.PENDIENTE; }

    public void marcarEjecutada() { estado = EstadoProgramada.EJECUTADA; }
    public void marcarFallida() { estado = EstadoProgramada.FALLIDA; }
    public void cancelar() { estado = EstadoProgramada.CANCELADA; }

    /** Deja la transaccion pendiente para su siguiente ejecucion. */
    public void reprogramar(LocalDateTime nuevaFecha) {
        this.fecha = nuevaFecha;
        this.estado = EstadoProgramada.PENDIENTE;
    }

    @Override
    public int compareTo(TransaccionProgramada otra) {
        int porFecha = fecha.compareTo(otra.fecha);
        return porFecha != 0 ? porFecha : Integer.compare(id, otra.id);
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s %s -> %s (%s)", id, fecha.format(FORMATO), tipo,
                monto.toPlainString(), destinoId == null ? clienteId : destinoId, estado);
    }
}
