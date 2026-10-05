package com.example.ui.screens

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayers.model.LocationInfo
import com.example.prayers.sensor.CompassOrientation
import com.example.ui.theme.IslamicEmeraldDark
import com.example.ui.theme.IslamicEmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.IslamicGoldBright
import com.example.ui.theme.PrayerEntryEmerald
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaCompassScreen(
    orientation: CompassOrientation,
    qiblaBearing: Float,
    distanceKm: Int,
    location: LocationInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val currentHeading = orientation.trueAzimuth

    // Angular difference calculation
    val diff = (qiblaBearing - currentHeading + 360f) % 360f
    val signedDiff = if (diff > 180f) diff - 360f else diff
    val isAligned = abs(signedDiff) <= 3.0f

    // Vibrate when user aligns with Qibla
    LaunchedEffect(isAligned) {
        if (isAligned) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 100, 80, 120), -1)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val dialBorderColor by animateColorAsState(
        targetValue = if (isAligned) PrayerEntryEmerald else Color(0x66D4AF37),
        label = "dialBorderColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("qibla_compass_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Title & Accuracy Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "بوصلة القبلة",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${location.cityName} • نحو الكعبة المشرفة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Accuracy Status Chip
            val (accText, accColor) = when (orientation.accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "دقة عالية ✓" to PrayerEntryEmerald
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "دقة جيدة" to IslamicGoldBright
                else -> "تحتاج معايرة" to Color(0xFFF97316)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accColor.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCalibrationDialog = true }
                    .padding(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = accText,
                        color = accColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alignment Alert Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAligned) PrayerEntryEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isAligned) PrayerEntryEmerald.copy(alpha = pulseGlow) else Color(0x33D4AF37)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isAligned) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "محاذاة القبلة",
                        tint = PrayerEntryEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أنت الآن باتجاه القبلة الشريفة تماماً",
                        color = PrayerEntryEmerald,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    val turnDirection = if (signedDiff > 0) "اليمين ↻" else "اليسار ↺"
                    val turnDegrees = abs(signedDiff).roundToInt()
                    Text(
                        text = "انحرف $turnDegrees° نحو $turnDirection للمحاذاة",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // THE COMPASS DIAL COMPONENT
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(310.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D3222),
                            Color(0xFF071B12),
                            Color(0xFF030D08)
                        )
                    )
                )
                .border(if (isAligned) 3.5.dp else 2.5.dp, dialBorderColor, CircleShape)
        ) {
            // Rotating Compass Rose (Rotates opposite to phone heading)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
                    .rotate(-currentHeading)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer decorative ring
                drawCircle(
                    color = Color(0x40D4AF37),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Degree marks (0° to 355° every 5°)
                for (i in 0 until 360 step 5) {
                    val isCardinal = i % 90 == 0
                    val isMajor = i % 30 == 0

                    val tickLength = when {
                        isCardinal -> 14.dp.toPx()
                        isMajor -> 9.dp.toPx()
                        else -> 5.dp.toPx()
                    }

                    val tickColor = when {
                        isCardinal -> Color(0xFFFBBF24)
                        isMajor -> Color(0xCCD4AF37)
                        else -> Color(0x55FFFFFF)
                    }

                    val strokeWidth = when {
                        isCardinal -> 3.dp.toPx()
                        isMajor -> 2.dp.toPx()
                        else -> 1.dp.toPx()
                    }

                    rotate(i.toFloat(), center) {
                        drawLine(
                            color = tickColor,
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, tickLength),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Dedicated Qibla Pointer to Kaaba Bearing on the rotating dial
                rotate(qiblaBearing, center) {
                    // Golden Pointer Arrow toward Kaaba
                    val qiblaArrow = Path().apply {
                        moveTo(center.x, 10f)
                        lineTo(center.x - 12f, 44f)
                        lineTo(center.x, 34f)
                        lineTo(center.x + 12f, 44f)
                        close()
                    }
                    drawPath(
                        path = qiblaArrow,
                        color = Color(0xFF10B981)
                    )

                    // Glow line from center to Qibla
                    drawLine(
                        color = Color(0x8010B981),
                        start = center,
                        end = Offset(center.x, 44f),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }

                // True North Marker
                rotate(0f, center) {
                    val northTip = Path().apply {
                        moveTo(center.x, 14f)
                        lineTo(center.x - 8f, 32f)
                        lineTo(center.x + 8f, 32f)
                        close()
                    }
                    drawPath(path = northTip, color = Color(0xFFEF4444))
                }
            }

            // Fixed Center Hub with Kaaba direction & Level Bubble
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Fixed Forward Phone Pointer Marker (Golden triangle pointing straight UP)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAligned) PrayerEntryEmerald else Color(0xDD000000),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isAligned) IslamicGoldBright else Color(0x66FFFFFF)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAligned) "القبلة الشريفة ✓" else "زاوية القبلة: ${qiblaBearing.toInt()}°",
                            color = if (isAligned) Color.Black else IslamicGoldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Big Digital Azimuth Reading
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF))
                ) {
                    Text(
                        text = "${currentHeading.roundToInt()}°",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Built-in Bubble Level (ميزان استواء الهاتف)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .border(1.dp, if (orientation.isFlat) PrayerEntryEmerald else Color(0x44FFFFFF), CircleShape)
                ) {
                    val bubbleOffsetX = (orientation.roll * 0.7f).coerceIn(-11f, 11f)
                    val bubbleOffsetY = (orientation.pitch * 0.7f).coerceIn(-11f, 11f)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (orientation.isFlat) PrayerEntryEmerald else IslamicGoldBright)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level Hint
        if (!orientation.isFlat) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x22F59E0B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55F59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = IslamicGoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "يرجى مسك الهاتف بشكل أفقي مستوٍ لأعلى دقة في المستشعر",
                        color = IslamicGoldBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Qibla & Kaaba Telemetry Grid
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بيانات الموقع والقبلة المباشرة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGoldBright
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "اتجاه القبلة المطلوب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${qiblaBearing.toInt()}° من الشمال",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "اتجاه الهاتف الفعلي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${currentHeading.roundToInt()}° حقيقي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAligned) PrayerEntryEmerald else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "المسافة المباشرة لمكة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$distanceKm كم",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicEmeraldPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "إحداثيات الكعبة المشرفة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "21.42° N, 39.82° E",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calibration Button
        Button(
            onClick = { showCalibrationDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "معايرة مستشعر البوصلة",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }

    // Figure-8 Calibration Dialog
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = {
                Text(
                    text = "معايرة مستشعر البوصلة المغناطيسي",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "لضبط دقة البوصلة ومستشعر المغناطيسية في هاتفك:\n\n" +
                                "1. أمسك الهاتف بيدك في الهواء بعيداً عن المعادن والأجهزة الكهربائية.\n" +
                                "2. حرّك الهاتف في الهواء على شكل رقم ( 8 ) بالإنجليزي أو رمز اللانهاية ( ∞ ) لعدة ثوانٍ.\n" +
                                "3. ضع الهاتف بوضع أفقي مستوٍ للحصول على اتجاه القبلة الأدق.",
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCalibrationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicEmeraldPrimary)
                ) {
                    Text("تم الفهم")
                }
            }
        )
    }
}
