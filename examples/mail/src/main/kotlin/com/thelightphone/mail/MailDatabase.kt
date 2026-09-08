package com.thelightphone.mail

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MailAccountEntity::class], version = 2, exportSchema = false)
abstract class MailDatabase : RoomDatabase() {
    abstract fun accountDao(): MailAccountDao
}

val MAIL_DATABASE_MIGRATIONS: Array<Migration> = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE mail_account ADD COLUMN last_seen_uid INTEGER NOT NULL DEFAULT 0")
        }
    },
)
