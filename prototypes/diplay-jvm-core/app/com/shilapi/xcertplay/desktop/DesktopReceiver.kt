// Wires DiPlay's portable core into a wired Linux CarPlay receiver: the Linux equivalent of
// CarPlayController.runStack (Android-only). Our code; GPL-3.0-or-later. RUNTIME-UNTESTED: this
// compiles against DiPlay's APIs but was never run against an iPhone in this environment.
package com.shilapi.xcertplay.desktop

import com.shilapi.xcertplay.airplay.AirPlayConfig
import com.shilapi.xcertplay.airplay.AirPlayDisplayConfig
import com.shilapi.xcertplay.airplay.AirPlayIdentity
import com.shilapi.xcertplay.airplay.AirPlaySession
import com.shilapi.xcertplay.airplay.AirPlaySessionListener
import com.shilapi.xcertplay.airplay.CarPlayMediaEngine
import com.shilapi.xcertplay.airplay.PairingStore
import com.shilapi.xcertplay.mfi.Iap2MfiAuthenticationClient
import com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient
import com.shilapi.xcertplay.network.AirPlayPortSelector
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import com.shilapi.xcertplay.iap2.session.Iap2Session
import com.shilapi.xcertplay.transport.Iap2UsbMuxHost
import com.shilapi.xcertplay.transport.Iap2WiredCarPlayEndpoint
import com.shilapi.xcertplay.transport.Iap2WiredControlClient
import com.shilapi.xcertplay.transport.LockdownCarKitClient
import com.shilapi.xcertplay.transport.LockdownPairingClient
import java.io.File
import java.net.Inet6Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class DesktopReceiver(
    private val keyDir: File,
    private val ncmInterface: String,
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val log: (String) -> Unit,
) {
    fun run() {
        val mfi = LocalMfiAuthenticationClient.load(File(keyDir, LocalMfiAuthenticationClient.DIRECTORY)) {
            log("MFi: signed a $it-byte challenge")
        }
        log("MFi identity loaded and self-verified from ${keyDir}/${LocalMfiAuthenticationClient.DIRECTORY}")

        val identity = loadOrCreateIdentity()
        val mac = deviceMac(identity)
        val pairings = PairingStore()
        val sink = FfmpegMediaSink(width, height, log = log)
        val media = CarPlayMediaEngine(sink)
        val config = airPlayConfig(identity, mac)

        log("requesting CarPlay USB mode; unlock the iPhone and tap Trust if asked")
        LinuxIphoneUsb.requestCarPlayMode(log)
        val usb = pollUsbMux()
        val host = Iap2UsbMuxHost.open(usb, onDiagnostic = log)
        log("USBMUX host opened; pairing with Lockdown")

        val pairing = LockdownPairingClient(host)
        val record = pairing.pair(
            label = LABEL, hostId = UUID.randomUUID().toString().uppercase(),
            systemBuid = UUID.randomUUID().toString().uppercase(), totalTimeoutMillis = 30_000,
        ).pairRecord
        val carkit = LockdownCarKitClient(host).open(record, LABEL)
        val csm = Iap2Session.open(carkit, traceContext = "wired", onTrace = log)
        log("iAP2 control channel open")

        // Kernel cdc_ncm owns the NCM interface; bind the AirPlay server to its IPv6 link-local.
        val hostAddress = linkLocalOf(ncmInterface)
        val servers = AirPlayPortSelector.bindAll(listOf(hostAddress), preferredPort = 7000) { busy, bound ->
            log("AirPlay port $busy busy; bound $bound")
        }
        val server = servers.first()
        val port = server.localPort
        log("AirPlay server on [${hostAddress.hostAddress}%$ncmInterface]:$port")

        val running = AtomicBoolean(true)
        Thread({
            while (running.get()) {
                val socket = try { server.accept() } catch (e: Exception) { break }
                socket.tcpNoDelay = true
                log("iPhone AirPlay connection from ${socket.remoteSocketAddress}")
                AirPlaySession(socket, config, identity, pairings, mfi, loggingListener(sink), media).start()
            }
        }, "airplay-accept").apply { isDaemon = true; start() }

        val endpoint = Iap2WiredCarPlayEndpoint(
            ipv6Addresses = listOf(hostAddress.hostAddress.substringBefore('%')),
            airPlayPort = port, publicKey = identity.publicKeyHex, sourceVersion = config.sourceVersion,
            deviceIdentifier = mac,
        )
        log("starting iAP2 wired control loop")
        val result = Iap2WiredControlClient(csm, Iap2MfiAuthenticationClient(mfi)).run(
            identification = identification(mac),
            endpoint = endpoint,
            availableCurrentMilliAmps = 1500,
            onProgress = { log("iap2 $it") },
        )
        running.set(false)
        servers.forEach { it.close() }
        sink.close()
        log("control loop ended: ${result.terminal} at stage ${result.stage}")
    }

    private fun pollUsbMux(): com.shilapi.xcertplay.transport.Iap2UsbSession {
        val deadline = System.nanoTime() + 10_000_000_000L
        var last: Exception? = null
        while (System.nanoTime() < deadline) {
            try { return LinuxIphoneUsb.openUsbMux(log) } catch (e: Exception) { last = e; Thread.sleep(200) }
        }
        throw last ?: IllegalStateException("iPhone did not re-enumerate with a USBMUX interface")
    }

    private fun loadOrCreateIdentity(): AirPlayIdentity {
        val priv = File(keyDir, "airplay-identity.priv")
        val pub = File(keyDir, "airplay-identity.pub")
        val id = File(keyDir, "airplay-identity.id")
        return if (priv.exists() && pub.exists() && id.exists()) {
            AirPlayIdentity(priv.readBytes(), pub.readBytes(), id.readText().trim())
        } else {
            AirPlayIdentity.generate().also {
                priv.writeBytes(it.privateKey); pub.writeBytes(it.publicKey); id.writeText(it.pairingId)
                log("generated a new AirPlay identity in $keyDir")
            }
        }
    }

    private fun deviceMac(identity: AirPlayIdentity): String {
        val h = MessageDigest.getInstance("SHA-256").digest(identity.publicKey)
        val b = h.copyOf(6); b[0] = (b[0].toInt() and 0xfe or 0x02).toByte() // locally-administered unicast
        return b.joinToString(":") { "%02x".format(it) }
    }

    private fun linkLocalOf(iface: String): Inet6Address {
        val ni = NetworkInterface.getByName(iface)
            ?: error("No network interface '$iface' (is the iPhone's cdc_ncm netdev up? check `ip link`)")
        return ni.inetAddresses.toList().filterIsInstance<Inet6Address>().firstOrNull { it.isLinkLocalAddress }
            ?: error("Interface '$iface' has no IPv6 link-local address yet")
    }

    private fun airPlayConfig(identity: AirPlayIdentity, mac: String) = AirPlayConfig(
        deviceName = "T7W-Linux", deviceId = mac, btMac = mac, sourceVersion = "950.7.1",
        main = AirPlayDisplayConfig(widthPixels = width, heightPixels = height, fps = fps),
        manufacturer = "t7w-re", model = "t7w-linux", oemLabel = "t7w-re",
    )

    private fun identification(mac: String) = Iap2IdentificationConfig(
        name = "T7W-Linux", modelIdentifier = "t7w-linux", manufacturer = "t7w-re",
        serialNumber = "T7W-" + mac.replace(":", ""), firmwareVersion = "0.1.0", hardwareVersion = "1.0",
        carPlayUsbInterfaceNumber = 3,
    )

    private fun loggingListener(sink: FfmpegMediaSink) = object : AirPlaySessionListener {
        override fun onSessionActive(session: AirPlaySession) {
            log("AirPlay session active; touch enabled")
            sink.window.onTouch = { contacts -> session.sendTouch(contacts) }
        }
        override fun onSessionEnded(session: AirPlaySession) {
            log("AirPlay session ended")
            sink.window.onTouch = { false }
        }
        override fun onTransportError(message: String) = log("AirPlay transport error: $message")
        override fun onDebugLog(message: String) {}
    }

    companion object { private const val LABEL = "T7W-Linux" }
}
