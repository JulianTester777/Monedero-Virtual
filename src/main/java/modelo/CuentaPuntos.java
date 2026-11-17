package modelo;

import modelo.tipo.Rango;

import estructuras.ListaEnlazada;
import java.time.LocalDateTime;

/**
 * Estado de puntos de un cliente.
 *  - acumulados: total historico ganado. Nunca baja al canjear (define el rango).
 *  - disponibles: lo que se puede gastar en canjes.
 */
public class CuentaPuntos {

    private final String clienteId;
    private int acumulados;
    private int disponibles;
    private final ListaEnlazada<MovimientoPuntos> historial = new ListaEnlazada<>();

    // Beneficios activos
    private boolean descuentoTransferencia;
    private LocalDateTime sinCargosRetiroHasta;

    public CuentaPuntos(String clienteId) { this.clienteId = clienteId; }

    public String getClienteId() { return clienteId; }
    public int getAcumulados() { return acumulados; }
    public int getDisponibles() { return disponibles; }
    public Rango getRango() { return Rango.desdePuntos(acumulados); }
    public ListaEnlazada<MovimientoPuntos> getHistorial() { return historial; }

    public boolean tieneDescuentoTransferencia() { return descuentoTransferencia; }
    public LocalDateTime getSinCargosRetiroHasta() { return sinCargosRetiroHasta; }

    public boolean tieneRetiroGratis(LocalDateTime ahora) {
        return sinCargosRetiroHasta != null && ahora.isBefore(sinCargosRetiroHasta);
    }

    // Publicos porque el servicio vive en otro paquete.
    public void sumar(int puntos) { acumulados += puntos; disponibles += puntos; }
    public void gastar(int puntos) { disponibles -= puntos; }
    public void revertir(int puntos) { acumulados -= puntos; disponibles -= puntos; }
    public void activarDescuentoTransferencia() { descuentoTransferencia = true; }
    public void activarRetiroGratisHasta(LocalDateTime fecha) { sinCargosRetiroHasta = fecha; }
}