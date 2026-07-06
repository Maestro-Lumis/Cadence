package com.application.cadence.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
object StudentsRoute

@Serializable
object DebtsRoute

@Serializable
object AddStudentRoute

@Serializable
object AddLessonRoute

@Serializable
data class EditLessonRoute(val lessonId: Long)

@Serializable
data class StudentProfileRoute(val studentId: Long)

@Serializable
data class ScheduleRoute(val studentId: Long, val studentName: String)

@Serializable
data class EditStudentRoute(val studentId: Long)