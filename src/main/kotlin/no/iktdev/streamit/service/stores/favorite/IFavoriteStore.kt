package no.iktdev.streamit.service.stores.favorite

import java.util.UUID

interface IFavoriteStore {

    fun getAll(userId: String): List<Long>
    fun add(userId: String, catalogId: Long)
    fun remove(userId: String, catalogId: Long)
}