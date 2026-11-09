// Desktop replacement for DiPlay's transport/IphoneUsbHost.kt. Keeps the two types the rest
// of DiPlay's transport layer uses (Iap2UsbSession, IphoneUsbException) with the same API,
// backed by libusb through usb4java instead of android.hardware.usb. Device discovery and
// configuration switching live in the desktop app (t7w.desktop.LinuxIphoneUsb).
// License: GPL-3.0-or-later (links DiPlay, GPL-3.0).
package com.shilapi.xcertplay.transport

import org.usb4java.DeviceHandle
import org.usb4java.LibUsb
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A blocking, full-duplex USBMUX pipe. A null [read] result means only that its timeout elapsed. */
class Iap2UsbSession internal constructor(
    private val handle: DeviceHandle,
    private val interfaceNumber: Int,
    private val outEndpoint: Byte,
    private val inEndpoint: Byte,
    private val onClose: () -> Unit = {},
) : Closeable {
    private val readLock = Any()
    private val writeLock = Any()
    @Volatile private var closed = false
    private val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(USBMUX_READ_CHUNK_BYTES)
    private val transferred = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    private val writeTransferred = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()

    fun write(data: ByteArray, timeoutMillis: Int) = synchronized(writeLock) {
        checkOpen()
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        if (data.isEmpty()) return@synchronized
        val buffer = ByteBuffer.allocateDirect(data.size).put(data).also { it.flip() }
        writeTransferred.put(0, 0)
        val rc = LibUsb.bulkTransfer(handle, outEndpoint, buffer, writeTransferred, timeoutMillis.toLong())
        if (rc != LibUsb.SUCCESS || writeTransferred.get(0) != data.size) {
            throw IphoneUsbException.DeviceUnavailable(
                "USBMUX write transferred ${writeTransferred.get(0)} of ${data.size} bytes (${LibUsb.errorName(rc)})",
            )
        }
    }

    /** Returns null only when nothing arrives before [timeoutMillis]. */
    fun read(timeoutMillis: Long): ByteArray? = synchronized(readLock) {
        checkOpen()
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        readBuffer.clear()
        transferred.put(0, 0)
        val rc = LibUsb.bulkTransfer(handle, inEndpoint, readBuffer, transferred, timeoutMillis)
        val count = transferred.get(0)
        // libusb reports partial data with a timeout; keep it so the USBMUX stream stays intact.
        if (rc == LibUsb.ERROR_TIMEOUT && count == 0) return@synchronized null
        if (rc != LibUsb.SUCCESS && rc != LibUsb.ERROR_TIMEOUT) {
            throw IphoneUsbException.DeviceUnavailable("USBMUX read failed (${LibUsb.errorName(rc)})")
        }
        ByteArray(count).also { readBuffer.get(it, 0, count) }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { LibUsb.releaseInterface(handle, interfaceNumber) }
        onClose()
    }

    private fun checkOpen() {
        if (closed) throw IphoneUsbException.DeviceUnavailable("USBMUX session is closed")
    }

    companion object {
        const val USBMUX_READ_CHUNK_BYTES = 64 * 1024
    }
}

/** USB bring-up failures that precede iAP2 and are distinct from MFi I2C failures. */
sealed class IphoneUsbException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class PermissionDenied(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)
    class DeviceUnavailable(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)
    class TimedOut(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)
    class Protocol(message: String) : IphoneUsbException(message)
}
