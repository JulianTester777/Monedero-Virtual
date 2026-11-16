package interfaz;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import modelo.Cliente;
import servicio.Banco;

import java.util.LinkedHashMap;
import java.util.Map;

/** Contenedor del monedero: cabecera, navegación entre vistas y tarjeta de cuenta. */
final class DashboardView extends BorderPane {

    private static final double ANCHO_COMPACTO = 1040;

    private final Banco banco;
    private final Cliente cliente;
    private final PassCard pase = new PassCard(false);
    private final StackPane contenido = new StackPane();
    private final Map<String, Vista> vistas = new LinkedHashMap<>();
    private final ToggleGroup navegacion = new ToggleGroup();
    private final VBox topBox = new VBox(14);
    private final Label sesionLabel = new Label();
    private final HBox cabecera;
    private final FlowPane barraNavegacion;

    private Vista vistaActual;
    private boolean compacto;
    private boolean inicializado;

    DashboardView(Banco banco, Cliente cliente, Runnable alSalir) {
        this.banco = banco;
        this.cliente = cliente;
        getStyleClass().add("dashboard-view");

        pase.setAnchoPreferido(340);
        cabecera = construirCabecera(alSalir);
        registrarVistas();
        barraNavegacion = construirNavegacion();

        topBox.getChildren().addAll(cabecera, barraNavegacion);
        setTop(topBox);
        setLeft(pase);
        BorderPane.setMargin(pase, new Insets(0, 36, 0, 0));
        setCenter(construirContenido());
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

        Button salir = new Button("Salir");
        salir.getStyleClass().add("secondary-button");
        salir.setOnAction(evento -> alSalir.run());

        HBox header = new HBox(16, marca, espacio, sesionLabel, salir);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dashboard-header");
        return header;
    }

    private FlowPane construirNavegacion() {
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

        FlowPane barra = new FlowPane(6, 6);
        barra.getStyleClass().add("dashboard-nav");
        barra.setAlignment(Pos.CENTER_LEFT);
        for (String[] item : items) {
            ToggleButton boton = new ToggleButton(item[1]);
            boton.getStyleClass().add("nav-tab");
            boton.setUserData(item[0]);
            boton.setToggleGroup(navegacion);
            boton.setAccessibleText("Ver " + item[1]);
            barra.getChildren().add(boton);
        }

        navegacion.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                if (anterior != null) anterior.setSelected(true);
                return;
            }
            mostrar((String) actual.getUserData());
        });
        return barra;
    }

    private ScrollPane construirContenido() {
        contenido.getStyleClass().add("dashboard-content");
        contenido.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("content-scroll");
        return scroll;
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
        contenido.getChildren().setAll(nueva.raiz());
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
            aplicarCompacto();
        } else {
            aplicarAmplio();
        }
    }

    private void aplicarCompacto() {
        if (!getStyleClass().contains("dashboard-view--compact")) {
            getStyleClass().add("dashboard-view--compact");
        }
        setLeft(null);
        topBox.getChildren().setAll(cabecera, barraNavegacion, pase);
        BorderPane.setMargin(topBox, new Insets(0, 0, 18, 0));
    }

    private void aplicarAmplio() {
        getStyleClass().remove("dashboard-view--compact");
        topBox.getChildren().setAll(cabecera, barraNavegacion);
        BorderPane.setMargin(topBox, new Insets(0, 0, 14, 0));
        setLeft(pase);
        BorderPane.setMargin(pase, new Insets(0, 36, 0, 0));
    }
}
