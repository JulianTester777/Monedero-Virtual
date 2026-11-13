package servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reglas de comisiones. Centralizadas aqui para poder cambiarlas en un solo lugar. */
public final class Comisiones {

    public static final BigDecimal TASA_TRANSFERENCIA = new BigDecimal("0.02"); // 2%
    public static final BigDecimal TASA_RETIRO = new BigDecimal("0.01");        // 1%
    private static final BigDecimal FACTOR_DESCUENTO = new BigDecimal("0.90");  // 10% menos

    private Comisiones() { }

    public static BigDecimal transferencia(BigDecimal monto, boolean conDescuento) {
        BigDecimal tasa = conDescuento ? TASA_TRANSFERENCIA.multiply(FACTOR_DESCUENTO) : TASA_TRANSFERENCIA;
        return monto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal retiro(BigDecimal monto, boolean gratis) {
        if (gratis) return BigDecimal.ZERO.setScale(2);
        return monto.multiply(TASA_RETIRO).setScale(2, RoundingMode.HALF_UP);
    }
}