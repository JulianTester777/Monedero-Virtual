package servicio;

import estructuras.ColaPrioridad;
import estructuras.ListaEnlazada;
import estructuras.Pila;
import estructuras.TablaHash;
import modelo.*;
import modelo.tipo.*;
import servicio.dto.BeneficioDto;
import servicio.dto.FlujoDto;
import servicio.dto.IntegridadDto;
import servicio.dto.MonederoDto;
import servicio.dto.MovimientoDto;
import servicio.dto.MovimientoPuntosDto;
import servicio.dto.NotificacionDto;
import servicio.dto.ProgramadaDto;
import servicio.dto.RankingDto;
import servicio.dto.ReporteDto;
import servicio.dto.ResumenDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import persistencia.Credenciales;
import persistencia.RegistroOperaciones;

/**
 * Fachada con la logica de negocio. Los clientes se guardan en una tabla hash por id,
 * las transacciones reversibles en pilas, las programadas en una cola de prioridad
 * y las notificaciones en listas circulares.
 */
public class Banco {

    private static final BigDecimal BONO_SALDO = new BigDecimal("50");

    private final TablaHash<String, Cliente> clientes = new TablaHash<>();
    private final ServicioPuntos puntos = new ServicioPuntos();
    private final Reloj reloj = new Reloj();
    private final TablaHash<Integer, Transaccion> transacciones = new TablaHash<>();
    private final TablaHash<String, Pila<Transaccion>> reversibles = new TablaHash<>();
    private final ColaPrioridad<TransaccionProgramada> programadas = new ColaPrioridad<>();
    private final TablaHash<Integer, TransaccionProgramada> programadasPorId = new TablaHash<>();
    private final ServicioNotificaciones notificaciones = new ServicioNotificaciones();
    private final ServicioReportes reportes = new ServicioReportes();
    private final ServicioIntegridad integridad = new ServicioIntegridad();
    private final TablaHash<String, String> credenciales = new TablaHash<>();
    private final RegistroOperaciones registro;

    /** Banco sin persistencia: lo usan las pruebas. */
    public Banco() {
        this.registro = null;
    }

    /** Banco con persistencia: cada operacion de estado se guarda en el registro. */
    public Banco(RegistroOperaciones registro) {
        this.registro = registro;
    }

    /** Guarda una linea en el registro; no hace nada si el banco no tiene persistencia. */
    private void persistir(String linea) {
        if (registro != null) registro.registrar(linea);
    }

    public Cliente registrarCliente(String id, String nombre) {
        if (id == null || id.isBlank()) throw new OperacionInvalidaException("El id es obligatorio.");
        if (nombre == null || nombre.isBlank()) throw new OperacionInvalidaException("El nombre es obligatorio.");
        if (clientes.contiene(id)) throw new OperacionInvalidaException("Ya existe un cliente con id " + id);
        Cliente c = new Cliente(id, nombre);
        clientes.poner(id, c);
        puntos.registrarCliente(id);
        reversibles.poner(id, new Pila<>());
        return c;
    }

    /** Registra un cliente con contrasena (se guarda como hash con sal, nunca en texto plano). */
    public Cliente registrarCliente(String id, String nombre, String password) {
        if (password == null || password.isBlank())
            throw new OperacionInvalidaException("La contrasena es obligatoria.");
        Cliente c = registrarCliente(id, nombre);
        credenciales.poner(id, Credenciales.guardar(password));
        persistir("CREAR;" + id + ";" + nombre + ";" + credenciales.obtener(id));
        return c;
    }

    /** Verifica las credenciales de un cliente registrado con contrasena. */
    public boolean autenticar(String id, String password) {
        if (id == null || password == null || !clientes.contiene(id)) return false;
        String guardado = credenciales.obtener(id);
        return guardado != null && Credenciales.verificar(guardado, password);
    }

    /**
     * Repone la credencial de un cliente durante la reproduccion del registro
     * y vuelve a escribir su linea CREAR para que el archivo quede intacto.
     */
    public void restaurarCredencial(String id, String credencial) {
        if (!clientes.contiene(id))
            throw new OperacionInvalidaException("No existe el cliente " + id);
        credenciales.poner(id, credencial);
        persistir("CREAR;" + id + ";" + clientes.obtener(id).getNombre() + ";" + credencial);
    }

    public Cliente buscarCliente(String id) {
        Cliente c = clientes.obtener(id);
        if (c == null) throw new OperacionInvalidaException("No existe el cliente " + id);
        return c;
    }

    public ListaEnlazada<Cliente> listarClientes() {
        ListaEnlazada<Cliente> lista = new ListaEnlazada<>();
        for (TablaHash.Entrada<String, Cliente> e : clientes) lista.agregar(e.getValor());
        return lista;
    }

    public Monedero crearMonedero(String idCliente, String idMonedero, String nombre, TipoMonedero tipo) {
        Cliente c = buscarCliente(idCliente);
        try {
            Monedero monedero = c.crearMonedero(idMonedero, nombre, tipo);
            persistir("MONEDERO;" + idCliente + ";" + monedero.getId() + ";" + nombre + ";" + tipo.name());
            return monedero;
        } catch (IllegalArgumentException ex) {
            throw new OperacionInvalidaException(ex.getMessage());
        }
    }

    public ListaEnlazada<Monedero> listarMonederos(String idCliente) {
        return buscarCliente(idCliente).listarMonederos();
    }

    public BigDecimal consultarSaldo(String id) { return buscarCliente(id).getSaldo(); }

    public BigDecimal consultarSaldo(String id, String idMonedero) {
        return buscarMonedero(buscarCliente(id), idMonedero).getSaldo();
    }

    public BigDecimal consultarSaldoTotal(String id) { return buscarCliente(id).getSaldoTotal(); }

