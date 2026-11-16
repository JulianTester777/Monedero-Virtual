package estructuras;

import java.util.NoSuchElementException;

/**
 * Pila (LIFO) implementada con nodos enlazados.
 * Complejidad: apilar, desapilar, cima, estaVacia y tamano O(1).
 */
public class Pila<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cima;
    private int tamano;

    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cima;
        cima = nuevo;
        tamano++;
    }

    public T desapilar() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila esta vacia");
        }
        T dato = cima.dato;
        cima = cima.siguiente;
        tamano--;
        return dato;
    }

    public T cima() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila esta vacia");
        }
        return cima.dato;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }
}
