package com.fabiancg.mpazticket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -----------------Colores y formatos-------------------------------------------
val MoradoMPAZ = Color(0xFF6A1B9A)
val GrisSalir = Color(0xFF757575)

/** Único formato de fecha y hora */
fun fecha(ms: Long?): String =
    if (ms == null) "—" else SimpleDateFormat("dd-MM-yyyy [HH:mm]", Locale("es", "CL")).format(Date(ms))

const val AVISO_EMERGENCIA =
    "Esta aplicación no reemplaza los procedimientos de emergencia. Ante un riesgo inmediato, " +
            "avise por los canales que defina la escuela."

fun colorEstado(e: Estado) = when (e) {
    Estado.PENDIENTE -> Color(0xFF757575)
    Estado.REVISADO -> Color(0xFF3949AB)
    Estado.ASIGNADO -> Color(0xFF8E24AA)
    Estado.EN_PROCESO -> Color(0xFFEF6C00)
    Estado.SOLUCIONADO -> Color(0xFF2E7D32)
    Estado.CERRADO -> Color(0xFF455A64)
    Estado.CANCELADO -> Color(0xFFC62828)
}

fun colorPrioridad(p: Prioridad) = when (p) {
    Prioridad.SIN_DEFINIR -> Color(0xFFEEEEEE)
    Prioridad.BAJA -> Color(0xFFE8F5E9)
    Prioridad.MEDIA -> Color(0xFFFFF8E1)
    Prioridad.ALTA -> Color(0xFFFFE0B2)
    Prioridad.URGENTE -> Color(0xFFFFCDD2)
}

// ----------------- Barra superior -------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraMPAZ(
    titulo: String,
    subtitulo: String? = null,
    onVolver: (() -> Unit)? = null,
    onSalir: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(titulo, fontWeight = FontWeight.Bold)
                if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.labelSmall)
            }
        },
        navigationIcon = {
            if (onVolver != null) {
                TextButton(onClick = onVolver, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Volver")
                }
            }
        },
        actions = {
            if (onSalir != null) {
                Button(
                    onClick = onSalir,
                    colors = ButtonDefaults.buttonColors(containerColor = GrisSalir, contentColor = Color.White),
                    modifier = Modifier.padding(end = 8.dp)
                ) { Text("Salir") }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MoradoMPAZ,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

/** flecha de retroceso (pantallas Detalle y Nuevo ticket). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSoloVolver(onVolver: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MoradoMPAZ,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

/** Título principal debajo del header */
@Composable
fun TituloPantalla(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto, modifier = modifier,
        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
    )
}

// ---------------------Animaciones---------------------------------------
/** Aparece al componerse (fundido + expansión vertical) **/
@Composable
fun Aparecer(indice: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val retraso = minOf(indice, 8) * 70
    val estado = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = estado,
        modifier = modifier,
        enter = fadeIn(tween(500, delayMillis = retraso)) +
                expandVertically(tween(500, delayMillis = retraso), expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically()
    ) { content() }
}

/** Sección que de "Ver más y Ver menos" */
@Composable
fun Desplegable(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(600), expandFrom = Alignment.Top) + fadeIn(tween(600)),
        exit = shrinkVertically(tween(600), shrinkTowards = Alignment.Top) + fadeOut(tween(300))
    ) { content() }
}

