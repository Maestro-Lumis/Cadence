package com.application.cadence.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
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

    @Query("SELECT * FROM lessons WHERE studentId = :studentId ORDER BY date DESC")
    fun observeByStudent(studentId: Long): Flow<List<LessonEntity>>

    @Insert suspend fun insert(l: LessonEntity): Long
    @Update suspend fun update(l: LessonEntity)
    @Delete suspend fun delete(l: LessonEntity)
    @Query("DELETE FROM lessons WHERE id = :id") suspend fun deleteById(id: Long)
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages WHERE studentId = :studentId")
    fun observeByStudent(studentId: Long): Flow<List<PackageEntity>>

    @Insert suspend fun insert(p: PackageEntity): Long
    @Update suspend fun update(p: PackageEntity)
}

@Database(
    entities = [StudentEntity::class, PackageEntity::class, LessonEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun packageDao(): PackageDao
}