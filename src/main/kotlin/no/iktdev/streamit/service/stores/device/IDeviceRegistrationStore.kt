package no.iktdev.streamit.service.stores.device

import no.iktdev.streamit.service.dto.RegisterDeviceData

interface IDeviceRegistrationStore {
    fun register(device: RegisterDeviceData): Boolean
}
