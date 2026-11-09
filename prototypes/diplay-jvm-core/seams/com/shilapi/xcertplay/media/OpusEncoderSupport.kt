// Desktop replacement for DiPlay's media/OpusEncoderSupport.kt. A desktop JVM has no
// android.media.MediaCodecList. DiPlay's own fallback is the pure-Java Concentus encoder
// (org.concentus, compiled here), so Opus is always available. The real microphone
// uplink (SoftwareOpusEncoder/MicrophoneUplink) is not ported yet; see docs/diplay-audit.md.
package com.shilapi.xcertplay.media

object OpusEncoderSupport {
    fun isAvailable(): Boolean = true
}
