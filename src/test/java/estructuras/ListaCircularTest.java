package estructuras;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ListaCircularTest {

    @Test
    void nuevaListaEstaVacia() {
        ListaCircular<Integer> lista = new ListaCircular<>();

        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamano());
    }

    @Test
    void agregarYRecorrerCircularmente() {
        ListaCircular<String> lista = new ListaCircular<>();
        lista.agregar("a");
        lista.agregar("b");
        lista.agregar("c");

        assertEquals(3, lista.tamano());
        assertEquals("a", lista.actual());
        assertEquals("b", lista.siguiente());
        assertEquals("c", lista.siguiente());
        assertEquals("a", lista.siguiente());
    }

    @Test
    void eliminarActualAvanzaAlSiguiente() {
        ListaCircular<Integer> lista = new ListaCircular<>();
        lista.agregar(1);
        lista.agregar(2);
        lista.agregar(3);

        assertEquals(1, lista.eliminar());
        assertEquals(2, lista.tamano());
        assertEquals(2, lista.actual());
        assertEquals(3, lista.siguiente());
        assertEquals(2, lista.siguiente());
    }

    @Test
    void eliminarHastaVaciar() {
        ListaCircular<Integer> lista = new ListaCircular<>();
        lista.agregar(10);

        assertEquals(10, lista.eliminar());
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamano());
    }

    @Test
    void operacionesEnListaVaciaLanzanExcepcion() {
        ListaCircular<Integer> lista = new ListaCircular<>();

        assertThrows(NoSuchElementException.class, lista::actual);
        assertThrows(NoSuchElementException.class, lista::siguiente);
        assertThrows(NoSuchElementException.class, lista::eliminar);
    }
}
