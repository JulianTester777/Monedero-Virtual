package modelo;

import modelo.tipo.TipoNotificacion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Aviso generado por el sistema para un cliente. */
public class Notificacion {

    private static int contador = 0;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final String clienteId;
    private final TipoNotificacion tipo;
    private final String mensaje;
    private final LocalDateTime fecha;
    private boolean leida;

    public Notificacion(String clienteId, TipoNotificacion tipo, String mensaje, LocalDateTime fecha) {
        this.id = ++contador;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public String getClienteId() { return clienteId; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean estaLeida() { return leida; }

    public void marcarLeida() { leida = true; }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s: %s", id, fecha.format(FORMATO), tipo, mensaje);
    }
}
