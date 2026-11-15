package servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RelojTest {

    @Test
    void avanzarDiasAdelantaElReloj() {
        Reloj reloj = new Reloj();
        LocalDateTime antes = reloj.ahora();

        reloj.avanzarDias(2);

        assertTrue(reloj.ahora().isAfter(antes.plusDays(1)));
    }

    @Test
    void avanzarCeroDiasNoRetrocedeElReloj() {
        Reloj reloj = new Reloj();
        LocalDateTime antes = reloj.ahora();

        reloj.avanzarDias(0);

        assertFalse(reloj.ahora().isBefore(antes));
    }
}
