package estructuras;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArbolAVLTest {

    @Test
    void insertaBuscaYMantieneOrden() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>();
        assertTrue(arbol.insertar(10));
        assertTrue(arbol.insertar(20));
        assertTrue(arbol.insertar(30));
        assertFalse(arbol.insertar(20));

        assertEquals(3, arbol.tamano());
        assertTrue(arbol.buscar(10));
        assertTrue(arbol.contiene(30));
        assertFalse(arbol.buscar(99));

        ListaEnlazada<Integer> inorden = arbol.inorden();
        assertEquals(10, inorden.obtener(0));
        assertEquals(20, inorden.obtener(1));
        assertEquals(30, inorden.obtener(2));
    }

    @Test
    void seBalanceaConInsercionesOrdenadas() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>();
        for (int i = 1; i <= 7; i++) {
            arbol.insertar(i);
        }
        assertEquals(7, arbol.tamano());
        assertTrue(arbol.altura() <= 4);
        assertTrue(Math.abs(arbol.factorEquilibrio()) <= 1);
    }

    @Test
    void eliminaYRebalancea() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>();
        int[] vals = {10, 20, 30, 40, 50, 25};
        for (int v : vals) arbol.insertar(v);

        assertTrue(arbol.eliminar(40));
        assertFalse(arbol.buscar(40));
        assertEquals(vals.length - 1, arbol.tamano());
        assertTrue(arbol.altura() <= 4);

        assertFalse(arbol.eliminar(999));
    }

    @Test
    void mayoresMinimoYMaximo() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>();
        for (int v : new int[]{5, 3, 8, 1, 4, 7, 9}) arbol.insertar(v);

        assertEquals(1, arbol.minimo());
        assertEquals(9, arbol.maximo());
        ListaEnlazada<Integer> mayores = arbol.mayores(3);
        assertEquals(3, mayores.tamano());
        assertEquals(9, mayores.obtener(0));
        assertEquals(8, mayores.obtener(1));
        assertEquals(7, mayores.obtener(2));
    }

    @Test
    void arbolVacio() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>();
        assertTrue(arbol.estaVacio());
        assertEquals(0, arbol.altura());
        assertNull(arbol.minimo());
        assertNull(arbol.maximo());
    }
}
