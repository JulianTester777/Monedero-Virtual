package servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProgramadaDto(
        int id,
        String tipo,
        BigDecimal monto,
        String destino,
        LocalDateTime fecha,
        String recurrencia,
        String estado,
        boolean pendiente) {
}
