package servicio.dto;

public record RankingDto(
        String clienteId,
        String nombre,
        int puntos,
        boolean actual) {
}
