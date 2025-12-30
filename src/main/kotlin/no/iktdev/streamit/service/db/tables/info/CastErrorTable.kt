package no.iktdev.streamit.service.db.tables.info

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import javax.print.attribute.standard.PrinterMoreInfoManufacturer

object CastErrorTable : IntIdTable(name = "CAST_ERROR") {
    val file: Column<String> = varchar("SOURCE", 200)
    val deviceModel: Column<String> = varchar("DEVICE_MODEL", 50)
    val deviceManufacturer: Column<String> = varchar("DEVICE_MANUFACTURER", 50)
    val deviceBrand: Column<String> = varchar("DEVICE_BRAND", 50)
    val deviceOsVersion = varchar("OS_VERSION", 10)
    val appVersion = varchar("APP_VERSION", 10)
    val castDeviceName: Column<String> = varchar("CAST_DEVICE_NAME", 50)
    val error = text("ERROR").nullable()
    val timestamp = datetime("REPORTED_AT").clientDefault { LocalDateTime.now() }


    fun insert(deviceOsVersion: String, castDeviceName: String, appVersion: String, file: String, deviceBrand: String, deviceModel: String, deviceManufacturer: String, error: String): EntityID<Int>? {
        return CastErrorTable.insertIgnoreAndGetId {
            it[CastErrorTable.file] = file
            it[CastErrorTable.deviceModel] = deviceModel
            it[CastErrorTable.deviceManufacturer] = deviceManufacturer
            it[CastErrorTable.deviceBrand] = deviceBrand
            it[CastErrorTable.deviceOsVersion] = deviceOsVersion
            it[CastErrorTable.appVersion] = appVersion
            it[CastErrorTable.castDeviceName] = castDeviceName
            it[CastErrorTable.error] = error
        }
    }
}