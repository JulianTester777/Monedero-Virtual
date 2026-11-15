package interfaz;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import modelo.Beneficio;
import modelo.Cliente;
import modelo.CuentaPuntos;
import modelo.EntradaPuntos;
import modelo.Transaccion;
import servicio.Banco;
import servicio.OperacionInvalidaException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Consumer;

/** Panel principal: estado de la cuenta, movimientos y operaciones. */
final class DashboardView extends BorderPane {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alSalir;
    private final PassCard pase = new PassCard(false);
    private final ListView<Transaccion> lista = new ListView<>();
    private final VBox rankingBox = new VBox();
    private final Label feedback = new Label();
    private final TextField montoCampo = new TextField();
    private final TextField destinoCampo = new TextField();
    private final ComboBox<Beneficio> beneficioCombo = new ComboBox<>();
    private final VBox grupoMonto = new VBox(6);
    private final VBox grupoDestino = new VBox(6);
    private final VBox grupoBeneficio = new VBox(6);
    private final Label ayuda = new Label();
    private final Button ejecutar = new Button();
    private final ToggleGroup operaciones = new ToggleGroup();
    private final VBox topBox = new VBox(16);
    private final HBox filaPaneles = new HBox(24);
    private final VBox paneles = new VBox(24);
    private final HBox cabecera;
    private final Label sesionLabel = new Label();
    private VBox panelOperacion;
    private VBox panelRanking;

    private String operacion = "depositar";
    private boolean compacto;
    private boolean inicializado;

    DashboardView(Banco banco, Cliente cliente, Runnable alSalir) {
        this.banco = banco;
        this.cliente = cliente;
        this.alSalir = alSalir;
        getStyleClass().add("dashboard-view");

        pase.setAnchoPreferido(360);
        cabecera = construirCabecera();
        panelOperacion = construirPanelOperacion();
        panelRanking = construirPanelRanking();
        HBox.setHgrow(panelOperacion, Priority.ALWAYS);
        paneles.getChildren().addAll(panelOperacion, panelRanking);

        setTop(cabecera);
        setLeft(pase);
        BorderPane.setMargin(pase, new Insets(0, 36, 0, 0));
        setCenter(construirScroll());
        actualizar();

        widthProperty().addListener((obs, anterior, actual) -> adaptar(actual.doubleValue()));
        Platform.runLater(() -> adaptar(getWidth()));
    }

