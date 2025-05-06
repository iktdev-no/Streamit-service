package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.other.CastErrorTable
import no.iktdev.streamit.library.db.withTransaction

fun CastErrorTable.executeInsert(deviceOsVersion: String, castDeviceName: String, appVersion: String, file: String, deviceBrand: String, deviceModel: String, deviceManufacturer: String, error: String) = withTransaction {
    CastErrorTable.insert(deviceOsVersion, castDeviceName, appVersion, file, deviceBrand, deviceModel, deviceManufacturer, error)
}