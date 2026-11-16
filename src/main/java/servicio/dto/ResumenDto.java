package servicio.dto;

import java.math.BigDecimal;

public record ResumenDto(
        String nombreCliente,
        BigDecimal saldo,
        BigDecimal saldoTotal,
        int puntosDisponibles,
        int puntosAcumulados,
        String rango,
        String siguienteRango,
        int faltanParaSiguiente,
        double progreso,
        int notificacionesNoLeidas) {
}
