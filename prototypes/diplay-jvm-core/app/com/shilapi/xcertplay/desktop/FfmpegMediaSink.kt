// Desktop MediaSink: pipes CarPlay's decrypted H.264 and audio to ffmpeg/ffplay. This is the
// rendering layer DiPlay's media/AndroidMediaSink.kt provides on Android (MediaCodec + Surface +
// AudioTrack); here it is external processes. Our code; GPL-3.0-or-later. RUNTIME-UNTESTED.
//
// Video: the main screen's H.264 is length-prefixed AVCC; MediaCodecSupport.toAnnexB (DiPlay, pure)
// converts each access unit, with SPS/PPS prepended from the codec config, into an Annex-B stream
// decoded by ffmpeg into [VideoWindow], which also forwards touch.
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
    @Volatile private var videoCodec: VideoCodec = VideoCodec.H264
    private val audioProcs = ConcurrentHashMap<AudioStreamId, Pair<Process, OutputStream>>()

    override fun onVideoCodec(type: Int, codec: VideoCodec) { if (type == MAIN) videoCodec = codec }

    /** The in-process screen: paints decoded frames and turns clicks/taps into CarPlay touches. */
    val window = VideoWindow(width, height, log = log)

    override fun onVideoConfig(type: Int, codecData: ByteArray) {
        if (type != MAIN) return
        val fmt = if (videoCodec == VideoCodec.H264) "h264" else "hevc"
        window.start(fmt)
        // Prepend the parameter sets as Annex-B so the decoder has them before the first frame.
        if (videoCodec == VideoCodec.H264) {
            val (sps, pps) = MediaCodecSupport.avcParameterSets(codecData)
            window.write(startCode + sps + startCode + pps)
        } else {
            window.write(MediaCodecSupport.hevcCodecSpecificData(codecData))
        }
    }

    override fun onVideoFrame(type: Int, naluBytes: ByteArray) {
        if (type != MAIN) return
        val annexB = MediaCodecSupport.toAnnexB(naluBytes)
        if (annexB.isNotEmpty()) window.write(annexB)
    }

    override fun onAudioStarted(id: AudioStreamId, format: AudioFormat, firstSample: Int) {
        val args = when (format.codec) {
            AudioCodecKind.LPCM -> listOf("-f", "s16le", "-ar", format.sampleRate.toString(), "-ch_layout",
                if (format.channels == 1) "mono" else "stereo", "-i", "-")
            AudioCodecKind.AAC_LC -> listOf("-f", "aac", "-i", "-") // fed ADTS frames, see onAudioRtp
            // Opus is decoded here with Concentus (pure Java, bundled with DiPlay) to 48 kHz PCM.
            AudioCodecKind.OPUS -> listOf("-f", "s16le", "-ar", "48000", "-ch_layout",
                if (format.channels == 1) "mono" else "stereo", "-i", "-")
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
        val bytes = when (format.codec) {
            // AirPlay LPCM is big-endian; the player is told s16le.
            AudioCodecKind.LPCM -> byteSwapS16(payload)
            // Raw AAC access units need an ADTS header for ffmpeg's aac demuxer (DiPlay does the same).
            AudioCodecKind.AAC_LC -> MediaCodecSupport.adtsFrame(payload, format.sampleRate, format.channels)
            AudioCodecKind.OPUS -> decodeOpus(id, format.channels, payload) ?: return
        }
        try { audioProcs[id]?.second?.apply { write(bytes); flush() } }
        catch (e: Exception) { log("audio pipe closed: ${e.message}") }
    }

    override fun onAudioStopped(id: AudioStreamId) {
        audioProcs.remove(id)?.let { (proc, _) -> proc.destroy() }
        opusDecoders.remove(id)
    }

    private val opusDecoders = ConcurrentHashMap<AudioStreamId, org.concentus.OpusDecoder>()

    private fun decodeOpus(id: AudioStreamId, channels: Int, packet: ByteArray): ByteArray? = try {
        val decoder = opusDecoders.getOrPut(id) { org.concentus.OpusDecoder(48_000, channels) }
        val pcm = ShortArray(5760 * channels) // 120 ms at 48 kHz, Opus's largest frame
        val samples = decoder.decode(packet, 0, packet.size, pcm, 0, 5760, false)
        ByteArray(samples * channels * 2).also { out ->
            for (i in 0 until samples * channels) {
                out[2 * i] = (pcm[i].toInt() and 0xff).toByte()
                out[2 * i + 1] = (pcm[i].toInt() shr 8).toByte()
            }
        }
    } catch (e: Exception) { log("opus decode failed: ${e.message}"); null }

    fun close() {
        window.close()
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
