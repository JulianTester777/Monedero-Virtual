package servicio.dto;

import estructuras.ListaEnlazada;

public record IntegridadDto(
        boolean valido,
        ListaEnlazada<String> errores) {
}
