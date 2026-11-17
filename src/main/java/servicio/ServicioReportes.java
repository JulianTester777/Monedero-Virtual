package servicio;

import estructuras.GrafoDirigido;
import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import modelo.tipo.TipoTransaccion;
import modelo.Transaccion;

/**
 * Analisis de patrones de gasto con grafos dirigidos.
 * Los vertices "cliente:X" y "categoria:TIPO" se conectan con cada transaccion.
 */
public class ServicioReportes {

    private static final String CLIENTE = "cliente:";
    private static final String CATEGORIA = "categoria:";
    private static final String MONEDERO = "monedero:";

    private final GrafoDirigido<String> relaciones = new GrafoDirigido<>();
    private final GrafoDirigido<String> monederos = new GrafoDirigido<>();
    private final TablaHash<String, Integer> conexiones = new TablaHash<>();
    private final ListaEnlazada<String> flujos = new ListaEnlazada<>();

    public void registrarTransaccion(Transaccion t) {
        if (t == null || t.getClienteId() == null) return;
        String cliente = CLIENTE + t.getClienteId();
        relaciones.agregarArista(cliente, CATEGORIA + t.getTipo().name());

        if (t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENVIADA && t.getContraparteId() != null) {
            String destino = CLIENTE + t.getContraparteId();
            relaciones.agregarArista(cliente, destino);
            flujos.agregar(cliente + " -> " + destino + " : " + t.getMonto().toPlainString());
            incrementar(cliente);
            incrementar(destino);
        }
    }

    public void registrarTransferenciaEntreMonederos(String idCliente, String idOrigen, String idDestino) {
        String origen = MONEDERO + idCliente + ":" + idOrigen;
        String destino = MONEDERO + idCliente + ":" + idDestino;
        monederos.agregarArista(origen, destino);
    }

    public ListaEnlazada<String> reportePatrones(String idCliente) {
        return relaciones.vecinos(CLIENTE + idCliente);
    }

    public ListaEnlazada<String> categoriasMasUsadas(String idCliente) {
        ListaEnlazada<String> salida = new ListaEnlazada<>();
        for (String vecino : relaciones.vecinos(CLIENTE + idCliente)) {
            if (vecino.startsWith(CATEGORIA)) salida.agregar(vecino.substring(CATEGORIA.length()));
        }
        return salida;
    }

    public ListaEnlazada<String> clientesMasConectados() {
        ListaEnlazada<String> orden = new ListaEnlazada<>();
        for (TablaHash.Entrada<String, Integer> e : conexiones) {
            String cliente = e.getClave().substring(CLIENTE.length());
            insertarPorConteo(orden, cliente, e.getValor());
        }
        return orden;
    }

    public boolean detectarCiclos() {
        return relaciones.tieneCiclo() || monederos.tieneCiclo();
    }

    public ListaEnlazada<String> flujoEntreClientes() {
        ListaEnlazada<String> copia = new ListaEnlazada<>();
        for (String flujo : flujos) copia.agregar(flujo);
        return copia;
    }

    public ListaEnlazada<String> caminoEntreClientes(String origen, String destino) {
        return relaciones.camino(CLIENTE + origen, CLIENTE + destino);
    }

    private void incrementar(String cliente) {
        Integer actual = conexiones.obtener(cliente);
        conexiones.poner(cliente, actual == null ? 1 : actual + 1);
    }

    private void insertarPorConteo(ListaEnlazada<String> orden, String cliente, int conteo) {
        ListaEnlazada<String> nueva = new ListaEnlazada<>();
        boolean insertado = false;
        for (String actual : orden) {
            Integer c = conexiones.obtener(CLIENTE + actual);
            if (!insertado && (c == null ? 0 : c) < conteo) {
                nueva.agregar(cliente);
                insertado = true;
            }
            nueva.agregar(actual);
        }
        if (!insertado) nueva.agregar(cliente);
        while (!orden.estaVacia()) orden.eliminar(orden.obtener(0));
        for (String valor : nueva) orden.agregar(valor);
    }
}
