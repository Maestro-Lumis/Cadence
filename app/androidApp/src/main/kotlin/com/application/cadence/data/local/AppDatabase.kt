package com.application.cadence.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeById(id: Long): Flow<StudentEntity?>

    @Insert suspend fun insert(s: StudentEntity): Long
    @Update suspend fun update(s: StudentEntity)
    @Delete suspend fun delete(s: StudentEntity)
    @Query("DELETE FROM students WHERE id = :id") suspend fun deleteById(id: Long)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE date = :date ORDER BY time")
    fun observeByDate(date: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE date >= :from AND date <= :to ORDER BY date, time")
    fun observeInDateRange(from: String, to: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE studentId = :studentId ORDER BY date DESC")
    fun observeByStudent(studentId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE status = 'SCHEDULED' AND date <= :date ORDER BY date, time")
    fun observeScheduledUpTo(date: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE status = 'HELD' AND paid = 0 ORDER BY date")
    fun observeUnpaidHeld(): Flow<List<LessonEntity>>

    @Query("UPDATE lessons SET paid = 1 WHERE studentId = :studentId AND status = 'HELD' AND paid = 0")
    suspend fun markStudentDebtsPaid(studentId: Long)

    @Query("SELECT * FROM lessons")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getById(id: Long): LessonEntity?

    @Insert suspend fun insert(l: LessonEntity): Long
    @Update suspend fun update(l: LessonEntity)
    @Delete suspend fun delete(l: LessonEntity)
    @Query("DELETE FROM lessons WHERE id = :id") suspend fun deleteById(id: Long)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE studentId = :studentId")
    fun observeByStudent(studentId: Long): Flow<List<ScheduleEntity>>

    @Insert suspend fun insert(s: ScheduleEntity): Long
    @Query("DELETE FROM schedules WHERE id = :id") suspend fun deleteById(id: Long)
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun observeByStudent(studentId: Long): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE id = :id")
    fun observeById(id: Long): Flow<PackageEntity?>

    @Insert suspend fun insert(p: PackageEntity): Long
    @Update suspend fun update(p: PackageEntity)
    @Query("DELETE FROM packages WHERE id = :id") suspend fun deleteById(id: Long)
}

@Database(
    entities = [StudentEntity::class, PackageEntity::class, LessonEntity::class, ScheduleEntity::class],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun packageDao(): PackageDao
    abstract fun scheduleDao(): ScheduleDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lessons ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 60")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE students ADD COLUMN timezone TEXT NOT NULL DEFAULT 'Europe/Moscow'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `schedules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`studentId` INTEGER NOT NULL, " +
                "`dayOfWeek` TEXT NOT NULL, " +
                "`time` TEXT NOT NULL, " +
                "`durationMinutes` INTEGER NOT NULL, " +
                "`active` INTEGER NOT NULL, " +
                "FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedules_studentId` ON `schedules` (`studentId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE students ADD COLUMN hourlyRate INTEGER NOT NULL DEFAULT 0")
    }
}