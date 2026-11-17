package interfaz;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import servicio.dto.ResumenDto;

import java.io.InputStream;
import org.kordamp.ikonli.feather.Feather;

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

        Region perforacion = new Region();
        perforacion.getStyleClass().add("pass-perforation");

        getChildren().addAll(construirCabecera(modoLogin), construirLema(modoLogin));
        if (!modoLogin) getChildren().add(construirDatos());
        getChildren().addAll(construirRangos(modoLogin), perforacion, construirPie(modoLogin));
    }

    void actualizar(ResumenDto resumen) {
        saldoValor.setText(Ui.monto(resumen.saldo()));
        puntosValor.setText(Ui.puntos(resumen.puntosDisponibles()) + " pts");
        rangoValor.setText(resumen.rango());
        acumuladosValor.setText(Ui.puntos(resumen.puntosAcumulados()) + " pts acumulados");
        ladder.setRango(resumen.rango());
        progreso.setProgress(resumen.progreso());
        siguiente.setText(resumen.siguienteRango() == null
                ? "Estás en el rango máximo."
                : "Faltan " + Ui.puntos(resumen.faltanParaSiguiente()) + " pts para "
                        + resumen.siguienteRango() + ".");
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
        setPrefWidth(compacto ? Region.USE_COMPUTED_SIZE : anchoPreferido);
    }

    void setAnchoPreferido(double ancho) {
        anchoPreferido = ancho;
        if (!compacto) {
            setPrefWidth(ancho);
            setMaxWidth(ancho);
        }
    }

    private HBox construirCabecera(boolean modoLogin) {
        Label marca = new Label("Monedero");
        marca.getStyleClass().add("wordmark");

        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);

        Label contexto = new Label(modoLogin ? "Acceso demo" : "Cuenta activa");
        contexto.getStyleClass().add("pass-kicker");

        HBox cabecera = new HBox(12, marca, separador, contexto);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ImageView sello = crearSello();
        if (sello != null) {
            cabecera.getChildren().add(0, sello);
        }
        return cabecera;
    }

    /** Sello hanko del proyecto; se omite en silencio si el recurso falta. */
    private ImageView crearSello() {
        InputStream flujo = PassCard.class.getResourceAsStream("/interfaz/images/sello-monedero.png");
        if (flujo == null) return null;
        Image imagen = new Image(flujo, 28, 28, true, true);
        if (imagen.isError()) return null;
        ImageView sello = new ImageView(imagen);
        sello.getStyleClass().add("pass-seal");
        sello.setAccessibleRole(javafx.scene.AccessibleRole.IMAGE_VIEW);
        return sello;
    }

    private Label construirLema(boolean modoLogin) {
        Label lema = new Label(modoLogin
                ? "Saldo, puntos y rangos en una sola cuenta."
                : "Tu saldo y tus puntos, sin separarlos.");
        lema.getStyleClass().add("pass-lead");
        lema.setWrapText(true);
        return lema;
    }

    private VBox construirDatos() {
        Label saldoEtiqueta = new Label("Saldo disponible");
        saldoEtiqueta.getStyleClass().add("pass-label");
        saldoEtiqueta.setGraphic(Ui.icono(Feather.CREDIT_CARD, 11));
        saldoValor.getStyleClass().add("pass-balance");

        Label puntosEtiqueta = new Label("Puntos disponibles");
        puntosEtiqueta.getStyleClass().add("pass-label");
        puntosEtiqueta.setGraphic(Ui.icono(Feather.STAR, 11));
        puntosValor.getStyleClass().add("pass-points");

        Label rangoEtiqueta = new Label("Rango actual");
        rangoEtiqueta.getStyleClass().add("pass-label");
        rangoEtiqueta.setGraphic(Ui.icono(Feather.AWARD, 11));
        rangoValor.getStyleClass().add("pass-range");

        VBox saldoBloque = new VBox(2, saldoEtiqueta, saldoValor);
        VBox puntosBloque = new VBox(2, puntosEtiqueta, puntosValor);
        VBox rangoBloque = new VBox(2, rangoEtiqueta, rangoValor);

        HBox resumen = new HBox(24, puntosBloque, rangoBloque);
        resumen.setAlignment(Pos.TOP_LEFT);

        acumuladosValor.getStyleClass().add("pass-hint");

        VBox datos = new VBox(10, saldoBloque, resumen, acumuladosValor);
        datos.getStyleClass().add("pass-data");
        return datos;
    }

    private VBox construirRangos(boolean modoLogin) {
        Label titulo = new Label("Rangos");
        titulo.getStyleClass().add("pass-label");
        titulo.setGraphic(Ui.icono(Feather.TRENDING_UP, 11));

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

        VBox bloque = new VBox(8, titulo, ladder, siguiente, progreso);
        bloque.getStyleClass().add("range-block");
        return bloque;
    }

    private Label construirPie(boolean modoLogin) {
        Label pie = new Label(modoLogin
                ? "Entra con tu ID para ver tu rango."
                : "Los puntos se calculan por cada 100 unidades.");
        pie.getStyleClass().add("pass-foot");
        pie.setWrapText(true);
        return pie;
    }

}
