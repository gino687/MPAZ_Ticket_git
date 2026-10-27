package com.fabiancg.mpazticket

enum class Rol(val etiqueta: String) {
    SOLICITANTE("Solicitante"), ADMINISTRADOR("Administrador (TI)"), RESPONSABLE("Responsable")
}

enum class Categoria(val etiqueta: String) {
    INFRAESTRUCTURA("Infraestructura"), TECNOLOGIA("Tecnología"), MOBILIARIO("Mobiliario"), OTRA("Otra")
}

enum class Prioridad(val etiqueta: String) {
    SIN_DEFINIR("Sin definir"), BAJA("Baja"), MEDIA("Media"), ALTA("Alta"), URGENTE("Urgente")
}

enum class Estado(val etiqueta: String) {
    PENDIENTE("Pendiente"), REVISADO("Revisado"), ASIGNADO("Asignado"), EN_PROCESO("En proceso"),
    SOLUCIONADO("Solucionado"), CERRADO("Cerrado"), CANCELADO("Cancelado")
}

data class Usuario(
    val id: Int, val nombre: String, val cargo: String,
    val rol: Rol, val usuario: String, val clave: String
)

data class Comentario(val autor: String, val rol: Rol, val texto: String, val fecha: Long)
data class Evento(val fecha: Long, val titulo: String, val detalle: String, val autor: String)

data class Ticket(
    val id: Int,
    val codigo: String,
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val subcategoria: String,
    val ubicacion: String,
    val fotos: List<String>,
    val solicitanteId: Int,
    val creado: Long,
    val prioridad: Prioridad = Prioridad.SIN_DEFINIR,
    val estado: Estado = Estado.PENDIENTE,
    val asignadoA: Int? = null,
    val fechaAsignacion: Long? = null,
    val comentarios: List<Comentario> = emptyList(),
    val acciones: String = "",
    val materiales: String = "",
    val evidencia: List<String> = emptyList(),
    val fechaSolucion: Long? = null,
    val fechaCierre: Long? = null,
    val historial: List<Evento> = emptyList()
)

/** Catálogo de categorías, subcategorías y ubicaciones */
object Catalogo {
    val subcategorias: Map<Categoria, List<String>> = mapOf(
        Categoria.INFRAESTRUCTURA to listOf("Electricidad", "Plomería / baños", "Puertas y ventanas", "Techumbre y muros", "Patios y áreas verdes"),
        Categoria.TECNOLOGIA to listOf("Computador / notebook", "Proyector", "Impresora", "Red / internet", "Parlantes y audio", "Robótica"),
        Categoria.MOBILIARIO to listOf("Sillas", "Mesas", "Estantes y muebles", "Pizarras"),
        Categoria.OTRA to listOf("Otra")
    )
    val ubicaciones = listOf(
        "Sala de clases", "Oficinas", "Biblioteca", "Laboratorio", "Sala sensorial",
        "CIEMP", "Patio", "Baños", "Huerto", "Otro espacio"
    )
}
