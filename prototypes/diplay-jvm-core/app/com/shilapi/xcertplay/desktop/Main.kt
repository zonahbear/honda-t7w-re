// CLI entry point for the wired Linux CarPlay receiver. Our code; GPL-3.0-or-later.
package com.shilapi.xcertplay.desktop

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val opts = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) { opts[a.substring(2)] = args.getOrNull(i + 1) ?: ""; i += 2 } else i++
    }
    if ("help" in opts || "key-dir" !in opts || "ncm-iface" !in opts) {
        System.err.println(
            """
            t7w-linux wired CarPlay receiver (prototype, RUNTIME-UNTESTED)

            Required:
              --key-dir DIR     holds offline-mfi/{identity.pk8,certificate.p7b} and the AirPlay identity
              --ncm-iface NAME  the iPhone's cdc_ncm netdev (see `ip link` after plugging in), e.g. enp0s20u1

            Optional:
              --width N  --height N  --fps N   (default 800 x 480 @ 30)

            Run usbmuxd-free (sudo systemctl stop usbmuxd) and with udev access to the iPhone.
            """.trimIndent(),
        )
        exitProcess(if ("help" in opts) 0 else 2)
    }
    DesktopReceiver(
        keyDir = File(opts.getValue("key-dir")),
        ncmInterface = opts.getValue("ncm-iface"),
        width = opts["width"]?.toInt() ?: 800,
        height = opts["height"]?.toInt() ?: 480,
        fps = opts["fps"]?.toInt() ?: 30,
        log = { println("[t7w] $it") },
    ).run()
}
