package interfaz;

import interfaz.vistas.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Contenedor del monedero. Amplio: barra lateral propia (navegación y pase,
 * cada zona con scroll) y contenido a la derecha. Compacto: un único scroll
 * vertical con todo apilado, para que ninguna sección quede fuera de alcance.
 */
final class DashboardView extends BorderPane {

    private static final double ANCHO_COMPACTO = 1040;

    private final Banco banco;
    private final Cliente cliente;
    private final Consumer<Boolean> alTema;
    private final boolean oscuro;
    private final PassCard pase = new PassCard(false);
    private final StackPane contenido = new StackPane();
    private final Map<String, Vista> vistas = new LinkedHashMap<>();
    private final ToggleGroup navegacion = new ToggleGroup();
    private final List<ToggleButton> botonesNav = new ArrayList<>();
    private final VBox topBox = new VBox(14);
    private final HBox cabecera;
    private final Label sesionLabel = new Label();
    private final FlowPane barraNavegacion = new FlowPane(6, 6);
    private final VBox columnaNav = new VBox(6);
    private final VBox paginaCompacta = new VBox(14);
    private final VBox contenidoLateral = new VBox(20);
    private final ScrollPane scrollInterno = new ScrollPane();
    private final ScrollPane scrollLateral = new ScrollPane();
    private final ScrollPane scrollExterno = new ScrollPane();

    private Vista vistaActual;
    private boolean compacto;
    private boolean inicializado;

    DashboardView(Banco banco, Cliente cliente, Runnable alSalir,
                  Consumer<Boolean> alTema, boolean oscuro) {
        this.banco = banco;
        this.cliente = cliente;
        this.alTema = alTema;
        this.oscuro = oscuro;
        getStyleClass().add("dashboard-view");
        if (oscuro) getStyleClass().add("theme-dark");

        pase.setAnchoPreferido(340);
        cabecera = construirCabecera(alSalir);
        registrarVistas();
        construirNavegacion();

        topBox.getChildren().add(cabecera);
        barraNavegacion.getStyleClass().add("dashboard-nav");
        columnaNav.getStyleClass().add("columna-nav");
        paginaCompacta.getStyleClass().add("dashboard-page");
        contenidoLateral.getStyleClass().add("dashboard-sidebar");
        contenido.getStyleClass().add("dashboard-content");
        contenido.setAlignment(Pos.TOP_LEFT);
        prepararScroll(scrollInterno, contenido);
        prepararScroll(scrollLateral, contenidoLateral);
        prepararScroll(scrollExterno, paginaCompacta);

        BorderPane.setMargin(topBox, new Insets(0, 0, 16, 0));
        setTop(topBox);
        setLeft(scrollLateral);
        setCenter(scrollInterno);
        seleccionar("resumen");

        widthProperty().addListener((obs, anterior, actual) -> adaptar(actual.doubleValue()));
        Platform.runLater(() -> adaptar(getWidth()));
    }

    private void registrarVistas() {
        vistas.put("resumen", new ResumenView(banco, cliente));
        vistas.put("movimientos", new MovimientosView(banco, cliente));
        vistas.put("operaciones", new OperacionesView(banco, cliente, this::actualizar));
        vistas.put("puntos", new PuntosView(banco, cliente, this::actualizar));
        vistas.put("ranking", new RankingView(banco, cliente));
        vistas.put("reversion", new ReversionView(banco, cliente, this::actualizar));
        vistas.put("monederos", new MonederosView(banco, cliente, this::actualizar));
        vistas.put("programadas", new ProgramadasView(banco, cliente, this::actualizar));
        vistas.put("avisos", new NotificacionesView(banco, cliente, this::actualizar));
        vistas.put("reportes", new ReportesView(banco, cliente));
        vistas.put("integridad", new IntegridadView(banco, cliente, this::actualizar));
    }

    private HBox construirCabecera(Runnable alSalir) {
        Label marca = new Label("Monedero");
        marca.getStyleClass().addAll("wordmark", "wordmark--small");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        sesionLabel.setText("Sesión de " + cliente.getNombre());
        sesionLabel.getStyleClass().add("hint");

        Button tema = new Button(oscuro ? "Tema claro" : "Tema oscuro");
        tema.getStyleClass().add("secondary-button");
        tema.setGraphic(Ui.icono(oscuro ? Feather.SUN : Feather.MOON, 14));
        tema.setAccessibleText(oscuro ? "Cambiar al tema claro" : "Cambiar al tema oscuro");
        tema.setOnAction(evento -> alTema.accept(!oscuro));

        Button salir = new Button("Salir");
        salir.getStyleClass().add("secondary-button");
        salir.setGraphic(Ui.icono(Feather.LOG_OUT, 14));
        salir.setOnAction(evento -> alSalir.run());

        HBox header = new HBox(12, marca, espacio, sesionLabel, tema, salir);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dashboard-header");
        return header;
    }

