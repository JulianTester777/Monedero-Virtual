package servicio.dto;

import java.time.LocalDateTime;

public record NotificacionDto(
        int id,
        String tipo,
        String mensaje,
        LocalDateTime fecha,
        boolean leida) {
}
