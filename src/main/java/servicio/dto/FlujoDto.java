package servicio.dto;

import java.math.BigDecimal;

public record FlujoDto(
        String origen,
        String destino,
        BigDecimal monto) {

    @Override
    public String toString() {
        return origen + " -> " + destino + ": " + monto.toPlainString();
    }
}