    private void construirNavegacion() {
        String[][] items = {
                {"resumen", "Resumen"},
                {"movimientos", "Movimientos"},
                {"operaciones", "Operaciones"},
                {"puntos", "Puntos"},
                {"ranking", "Ranking"},
                {"reversion", "Reversión"},
                {"monederos", "Monederos"},
                {"programadas", "Programadas"},
                {"avisos", "Avisos"},
                {"reportes", "Reportes"},
                {"integridad", "Integridad"}
        };

        Feather[] glifos = {
                Feather.HOME, Feather.LIST, Feather.CREDIT_CARD, Feather.STAR,
                Feather.AWARD, Feather.ROTATE_CCW, Feather.POCKET, Feather.CALENDAR,
                Feather.BELL, Feather.BAR_CHART_2, Feather.SHIELD
        };

        for (int i = 0; i < items.length; i++) {
            ToggleButton boton = new ToggleButton(items[i][1]);
            boton.setGraphic(Ui.icono(glifos[i], 15));
            boton.getStyleClass().add("nav-tab");
            boton.setUserData(items[i][0]);
            boton.setToggleGroup(navegacion);
            boton.setAccessibleText("Ver " + items[i][1]);
            botonesNav.add(boton);
        }

        navegacion.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                if (anterior != null) anterior.setSelected(true);
                return;
            }
            mostrar((String) actual.getUserData());
        });
    }

    private void colocarNavegacion(boolean vertical) {
        columnaNav.getChildren().clear();
        barraNavegacion.getChildren().clear();
        for (ToggleButton boton : botonesNav) {
            boton.setMaxWidth(vertical ? Double.MAX_VALUE : Region.USE_PREF_SIZE);
            (vertical ? columnaNav : barraNavegacion).getChildren().add(boton);
        }
    }

    private void prepararScroll(ScrollPane scroll, Node contenido) {
        scroll.getStyleClass().add("content-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setContent(contenido);
    }

    /**
     * Modo amplio: el contenido mide al menos lo que ve el viewport, para que
     * las listas con VGrow llenen el alto disponible y solo exista scroll
     * cuando el contenido natural es mas alto.
     */
    private void vincularAlturaMinima() {
        contenido.minHeightProperty().unbind();
        contenido.minHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            Bounds area = scrollInterno.getViewportBounds();
            return area == null ? 0.0 : Math.max(0.0, area.getHeight() - 2);
        }, scrollInterno.viewportBoundsProperty()));
    }

    private void seleccionar(String clave) {
        for (Toggle boton : navegacion.getToggles()) {
            if (clave.equals(boton.getUserData())) {
                boton.setSelected(true);
                return;
            }
        }
    }

    private void mostrar(String clave) {
        Vista nueva = vistas.get(clave);
        if (nueva == null) return;
        vistaActual = nueva;
        nueva.actualizar();
        Node raiz = nueva.raiz();
        raiz.setOpacity(0);
        contenido.getChildren().setAll(raiz);
        Timeline aparicion = new Timeline(new KeyFrame(Duration.millis(140),
                new KeyValue(raiz.opacityProperty(), 1, Interpolator.EASE_OUT)));
        aparicion.play();
        actualizarPase();
    }

    private void actualizar() {
        actualizarPase();
        if (vistaActual != null) {
            vistaActual.actualizar();
        }
    }

    private void actualizarPase() {
        pase.actualizar(banco.consultarResumenVista(cliente.getId()));
    }

    private void adaptar(double ancho) {
        boolean nuevoCompacto = ancho < ANCHO_COMPACTO;
        if (inicializado && nuevoCompacto == compacto) return;
        compacto = nuevoCompacto;
        inicializado = true;

        pase.setCompacto(compacto);
        sesionLabel.setVisible(!compacto);
        sesionLabel.setManaged(!compacto);

        if (compacto) {
            if (!getStyleClass().contains("dashboard-view--compact")) {
                getStyleClass().add("dashboard-view--compact");
            }
            contenido.minHeightProperty().unbind();
            contenidoLateral.getChildren().clear();
            setTop(null);
            setLeft(null);
            colocarNavegacion(false);
            paginaCompacta.getChildren().setAll(cabecera, barraNavegacion, pase, contenido);
            setCenter(scrollExterno);
        } else {
            getStyleClass().remove("dashboard-view--compact");
            paginaCompacta.getChildren().clear();
            colocarNavegacion(true);
            contenidoLateral.getChildren().setAll(columnaNav, pase);
            setTop(topBox);
            setLeft(scrollLateral);
            setCenter(scrollInterno);
            vincularAlturaMinima();
            BorderPane.setMargin(scrollLateral, new Insets(0, 28, 0, 0));
        }
    }
}
