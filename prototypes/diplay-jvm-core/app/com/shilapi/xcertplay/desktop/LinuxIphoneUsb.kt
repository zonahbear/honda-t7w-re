// Linux USB host for the iPhone, replacing DiPlay's Android transport/IphoneUsbHost.kt discovery
// and CarPlayController.openDataPaths/openNcm with libusb (usb4java). Our code; GPL-3.0-or-later.
//
// Flow, from DiPlay's source (transport/IphoneUsbHost.kt, IphoneCarPlayConfiguration.kt):
//   1. find the Apple device (VID 0x05ac);
//   2. send the CarPlay vendor control request (bmRequestType 0xc0, bRequest 0x52, wIndex 0x0004);
//   3. the iPhone re-enumerates exposing a config with the USBMUX interface (ff/fe/02) + CDC-NCM;
//   4. claim the USBMUX interface and hand its bulk pair to Iap2UsbSession.
// The kernel's cdc_ncm driver binds the NCM interface itself and creates a netdev, so unlike
// Android we do NOT run a userspace NCM bridge: the AirPlay server just binds the netdev's
// link-local address. RUNTIME-UNTESTED: no iPhone was available in this environment.
package com.shilapi.xcertplay.desktop

import com.shilapi.xcertplay.transport.Iap2UsbSession
import com.shilapi.xcertplay.transport.IphoneUsbException
import org.usb4java.ConfigDescriptor
import org.usb4java.Context
import org.usb4java.Device
import org.usb4java.DeviceHandle
import org.usb4java.DeviceList
import org.usb4java.LibUsb

object LinuxIphoneUsb {
    private const val APPLE_VID = 0x05ac
    private const val USBMUX_CLASS = 0xff
    private const val USBMUX_SUBCLASS = 0xfe
    private const val USBMUX_PROTOCOL = 0x02
    private const val PREFERRED_OUT = 0x04
    private const val PREFERRED_IN = 0x85.toByte()
    private const val CARPLAY_REQUEST = 0x52
    private const val CARPLAY_INDEX = 0x0004
    private const val VENDOR_DEVICE_IN = 0xc0

    val context: Context = Context().also {
        if (LibUsb.init(it) != LibUsb.SUCCESS) throw IphoneUsbException.DeviceUnavailable("libusb init failed")
    }

    /** Sends the CarPlay vendor request; the iPhone re-enumerates after this returns. */
    fun requestCarPlayMode(log: (String) -> Unit = {}) {
        val device = findApple() ?: throw IphoneUsbException.DeviceUnavailable("No Apple USB device (VID 0x05ac) found")
        val handle = DeviceHandle()
        if (LibUsb.open(device, handle) != LibUsb.SUCCESS)
            throw IphoneUsbException.PermissionDenied("Could not open the iPhone (is it a udev-permitted device?)")
        try {
            val buf = java.nio.ByteBuffer.allocateDirect(1)
            val read = LibUsb.controlTransfer(
                handle, VENDOR_DEVICE_IN.toByte(), CARPLAY_REQUEST.toByte(), 0, CARPLAY_INDEX.toShort(), buf, 1000,
            )
            // iOS rejects this when locked (DiPlay #100/#455/#509); a negative/short result is the signal.
            if (read < 0) throw IphoneUsbException.Protocol(
                "CarPlay mode request rejected (${LibUsb.errorName(read)}); unlock the iPhone and tap Trust",
            )
            log("CarPlay vendor request accepted; iPhone will re-enumerate")
        } finally {
            LibUsb.close(handle)
        }
    }

    /** Opens the USBMUX bulk pipe on the re-enumerated iPhone. Poll after [requestCarPlayMode]. */
    fun openUsbMux(log: (String) -> Unit = {}): Iap2UsbSession {
        val device = findUsbMuxDevice() ?: throw IphoneUsbException.Protocol(
            "iPhone exposes no USBMUX CarPlay configuration yet (still re-enumerating?)",
        )
        val handle = DeviceHandle()
        if (LibUsb.open(device, handle) != LibUsb.SUCCESS)
            throw IphoneUsbException.PermissionDenied("Could not reopen the iPhone after re-enumeration")
        val iface = usbMuxInterface(device)
            ?: run { LibUsb.close(handle); throw IphoneUsbException.Protocol("No USBMUX interface on the iPhone") }
        LibUsb.detachKernelDriver(handle, iface) // ignore result: usually no driver on this iface
        if (LibUsb.claimInterface(handle, iface) != LibUsb.SUCCESS) {
            LibUsb.close(handle)
            throw IphoneUsbException.DeviceUnavailable("Could not claim USBMUX interface $iface; stop usbmuxd")
        }
        log("claimed USBMUX interface $iface (ep out=0x${PREFERRED_OUT.toString(16)} in=0x85)")
        return Iap2UsbSession(handle, iface, PREFERRED_OUT.toByte(), PREFERRED_IN) { LibUsb.close(handle) }
    }

    private fun findApple(): Device? = scan { true }

    private fun findUsbMuxDevice(): Device? = scan { usbMuxInterface(it) != null }

    private fun usbMuxInterface(device: Device): Int? {
        val config = ConfigDescriptor()
        if (LibUsb.getActiveConfigDescriptor(device, config) != LibUsb.SUCCESS) return null
        try {
            for (iface in config.iface()) for (alt in iface.altsetting()) {
                if (alt.bInterfaceClass().toInt() and 0xff == USBMUX_CLASS &&
                    alt.bInterfaceSubClass().toInt() and 0xff == USBMUX_SUBCLASS &&
                    alt.bInterfaceProtocol().toInt() and 0xff == USBMUX_PROTOCOL
                ) return alt.bInterfaceNumber().toInt() and 0xff
            }
            return null
        } finally {
            LibUsb.freeConfigDescriptor(config)
        }
    }

    private fun scan(predicate: (Device) -> Boolean): Device? {
        val list = DeviceList()
        if (LibUsb.getDeviceList(context, list) < 0) return null
        try {
            for (device in list) {
                val desc = org.usb4java.DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, desc) != LibUsb.SUCCESS) continue
                if (desc.idVendor() == APPLE_VID.toShort() && predicate(device)) {
                    LibUsb.refDevice(device)
                    return device
                }
            }
            return null
        } finally {
            LibUsb.freeDeviceList(list, true)
        }
    }
}
