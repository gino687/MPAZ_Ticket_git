package com.fabiancg.mpazticket

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TicketViewModel : ViewModel() {
    private val repo = RepositorioTickets
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario
    val tickets: StateFlow<List<Ticket>> = repo.tickets
    private val u get() = _usuario.value!!

    fun login(usuario: String, clave: String): Boolean {
        _usuario.value = repo.autenticar(usuario, clave)
        return _usuario.value != null
    }

    fun logout() { _usuario.value = null }

    /** Cada perfil ve solo lo que le corresponde. */
    fun visibles(lista: List<Ticket>): List<Ticket> {
        val actual = _usuario.value ?: return emptyList()
        return when (actual.rol) {
            Rol.ADMINISTRADOR -> lista
            Rol.SOLICITANTE -> lista.filter { it.solicitanteId == actual.id }
            Rol.RESPONSABLE -> lista.filter { it.asignadoA == actual.id }
        }
    }

    fun crear(titulo: String, desc: String, cat: Categoria, sub: String, ubic: String, fotos: List<String>) =
        repo.crear(u, titulo, desc, cat, sub, ubic, fotos).codigo

    fun priorizar(id: Int, cat: Categoria, prio: Prioridad) = repo.priorizar(id, u, cat, prio)
    fun asignar(id: Int, resp: Usuario) = repo.asignar(id, u, resp)
    fun cambiarEstado(id: Int, estado: Estado) = repo.cambiarEstado(id, u, estado)
    fun comentar(id: Int, texto: String) = repo.comentar(id, u, texto)
    fun registrarAvance(id: Int, acciones: String, materiales: String) = repo.registrarAvance(id, u, acciones, materiales)
    fun agregarEvidencia(id: Int, ruta: String) = repo.agregarEvidencia(id, u, ruta)
}
