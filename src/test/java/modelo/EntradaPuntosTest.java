package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntradaPuntosTest {

    @Test
    void ordenaPorPuntosYDesempataPorId() {
        EntradaPuntos a = new EntradaPuntos("A", 10);
        EntradaPuntos b = new EntradaPuntos("B", 10);
        EntradaPuntos c = new EntradaPuntos("C", 20);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(c.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new EntradaPuntos("A", 10)));
    }

    @Test
    void equalsYHashCodeUsanClienteYPuntos() {
        assertEquals(new EntradaPuntos("A", 10), new EntradaPuntos("A", 10));
        assertEquals(new EntradaPuntos("A", 10).hashCode(), new EntradaPuntos("A", 10).hashCode());
        assertNotEquals(new EntradaPuntos("A", 10), new EntradaPuntos("A", 11));
        assertNotEquals(new EntradaPuntos("A", 10), new EntradaPuntos("B", 10));
    }
}
