package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configuration WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<Configuration?>

    @Query("SELECT * FROM configuration WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): Configuration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: Configuration)
}

@Dao
interface ForwardLogDao {
    @Query("SELECT * FROM forward_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ForwardLog>>

    @Query("SELECT * FROM forward_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(): ForwardLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ForwardLog)

    @Query("DELETE FROM forward_logs")
    suspend fun clearLogs()
}

@Dao
interface WithdrawJobDao {
    @Query("SELECT * FROM withdraw_jobs ORDER BY timestamp DESC")
    fun getAllJobs(): Flow<List<WithdrawJob>>

    @Query("SELECT * FROM withdraw_jobs WHERE ref = :ref LIMIT 1")
    suspend fun getJobByRef(ref: String): WithdrawJob?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: WithdrawJob)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertJobs(jobs: List<WithdrawJob>)

    @Query("DELETE FROM withdraw_jobs")
    suspend fun clearAllJobs()

    @Query("DELETE FROM withdraw_jobs WHERE ref = :ref")
    suspend fun deleteJobByRef(ref: String)
}

@Dao
interface AppUserDao {
    @Query("SELECT * FROM app_user WHERE id = 1 LIMIT 1")
    fun getAppUser(): Flow<AppUser?>

    @Query("SELECT * FROM app_user WHERE id = 1 LIMIT 1")
    suspend fun getAppUserDirect(): AppUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUser(user: AppUser)

    @Query("DELETE FROM app_user")
    suspend fun clearAppUser()
}
