package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CuentaPuntosTest {

    @Test
    void sumarAumentaAcumuladosYDisponibles() {
        CuentaPuntos cuenta = new CuentaPuntos("C1");

        cuenta.sumar(30);

        assertEquals(30, cuenta.getAcumulados());
        assertEquals(30, cuenta.getDisponibles());
    }

    @Test
    void gastarSoloReduceDisponibles() {
        CuentaPuntos cuenta = new CuentaPuntos("C1");
        cuenta.sumar(100);

        cuenta.gastar(40);

        assertEquals(100, cuenta.getAcumulados());
        assertEquals(60, cuenta.getDisponibles());
    }

    @Test
    void retiroGratisVenceEnLaFechaIndicada() {
        CuentaPuntos cuenta = new CuentaPuntos("C1");
        LocalDateTime ahora = LocalDateTime.now();

        assertFalse(cuenta.tieneRetiroGratis(ahora));

        cuenta.activarRetiroGratisHasta(ahora.plusDays(30));

        assertTrue(cuenta.tieneRetiroGratis(ahora.plusDays(29)));
        assertFalse(cuenta.tieneRetiroGratis(ahora.plusDays(30)));
    }

    @Test
    void descuentoTransferenciaSeActiva() {
        CuentaPuntos cuenta = new CuentaPuntos("C1");

        assertFalse(cuenta.tieneDescuentoTransferencia());

        cuenta.activarDescuentoTransferencia();

        assertTrue(cuenta.tieneDescuentoTransferencia());
    }
}
