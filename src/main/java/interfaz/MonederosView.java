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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import modelo.TipoMonedero;
import servicio.Banco;
import servicio.OperacionInvalidaException;
import servicio.dto.MonederoDto;

import java.math.BigDecimal;

final class MonederosView implements Vista {

    private final Banco banco;
    private final Cliente cliente;
    private final Runnable alCambiar;
    private final ListView<MonederoDto> lista = new ListView<>();
    private final Label total = new Label();
    private final TextField idCampo = new TextField();
    private final TextField nombreCampo = new TextField();
    private final ComboBox<TipoMonedero> tipoCombo = new ComboBox<>();
    private final TextField montoCampo = new TextField();
    private final ComboBox<MonederoDto> origenCombo = new ComboBox<>();
    private final ComboBox<MonederoDto> destinoCombo = new ComboBox<>();
    private final Label feedback = new Label();
    private final VBox raiz;

    MonederosView(Banco banco, Cliente cliente, Runnable alCambiar) {
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
        ObservableList<MonederoDto> monederos = FXCollections.observableArrayList();
        for (MonederoDto monedero : banco.consultarMonederosVista(cliente.getId())) {
            monederos.add(monedero);
        }
        lista.setItems(monederos);
        origenCombo.setItems(copiar(monederos));
        destinoCombo.setItems(copiar(monederos));
        total.setText("Saldo total: " + Ui.monto(banco.consultarResumenVista(cliente.getId()).saldoTotal()));
    }

    private VBox construir() {
        Label titulo = new Label("Monederos");
        titulo.getStyleClass().add("display-title");

        Label intro = new Label("Separa tu dinero en monederos y mueve saldo entre ellos sin comisión.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        total.getStyleClass().add("hint");

        lista.setPrefHeight(220);
        lista.setPlaceholder(new Label("Todavía no hay monederos."));
        lista.setAccessibleText("Monederos del cliente");
        lista.setCellFactory(v -> new MonederoCell());
        VBox.setVgrow(lista, Priority.ALWAYS);

        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        Ui.ocultarFeedback(feedback);

        VBox contenido = new VBox(18, titulo, intro, total, lista,
                crearFormularioNuevo(), crearFormularioTransferencia(), feedback);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private VBox crearFormularioNuevo() {
        Label titulo = Ui.seccion("Nuevo monedero", Feather.POCKET);

        idCampo.getStyleClass().add("text-field");
        idCampo.setPromptText("Ej. ahorro");
        idCampo.setAccessibleText("ID del nuevo monedero");

        nombreCampo.getStyleClass().add("text-field");
        nombreCampo.setPromptText("Ej. Ahorro");
        nombreCampo.setAccessibleText("Nombre del nuevo monedero");

        tipoCombo.getItems().setAll(TipoMonedero.values());
        tipoCombo.setValue(TipoMonedero.AHORRO);
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setAccessibleText("Tipo del nuevo monedero");

        Button crear = new Button("Crear monedero");
        crear.getStyleClass().add("primary-button");
        crear.setGraphic(Ui.icono(Feather.PLUS, 14));
        crear.setMaxWidth(Double.MAX_VALUE);
        crear.setOnAction(evento -> crearMonedero());

        VBox bloque = new VBox(10, titulo,
                campo("ID", idCampo), campo("Nombre", nombreCampo), campo("Tipo", tipoCombo), crear);
        bloque.getStyleClass().add("action-panel");
        return bloque;
    }

    private VBox crearFormularioTransferencia() {
        Label titulo = Ui.seccion("Mover saldo entre monederos", Feather.SHARE_2);

        origenCombo.setMaxWidth(Double.MAX_VALUE);
        origenCombo.setAccessibleText("Monedero de origen");
        destinoCombo.setMaxWidth(Double.MAX_VALUE);
        destinoCombo.setAccessibleText("Monedero de destino");

        montoCampo.getStyleClass().addAll("text-field", "amount-field");
        montoCampo.setPromptText("0,00");
        montoCampo.setAccessibleText("Monto a mover");

        Button mover = new Button("Mover saldo");
        mover.getStyleClass().add("primary-button");
        mover.setGraphic(Ui.icono(Feather.REPEAT, 14));
        mover.setMaxWidth(Double.MAX_VALUE);
        mover.setOnAction(evento -> moverSaldo());

        VBox bloque = new VBox(10, titulo,
                campo("Desde", origenCombo), campo("Hacia", destinoCombo),
                campo("Monto", montoCampo), mover);
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

    private void crearMonedero() {
        Ui.ocultarFeedback(feedback);
        try {
            banco.crearMonedero(cliente.getId(), idCampo.getText(), nombreCampo.getText(), tipoCombo.getValue());
            Ui.mostrarFeedback(feedback, "Monedero creado.", true);
            idCampo.clear();
            nombreCampo.clear();
            alCambiar.run();
        } catch (OperacionInvalidaException | IllegalArgumentException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private void moverSaldo() {
        Ui.ocultarFeedback(feedback);
        try {
            MonederoDto origen = origenCombo.getValue();
            MonederoDto destino = destinoCombo.getValue();
            if (origen == null || destino == null) {
                Ui.mostrarFeedback(feedback, "Elige el monedero de origen y el de destino.", false);
                return;
            }
            BigDecimal monto = Ui.parseMonto(montoCampo.getText());
            banco.transferirEntreMonederos(cliente.getId(), origen.id(), destino.id(), monto);
            Ui.mostrarFeedback(feedback, "Se movieron " + Ui.monto(monto) + " de "
                    + origen.nombre() + " a " + destino.nombre() + ".", true);
            montoCampo.clear();
            alCambiar.run();
        } catch (NumberFormatException ex) {
            Ui.mostrarFeedback(feedback, "Escribe un monto válido, por ejemplo 250,00.", false);
        } catch (OperacionInvalidaException ex) {
            Ui.mostrarFeedback(feedback, ex.getMessage(), false);
        }
    }

    private ObservableList<MonederoDto> copiar(ObservableList<MonederoDto> monederos) {
        return FXCollections.observableArrayList(monederos);
    }

    private final class MonederoCell extends ListCell<MonederoDto> {

        @Override
        protected void updateItem(MonederoDto monedero, boolean vacio) {
            super.updateItem(monedero, vacio);
            if (vacio || monedero == null) {
                setGraphic(null);
                setText(null);
                setAccessibleText(null);
                return;
            }

            Label nombre = new Label(monedero.nombre() + (monedero.principal() ? " · Principal" : ""));
            nombre.getStyleClass().add("movement-type");

            Label meta = new Label(monedero.id() + " · " + monedero.tipo());
            meta.getStyleClass().add("movement-meta");

            VBox texto = new VBox(3, nombre, meta);
            HBox.setHgrow(texto, Priority.ALWAYS);

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label saldo = new Label(Ui.monto(monedero.saldo()));
            saldo.getStyleClass().add("movement-amount");

            HBox fila = new HBox(12, texto, espacio, saldo);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("movement-row");

            setGraphic(fila);
            setText(null);
            setAccessibleText(monedero.nombre() + ", " + monedero.tipo() + ", saldo "
                    + Ui.monto(monedero.saldo()));
        }
    }
}
