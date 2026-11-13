package estructuras;


/**
 * Arbol binario de busqueda (sin balanceo). No admite duplicados.
 * Complejidad: insertar, buscar y eliminar O(h), donde h es la altura.
 * En promedio h = O(log n); en el peor caso (datos ordenados) h = O(n).
 */
public class Arbol<T extends Comparable<T>> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> izquierdo, derecho;
        Nodo(T dato) { this.dato = dato; }
    }

    private Nodo<T> raiz;
    private int tamano;

    /** Devuelve true si se inserto (false si ya existia). */
    public boolean insertar(T dato) {
        int antes = tamano;
        raiz = insertar(raiz, dato);
        return tamano > antes;
    }

    private Nodo<T> insertar(Nodo<T> nodo, T dato) {
        if (nodo == null) { tamano++; return new Nodo<>(dato); }
        int c = dato.compareTo(nodo.dato);
        if (c < 0) nodo.izquierdo = insertar(nodo.izquierdo, dato);
        else if (c > 0) nodo.derecho = insertar(nodo.derecho, dato);
        return nodo;
    }

    /** Devuelve true si se elimino. */
    public boolean eliminar(T dato) {
        int antes = tamano;
        raiz = eliminar(raiz, dato);
        return tamano < antes;
    }

    private Nodo<T> eliminar(Nodo<T> nodo, T dato) {
        if (nodo == null) return null;
        int c = dato.compareTo(nodo.dato);
        if (c < 0) nodo.izquierdo = eliminar(nodo.izquierdo, dato);
        else if (c > 0) nodo.derecho = eliminar(nodo.derecho, dato);
        else {
            if (nodo.izquierdo == null) { tamano--; return nodo.derecho; }
            if (nodo.derecho == null) { tamano--; return nodo.izquierdo; }
            // Dos hijos: se reemplaza por el sucesor inorden (minimo del subarbol derecho)
            Nodo<T> sucesor = minimo(nodo.derecho);
            nodo.dato = sucesor.dato;
            nodo.derecho = eliminar(nodo.derecho, sucesor.dato);
        }
        return nodo;
    }

    public boolean contiene(T dato) {
        Nodo<T> actual = raiz;
        while (actual != null) {
            int c = dato.compareTo(actual.dato);
            if (c == 0) return true;
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    private Nodo<T> minimo(Nodo<T> nodo) {
        while (nodo.izquierdo != null) nodo = nodo.izquierdo;
        return nodo;
    }

    public T minimo() { return raiz == null ? null : minimo(raiz).dato; }

    public T maximo() {
        if (raiz == null) return null;
        Nodo<T> actual = raiz;
        while (actual.derecho != null) actual = actual.derecho;
        return actual.dato;
    }

    /** Recorrido inorden: de menor a mayor. O(n). */
    public ListaEnlazada<T> inorden() {
        ListaEnlazada<T> salida = new ListaEnlazada<>();
        inorden(raiz, salida);
        return salida;
    }

    private void inorden(Nodo<T> nodo, ListaEnlazada<T> salida) {
        if (nodo == null) return;
        inorden(nodo.izquierdo, salida);
        salida.agregar(nodo.dato);
        inorden(nodo.derecho, salida);
    }

    /** Los n elementos mayores, de mayor a menor. O(h + n). */
    public ListaEnlazada<T> mayores(int n) {
        ListaEnlazada<T> salida = new ListaEnlazada<>();
        mayores(raiz, salida, n);
        return salida;
    }

    private void mayores(Nodo<T> nodo, ListaEnlazada<T> salida, int limite) {
        if (nodo == null || salida.tamano() >= limite) return;
        mayores(nodo.derecho, salida, limite);
        if (salida.tamano() < limite) salida.agregar(nodo.dato);
        mayores(nodo.izquierdo, salida, limite);
    }

    public int altura() { return altura(raiz); }

    private int altura(Nodo<T> nodo) {
        if (nodo == null) return 0;
        return 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
    }

    public int tamano() { return tamano; }
    public boolean estaVacio() { return tamano == 0; }
}