    public Transaccion depositar(String id, BigDecimal monto) {
        return depositar(id, Cliente.MONEDERO_PRINCIPAL, monto);
    }

    public Transaccion depositar(String id, String idMonedero, BigDecimal monto) {
        validarMonto(monto);
        Cliente c = buscarCliente(id);
        Monedero monedero = buscarMonedero(c, idMonedero);
        LocalDateTime ahora = reloj.ahora();
        monedero.acreditar(monto);
        Transaccion t = new Transaccion(TipoTransaccion.DEPOSITO, monto, BigDecimal.ZERO, ahora, id, null);
        t.setMonederoId(monedero.getId());
        c.getHistorial().agregar(t);
        puntos.otorgar(t);
        registrar(t, true);
        persistir("DEPOSITO;" + id + ";" + monedero.getId() + ";" + monto.toPlainString() + ";" + ahora);
        revisarSaldoBajo(id);
        return t;
    }

    public Transaccion retirar(String id, BigDecimal monto) {
        return retirar(id, Cliente.MONEDERO_PRINCIPAL, monto);
    }

    public Transaccion retirar(String id, String idMonedero, BigDecimal monto) {
        validarMonto(monto);
        Cliente c = buscarCliente(id);
        Monedero monedero = buscarMonedero(c, idMonedero);
        LocalDateTime ahora = reloj.ahora();
        BigDecimal comision = Comisiones.retiro(monto, puntos.consultar(id).tieneRetiroGratis(ahora));
        BigDecimal total = monto.add(comision);
        validarSaldo(monedero, total, comision);
        monedero.debitar(total);
        Transaccion t = new Transaccion(TipoTransaccion.RETIRO, monto, comision, ahora, id, null);
        t.setMonederoId(monedero.getId());
        c.getHistorial().agregar(t);
        puntos.otorgar(t);
        registrar(t, true);
        persistir("RETIRO;" + id + ";" + monedero.getId() + ";" + monto.toPlainString() + ";" + ahora);
        revisarSaldoBajo(id);
        return t;
    }

    /** Devuelve la transaccion registrada en el origen. La comision la paga quien envia. */
    public Transaccion transferir(String idOrigen, String idDestino, BigDecimal monto) {
        return transferir(idOrigen, Cliente.MONEDERO_PRINCIPAL,
                idDestino, Cliente.MONEDERO_PRINCIPAL, monto);
    }

    public Transaccion transferir(String idOrigen, String idMonederoOrigen,
                                  String idDestino, String idMonederoDestino, BigDecimal monto) {
        validarMonto(monto);
        if (idOrigen.equals(idDestino))
            throw new OperacionInvalidaException("No se puede transferir a la misma cuenta.");
        Cliente origen = buscarCliente(idOrigen);
        Cliente destino = buscarCliente(idDestino);
        Monedero monederoOrigen = buscarMonedero(origen, idMonederoOrigen);
        Monedero monederoDestino = buscarMonedero(destino, idMonederoDestino);
        LocalDateTime ahora = reloj.ahora();
        BigDecimal comision = Comisiones.transferencia(monto, puntos.consultar(idOrigen).tieneDescuentoTransferencia());
        BigDecimal total = monto.add(comision);
        validarSaldo(monederoOrigen, total, comision);
        monederoOrigen.debitar(total);
        monederoDestino.acreditar(monto);
        Transaccion enviada = new Transaccion(TipoTransaccion.TRANSFERENCIA_ENVIADA, monto, comision, ahora, idOrigen, idDestino);
        Transaccion recibida = new Transaccion(TipoTransaccion.TRANSFERENCIA_RECIBIDA, monto, BigDecimal.ZERO, ahora, idDestino, idOrigen);
        enviada.setMonederoId(monederoOrigen.getId());
        recibida.setMonederoId(monederoDestino.getId());
        enviada.setParId(recibida.getId());
        recibida.setParId(enviada.getId());
        origen.getHistorial().agregar(enviada);
        destino.getHistorial().agregar(recibida);
        puntos.otorgar(enviada); // solo quien envia gana puntos
        registrar(enviada, true);
        registrar(recibida, true);
        persistir("TRANSFERENCIA;" + idOrigen + ";" + monederoOrigen.getId() + ";" + idDestino + ";"
                + monederoDestino.getId() + ";" + monto.toPlainString() + ";" + ahora);
        revisarSaldoBajo(idOrigen);
        revisarSaldoBajo(idDestino);
        return enviada;
    }

    /** Mueve saldo entre dos monederos del mismo cliente sin otorgar puntos. */
    public Transaccion transferirEntreMonederos(String idCliente, String idOrigen,
                                                 String idDestino, BigDecimal monto) {
        validarMonto(monto);
        if (idOrigen.equals(idDestino))
            throw new OperacionInvalidaException("El monedero origen y destino deben ser distintos.");
        Cliente c = buscarCliente(idCliente);
        Monedero origen = buscarMonedero(c, idOrigen);
        Monedero destino = buscarMonedero(c, idDestino);
        validarSaldo(origen, monto, BigDecimal.ZERO);
        LocalDateTime ahora = reloj.ahora();
        origen.debitar(monto);
        destino.acreditar(monto);
        Transaccion t = new Transaccion(TipoTransaccion.TRANSFERENCIA_ENTRE_MONEDEROS, monto,
                BigDecimal.ZERO, ahora, idCliente, null);
        t.setMonederoId(origen.getId());
        t.setMonederoDestinoId(destino.getId());
        c.getHistorial().agregar(t);
        registrar(t, false);
        persistir("MOVIMIENTO;" + idCliente + ";" + origen.getId() + ";" + destino.getId() + ";"
                + monto.toPlainString() + ";" + ahora);
        reportes.registrarTransferenciaEntreMonederos(idCliente, origen.getId(), destino.getId());
        return t;
    }

