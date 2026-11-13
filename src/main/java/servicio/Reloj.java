package servicio;


import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Reloj del sistema con tiempo simulado: hora real + un desplazamiento
 * que se puede adelantar para probar beneficios con vencimiento
 * (y, en la Fase 5, transacciones programadas).
 */
public class Reloj {

    private Duration desplazamiento = Duration.ZERO;

    public LocalDateTime ahora() { return LocalDateTime.now().plus(desplazamiento); }

    public void avanzarDias(long dias) { desplazamiento = desplazamiento.plusDays(dias); }
}