package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.dto.RankingDto;

/** Ranking completo de clientes por puntos acumulados. */
final class RankingView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Label posicion = new Label();
    private final ListView<RankingDto> lista = new ListView<>();
    private final VBox raiz;

    RankingView(Banco banco, Cliente cliente) {
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
        ObservableList<RankingDto> entradas = FXCollections.observableArrayList();
        int miPuesto = 0;
        int puesto = 1;
        for (RankingDto entrada : banco.consultarRankingVista(cliente.getId())) {
            entradas.add(entrada);
            if (entrada.actual()) {
                miPuesto = puesto;
            }
            puesto++;
        }
        lista.setItems(entradas);
        posicion.setText(miPuesto > 0
                ? "Tu posición: #" + miPuesto + " de " + entradas.size()
                : "Todavía no apareces en el ranking.");
    }

    private VBox construir() {
        Label titulo = new Label("Ranking");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Los puntos acumulados definen el orden del ranking completo.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        posicion.getStyleClass().add("hint");

        lista.setPrefHeight(420);
        lista.setPlaceholder(new Label("Todavía no hay clientes en el ranking."));
        lista.setAccessibleText("Ranking de clientes por puntos");
        lista.setCellFactory(v -> new RankingCell());

        VBox contenido = new VBox(18, titulo, intro, posicion, lista);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private final class RankingCell extends ListCell<RankingDto> {

        @Override
        protected void updateItem(RankingDto entrada, boolean vacio) {
            super.updateItem(entrada, vacio);
            if (vacio || entrada == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            boolean yo = entrada.actual();
            Label puesto = new Label(String.valueOf(getIndex() + 1));
            puesto.getStyleClass().add("ranking-position");

            Label nombre = new Label(entrada.nombre() + (yo ? " (tú)" : ""));
            nombre.getStyleClass().add("ranking-name");

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label puntos = new Label(Ui.puntos(entrada.puntos()) + " pts");
            puntos.getStyleClass().add("ranking-points");

            HBox fila = new HBox(12, puesto, nombre, espacio, puntos);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("ranking-row");
            if (yo) {
                fila.getStyleClass().add("ranking-row--me");
            }

            setGraphic(fila);
            setText(null);
            setAccessibleText(puesto.getText() + ". " + nombre.getText() + ", " + puntos.getText());
        }
    }
}
