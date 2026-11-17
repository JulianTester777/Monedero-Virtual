package servicio;

import estructuras.ListaCircular;
import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import modelo.Notificacion;
import modelo.tipo.TipoNotificacion;

/**
 * Notificaciones por cliente. Cada cliente tiene una lista circular para recorrer
 * sus avisos y una lista enlazada para consultarlos en orden.
 */
public class ServicioNotificaciones {

    private static final BigDecimal UMBRAL_INICIAL = new BigDecimal("1000");

    private final TablaHash<String, ListaCircular<Notificacion>> colas = new TablaHash<>();
    private final TablaHash<String, ListaEnlazada<Notificacion>> historiales = new TablaHash<>();
    private final TablaHash<Integer, Notificacion> porId = new TablaHash<>();
    private BigDecimal umbralSaldoBajo = UMBRAL_INICIAL;

    public Notificacion encolar(String idCliente, TipoNotificacion tipo, String mensaje, LocalDateTime fecha) {
        Notificacion n = new Notificacion(idCliente, tipo, mensaje, fecha);
        ListaCircular<Notificacion> cola = colas.obtener(idCliente);
        if (cola == null) {
            cola = new ListaCircular<>();
            colas.poner(idCliente, cola);
        }
        cola.agregar(n);
        ListaEnlazada<Notificacion> historial = historiales.obtener(idCliente);
        if (historial == null) {
            historial = new ListaEnlazada<>();
            historiales.poner(idCliente, historial);
        }
        historial.agregar(n);
        porId.poner(n.getId(), n);
        return n;
    }

    /** Devuelve la notificacion actual y avanza el cursor circular. */
    public Notificacion siguiente(String idCliente) {
        ListaCircular<Notificacion> cola = colas.obtener(idCliente);
        if (cola == null || cola.estaVacia()) return null;
        Notificacion n = cola.actual();
        cola.siguiente();
        return n;
    }

    public ListaEnlazada<Notificacion> listar(String idCliente) {
        ListaEnlazada<Notificacion> salida = new ListaEnlazada<>();
        ListaEnlazada<Notificacion> historial = historiales.obtener(idCliente);
        if (historial != null) {
            for (Notificacion n : historial) salida.agregar(n);
        }
        return salida;
    }

    public boolean marcarLeida(int idNotificacion) {
        Notificacion n = porId.obtener(idNotificacion);
        if (n == null) return false;
        n.marcarLeida();
        return true;
    }

    public int contarNoLeidas(String idCliente) {
        int total = 0;
        ListaEnlazada<Notificacion> historial = historiales.obtener(idCliente);
        if (historial != null) {
            for (Notificacion n : historial) if (!n.estaLeida()) total++;
        }
        return total;
    }

    public BigDecimal getUmbralSaldoBajo() { return umbralSaldoBajo; }

    public void configurarUmbralSaldoBajo(BigDecimal umbral) {
        if (umbral == null || umbral.signum() < 0)
            throw new OperacionInvalidaException("El umbral de saldo bajo no puede ser negativo.");
        this.umbralSaldoBajo = umbral;
    }

    public boolean esSaldoBajo(BigDecimal saldo) {
        return saldo.compareTo(umbralSaldoBajo) < 0;
    }
}
