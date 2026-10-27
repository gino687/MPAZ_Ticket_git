package com.fabiancg.mpazticket

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// LOGIN ----------------------------------------------------------------
@Composable
fun PantallaLogin(vm: TicketViewModel, onOk: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verCuentas by remember { mutableStateOf(false) }

    // El título entra deslizándose desde la izquierda (como el ejemplo de desplazamiento).
    var listo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { listo = true }
    val desplazamientoTitulo by animateIntOffsetAsState(
        targetValue = if (listo) IntOffset.Zero else IntOffset(-700, 0),
        animationSpec = tween(700),
        label = "Entrada del título"
    )

    // El botón cambia de color cuando los campos están completos (como el ejemplo de color).
    val completo = usuario.isNotBlank() && clave.isNotBlank()
    val colorBoton by animateColorAsState(
        targetValue = if (completo) MoradoMPAZ else Color.Gray,
        animationSpec = tween(600),
        label = "Color del botón ingresar"
    )

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize()
                .padding(bottom = pad.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.fillMaxWidth().background(MoradoMPAZ).statusBarsPadding().padding(24.dp)) {
                Text(
                    "MPAZ Ticket", color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.offset { desplazamientoTitulo }
                )
                Text("Solicitudes de mantención · Escuela Escritora Marcela Paz", color = Color.White)
            }
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Aparecer(1) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuario") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(clave, { clave = it }, label = { Text("Clave") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    }
                }
                AnimatedVisibility(
                    visible = error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
                Aparecer(2) {
                    Button(
                        onClick = { if (vm.login(usuario, clave)) onOk() else error = "Usuario o clave incorrectos" },
                        colors = ButtonDefaults.buttonColors(containerColor = colorBoton),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ingresar") }
                }
                Aparecer(3) {
                    Column {
                        OutlinedButton(onClick = { verCuentas = !verCuentas }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (verCuentas) "Ocultar cuentas de prueba" else "Ver cuentas de prueba")
                        }
                        Desplegable(verCuentas) {
                            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Cuentas ficticias (clave 1234)", style = MaterialTheme.typography.titleSmall)
                                    Text("ana, luis → Solicitante")
                                    Text("admin → Administrador (encargada de TI)")
                                    Text("pedro, tecnico → Responsable de la solución")
                                }
                            }
                        }
                    }
                }
                Aparecer(4) { Text(AVISO_EMERGENCIA, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

// LISTA / BANDEJA ----------------------------------------------------------------
@Composable
fun PantallaLista(vm: TicketViewModel, onCrear: () -> Unit, onAbrir: (Int) -> Unit, onSalir: () -> Unit) {
    val usuarioActual by vm.usuario.collectAsState()
    val todos by vm.tickets.collectAsState()
    val u = usuarioActual ?: return

    var fCat by remember { mutableStateOf<Categoria?>(null) }
    var fPri by remember { mutableStateOf<Prioridad?>(null) }
    var fEst by remember { mutableStateOf<Estado?>(null) }
    var fUbi by remember { mutableStateOf<String?>(null) }
    var fResp by remember { mutableStateOf<Usuario?>(null) }
    var fFecha by remember { mutableStateOf<String?>(null) }
    var verFiltros by remember { mutableStateOf(false) }

    val rangos = mapOf("Últimas 24 horas" to 1L, "Últimos 7 días" to 7L, "Últimos 30 días" to 30L)
    val ahora = System.currentTimeMillis()
    val lista = vm.visibles(todos)
        .filter { t ->
            (fCat == null || t.categoria == fCat) &&
                    (fPri == null || t.prioridad == fPri) &&
                    (fEst == null || t.estado == fEst) &&
                    fUbi.let { f -> f == null || t.ubicacion.startsWith(f) } &&
                    fResp.let { f -> f == null || t.asignadoA == f.id } &&
                    fFecha.let { f -> f == null || t.creado >= ahora - (rangos[f] ?: 0L) * 86_400_000L }
        }
        .sortedWith(
            compareBy<Ticket> { it.estado == Estado.CERRADO || it.estado == Estado.CANCELADO }
                .thenByDescending { it.prioridad.ordinal }
                .thenByDescending { it.creado }
        )

    Scaffold(
        topBar = { BarraMPAZ("MPAZ Ticket", "${u.nombre} · ${u.rol.etiqueta}", onSalir = onSalir) },
        floatingActionButton = {
            if (u.rol == Rol.SOLICITANTE) {
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn(tween(600, delayMillis = 400)) +
                            expandHorizontally(tween(600, delayMillis = 400), expandFrom = Alignment.End)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onCrear, containerColor = MoradoMPAZ, contentColor = Color.White
                    ) { Text("+ Nuevo ticket") }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Aparecer(0) {
                    Text(
                        when (u.rol) {
                            Rol.SOLICITANTE -> "Mis solicitudes"
                            Rol.ADMINISTRADOR -> "Bandeja central de tickets"
                            Rol.RESPONSABLE -> "Tickets asignados a mí"
                        }, style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            if (u.rol == Rol.ADMINISTRADOR) {
                item {
                    Aparecer(1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { verFiltros = !verFiltros }) {
                                Text(if (verFiltros) "Ocultar filtros" else "Filtrar historial")
                            }
                            Desplegable(verFiltros) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SelectorFiltro("Categoría", Categoria.entries, fCat, { it.etiqueta }) { fCat = it }
                                    SelectorFiltro("Prioridad", Prioridad.entries, fPri, { it.etiqueta }) { fPri = it }
                                    SelectorFiltro("Estado", Estado.entries, fEst, { it.etiqueta }) { fEst = it }
                                    SelectorFiltro("Ubicación", Catalogo.ubicaciones, fUbi, { it }) { fUbi = it }
                                    SelectorFiltro("Responsable", RepositorioTickets.usuarios.filter { it.rol == Rol.RESPONSABLE }, fResp, { it.nombre }) { fResp = it }
                                    SelectorFiltro("Fecha", rangos.keys.toList(), fFecha, { it }) { fFecha = it }
                                    TextButton(onClick = { fCat = null; fPri = null; fEst = null; fUbi = null; fResp = null; fFecha = null }) {
                                        Text("Limpiar filtros")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Text("${lista.size} ticket(s)", style = MaterialTheme.typography.labelMedium) }
            if (lista.isEmpty()) item { Aparecer(2) { Text("No hay tickets para mostrar.") } }
            itemsIndexed(lista, key = { _, t -> t.id }) { i, t ->
                Aparecer(i + 2, Modifier.animateItem()) {
                    // El fondo de la tarjeta cambia con animación según la prioridad.
                    val fondo by animateColorAsState(colorPrioridad(t.prioridad), tween(600), label = "Color de la tarjeta")
                    Card(
                        Modifier.fillMaxWidth().clickable { onAbrir(t.id) },
                        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = Color.Black)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.codigo, style = MaterialTheme.typography.labelLarge)
                                ChipEstado(t.estado)
                            }
                            Text(t.titulo, style = MaterialTheme.typography.titleMedium)
                            Text("${t.categoria.etiqueta} · ${t.ubicacion}", style = MaterialTheme.typography.bodySmall)
                            Text("Prioridad: ${t.prioridad.etiqueta} · ${fecha(t.creado)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}









// CREAR TICKET ----------------------------------------------------------------
@Composable
fun PantallaCrear(vm: TicketViewModel, onVolver: () -> Unit) {
    var cat by remember { mutableStateOf(Categoria.INFRAESTRUCTURA) }
    var sub by remember { mutableStateOf(Catalogo.subcategorias.getValue(Categoria.INFRAESTRUCTURA).first()) }
    var ubic by remember { mutableStateOf(Catalogo.ubicaciones.first()) }
    var detalleUbic by remember { mutableStateOf("") }
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val fotos = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    var codigoCreado by remember { mutableStateOf<String?>(null) }

    // El botón pasa de gris a morado cuando el formulario es válido.
    val valido = titulo.trim().length >= 5 && desc.trim().length >= 10
    val colorEnviar by animateColorAsState(
        targetValue = if (valido) MoradoMPAZ else Color.Gray,
        animationSpec = tween(600),
        label = "Color del botón enviar"
    )

    codigoCreado?.let {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = onVolver) { Text("Aceptar") } },
            title = { Text("Ticket enviado") },
            text = { Text("Su número de ticket es $it. Puede revisar su estado y comentarios en \"Mis solicitudes\".") }
        )
    }

    Scaffold(topBar = { BarraSoloVolver(onVolver) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Aparecer(0) { TituloPantalla("Nuevo ticket") }
            Aparecer(0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Selector("Categoría", Categoria.entries, cat, { it.etiqueta }) {
                        cat = it; sub = Catalogo.subcategorias.getValue(it).first()
                    }
                    Selector("Subcategoría", Catalogo.subcategorias.getValue(cat), sub, { it }) { sub = it }
                }
            }
            Aparecer(1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Selector("Ubicación", Catalogo.ubicaciones, ubic, { it }) { ubic = it }
                    OutlinedTextField(detalleUbic, { detalleUbic = it }, label = { Text("Detalle de ubicación (ej. 5.º A, 2.º piso)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            Aparecer(2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(desc, { desc = it }, label = { Text("Descripción del problema") },
                        minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
            Aparecer(3) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fotografías (opcional)", style = MaterialTheme.typography.titleSmall)
                    BotonesFoto { fotos.add(it) }
                    Miniaturas(fotos.toList())
                }
            }
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            Aparecer(4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            when {
                                titulo.trim().length < 5 -> error = "El título debe tener al menos 5 caracteres."
                                desc.trim().length < 10 -> error = "Describa el problema (mínimo 10 caracteres)."
                                else -> {
                                    error = null
                                    val lugar = if (detalleUbic.isBlank()) ubic else "$ubic - ${detalleUbic.trim()}"
                                    codigoCreado = vm.crear(titulo.trim(), desc.trim(), cat, sub, lugar, fotos.toList())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorEnviar),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enviar reporte") }
                    Text(AVISO_EMERGENCIA, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
