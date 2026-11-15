package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void acreditarYDebitarActualizanSaldo() {
        Cliente cliente = new Cliente("C1", "Ana");

        cliente.acreditar(new BigDecimal("100"));
        cliente.debitar(new BigDecimal("30"));

        assertEquals("C1", cliente.getId());
        assertEquals("Ana", cliente.getNombre());
        assertEquals(0, cliente.getSaldo().compareTo(new BigDecimal("70")));
        assertEquals(0, cliente.getHistorial().tamano());
    }
}
