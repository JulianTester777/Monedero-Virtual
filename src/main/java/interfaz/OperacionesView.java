package interfaz;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.BeneficioDto;

import java.math.BigDecimal;
import java.util.Locale;

/** Formulario para depositar, retirar, transferir o canjear un beneficio. */
final class OperacionesView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final TextField montoCampo = new TextField();
    private final TextField destinoCampo = new TextField();
    private final ComboBox<BeneficioDto> beneficioCombo = new ComboBox<>();
    private final VBox grupoMonto = new VBox(6);
    private final VBox grupoDestino = new VBox(6);
    private final VBox grupoBeneficio = new VBox(6);
    private final Label ayuda = new Label();
    private final Button ejecutar = new Button();
    private final Label feedback = new Label();
    private final ToggleGroup operaciones = new ToggleGroup();
    private final VBox raiz;

    private String operacion = "depositar";

    OperacionesView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        if (esCanje()) {
            ayuda.setText(ayudaTexto());
        }
    }

    private VBox construir() {
        Label titulo = new Label("Nueva operación");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Elige una operación y completa los datos.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        configurarMonto();
        configurarDestino();
        configurarBeneficio();
        configurarBoton();

        VBox panel = new VBox(14, titulo, intro, construirTabs(), grupoMonto, grupoDestino,
                grupoBeneficio, ayuda, ejecutar, feedback);
        panel.getStyleClass().add("action-panel");
        panel.setPrefWidth(560);
        panel.setMaxWidth(640);
        return panel;
    }

    private FlowPane construirTabs() {
        ToggleButton depositar = crearToggle("Depositar", "depositar", Feather.PLUS_CIRCLE);
        ToggleButton retirar = crearToggle("Retirar", "retirar", Feather.MINUS_CIRCLE);
        ToggleButton transferir = crearToggle("Transferir", "transferir", Feather.REPEAT);
        ToggleButton canjear = crearToggle("Canjear", "canjear", Feather.GIFT);

        FlowPane tabs = new FlowPane(6, 6, depositar, retirar, transferir, canjear);
        tabs.getStyleClass().add("operation-tabs");

        operaciones.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                if (anterior != null) anterior.setSelected(true);
                return;
            }
            operacion = (String) actual.getUserData();
            actualizarFormulario();
        });
        depositar.setSelected(true);
        return tabs;
    }

    private ToggleButton crearToggle(String texto, String valor, Feather glifo) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setGraphic(Ui.icono(glifo, 14));
        boton.getStyleClass().add("operation-tab");
        boton.setUserData(valor);
        boton.setToggleGroup(operaciones);
        boton.setAccessibleText("Operación: " + texto.toLowerCase(Locale.ROOT));
        return boton;
    }

    private void configurarMonto() {
        Label etiqueta = new Label("Monto");
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(montoCampo);

        montoCampo.getStyleClass().addAll("text-field", "amount-field");
        montoCampo.setPromptText("0,00");
        montoCampo.setAccessibleText("Monto de la operación");
        montoCampo.setOnAction(evento -> ejecutarOperacion());

        grupoMonto.getChildren().addAll(etiqueta, montoCampo);
        grupoMonto.getStyleClass().add("field-group");
    }

    private void configurarDestino() {
        Label etiqueta = new Label("ID de destino");
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(destinoCampo);

        destinoCampo.getStyleClass().add("text-field");
        destinoCampo.setPromptText("Ej. bruno");
        destinoCampo.setAccessibleText("ID del cliente que recibe");
        destinoCampo.setOnAction(evento -> ejecutarOperacion());

        grupoDestino.getChildren().addAll(etiqueta, destinoCampo);
        grupoDestino.getStyleClass().add("field-group");
    }

    private void configurarBeneficio() {
        Label etiqueta = new Label("Beneficio");
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(beneficioCombo);

        for (BeneficioDto beneficio : banco.consultarBeneficiosVista(cliente.getId())) {
            beneficioCombo.getItems().add(beneficio);
        }
        beneficioCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(BeneficioDto beneficio) {
                return beneficio == null ? "" : textoBeneficio(beneficio);
            }

            @Override
            public BeneficioDto fromString(String texto) {
                return null;
            }
        });
        beneficioCombo.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(BeneficioDto beneficio, boolean vacio) {
                super.updateItem(beneficio, vacio);
                setText(vacio || beneficio == null ? null : textoBeneficio(beneficio));
            }
        });
        beneficioCombo.getSelectionModel().selectFirst();
        beneficioCombo.setMaxWidth(Double.MAX_VALUE);
        beneficioCombo.setAccessibleText("Beneficio para canjear");

        grupoBeneficio.getChildren().addAll(etiqueta, beneficioCombo);
        grupoBeneficio.getStyleClass().add("field-group");
    }

    private void configurarBoton() {
        ayuda.getStyleClass().add("hint");
        ayuda.setWrapText(true);

        ejecutar.getStyleClass().add("primary-button");
        ejecutar.setMaxWidth(Double.MAX_VALUE);
        ejecutar.setDefaultButton(true);
        ejecutar.setOnAction(evento -> ejecutarOperacion());

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);
    }

    private void actualizarFormulario() {
        boolean esCanje = esCanje();
        boolean esTransferencia = "transferir".equals(operacion);
        boolean pideMonto = !esCanje;

        grupoMonto.setVisible(pideMonto);
        grupoMonto.setManaged(pideMonto);
        grupoDestino.setVisible(esTransferencia);
        grupoDestino.setManaged(esTransferencia);
        grupoBeneficio.setVisible(esCanje);
        grupoBeneficio.setManaged(esCanje);

        ayuda.setText(ayudaTexto());
        ejecutar.setText(switch (operacion) {
            case "depositar" -> "Depositar";
            case "retirar" -> "Retirar";
            case "transferir" -> "Transferir";
            case "canjear" -> "Canjear beneficio";
            default -> "Ejecutar";
        });
        ejecutar.setGraphic(Ui.icono(iconoOperacion(), 15));

        Ui.ocultarFeedback(feedback);
        if (pideMonto) {
            montoCampo.requestFocus();
        } else {
            beneficioCombo.requestFocus();
        }
    }

    /** Glifo del boton ejecutar, segun la operacion elegida. */
    private Feather iconoOperacion() {
        return switch (operacion) {
            case "depositar" -> Feather.PLUS_CIRCLE;
            case "retirar" -> Feather.MINUS_CIRCLE;
            case "transferir" -> Feather.REPEAT;
            default -> Feather.GIFT;
        };
    }

    private String ayudaTexto() {
        if (esCanje()) {
            int disponibles = banco.consultarResumenVista(cliente.getId()).puntosDisponibles();
            return "Usa tus puntos disponibles (tienes " + Ui.puntos(disponibles)
                    + "). El canje no cambia tus puntos acumulados.";
        }
        return switch (operacion) {
            case "depositar" -> "El depósito suma saldo y 1 punto por cada 100 unidades.";
            case "retirar" -> "El retiro descuenta saldo y comisión; suma 2 puntos por cada 100.";
            case "transferir" -> "La comisión la paga quien envía. Suma 3 puntos por cada 100.";
            default -> "";
        };
    }

    private void ejecutarOperacion() {
        Ui.ocultarFeedback(feedback);
        try {
            if (esCanje()) {
                canjear();
            } else {
                operarConMonto();
            }
        } catch (NumberFormatException ex) {
            Ui.mostrarFeedback(feedback, "Escribe un monto válido, por ejemplo 250,00.", false);
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private void canjear() {
        BeneficioDto beneficio = beneficioCombo.getValue();
        if (beneficio == null) {
            Ui.mostrarFeedback(feedback, "Elige un beneficio para canjear.", false);
            return;
        }
        banco.canjearBeneficio(cliente.getId(), beneficio.beneficio());
        Ui.mostrarFeedback(feedback, "Canje realizado: " + beneficio.nombre() + ".", true);
        alCambiar.run();
    }

    private String textoBeneficio(BeneficioDto beneficio) {
        return beneficio.descripcion() + " (" + Ui.puntos(beneficio.costo()) + " pts)";
    }

    private void operarConMonto() {
        BigDecimal monto = Ui.parseMonto(montoCampo.getText());
        if (monto.signum() <= 0) {
            Ui.mostrarFeedback(feedback, "El monto debe ser mayor que cero.", false);
            return;
        }
        switch (operacion) {
            case "depositar" -> {
                banco.depositar(cliente.getId(), monto);
                Ui.mostrarFeedback(feedback, "Depósito de " + Ui.monto(monto) + " realizado.", true);
            }
            case "retirar" -> {
                banco.retirar(cliente.getId(), monto);
                Ui.mostrarFeedback(feedback, "Retiro de " + Ui.monto(monto) + " realizado.", true);
            }
            case "transferir" -> {
                if (!transferir(monto)) return;
            }
            default -> { }
        }
        montoCampo.clear();
        alCambiar.run();
    }

    private boolean transferir(BigDecimal monto) {
        String destino = destinoCampo.getText() == null
                ? "" : destinoCampo.getText().trim().toLowerCase(Locale.ROOT);
        if (destino.isEmpty()) {
            Ui.mostrarFeedback(feedback, "Escribe el ID del cliente que recibe.", false);
            return false;
        }
        banco.transferir(cliente.getId(), destino, monto);
        Ui.mostrarFeedback(feedback, "Transferencia de " + Ui.monto(monto) + " a " + destino + " realizada.", true);
        return true;
    }

    private boolean esCanje() {
        return "canjear".equals(operacion);
    }
}
