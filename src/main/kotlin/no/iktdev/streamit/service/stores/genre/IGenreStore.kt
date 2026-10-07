package no.iktdev.streamit.service.stores.genre

import no.iktdev.streamit.service.model.shared.content.Genre

interface IGenreStore {
    fun getById(id: Long): Genre?
    fun getAll(): List<Genre>
    fun getOrInsert(name: String): Genre
    fun getByName(name: String): Genre?
}