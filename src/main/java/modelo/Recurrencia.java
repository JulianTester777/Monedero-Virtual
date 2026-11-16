package modelo;

import java.time.LocalDateTime;

/** Frecuencia de una transaccion programada. */
public enum Recurrencia {
    NINGUNA,
    DIARIA,
    SEMANAL,
    MENSUAL;

    /** Fecha de la siguiente ejecucion, o null si la transaccion no se repite. */
    public LocalDateTime siguiente(LocalDateTime fecha) {
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        return switch (this) {
            case NINGUNA -> null;
            case DIARIA -> fecha.plusDays(1);
            case SEMANAL -> fecha.plusWeeks(1);
            case MENSUAL -> fecha.plusMonths(1);
        };
    }
}