@Composable
fun ChipEstado(e: Estado) {
    val color by animateColorAsState(colorEstado(e), tween(600), label = "Color del estado")
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            e.etiqueta, color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Línea de avance: el marcador se desplaza (animateIntOffsetAsState) según el estado. */
@Composable
fun IndicadorAvance(estado: Estado) {
    val pasos = listOf(
        Estado.PENDIENTE, Estado.REVISADO, Estado.ASIGNADO,
        Estado.EN_PROCESO, Estado.SOLUCIONADO, Estado.CERRADO
    )
    if (estado == Estado.CANCELADO) {
        Text("Solicitud cancelada", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
        return
    }
    var listo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { listo = true }
    val indice = if (listo) pasos.indexOf(estado).coerceAtLeast(0) else 0
    val color by animateColorAsState(colorEstado(estado), tween(600), label = "Color del avance")

    BoxWithConstraints(Modifier.fillMaxWidth().height(24.dp)) {
        val marcador = 20.dp
        val recorrido = with(LocalDensity.current) { (maxWidth - marcador).roundToPx() }
        val desplazamiento by animateIntOffsetAsState(
            targetValue = IntOffset(recorrido * indice / (pasos.size - 1), 0),
            animationSpec = tween(800),
            label = "Avance del ticket"
        )
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp)
                .background(Color.LightGray, RoundedCornerShape(2.dp))
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset { desplazamiento }
                .size(marcador).background(color, CircleShape)
        )
    }
}

// --------------------- Selectores ---------------------------------------
@Composable
fun <T> SelectorBase(
    etiqueta: String, opciones: List<T>, textoActual: String,
    texto: (T) -> String, conEtiqueta: Boolean = true, onSelect: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (conEtiqueta) "$etiqueta: $textoActual" else textoActual)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(text = { Text(texto(op)) }, onClick = { onSelect(op); abierto = false })
            }
        }
    }
}

@Composable
fun <T> Selector(
    etiqueta: String, opciones: List<T>, seleccionado: T, texto: (T) -> String,
    conEtiqueta: Boolean = true, onSelect: (T) -> Unit
) = SelectorBase(etiqueta, opciones, texto(seleccionado), texto, conEtiqueta, onSelect)

/** Selector para filtros: incluye la opción "Todos" (null). */
@Composable
fun <T> SelectorFiltro(etiqueta: String, opciones: List<T>, seleccionado: T?, texto: (T) -> String, onSelect: (T?) -> Unit) =
    SelectorBase<T?>(
        etiqueta, listOf<T?>(null) + opciones,
        if (seleccionado == null) "Todos" else texto(seleccionado),
        { x -> if (x == null) "Todos" else texto(x) }, onSelect = onSelect
    )

// -------------------- Fotografías ----------------------------------------



/** TIBIO - esto es para las miniaturas de como se vera en la app**/
@Composable
fun Miniaturas(rutas: List<String>) {
    if (rutas.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rutas.forEach { ruta ->
            val img = remember(ruta) {
                BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
            }
            val visible = remember(ruta) { MutableTransitionState(false).apply { targetState = true } }
            if (img != null) {
                AnimatedVisibility(
                    visibleState = visible,
                    enter = expandHorizontally(tween(500)) + fadeIn(tween(500)),
                    exit = shrinkHorizontally(tween(300)) + fadeOut(tween(300))
                ) {
                    Image(
                        bitmap = img, contentDescription = "Fotografía adjunta",
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private fun guardarBitmap(ctx: Context, bmp: Bitmap): String {
    val f = File(ctx.filesDir, "foto_${System.currentTimeMillis()}.jpg")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    return f.absolutePath
}

private fun guardarUri(ctx: Context, uri: Uri): String? = try {
    val f = File(ctx.filesDir, "foto_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    f.absolutePath
} catch (e: Exception) { null }





/** ESTA ES LA PARTE PARA PODER USAR LA CAMARA **/
/** cámara o galería. No requiere permisos ni GPS. **/

/** ES EN LA CAMARA**/
@Composable
fun BotonesFoto(onFoto: (String) -> Unit) {
    val ctx = LocalContext.current
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { onFoto(guardarBitmap(ctx, it)) }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { guardarUri(ctx, it)?.let(onFoto) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { camara.launch(null) }) { Text("Tomar foto") }
        OutlinedButton(onClick = {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("Galería") }
    }
    Text("Evite fotografiar rostros, documentos o datos personales.", style = MaterialTheme.typography.bodySmall)
}
