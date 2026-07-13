package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Configuration::class, ForwardLog::class, WithdrawJob::class, AppUser::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun forwardLogDao(): ForwardLogDao
    abstract fun withdrawJobDao(): WithdrawJobDao
    abstract fun appUserDao(): AppUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "easyo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Helper function to get merged config (existing + default packages)
        fun getMergedConfig(config: Configuration?): Configuration {
            val defaultConfig = Configuration()
            val existingConfig = config ?: defaultConfig
            
            val existingPackages = existingConfig.selectedBankPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val defaultPackages = defaultConfig.selectedBankPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val mergedPackages = (existingPackages + defaultPackages).distinct()
            
            // Update token to new default if it was the old default
            val oldDefaultToken = "fd49e732c5f5ed78fe5fe38b5f8ac8c2"
            val updatedToken = if (existingConfig.token == oldDefaultToken) {
                defaultConfig.token
            } else {
                existingConfig.token
            }
            
            return existingConfig.copy(
                selectedBankPackages = mergedPackages.joinToString(","),
                token = updatedToken
            )
        }
    }
}
