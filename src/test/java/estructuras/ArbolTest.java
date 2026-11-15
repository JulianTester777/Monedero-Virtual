package estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArbolTest {

    @Test
    void insertarYRechazarDuplicados() {
        Arbol<Integer> arbol = new Arbol<>();

        assertTrue(arbol.insertar(50));
        assertTrue(arbol.insertar(30));
        assertTrue(arbol.insertar(70));
        assertFalse(arbol.insertar(50));

        assertEquals(3, arbol.tamano());
    }

    @Test
    void minimoMaximoYContiene() {
        Arbol<Integer> arbol = new Arbol<>();
        arbol.insertar(50);
        arbol.insertar(30);
        arbol.insertar(70);
        arbol.insertar(20);
        arbol.insertar(80);

        assertEquals(20, arbol.minimo());
        assertEquals(80, arbol.maximo());
        assertTrue(arbol.contiene(30));
        assertFalse(arbol.contiene(40));
    }

    @Test
    void inordenDevuelveOrdenAscendente() {
        Arbol<Integer> arbol = new Arbol<>();
        for (int n : new int[]{50, 30, 70, 20, 40, 60, 80}) arbol.insertar(n);

        ListaEnlazada<Integer> inorden = arbol.inorden();
        int[] esperado = {20, 30, 40, 50, 60, 70, 80};

        assertEquals(esperado.length, inorden.tamano());
        for (int i = 0; i < esperado.length; i++) assertEquals(esperado[i], inorden.obtener(i));
    }

    @Test
    void eliminarHojaUnHijoYDosHijos() {
        Arbol<Integer> arbol = new Arbol<>();
        for (int n : new int[]{50, 30, 70, 20, 40, 60, 80}) arbol.insertar(n);

        assertTrue(arbol.eliminar(20));
        assertTrue(arbol.eliminar(30));
        assertTrue(arbol.eliminar(50));
        assertFalse(arbol.eliminar(999));

        assertEquals(4, arbol.tamano());
        assertFalse(arbol.contiene(50));
        assertEquals(40, arbol.minimo());
        assertEquals(80, arbol.maximo());

        int[] esperado = {40, 60, 70, 80};
        ListaEnlazada<Integer> inorden = arbol.inorden();
        for (int i = 0; i < esperado.length; i++) assertEquals(esperado[i], inorden.obtener(i));
    }

    @Test
    void mayoresDevuelveDescendenteYRespetaLimite() {
        Arbol<Integer> arbol = new Arbol<>();
        for (int i = 1; i <= 10; i++) arbol.insertar(i);

        ListaEnlazada<Integer> top3 = arbol.mayores(3);
        assertEquals(3, top3.tamano());
        assertEquals(10, top3.obtener(0));
        assertEquals(9, top3.obtener(1));
        assertEquals(8, top3.obtener(2));

        assertEquals(0, arbol.mayores(0).tamano());
        assertEquals(10, arbol.mayores(20).tamano());
    }

    @Test
    void alturaYVacio() {
        Arbol<Integer> arbol = new Arbol<>();
        assertTrue(arbol.estaVacio());
        assertEquals(0, arbol.altura());
        assertNull(arbol.minimo());
        assertNull(arbol.maximo());

        arbol.insertar(2);
        arbol.insertar(1);
        arbol.insertar(3);

        assertEquals(2, arbol.altura());
        assertFalse(arbol.estaVacio());
    }
}
