package estructuras;

/**
 * Grafo dirigido implementado con lista de adyacencia usando estructuras propias.
 */
public class GrafoDirigido<T> {

    private final TablaHash<T, ListaEnlazada<T>> adyacencia;
    private int aristas;

    public GrafoDirigido() {
        this.adyacencia = new TablaHash<>();
    }

    public void agregarVertice(T vertice) {
        if (vertice == null) {
            throw new IllegalArgumentException("El vertice no puede ser nulo");
        }
        if (!adyacencia.contiene(vertice)) {
            adyacencia.poner(vertice, new ListaEnlazada<>());
        }
    }

    public void agregarArista(T origen, T destino) {
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Los vertices no pueden ser nulos");
        }
        agregarVertice(origen);
        agregarVertice(destino);
        adyacencia.obtener(origen).agregar(destino);
        aristas++;
    }

    public ListaEnlazada<T> vecinos(T vertice) {
        if (vertice == null || !adyacencia.contiene(vertice)) {
            return new ListaEnlazada<>();
        }
        ListaEnlazada<T> copia = new ListaEnlazada<>();
        for (T v : adyacencia.obtener(vertice)) {
            copia.agregar(v);
        }
        return copia;
    }

    public ListaEnlazada<T> recorrer() {
        ListaEnlazada<T> orden = new ListaEnlazada<>();
        TablaHash<T, Boolean> visitado = new TablaHash<>();
        for (TablaHash.Entrada<T, ListaEnlazada<T>> e : adyacencia) {
            if (!estaVisitado(visitado, e.getClave())) {
                dfs(e.getClave(), visitado, orden);
            }
        }
        return orden;
    }

    private void dfs(T actual, TablaHash<T, Boolean> visitado, ListaEnlazada<T> orden) {
        visitado.poner(actual, Boolean.TRUE);
        orden.agregar(actual);
        ListaEnlazada<T> lista = adyacencia.obtener(actual);
        if (lista == null) {
            return;
        }
        for (T vecino : lista) {
            if (!estaVisitado(visitado, vecino)) {
                dfs(vecino, visitado, orden);
            }
        }
    }

    private boolean estaVisitado(TablaHash<T, Boolean> visitado, T vertice) {
        Boolean v = visitado.obtener(vertice);
        return v != null && v;
    }

    public boolean tieneCiclo() {
        TablaHash<T, Integer> estado = new TablaHash<>();
        for (TablaHash.Entrada<T, ListaEnlazada<T>> e : adyacencia) {
            if (estado.obtener(e.getClave()) == null) {
                if (tieneCicloDesde(e.getClave(), estado)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tieneCicloDesde(T actual, TablaHash<T, Integer> estado) {
        estado.poner(actual, 1);
        ListaEnlazada<T> lista = adyacencia.obtener(actual);
        if (lista != null) {
            for (T vecino : lista) {
                Integer e = estado.obtener(vecino);
                if (e == null) {
                    if (tieneCicloDesde(vecino, estado)) {
                        return true;
                    }
                } else if (e == 1) {
                    return true;
                }
            }
        }
        estado.poner(actual, 2);
        return false;
    }

    public ListaEnlazada<T> camino(T origen, T destino) {
        ListaEnlazada<T> ruta = new ListaEnlazada<>();
        if (origen == null || destino == null || !adyacencia.contiene(origen) || !adyacencia.contiene(destino)) {
            return ruta;
        }
        TablaHash<T, Boolean> visitado = new TablaHash<>();
        ListaEnlazada<T> actual = new ListaEnlazada<>();
        buscarCamino(origen, destino, visitado, actual, ruta);
        return ruta;
    }

    private boolean buscarCamino(T actual, T destino, TablaHash<T, Boolean> visitado,
                                 ListaEnlazada<T> rutaActual, ListaEnlazada<T> resultado) {
        visitado.poner(actual, Boolean.TRUE);
        rutaActual.agregar(actual);
        if (actual.equals(destino)) {
            for (T v : rutaActual) {
                resultado.agregar(v);
            }
            return true;
        }
        ListaEnlazada<T> lista = adyacencia.obtener(actual);
        if (lista != null) {
            for (T vecino : lista) {
                if (!estaVisitado(visitado, vecino)) {
                    if (buscarCamino(vecino, destino, visitado, rutaActual, resultado)) {
                        return true;
                    }
                }
            }
        }
        eliminarUltimo(rutaActual, actual);
        return false;
    }

    private void eliminarUltimo(ListaEnlazada<T> lista, T valor) {
        ListaEnlazada<T> reconstruida = new ListaEnlazada<>();
        int total = lista.tamano();
        int i = 0;
        for (T v : lista) {
            if (i < total - 1) {
                reconstruida.agregar(v);
            }
            i++;
        }
        while (!lista.estaVacia()) {
            lista.eliminar(lista.obtener(0));
        }
        for (T v : reconstruida) {
            lista.agregar(v);
        }
    }

    public int cantidadVertices() {
        return adyacencia.tamano();
    }

    public int cantidadAristas() {
        return aristas;
    }
}
