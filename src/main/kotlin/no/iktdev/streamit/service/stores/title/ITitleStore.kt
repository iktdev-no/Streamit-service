package no.iktdev.streamit.service.stores.title

import no.iktdev.streamit.service.model.shared.CollectionReference
import no.iktdev.streamit.service.model.shared.content.Title

interface ITitleStore {
    fun get(id: Long): Title?
    fun insert(catalogId: Long, title: String, language: String, preferred: Boolean = false): Title
    fun update(id: Long, title: String, language: String): Title?
    fun setPreferred(id: Long, preferred: Boolean)
    fun getAll(catalogId: Long): List<Title>
    fun findCollectionByTitles(titles: List<String>): CollectionReference?
}