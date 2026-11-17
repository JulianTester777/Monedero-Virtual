package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import modelo.Recurrencia;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.ProgramadaDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

final class ProgramadasView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final ListView<ProgramadaDto> lista = new ListView<>();
    private final ComboBox<String> tipoCombo = new ComboBox<>();
    private final TextField montoCampo = new TextField();
    private final TextField destinoCampo = new TextField();
    private final TextField fechaCampo = new TextField();
    private final ComboBox<Recurrencia> recurrenciaCombo = new ComboBox<>();
    private final VBox grupoDestino = new VBox(6);
    private final Label feedback = new Label();
    private final VBox raiz;

    ProgramadasView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        ObservableList<ProgramadaDto> programadas = FXCollections.observableArrayList();
        for (ProgramadaDto programada : banco.consultarProgramadasVista(cliente.getId())) {
            programadas.add(programada);
        }
        lista.setItems(programadas);
    }

    private VBox construir() {
        Label titulo = new Label("Programadas");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Deja preparada una operación para una fecha futura. "
                + "Puedes procesarla cuando el reloj llegue a ese momento.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        lista.setPrefHeight(240);
        lista.setPlaceholder(new Label("No tienes operaciones programadas."));
        lista.setAccessibleText("Operaciones programadas");
        lista.setCellFactory(v -> new ProgramadaCell());
        VBox.setVgrow(lista, Priority.ALWAYS);

        Button cancelar = new Button("Cancelar seleccionada");
        cancelar.getStyleClass().add("secondary-button");
        cancelar.setGraphic(Ui.icono(Feather.X, 14));
        cancelar.setOnAction(evento -> cancelarSeleccionada());

        Button procesar = new Button("Procesar pendientes");
        procesar.getStyleClass().add("primary-button");
        procesar.setGraphic(Ui.icono(Feather.PLAY, 15));
        procesar.setOnAction(evento -> procesarPendientes());

        FlowPane acciones = new FlowPane(10, 10, cancelar, procesar);
        acciones.getStyleClass().add("filter-row");

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);

        VBox contenido = new VBox(18, titulo, intro, lista, acciones,
                crearFormulario(), feedback);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private VBox crearFormulario() {
        Label titulo = Ui.seccion("Nueva programada", Feather.CLOCK);

        tipoCombo.getItems().setAll("Depósito", "Retiro", "Transferencia");
        tipoCombo.setValue("Depósito");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setAccessibleText("Tipo de operación programada");
        tipoCombo.valueProperty().addListener((obs, anterior, actual) -> {
            boolean transferencia = "Transferencia".equals(actual);
            grupoDestino.setVisible(transferencia);
            grupoDestino.setManaged(transferencia);
        });

        montoCampo.getStyleClass().addAll("text-field", "amount-field");
        montoCampo.setPromptText("0,00");
        montoCampo.setAccessibleText("Monto de la operación programada");

        destinoCampo.getStyleClass().add("text-field");
        destinoCampo.setPromptText("Ej. bruno");
        destinoCampo.setAccessibleText("ID del cliente que recibe la transferencia");

        fechaCampo.getStyleClass().add("text-field");
        fechaCampo.setPromptText("2026-12-31 18:30");
        fechaCampo.setAccessibleText("Fecha de ejecución");

        recurrenciaCombo.getItems().setAll(Recurrencia.values());
        recurrenciaCombo.setValue(Recurrencia.NINGUNA);
        recurrenciaCombo.setMaxWidth(Double.MAX_VALUE);
        recurrenciaCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Recurrencia recurrencia) {
                return recurrencia == null ? "" : Ui.nombreRecurrencia(recurrencia);
            }

            @Override
            public Recurrencia fromString(String texto) {
                return null;
            }
        });
        recurrenciaCombo.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Recurrencia recurrencia, boolean vacio) {
                super.updateItem(recurrencia, vacio);
                setText(vacio || recurrencia == null ? null : Ui.nombreRecurrencia(recurrencia));
            }
        });
        recurrenciaCombo.setAccessibleText("Recurrencia de la operación programada");

        grupoDestino.getChildren().addAll(etiqueta("ID de destino"), destinoCampo);
        grupoDestino.getStyleClass().add("field-group");
        grupoDestino.setVisible(false);
        grupoDestino.setManaged(false);

        Button programar = new Button("Programar");
        programar.getStyleClass().add("primary-button");
        programar.setGraphic(Ui.icono(Feather.CALENDAR, 15));
        programar.setMaxWidth(Double.MAX_VALUE);
        programar.setOnAction(evento -> programar());

        VBox bloque = new VBox(10, titulo, campo("Tipo", tipoCombo), campo("Monto", montoCampo),
                campo("Fecha (yyyy-MM-dd HH:mm)", fechaCampo), campo("Recurrencia", recurrenciaCombo),
                grupoDestino, programar);
        bloque.getStyleClass().add("action-panel");
        return bloque;
    }

    private Label etiqueta(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("field-label");
        return etiqueta;
    }

    private VBox campo(String etiqueta, Node control) {
        Label label = etiqueta(etiqueta);
        label.setLabelFor(control);
        VBox grupo = new VBox(6, label, control);
        grupo.getStyleClass().add("field-group");
        return grupo;
    }

    private void programar() {
        Ui.ocultarFeedback(feedback);
        try {
            BigDecimal monto = Ui.parseMonto(montoCampo.getText());
            LocalDateTime fecha = Ui.parseFecha(fechaCampo.getText());
            Recurrencia recurrencia = recurrenciaCombo.getValue();
            String tipo = tipoCombo.getValue();

            if ("Depósito".equals(tipo)) {
                banco.programarDeposito(cliente.getId(), monto, fecha, recurrencia);
            } else if ("Retiro".equals(tipo)) {
                banco.programarRetiro(cliente.getId(), monto, fecha, recurrencia);
            } else {
                String destino = destinoCampo.getText() == null ? "" : destinoCampo.getText().trim();
                banco.programarTransferencia(cliente.getId(), destino, monto, fecha, recurrencia);
            }

            Ui.mostrarFeedback(feedback, "Operación programada para " + fecha + ".", true);
            montoCampo.clear();
            destinoCampo.clear();
            alCambiar.run();
        } catch (NumberFormatException ex) {
            Ui.mostrarFeedback(feedback, "Escribe un monto válido, por ejemplo 250,00.", false);
        } catch (DateTimeParseException ex) {
            Ui.mostrarFeedback(feedback, "Usa una fecha como 2026-12-31 18:30.", false);
        } catch (IllegalArgumentException ex) {
            Ui.mostrarFeedback(feedback, "Usa una fecha como 2026-12-31 18:30.", false);
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private void cancelarSeleccionada() {
        Ui.ocultarFeedback(feedback);
        ProgramadaDto seleccionada = lista.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            Ui.mostrarFeedback(feedback, "Selecciona una operación programada.", false);
            return;
        }
        if (banco.cancelarProgramada(seleccionada.id())) {
            Ui.mostrarFeedback(feedback, "Programada #" + seleccionada.id() + " cancelada.", true);
            alCambiar.run();
        } else {
            Ui.mostrarFeedback(feedback, "Solo se pueden cancelar programadas pendientes.", false);
        }
    }

    private void procesarPendientes() {
        Ui.ocultarFeedback(feedback);
        try {
            int ejecutadas = banco.procesarProgramadasVista();
            Ui.mostrarFeedback(feedback, ejecutadas == 0
                    ? "No se ejecutaron programadas; revisa sus fechas o avisos."
                    : ejecutadas + " operaciones ejecutadas.", true);
            alCambiar.run();
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private final class ProgramadaCell extends ListCell<ProgramadaDto> {

        @Override
        protected void updateItem(ProgramadaDto programada, boolean vacio) {
            super.updateItem(programada, vacio);
            if (vacio || programada == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            Label tipo = new Label("#" + programada.id() + " · " + programada.tipo());
            tipo.getStyleClass().add("movement-type");

            String detalle = Ui.fecha(programada.fecha()) + " · " + programada.recurrencia()
                    + " · " + programada.estado();
            if (programada.destino() != null) {
                detalle += " · para " + programada.destino();
            }
            Label meta = new Label(detalle);
            meta.getStyleClass().add("movement-meta");
            meta.setWrapText(true);

            VBox texto = new VBox(3, tipo, meta);
            HBox.setHgrow(texto, Priority.ALWAYS);

            Label monto = new Label(Ui.monto(programada.monto()));
            monto.getStyleClass().add("movement-amount");

            HBox fila = new HBox(12, texto, monto);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("movement-row");

            setGraphic(fila);
            setText(null);
            setAccessibleText(tipo.getText() + ", " + detalle + ", " + monto.getText());
        }
    }
}
