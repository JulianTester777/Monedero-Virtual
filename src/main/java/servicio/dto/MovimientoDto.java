package servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDto(
        int id,
        String tipo,
        boolean entrada,
        BigDecimal monto,
        BigDecimal comision,
        LocalDateTime fecha,
        String contraparte,
        String monederoOrigen,
        String monederoDestino,
        boolean revertida,
        Integer puntos,
        boolean puntosEntrada) {
}
