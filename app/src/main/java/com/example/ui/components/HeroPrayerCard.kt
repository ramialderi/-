package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.HijriDate
import com.example.prayers.model.LocationInfo
import com.example.prayers.model.Prayer
import com.example.prayers.model.PrayerRealtimeStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HeroPrayerCard(
    status: PrayerRealtimeStatus,
    location: LocationInfo,
    hijriDate: HijriDate,
    isAzanPlaying: Boolean,
    isSimulationActive: Boolean,
    simulateEntryWindow: Boolean,
    onToggleAzan: () -> Unit,
    onToggleSimulation: (Boolean, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(cal.time)
    val gregorianDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(cal.time)
    val hijriFormatted = if (hijriDate.formattedEn.isNotBlank()) hijriDate.formattedEn else hijriDate.formatted
    val cityName = location.getDisplayCity(isEnglish = true)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_prayer_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer Card matching the exact uploaded screenshot
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TOP ROW: Left Prayer, Center Location/Date, Right Prayer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column (Current / Preceding Prayer)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = getPrayerIcon(status.currentPrayer)),
                                contentDescription = status.currentPrayer.englishName,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.currentPrayer.englishName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = PrayerCalculator.formatTime12h(status.currentPrayerTimeMillis),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }

                        // Center Column (Location, Day, Dates)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable { onOpenSettings() }
                        ) {
                            Text(
                                text = cityName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD9822B) // Warm Amber from screenshot
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = dayOfWeek,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D7D6F) // Teal from screenshot
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = gregorianDate,
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280) // Muted gray
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = hijriFormatted,
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        // Right Column (Next Prayer)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = getPrayerIcon(status.nextPrayer)),
                                contentDescription = status.nextPrayer.englishName,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.nextPrayer.englishName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = PrayerCalculator.formatTime12h(status.nextPrayerTimeMillis),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // THE SIGNATURE PROGRESS LINE (Orange filled line with light grey background track)
                    val progressFraction = if (status.isWithinEntryWindow) {
                        status.entryProgress.coerceIn(0.05f, 1f)
                    } else {
                        status.nextPrayerProgress.coerceIn(0.02f, 1f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE9ECEF)) // Light gray background track from screenshot
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressFraction)
                                .height(7.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5881E)) // Vibrant warm amber/orange from screenshot
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // BOTTOM CENTER: Status Title & Big Bold Countdown
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (status.isWithinEntryWindow) {
                                "Now ${status.currentPrayer.englishName} Time"
                            } else {
                                "Left until ${status.nextPrayer.englishName}"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Digital Clock matching screenshot
                        Text(
                            text = if (status.isWithinEntryWindow) {
                                PrayerCalculator.formatDurationCountdown(status.elapsedSinceEntryMillis)
                            } else {
                                PrayerCalculator.formatDurationCountdown(status.remainingToNextMillis)
                            },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Golden Lantern Pin in bottom right corner (from screenshot)
                Image(
                    painter = painterResource(id = R.drawable.ic_prayer_lantern),
                    contentDescription = "Lantern",
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        // Subtitle below the Card: "Ela-Salaty" centered (from screenshot)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ela-Salaty",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Controls: Audio Preview & Simulation Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleAzan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAzanPlaying) Color(0xFFDC2626) else Color(0xFF108A56)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_play_azan")
            ) {
                Icon(
                    imageVector = if (isAzanPlaying) Icons.Default.Stop else Icons.Default.NotificationsActive,
                    contentDescription = "صوت الأذان",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAzanPlaying) "إيقاف" else "صوت الأذان",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Interactive simulation buttons to test the 15-minute entry window vs countdown window
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSimulationActive && simulateEntryWindow) Color(0xFFE5881E) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSimulation(true, true) }
                ) {
                    Text(
                        text = "دخول الوقت (15د)",
                        color = if (isSimulationActive && simulateEntryWindow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSimulationActive && !simulateEntryWindow) Color(0xFFE5881E) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSimulation(true, false) }
                ) {
                    Text(
                        text = "العد التنازلي",
                        color = if (isSimulationActive && !simulateEntryWindow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                if (isSimulationActive) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleSimulation(false, true) }
                    ) {
                        Text(
                            text = "مباشر",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getPrayerIcon(prayer: Prayer): Int {
    return when (prayer) {
        Prayer.FAJR -> R.drawable.ic_prayer_fajr
        Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR -> R.drawable.ic_prayer_sun
        Prayer.MAGHRIB, Prayer.ISHA -> R.drawable.ic_prayer_crescent
    }
}
