package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.prayers.model.Prayer
import com.example.prayers.viewmodel.PrayerViewModel
import com.example.ui.components.HeroPrayerCard
import com.example.ui.components.HijriGregorianCalendarCard
import com.example.ui.components.PrayerScheduleList
import com.example.ui.screens.AthkarScreen
import com.example.ui.screens.QiblaCompassScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.theme.IslamicEmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.IslamicGoldBright

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    PRAYERS("المواقيت", Icons.Filled.AccessTime, Icons.Outlined.AccessTime, "tab_prayers"),
    QIBLA("القبلة", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_qibla"),
    ATHKAR("الأذكار", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_athkar"),
    TASBIH("السبحة", Icons.Filled.RadioButtonChecked, Icons.Outlined.RadioButtonUnchecked, "tab_tasbih"),
    SETTINGS("الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun MainScreen(
    viewModel: PrayerViewModel,
    initialTab: AppTab = AppTab.PRAYERS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // State collections
    val location by viewModel.location.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val realtimeStatus by viewModel.realtimeStatus.collectAsStateWithLifecycle()
    val hijriDate by viewModel.hijriDate.collectAsStateWithLifecycle()
    val isAzanPlaying by viewModel.isAzanPlaying.collectAsStateWithLifecycle()
    val isSimulationActive by viewModel.isSimulationActive.collectAsStateWithLifecycle()
    val simulateEntryWindow by viewModel.simulateEntryWindow.collectAsStateWithLifecycle()
    val prayerLog by viewModel.todayPrayerLog.collectAsStateWithLifecycle()

    val calculationMethod by viewModel.calculationMethod.collectAsStateWithLifecycle()
    val juristicMethod by viewModel.juristicMethod.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()

    val compassOrientation by viewModel.compassOrientation.collectAsStateWithLifecycle()
    val compassAzimuth by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    val qiblaBearing by viewModel.qiblaBearing.collectAsStateWithLifecycle()
    val distanceKm by viewModel.distanceToKaabaKm.collectAsStateWithLifecycle()

    val tasbihList by viewModel.tasbihList.collectAsStateWithLifecycle()

    // Handle back button on sub-tabs
    BackHandler(enabled = selectedTab != AppTab.PRAYERS) {
        selectedTab = AppTab.PRAYERS
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IslamicEmeraldPrimary,
                            selectedTextColor = IslamicEmeraldPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { currentTab ->
                    when (currentTab) {
                        AppTab.PRAYERS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("prayers_tab_content")
                            ) {
                                // 1. The Signature Hero Card with the Entry Line & Countdown Line
                                item {
                                    HeroPrayerCard(
                                        status = realtimeStatus,
                                        location = location,
                                        hijriDate = hijriDate,
                                        isAzanPlaying = isAzanPlaying,
                                        isSimulationActive = isSimulationActive,
                                        simulateEntryWindow = simulateEntryWindow,
                                        onToggleAzan = { viewModel.playAzanPreview() },
                                        onToggleSimulation = { active, isEntry ->
                                            viewModel.setSimulationMode(active, isEntry)
                                        },
                                        onOpenSettings = { selectedTab = AppTab.SETTINGS }
                                    )
                                }

                                item { Spacer(modifier = Modifier.height(16.dp)) }

                                // 2. Dual Hijri & Gregorian Calendar with Upcoming Islamic Occasions
                                item {
                                    HijriGregorianCalendarCard(
                                        hijriDate = hijriDate,
                                        currentHijriAdjustment = hijriAdjustment,
                                        onAdjustHijri = { newAdj ->
                                            viewModel.setHijriAdjustment(newAdj)
                                        }
                                    )
                                }

                                item { Spacer(modifier = Modifier.height(16.dp)) }

                                // 3. Prayer Schedule List with Tracking
                                item {
                                    PrayerScheduleList(
                                        schedule = schedule,
                                        status = realtimeStatus,
                                        prayerLog = prayerLog,
                                        onTogglePrayerCompleted = { prayer ->
                                            viewModel.togglePrayerCompleted(prayer)
                                        }
                                    )
                                }

                                item { Spacer(modifier = Modifier.height(20.dp)) }
                            }
                        }

                        AppTab.QIBLA -> {
                            QiblaCompassScreen(
                                orientation = compassOrientation,
                                qiblaBearing = qiblaBearing,
                                distanceKm = distanceKm,
                                location = location
                            )
                        }

                        AppTab.ATHKAR -> {
                            AthkarScreen()
                        }

                        AppTab.TASBIH -> {
                            TasbihScreen(
                                tasbihList = tasbihList,
                                onIncrement = { viewModel.incrementTasbih(it) },
                                onReset = { viewModel.resetTasbih(it) },
                                onAddCustom = { title, target ->
                                    viewModel.addCustomTasbih(title, target)
                                }
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                location = location,
                                calculationMethod = calculationMethod,
                                juristicMethod = juristicMethod,
                                hijriAdjustment = hijriAdjustment,
                                notificationsEnabled = notificationsEnabled,
                                isDetectingLocation = isDetectingLocation,
                                onSelectCity = { viewModel.selectCity(it) },
                                onSelectCalculationMethod = { viewModel.selectCalculationMethod(it) },
                                onSelectJuristicMethod = { viewModel.selectJuristicMethod(it) },
                                onAdjustHijri = { viewModel.adjustHijri(it) },
                                onToggleNotifications = { viewModel.toggleNotifications(it) },
                                onDetectGpsLocation = { viewModel.detectGpsLocation(it) },
                                onTestAzan = { viewModel.playAzanPreview() }
                            )
                        }
                    }
                }
            }
        }
    }
}
