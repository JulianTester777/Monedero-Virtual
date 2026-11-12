package estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista simplemente enlazada con referencia a la cola.
 * Complejidad: agregar al final O(1), agregar al inicio O(1),
 * obtener por indice O(n), recorrer O(n).
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;
        Nodo(T dato) { this.dato = dato; }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    /** Agrega al final: O(1). */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = cola = nuevo;
        } else {
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    /** Agrega al inicio: O(1). */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) cola = nuevo;
        tamano++;
    }

    /** Obtiene el elemento en la posicion indicada: O(n). */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano)
            throw new IndexOutOfBoundsException("Indice: " + indice + ", tamano: " + tamano);
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) actual = actual.siguiente;
        return actual.dato;
    }

    /** Elimina la primera aparicion del dato. Devuelve true si se elimino: O(n). */
    public boolean eliminar(T dato) {
        Nodo<T> previo = null, actual = cabeza;
        while (actual != null) {
            if (actual.dato.equals(dato)) {
                if (previo == null) cabeza = actual.siguiente;
                else previo.siguiente = actual.siguiente;
                if (actual == cola) cola = previo;
                tamano--;
                return true;
            }
            previo = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    public int tamano() { return tamano; }
    public boolean estaVacia() { return tamano == 0; }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;
            @Override public boolean hasNext() { return actual != null; }
            @Override public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }
}