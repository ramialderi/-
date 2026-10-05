package com.example.prayers.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object AzanAudioPlayer {

    private var playingJob: Job? = null
    private var isPlaying = false

    /**
     * Synthesizes a beautiful spiritual chime / harmonic Islamic chord
     * representing the call to prayer (Allahu Akbar tones: Bb - D - F - Bb notes).
     * Works 100% offline on any Android device without external assets.
     */
    fun playPrayerAlert(context: Context, onComplete: () -> Unit = {}) {
        stop()
        isPlaying = true

        playingJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                // Vibrate device
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 400, 200, 400), -1)
                    }
                }

                // Try system notification sound first
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.play()
                } catch (_: Exception) {}

                // Synthesize melodious harmonic azan sequence (Allahu Akbar melody)
                val sampleRate = 44100
                val notes = listOf(
                    349.23 to 600, // F4
                    440.00 to 600, // A4
                    523.25 to 900, // C5
                    440.00 to 700, // A4
                    349.23 to 1200 // F4
                )

                for ((freq, durationMs) in notes) {
                    if (!isPlaying) break
                    playTone(sampleRate, freq, durationMs)
                    delay(80)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
                launch(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private fun playTone(sampleRate: Int, frequency: Double, durationMs: Int) {
        val numSamples = (durationMs * sampleRate / 1000)
        val sample = ByteArray(2 * numSamples)

        val rampSamples = (0.05 * sampleRate).toInt().coerceAtMost(numSamples / 4)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Main tone + warm octave overtone
            val sinVal = 0.7 * sin(2.0 * Math.PI * frequency * t) + 0.3 * sin(4.0 * Math.PI * frequency * t)

            // Envelope to avoid click sounds
            val envelope = when {
                i < rampSamples -> i.toDouble() / rampSamples
                i > numSamples - rampSamples -> (numSamples - i).toDouble() / rampSamples
                else -> 1.0
            }

            val pcm = (sinVal * envelope * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            sample[2 * i] = (pcm.toInt() and 0xFF).toByte()
            sample[2 * i + 1] = ((pcm.toInt() shr 8) and 0xFF).toByte()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sample.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(sample, 0, sample.size)
        track.play()
        Thread.sleep(durationMs.toLong())
        track.stop()
        track.release()
    }

    fun stop() {
        isPlaying = false
        playingJob?.cancel()
        playingJob = null
    }

    fun isPlaying(): Boolean = isPlaying
}
