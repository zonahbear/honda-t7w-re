// Self-test for VideoWindow without an iPhone: decodes a generated H.264 stream and drives the
// pointer with java.awt.Robot, then checks the CarPlay touch reports that would reach
// AirPlaySession.sendTouch. Needs a display (use xvfb-run headless) and ffmpeg with libx264.
// Our code; GPL-3.0-or-later.
package com.shilapi.xcertplay.desktop

import com.shilapi.xcertplay.airplay.AirPlayContact
import java.awt.Robot
import java.awt.event.InputEvent
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.exitProcess

fun main() {
    val w = 800; val h = 480
    val window = VideoWindow(w, h, log = { println("[test] $it") })
    val reports = CopyOnWriteArrayList<List<AirPlayContact>>()
    window.onTouch = { reports.add(it); true }

    window.start("h264")
    val source = ProcessBuilder(
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i", "testsrc=size=${w}x$h:rate=30",
        "-t", "4", "-c:v", "libx264", "-tune", "zerolatency", "-pix_fmt", "yuv420p", "-f", "h264", "-",
    ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    Thread {
        val buf = ByteArray(16 * 1024)
        source.inputStream.use { input ->
            while (true) { val n = input.read(buf); if (n < 0) break; window.write(buf.copyOf(n)) }
        }
    }.apply { isDaemon = true; start() }

    Thread.sleep(2500)
    val frames = window.framesShown

    val robot = Robot().apply { autoDelay = 40 }
    val start = window.screenPointOf(0.5, 0.5)
    val end = window.screenPointOf(0.75, 0.25)
    println("[test] panel origin=${window.screenPointOf(0.0, 0.0)} start=$start end=$end")
    robot.mouseMove(start.x, start.y)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    for (step in 1..5) {
        robot.mouseMove(start.x + (end.x - start.x) * step / 5, start.y + (end.y - start.y) * step / 5)
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    Thread.sleep(500)

    val first = reports.firstOrNull()?.singleOrNull()
    val lastDown = reports.lastOrNull { it.isNotEmpty() }?.singleOrNull()
    val released = reports.lastOrNull()?.isEmpty() == true
    fun near(a: Double, b: Double) = kotlin.math.abs(a - b) < 0.02
    val checks = listOf(
        "decoded frames > 30 (got $frames)" to (frames > 30),
        "first report is a press near (0.50, 0.50): $first" to
            (first != null && first.down && near(first.x, 0.5) && near(first.y, 0.5)),
        "drag ends near (0.75, 0.25): $lastDown" to
            (lastDown != null && lastDown.down && near(lastDown.x, 0.75) && near(lastDown.y, 0.25)),
        "release sends an empty contact list" to released,
        "reports seen: ${reports.size}" to (reports.size >= 3),
    )
    checks.forEach { (name, ok) -> println("${if (ok) "PASS" else "FAIL"}  $name") }
    source.destroy(); window.close()
    exitProcess(if (checks.all { it.second }) 0 else 1)
}
