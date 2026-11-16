package estructuras;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class PilaTest {

    @Test
    void nuevaPilaEstaVacia() {
        Pila<Integer> pila = new Pila<>();

        assertTrue(pila.estaVacia());
        assertEquals(0, pila.tamano());
    }

    @Test
    void apilarYDesapilarRespetaLifo() {
        Pila<String> pila = new Pila<>();

        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");

        assertEquals(3, pila.tamano());
        assertEquals("c", pila.cima());
        assertEquals("c", pila.desapilar());
        assertEquals("b", pila.desapilar());
        assertEquals("a", pila.desapilar());
        assertTrue(pila.estaVacia());
        assertEquals(0, pila.tamano());
    }

    @Test
    void cimaNoEliminaElemento() {
        Pila<Integer> pila = new Pila<>();
        pila.apilar(10);

        assertEquals(10, pila.cima());
        assertEquals(1, pila.tamano());
        assertFalse(pila.estaVacia());
    }

    @Test
    void desapilarEnPilaVaciaLanzaExcepcion() {
        Pila<Integer> pila = new Pila<>();

        assertThrows(NoSuchElementException.class, pila::desapilar);
    }

    @Test
    void cimaEnPilaVaciaLanzaExcepcion() {
        Pila<Integer> pila = new Pila<>();

        assertThrows(NoSuchElementException.class, pila::cima);
    }
}