    private HBox construirCabecera() {
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

    private ScrollPane construirScroll() {
        Label saludo = new Label("Hola, " + cliente.getNombre() + ".");
        saludo.getStyleClass().add("display-title");
        saludo.setWrapText(true);

        Label intro = new Label("Este es el estado de tu monedero. Los movimientos más recientes aparecen primero.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        Label tituloMovimientos = new Label("Movimientos");
        tituloMovimientos.getStyleClass().add("section-title");

        lista.setPrefHeight(300);
        lista.setPlaceholder(new Label("Aún no hay movimientos. Empieza con un depósito."));
        lista.setAccessibleText("Movimientos recientes");
        lista.setCellFactory(v -> new MovimientoCell());

        VBox contenido = new VBox(22, saludo, intro, tituloMovimientos, lista, paneles);
        contenido.getStyleClass().add("dashboard-main");
        contenido.setMaxWidth(920);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("content-scroll");
        return scroll;
    }

    private VBox construirPanelOperacion() {
        Label titulo = new Label("Nueva operación");
        titulo.getStyleClass().add("section-title");

        Label intro = new Label("Elige una operación y completa los datos.");
        intro.getStyleClass().add("hint");
        intro.setWrapText(true);

        ToggleButton depositar = crearToggle("Depositar", "depositar");
        ToggleButton retirar = crearToggle("Retirar", "retirar");
        ToggleButton transferir = crearToggle("Transferir", "transferir");
        ToggleButton canjear = crearToggle("Canjear", "canjear");

        FlowPane tabs = new FlowPane(6, 6, depositar, retirar, transferir, canjear);
        tabs.getStyleClass().add("operation-tabs");

        Label etiquetaMonto = new Label("Monto");
        etiquetaMonto.getStyleClass().add("field-label");
        etiquetaMonto.setLabelFor(montoCampo);

        montoCampo.getStyleClass().addAll("text-field", "amount-field");
        montoCampo.setPromptText("0,00");
        montoCampo.setAccessibleText("Monto de la operación");
        montoCampo.setOnAction(evento -> ejecutarOperacion());
        grupoMonto.getChildren().addAll(etiquetaMonto, montoCampo);
        grupoMonto.getStyleClass().add("field-group");

        Label etiquetaDestino = new Label("ID de destino");
        etiquetaDestino.getStyleClass().add("field-label");
        etiquetaDestino.setLabelFor(destinoCampo);

        destinoCampo.getStyleClass().add("text-field");
        destinoCampo.setPromptText("Ej. bruno");
        destinoCampo.setAccessibleText("ID del cliente que recibe");
        destinoCampo.setOnAction(evento -> ejecutarOperacion());
        grupoDestino.getChildren().addAll(etiquetaDestino, destinoCampo);
        grupoDestino.getStyleClass().add("field-group");

        Label etiquetaBeneficio = new Label("Beneficio");
        etiquetaBeneficio.getStyleClass().add("field-label");
        etiquetaBeneficio.setLabelFor(beneficioCombo);

        beneficioCombo.setItems(FXCollections.observableArrayList(Beneficio.values()));
        beneficioCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Beneficio beneficio) {
                return beneficio == null ? "" : Ui.beneficio(beneficio);
            }

            @Override
            public Beneficio fromString(String texto) {
                return null;
            }
        });
        beneficioCombo.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Beneficio beneficio, boolean vacio) {
                super.updateItem(beneficio, vacio);
                setText(vacio || beneficio == null ? null : Ui.beneficio(beneficio));
            }
        });
        beneficioCombo.getSelectionModel().selectFirst();
        beneficioCombo.setMaxWidth(Double.MAX_VALUE);
        beneficioCombo.setAccessibleText("Beneficio para canjear");
        grupoBeneficio.getChildren().addAll(etiquetaBeneficio, beneficioCombo);
        grupoBeneficio.getStyleClass().add("field-group");

        ayuda.getStyleClass().add("hint");
        ayuda.setWrapText(true);

        ejecutar.getStyleClass().add("primary-button");
        ejecutar.setMaxWidth(Double.MAX_VALUE);
        ejecutar.setDefaultButton(true);
        ejecutar.setOnAction(evento -> ejecutarOperacion());

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);
        feedback.setAccessibleRole(AccessibleRole.TEXT);

        operaciones.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                if (anterior != null) anterior.setSelected(true);
                return;
            }
            operacion = (String) actual.getUserData();
            actualizarFormulario();
        });
        depositar.setSelected(true);

        VBox panel = new VBox(14, titulo, intro, tabs, grupoMonto, grupoDestino, grupoBeneficio,
                ayuda, ejecutar, feedback);
        panel.getStyleClass().add("action-panel");
        panel.setPrefWidth(320);
        panel.setMaxWidth(560);
        panel.setMinWidth(0);
        return panel;
    }

    private ToggleButton crearToggle(String texto, String valor) {
        ToggleButton boton = new ToggleButton(texto);
        boton.getStyleClass().add("operation-tab");
        boton.setUserData(valor);
        boton.setToggleGroup(operaciones);
        boton.setAccessibleText("Operación: " + texto.toLowerCase(Locale.ROOT));
        return boton;
    }

    private VBox construirPanelRanking() {
        Label titulo = new Label("Ranking");
        titulo.getStyleClass().add("section-title");

        Label intro = new Label("Los puntos acumulados definen el orden.");
        intro.getStyleClass().add("hint");
        intro.setWrapText(true);

        rankingBox.setSpacing(0);

        VBox panel = new VBox(12, titulo, intro, rankingBox);
        panel.getStyleClass().add("ranking-panel");
        panel.setPrefWidth(280);
        panel.setMaxWidth(340);
        panel.setMinWidth(0);
        return panel;
    }

    private void actualizarFormulario() {
        boolean esCanje = "canjear".equals(operacion);
        boolean esTransferencia = "transferir".equals(operacion);
        boolean pideMonto = !esCanje;

        grupoMonto.setVisible(pideMonto);
        grupoMonto.setManaged(pideMonto);
        grupoDestino.setVisible(esTransferencia);
        grupoDestino.setManaged(esTransferencia);
        grupoBeneficio.setVisible(esCanje);
        grupoBeneficio.setManaged(esCanje);

        ayuda.setText(switch (operacion) {
            case "depositar" -> "El depósito suma saldo y 1 punto por cada 100 unidades.";
            case "retirar" -> "El retiro descuenta saldo y comisión; suma 2 puntos por cada 100.";
            case "transferir" -> "La comisión la paga quien envía. Suma 3 puntos por cada 100.";
            case "canjear" -> "Usa tus puntos disponibles. El canje no cambia tus puntos acumulados.";
            default -> "";
        });
        ejecutar.setText(switch (operacion) {
            case "depositar" -> "Depositar";
            case "retirar" -> "Retirar";
            case "transferir" -> "Transferir";
            case "canjear" -> "Canjear beneficio";
            default -> "Ejecutar";
        });

        ocultarFeedback();
        if (pideMonto) {
            montoCampo.requestFocus();
        } else {
            beneficioCombo.requestFocus();
        }
    }

    private void ejecutarOperacion() {
        ocultarFeedback();
        try {
            if ("canjear".equals(operacion)) {
                Beneficio beneficio = beneficioCombo.getValue();
                if (beneficio == null) {
                    mostrarFeedback("Elige un beneficio para canjear.", false);
                    return;
                }
                banco.canjearBeneficio(cliente.getId(), beneficio);
                mostrarFeedback("Canje realizado: " + Ui.beneficio(beneficio) + ".", true);
            } else {
                BigDecimal monto = Ui.parseMonto(montoCampo.getText());
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    mostrarFeedback("El monto debe ser mayor que cero.", false);
                    return;
                }
                switch (operacion) {
                    case "depositar" -> {
                        banco.depositar(cliente.getId(), monto);
                        mostrarFeedback("Depósito de " + Ui.monto(monto) + " realizado.", true);
                    }
                    case "retirar" -> {
                        banco.retirar(cliente.getId(), monto);
                        mostrarFeedback("Retiro de " + Ui.monto(monto) + " realizado.", true);
                    }
                    case "transferir" -> {
                        String destino = destinoCampo.getText() == null
                                ? "" : destinoCampo.getText().trim().toLowerCase(Locale.ROOT);
                        if (destino.isEmpty()) {
                            mostrarFeedback("Escribe el ID del cliente que recibe.", false);
                            return;
                        }
                        banco.transferir(cliente.getId(), destino, monto);
                        mostrarFeedback("Transferencia de " + Ui.monto(monto) + " a " + destino + " realizada.", true);
                    }
                    default -> { }
                }
                montoCampo.clear();
            }
            actualizar();
        } catch (NumberFormatException ex) {
            mostrarFeedback("Escribe un monto válido, por ejemplo 250,00.", false);
        } catch (OperacionInvalidaException ex) {
            mostrarFeedback(ex.getMessage(), false);
        }
    }

    private void actualizar() {
        CuentaPuntos cuenta = banco.consultarPuntos(cliente.getId());
        pase.actualizar(banco.consultarSaldo(cliente.getId()), cuenta);

        ObservableList<Transaccion> movimientos = FXCollections.observableArrayList();
        for (Transaccion transaccion : banco.consultarHistorial(cliente.getId())) {
            movimientos.add(0, transaccion);
        }
        lista.setItems(movimientos);
        actualizarRanking();
    }

    private void actualizarRanking() {
        rankingBox.getChildren().clear();
        int posicion = 1;
        for (EntradaPuntos entrada : banco.ranking(5)) {
            HBox fila = new HBox(12);
            fila.getStyleClass().add("ranking-row");
            fila.setAlignment(Pos.CENTER_LEFT);

            Label puesto = new Label(String.valueOf(posicion++));
            puesto.getStyleClass().add("ranking-position");

            Label nombre = new Label(banco.buscarCliente(entrada.getClienteId()).getNombre());
            nombre.getStyleClass().add("ranking-name");

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label puntos = new Label(Ui.puntos(entrada.getPuntos()) + " pts");
            puntos.getStyleClass().add("ranking-points");

            if (entrada.getClienteId().equals(cliente.getId())) {
                fila.getStyleClass().add("ranking-row--me");
                nombre.setText(nombre.getText() + " (tú)");
            }

            fila.getChildren().addAll(puesto, nombre, espacio, puntos);
            rankingBox.getChildren().add(fila);
        }
        if (rankingBox.getChildren().isEmpty()) {
            Label vacio = new Label("Todavía no hay clientes en el ranking.");
            vacio.getStyleClass().add("hint");
            rankingBox.getChildren().add(vacio);
        }
    }

    private void mostrarFeedback(String texto, boolean exito) {
        feedback.setText(texto);
        feedback.setAccessibleText(texto);
        feedback.getStyleClass().removeAll("feedback--ok", "feedback--error");
        feedback.getStyleClass().add(exito ? "feedback--ok" : "feedback--error");
        feedback.setVisible(true);
        feedback.setManaged(true);
        feedback.setAccessibleRole(AccessibleRole.TEXT);
    }

    private void ocultarFeedback() {
        feedback.setText("");
        feedback.setAccessibleText("");
        feedback.setVisible(false);
        feedback.setManaged(false);
    }

    private String nombreDe(String id) {
        try {
            return banco.buscarCliente(id).getNombre();
        } catch (OperacionInvalidaException ex) {
            return id;
        }
    }

    private void adaptar(double ancho) {
        boolean nuevoCompacto = ancho < 1040;
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
            panelOperacion.setMaxWidth(Double.MAX_VALUE);
            panelRanking.setMaxWidth(Double.MAX_VALUE);
            filaPaneles.getChildren().clear();
            paneles.getChildren().setAll(panelOperacion, panelRanking);

            setLeft(null);
            setTop(null);
            topBox.getChildren().setAll(cabecera, pase);
            setTop(topBox);
            BorderPane.setMargin(topBox, new Insets(0, 0, 18, 0));
        } else {
            getStyleClass().remove("dashboard-view--compact");
            panelOperacion.setMaxWidth(560);
            panelRanking.setMaxWidth(340);
            paneles.getChildren().clear();
            filaPaneles.getChildren().setAll(panelOperacion, panelRanking);
            paneles.getChildren().add(filaPaneles);

            setTop(null);
            topBox.getChildren().clear();
            setTop(cabecera);
            setLeft(pase);
            BorderPane.setMargin(pase, new Insets(0, 36, 0, 0));
        }
    }

    private final class MovimientoCell extends ListCell<Transaccion> {

        @Override
        protected void updateItem(Transaccion transaccion, boolean vacio) {
            super.updateItem(transaccion, vacio);
            if (vacio || transaccion == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            Label tipo = new Label(Ui.nombreTipo(transaccion.getTipo()));
            tipo.getStyleClass().add("movement-type");

            String detalle = Ui.fecha(transaccion.getFecha());
            if (transaccion.getContraparteId() != null && !transaccion.getContraparteId().isBlank()) {
                detalle += ", " + nombreDe(transaccion.getContraparteId());
            }
            Label meta = new Label(detalle);
            meta.getStyleClass().add("movement-meta");

            VBox texto = new VBox(3, tipo, meta);
            HBox.setHgrow(texto, Priority.ALWAYS);

            boolean entrada = Ui.esEntrada(transaccion.getTipo());
            Label monto = new Label((entrada ? "+" : "−") + Ui.monto(transaccion.getMonto()));
            monto.getStyleClass().addAll("movement-amount",
                    entrada ? "movement-amount--in" : "movement-amount--out");

            VBox dinero = new VBox(2, monto);
            dinero.setAlignment(Pos.CENTER_RIGHT);
            if (transaccion.getComision().signum() > 0) {
                Label comision = new Label("Comisión " + Ui.monto(transaccion.getComision()));
                comision.getStyleClass().add("movement-meta");
                dinero.getChildren().add(comision);
            }

            HBox fila = new HBox(16, texto, dinero);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("movement-row");

            setGraphic(fila);
            setText(null);
            setAccessibleText(Ui.nombreTipo(transaccion.getTipo()) + ", " + monto.getText() + ", " + detalle);
        }
    }
}
