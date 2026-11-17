package servicio;

import estructuras.ListaEnlazada;
import estructuras.TablaHash;
import java.math.BigDecimal;
import modelo.Cliente;
import modelo.CuentaPuntos;
import modelo.Monedero;
import modelo.MovimientoPuntos;
import modelo.ResultadoIntegridad;
import modelo.tipo.TipoMovimientoPuntos;
import modelo.tipo.TipoTransaccion;
import modelo.Transaccion;

/** Verificaciones recursivas sobre saldos, puntos y transacciones. */
public class ServicioIntegridad {

    public ResultadoIntegridad verificarCliente(Cliente cliente) {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        if (cliente == null) {
            resultado.agregarError("Cliente nulo.");
            return resultado;
        }

        TablaHash<String, BigDecimal> reconstruido = new TablaHash<>();
        for (Monedero monedero : cliente.listarMonederos()) {
            reconstruido.poner(monedero.getId(), BigDecimal.ZERO);
            if (monedero.getSaldo().signum() < 0) {
                resultado.agregarError("El monedero " + monedero.getId() + " tiene saldo negativo.");
            }
        }

        TablaHash<Integer, Boolean> ids = new TablaHash<>();
        for (Transaccion t : cliente.getHistorial()) {
            if (ids.contiene(t.getId())) {
                resultado.agregarError("Transaccion duplicada en el historial: #" + t.getId());
            } else {
                ids.poner(t.getId(), Boolean.TRUE);
            }
            if (t.getMonto() == null || t.getMonto().signum() <= 0) {
                resultado.agregarError("Transaccion #" + t.getId() + " con monto invalido.");
            }
            if (t.getComision() == null || t.getComision().signum() < 0) {
                resultado.agregarError("Transaccion #" + t.getId() + " con comision invalida.");
            }
            if ((t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENVIADA
                    || t.getTipo() == TipoTransaccion.TRANSFERENCIA_RECIBIDA)
                    && (t.getParId() <= 0 || t.getContraparteId() == null)) {
                resultado.agregarError("Transferencia #" + t.getId() + " sin pareja o contraparte.");
            }
            if (t.estaRevertida()) continue;

            String monederoId = t.getMonederoId() == null
                    ? Cliente.MONEDERO_PRINCIPAL : t.getMonederoId();
            if (!reconstruido.contiene(monederoId)) {
                resultado.agregarError("Transaccion #" + t.getId() + " usa un monedero inexistente.");
                continue;
            }
            switch (t.getTipo()) {
                case DEPOSITO, TRANSFERENCIA_RECIBIDA, BONO ->
                        sumar(reconstruido, monederoId, t.getMonto());
                case RETIRO, TRANSFERENCIA_ENVIADA ->
                        sumar(reconstruido, monederoId, t.getMonto().add(t.getComision()).negate());
                case TRANSFERENCIA_ENTRE_MONEDEROS -> {
                    if (t.getMonederoDestinoId() == null || !reconstruido.contiene(t.getMonederoDestinoId())) {
                        resultado.agregarError("Transferencia #" + t.getId() + " con monedero destino invalido.");
                        continue;
                    }
                    sumar(reconstruido, monederoId, t.getMonto().negate());
                    sumar(reconstruido, t.getMonederoDestinoId(), t.getMonto());
                }
            }
        }

        for (Monedero monedero : cliente.listarMonederos()) {
            BigDecimal esperado = reconstruido.obtener(monedero.getId());
            if (esperado == null || esperado.compareTo(monedero.getSaldo()) != 0) {
                resultado.agregarError("Saldo inconsistente en el monedero " + monedero.getId()
                        + ": esperado " + (esperado == null ? "0" : esperado.toPlainString())
                        + ", real " + monedero.getSaldo().toPlainString() + ".");
            }
        }
        return resultado;
    }

