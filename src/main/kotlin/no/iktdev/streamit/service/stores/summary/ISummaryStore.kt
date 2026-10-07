package no.iktdev.streamit.service.stores.summary

import no.iktdev.streamit.service.model.shared.content.Summary

interface ISummaryStore {
    fun get(summaryId: Long): Summary?
    fun getAll(catalogId: Long): List<Summary>
    fun insert(catalogId: Long, description: String, language: String): Summary
    fun update(id: Long, description: String, language: String): Summary?
    fun delete(id: Long)
}