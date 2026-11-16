package servicio.dto;

import java.time.LocalDateTime;

public record MovimientoPuntosDto(
        String tipo,
        boolean suma,
        int puntos,
        int transaccionId,
        String descripcion,
        LocalDateTime fecha) {
}
