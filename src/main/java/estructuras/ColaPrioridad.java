package estructuras;

import java.util.NoSuchElementException;

/**
 * Cola de prioridad minima implementada con un monticulo binario.
 * Complejidad: insertar y extraerMinimo O(log n), verMinimo O(1).
 */
public class ColaPrioridad<T extends Comparable<T>> {

    private Object[] datos;
    private int tamano;

    public ColaPrioridad() {
        this.datos = new Object[8];
    }

    public void insertar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("El dato no puede ser nulo");
        }
        asegurarCapacidad();
        datos[tamano] = dato;
        subir(tamano);
        tamano++;
    }

    @SuppressWarnings("unchecked")
    public T extraerMinimo() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola de prioridad esta vacia");
        }
        T minimo = (T) datos[0];
        tamano--;
        datos[0] = datos[tamano];
        datos[tamano] = null;
        if (tamano > 0) {
            bajar(0);
        }
        return minimo;
    }

    @SuppressWarnings("unchecked")
    public T verMinimo() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola de prioridad esta vacia");
        }
        return (T) datos[0];
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }

    private void asegurarCapacidad() {
        if (tamano == datos.length) {
            Object[] nuevo = new Object[datos.length * 2];
            for (int i = 0; i < datos.length; i++) {
                nuevo[i] = datos[i];
            }
            datos = nuevo;
        }
    }

    @SuppressWarnings("unchecked")
    private void subir(int indice) {
        while (indice > 0) {
            int padre = (indice - 1) / 2;
            T actual = (T) datos[indice];
            T valorPadre = (T) datos[padre];
            if (actual.compareTo(valorPadre) >= 0) {
                return;
            }
            intercambiar(indice, padre);
            indice = padre;
        }
    }

    @SuppressWarnings("unchecked")
    private void bajar(int indice) {
        while (true) {
            int izquierdo = 2 * indice + 1;
            int derecho = 2 * indice + 2;
            int menor = indice;

            if (izquierdo < tamano
                    && ((T) datos[izquierdo]).compareTo((T) datos[menor]) < 0) {
                menor = izquierdo;
            }
            if (derecho < tamano
                    && ((T) datos[derecho]).compareTo((T) datos[menor]) < 0) {
                menor = derecho;
            }
            if (menor == indice) {
                return;
            }
            intercambiar(indice, menor);
            indice = menor;
        }
    }

    private void intercambiar(int i, int j) {
        Object tmp = datos[i];
        datos[i] = datos[j];
        datos[j] = tmp;
    }
}
