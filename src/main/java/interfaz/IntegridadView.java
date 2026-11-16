package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.dto.IntegridadDto;

final class IntegridadView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final Label estado = new Label();
    private final ListView<String> errores = new ListView<>();
    private final VBox raiz;

    IntegridadView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        IntegridadDto resultado = banco.verificarIntegridadVista();
        ObservableList<String> lista = FXCollections.observableArrayList();
        for (String error : resultado.errores()) lista.add(error);
        errores.setItems(lista);
        Ui.mostrarFeedback(estado, resultado.valido()
                ? "Todo consistente."
                : lista.size() + " problemas encontrados.", resultado.valido());
    }

    private VBox construir() {
        Label titulo = new Label("Integridad");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Revisa que los saldos, los puntos y las transacciones de "
                + cliente.getNombre() + " y del resto de cuentas sean coherentes.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        estado.getStyleClass().add("feedback");
        estado.setWrapText(true);
        Ui.ocultarFeedback(estado);

        errores.setPrefHeight(320);
        errores.setPlaceholder(new Label("No se encontraron errores."));
        errores.setAccessibleText("Errores de integridad");

        Button verificar = new Button("Verificar ahora");
        verificar.getStyleClass().add("primary-button");
        verificar.setOnAction(evento -> alCambiar.run());

        VBox contenido = new VBox(18, titulo, intro, estado, errores, verificar);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }
}
