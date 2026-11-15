package estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ListaEnlazadaTest {

    @Test
    void agregaAlFinalYConservaOrden() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("A");
        lista.agregar("B");
        lista.agregar("C");

        assertEquals(3, lista.tamano());
        assertEquals("A", lista.obtener(0));
        assertEquals("B", lista.obtener(1));
        assertEquals("C", lista.obtener(2));
    }

    @Test
    void agregaAlInicio() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(2);
        lista.agregarAlInicio(1);
        lista.agregarAlInicio(0);

        assertEquals(3, lista.tamano());
        assertEquals(0, lista.obtener(0));
        assertEquals(1, lista.obtener(1));
        assertEquals(2, lista.obtener(2));
    }

    @Test
    void obtenerValidaIndicesFueraDeRango() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("A");

        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(1));
    }

    @Test
    void eliminarPrimeraAparicion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("A");
        lista.agregar("B");
        lista.agregar("A");

        assertTrue(lista.eliminar("A"));
        assertEquals(2, lista.tamano());
        assertEquals("B", lista.obtener(0));
        assertEquals("A", lista.obtener(1));
        assertFalse(lista.eliminar("Z"));
    }

    @Test
    void eliminarUltimoElementoActualizaCola() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("A");
        lista.agregar("B");

        assertTrue(lista.eliminar("B"));
        lista.agregar("C");

        assertEquals(2, lista.tamano());
        assertEquals("A", lista.obtener(0));
        assertEquals("C", lista.obtener(1));
    }

    @Test
    void iteradorRecorreYFallaAlFinal() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(1);
        lista.agregar(2);

        Iterator<Integer> it = lista.iterator();
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void estaVacia() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia());
        lista.agregar("A");
        assertFalse(lista.estaVacia());
    }
}
