package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.MovimientoDto;

final class ReversionView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final ListView<MovimientoDto> lista = new ListView<>();
    private final Label resumen = new Label();
    private final Label feedback = new Label();
    private final VBox raiz;

    ReversionView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        ObservableList<MovimientoDto> movimientos = FXCollections.observableArrayList();
        for (MovimientoDto movimiento : banco.consultarReversiblesVista(cliente.getId())) {
            movimientos.add(movimiento);
        }
        lista.setItems(movimientos);
        resumen.setText(movimientos.isEmpty()
                ? "No hay operaciones reversibles."
                : movimientos.size() + " operaciones reversibles. La más reciente aparece primero.");
    }

    private VBox construir() {
        Label titulo = new Label("Reversiones");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Deshaz un depósito, un retiro o una transferencia. "
                + "La operación contraria se registra en el historial.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        resumen.getStyleClass().add("hint");
        resumen.setWrapText(true);

        lista.setPrefHeight(320);
        lista.setPlaceholder(new Label("Cuando hagas una operación reversible aparecerá aquí."));
        lista.setAccessibleText("Operaciones reversibles");
        lista.setCellFactory(v -> new MovimientoCell());
        VBox.setVgrow(lista, Priority.ALWAYS);

        Button seleccionada = new Button("Revertir seleccionada");
        seleccionada.getStyleClass().add("primary-button");
        seleccionada.setGraphic(Ui.icono(Feather.ROTATE_CCW, 15));
        seleccionada.setOnAction(evento -> revertirSeleccionada());

        Button ultima = new Button("Revertir la última");
        ultima.getStyleClass().add("secondary-button");
        ultima.setGraphic(Ui.icono(Feather.CORNER_DOWN_LEFT, 14));
        ultima.setOnAction(evento -> revertirUltima());

        FlowPane acciones = new FlowPane(10, 10, seleccionada, ultima);
        acciones.getStyleClass().add("filter-row");

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);

        VBox contenido = new VBox(18, titulo, intro, resumen, lista, acciones, feedback);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private void revertirSeleccionada() {
        MovimientoDto seleccionado = lista.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Ui.mostrarFeedback(feedback, "Selecciona una operación de la lista.", false);
            return;
        }
        ejecutar(() -> banco.revertirTransaccion(cliente.getId(), seleccionado.id()),
                "Reversión realizada para el movimiento #" + seleccionado.id() + ".");
    }

    private void revertirUltima() {
        ejecutar(() -> banco.revertirUltimaTransaccion(cliente.getId()),
                "Se revirtió la última operación reversible.");
    }

    private void ejecutar(Runnable accion, String mensaje) {
        Ui.ocultarFeedback(feedback);
        try {
            accion.run();
            Ui.mostrarFeedback(feedback, mensaje, true);
            alCambiar.run();
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }
}
