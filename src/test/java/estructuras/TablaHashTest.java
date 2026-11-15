package estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TablaHashTest {

    @Test
    void ponerObtenerYActualizar() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        tabla.poner("uno", 1);
        tabla.poner("dos", 2);

        assertEquals(1, tabla.obtener("uno"));
        assertEquals(2, tabla.obtener("dos"));

        tabla.poner("uno", 11);
        assertEquals(11, tabla.obtener("uno"));
        assertEquals(2, tabla.tamano());
        assertTrue(tabla.contiene("dos"));
        assertFalse(tabla.contiene("tres"));
        assertNull(tabla.obtener("tres"));
    }

    @Test
    void eliminar() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        tabla.poner("uno", 1);

        assertTrue(tabla.eliminar("uno"));
        assertFalse(tabla.eliminar("uno"));
        assertFalse(tabla.contiene("uno"));
        assertEquals(0, tabla.tamano());
    }

    @Test
    void redimensionaYConservaTodosLosValores() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>();
        for (int i = 0; i < 100; i++) tabla.poner(i, i * 10);

        assertEquals(100, tabla.tamano());
        for (int i = 0; i < 100; i++) assertEquals(i * 10, tabla.obtener(i));
    }

    @Test
    void iteradorRecorreTodasLasEntradas() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        tabla.poner("a", 1);
        tabla.poner("b", 2);
        tabla.poner("c", 3);

        Set<String> claves = new HashSet<>();
        for (TablaHash.Entrada<String, Integer> e : tabla) claves.add(e.getClave());

        assertEquals(Set.of("a", "b", "c"), claves);
    }

    @Test
    void estaVacia() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        assertTrue(tabla.estaVacia());
        tabla.poner("a", 1);
        assertFalse(tabla.estaVacia());
    }
}
