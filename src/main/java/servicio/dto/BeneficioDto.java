package servicio.dto;

import modelo.tipo.Beneficio;

public record BeneficioDto(
        Beneficio beneficio,
        String nombre,
        String descripcion,
        int costo,
        boolean activo,
        boolean puedeCanjear,
        int faltan,
        String textoEstado) {
}
