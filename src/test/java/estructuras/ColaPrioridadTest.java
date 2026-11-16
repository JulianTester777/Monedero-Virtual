package estructuras;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ColaPrioridadTest {

    @Test
    void nuevaColaEstaVacia() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();

        assertTrue(cola.estaVacia());
        assertEquals(0, cola.tamano());
    }

    @Test
    void insertaYExtraeEnOrdenMinimo() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();
        cola.insertar(5);
        cola.insertar(1);
        cola.insertar(3);
        cola.insertar(4);
        cola.insertar(2);

        assertEquals(5, cola.tamano());
        assertEquals(1, cola.verMinimo());
        assertEquals(1, cola.extraerMinimo());
        assertEquals(2, cola.extraerMinimo());
        assertEquals(3, cola.extraerMinimo());
        assertEquals(4, cola.extraerMinimo());
        assertEquals(5, cola.extraerMinimo());
        assertTrue(cola.estaVacia());
    }

    @Test
    void verMinimoNoEliminaElemento() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();
        cola.insertar(10);
        cola.insertar(7);

        assertEquals(7, cola.verMinimo());
        assertEquals(2, cola.tamano());
    }

    @Test
    void soportaMuchosElementos() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();
        for (int i = 20; i >= 1; i--) {
            cola.insertar(i);
        }
        assertEquals(20, cola.tamano());
        for (int esperado = 1; esperado <= 20; esperado++) {
            assertEquals(esperado, cola.extraerMinimo());
        }
    }

    @Test
    void extraerEnColaVaciaLanzaExcepcion() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();

        assertThrows(NoSuchElementException.class, cola::extraerMinimo);
    }

    @Test
    void verMinimoEnColaVaciaLanzaExcepcion() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();

        assertThrows(NoSuchElementException.class, cola::verMinimo);
    }

    @Test
    void insertarNuloLanzaExcepcion() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>();

        assertThrows(IllegalArgumentException.class, () -> cola.insertar(null));
    }
}
