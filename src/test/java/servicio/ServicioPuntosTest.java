package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import estructuras.ListaEnlazada;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import modelo.Beneficio;
import modelo.CuentaPuntos;
import modelo.EntradaPuntos;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import org.junit.jupiter.api.Test;

class ServicioPuntosTest {

    private static Transaccion transaccion(TipoTransaccion tipo, String monto, String clienteId) {
        return new Transaccion(tipo, new BigDecimal(monto), BigDecimal.ZERO,
                LocalDateTime.now(), clienteId, null);
    }

    @Test
    void calcularPuntosPorTipoYDivisionEntera() {
        assertEquals(10, ServicioPuntos.calcularPuntos(TipoTransaccion.DEPOSITO, new BigDecimal("1000")));
        assertEquals(4, ServicioPuntos.calcularPuntos(TipoTransaccion.RETIRO, new BigDecimal("200")));
        assertEquals(9, ServicioPuntos.calcularPuntos(TipoTransaccion.TRANSFERENCIA_ENVIADA, new BigDecimal("300")));
        assertEquals(0, ServicioPuntos.calcularPuntos(TipoTransaccion.TRANSFERENCIA_RECIBIDA, new BigDecimal("300")));
        assertEquals(0, ServicioPuntos.calcularPuntos(TipoTransaccion.BONO, new BigDecimal("50")));
        assertEquals(1, ServicioPuntos.calcularPuntos(TipoTransaccion.DEPOSITO, new BigDecimal("199")));
        assertEquals(0, ServicioPuntos.calcularPuntos(TipoTransaccion.DEPOSITO, new BigDecimal("99")));
    }

    @Test
    void registrarYConsultar() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");

        CuentaPuntos cuenta = servicio.consultar("C1");

        assertEquals("C1", cuenta.getClienteId());
        assertEquals(0, cuenta.getAcumulados());
        assertEquals(0, cuenta.getDisponibles());
        assertThrows(OperacionInvalidaException.class, () -> servicio.consultar("X"));
    }

    @Test
    void otorgarSumaPuntosYActualizaRanking() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");

        int ganados = servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "250", "C1"));

        assertEquals(2, ganados);
        CuentaPuntos cuenta = servicio.consultar("C1");
        assertEquals(2, cuenta.getAcumulados());
        assertEquals(2, cuenta.getDisponibles());
        assertEquals(1, cuenta.getHistorial().tamano());

        ListaEnlazada<EntradaPuntos> top = servicio.top(1);
        assertEquals(1, top.tamano());
        assertEquals("C1", top.obtener(0).getClienteId());
        assertEquals(2, top.obtener(0).getPuntos());
    }

    @Test
    void otorgarCeroNoModificaNada() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");

        int ganados = servicio.otorgar(transaccion(TipoTransaccion.TRANSFERENCIA_RECIBIDA, "300", "C1"));

        assertEquals(0, ganados);
        assertEquals(0, servicio.consultar("C1").getAcumulados());
        assertEquals(0, servicio.consultar("C1").getHistorial().tamano());
    }

    @Test
    void topOrdenaDeMayorAMenor() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");
        servicio.registrarCliente("C2");
        servicio.registrarCliente("C3");
        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "1000", "C1"));
        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "500", "C2"));
        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "2000", "C3"));

        ListaEnlazada<EntradaPuntos> top = servicio.top(3);

        assertEquals(3, top.tamano());
        assertEquals("C3", top.obtener(0).getClienteId());
        assertEquals("C1", top.obtener(1).getClienteId());
        assertEquals("C2", top.obtener(2).getClienteId());
    }

    @Test
    void canjearValidaPuntosYDuplicados() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");
        LocalDateTime ahora = LocalDateTime.now();

        assertThrows(OperacionInvalidaException.class,
                () -> servicio.canjear("C1", Beneficio.DESCUENTO_TRANSFERENCIA, ahora));

        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "10000", "C1"));
        servicio.canjear("C1", Beneficio.DESCUENTO_TRANSFERENCIA, ahora);

        assertEquals(0, servicio.consultar("C1").getDisponibles());
        assertThrows(OperacionInvalidaException.class,
                () -> servicio.canjear("C1", Beneficio.DESCUENTO_TRANSFERENCIA, ahora));
    }

    @Test
    void canjearRetiroGratisActivaTreintaDias() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");
        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "50000", "C1"));
        LocalDateTime ahora = LocalDateTime.now();

        servicio.canjear("C1", Beneficio.SIN_CARGOS_RETIRO, ahora);

        CuentaPuntos cuenta = servicio.consultar("C1");
        assertEquals(0, cuenta.getDisponibles());
        assertTrue(cuenta.tieneRetiroGratis(ahora.plusDays(29)));
        assertFalse(cuenta.tieneRetiroGratis(ahora.plusDays(30)));
        assertFalse(cuenta.tieneRetiroGratis(ahora.plusDays(31)));
    }

    @Test
    void canjearBonoSoloGastaPuntos() {
        ServicioPuntos servicio = new ServicioPuntos();
        servicio.registrarCliente("C1");
        servicio.otorgar(transaccion(TipoTransaccion.DEPOSITO, "100000", "C1"));

        servicio.canjear("C1", Beneficio.BONO_SALDO, LocalDateTime.now());

        CuentaPuntos cuenta = servicio.consultar("C1");
        assertEquals(1000, cuenta.getAcumulados());
        assertEquals(0, cuenta.getDisponibles());
    }
}
