# Apple CarPlay on the T7W, with touch (2026-10-21)

**Goal:** CarPlay on the factory 7" screen, operable by the factory touchscreen — not a Park-only mirrored video feed.

## How CarPlay works (and where touch comes from)
- The **iPhone renders the UI** and streams H.264 video + audio to the "receiver". The receiver sends **touch coordinates, knob/button and mic** back to the phone. Transport is USB (iAP2 + NCM) or Wi-Fi after a Bluetooth handshake.
- The receiver must authenticate as a licensed accessory via an Apple **MFi coprocessor** + Apple's CarPlay plug-in. **No open-source receiver passes auth without that chip** (STRONGLY SUPPORTED — every DIY build, S1 included, puts the licensed part in a dongle).
- **Touch is the crux of this project.** CarPlay is useless without the screen sending coordinates back. Mirroring an iPhone over HDMI (S5) gives video only, no CarPlay UI, no touch.

## The two viable architectures

### Path 1 — Integrated / native (root the unit, app drives the factory touch)
A CarPlay-to-Android **dongle** (Carlinkit CPC200 class, USB `1314:1521`) does the Apple auth and phone link; a **custom app on the factory OS** displays the dongle's H.264 output and forwards the factory touchscreen's coordinates back to the dongle. The factory panel stays the input device, so **touch works by construction** — this is exactly what S1 achieved on the (Fujitsu Ten) Brazilian HR-V: video, touch, steering keys, calls.

What it needs on the **Pioneer US T7W**:
| Requirement | Status |
|---|---|
| It's Android and runs third-party code | STRONGLY SUPPORTED it's Android (S17/S18 Pioneer siblings); running our code needs **root/ADB** (root-access.md), since the US UI exposes no app install (hardware.md UI survey) |
| USB host can talk to a vendor-class bulk device | UNKNOWN (demonstrated on S1's unit) |
| H.264 decode reachable from an app | HYPOTHESIS (sibling Android units have HW decode; the Pioneer SoC is UNKNOWN) |
| Touch events readable + forwardable by an app | HYPOTHESIS — the native integration is the whole point; S1 did it on its unit |
| Keep video while moving / audio routing / mic / wheel keys | UNKNOWN on Pioneer (S1 solved these via the Fujitsu Ten framework, which **won't be present** on a Pioneer unit — this is the biggest port risk) |

**Verdict:** the only path that gives real CarPlay-with-touch on the factory screen. Gated on root (root-access.md) and on re-solving the integration for Pioneer rather than Fujitsu Ten.

### Path 2 — Dongle into the HDMI / HondaLink input  (CONFIRMED not viable for touch — see [hdmi-hondalink.md](hdmi-hondalink.md))
The HDMI input is the **HondaLink** input (Jonah). It was worth checking whether HondaLink passes factory-touch back to the phone over the USB leg of its cable kit — if so, a dongle could impersonate that endpoint.
- **It does not.** Honda's own 2016 HR-V material states the input "does not allow control of the connected device through the touchscreen," and a Fit hands-on quotes Honda: "you have to use the device itself" (S21). It's Park-locked video + phone-side control only. There is **no touch-back channel to feed**.
- Making it work would mean inventing a channel the firmware doesn't expose, plus defeating the Park lock (a safety interlock — **out of scope**, recovery.md).
- **Verdict:** dead for CarPlay-with-touch on this unit. Not even a real fallback.

> The aftermarket Pioneer AppRadio *did* carry touch over USB, but that's a Pioneer consumer product. The Pioneer-built *Honda* DA ships only the display-only HDMI feature above.

## Recommendation
Pursue **Path 1**. Concretely: get **ADB/root** (root-access.md E1→E2), confirm Android version + that an app can read touch and host the dongle, then port an S1-style receiver app to the Pioneer framework. Path 2 is kept only as a fallback and is likely blocked by the HDMI input being display-only.

**The gating question is no longer "can it run an app" in the abstract — it's "can we get root and read/forward touch on this Pioneer unit."**

## Confidence
- CarPlay needs a licensed dongle for auth: **0.95**.
- Path 2 can't give touch on this unit: **0.9** (Honda's own HR-V material + a Fit hands-on both say the touchscreen can't control the HDMI/HondaLink source — S21, hdmi-hondalink.md).
- Path 1 is technically achievable *if* root lands: **0.6** (Android is likely; the Pioneer integration is unproven and is real work).
