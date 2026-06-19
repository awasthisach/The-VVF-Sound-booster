package com.example.data

import kotlinx.coroutines.flow.Flow

class EqRepository(private val eqDao: EqDao) {
    val allProfiles: Flow<List<EqProfile>> = eqDao.getAllProfilesFlow()
    val allDeviceMappings: Flow<List<DeviceMapping>> = eqDao.getAllDeviceMappingsFlow()

    suspend fun getAllProfiles(): List<EqProfile> {
        return eqDao.getAllProfiles()
    }

    suspend fun getProfileById(id: Int): EqProfile? {
        return eqDao.getProfileById(id)
    }

    suspend fun getProfileByName(name: String): EqProfile? {
        return eqDao.getProfileByName(name)
    }

    suspend fun insertProfile(profile: EqProfile): Long {
        return eqDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: EqProfile) {
        eqDao.deleteProfile(profile)
    }

    suspend fun getDeviceMapping(deviceName: String): DeviceMapping? {
        return eqDao.getDeviceMapping(deviceName)
    }

    suspend fun insertDeviceMapping(mapping: DeviceMapping) {
        eqDao.insertDeviceMapping(mapping)
    }

    suspend fun deleteDeviceMapping(deviceName: String) {
        eqDao.deleteDeviceMapping(deviceName)
    }
}
