package interfaz;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;
import servicio.OperacionInvalidaException;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Pantalla de acceso con dos momentos: acceder con ID y contrasena, o crear
 * una cuenta nueva. Todo el contenido (pase, formulario y boton de tema) vive
 * dentro de un unico scroll vertical: en pantallas bajas nada queda fuera de
 * alcance.
 */
final class LoginView extends ScrollPane {

    private static final double ANCHO_COMPACTO = 900;

    private final Banco banco;
    private final Consumer<Cliente> alEntrar;
    private final Consumer<Boolean> alTema;
    private final boolean oscuro;
    private final PassCard pase = new PassCard(true);
    private final StackPane contenedorFormularios = new StackPane();
    private final TextField idAcceso = campoTexto("ID de cliente", "Ej. ana");
    private final PasswordField claveAcceso = new PasswordField();
    private final TextField idNuevo = campoTexto("ID de cliente", "Ej. dehib");
    private final TextField nombreNuevo = campoTexto("Nombre completo", "Ej. Dehib Pérez");
    private final PasswordField claveNueva = new PasswordField();
    private final ToggleButton momentoAcceder = new ToggleButton("Acceder");
    private final ToggleButton momentoCrear = new ToggleButton("Crear cuenta");
    private final Label error = new Label();
    private final VBox pagina = new VBox(16);
    private final HBox cuerpoAmplio = new HBox(32);
    private final HBox filaSuperior = construirFilaSuperior();
    // Se construyen en el constructor: los chips de usuarios necesitan el banco ya asignado.
    private final VBox formularioAcceso;
    private final VBox formularioCreacion;
    private final VBox formulario;

    private boolean compacto;
    private boolean inicializado;

    LoginView(Banco banco, Consumer<Cliente> alEntrar, Consumer<Boolean> alTema, boolean oscuro) {
        this.banco = banco;
        this.alEntrar = alEntrar;
        this.alTema = alTema;
        this.oscuro = oscuro;
        this.formularioAcceso = construirFormularioAcceso();
        this.formularioCreacion = construirFormularioCreacion();
        this.formulario = construirFormulario();
        getStyleClass().addAll("login-view", "content-scroll");
        if (oscuro) getStyleClass().add("theme-dark");

        setFitToWidth(true);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setContent(pagina);

        configurarMomentos();

        contenedorFormularios.getChildren().setAll(formularioAcceso, formularioCreacion);
        mostrarMomento(true);
        contenedorFormularios.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(contenedorFormularios, Priority.ALWAYS);
        HBox.setMargin(pase, new Insets(0, 48, 0, 0));
        cuerpoAmplio.setAlignment(Pos.CENTER_LEFT);
        cuerpoAmplio.setMaxWidth(Double.MAX_VALUE);

        widthProperty().addListener((obs, anterior, actual) -> adaptar(actual.doubleValue()));
        Platform.runLater(() -> {
            adaptar(getWidth());
            idAcceso.requestFocus();
        });
    }

    private VBox construirFormulario() {
        Label titulo = new Label("Tu monedero te espera");
        titulo.getStyleClass().add("display-title");
        titulo.setWrapText(true);

        Label intro = new Label("Accede con tu ID y contraseña, o crea una cuenta nueva en segundos.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        Label nota = new Label("Las contraseñas se guardan como hash SHA-256 con sal; nunca en texto plano.");
        nota.getStyleClass().add("fine-print");
        nota.setWrapText(true);

        configurarError();

        VBox formulario = new VBox(16, titulo, intro, construirMomentos(), contenedorFormularios, error, nota);
        formulario.getStyleClass().add("login-form");
        formulario.setMaxWidth(480);
        return formulario;
    }

    private HBox construirMomentos() {
        ToggleGroup grupo = new ToggleGroup();
        momentoAcceder.getStyleClass().add("operation-tab");
        momentoAcceder.setGraphic(Ui.icono(Feather.LOG_IN, 14));
        momentoAcceder.setToggleGroup(grupo);
        momentoAcceder.setAccessibleText("Acceder con un ID y contrasena existentes");

        momentoCrear.getStyleClass().add("operation-tab");
        momentoCrear.setGraphic(Ui.icono(Feather.USER_PLUS, 14));
        momentoCrear.setToggleGroup(grupo);
        momentoCrear.setAccessibleText("Crear una cuenta nueva");

        momentoAcceder.setOnAction(evento -> mostrarMomento(true));
        momentoCrear.setOnAction(evento -> mostrarMomento(false));

        HBox momentos = new HBox(8, momentoAcceder, momentoCrear);
        momentos.setMaxWidth(Double.MAX_VALUE);
        return momentos;
    }

    private void configurarMomentos() {
        claveAcceso.getStyleClass().add("text-field");
        claveAcceso.setPromptText("Contraseña");
        claveAcceso.setAccessibleText("Contraseña");
        claveAcceso.setOnAction(evento -> intentarAcceder());

        claveNueva.getStyleClass().add("text-field");
        claveNueva.setPromptText("Elige una contraseña");
        claveNueva.setAccessibleText("Contraseña nueva");
        claveNueva.setOnAction(evento -> intentarCrearCuenta());
    }

    private VBox construirFormularioAcceso() {
        Button entrar = new Button("Entrar");
        entrar.getStyleClass().add("primary-button");
        entrar.setGraphic(Ui.icono(Feather.LOG_IN, 15));
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setDefaultButton(true);
        entrar.setOnAction(evento -> intentarAcceder());

        Label ayuda = new Label("Cuentas registradas · las de demostración usan la contraseña demo");
        ayuda.getStyleClass().add("hint");
        ayuda.setWrapText(true);

        VBox formulario = new VBox(16, campoEtiquetado(idAcceso, "ID de cliente"),
                campoEtiquetado(claveAcceso, "Contraseña"), construirChipsUsuarios(), ayuda, entrar);
        return formulario;
    }

    private VBox construirFormularioCreacion() {
        Button crear = new Button("Crear cuenta");
        crear.getStyleClass().add("primary-button");
        crear.setGraphic(Ui.icono(Feather.USER_PLUS, 15));
        crear.setMaxWidth(Double.MAX_VALUE);
        crear.setOnAction(evento -> intentarCrearCuenta());

        Label notaId = new Label("El ID se escribe en minúsculas y sin espacios; será tu usuario.");
        notaId.getStyleClass().add("hint");
        notaId.setWrapText(true);

        VBox formulario = new VBox(16, campoEtiquetado(idNuevo, "ID de cliente"),
                campoEtiquetado(nombreNuevo, "Nombre completo"),
                campoEtiquetado(claveNueva, "Contraseña"), notaId, crear);
        return formulario;
    }

    private TextField campoTexto(String accesible, String ejemplo) {
        TextField campo = new TextField();
        campo.getStyleClass().add("text-field");
        campo.setPromptText(ejemplo);
        campo.setAccessibleText(accesible);
        return campo;
    }

    private VBox campoEtiquetado(TextField campo, String etiqueta) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("field-label");
        titulo.setLabelFor(campo);
        campo.textProperty().addListener((obs, anterior, actual) -> ocultarError());
        return new VBox(6, titulo, campo);
    }