    /** Canjea un beneficio con los puntos disponibles del cliente. */
    public MovimientoPuntos canjearBeneficio(String id, Beneficio beneficio) {
        Cliente c = buscarCliente(id);
        LocalDateTime ahora = reloj.ahora();
        MovimientoPuntos m = puntos.canjear(id, beneficio, ahora);
        if (beneficio == Beneficio.BONO_SALDO) {
            c.acreditar(BONO_SALDO);
            Transaccion t = new Transaccion(TipoTransaccion.BONO, BONO_SALDO, BigDecimal.ZERO, ahora, id, null);
            t.setMonederoId(Cliente.MONEDERO_PRINCIPAL);
            c.getHistorial().agregar(t);
            registrar(t, false);
            revisarSaldoBajo(id);
        }
        persistir("CANJE;" + id + ";" + beneficio.name() + ";" + ahora);
        return m;
    }

    /** Revierte la ultima transaccion reversible del cliente. */
    public Transaccion revertirUltimaTransaccion(String idCliente) {
        buscarCliente(idCliente);
        Pila<Transaccion> pila = reversibles.obtener(idCliente);
        if (pila == null || pila.estaVacia())
            throw new OperacionInvalidaException("No hay transacciones reversibles para " + idCliente);
        while (!pila.estaVacia() && pila.cima().estaRevertida()) pila.desapilar();
        if (pila.estaVacia())
            throw new OperacionInvalidaException("No hay transacciones reversibles para " + idCliente);
        return aplicarReversion(pila.cima());
    }

    /** Revierte una transaccion concreta del cliente. */
    public Transaccion revertirTransaccion(String idCliente, int idTransaccion) {
        buscarCliente(idCliente);
        Transaccion t = transacciones.obtener(idTransaccion);
        if (t == null)
            throw new OperacionInvalidaException("No existe la transaccion #" + idTransaccion);
        if (!idCliente.equals(t.getClienteId()))
            throw new OperacionInvalidaException("La transaccion no pertenece al cliente " + idCliente);
        return aplicarReversion(t);
    }

    public TransaccionProgramada programarDeposito(String id, BigDecimal monto,
                                                    LocalDateTime fecha, Recurrencia recurrencia) {
        return programar(id, TipoTransaccion.DEPOSITO, monto, null, fecha, recurrencia);
    }

    public TransaccionProgramada programarDeposito(String id, BigDecimal monto, LocalDateTime fecha) {
        return programarDeposito(id, monto, fecha, Recurrencia.NINGUNA);
    }

    public TransaccionProgramada programarRetiro(String id, BigDecimal monto,
                                                  LocalDateTime fecha, Recurrencia recurrencia) {
        return programar(id, TipoTransaccion.RETIRO, monto, null, fecha, recurrencia);
    }

    public TransaccionProgramada programarRetiro(String id, BigDecimal monto, LocalDateTime fecha) {
        return programarRetiro(id, monto, fecha, Recurrencia.NINGUNA);
    }

    public TransaccionProgramada programarTransferencia(String idOrigen, String idDestino,
                                                         BigDecimal monto, LocalDateTime fecha,
                                                         Recurrencia recurrencia) {
        return programar(idOrigen, TipoTransaccion.TRANSFERENCIA_ENVIADA, monto, idDestino, fecha, recurrencia);
    }

    public TransaccionProgramada programarTransferencia(String idOrigen, String idDestino,
                                                         BigDecimal monto, LocalDateTime fecha) {
        return programarTransferencia(idOrigen, idDestino, monto, fecha, Recurrencia.NINGUNA);
    }

    public ListaEnlazada<TransaccionProgramada> consultarProgramadas(String idCliente) {
        buscarCliente(idCliente);
        ListaEnlazada<TransaccionProgramada> lista = new ListaEnlazada<>();
        for (TablaHash.Entrada<Integer, TransaccionProgramada> e : programadasPorId) {
            if (idCliente.equals(e.getValor().getClienteId())) lista.agregar(e.getValor());
        }
        return lista;
    }

    public boolean cancelarProgramada(int idProgramada) {
        TransaccionProgramada p = programadasPorId.obtener(idProgramada);
        if (p == null || !p.esPendiente()) return false;
        p.cancelar();
        persistir("CANCELAR_PROGRAMADA;" + idProgramada);
        return true;
    }

    /** Marca una programada como ejecutada durante la reproduccion del registro. */
    public void marcarProgramadaEjecutada(int idProgramada) {
        TransaccionProgramada p = buscarProgramada(idProgramada);
        p.marcarEjecutada();
        persistir("EJECUTADA;" + idProgramada);
    }

    /**
     * Reproduce una programada fallida: deja el estado FALLIDA y vuelve a crear
     * el aviso original para que los ids de notificaciones queden alineados.
     */
    public void restaurarFallaProgramada(int idProgramada, LocalDateTime fechaAviso, String mensaje) {
        TransaccionProgramada p = buscarProgramada(idProgramada);
        p.marcarFallida();
        notificaciones.encolar(p.getClienteId(), TipoNotificacion.PROGRAMADA, mensaje, fechaAviso);
        persistir("FALLIDA;" + idProgramada + ";" + fechaAviso + ";" + mensaje);
    }

    /**
     * Reprograma una programada pendiente durante la reproduccion, sin volver a
     * insertarla en la cola (ya esta desde su linea PROGRAMAR).
     */
    public void reprogramarProgramada(int idProgramada, LocalDateTime nuevaFecha) {
        TransaccionProgramada p = buscarProgramada(idProgramada);
        p.reprogramar(nuevaFecha);
        reordenarProgramadas();
        persistir("REPROGRAMAR;" + idProgramada + ";" + nuevaFecha);
    }

