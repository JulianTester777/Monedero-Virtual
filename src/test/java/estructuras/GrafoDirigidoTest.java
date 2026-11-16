package estructuras;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrafoDirigidoTest {

    @Test
    void agregaVerticesYAristas() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        grafo.agregarVertice("A");
        grafo.agregarArista("A", "B");
        grafo.agregarArista("A", "C");

        assertEquals(3, grafo.cantidadVertices());
        assertEquals(2, grafo.cantidadAristas());
        ListaEnlazada<String> vecinos = grafo.vecinos("A");
        assertEquals(2, vecinos.tamano());
        assertEquals(0, grafo.vecinos("Z").tamano());
    }

    @Test
    void recorrerVisitaVerticesAlcanzables() {
        GrafoDirigido<Integer> grafo = new GrafoDirigido<>();
        grafo.agregarArista(1, 2);
        grafo.agregarArista(1, 3);
        grafo.agregarArista(2, 4);

        ListaEnlazada<Integer> orden = grafo.recorrer();
        assertTrue(orden.tamano() >= 4);
    }

    @Test
    void detectaCiclos() {
        GrafoDirigido<String> conCiclo = new GrafoDirigido<>();
        conCiclo.agregarArista("A", "B");
        conCiclo.agregarArista("B", "C");
        conCiclo.agregarArista("C", "A");
        assertTrue(conCiclo.tieneCiclo());

        GrafoDirigido<String> sinCiclo = new GrafoDirigido<>();
        sinCiclo.agregarArista("A", "B");
        sinCiclo.agregarArista("B", "C");
        assertFalse(sinCiclo.tieneCiclo());
    }

    @Test
    void encuentraCamino() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        grafo.agregarArista("A", "B");
        grafo.agregarArista("B", "C");
        grafo.agregarArista("A", "D");

        ListaEnlazada<String> camino = grafo.camino("A", "C");
        assertEquals(3, camino.tamano());
        assertEquals("A", camino.obtener(0));
        assertEquals("C", camino.obtener(2));

        ListaEnlazada<String> inexistente = grafo.camino("C", "A");
        assertEquals(0, inexistente.tamano());
    }
}
