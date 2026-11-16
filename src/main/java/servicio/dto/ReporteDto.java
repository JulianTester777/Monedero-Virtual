package servicio.dto;

import estructuras.ListaEnlazada;

public record ReporteDto(
        ListaEnlazada<String> categorias,
        ListaEnlazada<String> clientesRelacionados,
        ListaEnlazada<String> clientesMasConectados,
        ListaEnlazada<FlujoDto> flujos,
        ListaEnlazada<String> camino,
        boolean hayCiclos) {
}
