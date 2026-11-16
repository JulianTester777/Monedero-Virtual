package estructuras;

import java.util.NoSuchElementException;

/**
 * Lista circular simplemente enlazada con cursor.
 * Complejidad: agregar O(1), siguiente O(1), actual O(1), eliminar O(1), tamano O(1).
 */
public class ListaCircular<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cola;
    private Nodo<T> actual;
    private Nodo<T> previoActual;
    private int tamano;

    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cola == null) {
            cola = nuevo;
            nuevo.siguiente = nuevo;
            actual = nuevo;
            previoActual = nuevo;
        } else {
            nuevo.siguiente = cola.siguiente;
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    public T siguiente() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista circular esta vacia");
        }
        previoActual = actual;
        actual = actual.siguiente;
        return actual.dato;
    }

    public T actual() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista circular esta vacia");
        }
        return actual.dato;
    }

    public T eliminar() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista circular esta vacia");
        }
        Nodo<T> eliminado = actual;
        T dato = eliminado.dato;
        if (tamano == 1) {
            cola = null;
            actual = null;
            previoActual = null;
        } else {
            previoActual.siguiente = actual.siguiente;
            if (eliminado == cola) {
                cola = previoActual;
            }
            actual = actual.siguiente;
        }
        tamano--;
        return dato;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }
}
