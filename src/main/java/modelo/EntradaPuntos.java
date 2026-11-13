package modelo;

/**
 * Nodo del arbol de ranking: (cliente, puntos acumulados).
 * Se ordena por puntos y, si empatan, por id, para que dos clientes
 * con los mismos puntos puedan convivir en el arbol.
 */
public class EntradaPuntos implements Comparable<EntradaPuntos> {

    private final String clienteId;
    private final int puntos;

    public EntradaPuntos(String clienteId, int puntos) {
        this.clienteId = clienteId;
        this.puntos = puntos;
    }

    public String getClienteId() { return clienteId; }
    public int getPuntos() { return puntos; }

    @Override
    public int compareTo(EntradaPuntos otra) {
        if (puntos != otra.puntos) return Integer.compare(puntos, otra.puntos);
        return clienteId.compareTo(otra.clienteId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntradaPuntos e && e.puntos == puntos && e.clienteId.equals(clienteId);
    }

    @Override
    public int hashCode() { return 31 * clienteId.hashCode() + puntos; }

    @Override
    public String toString() { return clienteId + ": " + puntos + " pts"; }
}