---
version: "1.0"
name: "Shizen"
description: "Interfaz JavaFX con estética japonesa (kanso, ma, wabi-sabi). Accesible."

colors:
  shu: "#9B3B3B"
  shu-dark: "#7A2E2E"
  shu-light: "#C46A6A"
  sumi: "#2C2C2E"
  sumi-light: "#4A4A4E"
  washi: "#F5F1EA"
  washi-dark: "#EDE8DF"
  kin: "#B5A78C"
  moss: "#5E6E52"
  kin-ash: "#8A8580"

typography:
  display:
    fontFamily: "Zen Old Mincho"
    fontSize: 28px
  heading:
    fontFamily: "Zen Old Mincho"
    fontSize: 20px
  body:
    fontFamily: "Zen Kaku Gothic New"
    fontSize: 14px
    lineHeight: 1.8
  label:
    fontFamily: "Zen Kaku Gothic New"
    fontSize: 12px
    fontWeight: bold

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 32px
  xl: 64px

rounded:
  sm: 2px
  md: 4px
  full: 9999px
---

## Overview

Interfaz guiada por **kanso** (simplicidad) y **ma** (espacio intencional).
Paleta de dos familias: **rojo shu** (acento, < 10% de superficie) y **carbón cálido** (texto, estructura).
Fondos en tonos de papel. Sin gradientes, sin sombras.

---

## Layout Strategy

JavaFX no tiene media queries ni CSS Grid. La responsividad se logra con **layout managers + Region con min/pref/max**.

### Reglas

| Principio | Implementación |
|---|---|
| Columnas elásticas | `GridPane` con `ColumnConstraints.setHgrow(Priority.ALWAYS)` |
| Filas elásticas | `RowConstraints.setVgrow(Priority.ALWAYS)` |
| Contenido centrado | `Region` con `setMinWidth(Region.USE_PREF_SIZE)` + `HBox` con `setFillWidth(true)` |
| Apilado vertical | `VBox` con `setSpacing(spacing.md)` |
| Sidebar + contenido | `BorderPane` (left + center) o `SplitPane` |
| Overflow | `ScrollPane` solo como último recurso |
| Padding en bordes | `setPadding(new Insets(lg, lg, lg, lg))` en contenedor raíz |

### Reglas de tamaño

- **Nunca** `setPrefWidth` / `setPrefHeight` en contenedores principales
- `Region` con `minWidth=0`, `prefWidth=600`, `maxWidth=Double.MAX_VALUE` para áreas fluidas
- `setFixedCellSize(44)` en `TableView` para filas táctiles
- Touch targets: `-fx-min-width: 44px; -fx-min-height: 44px` en todo control interactivo
- `maxWidth` en `Label` para limitar medida a ~65ch (aprox. 500px en body 14px)

   