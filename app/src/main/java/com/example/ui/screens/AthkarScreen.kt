package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayers.data.AthkarData
import com.example.prayers.data.DhikrItem
import com.example.prayers.notifications.AthkarReminder
import com.example.prayers.notifications.AthkarReminderScheduler
import com.example.ui.theme.IslamicEmeraldDark
import com.example.ui.theme.IslamicEmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.IslamicGoldBright
import com.example.ui.theme.PrayerEntryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthkarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(AthkarData.CATEGORIES[0]) }
    var showRemindersSheet by remember { mutableStateOf(false) }

    // Repetitions map: dhikrId -> remaining count
    val remainingCounts = remember {
        mutableStateMapOf<Int, Int>().apply {
            AthkarData.ATHKAR_LIST.forEach { item ->
                put(item.id, item.count)
            }
        }
    }

    val filteredList = remember(selectedCategory) {
        AthkarData.ATHKAR_LIST.filter { it.category == selectedCategory }
    }

    // Reading Progress Indicator calculations for current category
    val totalAthkarInCategory = filteredList.size
    val completedAthkarCount = filteredList.count { (remainingCounts[it.id] ?: it.count) <= 0 }
    val categoryProgressFraction = if (totalAthkarInCategory > 0) {
        completedAthkarCount.toFloat() / totalAthkarInCategory.toFloat()
    } else 0f
    val isCategoryFullyCompleted = totalAthkarInCategory > 0 && completedAthkarCount == totalAthkarInCategory

    // Global daily progress calculation
    val totalDailyAthkar = AthkarData.ATHKAR_LIST.size
    val totalDailyCompleted = AthkarData.ATHKAR_LIST.count { (remainingCounts[it.id] ?: it.count) <= 0 }
    val globalProgressFraction = if (totalDailyAthkar > 0) {
        totalDailyCompleted.toFloat() / totalDailyAthkar.toFloat()
    } else 0f

    fun vibrateShort() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(35)
            }
        }
    }

    fun vibrateCompletion() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 80, 50, 100), -1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("athkar_screen")
    ) {
        // TOP HEADER: Title & Scheduled Notifications Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأذكار اليومية وحصن المسلم",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ألا بذكر الله تطمئن القلوب",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Scheduled Notifications Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IslamicEmeraldPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IslamicEmeraldPrimary),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showRemindersSheet = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "التنبيهات المجدولة",
                        tint = IslamicEmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "التنبيهات",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicEmeraldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // READING PROGRESS INDICATOR CARD (مؤشر القراءة والإنجاز)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مؤشر قراءة: $selectedCategory",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCategoryFullyCompleted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrayerEntryEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تمت قراءة $completedAthkarCount من أصل $totalAthkarInCategory ذكراً (${(categoryProgressFraction * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reset button for current category
                    IconButton(
                        onClick = {
                            filteredList.forEach { item ->
                                remainingCounts[item.id] = item.count
                            }
                            vibrateShort()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة ضبط القسم",
                            tint = IslamicGoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar with animated fill
                val animatedProgress by animateFloatAsState(
                    targetValue = categoryProgressFraction,
                    label = "categoryProgress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (isCategoryFullyCompleted) PrayerEntryEmerald else IslamicGold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                // Completion Congratulation banner
                AnimatedVisibility(visible = isCategoryFullyCompleted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrayerEntryEmerald.copy(alpha = 0.15f))
                            .border(1.dp, PrayerEntryEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مبارك! أتممت قراءة $selectedCategory كاملةً بنجاح",
                            color = PrayerEntryEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "تقبل الله طاعتكم وكتب لكم الأجر والتحصين",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Categories Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AthkarData.CATEGORIES) { category ->
                val isSelected = category == selectedCategory
                val catItems = AthkarData.ATHKAR_LIST.filter { it.category == category }
                val catCompleted = catItems.count { (remainingCounts[it.id] ?: it.count) <= 0 }
                val isAllDone = catItems.isNotEmpty() && catCompleted == catItems.size

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category, fontSize = 13.sp)
                            if (isAllDone) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else PrayerEntryEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IslamicEmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Athkar Items List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredList, key = { it.id }) { item ->
                val remaining = remainingCounts[item.id] ?: item.count
                val isCompleted = remaining <= 0
                val progressFraction = 1f - (remaining.toFloat() / item.count.toFloat()).coerceIn(0f, 1f)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) {
                            PrayerEntryEmerald.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCompleted) PrayerEntryEmerald.copy(alpha = 0.4f) else Color(0x15FFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Thikr Text with Tashkeel
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Virtue & Reference
                        if (item.virtue.isNotBlank()) {
                            Text(
                                text = "الفضل: ${item.virtue}",
                                fontSize = 12.sp,
                                color = IslamicGoldBright,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.reference,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Reading Counter Button with Circular Progress Indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (remaining > 0) {
                                            val newCount = remaining - 1
                                            remainingCounts[item.id] = newCount
                                            if (newCount == 0) {
                                                vibrateCompletion()
                                            } else {
                                                vibrateShort()
                                            }
                                        }
                                    }
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (isCompleted) PrayerEntryEmerald else IslamicGold,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 3.5.dp,
                                    strokeCap = StrokeCap.Round
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = if (isCompleted) PrayerEntryEmerald else IslamicEmeraldPrimary,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "تم",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "$remaining",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SCHEDULED NOTIFICATIONS BOTTOM SHEET
    if (showRemindersSheet) {
        AthkarRemindersSheet(
            onDismiss = { showRemindersSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthkarRemindersSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var remindersList by remember { mutableStateOf(AthkarReminderScheduler.getReminders(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التنبيهات المجدولة للأذكار",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تذكير يومي بمواعيد الأذكار حتى لا تغفل عنها",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = IslamicEmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminders List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                remindersList.forEach { reminder ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = reminder.subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Time selector button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IslamicEmeraldPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    AthkarReminderScheduler.updateReminder(
                                                        context = context,
                                                        reminderId = reminder.id,
                                                        enabled = reminder.isEnabled,
                                                        hour = hourOfDay,
                                                        minute = minute
                                                    )
                                                    remindersList = AthkarReminderScheduler.getReminders(context)
                                                },
                                                reminder.hour,
                                                reminder.minute,
                                                false
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = IslamicEmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الوقت: ${reminder.formattedTime} (تغيير)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IslamicEmeraldPrimary
                                        )
                                    }
                                }
                            }

                            // Switch Toggle
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { isChecked ->
                                    AthkarReminderScheduler.updateReminder(
                                        context = context,
                                        reminderId = reminder.id,
                                        enabled = isChecked,
                                        hour = reminder.hour,
                                        minute = reminder.minute
                                    )
                                    remindersList = AthkarReminderScheduler.getReminders(context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IslamicEmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Notification Now Button
            OutlinedButton(
                onClick = {
                    AthkarReminderScheduler.sendTestNotification(
                        context = context,
                        title = "أذكار الصباح • إلا صلاتي",
                        subtitle = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ"
                    )
                    Toast.makeText(context, "تم إرسال إشعار التذكير بنجاح", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = IslamicEmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إرسال إشعار تجريبي الآن",
                    color = IslamicEmeraldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IslamicEmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "حفظ وإغلاق",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
