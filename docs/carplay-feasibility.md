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

### Path 2 — Dongle into the HDMI input
A wireless CarPlay "AI box" into the factory **HDMI input**. The open question is **touch passthrough**: the box needs the screen's coordinates, and factory HDMI inputs are usually **display-only**.
- The 2015 Fit HDMI input is documented as **video-only, no touch, and Park-locked** (S5; HYPOTHESIS the HR-V matches). If the HR-V behaves the same, **Path 2 cannot deliver touch**, full stop — you'd be poking a phone UI you can't touch, only in Park. Useless for CarPlay.
- Making it work would require a touch back-channel the factory HDMI path doesn't expose, plus defeating the Park lock (a safety interlock — **out of scope**, recovery.md).
- **Verdict:** very unlikely to meet the "with touch" bar on this unit. Fallback only, and probably a dead one here.

> Note: the common "CarPlay AI box" that *does* have touch is sold for head units that **already run Android and expose their touch layer to apps** — i.e. it's really Path 1 in a box. On a locked Pioneer unit with a display-only HDMI in, that doesn't apply.

## Recommendation
Pursue **Path 1**. Concretely: get **ADB/root** (root-access.md E1→E2), confirm Android version + that an app can read touch and host the dongle, then port an S1-style receiver app to the Pioneer framework. Path 2 is kept only as a fallback and is likely blocked by the HDMI input being display-only.

**The gating question is no longer "can it run an app" in the abstract — it's "can we get root and read/forward touch on this Pioneer unit."**

## Confidence
- CarPlay needs a licensed dongle for auth: **0.95**.
- Path 2 can't give touch on this unit as-is: **0.7** (rests on the HR-V HDMI matching the Fit's documented display-only behaviour — needs an in-car check).
- Path 1 is technically achievable *if* root lands: **0.6** (Android is likely; the Pioneer integration is unproven and is real work).
