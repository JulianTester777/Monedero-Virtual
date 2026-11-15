package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ComisionesTest {

    @Test
    void transferenciaSinDescuento() {
        assertEquals(new BigDecimal("20.00"), Comisiones.transferencia(new BigDecimal("1000"), false));
    }

    @Test
    void transferenciaConDescuento() {
        assertEquals(new BigDecimal("18.00"), Comisiones.transferencia(new BigDecimal("1000"), true));
    }

    @Test
    void retiroNormalYGratis() {
        assertEquals(new BigDecimal("10.00"), Comisiones.retiro(new BigDecimal("1000"), false));
        assertEquals(new BigDecimal("0.00"), Comisiones.retiro(new BigDecimal("1000"), true));
    }

    @Test
    void redondeaHalfUpADosDecimales() {
        assertEquals(new BigDecimal("0.01"), Comisiones.transferencia(new BigDecimal("0.25"), false));
        assertEquals(new BigDecimal("0.00"), Comisiones.transferencia(new BigDecimal("0.10"), false));
        assertEquals(new BigDecimal("0.01"), Comisiones.retiro(new BigDecimal("0.50"), false));
    }
}
