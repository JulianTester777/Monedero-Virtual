package interfaz;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import modelo.Cliente;
import modelo.TipoMonedero;
import servicio.Banco;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Demo de la interfaz JavaFX del monedero. */
public final class Main extends Application {

    private static final List<String> ESTILOS = List.of(
            "base.css", "components.css", "login.css",
            "pass-card.css", "dashboard.css", "views.css");

    private Banco banco;
    private Scene escena;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        banco = crearBancoDemo();
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

    private void mostrarLogin() {
        escena.setRoot(new LoginView(banco, this::mostrarDashboard));
    }

    private void mostrarDashboard(Cliente cliente) {
        escena.setRoot(new DashboardView(banco, cliente, this::mostrarLogin));
    }

    private Banco crearBancoDemo() {
        Banco banco = new Banco();
        banco.registrarCliente("ana", "Ana Torres");
        banco.registrarCliente("bruno", "Bruno Ríos");
        banco.registrarCliente("carla", "Carla Méndez");

        banco.depositar("ana", new BigDecimal("65000"));
        banco.transferir("ana", "bruno", new BigDecimal("5000"));
        banco.depositar("bruno", new BigDecimal("120000"));
        banco.retirar("bruno", new BigDecimal("10000"));
        banco.depositar("carla", new BigDecimal("4000"));
        banco.crearMonedero("ana", "ahorro", "Ahorro", TipoMonedero.AHORRO);
        banco.transferirEntreMonederos("ana", Cliente.MONEDERO_PRINCIPAL, "ahorro", new BigDecimal("15000"));
        banco.programarDeposito("carla", new BigDecimal("5000"), LocalDateTime.now().plusDays(7));
        return banco;
    }
}