// Desktop replacement for DiPlay's network/TcpLiveness.kt (which tunes keepalive through
// android.system.Os). Same package and signature, so AirPlaySession compiles unchanged.
// Our code; uses the JDK's portable extended socket options instead.
package com.shilapi.xcertplay.network

import java.net.Socket
import java.net.StandardSocketOptions
import jdk.net.ExtendedSocketOptions

internal object TcpLiveness {
    fun configure(socket: Socket, diagnostic: (String) -> Unit) {
        runCatching {
            socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
            socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 5)
            socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 2)
            socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 3)
            socket.tcpNoDelay = true
        }.onFailure { diagnostic("tcp keepalive tuning unavailable: ${it.message}") }
    }
}
