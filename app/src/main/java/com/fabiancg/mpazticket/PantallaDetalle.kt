package com.fabiancg.mpazticket

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PantallaDetalle(vm: TicketViewModel, id: Int, onVolver: () -> Unit) {
    val usuarioActual by vm.usuario.collectAsState()
    val todos by vm.tickets.collectAsState()
    val u = usuarioActual ?: return
    val t = todos.firstOrNull { it.id == id }

    Scaffold(topBar = { BarraSoloVolver(onVolver) }) { pad ->
        if (t == null) {
            Text("Ticket no encontrado.", Modifier.padding(pad).padding(16.dp)); return@Scaffold
        }
        val solicitante = RepositorioTickets.usuario(t.solicitanteId)
        val asignado = RepositorioTickets.usuario(t.asignadoA)
        val abierto = t.estado != Estado.CERRADO && t.estado != Estado.CANCELADO
        // El fondo de la cabecera cambia con animación cuando el administrador cambia la prioridad.
        val fondoCabecera by animateColorAsState(colorPrioridad(t.prioridad), tween(600), label = "Color de la cabecera")

        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Aparecer(0) { TituloPantalla(t.codigo, Modifier.padding(top = 16.dp)) } }
            item {
                Aparecer(0) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = fondoCabecera, contentColor = Color.Black)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChipEstado(t.estado)
                            Text(t.titulo, style = MaterialTheme.typography.titleLarge)
                            IndicadorAvance(t.estado)
                            Text("Categoría: ${t.categoria.etiqueta} · ${t.subcategoria}")
                            Text("Ubicación: ${t.ubicacion}")
                            Text("Prioridad: ${t.prioridad.etiqueta}")
                            Text("Solicitante: ${solicitante?.nombre} (${solicitante?.cargo})")
                            Text("Creado: ${fecha(t.creado)}")
                            Text("Responsable: ${asignado?.nombre ?: "Sin asignar"}")
                            Text("Asignado: ${fecha(t.fechaAsignacion)}")
                            Text("Solucionado: ${fecha(t.fechaSolucion)}")
                            Text("Cerrado: ${fecha(t.fechaCierre)}")
                        }
                    }
                }
            }
            item {
                Aparecer(1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Descripción", style = MaterialTheme.typography.titleMedium)
                        Text(t.descripcion)
                        Miniaturas(t.fotos)
                    }
                }
            }
            if (t.acciones.isNotBlank() || t.materiales.isNotBlank() || t.evidencia.isNotEmpty()) {
                item {
                    Aparecer(2) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Acciones realizadas", style = MaterialTheme.typography.titleMedium)
                            if (t.acciones.isNotBlank()) Text(t.acciones)
                            if (t.materiales.isNotBlank()) Text("Materiales / información requerida: ${t.materiales}")
                            Miniaturas(t.evidencia)
                        }
                    }
                }
            }
            if (u.rol == Rol.ADMINISTRADOR && abierto) item { Aparecer(3) { PanelAdmin(vm, t) } }
            if (u.rol == Rol.RESPONSABLE && t.asignadoA == u.id && abierto) item { Aparecer(3) { PanelResponsable(vm, t) } }
            if (u.rol == Rol.SOLICITANTE && t.estado == Estado.PENDIENTE) {
                item {
                    Aparecer(3) {
                        OutlinedButton(onClick = { vm.cambiarEstado(t.id, Estado.CANCELADO) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar mi solicitud")
                        }
                    }
                }
            }
            item { Aparecer(4) { Comentarios(vm, t, puedeComentar = abierto) } }
            item { Aparecer(5) { Historial(t) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}


@Composable
private fun Historial(t: Ticket) {
    var visible by remember { mutableStateOf(true) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Historial de cambios", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ver menos" else "Ver más") }
        }
        Desplegable(visible) {
            Column {
                t.historial.reversed().forEachIndexed { i, e ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(color = Color(0xFFEDE7F6), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                fecha(e.fecha), color = MoradoMPAZ, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(e.titulo, fontWeight = FontWeight.SemiBold)
                        if (e.detalle.isNotBlank()) Text(e.detalle)
                        Text("Por: ${e.autor}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelAdmin(vm: TicketViewModel, t: Ticket) {
    var cat by remember(t.id, t.categoria) { mutableStateOf(t.categoria) }
    var pri by remember(t.id, t.prioridad) { mutableStateOf(t.prioridad) }
    val responsables = RepositorioTickets.usuarios.filter { it.rol == Rol.RESPONSABLE }
    var resp by remember(t.id, t.asignadoA) { mutableStateOf(RepositorioTickets.usuario(t.asignadoA) ?: responsables.first()) }
    var est by remember(t.id, t.estado) { mutableStateOf(t.estado) }
    var visible by remember { mutableStateOf(true) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gestión del ticket (administrador)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ver menos" else "Ver más") }
            }
            Desplegable(visible) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Selector("Categoría", Categoria.entries, cat, { it.etiqueta }) { cat = it }
                    Selector("Prioridad", Prioridad.entries.filter { it != Prioridad.SIN_DEFINIR }, pri, { it.etiqueta }) { pri = it }
                    Button(onClick = { vm.priorizar(t.id, cat, pri) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirmar categoría y prioridad")
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Asignar a", style = MaterialTheme.typography.titleSmall)
                        Selector("Asignar a", responsables, resp, { "${it.nombre} · ${it.cargo}" }, conEtiqueta = false) { resp = it }
                    }
                    Button(onClick = { vm.asignar(t.id, resp) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (t.asignadoA == null) "Asignar ticket" else "Reasignar ticket")
                    }
                    HorizontalDivider()
                    Selector("Estado", Estado.entries, est, { it.etiqueta }) { est = it }
                    OutlinedButton(onClick = { vm.cambiarEstado(t.id, est) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Aplicar estado")
                    }
                    if (t.estado == Estado.SOLUCIONADO) {
                        Button(onClick = { vm.cambiarEstado(t.id, Estado.CERRADO) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Confirmar solución y cerrar ticket")
                        }
                    }
                    TextButton(onClick = { vm.cambiarEstado(t.id, Estado.CANCELADO) }) { Text("Cancelar ticket") }
                }
            }
        }
    }
}

@Composable
private fun PanelResponsable(vm: TicketViewModel, t: Ticket) {
    var acciones by remember(t.id) { mutableStateOf(t.acciones) }
    var materiales by remember(t.id) { mutableStateOf(t.materiales) }
    var visible by remember { mutableStateOf(true) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Trabajo del responsable", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ver menos" else "Ver más") }
            }
            Desplegable(visible) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (t.estado == Estado.ASIGNADO) {
                        Button(onClick = { vm.cambiarEstado(t.id, Estado.EN_PROCESO) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Comenzar trabajo (En proceso)")
                        }
                    }
                    OutlinedTextField(acciones, { acciones = it }, label = { Text("Acciones realizadas") },
                        minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(materiales, { materiales = it }, label = { Text("Materiales o información adicional que necesito") },
                        minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { vm.registrarAvance(t.id, acciones.trim(), materiales.trim()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar avance")
                    }
                    Text("Evidencia de la reparación (opcional)", style = MaterialTheme.typography.titleSmall)
                    BotonesFoto { vm.agregarEvidencia(t.id, it) }
                    if (t.estado == Estado.EN_PROCESO) {
                        Button(
                            onClick = {
                                vm.registrarAvance(t.id, acciones.trim(), materiales.trim())
                                vm.cambiarEstado(t.id, Estado.SOLUCIONADO)
                            },
                            enabled = acciones.isNotBlank(), modifier = Modifier.fillMaxWidth()
                        ) { Text("Marcar como solucionado") }
                        if (acciones.isBlank()) Text("Describa las acciones realizadas para poder marcarlo como solucionado.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Comentarios(vm: TicketViewModel, t: Ticket, puedeComentar: Boolean) {
    var texto by remember(t.id) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Comentarios", style = MaterialTheme.typography.titleMedium)
        if (t.comentarios.isEmpty()) Text("Sin comentarios.")
        t.comentarios.forEach { c ->
            // Cada comentario nuevo aparece con animación.
            Aparecer {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${c.autor} (${c.rol.etiqueta}) · ${fecha(c.fecha)}", style = MaterialTheme.typography.labelMedium)
                        Text(c.texto)
                    }
                }
            }
        }
        if (puedeComentar) {
            OutlinedTextField(texto, { texto = it }, label = { Text("Escribir comentario") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.comentar(t.id, texto.trim()); texto = "" }, enabled = texto.isNotBlank()) {
                Text("Enviar comentario")
            }
        }
    }
}
