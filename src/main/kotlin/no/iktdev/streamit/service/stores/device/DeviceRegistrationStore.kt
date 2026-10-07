package no.iktdev.streamit.service.stores.device

import no.iktdev.streamit.service.db.tables.auth.RegisteredDevicesTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.dto.RegisterDeviceData
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Component

@Component
class DeviceRegistrationStore : IDeviceRegistrationStore {
    override fun register(device: RegisterDeviceData): Boolean =
        withTransaction(rollbackOnFailure = true) {
            RegisteredDevicesTable.insert {
                it[deviceId] = device.deviceId
                it[applicationPackageName] = device.applicationPackageName
                it[osVersion] = device.osVersion
                it[osPlatform] = device.osPlatform
            }
        }.isSuccess
}
