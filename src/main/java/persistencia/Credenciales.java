package persistencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Contraseñas del monedero: SHA-256 con sal aleatoria por usuario.
 * Nunca se guarda el texto de la contraseña; solo "sal:hash" en el registro.
 */
public final class Credenciales {

    private static final SecureRandom AZAR = new SecureRandom();

    private Credenciales() { }

    /** Devuelve "sal:hash" listo para almacenar en el registro. */
    public static String guardar(String password) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        return HexFormat.of().formatHex(sal) + ":" + hex(sal, password);
    }

    /** Verifica una contraseña contra el valor almacenado, en tiempo constante. */
    public static boolean verificar(String almacenado, String password) {
        int separador = almacenado == null ? -1 : almacenado.indexOf(':');
        if (separador <= 0) return false;
        byte[] sal = HexFormat.of().parseHex(almacenado.substring(0, separador));
        byte[] calculado = hex(sal, password).getBytes(StandardCharsets.UTF_8);
        byte[] guardado = almacenado.substring(separador + 1).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculado, guardado);
    }

    private static String hex(byte[] sal, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sal);
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", ex);
        }
    }
}
