package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Registro inmutable de una operacion realizada sobre una cuenta. */
public class Transaccion {

    private static int contador = 0;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final TipoTransaccion tipo;
    private final BigDecimal monto;
    private final BigDecimal comision;
    private final LocalDateTime fecha;
    private final String clienteId;     // dueno del registro
    private final String contraparteId; // otro cliente (solo transferencias), o null

    public Transaccion(TipoTransaccion tipo, BigDecimal monto, BigDecimal comision,
                       LocalDateTime fecha, String clienteId, String contraparteId) {
        this.id = ++contador;
        this.tipo = tipo;
        this.monto = monto;
        this.comision = comision;
        this.fecha = fecha;
        this.clienteId = clienteId;
        this.contraparteId = contraparteId;
    }

    public int getId() { return id; }
    public TipoTransaccion getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getComision() { return comision; }
    public LocalDateTime getFecha() { return fecha; }
    public String getClienteId() { return clienteId; }
    public String getContraparteId() { return contraparteId; }

    @Override
    public String toString() {
        String extra = contraparteId != null ? " (" + contraparteId + ")" : "";
        String com = comision.signum() > 0 ? " | comision " + comision.toPlainString() : "";
        return String.format("#%d [%s] %s %s%s%s", id, fecha.format(FORMATO), tipo, monto.toPlainString(), extra, com);
    }
}