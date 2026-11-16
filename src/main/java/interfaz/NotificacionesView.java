package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.NotificacionDto;
import servicio.dto.ResumenDto;

import java.math.BigDecimal;

final class NotificacionesView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final ListView<NotificacionDto> lista = new ListView<>();
    private final Label resumen = new Label();
    private final Label siguiente = new Label();
    private final TextField umbralCampo = new TextField();
    private final Label feedback = new Label();
    private final VBox raiz;

    NotificacionesView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        ObservableList<NotificacionDto> avisos = FXCollections.observableArrayList();
        for (NotificacionDto aviso : banco.consultarNotificacionesVista(cliente.getId())) {
            avisos.add(0, aviso);
        }
        lista.setItems(avisos);
        ResumenDto datos = banco.consultarResumenVista(cliente.getId());
        resumen.setText(datos.notificacionesNoLeidas() == 1
                ? "1 aviso sin leer"
                : datos.notificacionesNoLeidas() + " avisos sin leer");
    }

    private VBox construir() {
        Label titulo = new Label("Avisos");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Consulta los avisos del sistema y elige cuándo considerar que tu saldo es bajo.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        resumen.getStyleClass().add("hint");

        lista.setPrefHeight(260);
        lista.setPlaceholder(new Label("No tienes avisos."));
        lista.setAccessibleText("Notificaciones");
        lista.setCellFactory(v -> new NotificacionCell());

        Button leer = new Button("Marcar como leída");
        leer.getStyleClass().add("primary-button");
        leer.setOnAction(evento -> marcarLeida());

        Button siguienteAviso = new Button("Ver siguiente aviso");
        siguienteAviso.getStyleClass().add("secondary-button");
        siguienteAviso.setOnAction(evento -> verSiguiente());

        FlowPane acciones = new FlowPane(10, 10, leer, siguienteAviso);
        acciones.getStyleClass().add("filter-row");

        siguiente.getStyleClass().add("hint");
        siguiente.setWrapText(true);

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);

        VBox contenido = new VBox(18, titulo, intro, resumen, lista, acciones,
                siguiente, crearUmbral(), feedback);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private VBox crearUmbral() {
        Label titulo = new Label("Umbral de saldo bajo");
        titulo.getStyleClass().add("section-title");

        umbralCampo.getStyleClass().add("text-field");
        umbralCampo.setText(Ui.monto(banco.consultarUmbralSaldoBajo()));
        umbralCampo.setAccessibleText("Umbral de saldo bajo");

        Button guardar = new Button("Guardar umbral");
        guardar.getStyleClass().add("primary-button");
        guardar.setOnAction(evento -> guardarUmbral());

        VBox bloque = new VBox(10, titulo,
                campo("Saldo mínimo", umbralCampo), guardar);
        bloque.getStyleClass().add("action-panel");
        return bloque;
    }

    private VBox campo(String etiqueta, Node control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("field-label");
        label.setLabelFor(control);
        VBox grupo = new VBox(6, label, control);
        grupo.getStyleClass().add("field-group");
        return grupo;
    }

    private void marcarLeida() {
        Ui.ocultarFeedback(feedback);
        NotificacionDto seleccionada = lista.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            Ui.mostrarFeedback(feedback, "Selecciona un aviso.", false);
            return;
        }
        if (banco.marcarNotificacionLeida(seleccionada.id())) {
            Ui.mostrarFeedback(feedback, "Aviso #" + seleccionada.id() + " marcado como leído.", true);
            alCambiar.run();
        } else {
            Ui.mostrarFeedback(feedback, "No se encontró el aviso.", false);
        }
    }

    private void verSiguiente() {
        Ui.ocultarFeedback(feedback);
        NotificacionDto aviso = banco.siguienteNotificacionVista(cliente.getId());
        siguiente.setText(aviso == null
                ? "No hay avisos pendientes."
                : aviso.tipo() + ": " + aviso.mensaje() + " (" + Ui.fecha(aviso.fecha()) + ")");
    }

    private void guardarUmbral() {
        Ui.ocultarFeedback(feedback);
        try {
            BigDecimal umbral = Ui.parseMonto(umbralCampo.getText());
            banco.configurarUmbralSaldoBajo(umbral);
            Ui.mostrarFeedback(feedback, "Umbral actualizado a " + Ui.monto(umbral) + ".", true);
        } catch (NumberFormatException ex) {
            Ui.mostrarFeedback(feedback, "Escribe un monto válido para el umbral.", false);
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private final class NotificacionCell extends ListCell<NotificacionDto> {

        @Override
        protected void updateItem(NotificacionDto aviso, boolean vacio) {
            super.updateItem(aviso, vacio);
            if (vacio || aviso == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            Label tipo = new Label("#" + aviso.id() + " · " + aviso.tipo()
                    + (aviso.leida() ? " · Leída" : " · Sin leer"));
            tipo.getStyleClass().add("movement-type");

            Label mensaje = new Label(aviso.mensaje());
            mensaje.getStyleClass().add("movement-meta");
            mensaje.setWrapText(true);

            Label fecha = new Label(Ui.fecha(aviso.fecha()));
            fecha.getStyleClass().add("movement-meta");

            VBox texto = new VBox(3, tipo, mensaje, fecha);
            HBox.setHgrow(texto, Priority.ALWAYS);

            HBox fila = new HBox(12, texto);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("movement-row");

            setGraphic(fila);
            setText(null);
            setAccessibleText(tipo.getText() + ", " + aviso.mensaje());
        }
    }
}
