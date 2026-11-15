package interfaz;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import modelo.Cliente;
import servicio.Banco;

import java.math.BigDecimal;
import java.util.Objects;

/** Demo de la interfaz JavaFX del monedero. */
public final class Main extends Application {

    private Banco banco;
    private Scene escena;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        banco = crearBancoDemo();
        escena = new Scene(new StackPane(), 1120, 720);
        escena.getStylesheets().add(Objects.requireNonNull(
                Main.class.getResource("/interfaz/app.css"),
                "No se encontró /interfaz/app.css").toExternalForm());

        stage.setTitle("Monedero");
        stage.setMinWidth(360);
        stage.setMinHeight(640);
        stage.setScene(escena);
        mostrarLogin();
        stage.show();
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
        return banco;
    }
}