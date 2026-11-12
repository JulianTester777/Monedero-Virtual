package estructuras;

import java.util.Iterator;

/**
 * Tabla hash con encadenamiento separado (cada cubeta es una lista enlazada propia)
 * y redimensionamiento dinamico cuando el factor de carga supera 0.75.
 * Complejidad promedio: insertar, buscar y eliminar O(1).
 */
public class TablaHash<K, V> implements Iterable<TablaHash.Entrada<K, V>> {

    public static class Entrada<K, V> {
        private final K clave;
        private V valor;
        Entrada(K clave, V valor) { this.clave = clave; this.valor = valor; }
        public K getClave() { return clave; }
        public V getValor() { return valor; }
    }

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA_MAX = 0.75;

    private ListaEnlazada<Entrada<K, V>>[] cubetas;
    private int tamano;

    public TablaHash() { cubetas = crearArreglo(CAPACIDAD_INICIAL); }

    @SuppressWarnings("unchecked")
    private ListaEnlazada<Entrada<K, V>>[] crearArreglo(int capacidad) {
        return (ListaEnlazada<Entrada<K, V>>[]) new ListaEnlazada[capacidad];
    }

    private int indice(K clave, int capacidad) {
        return (clave.hashCode() & 0x7fffffff) % capacidad;
    }

    /** Inserta o actualiza: O(1) promedio. */
    public void poner(K clave, V valor) {
        int i = indice(clave, cubetas.length);
        if (cubetas[i] == null) cubetas[i] = new ListaEnlazada<>();
        for (Entrada<K, V> e : cubetas[i]) {
            if (e.clave.equals(clave)) { e.valor = valor; return; }
        }
        cubetas[i].agregar(new Entrada<>(clave, valor));
        tamano++;
        if ((double) tamano / cubetas.length > FACTOR_CARGA_MAX) redimensionar();
    }

    /** Busca por clave. Devuelve null si no existe: O(1) promedio. */
    public V obtener(K clave) {
        int i = indice(clave, cubetas.length);
        if (cubetas[i] == null) return null;
        for (Entrada<K, V> e : cubetas[i]) {
            if (e.clave.equals(clave)) return e.valor;
        }
        return null;
    }

    public boolean contiene(K clave) { return obtener(clave) != null; }


    public boolean eliminar(K clave) {
        int i = indice(clave, cubetas.length);
        if (cubetas[i] == null) return false;
        for (Entrada<K, V> e : cubetas[i]) {
            if (e.clave.equals(clave)) {
                cubetas[i].eliminar(e);
                tamano--;
                return true;
            }
        }
        return false;
    }

    private void redimensionar() {
        ListaEnlazada<Entrada<K, V>>[] viejas = cubetas;
        cubetas = crearArreglo(viejas.length * 2);
        for (ListaEnlazada<Entrada<K, V>> cubeta : viejas) {
            if (cubeta == null) continue;
            for (Entrada<K, V> e : cubeta) {
                int i = indice(e.clave, cubetas.length);
                if (cubetas[i] == null) cubetas[i] = new ListaEnlazada<>();
                cubetas[i].agregar(e);
            }
        }
    }

    public int tamano() { return tamano; }
    public boolean estaVacia() { return tamano == 0; }

    /** Recorre todas las entradas: O(capacidad + n). */
    @Override
    public Iterator<Entrada<K, V>> iterator() {
        ListaEnlazada<Entrada<K, V>> todas = new ListaEnlazada<>();
        for (ListaEnlazada<Entrada<K, V>> cubeta : cubetas) {
            if (cubeta == null) continue;
            for (Entrada<K, V> e : cubeta) todas.agregar(e);
        }
        return todas.iterator();
    }
}