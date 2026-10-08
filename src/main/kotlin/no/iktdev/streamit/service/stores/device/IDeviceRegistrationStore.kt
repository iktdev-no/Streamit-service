package no.iktdev.streamit.service.stores.device

import no.iktdev.streamit.service.model.shared.RegisterDeviceData

interface IDeviceRegistrationStore {
    fun register(device: RegisterDeviceData): Boolean
}
