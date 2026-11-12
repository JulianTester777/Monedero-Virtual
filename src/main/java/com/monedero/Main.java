package com.monedero;

import modelo.Transaccion;
import servicio.*;
import java.math.BigDecimal;


public class Main {
    public static void main(String[] args) {
        Banco banco = new Banco();
        banco.registrarCliente("C1", "Ana");
        banco.registrarCliente("C2", "Luis");

        banco.depositar("C1", new BigDecimal("1000"));
        banco.retirar("C1", new BigDecimal("200"));
        banco.transferir("C1", "C2", new BigDecimal("300"));

        System.out.println("Saldo Ana : " + banco.consultarSaldo("C1"));
        System.out.println("Saldo Luis: " + banco.consultarSaldo("C2"));

        System.out.println("\nHistorial de Ana:");
        for (Transaccion t : banco.consultarHistorial("C1")) System.out.println("  " + t);
        System.out.println("Historial de Luis:");
        for (Transaccion t : banco.consultarHistorial("C2")) System.out.println("  " + t);

        System.out.println("\nPruebas de error:");
        try { banco.retirar("C2", new BigDecimal("999")); }
        catch (OperacionInvalidaException e) { System.out.println("  OK -> " + e.getMessage()); }
        try { banco.depositar("C1", new BigDecimal("-5")); }
        catch (OperacionInvalidaException e) { System.out.println("  OK -> " + e.getMessage()); }
        try { banco.transferir("C1", "C9", new BigDecimal("10")); }
        catch (OperacionInvalidaException e) { System.out.println("  OK -> " + e.getMessage()); }

        // Prueba de estres de la tabla hash (fuerza redimensionamiento)
        for (int i = 0; i < 1000; i++) banco.registrarCliente("X" + i, "Cliente" + i);
        System.out.println("\nClientes totales tras estres: " + banco.listarClientes().tamano()
                + " | Busqueda X777: " + banco.buscarCliente("X777").getNombre());
    }
}