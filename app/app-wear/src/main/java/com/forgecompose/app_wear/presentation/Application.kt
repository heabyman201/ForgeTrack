package com.forgecompose.app_wear.presentation



import android.app.Application
import com.forgecompose.app_wear.passive.registerPassiveHr
import com.forgecompose.app_wear.presentation.theme.hasHeartRatePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearApplication : Application() {
    // Lazy initialization of the database
    val database by lazy { AppDatabase.getDatabase(this) }

    // Lazy initialization of the repository
    val repository by lazy { WorkoutRepositoryImpl(database.workoutDao()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (!HrMonitorService.isExplicitStopRequested(this@WearApplication) &&
                hasHeartRatePermission(this@WearApplication)
            ) {
                registerPassiveHr(this@WearApplication)
            }
        }
    }
}
