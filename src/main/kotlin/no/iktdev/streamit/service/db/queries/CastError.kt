package no.iktdev.streamit.service.db.queries

import no.iktdev.streamit.service.db.tables.info.CastErrorTable
import no.iktdev.streamit.service.db.tables.util.withTransaction


fun CastErrorTable.executeInsert(deviceOsVersion: String, castDeviceName: String, appVersion: String, file: String, deviceBrand: String, deviceModel: String, deviceManufacturer: String, error: String) = withTransaction {
    CastErrorTable.insert(deviceOsVersion, castDeviceName, appVersion, file, deviceBrand, deviceModel, deviceManufacturer, error)
}