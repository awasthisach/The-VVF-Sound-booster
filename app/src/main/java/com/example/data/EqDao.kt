package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EqDao {
    @Query("SELECT * FROM eq_profiles ORDER BY name ASC")
    fun getAllProfilesFlow(): Flow<List<EqProfile>>

    @Query("SELECT * FROM eq_profiles ORDER BY name ASC")
    suspend fun getAllProfiles(): List<EqProfile>

    @Query("SELECT * FROM eq_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): EqProfile?

    @Query("SELECT * FROM eq_profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): EqProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: EqProfile): Long

    @Delete
    suspend fun deleteProfile(profile: EqProfile)

    @Query("SELECT * FROM device_mappings")
    fun getAllDeviceMappingsFlow(): Flow<List<DeviceMapping>>

    @Query("SELECT * FROM device_mappings WHERE deviceName = :deviceName LIMIT 1")
    suspend fun getDeviceMapping(deviceName: String): DeviceMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceMapping(mapping: DeviceMapping)

    @Query("DELETE FROM device_mappings WHERE deviceName = :deviceName")
    suspend fun deleteDeviceMapping(deviceName: String)
}
