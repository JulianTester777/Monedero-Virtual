package estructuras;

/**
 * Arbol AVL (BST balanceado). No admite duplicados.
 * Operaciones insertar, eliminar y buscar en O(log n).
 */
public class ArbolAVL<T extends Comparable<T>> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> izquierdo;
        Nodo<T> derecho;
        int altura;

        Nodo(T dato) {
            this.dato = dato;
            this.altura = 1;
        }
    }

    private Nodo<T> raiz;
    private int tamano;

    public boolean insertar(T dato) {
        int antes = tamano;
        raiz = insertar(raiz, dato);
        return tamano > antes;
    }

    private Nodo<T> insertar(Nodo<T> nodo, T dato) {
        if (nodo == null) {
            tamano++;
            return new Nodo<>(dato);
        }
        int c = dato.compareTo(nodo.dato);
        if (c < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, dato);
        } else if (c > 0) {
            nodo.derecho = insertar(nodo.derecho, dato);
        } else {
            return nodo;
        }
        return balancear(actualizarAlturaNodo(nodo));
    }

    public boolean eliminar(T dato) {
        int antes = tamano;
        raiz = eliminar(raiz, dato);
        return tamano < antes;
    }

    private Nodo<T> eliminar(Nodo<T> nodo, T dato) {
        if (nodo == null) {
            return null;
        }
        int c = dato.compareTo(nodo.dato);
        if (c < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, dato);
        } else if (c > 0) {
            nodo.derecho = eliminar(nodo.derecho, dato);
        } else {
            if (nodo.izquierdo == null || nodo.derecho == null) {
                tamano--;
                Nodo<T> hijo = nodo.izquierdo != null ? nodo.izquierdo : nodo.derecho;
                return hijo;
            }
            Nodo<T> sucesor = minimoNodo(nodo.derecho);
            nodo.dato = sucesor.dato;
            nodo.derecho = eliminar(nodo.derecho, sucesor.dato);
        }
        return balancear(actualizarAlturaNodo(nodo));
    }

    public boolean buscar(T dato) {
        Nodo<T> actual = raiz;
        while (actual != null) {
            int c = dato.compareTo(actual.dato);
            if (c == 0) {
                return true;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    public boolean contiene(T dato) {
        return buscar(dato);
    }

    public NodoBalanceado<T> rotarIzquierda(NodoBalanceado<T> nodo) {
        return null;
    }

    public NodoBalanceado<T> rotarDerecha(NodoBalanceado<T> nodo) {
        return null;
    }

    private Nodo<T> rotarIzquierda(Nodo<T> x) {
        Nodo<T> y = x.derecho;
        Nodo<T> t2 = y.izquierdo;
        y.izquierdo = x;
        x.derecho = t2;
        actualizarAlturaNodo(x);
        actualizarAlturaNodo(y);
        return y;
    }

    private Nodo<T> rotarDerecha(Nodo<T> y) {
        Nodo<T> x = y.izquierdo;
        Nodo<T> t2 = x.derecho;
        x.derecho = y;
        y.izquierdo = t2;
        actualizarAlturaNodo(y);
        actualizarAlturaNodo(x);
        return x;
    }

    public int factorEquilibrio() {
        return factorEquilibrio(raiz);
    }

    private int factorEquilibrio(Nodo<T> nodo) {
        if (nodo == null) {
            return 0;
        }
        return alturaNodo(nodo.izquierdo) - alturaNodo(nodo.derecho);
    }

    public NodoBalanceado<T> balancear(NodoBalanceado<T> nodo) {
        return null;
    }

    private Nodo<T> balancear(Nodo<T> nodo) {
        if (nodo == null) {
            return null;
        }
        int fe = factorEquilibrio(nodo);
        if (fe > 1) {
            if (factorEquilibrio(nodo.izquierdo) < 0) {
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo);
            }
            return rotarDerecha(nodo);
        }
        if (fe < -1) {
            if (factorEquilibrio(nodo.derecho) > 0) {
                nodo.derecho = rotarDerecha(nodo.derecho);
            }
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    public int altura() {
        return alturaNodo(raiz);
    }

    private int alturaNodo(Nodo<T> nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    private Nodo<T> actualizarAlturaNodo(Nodo<T> nodo) {
        nodo.altura = 1 + Math.max(alturaNodo(nodo.izquierdo), alturaNodo(nodo.derecho));
        return nodo;
    }

    private Nodo<T> minimoNodo(Nodo<T> nodo) {
        while (nodo.izquierdo != null) {
            nodo = nodo.izquierdo;
        }
        return nodo;
    }

    public T minimo() {
        return raiz == null ? null : minimoNodo(raiz).dato;
    }

    public T maximo() {
        if (raiz == null) {
            return null;
        }
        Nodo<T> actual = raiz;
        while (actual.derecho != null) {
            actual = actual.derecho;
        }
        return actual.dato;
    }

    public ListaEnlazada<T> inorden() {
        ListaEnlazada<T> salida = new ListaEnlazada<>();
        inorden(raiz, salida);
        return salida;
    }

    private void inorden(Nodo<T> nodo, ListaEnlazada<T> salida) {
        if (nodo == null) {
            return;
        }
        inorden(nodo.izquierdo, salida);
        salida.agregar(nodo.dato);
        inorden(nodo.derecho, salida);
    }

    public ListaEnlazada<T> mayores(int n) {
        ListaEnlazada<T> salida = new ListaEnlazada<>();
        mayores(raiz, salida, n);
        return salida;
    }

    private void mayores(Nodo<T> nodo, ListaEnlazada<T> salida, int limite) {
        if (nodo == null || salida.tamano() >= limite) {
            return;
        }
        mayores(nodo.derecho, salida, limite);
        if (salida.tamano() < limite) {
            salida.agregar(nodo.dato);
        }
        mayores(nodo.izquierdo, salida, limite);
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }

    public static class NodoBalanceado<T> {
        public T dato;
    }
}
