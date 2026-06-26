package com.application.cadence.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val course: String,
    val createdAt: Long
)

@Entity(
    tableName = "packages",
    foreignKeys = [ForeignKey(StudentEntity::class, ["id"], ["studentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("studentId")]
)
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val totalLessons: Int,
    val usedLessons: Int,
    val paid: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(StudentEntity::class, ["id"], ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PackageEntity::class, ["id"], ["packageId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("studentId"), Index("packageId")]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val time: String,
    val status: String,
    val lessonNumber: Int?,
    val packageId: Long?,
    val paid: Boolean
)