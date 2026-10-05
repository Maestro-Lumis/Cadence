package com.application.cadence.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.application.cadence.CadenceApplication
import com.application.cadence.R
import com.application.cadence.presentation.addlesson.AddLessonScreen
import com.application.cadence.presentation.addlesson.AddLessonViewModelFactory
import com.application.cadence.presentation.addstudent.AddStudentScreen
import com.application.cadence.presentation.backup.BackupScreen
import com.application.cadence.presentation.calendar.CalendarScreen
import com.application.cadence.presentation.calendar.CalendarViewModelFactory
import com.application.cadence.presentation.debts.DebtsScreen
import com.application.cadence.presentation.debts.DebtsViewModelFactory
import com.application.cadence.presentation.earnings.EarningsScreen
import com.application.cadence.presentation.earnings.EarningsViewModelFactory
import com.application.cadence.presentation.editlesson.EditLessonScreen
import com.application.cadence.presentation.editlesson.EditLessonViewModelFactory
import com.application.cadence.presentation.editstudent.EditStudentScreen
import com.application.cadence.presentation.editstudent.EditStudentViewModelFactory
import com.application.cadence.presentation.profile.ProfileScreen
import com.application.cadence.presentation.profile.ProfileViewModelFactory
import com.application.cadence.presentation.settings.SettingsScreen
import com.application.cadence.presentation.grouplesson.GroupLessonScreen
import com.application.cadence.presentation.grouplesson.GroupLessonViewModelFactory
import com.application.cadence.presentation.report.ReportScreen
import com.application.cadence.presentation.report.ReportViewModelFactory
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun onRoute(suffix: String): Boolean =
        currentRoute?.substringBefore("?")?.substringBefore("/")?.endsWith(suffix) == true

    val showBottomBar = onRoute("TodayRoute") || onRoute("CalendarRoute") ||
        onRoute("StudentsRoute") || onRoute("ProfileRoute")

    fun switchTab(destination: Any) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = onRoute("TodayRoute"),
                        onClick = { switchTab(TodayRoute) },
                        icon = { Text("🏠", fontSize = 18.sp) },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = onRoute("CalendarRoute"),
                        onClick = { switchTab(CalendarRoute) },
                        icon = { Text("📅", fontSize = 18.sp) },
                        label = { Text(stringResource(R.string.nav_calendar)) }
                    )
                    NavigationBarItem(
                        selected = onRoute("StudentsRoute"),
                        onClick = { switchTab(StudentsRoute) },
                        icon = { Text("👥", fontSize = 18.sp) },
                        label = { Text(stringResource(R.string.nav_students)) }
                    )
                    NavigationBarItem(
                        selected = onRoute("ProfileRoute"),
                        onClick = { switchTab(ProfileRoute) },
                        icon = { Text("👤", fontSize = 18.sp) },
                        label = { Text(stringResource(R.string.nav_profile)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<TodayRoute> {
                val factory = TodayViewModelFactory(app.lessonRepository, app.studentRepository, app)
                TodayScreen(
                    viewModel = viewModel(factory = factory),
                    onLessonClick = { lessonId -> navController.navigate(EditLessonRoute(lessonId)) },
                    onGroupClick = { groupId -> navController.navigate(GroupLessonRoute(groupId)) },
                    onAddLessonClick = { date -> navController.navigate(AddLessonRoute(date.toString())) }
                )
            }
            composable<CalendarRoute> {
                val factory = CalendarViewModelFactory(app.lessonRepository, app.studentRepository, app)
                CalendarScreen(
                    viewModel = viewModel(factory = factory),
                    onLessonClick = { lessonId -> navController.navigate(EditLessonRoute(lessonId)) },
                    onGroupClick = { groupId -> navController.navigate(GroupLessonRoute(groupId)) },
                    onAddLessonClick = { date -> navController.navigate(AddLessonRoute(date.toString())) }
                )
            }
            composable<StudentsRoute> {
                val factory = StudentsViewModelFactory(app, app.studentRepository, app.lessonRepository)
                StudentsScreen(
                    viewModel = viewModel(factory = factory),
                    onStudentClick = { studentId -> navController.navigate(StudentProfileRoute(studentId)) },
                    onAddStudentClick = { navController.navigate(AddStudentRoute) }
                )
            }
            composable<ProfileRoute> {
                val factory = ProfileViewModelFactory(app, app.studentRepository, app.lessonRepository)
                ProfileScreen(
                    viewModel = viewModel(factory = factory),
                    onDebts = { navController.navigate(DebtsRoute) },
                    onEarnings = { navController.navigate(EarningsRoute) },
                    onBackup = { navController.navigate(BackupRoute) },
                    onSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<DebtsRoute> {
                val factory = DebtsViewModelFactory(app.lessonRepository, app.studentRepository)
                DebtsScreen(
                    viewModel = viewModel(factory = factory),
                    onBack = { navController.popBackStack() },
                    onStudentClick = { studentId -> navController.navigate(StudentProfileRoute(studentId)) }
                )
            }
            composable<EarningsRoute> {
                val factory = EarningsViewModelFactory(app, app.studentRepository, app.lessonRepository)
                EarningsScreen(
                    viewModel = viewModel(factory = factory),
                    onBack = { navController.popBackStack() },
                    onStudentClick = { studentId -> navController.navigate(StudentProfileRoute(studentId)) }
                )
            }
            composable<BackupRoute> {
                BackupScreen(
                    backupManager = app.backupManager,
                    onBack = { navController.popBackStack() }
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
                    onReportClick = { navController.navigate(ReportRoute(route.studentId)) },
                    onEditClick = { navController.navigate(EditStudentRoute(route.studentId)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable<EditStudentRoute> { backStackEntry ->
                val route: EditStudentRoute = backStackEntry.toRoute()
                val factory = EditStudentViewModelFactory(route.studentId, app, app.studentRepository)
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
            composable<ReportRoute> { backStackEntry ->
                val route: ReportRoute = backStackEntry.toRoute()
                val factory = ReportViewModelFactory(app, route.studentId, app.studentRepository, app.lessonRepository)
                ReportScreen(
                    viewModel = viewModel(factory = factory),
                    onBack = { navController.popBackStack() }
                )
            }
            composable<GroupLessonRoute> { backStackEntry ->
                val route: GroupLessonRoute = backStackEntry.toRoute()
                val factory = GroupLessonViewModelFactory(route.groupId, app.studentRepository, app.lessonRepository, app)
                GroupLessonScreen(
                    viewModel = viewModel(factory = factory),
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable<EditLessonRoute> { backStackEntry ->
                val route: EditLessonRoute = backStackEntry.toRoute()
                val factory = EditLessonViewModelFactory(route.lessonId, app.lessonRepository, app.studentRepository, app)
                EditLessonScreen(
                    viewModel = viewModel(factory = factory),
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<AddStudentRoute> {
                val factory = AddStudentViewModelFactory(app, app.studentRepository)
                AddStudentScreen(
                    viewModel = viewModel(factory = factory),
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<AddLessonRoute> { backStackEntry ->
                val route: AddLessonRoute = backStackEntry.toRoute()
                val factory = AddLessonViewModelFactory(app, app.lessonRepository, app.studentRepository)
                AddLessonScreen(
                    viewModel = viewModel(factory = factory),
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    initialDate = route.date
                )
            }
        }
    }
}