    private TransaccionProgramada buscarProgramada(int idProgramada) {
        TransaccionProgramada p = programadasPorId.obtener(idProgramada);
        if (p == null)
            throw new OperacionInvalidaException("No existe la programada #" + idProgramada);
        return p;
    }

    /** Vacia y vuelve a insertar la cola: restaura el orden del monticulo tras cambiar una fecha. */
    private void reordenarProgramadas() {
        ListaEnlazada<TransaccionProgramada> todas = new ListaEnlazada<>();
        while (!programadas.estaVacia()) todas.agregar(programadas.extraerMinimo());
        for (TransaccionProgramada p : todas) programadas.insertar(p);
    }

    public ListaEnlazada<Transaccion> procesarProgramadas() {
        return procesarHasta(reloj.ahora());
    }

    public int procesarProgramadasVista() {
        return procesarProgramadas().tamano();
    }

    public ListaEnlazada<Transaccion> procesarHasta(LocalDateTime limite) {
        ListaEnlazada<Transaccion> ejecutadas = new ListaEnlazada<>();
        while (!programadas.estaVacia() && !programadas.verMinimo().getFecha().isAfter(limite)) {
            TransaccionProgramada p = programadas.extraerMinimo();
            if (!p.esPendiente()) continue;
            try {
                Transaccion t = ejecutarProgramada(p);
                ejecutadas.agregar(t);
                if (p.getRecurrencia() != Recurrencia.NINGUNA) {
                    p.reprogramar(p.getRecurrencia().siguiente(p.getFecha()));
                    programadas.insertar(p);
                    persistir("REPROGRAMAR;" + p.getId() + ";" + p.getFecha());
                } else {
                    p.marcarEjecutada();
                    persistir("EJECUTADA;" + p.getId());
                }
            } catch (RuntimeException ex) {
                p.marcarFallida();
                String mensaje = "No se pudo ejecutar la programada #" + p.getId() + ": " + ex.getMessage();
                LocalDateTime aviso = reloj.ahora();
                notificaciones.encolar(p.getClienteId(), TipoNotificacion.PROGRAMADA, mensaje, aviso);
                persistir("FALLIDA;" + p.getId() + ";" + aviso + ";" + mensaje);
            }
        }
        return ejecutadas;
    }

    public ListaEnlazada<Notificacion> consultarNotificaciones(String idCliente) {
        buscarCliente(idCliente);
        return notificaciones.listar(idCliente);
    }

    public Notificacion siguienteNotificacion(String idCliente) {
        buscarCliente(idCliente);
        return notificaciones.siguiente(idCliente);
    }

    public boolean marcarNotificacionLeida(int idNotificacion) {
        boolean leida = notificaciones.marcarLeida(idNotificacion);
        if (leida) persistir("LEIDA;" + idNotificacion);
        return leida;
    }

    public int notificacionesNoLeidas(String idCliente) {
        buscarCliente(idCliente);
        return notificaciones.contarNoLeidas(idCliente);
    }

    public BigDecimal consultarUmbralSaldoBajo() {
        return notificaciones.getUmbralSaldoBajo();
    }

    public void configurarUmbralSaldoBajo(BigDecimal umbral) {
        notificaciones.configurarUmbralSaldoBajo(umbral);
        persistir("UMBRAL;" + umbral.toPlainString());
    }

    public ListaEnlazada<String> generarReportePatrones(String idCliente) {
        buscarCliente(idCliente);
        return reportes.reportePatrones(idCliente);
    }

    public ListaEnlazada<String> categoriasMasUsadas(String idCliente) {
        buscarCliente(idCliente);
        return reportes.categoriasMasUsadas(idCliente);
    }

    public ListaEnlazada<String> clientesMasConectados() {
        return reportes.clientesMasConectados();
    }

    public boolean detectarCiclosEnReportes() {
        return reportes.detectarCiclos();
    }

    public ListaEnlazada<String> flujoEntreClientes() {
        return reportes.flujoEntreClientes();
    }

    public ListaEnlazada<String> caminoEntreClientes(String origen, String destino) {
        return reportes.caminoEntreClientes(origen, destino);
    }

    public ResultadoIntegridad verificarIntegridad() {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        ListaEnlazada<Cliente> lista = listarClientes();
        for (Cliente c : lista) {
            resultado.fusionar(integridad.verificarCliente(c));
            resultado.fusionar(integridad.verificarCuentaPuntos(puntos.consultar(c.getId())));
            resultado.fusionar(integridad.verificarTransacciones(c.getHistorial()));
        }
        resultado.fusionar(integridad.verificarGlobal(transacciones, lista));
        return resultado;
    }

    public ListaEnlazada<Transaccion> consultarHistorial(String id) { return buscarCliente(id).getHistorial(); }

    public CuentaPuntos consultarPuntos(String id) { buscarCliente(id); return puntos.consultar(id); }

    /** Historial de movimientos de puntos del cliente. */
    public ListaEnlazada<MovimientoPuntos> consultarHistorialPuntos(String id) {
        buscarCliente(id);
        return puntos.historial(id);
    }

    /** Movimiento de puntos generado por una transaccion, o null si no existe. */
    public MovimientoPuntos buscarMovimientoPuntos(String id, int transaccionId) {
        buscarCliente(id);
        return puntos.buscarMovimientoPorTransaccion(id, transaccionId);
    }

    /** Rango actual del cliente segun sus puntos acumulados. */
    public Rango consultarRango(String id) { buscarCliente(id); return puntos.consultarRango(id); }

    public ListaEnlazada<EntradaPuntos> ranking(int n) { return puntos.top(n); }

    public ListaEnlazada<EntradaPuntos> rankingCompleto() { return puntos.rankingCompleto(); }

    public Reloj getReloj() { return reloj; }

