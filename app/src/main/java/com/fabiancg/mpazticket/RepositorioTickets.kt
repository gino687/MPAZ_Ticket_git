package com.fabiancg.mpazticket

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Base de datos de prueba en memoria (MVP). Todos los datos son ficticios.
 * Para persistir, se puede reemplazar por Room o Firebase manteniendo estas mismas funciones.
 */
object RepositorioTickets {
    val usuarios = listOf(
        Usuario(1, "Ana Rojas", "Profesora", Rol.SOLICITANTE, "ana", "1234"),
        Usuario(2, "Luis Soto", "Secretario", Rol.SOLICITANTE, "luis", "1234"),
        Usuario(3, "Carla Muñoz", "Encargada de TI", Rol.ADMINISTRADOR, "admin", "1234"),
        Usuario(4, "Pedro Díaz", "Auxiliar de mantención", Rol.RESPONSABLE, "pedro", "1234"),
        Usuario(5, "Marta Vega", "Técnico de TI", Rol.RESPONSABLE, "tecnico", "1234")
    )

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets
    private var contador = 0

    private fun ahora() = System.currentTimeMillis()
    fun usuario(id: Int?): Usuario? = usuarios.firstOrNull { it.id == id }
    fun autenticar(u: String, c: String): Usuario? =
        usuarios.firstOrNull { it.usuario == u.trim().lowercase() && it.clave == c }

    fun crear(
        s: Usuario, titulo: String, desc: String, cat: Categoria,
        sub: String, ubic: String, fotos: List<String>
    ): Ticket {
        contador++
        val t = Ticket(
            id = contador, codigo = "MPAZ-%04d".format(contador), titulo = titulo, descripcion = desc,
            categoria = cat, subcategoria = sub, ubicacion = ubic, fotos = fotos,
            solicitanteId = s.id, creado = ahora(),
            historial = listOf(
                Evento(
                    ahora(), "Ticket creado",
                    "Categoría: ${cat.etiqueta} · $sub\nUbicación: $ubic\nFotografías adjuntas: ${fotos.size}",
                    "${s.nombre} · ${s.cargo}"
                )
            )
        )
        _tickets.update { it + t }
        return t
    }

    /** detalle, recibe el ticket ANTES del cambio, para poder explicar "de X a Y". */
    private fun modificar(
        id: Int, autor: Usuario, titulo: String?,
        detalle: (Ticket) -> String = { "" },
        cambio: (Ticket) -> Ticket
    ) {
        _tickets.update { lista ->
            lista.map { t ->
                if (t.id != id) t else {
                    val n = cambio(t)
                    if (titulo == null) n
                    else n.copy(historial = n.historial + Evento(ahora(), titulo, detalle(t), "${autor.nombre} · ${autor.cargo}"))
                }
            }
        }
    }

    fun priorizar(id: Int, admin: Usuario, cat: Categoria, prio: Prioridad) =
        modificar(id, admin, "Categoría y prioridad definidas",
            detalle = { t ->
                buildString {
                    append("Categoría: ${t.categoria.etiqueta} → ${cat.etiqueta}")
                    append("\nPrioridad: ${t.prioridad.etiqueta} → ${prio.etiqueta}")
                    if (t.estado == Estado.PENDIENTE) append("\nEstado: Pendiente → Revisado")
                }
            }
        ) {
            it.copy(
                categoria = cat,
                subcategoria = if (cat == it.categoria) it.subcategoria else Catalogo.subcategorias.getValue(cat).first(),
                prioridad = prio,
                estado = if (it.estado == Estado.PENDIENTE) Estado.REVISADO else it.estado
            )
        }

    fun asignar(id: Int, admin: Usuario, resp: Usuario) =
        modificar(id, admin, "Asignación de responsable",
            detalle = { t ->
                val anterior = usuario(t.asignadoA)
                buildString {
                    if (anterior == null) append("Responsable asignado: ${resp.nombre} (${resp.cargo})")
                    else append("Reasignado de ${anterior.nombre} a ${resp.nombre} (${resp.cargo})")
                    if (t.estado in listOf(Estado.PENDIENTE, Estado.REVISADO)) append("\nEstado: ${t.estado.etiqueta} → Asignado")
                }
            }
        ) {
            it.copy(
                asignadoA = resp.id, fechaAsignacion = ahora(),
                estado = if (it.estado in listOf(Estado.PENDIENTE, Estado.REVISADO, Estado.ASIGNADO)) Estado.ASIGNADO else it.estado
            )
        }

    fun cambiarEstado(id: Int, autor: Usuario, nuevo: Estado) =
        modificar(id, autor, "Cambio de estado",
            detalle = { t ->
                "De «${t.estado.etiqueta}» a «${nuevo.etiqueta}»" + when (nuevo) {
                    Estado.SOLUCIONADO -> "\nSe registró la fecha de solución."
                    Estado.CERRADO -> "\nSe confirmó la solución y se registró la fecha de cierre."
                    Estado.CANCELADO -> "\nEl ticket fue cancelado."
                    else -> ""
                }
            }
        ) {
            it.copy(
                estado = nuevo,
                fechaSolucion = if (nuevo == Estado.SOLUCIONADO) ahora() else it.fechaSolucion,
                fechaCierre = if (nuevo == Estado.CERRADO) ahora() else it.fechaCierre
            )
        }

    fun comentar(id: Int, autor: Usuario, texto: String) =
        modificar(id, autor, null) {
            it.copy(comentarios = it.comentarios + Comentario(autor.nombre, autor.rol, texto, ahora()))
        }

    fun registrarAvance(id: Int, autor: Usuario, acciones: String, materiales: String) =
        modificar(id, autor, "Acciones registradas",
            detalle = {
                "Acciones realizadas: ${acciones.ifBlank { "(sin descripción)" }}" +
                        "\nMateriales o información requerida: ${materiales.ifBlank { "ninguna" }}"
            }
        ) { it.copy(acciones = acciones, materiales = materiales) }

    fun agregarEvidencia(id: Int, autor: Usuario, ruta: String) =
        modificar(id, autor, "Evidencia adjuntada",
            detalle = { "Se adjuntó una fotografía de la reparación (total de evidencias: ${it.evidencia.size + 1})." }
        ) { it.copy(evidencia = it.evidencia + ruta) }

    init {
        val (ana, luis, admin, pedro) = usuarios
        crear(ana, "Proyector no enciende", "El proyector no enciende desde el lunes.",
            Categoria.TECNOLOGIA, "Proyector", "Sala de clases - 5.º A", emptyList())
        val t2 = crear(luis, "Sillas con patas sueltas", "Dos sillas inestables en la zona de lectura.",
            Categoria.MOBILIARIO, "Sillas", "Biblioteca - Zona de lectura", emptyList())
        priorizar(t2.id, admin, Categoria.MOBILIARIO, Prioridad.MEDIA)
        asignar(t2.id, admin, pedro)
        val t3 = crear(ana, "Filtración de agua", "Gotea agua desde el techo del baño.",
            Categoria.INFRAESTRUCTURA, "Plomería / baños", "Baños - Primer piso", emptyList())
        priorizar(t3.id, admin, Categoria.INFRAESTRUCTURA, Prioridad.ALTA)
        asignar(t3.id, admin, pedro)
        cambiarEstado(t3.id, pedro, Estado.EN_PROCESO)
    }
}
