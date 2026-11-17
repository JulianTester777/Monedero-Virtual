package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.BeneficioDto;
import servicio.dto.MovimientoPuntosDto;
import servicio.dto.ResumenDto;

import java.util.HashMap;
import java.util.Map;

/** Detalle de puntos: progreso de rango, historial y canje de beneficios. */
final class PuntosView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final Label disponibles = new Label();
    private final Label acumulados = new Label();
    private final Label rango = new Label();
    private final Label siguiente = new Label();
    private final ProgressBar progreso = new ProgressBar(0);
    private final ListView<MovimientoPuntosDto> historial = new ListView<>();
    private final Label feedback = new Label();
    private final Map<String, Button> botones = new HashMap<>();
    private final Map<String, Label> estados = new HashMap<>();
    private final VBox raiz;

    PuntosView(Banco banco, Cliente cliente, Runnable alCambiar) {
        this.banco = banco;
        this.cliente = cliente;
        this.alCambiar = alCambiar;
        raiz = construir();
    }

    @Override
    public Node raiz() {
        return raiz;
    }

    @Override
    public void actualizar() {
        ResumenDto resumen = banco.consultarResumenVista(cliente.getId());
        disponibles.setText(Ui.puntos(resumen.puntosDisponibles()) + " pts");
        acumulados.setText(Ui.puntos(resumen.puntosAcumulados()) + " pts");
        rango.setText(resumen.rango());
        progreso.setProgress(resumen.progreso());
        siguiente.setText(resumen.siguienteRango() == null
                ? "Estás en el rango máximo."
                : "Faltan " + Ui.puntos(resumen.faltanParaSiguiente()) + " pts para "
                        + resumen.siguienteRango() + ".");
        historial.setItems(movimientos());
        actualizarBeneficios();
    }

    private VBox construir() {
        Label titulo = new Label("Puntos y rangos");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Gana puntos con cada operación y canjéalos por beneficios.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        FlowPane tarjetas = new FlowPane(14, 14,
                tarjeta("Disponibles", disponibles, Feather.STAR),
                tarjeta("Acumulados", acumulados, Feather.TRENDING_UP),
                tarjeta("Rango actual", rango, Feather.AWARD));
        tarjetas.getStyleClass().add("stat-grid");

        VBox contenido = new VBox(22, titulo, intro, tarjetas,
                construirProgreso(), construirHistorial(), construirBeneficios());
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private VBox tarjeta(String etiqueta, Label valor, Feather glifo) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("stat-label");
        titulo.setGraphic(Ui.icono(glifo, 12));
        valor.getStyleClass().add("stat-value");

        VBox tarjeta = new VBox(6, titulo, valor);
        tarjeta.getStyleClass().add("stat-card");
        return tarjeta;
    }

    private VBox construirProgreso() {
        Label titulo = Ui.seccion("Progreso de rango", Feather.TRENDING_UP);

        siguiente.getStyleClass().add("hint");
        siguiente.setWrapText(true);

        progreso.getStyleClass().add("points-progress");
        progreso.setMaxWidth(Double.MAX_VALUE);

        VBox bloque = new VBox(8, titulo, siguiente, progreso);
        bloque.getStyleClass().add("view-section");
        return bloque;
    }

    private VBox construirHistorial() {
        Label titulo = Ui.seccion("Movimientos de puntos", Feather.LIST);

        historial.setPrefHeight(240);
        historial.setPlaceholder(new Label("Todavía no has ganado puntos."));
        historial.setAccessibleText("Historial de puntos");
        historial.setCellFactory(v -> new PuntosCell());
        VBox.setVgrow(historial, Priority.ALWAYS);

        VBox bloque = new VBox(10, titulo, historial);
        bloque.getStyleClass().add("view-section");
        return bloque;
    }

    private VBox construirBeneficios() {
        Label titulo = Ui.seccion("Beneficios", Feather.GIFT);

        Label intro = new Label("Canjea tus puntos disponibles. Los acumulados no bajan.");
        intro.getStyleClass().add("hint");
        intro.setWrapText(true);

        FlowPane tarjetas = new FlowPane(14, 14);
        tarjetas.getStyleClass().add("benefit-grid");
        for (BeneficioDto beneficio : banco.consultarBeneficiosVista(cliente.getId())) {
            tarjetas.getChildren().add(crearBeneficio(beneficio));
        }

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);

        VBox bloque = new VBox(10, titulo, intro, tarjetas, feedback);
        bloque.getStyleClass().add("view-section");
        return bloque;
    }

    private VBox crearBeneficio(BeneficioDto beneficio) {
        Label titulo = new Label(beneficio.nombre());
        titulo.getStyleClass().add("benefit-title");

        Label descripcion = new Label(beneficio.descripcion());
        descripcion.getStyleClass().add("benefit-description");
        descripcion.setWrapText(true);

        Label costo = new Label(Ui.puntos(beneficio.costo()) + " pts");
        costo.getStyleClass().add("benefit-cost");

        Label estado = new Label();
        estado.getStyleClass().add("benefit-status");
        estado.setVisible(false);
        estado.setManaged(false);

        Button canjear = new Button("Canjear");
        canjear.getStyleClass().add("secondary-button");
        canjear.setMaxWidth(Double.MAX_VALUE);
        canjear.setOnAction(evento -> canjear(beneficio));
        canjear.setAccessibleText("Canjear " + beneficio.nombre());

        botones.put(beneficio.nombre(), canjear);
        estados.put(beneficio.nombre(), estado);

        VBox tarjeta = new VBox(8, titulo, descripcion, costo, estado, canjear);
        tarjeta.getStyleClass().add("benefit-card");
        return tarjeta;
    }

    private ObservableList<MovimientoPuntosDto> movimientos() {
        ObservableList<MovimientoPuntosDto> items = FXCollections.observableArrayList();
        for (MovimientoPuntosDto movimiento : banco.consultarMovimientosPuntosVista(cliente.getId())) {
            items.add(0, movimiento);
        }
        return items;
    }

    private void actualizarBeneficios() {
        for (BeneficioDto beneficio : banco.consultarBeneficiosVista(cliente.getId())) {
            Label estado = estados.get(beneficio.nombre());
            Button boton = botones.get(beneficio.nombre());
            if (estado == null || boton == null) continue;

            estado.setVisible(beneficio.activo());
            estado.setManaged(beneficio.activo());
            if (beneficio.activo()) {
                estado.setText(beneficio.textoEstado());
                boton.setText("Activo");
                boton.setDisable(true);
            } else {
                boton.setText(beneficio.puedeCanjear()
                        ? "Canjear"
                        : "Faltan " + Ui.puntos(beneficio.faltan()) + " pts");
                boton.setDisable(!beneficio.puedeCanjear());
            }
        }
    }

    private void canjear(BeneficioDto beneficio) {
        Ui.ocultarFeedback(feedback);
        try {
            banco.canjearBeneficio(cliente.getId(), beneficio.beneficio());
            Ui.mostrarFeedback(feedback, "Canje realizado: " + beneficio.nombre() + ".", true);
            alCambiar.run();
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private final class PuntosCell extends ListCell<MovimientoPuntosDto> {

        @Override
        protected void updateItem(MovimientoPuntosDto movimiento, boolean vacio) {
            super.updateItem(movimiento, vacio);
            if (vacio || movimiento == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            Label tipo = new Label(movimiento.tipo());
            tipo.getStyleClass().add("movement-type");

            Label meta = new Label(Ui.fecha(movimiento.fecha()) + " · " + movimiento.descripcion());
            meta.getStyleClass().add("movement-meta");
            meta.setWrapText(true);

            VBox texto = new VBox(3, tipo, meta);
            HBox.setHgrow(texto, Priority.ALWAYS);

            Label puntos = new Label((movimiento.suma() ? "+" : "−")
                    + Ui.puntos(movimiento.puntos()) + " pts");
            puntos.getStyleClass().addAll("points-amount",
                    movimiento.suma() ? "points-amount--in" : "points-amount--out");

            HBox fila = new HBox(16, texto, puntos);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("movement-row");

            setGraphic(fila);
            setText(null);
            setAccessibleText(tipo.getText() + ", " + puntos.getText() + ", " + meta.getText());
        }
    }
}
