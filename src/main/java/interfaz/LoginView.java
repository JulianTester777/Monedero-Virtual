package interfaz;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Pantalla de acceso por ID de cliente. */
final class LoginView extends BorderPane {

    private final Banco banco;
    private final Consumer<Cliente> alEntrar;
    private final PassCard pase = new PassCard(true);
    private final StackPane contenedorFormulario = new StackPane();
    private final TextField idCampo = new TextField();
    private final Label error = new Label();
    private final ScrollPane scrollCompacto = new ScrollPane();
    private final VBox compactoContenido = new VBox(24);

    private boolean compacto;
    private boolean inicializado;

    LoginView(Banco banco, Consumer<Cliente> alEntrar) {
        this.banco = banco;
        this.alEntrar = alEntrar;
        getStyleClass().add("login-view");

        VBox formulario = construirFormulario();
        contenedorFormulario.getChildren().add(formulario);
        contenedorFormulario.setAlignment(Pos.CENTER_LEFT);

        scrollCompacto.getStyleClass().add("content-scroll");
        scrollCompacto.setFitToWidth(true);
        scrollCompacto.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollCompacto.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollCompacto.setContent(compactoContenido);

        setLeft(pase);
        setCenter(contenedorFormulario);
        BorderPane.setMargin(pase, new Insets(0, 48, 0, 0));

        widthProperty().addListener((obs, anterior, actual) -> adaptar(actual.doubleValue()));
        Platform.runLater(() -> {
            adaptar(getWidth());
            idCampo.requestFocus();
        });
    }

    private VBox construirFormulario() {
        Label titulo = new Label("Entra a tu monedero");
        titulo.getStyleClass().add("display-title");
        titulo.setWrapText(true);

        Label intro = new Label("Escribe tu ID de cliente para ver tu saldo, tus puntos y tus movimientos.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        Label demo = new Label("IDs de demostración");
        demo.getStyleClass().add("hint");

        configurarError();

        VBox formulario = new VBox(16, titulo, intro, construirCampoId(), demo,
                construirChipsDemo(), construirBotonEntrar(), error, construirNota());
        formulario.getStyleClass().add("login-form");
        formulario.setMaxWidth(440);
        return formulario;
    }

    private VBox construirCampoId() {
        Label etiqueta = new Label("ID de cliente");
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(idCampo);

        idCampo.getStyleClass().add("text-field");
        idCampo.setPromptText("Ej. ana");
        idCampo.setAccessibleText("ID de cliente");
        idCampo.setOnAction(evento -> intentarEntrar());
        idCampo.textProperty().addListener((obs, anterior, actual) -> ocultarError());

        return new VBox(6, etiqueta, idCampo);
    }

    private HBox construirChipsDemo() {
        HBox chips = new HBox(8);
        for (String id : List.of("ana", "bruno", "carla")) {
            Button chip = new Button(id);
            chip.getStyleClass().add("chip");
            chip.setAccessibleText("Usar el ID de demostración " + id);
            chip.setOnAction(evento -> usarDemo(id));
            chips.getChildren().add(chip);
        }
        return chips;
    }

    private void usarDemo(String id) {
        idCampo.setText(id);
        idCampo.requestFocus();
        idCampo.end();
    }

    private Button construirBotonEntrar() {
        Button entrar = new Button("Entrar");
        entrar.getStyleClass().add("primary-button");
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setDefaultButton(true);
        entrar.setOnAction(evento -> intentarEntrar());
        return entrar;
    }

    private void configurarError() {
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        error.setAccessibleRole(AccessibleRole.TEXT);
    }

    private Label construirNota() {
        Label nota = new Label("Esta demo no pide contraseña: el ID abre la cuenta.");
        nota.getStyleClass().add("fine-print");
        nota.setWrapText(true);
        return nota;
    }

    private void intentarEntrar() {
        String id = idCampo.getText() == null ? "" : idCampo.getText().trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            mostrarError("Escribe tu ID de cliente. Puedes probar con ana, bruno o carla.");
            return;
        }
        try {
            alEntrar.accept(banco.buscarCliente(id));
        } catch (OperacionInvalidaException ex) {
            mostrarError("No encontramos ese ID. Prueba con ana, bruno o carla.");
        }
    }

    private void mostrarError(String texto) {
        error.setText(texto);
        error.setAccessibleText(texto);
        error.setVisible(true);
        error.setManaged(true);
        idCampo.requestFocus();
        idCampo.selectAll();
    }

    private void ocultarError() {
        if (!error.isVisible()) return;
        error.setText("");
        error.setAccessibleText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private void adaptar(double ancho) {
        boolean nuevoCompacto = ancho < 900;
        if (inicializado && nuevoCompacto == compacto) return;
        compacto = nuevoCompacto;
        inicializado = true;

        pase.setCompacto(compacto);
        if (compacto) {
            if (!getStyleClass().contains("login-view--compact")) getStyleClass().add("login-view--compact");
            setLeft(null);
            setTop(null);
            setCenter(null);
            compactoContenido.getChildren().setAll(pase, contenedorFormulario);
            setCenter(scrollCompacto);
        } else {
            getStyleClass().remove("login-view--compact");
            compactoContenido.getChildren().clear();
            setCenter(null);
            setTop(null);
            setLeft(pase);
            setCenter(contenedorFormulario);
            BorderPane.setMargin(pase, new Insets(0, 48, 0, 0));
        }
    }
}
