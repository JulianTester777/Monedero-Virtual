package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.dto.MovimientoDto;

/** Historial completo de movimientos, con filtro por tipo. */
final class MovimientosView implements Vista {

    private static final String TODOS = "Todos los tipos";

    private final Banco banco;
    private final Cliente cliente;
    private final ComboBox<String> filtro = new ComboBox<>();
    private final ListView<MovimientoDto> lista = new ListView<>();
    private final Label resumen = new Label();
    private final VBox raiz;

    MovimientosView(Banco banco, Cliente cliente) {
        this.banco = banco;
        this.cliente = cliente;
        raiz = construir();
    }

    @Override
    public Node raiz() {
        return raiz;
    }

    @Override
    public void actualizar() {
        ObservableList<MovimientoDto> movimientos = FXCollections.observableArrayList();
        for (MovimientoDto movimiento : banco.consultarMovimientosVista(cliente.getId())) {
            if (coincide(movimiento)) {
                movimientos.add(0, movimiento);
            }
        }
        lista.setItems(movimientos);
        resumen.setText(movimientos.size() + (movimientos.size() == 1
                ? " movimiento" : " movimientos"));
    }

    private VBox construir() {
        Label titulo = new Label("Movimientos");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Historial completo de tu cuenta. Los más recientes aparecen primero.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        filtro.setItems(FXCollections.observableArrayList(TODOS));
        for (String tipo : banco.tiposMovimientoVista()) {
            filtro.getItems().add(tipo);
        }
        filtro.setValue(TODOS);
        filtro.setMaxWidth(260);
        filtro.setAccessibleText("Filtrar movimientos por tipo");
        filtro.valueProperty().addListener((obs, anterior, actual) -> actualizar());

        Label etiquetaFiltro = new Label("Tipo");
        etiquetaFiltro.getStyleClass().add("field-label");
        etiquetaFiltro.setLabelFor(filtro);

        HBox barraFiltro = new HBox(10, etiquetaFiltro, filtro);
        barraFiltro.getStyleClass().add("filter-row");

        resumen.getStyleClass().add("hint");

        lista.setPrefHeight(420);
        lista.setPlaceholder(new Label("No hay movimientos para este filtro."));
        lista.setAccessibleText("Historial de movimientos");
        lista.setCellFactory(v -> new MovimientoCell());
        VBox.setVgrow(lista, Priority.ALWAYS);

        VBox contenido = new VBox(18, titulo, intro, barraFiltro, resumen, lista);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private boolean coincide(MovimientoDto movimiento) {
        String seleccion = filtro.getValue();
        return seleccion == null || TODOS.equals(seleccion)
                || movimiento.tipo().equals(seleccion);
    }
}