    public ResumenDto consultarResumenVista(String idCliente) {
        Cliente c = buscarCliente(idCliente);
        CuentaPuntos cuenta = puntos.consultar(idCliente);
        Rango actual = cuenta.getRango();
        Rango siguiente = siguienteRango(actual);
        int faltan = siguiente == null ? 0 : Math.max(0, actual.getHasta() + 1 - cuenta.getAcumulados());
        double progreso = siguiente == null ? 1 : progresoRango(cuenta.getAcumulados(), actual);
        return new ResumenDto(c.getNombre(), c.getSaldo(), c.getSaldoTotal(),
                cuenta.getDisponibles(), cuenta.getAcumulados(), actual.getNombre(),
                siguiente == null ? null : siguiente.getNombre(), faltan, progreso,
                notificaciones.contarNoLeidas(idCliente));
    }

    public ListaEnlazada<MovimientoDto> consultarMovimientosVista(String idCliente) {
        buscarCliente(idCliente);
        ListaEnlazada<MovimientoDto> salida = new ListaEnlazada<>();
        for (Transaccion t : buscarCliente(idCliente).getHistorial()) salida.agregar(aMovimientoVista(t));
        return salida;
    }

    public ListaEnlazada<MovimientoDto> consultarMovimientosRecientesVista(String idCliente, int cantidad) {
        ListaEnlazada<MovimientoDto> todos = consultarMovimientosVista(idCliente);
        ListaEnlazada<MovimientoDto> salida = new ListaEnlazada<>();
        int desde = Math.max(0, todos.tamano() - Math.max(0, cantidad));
        for (int i = desde; i < todos.tamano(); i++) salida.agregar(todos.obtener(i));
        return salida;
    }

    public ListaEnlazada<MovimientoDto> consultarReversiblesVista(String idCliente) {
        buscarCliente(idCliente);
        Pila<Transaccion> pila = reversibles.obtener(idCliente);
        ListaEnlazada<MovimientoDto> salida = new ListaEnlazada<>();
        if (pila == null) return salida;
        for (Transaccion t : pila.elementos()) {
            if (!t.estaRevertida()) salida.agregar(aMovimientoVista(t));
        }
        return salida;
    }

    public ListaEnlazada<MovimientoPuntosDto> consultarMovimientosPuntosVista(String idCliente) {
        buscarCliente(idCliente);
        ListaEnlazada<MovimientoPuntosDto> salida = new ListaEnlazada<>();
        for (MovimientoPuntos m : puntos.historial(idCliente)) {
            salida.agregar(new MovimientoPuntosDto(nombreMovimientoPuntos(m.getTipo()),
                    m.getTipo() == TipoMovimientoPuntos.GANADOS, m.getPuntos(), m.getTransaccionId(),
                    m.getDescripcion(), m.getFecha()));
        }
        return salida;
    }

    public ListaEnlazada<BeneficioDto> consultarBeneficiosVista(String idCliente) {
        CuentaPuntos cuenta = puntos.consultar(idCliente);
        ListaEnlazada<BeneficioDto> salida = new ListaEnlazada<>();
        for (Beneficio beneficio : Beneficio.values()) {
            boolean activo = beneficioActivo(cuenta, beneficio);
            int faltan = Math.max(0, beneficio.getCosto() - cuenta.getDisponibles());
            String texto = activo ? textoBeneficioActivo(cuenta, beneficio)
                    : faltan > 0 ? "Faltan " + faltan + " pts" : "Disponible";
            salida.agregar(new BeneficioDto(beneficio, nombreBeneficio(beneficio),
                    beneficio.getDescripcion(), beneficio.getCosto(), activo,
                    !activo && faltan == 0, faltan, texto));
        }
        return salida;
    }

    public ListaEnlazada<RankingDto> consultarRankingVista(String idClienteActual) {
        ListaEnlazada<RankingDto> salida = new ListaEnlazada<>();
        for (EntradaPuntos entrada : puntos.rankingCompleto()) {
            salida.agregar(new RankingDto(entrada.getClienteId(), nombreCliente(entrada.getClienteId()),
                    entrada.getPuntos(), entrada.getClienteId().equals(idClienteActual)));
        }
        return salida;
    }

    public ListaEnlazada<MonederoDto> consultarMonederosVista(String idCliente) {
        Cliente c = buscarCliente(idCliente);
        ListaEnlazada<MonederoDto> salida = new ListaEnlazada<>();
        for (Monedero monedero : c.listarMonederos()) {
            salida.agregar(new MonederoDto(monedero.getId(), monedero.getNombre(),
                    nombreTipoMonedero(monedero.getTipo()), monedero.getSaldo(),
                    monedero.getId().equals(Cliente.MONEDERO_PRINCIPAL)));
        }
        return salida;
    }

    public ListaEnlazada<ProgramadaDto> consultarProgramadasVista(String idCliente) {
        ListaEnlazada<ProgramadaDto> salida = new ListaEnlazada<>();
        for (TransaccionProgramada p : consultarProgramadas(idCliente)) {
            ProgramadaDto dto = new ProgramadaDto(p.getId(), nombreTipo(p.getTipo()), p.getMonto(),
                    p.getDestinoId() == null ? null : nombreCliente(p.getDestinoId()), p.getFecha(),
                    nombreRecurrencia(p.getRecurrencia()), nombreEstadoProgramada(p.getEstado()),
                    p.esPendiente());
            insertarProgramadaOrdenada(salida, dto);
        }
        return salida;
    }

    public ListaEnlazada<NotificacionDto> consultarNotificacionesVista(String idCliente) {
        buscarCliente(idCliente);
        ListaEnlazada<NotificacionDto> salida = new ListaEnlazada<>();
        for (Notificacion n : notificaciones.listar(idCliente)) salida.agregar(aNotificacionVista(n));
        return salida;
    }

