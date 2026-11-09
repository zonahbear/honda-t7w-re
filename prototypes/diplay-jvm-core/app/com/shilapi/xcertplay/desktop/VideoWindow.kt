// In-process CarPlay screen with touch. Our code; GPL-3.0-or-later. RUNTIME-UNTESTED.
//
// ffmpeg (subprocess) decodes the Annex-B H.264/HEVC stream to raw BGR frames on stdout; this
// window paints them and owns the pointer, so a click, drag or touchscreen tap becomes a CarPlay
// touch report through [onTouch] (AirPlaySession.sendTouch, which expects coordinates in 0..1).
// Single touch only: Swing exposes the touchscreen as one mouse pointer.
package com.shilapi.xcertplay.desktop

import com.shilapi.xcertplay.airplay.AirPlayContact
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.RenderingHints
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.DataInputStream
import java.io.EOFException
import java.io.OutputStream
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

class VideoWindow(
    private val width: Int,
    private val height: Int,
    private val ffmpeg: String = "ffmpeg",
    private val log: (String) -> Unit = {},
) {
    /** Set by the receiver once an AirPlay session is active; returns false if the event channel is not up. */
    @Volatile var onTouch: (List<AirPlayContact>) -> Boolean = { false }

    private val image = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
    private val pixels = (image.raster.dataBuffer as DataBufferByte).data
    private var decoder: Process? = null
    private var decoderIn: OutputStream? = null
    @Volatile private var touching = false
    /** Frames decoded and painted so far; a diagnostic, and what the self-test checks. */
    @Volatile var framesShown = 0L
        private set

    private val panel = object : JPanel() {
        init {
            // this@VideoWindow: inside JPanel, bare width/height are the panel's own (still 0) size.
            preferredSize = Dimension(this@VideoWindow.width, this@VideoWindow.height)
            background = Color.BLACK
        }
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            synchronized(image) { g2.drawImage(image, 0, 0, this.width, this.height, null) }
        }
    }

    private val frame = JFrame("CarPlay").apply {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.add(panel)
        pack()
        setLocationRelativeTo(null)
    }

    init {
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { touching = true; send(e, true) }
            override fun mouseDragged(e: MouseEvent) { if (touching) send(e, true) }
            override fun mouseReleased(e: MouseEvent) { touching = false; onTouch(emptyList()) }
        }
        panel.addMouseListener(mouse)
        panel.addMouseMotionListener(mouse)
        SwingUtilities.invokeLater { frame.isVisible = true }
    }

    private fun send(e: MouseEvent, down: Boolean) {
        val x = (e.x.toDouble() / panel.width.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val y = (e.y.toDouble() / panel.height.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        if (!onTouch(listOf(AirPlayContact(id = 0, x = x, y = y, down = down)))) log("touch dropped: session not ready")
    }

    /** (Re)starts the decoder for a new stream; [format] is ffmpeg's demuxer name, h264 or hevc. */
    @Synchronized
    fun start(format: String) {
        stopDecoder()
        val proc = ProcessBuilder(
            ffmpeg, "-hide_banner", "-loglevel", "warning",
            // No "-fflags nobuffer": with a stdin pipe it makes ffmpeg emit no frames at all (tested).
            "-flags", "low_delay", "-probesize", "32", "-analyzeduration", "0",
            "-f", format, "-i", "-",
            "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", "${width}x$height", "-",
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        decoder = proc
        decoderIn = proc.outputStream
        Thread({ readFrames(proc) }, "video-frames").apply { isDaemon = true; start() }
        log("video: in-process $format decoder at ${width}x$height")
    }

    @Synchronized
    fun write(bytes: ByteArray) {
        try { decoderIn?.apply { write(bytes); flush() } }
        catch (e: Exception) { log("video decoder pipe closed: ${e.message}"); decoderIn = null }
    }

    private fun readFrames(proc: Process) {
        val input = DataInputStream(proc.inputStream.buffered(pixels.size))
        val buffer = ByteArray(pixels.size)
        try {
            while (true) {
                input.readFully(buffer)
                synchronized(image) { System.arraycopy(buffer, 0, pixels, 0, buffer.size) }
                framesShown++
                panel.repaint()
            }
        } catch (_: EOFException) {
        } catch (e: Exception) { log("video frame reader stopped: ${e.message}") }
    }

    @Synchronized
    private fun stopDecoder() {
        runCatching { decoderIn?.close() }
        decoder?.destroy()
        decoder = null
        decoderIn = null
    }

    /** Screen coordinates of a normalized point on the video panel (for the self-test's Robot). */
    fun screenPointOf(nx: Double, ny: Double): java.awt.Point {
        val origin = panel.locationOnScreen
        return java.awt.Point(origin.x + (nx * panel.width).toInt(), origin.y + (ny * panel.height).toInt())
    }

    fun close() {
        stopDecoder()
        SwingUtilities.invokeLater { frame.dispose() }
    }
}
