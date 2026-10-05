package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.local.PrayerLogEntity
import com.example.prayers.model.Prayer
import com.example.prayers.model.PrayerDaySchedule
import com.example.prayers.model.PrayerRealtimeStatus
import com.example.ui.theme.IslamicEmeraldDark
import com.example.ui.theme.IslamicEmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.IslamicGoldBright
import com.example.ui.theme.PrayerEntryEmerald

@Composable
fun PrayerScheduleList(
    schedule: PrayerDaySchedule,
    status: PrayerRealtimeStatus,
    prayerLog: PrayerLogEntity,
    onTogglePrayerCompleted: (Prayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        PrayerItemData(Prayer.FAJR, "الفجر", "Fajr", schedule.fajrMillis, Icons.Default.WbTwilight, prayerLog.fajrDone),
        PrayerItemData(Prayer.SUNRISE, "الشروق", "Sunrise", schedule.sunriseMillis, Icons.Default.WbSunny, false),
        PrayerItemData(Prayer.DHUHR, "الظهر", "Dhuhr", schedule.dhuhrMillis, Icons.Default.WbSunny, prayerLog.dhuhrDone),
        PrayerItemData(Prayer.ASR, "العصر", "Asr", schedule.asrMillis, Icons.Default.WbSunny, prayerLog.asrDone),
        PrayerItemData(Prayer.MAGHRIB, "المغرب", "Maghrib", schedule.maghribMillis, Icons.Default.WbTwilight, prayerLog.maghribDone),
        PrayerItemData(Prayer.ISHA, "العشاء", "Isha", schedule.ishaMillis, Icons.Default.Bedtime, prayerLog.ishaDone)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مواقيت صلوات اليوم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "سجّل صلواتك اليومية ✓",
                fontSize = 12.sp,
                color = IslamicEmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        items.forEach { item ->
            val isCurrent = status.currentPrayer == item.prayer
            val isNext = status.nextPrayer == item.prayer

            PrayerRowCard(
                item = item,
                isCurrent = isCurrent,
                isNext = isNext,
                onToggleDone = {
                    if (item.prayer.isObligatory) {
                        onTogglePrayerCompleted(item.prayer)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Night Extra Times (Qiyam & Midnight)
        Spacer(modifier = Modifier.height(8.dp))
        NightTimesCard(schedule = schedule)
    }
}

data class PrayerItemData(
    val prayer: Prayer,
    val arabicName: String,
    val englishName: String,
    val timeMillis: Long,
    val icon: ImageVector,
    val isDone: Boolean
)

@Composable
private fun PrayerRowCard(
    item: PrayerItemData,
    isCurrent: Boolean,
    isNext: Boolean,
    onToggleDone: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrent -> PrayerEntryEmerald
            isNext -> IslamicGold
            else -> Color.Transparent
        },
        label = "borderColor"
    )

    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        isNext -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrent || isNext) 1.5.dp else 0.5.dp,
                color = if (isCurrent || isNext) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("prayer_card_${item.prayer.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Prayer Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCurrent -> PrayerEntryEmerald.copy(alpha = 0.2f)
                        isNext -> IslamicGold.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.arabicName,
                            tint = when {
                                isCurrent -> PrayerEntryEmerald
                                isNext -> IslamicGold
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.arabicName,
                            fontSize = 17.sp,
                            fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrayerEntryEmerald
                            ) {
                                Text(
                                    text = "الآن",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isNext) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IslamicGold
                            ) {
                                Text(
                                    text = "التالية",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = item.englishName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Time & Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = PrayerCalculator.formatTime(item.timeMillis),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrent -> PrayerEntryEmerald
                        isNext -> IslamicGold
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (item.prayer.isObligatory) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isDone) IslamicEmeraldPrimary else Color.Transparent
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (item.isDone) IslamicEmeraldPrimary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onToggleDone() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تمت الصلاة",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightTimesCard(schedule: PrayerDaySchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "الإمساك",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = PrayerCalculator.formatTime(schedule.imsakMillis),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "منتصف الليل",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = PrayerCalculator.formatTime(schedule.midnightMillis),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "قيام الليل (الثلث الأخير)",
                    fontSize = 12.sp,
                    color = IslamicGold
                )
                Text(
                    text = PrayerCalculator.formatTime(schedule.qiyamMillis),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGoldBright
                )
            }
        }
    }
}
