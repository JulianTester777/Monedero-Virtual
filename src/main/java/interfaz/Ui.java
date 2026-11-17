package interfaz;

import javafx.scene.control.Label;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import modelo.Recurrencia;

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
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    static LocalDateTime parseFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        return LocalDateTime.parse(texto.trim(), FECHA_HORA);
    }

    static String nombreRecurrencia(Recurrencia recurrencia) {
        return switch (recurrencia) {
            case NINGUNA -> "No se repite";
            case DIARIA -> "Diaria";
            case SEMANAL -> "Semanal";
            case MENSUAL -> "Mensual";
        };
    }

    static void mostrarFeedback(Label etiqueta, String texto, boolean exito) {
        etiqueta.setText(texto);
        etiqueta.setAccessibleText(texto);
        etiqueta.getStyleClass().removeAll("feedback--ok", "feedback--error");
        etiqueta.getStyleClass().add(exito ? "feedback--ok" : "feedback--error");
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
    }

    static void ocultarFeedback(Label etiqueta) {
        etiqueta.setText("");
        etiqueta.setAccessibleText("");
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
    }

    /** Glifo Feather (Ikonli) listo para usar como grafico de boton o etiqueta. */
    static FontIcon icono(Ikon ikon, int tamano) {
        FontIcon glifo = new FontIcon(ikon);
        glifo.setIconSize(tamano);
        return glifo;
    }

    /** Titulo de seccion con glifo pequeno a la izquierda. */
    static Label seccion(String texto, Ikon ikon) {
        Label titulo = new Label(texto);
        titulo.getStyleClass().add("section-title");
        titulo.setGraphic(icono(ikon, 18));
        titulo.setGraphicTextGap(10);
        return titulo;
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
