package com.example.data

import kotlinx.coroutines.flow.Flow

class EqRepository(private val eqDao: EqDao) {
    val allProfiles: Flow<List<EqProfile>> = eqDao.getAllProfilesFlow()
    val systemProfiles: Flow<List<EqProfile>> = eqDao.getSystemProfilesFlow()
    val userProfiles: Flow<List<EqProfile>> = eqDao.getUserProfilesFlow()
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
        // Safe check: If target is isCustom = false (system/default), and there is already
        // an existing profile that has isCustom = true (user-customized) under that ID,
        // do NOT overwrite it, thus preserving the user's customizations.
        val existing = eqDao.getProfileById(profile.id) ?: eqDao.getProfileByName(profile.name)
        if (existing != null && existing.isCustom && !profile.isCustom) {
            return existing.id.toLong()
        }
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