    public ResultadoIntegridad verificarCuentaPuntos(CuentaPuntos cuenta) {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        if (cuenta == null) {
            resultado.agregarError("Cuenta de puntos nula.");
            return resultado;
        }
        int ganados = 0;
        int canjeados = 0;
        int revertidos = 0;
        for (MovimientoPuntos m : cuenta.getHistorial()) {
            if (m.getPuntos() < 0) {
                resultado.agregarError("Movimiento de puntos negativo para " + cuenta.getClienteId() + ".");
            }
            switch (m.getTipo()) {
                case GANADOS -> ganados += m.getPuntos();
                case CANJE -> canjeados += m.getPuntos();
                case REVERSION -> revertidos += m.getPuntos();
            }
        }
        int acumuladosEsperados = ganados - revertidos;
        int disponiblesEsperados = ganados - canjeados - revertidos;
        if (acumuladosEsperados != cuenta.getAcumulados()) {
            resultado.agregarError("Puntos acumulados inconsistentes para " + cuenta.getClienteId()
                    + ": esperado " + acumuladosEsperados + ", real " + cuenta.getAcumulados() + ".");
        }
        if (disponiblesEsperados != cuenta.getDisponibles()) {
            resultado.agregarError("Puntos disponibles inconsistentes para " + cuenta.getClienteId()
                    + ": esperado " + disponiblesEsperados + ", real " + cuenta.getDisponibles() + ".");
        }
        return resultado;
    }

    public ResultadoIntegridad verificarTransacciones(ListaEnlazada<Transaccion> historial) {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        TablaHash<Integer, Boolean> ids = new TablaHash<>();
        for (Transaccion t : historial) {
            if (ids.contiene(t.getId())) {
                resultado.agregarError("Transaccion duplicada: #" + t.getId());
            } else {
                ids.poner(t.getId(), Boolean.TRUE);
            }
            if (t.getTipo() == null) resultado.agregarError("Transaccion sin tipo.");
        }
        return resultado;
    }

    /** Comprueba que el registro global y los historiales de los clientes coincidan. */
    public ResultadoIntegridad verificarGlobal(TablaHash<Integer, Transaccion> global,
                                                ListaEnlazada<Cliente> clientes) {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        TablaHash<String, Cliente> porId = new TablaHash<>();
        for (Cliente c : clientes) porId.poner(c.getId(), c);

        for (TablaHash.Entrada<Integer, Transaccion> e : global) {
            Transaccion t = e.getValor();
            Cliente cliente = porId.obtener(t.getClienteId());
            if (cliente == null) {
                resultado.agregarError("Transaccion #" + t.getId() + " de un cliente inexistente.");
                continue;
            }
            boolean encontrada = false;
            for (Transaccion h : cliente.getHistorial()) {
                if (h == t) {
                    encontrada = true;
                    break;
                }
            }
            if (!encontrada) {
                resultado.agregarError("Transaccion #" + t.getId() + " no aparece en el historial de "
                        + t.getClienteId() + ".");
            }
            if (t.getTipo() == TipoTransaccion.TRANSFERENCIA_ENVIADA
                    || t.getTipo() == TipoTransaccion.TRANSFERENCIA_RECIBIDA) {
                Transaccion pareja = global.obtener(t.getParId());
                if (pareja == null) {
                    resultado.agregarError("Transferencia #" + t.getId() + " sin transaccion pareja.");
                } else {
                    if (pareja.estaRevertida() != t.estaRevertida()) {
                        resultado.agregarError("Transferencia #" + t.getId()
                                + " y su pareja tienen estados de reversion distintos.");
                    }
                    if (t.getContraparteId() == null
                            || !t.getContraparteId().equals(pareja.getClienteId())) {
                        resultado.agregarError("Transferencia #" + t.getId() + " con contraparte inconsistente.");
                    }
                }
            }
        }
        for (Cliente cliente : clientes) {
            for (Transaccion t : cliente.getHistorial()) {
                if (global.obtener(t.getId()) != t) {
                    resultado.agregarError("Transaccion #" + t.getId() + " del historial de "
                            + cliente.getId() + " no esta en el registro global.");
                }
            }
        }
        return resultado;
    }

    private void sumar(TablaHash<String, BigDecimal> mapa, String clave, BigDecimal delta) {
        BigDecimal actual = mapa.obtener(clave);
        mapa.poner(clave, actual == null ? delta : actual.add(delta));
    }
}
