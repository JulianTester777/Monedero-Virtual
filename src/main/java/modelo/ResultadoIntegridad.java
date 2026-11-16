package modelo;

import estructuras.ListaEnlazada;

/** Resultado de una verificacion de integridad. */
public class ResultadoIntegridad {

    private boolean valido = true;
    private final ListaEnlazada<String> errores = new ListaEnlazada<>();

    public boolean esValido() { return valido; }
    public ListaEnlazada<String> getErrores() { return errores; }

    public void agregarError(String error) {
        valido = false;
        errores.agregar(error);
    }

    public void fusionar(ResultadoIntegridad otro) {
        if (otro == null) return;
        for (String error : otro.errores) agregarError(error);
    }
}
