package com.dpa.sportpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PendingMatchEvent::class], version = 2, exportSchema = true)
abstract class SportProDatabase : RoomDatabase() {
    abstract fun pendingEvents(): PendingMatchEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_match_events ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_match_events ADD COLUMN lastError TEXT")
                db.execSQL("ALTER TABLE pending_match_events ADD COLUMN confirmedAtMillis INTEGER")
            }
        }

        @Volatile private var instance: SportProDatabase? = null

        fun getInstance(context: Context): SportProDatabase =
            instance ?: synchronized(this) {
                instance ?: builder(context, "sportpro_offline.db").build().also { instance = it }
            }

        fun builder(context: Context, name: String): Builder<SportProDatabase> =
            Room.databaseBuilder(context.applicationContext, SportProDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
        // No destructive fallback: a missing migration must fail instead of deleting data.
    }
}
