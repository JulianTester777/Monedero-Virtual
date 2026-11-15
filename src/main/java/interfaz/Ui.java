package interfaz;

import modelo.Beneficio;
import modelo.TipoTransaccion;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos y textos compartidos por las vistas. */
final class Ui {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final NumberFormat MONTO = NumberFormat.getNumberInstance(ES);
    private static final NumberFormat PUNTOS = NumberFormat.getIntegerInstance(ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM, HH:mm", ES);

    static {
        MONTO.setMinimumFractionDigits(2);
        MONTO.setMaximumFractionDigits(2);
    }

    private Ui() { }

    static String monto(BigDecimal valor) {
        return MONTO.format(valor);
    }

    static String puntos(int valor) {
        return PUNTOS.format(valor);
    }

    static String fecha(LocalDateTime valor) {
        return FECHA.format(valor);
    }

    static String nombreTipo(TipoTransaccion tipo) {
        return switch (tipo) {
            case DEPOSITO -> "Depósito";
            case RETIRO -> "Retiro";
            case TRANSFERENCIA_ENVIADA -> "Transferencia enviada";
            case TRANSFERENCIA_RECIBIDA -> "Transferencia recibida";
            case BONO -> "Bono de saldo";
        };
    }

    static boolean esEntrada(TipoTransaccion tipo) {
        return tipo == TipoTransaccion.DEPOSITO
                || tipo == TipoTransaccion.TRANSFERENCIA_RECIBIDA
                || tipo == TipoTransaccion.BONO;
    }

    static String beneficio(Beneficio beneficio) {
        String descripcion = beneficio.getDescripcion();
        if (descripcion.endsWith(".")) {
            descripcion = descripcion.substring(0, descripcion.length() - 1);
        }
        return descripcion + " (" + puntos(beneficio.getCosto()) + " pts)";
    }

    static BigDecimal parseMonto(String texto) {
        if (texto == null) throw new NumberFormatException("Monto vacío");
        String limpio = texto.trim().replace(" ", "");
        if (limpio.isEmpty()) throw new NumberFormatException("Monto vacío");
        if (limpio.indexOf(',') >= 0 && limpio.indexOf('.') >= 0) {
            limpio = limpio.replace(".", "").replace(',', '.');
        } else if (limpio.indexOf(',') >= 0) {
            limpio = limpio.replace(',', '.');
        }
        return new BigDecimal(limpio);
    }
}
