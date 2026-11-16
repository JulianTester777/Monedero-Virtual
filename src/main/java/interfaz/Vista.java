package interfaz;

import javafx.scene.Node;

/** Contrato común de las pantallas que se muestran dentro del dashboard. */
interface Vista {

    Node raiz();

    void actualizar();
}
