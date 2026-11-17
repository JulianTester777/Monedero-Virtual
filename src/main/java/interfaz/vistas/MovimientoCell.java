package interfaz.vistas;

import interfaz.Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import servicio.dto.MovimientoDto;

/** Fila compartida por el resumen, el historial y las reversiones. */
public final class MovimientoCell extends ListCell<MovimientoDto> {

    @Override
    protected void updateItem(MovimientoDto movimiento, boolean vacio) {
        super.updateItem(movimiento, vacio);
        if (vacio || movimiento == null) {
            setGraphic(null);
            setText(null);
            setAccessibleText(null);
            return;
        }

        VBox texto = crearTexto(movimiento);
        VBox dinero = crearDinero(movimiento);

        HBox fila = new HBox(16, texto, dinero);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("movement-row");

        String signo = movimiento.entrada() ? "+" : "−";
        setGraphic(fila);
        setText(null);
        setAccessibleText(movimiento.tipo() + (movimiento.revertida() ? ", revertida" : "")
                + ", " + signo + Ui.monto(movimiento.monto())
                + ", " + Ui.fecha(movimiento.fecha()));
    }

    private VBox crearTexto(MovimientoDto movimiento) {
        Label tipo = new Label(movimiento.tipo() + (movimiento.revertida() ? " · Revertida" : ""));
        tipo.getStyleClass().add("movement-type");

        Label meta = new Label(detalle(movimiento));
        meta.getStyleClass().add("movement-meta");
        meta.setWrapText(true);

        VBox texto = new VBox(3, tipo, meta);
        HBox.setHgrow(texto, Priority.ALWAYS);
        if (movimiento.puntos() != null) {
            Label puntos = new Label((movimiento.puntosEntrada() ? "+" : "−")
                    + Ui.puntos(movimiento.puntos()) + " pts");
            puntos.getStyleClass().add("movement-points");
            texto.getChildren().add(puntos);
        }
        return texto;
    }

    private String detalle(MovimientoDto movimiento) {
        String detalle = Ui.fecha(movimiento.fecha());
        if (movimiento.monederoDestino() != null) {
            detalle += ", " + movimiento.monederoOrigen() + " → " + movimiento.monederoDestino();
        } else if (movimiento.contraparte() != null && !movimiento.contraparte().isBlank()) {
            detalle += ", " + movimiento.contraparte();
        }
        return detalle;
    }

    private VBox crearDinero(MovimientoDto movimiento) {
        Label monto = new Label((movimiento.entrada() ? "+" : "−") + Ui.monto(movimiento.monto()));
        monto.getStyleClass().addAll("movement-amount",
                movimiento.entrada() ? "movement-amount--in" : "movement-amount--out");

        VBox dinero = new VBox(2, monto);
        dinero.setAlignment(Pos.CENTER_RIGHT);
        if (movimiento.comision().signum() > 0) {
            Label comision = new Label("Comisión " + Ui.monto(movimiento.comision()));
            comision.getStyleClass().add("movement-meta");
            dinero.getChildren().add(comision);
        }
        return dinero;
    }
}
