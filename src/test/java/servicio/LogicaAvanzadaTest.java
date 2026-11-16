package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import modelo.TipoMonedero;
import modelo.Transaccion;
import modelo.TransaccionProgramada;
import org.junit.jupiter.api.Test;

class LogicaAvanzadaTest {

    private static Banco bancoConClientes() {
        Banco banco = new Banco();
        banco.registrarCliente("C1", "Ana");
        banco.registrarCliente("C2", "Luis");
        return banco;
    }

    @Test
    void revertirDepositoDevuelveSaldoYPuntos() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));

        Transaccion revertida = banco.revertirUltimaTransaccion("C1");

        assertTrue(revertida.estaRevertida());
        assertEquals(0, banco.consultarSaldo("C1").compareTo(BigDecimal.ZERO));
        assertEquals(0, banco.consultarPuntos("C1").getAcumulados());
        assertEquals(0, banco.consultarPuntos("C1").getDisponibles());
        assertThrows(OperacionInvalidaException.class, () -> banco.revertirUltimaTransaccion("C1"));
        assertTrue(banco.verificarIntegridad().esValido());
    }

    @Test
    void revertirTransferenciaAfectaAmbosLadosYRanking() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));
        Transaccion enviada = banco.transferir("C1", "C2", new BigDecimal("300"));

        Transaccion revertida = banco.revertirUltimaTransaccion("C1");

        assertEquals(enviada.getId(), revertida.getId());
        assertTrue(enviada.estaRevertida());
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("1000")));
        assertEquals(0, banco.consultarSaldo("C2").compareTo(BigDecimal.ZERO));
        assertEquals(10, banco.consultarPuntos("C1").getAcumulados());
        assertEquals(0, banco.consultarPuntos("C2").getAcumulados());
        assertTrue(banco.verificarIntegridad().esValido());
    }

    @Test
    void noRevierteSiLosPuntosYaSeGastaron() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("100000"));
        banco.canjearBeneficio("C1", modelo.Beneficio.BONO_SALDO);

        assertThrows(OperacionInvalidaException.class, () -> banco.revertirUltimaTransaccion("C1"));
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("100050")));
    }

    @Test
    void ejecutaProgramadasAlAvanzarElReloj() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("500"));
        LocalDateTime fecha = banco.getReloj().ahora().plusDays(1);

        TransaccionProgramada p = banco.programarDeposito("C1", new BigDecimal("200"), fecha);

        assertEquals(1, banco.consultarProgramadas("C1").tamano());
        assertEquals(0, banco.procesarProgramadas().tamano());
        banco.getReloj().avanzarDias(1);
        assertEquals(1, banco.procesarProgramadas().tamano());
        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("700")));
        assertEquals(modelo.EstadoProgramada.EJECUTADA, p.getEstado());
    }

    @Test
    void programadaTransferenciaFallidaGeneraNotificacion() {
        Banco banco = bancoConClientes();
        LocalDateTime fecha = banco.getReloj().ahora().plusHours(1);
        TransaccionProgramada p = banco.programarTransferencia("C1", "C2", new BigDecimal("500"), fecha);
        banco.getReloj().avanzarHoras(1);

        banco.procesarProgramadas();

        assertEquals(modelo.EstadoProgramada.FALLIDA, p.getEstado());
        assertTrue(banco.notificacionesNoLeidas("C1") > 0);
    }

    @Test
    void notificacionesSePuedenMarcarComoLeidas() {
        Banco banco = bancoConClientes();
        banco.configurarUmbralSaldoBajo(new BigDecimal("2000"));
        banco.depositar("C1", new BigDecimal("1000"));

        assertTrue(banco.notificacionesNoLeidas("C1") > 0);
        int id = banco.consultarNotificaciones("C1").obtener(0).getId();
        assertTrue(banco.marcarNotificacionLeida(id));
        assertEquals(0, banco.notificacionesNoLeidas("C1"));
    }

    @Test
    void reportesRegistranCategoriasYTransferencias() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));
        banco.transferir("C1", "C2", new BigDecimal("300"));
        banco.transferir("C2", "C1", new BigDecimal("100"));

        assertFalse(banco.generarReportePatrones("C1").estaVacia());
        assertFalse(banco.categoriasMasUsadas("C1").estaVacia());
        assertEquals(2, banco.flujoEntreClientes().tamano());
        assertTrue(banco.detectarCiclosEnReportes());
        assertFalse(banco.caminoEntreClientes("C1", "C2").estaVacia());
    }

    @Test
    void multimonederoMueveSaldoEntreMonederos() {
        Banco banco = bancoConClientes();
        banco.depositar("C1", new BigDecimal("1000"));
        banco.crearMonedero("C1", "ahorro", "Ahorro", TipoMonedero.AHORRO);

        banco.transferirEntreMonederos("C1", "principal", "ahorro", new BigDecimal("300"));

        assertEquals(0, banco.consultarSaldo("C1").compareTo(new BigDecimal("700")));
        assertEquals(0, banco.consultarSaldo("C1", "ahorro").compareTo(new BigDecimal("300")));
        assertEquals(0, banco.consultarSaldoTotal("C1").compareTo(new BigDecimal("1000")));
        assertTrue(banco.verificarIntegridad().esValido());
    }
}
