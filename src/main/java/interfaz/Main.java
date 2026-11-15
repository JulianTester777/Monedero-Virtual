package interfaz;

import estructuras.Arbol;
import modelo.*;
import servicio.*;
import java.math.BigDecimal;

/** Prueba de consola de las Fases 1 y 2. */
public class Main {

    public static void main(String[] args) {
        pruebaBST();
        pruebaPuntos();
    }

    private static void pruebaBST() {
        System.out.println("=== Prueba del arbol BST ===");
        Arbol<Integer> arbol = new Arbol<>();
        // Numeros 1..1000 en un orden pseudo-aleatorio (17 es coprimo con 1001)
        for (int i = 1; i <= 1000; i++) arbol.insertar((i * 17) % 1001);
        System.out.println("Tamano: " + arbol.tamano() + " | altura: " + arbol.altura()
                + " | min: " + arbol.minimo() + " | max: " + arbol.maximo());
        boolean ordenado = true;
        Integer previo = null;
        for (Integer x : arbol.inorden()) {
            if (previo != null && previo >= x) ordenado = false;
            previo = x;
        }
        System.out.println("Inorden ordenado: " + ordenado);
        System.out.println("Duplicado rechazado: " + !arbol.insertar(500));
        for (int i = 1; i <= 500; i++) arbol.eliminar(i);
        System.out.println("Tras eliminar 1..500 -> tamano: " + arbol.tamano() + " | min: " + arbol.minimo()
                + " | contiene 250: " + arbol.contiene(250) + " | contiene 750: " + arbol.contiene(750));
        var top3 = arbol.mayores(3);
        System.out.println("Top 3: " + top3.obtener(0) + ", " + top3.obtener(1) + ", " + top3.obtener(2));
    }

    private static void pruebaPuntos() {
        System.out.println("\n=== Prueba del sistema de puntos ===");
        Banco banco = new Banco();
        banco.registrarCliente("C1", "Ana");
        banco.registrarCliente("C2", "Luis");
        banco.registrarCliente("C3", "Sara");

        banco.depositar("C1", new BigDecimal("1000"));       // +10 pts
        banco.retirar("C1", new BigDecimal("200"));          // +4 pts, comision 2.00
        banco.transferir("C1", "C2", new BigDecimal("300")); // +9 pts, comision 6.00
        banco.depositar("C3", new BigDecimal("150"));        // 1 pt (division entera)

        CuentaPuntos ana = banco.consultarPuntos("C1");
        System.out.println("Saldo Ana : " + banco.consultarSaldo("C1") + " (esperado 492.00)");
        System.out.println("Saldo Luis: " + banco.consultarSaldo("C2") + " (esperado 300)");
        System.out.println("Puntos Ana: acum " + ana.getAcumulados() + " / disp " + ana.getDisponibles() + " (esperado 23/23)");
        System.out.println("Puntos Luis (recibio transferencia): " + banco.consultarPuntos("C2").getAcumulados() + " (esperado 0)");
        System.out.println("Puntos Sara (150 depositados): " + banco.consultarPuntos("C3").getAcumulados() + " (esperado 1)");

        System.out.println("\nHistorial de Ana:");
        for (Transaccion t : banco.consultarHistorial("C1")) System.out.println("  " + t);
        System.out.println("Historial de puntos de Ana:");
        for (MovimientoPuntos m : ana.getHistorial()) System.out.println("  " + m);

        System.out.println("\nRanking:");
        int pos = 1;
        for (EntradaPuntos e : banco.ranking(10)) System.out.println("  " + (pos++) + ". " + e);

        System.out.println("\n--- Canjes ---");
        intentarCanje(banco, "C1", Beneficio.DESCUENTO_TRANSFERENCIA); // error: solo 23 pts

        banco.depositar("C1", new BigDecimal("10000")); // +100 -> 123
        banco.canjearBeneficio("C1", Beneficio.DESCUENTO_TRANSFERENCIA);
        System.out.println("Tras canje: acum " + ana.getAcumulados() + " / disp " + ana.getDisponibles() + " (esperado 123/23)");
        Transaccion t = banco.transferir("C1", "C2", new BigDecimal("1000"));
        System.out.println("Comision con descuento sobre 1000: " + t.getComision() + " (esperado 18.00; sin descuento seria 20.00)");
        banco.depositar("C1", new BigDecimal("5000")); // +50 para tener 100 disponibles
        intentarCanje(banco, "C1", Beneficio.DESCUENTO_TRANSFERENCIA); // ya activo

        banco.depositar("C1", new BigDecimal("50000")); // +500
        intentarCanje(banco, "C1", Beneficio.SIN_CARGOS_RETIRO);
        Transaccion r1 = banco.retirar("C1", new BigDecimal("1000"));
        System.out.println("Retiro con mes gratis, comision: " + r1.getComision() + " (esperado 0.00)");
        banco.getReloj().avanzarDias(31);
        Transaccion r2 = banco.retirar("C1", new BigDecimal("1000"));
        System.out.println("Retiro 31 dias despues, comision: " + r2.getComision() + " (esperado 10.00)");

        banco.depositar("C1", new BigDecimal("100000")); // +1000
        BigDecimal antes = banco.consultarSaldo("C1");
        banco.canjearBeneficio("C1", Beneficio.BONO_SALDO);
        System.out.println("Bono de saldo: " + antes + " -> " + banco.consultarSaldo("C1") + " (esperado +50)");
        System.out.println("Puntos finales Ana: acum " + ana.getAcumulados() + " / disp " + ana.getDisponibles());

        System.out.println("\nRanking final:");
        pos = 1;
        for (EntradaPuntos e : banco.ranking(10)) System.out.println("  " + (pos++) + ". " + e);

        System.out.println("\n--- Errores esperados ---");
        try { banco.retirar("C2", new BigDecimal("99999")); }
        catch (OperacionInvalidaException e) { System.out.println("  OK -> " + e.getMessage()); }
    }

    private static void intentarCanje(Banco banco, String id, Beneficio b) {
        try {
            banco.canjearBeneficio(id, b);
            System.out.println("Canje OK: " + b.getDescripcion());
        } catch (OperacionInvalidaException e) {
            System.out.println("Canje rechazado -> " + e.getMessage());
        }
    }
}