    public NotificacionDto siguienteNotificacionVista(String idCliente) {
        Notificacion n = siguienteNotificacion(idCliente);
        return n == null ? null : aNotificacionVista(n);
    }

    public ReporteDto consultarReporteVista(String idCliente, String idDestino) {
        buscarCliente(idCliente);
        ListaEnlazada<String> categorias = new ListaEnlazada<>();
        ListaEnlazada<String> relacionados = new ListaEnlazada<>();
        for (String patron : reportes.reportePatrones(idCliente)) {
            if (patron.startsWith("categoria:")) {
                categorias.agregar(nombreCategoria(patron.substring("categoria:".length())));
            } else if (patron.startsWith("cliente:")) {
                relacionados.agregar(nombreCliente(patron.substring("cliente:".length())));
            }
        }

        ListaEnlazada<String> conectados = new ListaEnlazada<>();
        for (String id : reportes.clientesMasConectados()) conectados.agregar(nombreCliente(id));

        ListaEnlazada<FlujoDto> flujos = new ListaEnlazada<>();
        for (String texto : reportes.flujoEntreClientes()) flujos.agregar(aFlujoVista(texto));

        ListaEnlazada<String> camino = new ListaEnlazada<>();
        if (idDestino != null && !idDestino.isBlank()) {
            for (String id : reportes.caminoEntreClientes(idCliente, idDestino)) {
                camino.agregar(nombreCliente(id));
            }
        }

        return new ReporteDto(categorias, relacionados, conectados, flujos, camino,
                reportes.detectarCiclos());
    }

    public IntegridadDto verificarIntegridadVista() {
        ResultadoIntegridad resultado = verificarIntegridad();
        return new IntegridadDto(resultado.esValido(), resultado.getErrores());
    }

    public ListaEnlazada<String> tiposMovimientoVista() {
        ListaEnlazada<String> salida = new ListaEnlazada<>();
        for (TipoTransaccion tipo : TipoTransaccion.values()) salida.agregar(nombreTipo(tipo));
        return salida;
    }

    private MovimientoDto aMovimientoVista(Transaccion t) {
        MovimientoPuntos movimiento = movimientoPuntosDe(t);
        return new MovimientoDto(t.getId(), nombreTipo(t.getTipo()), esEntrada(t.getTipo()),
                t.getMonto(), t.getComision(), t.getFecha(),
                t.getContraparteId() == null ? null : nombreCliente(t.getContraparteId()),
                nombreMonedero(t.getClienteId(), t.getMonederoId()),
                t.getMonederoDestinoId() == null ? null
                        : nombreMonedero(t.getClienteId(), t.getMonederoDestinoId()),
                t.estaRevertida(),
                movimiento == null ? null : movimiento.getPuntos(),
                movimiento != null && movimiento.getTipo() == TipoMovimientoPuntos.GANADOS
                        && !t.estaRevertida());
    }

    private MovimientoPuntos movimientoPuntosDe(Transaccion t) {
        MovimientoPuntos ganados = null;
        for (MovimientoPuntos m : puntos.historial(t.getClienteId())) {
            if (m.getTransaccionId() != t.getId()) continue;
            if (m.getTipo() == TipoMovimientoPuntos.GANADOS) ganados = m;
            if (m.getTipo() == TipoMovimientoPuntos.REVERSION && t.estaRevertida()) return m;
        }
        return ganados;
    }

    private NotificacionDto aNotificacionVista(Notificacion n) {
        return new NotificacionDto(n.getId(), nombreTipoNotificacion(n.getTipo()),
                n.getMensaje(), n.getFecha(), n.estaLeida());
    }

    private FlujoDto aFlujoVista(String texto) {
        try {
            int separadorMonto = texto.lastIndexOf(" : ");
            String tramo = texto.substring(0, separadorMonto);
            BigDecimal monto = new BigDecimal(texto.substring(separadorMonto + 3));
            String[] extremos = tramo.split(" -> ");
            return new FlujoDto(nombreCliente(quitarPrefijo(extremos[0])),
                    nombreCliente(quitarPrefijo(extremos[1])), monto);
        } catch (RuntimeException ex) {
            return new FlujoDto(texto, "", BigDecimal.ZERO);
        }
    }

    private void insertarProgramadaOrdenada(ListaEnlazada<ProgramadaDto> lista, ProgramadaDto nueva) {
        ListaEnlazada<ProgramadaDto> ordenada = new ListaEnlazada<>();
        boolean insertada = false;
        for (ProgramadaDto actual : lista) {
            if (!insertada && nueva.fecha().isBefore(actual.fecha())) {
                ordenada.agregar(nueva);
                insertada = true;
            }
            ordenada.agregar(actual);
        }
        if (!insertada) ordenada.agregar(nueva);
        while (!lista.estaVacia()) lista.eliminar(lista.obtener(0));
        for (ProgramadaDto dto : ordenada) lista.agregar(dto);
    }

    private String nombreMonedero(String idCliente, String idMonedero) {
        Cliente c = clientes.obtener(idCliente);
        if (c == null || idMonedero == null) return null;
        Monedero monedero = c.buscarMonedero(idMonedero);
        return monedero == null ? idMonedero : monedero.getNombre();
    }

    private String nombreCliente(String id) {
        Cliente c = clientes.obtener(id);
        return c == null ? id : c.getNombre();
    }

    private String quitarPrefijo(String vertice) {
        int posicion = vertice.indexOf(':');
        return posicion < 0 ? vertice : vertice.substring(posicion + 1);
    }

    private String nombreCategoria(String tipo) {
        try {
            return nombreTipo(TipoTransaccion.valueOf(tipo));
        } catch (IllegalArgumentException ex) {
            return tipo;
        }
    }

