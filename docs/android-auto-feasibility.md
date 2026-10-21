# Android Auto feasibility (preliminary, 2026-10-21)

## How AA works

- The phone renders the UI and sends H.264 video and audio. Wired: the head unit is the **USB host** and switches the phone into Android Open Accessory (AOAP) mode, then speaks the AA protobuf protocol over it, wrapped in TLS. Wireless AA needs Bluetooth RFCOMM bootstrap plus 5 GHz Wi-Fi on the receiver.
- **Open-source receivers exist:** `aasdk` (protocol library), OpenAuto/Crankshaft (Raspberry Pi), and **OpenDroidAuto** (MaKi983), which runs **directly on Honda Connect Android 4.0.4** units with Tegra OMX H.264 decode (S2).
- Caveat: these receivers present a head-unit certificate in the TLS handshake. Google could change phone-side validation. Long-term stability is **UNKNOWN**.

## Approaches

| | Approach | Feasibility | Evidence |
|---|---|---|---|
| A | **Native**: AA receiver app (OpenDroidAuto-style) on the factory OS, phone on factory USB, no dongle | **STRONGLY SUPPORTED for Honda Connect units; UNKNOWN for T7W** | S2 exists for exactly this. It needs app install, AOAP-capable USB host access, OMX decode, and the whitelist permissions. |
| A′ | Same Carlinkit dongle path as CarPlay | Demonstrated on S1's HR-V | One app covers both CarPlay and AA. |
| B | External Pi/SBC running OpenAuto + modified firmware for display | Only if A/A′ fail | Needs firmware. |
| C | HDMI from an AA box | Weak (Park-only video, no touch) | S5. |

## Notes

- Wired AA on API 15 has no `MediaCodec` and no `UsbManager` niceties. OpenDroidAuto already solves both, with native OMX and a custom libusb layer (S1 FINDINGS).
- Wireless AA would need Wi-Fi the T7W may not have. Wi-Fi hardware: **UNKNOWN**. The Carlinkit route sidesteps this because the dongle has its own radio.
