package interfaz;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import modelo.Cliente;
import modelo.tipo.TipoMonedero;
import persistencia.RegistroOperaciones;
import servicio.Banco;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Demo de la interfaz JavaFX del monedero. */
public final class Main extends Application {

    private static final List<String> ESTILOS = List.of(
            "base.css", "components.css", "login.css",
            "pass-card.css", "dashboard.css", "views.css");

    private static final List<String> FUENTES = List.of(
            "fonts/ZenOldMincho-Regular.ttf", "fonts/ZenOldMincho-Bold.ttf",
            "fonts/ZenKakuGothicNew-Regular.ttf", "fonts/ZenKakuGothicNew-Bold.ttf");

    private Banco banco;
    private Scene escena;
    private Cliente clienteActual;
    private boolean temaOscuro;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        cargarFuentes();
        banco = crearBanco();
        escena = new Scene(new StackPane(), 1120, 720);
        cargarEstilos();

        stage.setTitle("Monedero");
        stage.setMinWidth(360);
        stage.setMinHeight(640);
        stage.setScene(escena);
        mostrarLogin();
        stage.show();
    }

    private void cargarEstilos() {
        for (String hoja : ESTILOS) {
            escena.getStylesheets().add(Objects.requireNonNull(
                    Main.class.getResource("/interfaz/" + hoja),
                    "No se encontró /interfaz/" + hoja).toExternalForm());
        }
    }

    private void cargarFuentes() {
        for (String fuente : FUENTES) {
            try (InputStream flujo = Main.class.getResourceAsStream("/interfaz/" + fuente)) {
                if (flujo != null) {
                    Font.loadFont(flujo, 12);
                }
            } catch (IOException ex) {
                System.err.println("No se pudo cargar la fuente " + fuente);
            }
        }
    }

    private void mostrarLogin() {
        clienteActual = null;
        escena.setRoot(new LoginView(banco, this::mostrarDashboard, this::alternarTema, temaOscuro));
    }

    private void mostrarDashboard(Cliente cliente) {
        clienteActual = cliente;
        escena.setRoot(new DashboardView(banco, cliente,
                this::mostrarLogin, this::alternarTema, temaOscuro));
    }

    private void alternarTema(boolean oscuro) {
        temaOscuro = oscuro;
        if (clienteActual != null) {
            mostrarDashboard(clienteActual);
        } else {
            mostrarLogin();
        }
    }

    /**
     * Crea el banco con persistencia en monedero.log: si el registro existe de
     * una sesion anterior, se reproduce; si no, se cargan las cuentas demo.
     */
    private Banco crearBanco() {
        RegistroOperaciones registro = new RegistroOperaciones(Path.of("monedero.log"));
        Banco banco = new Banco(registro);
        if (registro.existe()) {
            registro.repetir(banco);
        } else {
            cargarDemo(banco);
        }
        return banco;
    }

    private void cargarDemo(Banco banco) {
        banco.registrarCliente("ana", "Ana Torres", "demo");
        banco.registrarCliente("bruno", "Bruno Ríos", "demo");
        banco.registrarCliente("carla", "Carla Méndez", "demo");

        banco.depositar("ana", new BigDecimal("65000"));
        banco.transferir("ana", "bruno", new BigDecimal("5000"));
        banco.depositar("bruno", new BigDecimal("120000"));
        banco.retirar("bruno", new BigDecimal("10000"));
        banco.depositar("carla", new BigDecimal("4000"));
        banco.crearMonedero("ana", "ahorro", "Ahorro", TipoMonedero.AHORRO);
        banco.transferirEntreMonederos("ana", Cliente.MONEDERO_PRINCIPAL, "ahorro", new BigDecimal("15000"));
        banco.programarDeposito("carla", new BigDecimal("5000"), LocalDateTime.now().plusDays(7));
    }
}