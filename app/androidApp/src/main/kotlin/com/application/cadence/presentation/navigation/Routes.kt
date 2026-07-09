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
data class AddLessonRoute(val date: String? = null)

@Serializable
data class EditLessonRoute(val lessonId: Long)

@Serializable
data class StudentProfileRoute(val studentId: Long)

@Serializable
data class ScheduleRoute(val studentId: Long, val studentName: String)

@Serializable
data class EditStudentRoute(val studentId: Long)