    /** Un boton por cada cuenta registrada: los demo y las creadas desde la vista. */
    private HBox construirChipsUsuarios() {
        HBox chips = new HBox(8);
        for (Cliente cliente : banco.listarClientes()) {
            Button chip = new Button(cliente.getId());
            chip.getStyleClass().add("chip");
            chip.setAccessibleText("Usar la cuenta " + cliente.getId());
            chip.setOnAction(evento -> usarCuenta(cliente.getId()));
            chips.getChildren().add(chip);
        }
        return chips;
    }

    private void usarCuenta(String id) {
        mostrarMomento(true);
        idAcceso.setText(id);
        claveAcceso.clear();
        Platform.runLater(claveAcceso::requestFocus);
    }

    private HBox construirFilaSuperior() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Button tema = new Button(oscuro ? "Tema claro" : "Tema oscuro");
        tema.getStyleClass().add("chip");
        tema.setGraphic(Ui.icono(oscuro ? Feather.SUN : Feather.MOON, 13));
        tema.setAccessibleText(oscuro ? "Cambiar al tema claro" : "Cambiar al tema oscuro");
        tema.setOnAction(evento -> alTema.accept(!oscuro));

        HBox fila = new HBox(8, espacio, tema);
        fila.setAlignment(Pos.CENTER_RIGHT);
        fila.setMaxWidth(Double.MAX_VALUE);
        return fila;
    }

    private void configurarError() {
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        error.setAccessibleRole(AccessibleRole.TEXT);
    }

    private void mostrarMomento(boolean acceder) {
        momentoAcceder.setSelected(acceder);
        momentoCrear.setSelected(!acceder);
        formularioAcceso.setVisible(acceder);
        formularioAcceso.setManaged(acceder);
        formularioCreacion.setVisible(!acceder);
        formularioCreacion.setManaged(!acceder);
        ocultarError();
        Platform.runLater(() -> (acceder ? idAcceso : idNuevo).requestFocus());
    }

    private void intentarAcceder() {
        String id = normalizar(idAcceso.getText());
        String clave = texto(claveAcceso.getText());
        if (id.isEmpty() || clave.isEmpty()) {
            mostrarError("Escribe tu ID y tu contraseña. Las cuentas de prueba usan la contraseña demo.");
            return;
        }
        if (!banco.autenticar(id, clave)) {
            mostrarError("ID o contraseña incorrectos.");
            return;
        }
        alEntrar.accept(banco.buscarCliente(id));
    }

    private void intentarCrearCuenta() {
        String id = normalizar(idNuevo.getText());
        String nombre = texto(nombreNuevo.getText());
        String clave = texto(claveNueva.getText());
        if (id.isEmpty() || nombre.isEmpty() || clave.isEmpty()) {
            mostrarError("Completa el ID, el nombre y la contraseña para crear tu cuenta.");
            return;
        }
        try {
            Cliente cliente = banco.registrarCliente(id, nombre, clave);
            alEntrar.accept(cliente);
        } catch (OperacionInvalidaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void mostrarError(String mensaje) {
        error.setText(mensaje);
        error.setAccessibleText(mensaje);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void ocultarError() {
        if (!error.isVisible()) return;
        error.setText("");
        error.setAccessibleText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private void adaptar(double ancho) {
        boolean nuevoCompacto = ancho < ANCHO_COMPACTO;
        if (inicializado && nuevoCompacto == compacto) return;
        compacto = nuevoCompacto;
        inicializado = true;

        pase.setCompacto(compacto);
        if (compacto) {
            if (!getStyleClass().contains("login-view--compact")) getStyleClass().add("login-view--compact");
            cuerpoAmplio.getChildren().clear();
            pagina.getChildren().setAll(filaSuperior, pase, formulario);
        } else {
            getStyleClass().remove("login-view--compact");
            pagina.getChildren().setAll(filaSuperior, cuerpoAmplio);
            cuerpoAmplio.getChildren().setAll(pase, formulario);
        }
    }
}
