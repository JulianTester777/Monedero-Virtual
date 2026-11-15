package interfaz;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.Rango;

import java.util.ArrayList;
import java.util.List;

/** Escalera de rangos: vertical en escritorio y horizontal en ventanas angostas. */
final class RangeLadder extends StackPane {

    private final VBox vertical = new VBox(3);
    private final HBox horizontal = new HBox(6);
    private final List<RangeStep> verticalSteps = new ArrayList<>();
    private final List<CompactStep> horizontalSteps = new ArrayList<>();

    RangeLadder() {
        getStyleClass().add("range-ladder");
        vertical.getStyleClass().add("range-ladder-vertical");
        horizontal.getStyleClass().add("range-ladder-horizontal");

        for (Rango rango : Rango.values()) {
            RangeStep paso = new RangeStep(rango);
            CompactStep compacto = new CompactStep(rango);
            verticalSteps.add(paso);
            horizontalSteps.add(compacto);
            vertical.getChildren().add(paso);
            horizontal.getChildren().add(compacto);
            HBox.setHgrow(compacto, Priority.ALWAYS);
            compacto.setMaxWidth(Double.MAX_VALUE);
        }

        getChildren().addAll(vertical, horizontal);
        setCompacto(false);
    }

    void setCompacto(boolean compacto) {
        vertical.setVisible(!compacto);
        vertical.setManaged(!compacto);
        horizontal.setVisible(compacto);
        horizontal.setManaged(compacto);
    }

    void setRango(Rango actual) {
        for (RangeStep paso : verticalSteps) paso.setActivo(paso.rango == actual);
        for (CompactStep paso : horizontalSteps) paso.setActivo(paso.rango == actual);
    }

    private static String claseRango(Rango rango) {
        return switch (rango) {
            case BRONCE -> "range--bronce";
            case PLATA -> "range--plata";
            case ORO -> "range--oro";
            case PLATINO -> "range--platino";
        };
    }

    private static String limite(Rango rango) {
        if (rango == Rango.PLATINO) return Ui.puntos(rango.getDesde()) + "+ pts";
        return Ui.puntos(rango.getDesde()) + "–" + Ui.puntos(rango.getHasta()) + " pts";
    }

    private static String limiteCorto(Rango rango) {
        return switch (rango) {
            case BRONCE -> "0–500";
            case PLATA -> "501–1k";
            case ORO -> "1k–5k";
            case PLATINO -> "5k+";
        };
    }

    private static final class RangeStep extends HBox {

        private final Rango rango;
        private final Label estado;

        RangeStep(Rango rango) {
            this.rango = rango;
            getStyleClass().addAll("range-step", claseRango(rango));
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(10);
            setMaxWidth(Double.MAX_VALUE);

            Region barra = new Region();
            barra.getStyleClass().add("range-bar");
            barra.setPrefSize(4, 26);

            VBox texto = new VBox(1);
            Label nombre = new Label(rango.getNombre());
            nombre.getStyleClass().add("range-name");
            Label umbral = new Label(limite(rango));
            umbral.getStyleClass().add("range-threshold");
            texto.getChildren().addAll(nombre, umbral);
            HBox.setHgrow(texto, Priority.ALWAYS);

            estado = new Label("Actual");
            estado.getStyleClass().add("range-state");
            estado.setVisible(false);

            getChildren().addAll(barra, texto, estado);
        }

        void setActivo(boolean activo) {
            if (activo) {
                if (!getStyleClass().contains("range-step--active")) getStyleClass().add("range-step--active");
            } else {
                getStyleClass().remove("range-step--active");
            }
            estado.setVisible(activo);
        }
    }

    private static final class CompactStep extends VBox {

        private final Rango rango;

        CompactStep(Rango rango) {
            this.rango = rango;
            getStyleClass().addAll("range-pill", claseRango(rango));
            setSpacing(3);
            setMaxWidth(Double.MAX_VALUE);

            Region barra = new Region();
            barra.getStyleClass().add("range-pill-bar");
            barra.setPrefHeight(4);
            barra.setMaxWidth(Double.MAX_VALUE);

            Label nombre = new Label(rango.getNombre());
            nombre.getStyleClass().add("range-pill-name");
            Label limite = new Label(limiteCorto(rango));
            limite.getStyleClass().add("range-pill-limit");

            getChildren().addAll(barra, nombre, limite);
        }

        void setActivo(boolean activo) {
            if (activo) {
                if (!getStyleClass().contains("range-pill--active")) getStyleClass().add("range-pill--active");
            } else {
                getStyleClass().remove("range-pill--active");
            }
        }
    }
}
