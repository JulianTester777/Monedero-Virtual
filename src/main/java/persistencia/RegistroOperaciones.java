package persistencia;

import modelo.tipo.Beneficio;
import modelo.tipo.Recurrencia;
import modelo.tipo.TipoMonedero;
import servicio.Banco;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Persistencia del monedero sin base de datos: cada operacion del usuario se
 * agrega como una linea a un archivo de texto. Al arrancar la app,
 * {@link #repetir(Banco)} vuelve a ejecutar esas lineas con los metodos
 * publicos de Banco, de modo que el estado derivado (puntos, ranking,
 * reportes, historiales y avisos) se reconstruye solo.
 *
 * Formato: campos separados por ';'. Tipos de linea: CREAR, DEPOSITO, RETIRO,
 * TRANSFERENCIA, MOVIMIENTO, MONEDERO, CANJE, PROGRAMAR, CANCELAR_PROGRAMADA,
 * EJECUTADA, FALLIDA, REPROGRAMAR, REVERSION, LEIDA y UMBRAL.
 *
 * La reproduccion asume un Banco recien creado (como hace la app al arrancar),
 * porque los ids de transacciones, programadas y avisos se reasignan en orden.
 * Lo que no se puede repetir se descarta con un aviso: el archivo queda
 * consistente con el estado realmente reconstruido.
 */
public final class RegistroOperaciones {

    private final Path archivo;

    public RegistroOperaciones(Path archivo) {
        this.archivo = archivo;
    }

    /** true si ya existe un registro de una sesion anterior. */
    public boolean existe() {
        return Files.exists(archivo);
    }

    /**
     * Agrega una linea al registro. Si el disco falla, avisa y continúa:
     * la app sigue funcionando, solo deja de guardar.
     */
    public void registrar(String linea) {
        try {
            Files.writeString(archivo, linea + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("No se pudo guardar el registro del monedero: " + ex.getMessage());
        }
    }

    /**
     * Reproduce el registro en un Banco recien creado: lee las lineas, vacia el
     * archivo y repite cada operacion. Las operaciones repetidas vuelven a
     * escribirse solas, así el archivo queda igual al estado reconstruido.
     */
    public void repetir(Banco banco) {
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("No se pudo leer el registro del monedero: " + ex.getMessage());
            return;
        }
        try {
            Files.write(archivo, new byte[0]);
        } catch (IOException ex) {
            System.err.println("No se pudo vaciar el registro del monedero: " + ex.getMessage());
        }
        for (String linea : lineas) {
            String limpia = linea.strip();
            if (limpia.isEmpty()) continue;
            try {
                aplicar(banco, limpia);
            } catch (RuntimeException ex) {
                System.err.println("Linea ignorada del registro: " + limpia
                        + " (" + ex.getMessage() + ")");
            }
        }
    }

    /** Ejecuta una linea del registro contra el banco. */
    private void aplicar(Banco banco, String linea) {
        String[] c = linea.split(";", -1);
        switch (c[0]) {
            case "CREAR" -> {
                banco.registrarCliente(c[1], nombre(c, 2));
                banco.restaurarCredencial(c[1], c[c.length - 1]);
            }
            case "DEPOSITO" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[4]));
                banco.depositar(c[1], c[2], monto(c[3]));
            }
            case "RETIRO" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[4]));
                banco.retirar(c[1], c[2], monto(c[3]));
            }
            case "TRANSFERENCIA" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[6]));
                banco.transferir(c[1], c[2], c[3], c[4], monto(c[5]));
            }
            case "MOVIMIENTO" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[5]));
                banco.transferirEntreMonederos(c[1], c[2], c[3], monto(c[4]));
            }
            case "MONEDERO" -> banco.crearMonedero(c[1], c[2], nombre(c, 3),
                    TipoMonedero.valueOf(c[c.length - 1]));
            case "CANJE" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[3]));
                banco.canjearBeneficio(c[1], Beneficio.valueOf(c[2]));
            }
            case "PROGRAMAR" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[7]));
                programar(banco, c);
            }
            case "CANCELAR_PROGRAMADA" -> banco.cancelarProgramada(Integer.parseInt(c[1]));
            case "EJECUTADA" -> banco.marcarProgramadaEjecutada(Integer.parseInt(c[1]));
            case "FALLIDA" -> banco.restaurarFallaProgramada(Integer.parseInt(c[1]),
                    LocalDateTime.parse(c[2]), mensaje(c, 3));
            case "REPROGRAMAR" -> banco.reprogramarProgramada(Integer.parseInt(c[1]),
                    LocalDateTime.parse(c[2]));
            case "REVERSION" -> {
                banco.getReloj().establecer(LocalDateTime.parse(c[3]));
                banco.revertirTransaccion(c[1], Integer.parseInt(c[2]));
            }
            case "LEIDA" -> banco.marcarNotificacionLeida(Integer.parseInt(c[1]));
            case "UMBRAL" -> banco.configurarUmbralSaldoBajo(monto(c[1]));
            default -> throw new IllegalArgumentException("Tipo de linea desconocido: " + c[0]);
        }
    }

    private void programar(Banco banco, String[] c) {
        BigDecimal monto = monto(c[4]);
        LocalDateTime fecha = LocalDateTime.parse(c[5]);
        Recurrencia recurrencia = Recurrencia.valueOf(c[6]);
        switch (c[1]) {
            case "DEPOSITO" -> banco.programarDeposito(c[2], monto, fecha, recurrencia);
            case "RETIRO" -> banco.programarRetiro(c[2], monto, fecha, recurrencia);
            case "TRANSFERENCIA_ENVIADA" -> banco.programarTransferencia(c[2],
                    c[3].isBlank() ? null : c[3], monto, fecha, recurrencia);
            default -> throw new IllegalArgumentException("Programada no soportada: " + c[1]);
        }
    }

    /** Nombre en medio de la linea (puede contener ';'): el ultimo campo es otro dato. */
    private String nombre(String[] c, int desde) {
        return String.join(";", Arrays.copyOfRange(c, desde, c.length - 1));
    }

    /** Mensaje al final de la linea (puede contener ';'). */
    private String mensaje(String[] c, int desde) {
        return String.join(";", Arrays.copyOfRange(c, desde, c.length));
    }

    private BigDecimal monto(String texto) {
        return new BigDecimal(texto);
    }
}
