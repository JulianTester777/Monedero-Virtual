package interfaz;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.CuentaPuntos;
import modelo.Rango;

import java.math.BigDecimal;

/** Tarjeta de pase: identidad, saldo y escalera de rangos. */
final class PassCard extends VBox {

    private final Label saldoValor = new Label("—");
    private final Label puntosValor = new Label("—");
    private final Label rangoValor = new Label("—");
    private final Label acumuladosValor = new Label();
    private final Label siguiente = new Label();
    private final ProgressBar progreso = new ProgressBar(0);
    private final RangeLadder ladder = new RangeLadder();

    private boolean compacto;
    private double anchoPreferido = 400;

    PassCard(boolean modoLogin) {
        getStyleClass().add("pass-card");
        if (modoLogin) getStyleClass().add("pass-card--intro");
        setSpacing(18);
        setPadding(new Insets(30));
        setPrefWidth(anchoPreferido);
        setMaxWidth(anchoPreferido);

        Label marca = new Label("Monedero");
        marca.getStyleClass().add("wordmark");

        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);

        Label contexto = new Label(modoLogin ? "Acceso demo" : "Cuenta activa");
        contexto.getStyleClass().add("pass-kicker");

        HBox cabecera = new HBox(12, marca, separador, contexto);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label lema = new Label(modoLogin
                ? "Saldo, puntos y rangos en una sola cuenta."
                : "Tu saldo y tus puntos, sin separarlos.");
        lema.getStyleClass().add("pass-lead");
        lema.setWrapText(true);

        Label saldoEtiqueta = new Label("Saldo disponible");
        saldoEtiqueta.getStyleClass().add("pass-label");
        saldoValor.getStyleClass().add("pass-balance");

        Label puntosEtiqueta = new Label("Puntos disponibles");
        puntosEtiqueta.getStyleClass().add("pass-label");
        puntosValor.getStyleClass().add("pass-points");

        Label rangoEtiqueta = new Label("Rango actual");
        rangoEtiqueta.getStyleClass().add("pass-label");
        rangoValor.getStyleClass().add("pass-range");

        VBox saldoBloque = new VBox(2, saldoEtiqueta, saldoValor);
        VBox puntosBloque = new VBox(2, puntosEtiqueta, puntosValor);
        VBox rangoBloqueDato = new VBox(2, rangoEtiqueta, rangoValor);

        HBox resumen = new HBox(24, puntosBloque, rangoBloqueDato);
        resumen.setAlignment(Pos.TOP_LEFT);

        acumuladosValor.getStyleClass().add("pass-hint");

        VBox datos = new VBox(10, saldoBloque, resumen, acumuladosValor);
        datos.getStyleClass().add("pass-data");
        if (modoLogin) {
            datos.setVisible(false);
            datos.setManaged(false);
        }

        Label rangosTitulo = new Label("Rangos");
        rangosTitulo.getStyleClass().add("pass-label");

        siguiente.getStyleClass().add("pass-hint");
        siguiente.setWrapText(true);

        progreso.getStyleClass().add("range-progress");
        progreso.setPrefHeight(6);
        progreso.setMaxWidth(Double.MAX_VALUE);
        if (modoLogin) {
            progreso.setVisible(false);
            progreso.setManaged(false);
            siguiente.setText("El rango se calcula con tus puntos acumulados.");
            ladder.setRango(null);
        }

        VBox rangoBloque = new VBox(8, rangosTitulo, ladder, siguiente, progreso);

        Region perforacion = new Region();
        perforacion.getStyleClass().add("pass-perforation");

        Label pie = new Label(modoLogin
                ? "Entra con tu ID para ver tu rango."
                : "Los puntos se calculan por cada 100 unidades.");
        pie.getStyleClass().add("pass-foot");
        pie.setWrapText(true);

        getChildren().addAll(cabecera, lema);
        if (!modoLogin) getChildren().add(datos);
        getChildren().addAll(rangoBloque, perforacion, pie);
    }

    void actualizar(BigDecimal saldo, CuentaPuntos cuenta) {
        Rango rango = cuenta.getRango();
        saldoValor.setText(Ui.monto(saldo));
        puntosValor.setText(Ui.puntos(cuenta.getDisponibles()) + " pts");
        rangoValor.setText(rango.getNombre());
        acumuladosValor.setText(Ui.puntos(cuenta.getAcumulados()) + " pts acumulados");
        ladder.setRango(rango);
        actualizarProgreso(cuenta.getAcumulados(), rango);
    }

    void setCompacto(boolean compacto) {
        this.compacto = compacto;
        if (compacto) {
            if (!getStyleClass().contains("pass-card--compact")) getStyleClass().add("pass-card--compact");
        } else {
            getStyleClass().remove("pass-card--compact");
        }
        ladder.setCompacto(compacto);
        setMaxWidth(compacto ? Double.MAX_VALUE : anchoPreferido);
        setPrefWidth(compacto ? 640 : anchoPreferido);
    }

    void setAnchoPreferido(double ancho) {
        anchoPreferido = ancho;
        if (!compacto) {
            setPrefWidth(ancho);
            setMaxWidth(ancho);
        }
    }

    private void actualizarProgreso(int acumulados, Rango rango) {
        if (rango == Rango.PLATINO) {
            progreso.setProgress(1);
            siguiente.setText("Estás en el rango máximo.");
            return;
        }
        Rango siguienteRango = Rango.values()[rango.ordinal() + 1];
        int base = rango.getDesde();
        int meta = rango.getHasta() + 1;
        double avance = (acumulados - base) / (double) (meta - base);
        progreso.setProgress(Math.max(0, Math.min(1, avance)));
        int faltan = Math.max(0, meta - acumulados);
        siguiente.setText("Faltan " + Ui.puntos(faltan) + " pts para " + siguienteRango.getNombre() + ".");
    }
}