    private boolean beneficioActivo(CuentaPuntos cuenta, Beneficio beneficio) {
        return switch (beneficio) {
            case DESCUENTO_TRANSFERENCIA -> cuenta.tieneDescuentoTransferencia();
            case SIN_CARGOS_RETIRO -> cuenta.tieneRetiroGratis(reloj.ahora());
            case BONO_SALDO -> false;
        };
    }

    private String textoBeneficioActivo(CuentaPuntos cuenta, Beneficio beneficio) {
        if (beneficio == Beneficio.SIN_CARGOS_RETIRO) {
            return "Activo hasta " + cuenta.getSinCargosRetiroHasta();
        }
        return "Activo";
    }

    private Rango siguienteRango(Rango actual) {
        Rango[] rangos = Rango.values();
        int siguiente = actual.ordinal() + 1;
        return siguiente < rangos.length ? rangos[siguiente] : null;
    }

    private double progresoRango(int acumulados, Rango actual) {
        int base = actual.getDesde();
        int meta = actual.getHasta() + 1;
        double avance = (acumulados - base) / (double) (meta - base);
        return Math.max(0, Math.min(1, avance));
    }

    private String nombreTipo(TipoTransaccion tipo) {
        return switch (tipo) {
            case DEPOSITO -> "Depósito";
            case RETIRO -> "Retiro";
            case TRANSFERENCIA_ENVIADA -> "Transferencia enviada";
            case TRANSFERENCIA_RECIBIDA -> "Transferencia recibida";
            case TRANSFERENCIA_ENTRE_MONEDEROS -> "Transferencia entre monederos";
            case BONO -> "Bono de saldo";
        };
    }

    private boolean esEntrada(TipoTransaccion tipo) {
        return tipo == TipoTransaccion.DEPOSITO
                || tipo == TipoTransaccion.TRANSFERENCIA_RECIBIDA
                || tipo == TipoTransaccion.BONO;
    }

    private String nombreMovimientoPuntos(TipoMovimientoPuntos tipo) {
        return switch (tipo) {
            case GANADOS -> "Puntos ganados";
            case CANJE -> "Canje";
            case REVERSION -> "Reversión";
        };
    }

    private String nombreBeneficio(Beneficio beneficio) {
        return switch (beneficio) {
            case DESCUENTO_TRANSFERENCIA -> "Descuento en transferencias";
            case SIN_CARGOS_RETIRO -> "Mes sin cargos por retiro";
            case BONO_SALDO -> "Bono de saldo";
        };
    }

    private String nombreTipoMonedero(TipoMonedero tipo) {
        return switch (tipo) {
            case AHORRO -> "Ahorro";
            case DIARIO -> "Diario";
            case OTRO -> "Otro";
        };
    }

    private String nombreRecurrencia(Recurrencia recurrencia) {
        return switch (recurrencia) {
            case NINGUNA -> "No se repite";
            case DIARIA -> "Diaria";
            case SEMANAL -> "Semanal";
            case MENSUAL -> "Mensual";
        };
    }

