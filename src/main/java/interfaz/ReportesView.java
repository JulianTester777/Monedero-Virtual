package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.dto.FlujoDto;
import servicio.dto.ReporteDto;
import estructuras.ListaEnlazada;

final class ReportesView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final ListView<String> categorias = new ListView<>();
    private final ListView<String> relacionados = new ListView<>();
    private final ListView<String> conectados = new ListView<>();
    private final ListView<FlujoDto> flujos = new ListView<>();
    private final ListView<String> camino = new ListView<>();
    private final TextField destinoCampo = new TextField();
    private final Label ciclos = new Label();
    private final VBox raiz;

    ReportesView(Banco banco, Cliente cliente) {
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
        String destino = destinoCampo.getText() == null ? "" : destinoCampo.getText().trim();
        ReporteDto reporte = banco.consultarReporteVista(cliente.getId(), destino);
        categorias.setItems(copiar(reporte.categorias()));
        relacionados.setItems(copiar(reporte.clientesRelacionados()));
        conectados.setItems(copiar(reporte.clientesMasConectados()));
        flujos.setItems(copiar(reporte.flujos()));
        camino.setItems(copiar(reporte.camino()));
        ciclos.setText(reporte.hayCiclos()
                ? "Se detectaron ciclos en las relaciones."
                : "No se detectaron ciclos en las relaciones.");
    }

    private VBox construir() {
        Label titulo = new Label("Reportes");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Explora tus categorías de gasto, las conexiones entre clientes "
                + "y el camino entre dos cuentas.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        ciclos.getStyleClass().add("hint");

        configurarLista(categorias, "Categorías usadas");
        configurarLista(relacionados, "Clientes relacionados");
        configurarLista(conectados, "Clientes más conectados");
        configurarLista(flujos, "Flujos entre clientes");
        configurarLista(camino, "Camino entre clientes");

        destinoCampo.getStyleClass().add("text-field");
        destinoCampo.setPromptText("ID del cliente destino");
        destinoCampo.setAccessibleText("ID del cliente destino para calcular el camino");

        Button calcular = new Button("Calcular camino");
        calcular.getStyleClass().add("primary-button");
        calcular.setOnAction(evento -> actualizar());

        FlowPane caminoFormulario = new FlowPane(10, 10, destinoCampo, calcular);
        caminoFormulario.getStyleClass().add("filter-row");

        VBox contenido = new VBox(18, titulo, intro, ciclos,
                bloque("Categorías usadas", categorias),
                bloque("Clientes relacionados", relacionados),
                bloque("Clientes más conectados", conectados),
                bloque("Flujos entre clientes", flujos),
                bloque("Camino entre clientes", camino),
                caminoFormulario);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private <T> void configurarLista(ListView<T> lista, String accesible) {
        lista.setPrefHeight(120);
        lista.setAccessibleText(accesible);
    }

    private <T> VBox bloque(String titulo, ListView<T> lista) {
        Label label = new Label(titulo);
        label.getStyleClass().add("section-title");
        VBox bloque = new VBox(8, label, lista);
        bloque.getStyleClass().add("view-section");
        return bloque;
    }

    private <T> ObservableList<T> copiar(ListaEnlazada<T> datos) {
        ObservableList<T> salida = FXCollections.observableArrayList();
        for (T dato : datos) salida.add(dato);
        return salida;
    }
}
