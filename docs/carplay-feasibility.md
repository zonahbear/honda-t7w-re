# Apple CarPlay feasibility (preliminary, 2026-10-21)

## How CarPlay works

- The **iPhone renders the UI** and streams H.264 video plus audio to the "receiver" (the head unit), over USB (iAP2 + NCM) or Wi-Fi (after Bluetooth setup). The receiver sends back touch, knob and mic data.
- The receiver must prove it's a licensed accessory: an Apple **MFi authentication coprocessor** signs a challenge, and the receiver software comes from Apple's licensed CarPlay communication plug-in. **No open-source receiver gets past authentication without an MFi chip** (STRONGLY SUPPORTED: every DIY solution, including S1, uses a dongle that contains the licensed part).
- **Screen mirroring is not CarPlay.** HDMI from an iPhone (S5) mirrors the phone screen with no CarPlay UI and no touch.

## Approaches

| | Approach | Feasibility | Evidence |
|---|---|---|---|
| A | Truly native: CarPlay stack running in the T7W OS, no extra hardware | **Not feasible** (high confidence) | No MFi chip in the unit, no licensed stack, and Android 4.0.4 (if that's what it is) would need the whole receiver ported. |
| A′ | **"Adapter-native"**: CarPlay-to-Android dongle (Carlinkit CPC200 class, USB `1314:1521`) on the factory USB port, plus a **custom app on the factory OS** that shows the H.264 stream, forwards touch, and routes audio/mic | **Demonstrated on a non-US 2016 HR-V** (S1). **UNKNOWN on T7W**, because it depends on T7W being the same Android box and allowing APK installs | S1: video, touch, steering keys, calls, AA and CarPlay all working. Known issues: Wi-Fi stutter, and the app gets killed by the TALK button. |
| B | External receiver box plus modified factory firmware to show its output | Only needed if A′ is impossible. Needs firmware we don't have. | — |
| C | HDMI input from a CarPlay "AI box" | **Weak fallback** | Video only in Park and no touch passthrough (S5, Fit doc, HYPOTHESIS for HR-V). Getting around the Park lock would mean bypassing a safety interlock, which is out of scope. |

## Recommendation (preliminary)

Approach A′ is the only realistic factory-screen path. Pure-native CarPlay isn't happening without Apple's licensed parts (sorry). Everything hinges on one question: **can the US T7W install and run a third-party APK?** See [next-steps.md](next-steps.md).

## Requirements checklist for A′ on T7W

| Need | Status on T7W |
|---|---|
| USB host that can talk to a vendor-class bulk device | UNKNOWN |
| A way to install an app | UNKNOWN (rename trick works on S1's unit) |
| H.264 hardware decode reachable from an app | HYPOTHESIS (Tegra OMX, S2) |
| Permission to keep video on while driving / exclusive audio | UNKNOWN (HondaPermissions whitelist on S1) |
| Mic capture | UNKNOWN (S1 uses the Fujitsu Ten `ecncservice`) |
| Steering-wheel keys | UNKNOWN (S1 uses `ISteeringMenuService`) |
