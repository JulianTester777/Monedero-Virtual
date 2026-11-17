package interfaz.vistas;

import javafx.scene.Node;

/** Contrato común de las pantallas que se muestran dentro del dashboard. */
public interface Vista {

    Node raiz();

    void actualizar();
}
