package servicio.dto;

import java.math.BigDecimal;

public record MonederoDto(
        String id,
        String nombre,
        String tipo,
        BigDecimal saldo,
        boolean principal) {
}
