package persistencia;

import modelo.Cliente;
import modelo.tipo.EstadoProgramada;
import modelo.Notificacion;
import modelo.tipo.TipoMonedero;
import modelo.Transaccion;
import modelo.TransaccionProgramada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import servicio.Banco;
import servicio.OperacionInvalidaException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del registro de operaciones: cada caso escribe un registro con un
 * banco con persistencia y luego lo reproduce en un banco nuevo, tal como hace
 * la interfaz al arrancar. Los contadores de ids se reinician para simular una
 * JVM nueva.
 */
class RegistroOperacionesTest {

    @TempDir
    Path carpeta;

    private Path archivo;

    @BeforeEach
    void preparar() throws Exception {
        archivo = carpeta.resolve("monedero.log");
        reiniciarContadores();
    }

    /**
     * Reinicia los contadores de ids de Notificacion, Transaccion y
     * TransaccionProgramada: en la app real la reproduccion ocurre en una JVM
     * nueva, aqui se simula eso antes de cada reproduccion.
     */
    private static void reiniciarContadores() throws Exception {
        for (Class<?> clase : List.of(Notificacion.class, Transaccion.class, TransaccionProgramada.class)) {
            Field contador = clase.getDeclaredField("contador");
            contador.setAccessible(true);
            contador.setInt(null, 0);
        }
    }

    /** Banco que escribe el registro, como la app en su primera sesion. */
    private Banco escritor() {
        return new Banco(new RegistroOperaciones(archivo));
    }

    /** Banco nuevo que reproduce el registro, como la app al arrancar de nuevo. */
    private Banco lector() throws Exception {
        reiniciarContadores();
        Banco banco = new Banco(new RegistroOperaciones(archivo));
        new RegistroOperaciones(archivo).repetir(banco);
        return banco;
    }

    @Test
    void laReproduccionRestauraSaldosHistorialesYPuntos() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        banco.registrarCliente("bruno", "Bruno", "demo");
        banco.depositar("ana", new BigDecimal("65000"));
        banco.transferir("ana", "bruno", new BigDecimal("5000"));
        banco.retirar("ana", new BigDecimal("1000"));
        banco.crearMonedero("ana", "ahorro", "Ahorro", TipoMonedero.AHORRO);
        banco.transferirEntreMonederos("ana", Cliente.MONEDERO_PRINCIPAL, "ahorro", new BigDecimal("15000"));

        BigDecimal saldoAna = banco.consultarSaldo("ana");
        BigDecimal saldoBruno = banco.consultarSaldo("bruno");
        int movimientosAna = banco.consultarHistorial("ana").tamano();
        int puntosAna = banco.consultarPuntos("ana").getAcumulados();

        Banco restaurado = lector();
        assertEquals(0, saldoAna.compareTo(restaurado.consultarSaldo("ana")));
        assertEquals(0, saldoBruno.compareTo(restaurado.consultarSaldo("bruno")));
        assertEquals(movimientosAna, restaurado.consultarHistorial("ana").tamano());
        assertEquals(puntosAna, restaurado.consultarPuntos("ana").getAcumulados());
        assertEquals(0, new BigDecimal("15000").compareTo(restaurado.consultarSaldo("ana", "ahorro")));
    }

    @Test
    void laContrasenaNuncaSeGuardaEnTextoPlano() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "secreto123");

        String contenido = Files.readString(archivo);
        assertFalse(contenido.contains("secreto123"), "La contrasena no debe aparecer en el registro");
        assertTrue(contenido.contains("CREAR;ana"), "El alta debe quedar registrada");

        Banco restaurado = lector();
        assertTrue(restaurado.autenticar("ana", "secreto123"));
        assertFalse(restaurado.autenticar("ana", "otra"));
        assertTrue(Files.readString(archivo).contains("CREAR;ana"),
                "La linea CREAR debe sobrevivir a la reproduccion");
    }

    @Test
    void laReversionSeReproduce() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        banco.depositar("ana", new BigDecimal("1000"));
        banco.revertirUltimaTransaccion("ana");

        Banco restaurado = lector();
        assertEquals(0, BigDecimal.ZERO.compareTo(restaurado.consultarSaldo("ana")));
        assertTrue(restaurado.consultarHistorial("ana").obtener(0).estaRevertida());
    }

    @Test
    void laProgramadaEjecutadaNoSeEjecutaDosVeces() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        banco.getReloj().avanzarMinutos(1);
        banco.programarDeposito("ana", new BigDecimal("100"), banco.getReloj().ahora().plusSeconds(1));
        banco.getReloj().avanzarMinutos(1);
        assertEquals(1, banco.procesarProgramadasVista());
        BigDecimal saldo = banco.consultarSaldo("ana");

        Banco restaurado = lector();
        assertEquals(0, saldo.compareTo(restaurado.consultarSaldo("ana")),
                "El deposito ejecutado no debe duplicarse");
        assertEquals(EstadoProgramada.EJECUTADA, restaurado.consultarProgramadas("ana").obtener(0).getEstado());
        assertEquals(0, restaurado.procesarProgramadasVista());
        assertTrue(Files.readString(archivo).contains("EJECUTADA;"),
                "La linea EJECUTADA debe sobrevivir a la reproduccion");
    }

    @Test
    void laProgramadaCanceladaNoSeEjecuta() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        int id = banco.programarDeposito("ana", new BigDecimal("100"),
                banco.getReloj().ahora().plusDays(1)).getId();
        assertTrue(banco.cancelarProgramada(id));
        BigDecimal saldo = banco.consultarSaldo("ana");

        Banco restaurado = lector();
        assertEquals(EstadoProgramada.CANCELADA, restaurado.consultarProgramadas("ana").obtener(0).getEstado());
        restaurado.getReloj().avanzarDias(2);
        assertEquals(0, restaurado.procesarProgramadasVista());
        assertEquals(0, saldo.compareTo(restaurado.consultarSaldo("ana")));
    }

    @Test
    void elUmbralYElAvisoLeidoSeReproducen() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        banco.configurarUmbralSaldoBajo(new BigDecimal("100000"));
        banco.depositar("ana", new BigDecimal("100"));
        int idAviso = buscarAvisoSaldoBajo(banco).getId();
        assertTrue(banco.marcarNotificacionLeida(idAviso));

        Banco restaurado = lector();
        assertEquals(0, new BigDecimal("100000").compareTo(restaurado.consultarUmbralSaldoBajo()));
        assertTrue(buscarAvisoSaldoBajo(restaurado).estaLeida());
    }

    private Notificacion buscarAvisoSaldoBajo(Banco banco) {
        return banco.consultarNotificaciones("ana").obtener(0);
    }

    @Test
    void lasOperacionesFallidasNoSeRegistran() throws Exception {
        Banco banco = escritor();
        banco.registrarCliente("ana", "Ana", "demo");
        assertThrows(OperacionInvalidaException.class,
                () -> banco.retirar("ana", new BigDecimal("999999")));
        assertThrows(OperacionInvalidaException.class,
                () -> banco.transferir("ana", "nadie", new BigDecimal("10")));

        assertFalse(Files.readString(archivo).contains("RETIRO;"),
                "Una operacion que fallo no debe quedar en el registro");

        Banco restaurado = lector();
        assertEquals(0, BigDecimal.ZERO.compareTo(restaurado.consultarSaldo("ana")));
    }
}
