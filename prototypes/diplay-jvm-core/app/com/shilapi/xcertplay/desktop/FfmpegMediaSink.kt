// Desktop MediaSink: pipes CarPlay's decrypted H.264 and audio to ffmpeg/ffplay. This is the
// rendering layer DiPlay's media/AndroidMediaSink.kt provides on Android (MediaCodec + Surface +
// AudioTrack); here it is external processes. Our code; GPL-3.0-or-later. RUNTIME-UNTESTED.
//
// Video: the main screen's H.264 is length-prefixed AVCC; MediaCodecSupport.toAnnexB (DiPlay, pure)
// converts each access unit, with SPS/PPS prepended from the codec config, into an Annex-B stream
// fed to `ffplay -f h264 -`.
// Audio: LPCM is byte-swapped S16BE PCM -> `ffplay -f s16le`; AAC/Opus are handed to ffmpeg to
// decode. Audio sync with video is left to the two players (good enough for a bring-up demo).
package com.shilapi.xcertplay.desktop

import com.shilapi.xcertplay.airplay.AudioCodecKind
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.AudioStreamId
import com.shilapi.xcertplay.airplay.MediaSink
import com.shilapi.xcertplay.airplay.VideoCodec
import com.shilapi.xcertplay.media.MediaCodecSupport
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class FfmpegMediaSink(
    private val width: Int,
    private val height: Int,
    private val ffplay: String = "ffplay",
    private val log: (String) -> Unit = {},
) : MediaSink {
    private val startCode = byteArrayOf(0, 0, 0, 1)
    private var videoProc: Process? = null
    private var videoOut: OutputStream? = null
    @Volatile private var videoCodec: VideoCodec = VideoCodec.H264
    private val audioProcs = ConcurrentHashMap<AudioStreamId, Pair<Process, OutputStream>>()

    override fun onVideoCodec(type: Int, codec: VideoCodec) { if (type == MAIN) videoCodec = codec }

    @Synchronized
    override fun onVideoConfig(type: Int, codecData: ByteArray) {
        if (type != MAIN) return
        val fmt = if (videoCodec == VideoCodec.H264) "h264" else "hevc"
        val proc = ProcessBuilder(
            ffplay, "-hide_banner", "-loglevel", "warning", "-fflags", "nobuffer",
            "-flags", "low_delay", "-framedrop", "-x", width.toString(), "-y", height.toString(),
            "-window_title", "CarPlay", "-f", fmt, "-i", "-",
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        videoProc?.destroy()
        videoProc = proc
        videoOut = proc.outputStream
        // Prepend SPS/PPS as Annex-B so the decoder has parameter sets before the first frame.
        if (videoCodec == VideoCodec.H264) {
            val (sps, pps) = MediaCodecSupport.avcParameterSets(codecData)
            writeVideo(startCode + sps + startCode + pps)
        } else {
            writeVideo(MediaCodecSupport.hevcCodecSpecificData(codecData))
        }
        log("video: started $fmt ${width}x$height player")
    }

    override fun onVideoFrame(type: Int, naluBytes: ByteArray) {
        if (type != MAIN) return
        val annexB = MediaCodecSupport.toAnnexB(naluBytes)
        if (annexB.isNotEmpty()) writeVideo(annexB)
    }

    @Synchronized
    private fun writeVideo(bytes: ByteArray) {
        try { videoOut?.apply { write(bytes); flush() } }
        catch (e: Exception) { log("video pipe closed: ${e.message}"); videoOut = null }
    }

    override fun onAudioStarted(id: AudioStreamId, format: AudioFormat, firstSample: Int) {
        val args = when (format.codec) {
            AudioCodecKind.LPCM -> listOf("-f", "s16le", "-ar", format.sampleRate.toString(), "-ch_layout",
                if (format.channels == 1) "mono" else "stereo", "-i", "-")
            AudioCodecKind.AAC_LC -> listOf("-f", "aac", "-i", "-")
            AudioCodecKind.OPUS -> listOf("-f", "data", "-i", "-") // raw Opus packets; ffmpeg may need a container
        }
        try {
            val proc = ProcessBuilder(listOf(ffplay, "-hide_banner", "-loglevel", "warning", "-nodisp", "-autoexit") + args)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start()
            audioProcs[id] = proc to proc.outputStream
            log("audio: started ${format.codec} ${format.sampleRate}Hz x${format.channels} (${id.audioType})")
        } catch (e: Exception) { log("audio start failed: ${e.message}") }
    }

    override fun onAudioRtp(id: AudioStreamId, format: AudioFormat, rtp: ByteArray, sample: Int) {
        if (rtp.size <= 12) return
        val payload = rtp.copyOfRange(12, rtp.size)
        val bytes = if (format.codec == AudioCodecKind.LPCM) byteSwapS16(payload) else payload
        try { audioProcs[id]?.second?.apply { write(bytes); flush() } }
        catch (e: Exception) { log("audio pipe closed: ${e.message}") }
    }

    override fun onAudioStopped(id: AudioStreamId) {
        audioProcs.remove(id)?.let { (proc, _) -> proc.destroy() }
    }

    fun close() {
        videoProc?.destroy()
        audioProcs.values.forEach { (p, _) -> p.destroy() }
    }

    private fun byteSwapS16(src: ByteArray): ByteArray {
        val out = ByteArray(src.size)
        var i = 0
        while (i + 1 < src.size) { out[i] = src[i + 1]; out[i + 1] = src[i]; i += 2 }
        return out
    }

    companion object { private const val MAIN = 110 }
}
