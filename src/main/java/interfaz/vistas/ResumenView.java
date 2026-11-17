package interfaz.vistas;

import interfaz.Ui;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import modelo.Cliente;
import servicio.Banco;
import servicio.dto.MovimientoDto;
import servicio.dto.ResumenDto;

/** Vista inicial: saldo, puntos y los últimos movimientos. */
public final class ResumenView implements Vista {

    private static final int RECIENTES = 5;

    private final Banco banco;
    private final Cliente cliente;
    private final Label saldo = new Label();
    private final Label saldoTotal = new Label();
    private final Label disponibles = new Label();
    private final Label acumulados = new Label();
    private final Label rango = new Label();
    private final ListView<MovimientoDto> lista = new ListView<>();
    private final VBox raiz;

    public ResumenView(Banco banco, Cliente cliente) {
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
        ResumenDto resumen = banco.consultarResumenVista(cliente.getId());
        saldo.setText(Ui.monto(resumen.saldo()));
        saldoTotal.setText(Ui.monto(resumen.saldoTotal()));
        disponibles.setText(Ui.puntos(resumen.puntosDisponibles()) + " pts");
        acumulados.setText(Ui.puntos(resumen.puntosAcumulados()) + " pts");
        rango.setText(resumen.rango());
        lista.setItems(recientes());
    }

    private VBox construir() {
        Label titulo = new Label("Hola, " + cliente.getNombre() + ".");
        titulo.getStyleClass().add("display-title");
        titulo.setWrapText(true);

        Label intro = new Label("Este es el resumen de tu monedero.");
        intro.getStyleClass().add("lead");
        intro.setWrapText(true);

        FlowPane tarjetas = new FlowPane(14, 14,
                tarjeta("Saldo en el monedero principal", saldo, Feather.CREDIT_CARD),
                tarjeta("Saldo total", saldoTotal, Feather.POCKET),
                tarjeta("Puntos disponibles", disponibles, Feather.STAR),
                tarjeta("Puntos acumulados", acumulados, Feather.TRENDING_UP),
                tarjeta("Rango actual", rango, Feather.AWARD));
        tarjetas.getStyleClass().add("stat-grid");

        Label tituloMovimientos = Ui.seccion("Últimos movimientos", Feather.LIST);

        lista.setPrefHeight(260);
        lista.setPlaceholder(new Label("Aún no hay movimientos. Empieza con un depósito."));
        lista.setAccessibleText("Últimos movimientos");
        lista.setCellFactory(v -> new MovimientoCell());
        VBox.setVgrow(lista, Priority.ALWAYS);

        VBox contenido = new VBox(22, titulo, intro, tarjetas, tituloMovimientos, lista);
        contenido.getStyleClass().add("view-root");
        return contenido;
    }

    private VBox tarjeta(String etiqueta, Label valor, Feather glifo) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("stat-label");
        titulo.setGraphic(Ui.icono(glifo, 12));
        valor.getStyleClass().add("stat-value");

        VBox tarjeta = new VBox(6, titulo, valor);
        tarjeta.getStyleClass().add("stat-card");
        return tarjeta;
    }

    private ObservableList<MovimientoDto> recientes() {
        ObservableList<MovimientoDto> movimientos = FXCollections.observableArrayList();
        for (MovimientoDto movimiento : banco.consultarMovimientosRecientesVista(cliente.getId(), RECIENTES)) {
            movimientos.add(0, movimiento);
        }
        return movimientos;
    }
}