    private String nombreEstadoProgramada(EstadoProgramada estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EJECUTADA -> "Ejecutada";
            case FALLIDA -> "Fallida";
            case CANCELADA -> "Cancelada";
        };
    }

    private String nombreTipoNotificacion(TipoNotificacion tipo) {
        return switch (tipo) {
            case SALDO_BAJO -> "Saldo bajo";
            case PROGRAMADA -> "Programada";
            case RANGO -> "Rango";
            case BENEFICIO -> "Beneficio";
            case GENERAL -> "General";
        };
    }

    private Transaccion aplicarReversion(Transaccion t) {
        if (t.estaRevertida())
            throw new OperacionInvalidaException("La transaccion #" + t.getId() + " ya fue revertida.");
        Transaccion reversada = switch (t.getTipo()) {
            case BONO -> throw new OperacionInvalidaException("No se puede revertir un bono de saldo.");
            case DEPOSITO -> revertirDeposito(t);
            case RETIRO -> revertirRetiro(t);
            case TRANSFERENCIA_ENVIADA -> revertirTransferencia(t);
            case TRANSFERENCIA_RECIBIDA -> revertirTransferenciaRecibida(t);
            case TRANSFERENCIA_ENTRE_MONEDEROS -> revertirEntreMonederos(t);
        };
        persistir("REVERSION;" + reversada.getClienteId() + ";" + reversada.getId() + ";" + reloj.ahora());
        return reversada;
    }

    private Transaccion revertirDeposito(Transaccion t) {
        Cliente c = buscarCliente(t.getClienteId());
        Monedero monedero = buscarMonedero(c, t.getMonederoId());
        if (monedero.getSaldo().compareTo(t.getMonto()) < 0)
            throw new OperacionInvalidaException("No se puede revertir el deposito: el saldo ya fue utilizado.");
        validarReversionPuntos(t);
        monedero.debitar(t.getMonto());
        puntos.revertir(t, reloj.ahora());
        t.marcarRevertida();
        revisarSaldoBajo(t.getClienteId());
        return t;
    }

    private Transaccion revertirRetiro(Transaccion t) {
        Cliente c = buscarCliente(t.getClienteId());
        Monedero monedero = buscarMonedero(c, t.getMonederoId());
        validarReversionPuntos(t);
        monedero.acreditar(t.getMonto().add(t.getComision()));
        puntos.revertir(t, reloj.ahora());
        t.marcarRevertida();
        revisarSaldoBajo(t.getClienteId());
        return t;
    }

    private Transaccion revertirTransferenciaRecibida(Transaccion recibida) {
        Transaccion enviada = transacciones.obtener(recibida.getParId());
        if (enviada == null)
            throw new OperacionInvalidaException("No se encontro la transferencia de origen.");
        return revertirTransferencia(enviada);
    }

    private Transaccion revertirTransferencia(Transaccion enviada) {
        Transaccion recibida = transacciones.obtener(enviada.getParId());
        if (recibida == null)
            throw new OperacionInvalidaException("No se encontro la transferencia de destino.");
        if (enviada.estaRevertida() || recibida.estaRevertida())
            throw new OperacionInvalidaException("La transferencia ya fue revertida.");
        Cliente origen = buscarCliente(enviada.getClienteId());
        Cliente destino = buscarCliente(recibida.getClienteId());
        Monedero monederoOrigen = buscarMonedero(origen, enviada.getMonederoId());
        Monedero monederoDestino = buscarMonedero(destino, recibida.getMonederoId());
        if (monederoDestino.getSaldo().compareTo(enviada.getMonto()) < 0)
            throw new OperacionInvalidaException("No se puede revertir: el destinatario ya uso el dinero.");
        validarReversionPuntos(enviada);
        monederoDestino.debitar(enviada.getMonto());
        monederoOrigen.acreditar(enviada.getMonto().add(enviada.getComision()));
        puntos.revertir(enviada, reloj.ahora());
        enviada.marcarRevertida();
        recibida.marcarRevertida();
        revisarSaldoBajo(enviada.getClienteId());
        revisarSaldoBajo(recibida.getClienteId());
        return enviada;
    }

    private Transaccion revertirEntreMonederos(Transaccion t) {
        Cliente c = buscarCliente(t.getClienteId());
        Monedero origen = buscarMonedero(c, t.getMonederoId());
        Monedero destino = buscarMonedero(c, t.getMonederoDestinoId());
        if (destino.getSaldo().compareTo(t.getMonto()) < 0)
            throw new OperacionInvalidaException("No se puede revertir: el monedero destino ya uso el dinero.");
        destino.debitar(t.getMonto());
        origen.acreditar(t.getMonto());
        t.marcarRevertida();
        return t;
    }

    private void validarReversionPuntos(Transaccion t) {
        int generados = ServicioPuntos.calcularPuntos(t.getTipo(), t.getMonto());
        if (generados > 0 && puntos.consultar(t.getClienteId()).getDisponibles() < generados)
            throw new OperacionInvalidaException("No se puede revertir: el cliente ya gasto los puntos generados.");
    }

    private Transaccion ejecutarProgramada(TransaccionProgramada p) {
        return switch (p.getTipo()) {
            case DEPOSITO -> depositar(p.getClienteId(), p.getMonto());
            case RETIRO -> retirar(p.getClienteId(), p.getMonto());
            case TRANSFERENCIA_ENVIADA -> transferir(p.getClienteId(), p.getDestinoId(), p.getMonto());
            default -> throw new OperacionInvalidaException("Tipo de programada no soportado: " + p.getTipo());
        };
    }

    private TransaccionProgramada programar(String idCliente, TipoTransaccion tipo, BigDecimal monto,
                                            String destinoId, LocalDateTime fecha, Recurrencia recurrencia) {
        validarMonto(monto);
        if (fecha == null) throw new OperacionInvalidaException("La fecha de ejecucion es obligatoria.");
        buscarCliente(idCliente);
        if (tipo == TipoTransaccion.TRANSFERENCIA_ENVIADA) {
            if (destinoId == null || destinoId.isBlank())
                throw new OperacionInvalidaException("La transferencia programada necesita un destino.");
            if (destinoId.equals(idCliente))
                throw new OperacionInvalidaException("No se puede transferir a la misma cuenta.");
            buscarCliente(destinoId);
        }
        TransaccionProgramada p = new TransaccionProgramada(idCliente, tipo, monto, destinoId,
                fecha, recurrencia == null ? Recurrencia.NINGUNA : recurrencia);
        LocalDateTime ahora = reloj.ahora();
        programadas.insertar(p);
        programadasPorId.poner(p.getId(), p);
        notificaciones.encolar(idCliente, TipoNotificacion.PROGRAMADA,
                "Programada #" + p.getId() + " para " + fecha + ".", ahora);
        persistir("PROGRAMAR;" + tipo.name() + ";" + idCliente + ";" + (destinoId == null ? "" : destinoId)
                + ";" + monto.toPlainString() + ";" + fecha + ";" + p.getRecurrencia().name() + ";" + ahora);
        return p;
    }

    private void registrar(Transaccion t, boolean reversible) {
        transacciones.poner(t.getId(), t);
        if (reversible) {
            Pila<Transaccion> pila = reversibles.obtener(t.getClienteId());
            if (pila == null) {
                pila = new Pila<>();
                reversibles.poner(t.getClienteId(), pila);
            }
            pila.apilar(t);
        }
        reportes.registrarTransaccion(t);
    }

    private Monedero buscarMonedero(Cliente c, String idMonedero) {
        String id = idMonedero == null ? Cliente.MONEDERO_PRINCIPAL : idMonedero;
        Monedero monedero = c.buscarMonedero(id);
        if (monedero == null)
            throw new OperacionInvalidaException("No existe el monedero " + id + " para " + c.getId());
        return monedero;
    }

    private void revisarSaldoBajo(String idCliente) {
        BigDecimal saldo = consultarSaldo(idCliente);
        if (notificaciones.esSaldoBajo(saldo)) {
            notificaciones.encolar(idCliente, TipoNotificacion.SALDO_BAJO,
                    "Saldo bajo: " + saldo.toPlainString() + ".", reloj.ahora());
        }
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new OperacionInvalidaException("El monto debe ser mayor que cero.");
    }

    private void validarSaldo(Monedero monedero, BigDecimal total, BigDecimal comision) {
        if (monedero.getSaldo().compareTo(total) < 0)
            throw new OperacionInvalidaException("Saldo insuficiente. Se necesitan " + total.toPlainString()
                    + " (incluye comision de " + comision.toPlainString() + "). Saldo actual: "
                    + monedero.getSaldo().toPlainString());
    }
}
