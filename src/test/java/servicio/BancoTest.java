package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import modelo.Beneficio;
import modelo.MovimientoPuntos;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import org.junit.jupiter.api.Test;

class BancoTest {

    private static Banco bancoConClientes() {
        Banco banco = new Banco();
        banco.registrarCliente("C1", "Ana");
        banco.registrarCliente("C2", "Luis");
        return banco;
    }

    @Test
    void registrarClienteValidaDatosYDuplicados() {
        Banco banco = new Banco();

        assertThrows(OperacionInvalidaException.class, () -> banco.registrarCliente(null, "Ana"));
        assertThrows(OperacionInvalidaException.class, () -> banco.registrarCliente(" ", "Ana"));
        assertThrows(OperacionInvalidaException.class, () -> banco.registrarCliente("C1", null));
        assertThrows(OperacionInvalidaException.class, () -> banco.registrarCliente("C1", " "));

        banco.registrarCliente("C1", "Ana");
        assertThrows(OperacionInvalidaException.class, () -> banco.registrarCliente("C1", "Otra"));
        assertEquals(1, banco.listarClientes().tamano());
    }

    @Test
    void depositarAcreditaYOtorgaPuntos() {
        Banco banco = bancoConClientes();

        Transaccion t = banco.depositar("C1", new BigDecimal("1000"));

        assertEquals(TipoTransaccion.DEPOSITO, t.getTipo());
        assertEquals(0, t.getComision().compareTo(BigDecimal.ZERO));
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("1000")));
        assertEquals(10, banco.consultarPuntos("C1").getAcumulados());
        assertEquals(1, banco.consultarHistorial("C1").tamano());
    }

    @Test
    void depositarRechazaMontoInvalido() {
        Banco banco = bancoConClientes();

        assertThrows(OperacionInvalidaException.class, () -> banco.depositar("C1", null));
        assertThrows(OperacionInvalidaException.class, () -> banco.depositar("C1", BigDecimal.ZERO));
        assertThrows(OperacionInvalidaException.class, () -> banco.depositar("C1", new BigDecimal("-1")));
    }

    @Test
    void retirarCobraComisionYValidaSaldo() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));

        Transaccion t = banco.retirar("C1", new BigDecimal("200"));

        assertEquals(TipoTransaccion.RETIRO, t.getTipo());
        assertEquals(0, t.getComision().compareTo(new BigDecimal("2.00")));
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("798")));
        assertEquals(14, banco.consultarPuntos("C1").getAcumulados());

        OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                () -> banco.retirar("C1", new BigDecimal("99999")));
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    }

    @Test
    void transferirMueveSaldoYCobraComisionAlOrigen() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));

        Transaccion t = banco.transferir("C1", "C2", new BigDecimal("300"));

        assertEquals(TipoTransaccion.TRANSFERENCIA_ENVIADA, t.getTipo());
        assertEquals("C2", t.getContraparteId());
        assertEquals(0, t.getComision().compareTo(new BigDecimal("6.00")));
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("694")));
        assertEquals(0, banco.consultarSaldo("C2").compareTo(new BigDecimal("300")));
        assertEquals(19, banco.consultarPuntos("C1").getAcumulados());
        assertEquals(0, banco.consultarPuntos("C2").getAcumulados());
        assertEquals(2, banco.consultarHistorial("C1").tamano());
        assertEquals(1, banco.consultarHistorial("C2").tamano());
        assertEquals(TipoTransaccion.TRANSFERENCIA_RECIBIDA,
                banco.consultarHistorial("C2").obtener(0).getTipo());
    }

    @Test
    void transferirAMismaCuentaOClienteInexistenteLanza() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));

        assertThrows(OperacionInvalidaException.class,
                () -> banco.transferir("C1", "C1", new BigDecimal("10")));
        assertThrows(OperacionInvalidaException.class,
                () -> banco.transferir("C1", "X", new BigDecimal("10")));
    }

    @Test
    void canjeBonoSaldoAcreditaCincuenta() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("100000"));

        banco.canjearBeneficio("C1", Beneficio.BONO_SALDO);

        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("100050")));
        assertEquals(0, banco.consultarPuntos("C1").getDisponibles());
        assertEquals(1000, banco.consultarPuntos("C1").getAcumulados());
        assertEquals(TipoTransaccion.BONO, banco.consultarHistorial("C1").obtener(1).getTipo());
    }

    @Test
    void descuentoTransferenciaReduceComision() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("10000"));

        banco.canjearBeneficio("C1", Beneficio.DESCUENTO_TRANSFERENCIA);
        Transaccion t = banco.transferir("C1", "C2", new BigDecimal("1000"));

        assertEquals(0, t.getComision().compareTo(new BigDecimal("18.00")));
    }

    @Test
    void retiroGratisVenceTrasTreintaDias() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("100000"));
        banco.canjearBeneficio("C1", Beneficio.SIN_CARGOS_RETIRO);

        Transaccion primero = banco.retirar("C1", new BigDecimal("1000"));
        assertEquals(0, primero.getComision().compareTo(new BigDecimal("0.00")));

        banco.getReloj().avanzarDias(31);
        Transaccion segundo = banco.retirar("C1", new BigDecimal("1000"));
        assertEquals(0, segundo.getComision().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void rankingOrdenaPorPuntos() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));
        banco.depositar("C2", new BigDecimal("2000"));

        var ranking = banco.ranking(2);

        assertEquals(2, ranking.tamano());
        assertEquals("C2", ranking.obtener(0).getClienteId());
        assertEquals("C1", ranking.obtener(1).getClienteId());
    }

    @Test
    void consultarHistorialPuntosYBuscarMovimiento() {
        Banco banco = bancoConClientes();
        Transaccion t = banco.depositar("C1", new BigDecimal("250"));

        assertEquals(1, banco.consultarHistorialPuntos("C1").tamano());
        MovimientoPuntos m = banco.buscarMovimientoPuntos("C1", t.getId());
        assertNotNull(m);
        assertEquals(t.getId(), m.getTransaccionId());
        assertEquals(2, m.getPuntos());
        assertNull(banco.buscarMovimientoPuntos("C1", 999999));
    }

    @Test
    void historialPuntosYBusquedaLanzanSiClienteNoExiste() {
        Banco banco = new Banco();

        assertThrows(OperacionInvalidaException.class, () -> banco.consultarHistorialPuntos("X"));
        assertThrows(OperacionInvalidaException.class, () -> banco.buscarMovimientoPuntos("X", 1));
    }

    @Test
    void rankingCompletoIncluyeATodosLosClientes() {
        Banco banco = bancoConClientes();
        banco.registrarCliente("C3", "Mia");
        banco.depositar("C1", new BigDecimal("1000"));
        banco.depositar("C3", new BigDecimal("500"));

        var ranking = banco.rankingCompleto();

        assertEquals(3, ranking.tamano());
        assertEquals("C1", ranking.obtener(0).getClienteId());
        assertEquals("C3", ranking.obtener(1).getClienteId());
        assertEquals("C2", ranking.obtener(2).getClienteId());
    }
}
