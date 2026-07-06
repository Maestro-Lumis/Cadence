package com.application.cadence.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.application.cadence.CadenceApplication
import com.application.cadence.presentation.addlesson.AddLessonScreen
import com.application.cadence.presentation.addlesson.AddLessonViewModelFactory
import com.application.cadence.presentation.addstudent.AddStudentScreen
import com.application.cadence.presentation.debts.DebtsScreen
import com.application.cadence.presentation.debts.DebtsViewModelFactory
import com.application.cadence.presentation.editlesson.EditLessonScreen
import com.application.cadence.presentation.editlesson.EditLessonViewModelFactory
import com.application.cadence.presentation.editstudent.EditStudentScreen
import com.application.cadence.presentation.editstudent.EditStudentViewModelFactory
import com.application.cadence.presentation.schedule.ScheduleScreen
import com.application.cadence.presentation.schedule.ScheduleViewModelFactory
import com.application.cadence.presentation.addstudent.AddStudentViewModelFactory
import com.application.cadence.presentation.students.StudentsScreen
import com.application.cadence.presentation.students.StudentsViewModelFactory
import com.application.cadence.presentation.studentprofile.StudentProfileScreen
import com.application.cadence.presentation.studentprofile.StudentProfileViewModelFactory
import com.application.cadence.presentation.today.TodayScreen
import com.application.cadence.presentation.today.TodayViewModelFactory

@Composable
fun AppNavHost(app: CadenceApplication) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TodayRoute) {
        composable<TodayRoute> {
            val factory = TodayViewModelFactory(app.lessonRepository, app.studentRepository)
            TodayScreen(
                viewModel = viewModel(factory = factory),
                onLessonClick = { lessonId -> navController.navigate(EditLessonRoute(lessonId)) },
                onAddStudentClick = { navController.navigate(AddStudentRoute) },
                onAddLessonClick = { navController.navigate(AddLessonRoute) },
                onAllStudentsClick = { navController.navigate(StudentsRoute) },
                onDebtsClick = { navController.navigate(DebtsRoute) }
            )
        }
        composable<StudentsRoute> {
            val factory = StudentsViewModelFactory(app.studentRepository)
            StudentsScreen(
                viewModel = viewModel(factory = factory),
                onBack = { navController.popBackStack() },
                onStudentClick = { studentId -> navController.navigate(StudentProfileRoute(studentId)) },
                onAddStudentClick = { navController.navigate(AddStudentRoute) }
            )
        }
        composable<DebtsRoute> {
            val factory = DebtsViewModelFactory(app.lessonRepository, app.studentRepository)
            DebtsScreen(
                viewModel = viewModel(factory = factory),
                onBack = { navController.popBackStack() },
                onStudentClick = { studentId -> navController.navigate(StudentProfileRoute(studentId)) }
            )
        }
        composable<StudentProfileRoute> { backStackEntry ->
            val route: StudentProfileRoute = backStackEntry.toRoute()
            val factory = StudentProfileViewModelFactory(route.studentId, app.lessonRepository, app.studentRepository)
            StudentProfileScreen(
                viewModel = viewModel(factory = factory),
                onBack = { navController.popBackStack() },
                onLessonClick = { lessonId -> navController.navigate(EditLessonRoute(lessonId)) },
                onScheduleClick = { name -> navController.navigate(ScheduleRoute(route.studentId, name)) },
                onEditClick = { navController.navigate(EditStudentRoute(route.studentId)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable<EditStudentRoute> { backStackEntry ->
            val route: EditStudentRoute = backStackEntry.toRoute()
            val factory = EditStudentViewModelFactory(route.studentId, app.studentRepository)
            EditStudentScreen(
                viewModel = viewModel(factory = factory),
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ScheduleRoute> { backStackEntry ->
            val route: ScheduleRoute = backStackEntry.toRoute()
            val factory = ScheduleViewModelFactory(route.studentId, app.scheduleRepository, app.lessonRepository)
            ScheduleScreen(
                viewModel = viewModel(factory = factory),
                studentName = route.studentName,
                onBack = { navController.popBackStack() }
            )
        }
        composable<EditLessonRoute> { backStackEntry ->
            val route: EditLessonRoute = backStackEntry.toRoute()
            val factory = EditLessonViewModelFactory(route.lessonId, app.lessonRepository, app.studentRepository)
            EditLessonScreen(
                viewModel = viewModel(factory = factory),
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<AddStudentRoute> {
            val factory = AddStudentViewModelFactory(app.studentRepository)
            AddStudentScreen(
                viewModel = viewModel(factory = factory),
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<AddLessonRoute> {
            val factory = AddLessonViewModelFactory(app.lessonRepository, app.studentRepository)
            AddLessonScreen(
                viewModel = viewModel(factory = factory),
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}