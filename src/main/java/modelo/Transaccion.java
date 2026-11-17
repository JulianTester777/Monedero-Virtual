package modelo;

import modelo.tipo.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Registro de una operacion realizada sobre una cuenta. */
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
    private boolean revertida;
    private int parId;                  // id de la transaccion contraria en una transferencia
    private String monederoId;          // monedero afectado; null representa el principal
    private String monederoDestinoId;   // monedero destino en transferencias entre monederos

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
    public boolean estaRevertida() { return revertida; }
    public int getParId() { return parId; }
    public String getMonederoId() { return monederoId; }
    public String getMonederoDestinoId() { return monederoDestinoId; }

    public void marcarRevertida() { revertida = true; }
    public void setParId(int parId) { this.parId = parId; }
    public void setMonederoId(String monederoId) { this.monederoId = monederoId; }
    public void setMonederoDestinoId(String monederoDestinoId) { this.monederoDestinoId = monederoDestinoId; }

    @Override
    public String toString() {
        String extra = contraparteId != null ? " (" + contraparteId + ")" : "";
        String com = comision.signum() > 0 ? " | comision " + comision.toPlainString() : "";
        String estado = revertida ? " | REVERTIDA" : "";
        return String.format("#%d [%s] %s %s%s%s%s", id, fecha.format(FORMATO), tipo,
                monto.toPlainString(), extra, com, estado);
    }
}