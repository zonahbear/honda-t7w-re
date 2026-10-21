# Design feasibility: a dongle that impersonates a HondaLink phone (2026-10-21)

Jonah's idea: the HondaLink input uses **HDMI (video) + USB (data)**. Build a dongle that presents itself to the head unit as a HondaLink-capable phone, so a CarPlay source rides in on that input — and crucially, so the head unit sends **touch** back the way it would to a real HondaLink phone.

This note analyzes it honestly. Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN.

## 1. What the HondaLink interface is, on this generation

- **Architecture (STRONGLY SUPPORTED):** two legs. Video over **HDMI** (iPhone via a Lightning Digital AV adapter); data/control over **USB** (Lightning-to-USB). Honda's cable kit bundles exactly these (S5, S21).
- **Platform lineage (STRONGLY SUPPORTED):** Honda's "HondaLink Next Generation" + the Pioneer-built head unit (S16) is Pioneer's **"AppRadio Mode" / Advanced App Mode** family. On iPhone that control leg is **iAP/iAP2** over USB; the head unit runs approved apps via the HondaLink "Launcher" (S5).
- **The precedent that makes the idea non-crazy (STRONGLY SUPPORTED):** Pioneer's *aftermarket* AppRadio units **did** send touchscreen coordinates to the phone over USB — that's what AppRadio Mode is. The community mod **"AppRadio Unchained"** existed precisely because, by default, the head unit only delivered touch to apps built with Pioneer's SDK; the mod tricked it into mirroring/controlling *any* app. So on Pioneer's platform the **touch channel is real and rides the USB control leg.**

## 2. The crux: is touch actually on the wire during a HondaLink session on THIS unit?

This is the whole question, and it is **UNKNOWN** from public info. Two readings of the evidence, both defensible:

- **Reading A — touch is absent/stripped (what Honda's doc literally says).** Honda's 2016 HR-V material: the input "does not allow control of the connected device through the touchscreen"; a Fit hands-on quotes Honda, "you have to use the device itself" (S21, [hdmi-hondalink.md](hdmi-hondalink.md)). If Honda disabled the AppRadio touch channel in firmware, **no dongle can recover touch the head unit never transmits.** → back to rooting.
- **Reading B — touch exists but is gated.** Honda's consumer sentence may describe the **HDMI A/V *playback* source** (watch a video), not a latent AppRadio control channel that the firmware still speaks to HondaLink-SDK apps. On Pioneer hardware the channel physically exists; Honda may simply restrict *which* app receives it. If so, a dongle that speaks the HondaLink/AppRadio control handshake **might** receive touch — an "AppRadio Unchained" for the Honda variant.

**Nothing in hand resolves A vs B.** It can only be settled by **observing the USB traffic of a real HondaLink session on this exact unit.** That is the decisive experiment (section 4).

## 3. The two blockers, and why even Reading B is a hard road

Even if touch turns out to be on the wire (Reading B), the HDMI path carries **two** independent problems:

1. **Touch channel (UNKNOWN):** must exist and be reverse-engineerable (section 2).
2. **The Park video lock (CONFIRMED, and fatal on its own for driving use):** this input shows video **only in Park**; shift to Drive and the video cuts to the HDMI menu (S21). CarPlay is something you want *while driving*. Defeating the lock means bypassing a safety interlock tied to vehicle state — **out of scope** ([recovery.md](recovery.md)). So even a perfect touch solution yields **Park-only CarPlay**, which is close to useless.

And an MFi wrinkle: to impersonate an **iPhone** to the head unit over iAP2, the dongle must itself pass Apple's iAP2 authentication (an Apple-side MFi requirement) *while also* being a CarPlay receiver — i.e. a two-sided bridge that is MFi-licensed on one face. Non-trivial, and likely needs licensed silicon on both ends.

> Net: the HDMI/HondaLink dongle has to beat **three** things (touch channel + Park lock + dual-auth bridge). The root path (Path 1) beats **one** (get root), and gives full-screen, always-on, touch-native CarPlay. That asymmetry is why Path 1 is the recommendation.

## 4. The decisive experiment (cheap, settles section 2)

**Capture the USB traffic of a live HondaLink session on this unit.** Two ways:
- **Hardware sniffer (no root needed):** a USB protocol analyzer (or a cheap USB-passthrough tap / a Linux box with `usbmon` wired inline via a USB-A↔A breakout) between phone and head unit, with the HondaLink cable kit + a compatible iPhone. Watch for an inbound channel of **small, high-rate packets correlated with screen taps** (that's touch). If present → Reading B, touch is recoverable. If the only inbound data is control/metadata and no tap-correlated stream → Reading A, dongle idea is dead.
- **After root (free, reuses Path 1 work):** once ADB/root exists, read the head unit's iAP/HondaLink service and logs to see whether it ever emits touch events for this input. This folds into Path 1 anyway.

Either way, **the touch question is answered by a capture, not by more web search.** Until then the dongle's feasibility is UNKNOWN, leaning unlikely (Honda's own doc is Reading A, and the Park lock remains regardless).

## 5. IF (and only if) touch is on the wire — what the dongle would need to do

Sketch, conditional on section 4 showing touch frames:
1. **Enumerate as the phone endpoint** the head unit expects (Apple device doing iAP2 for the iPhone path; this is where the MFi/auth problem bites) and complete the HondaLink/AppRadio-Mode handshake so the unit enters "app projection", not plain HDMI playback.
2. **Feed HDMI video** from the CarPlay source into the input (the CarPlay dongle renders the UI; this dongle muxes it to HDMI).
3. **Receive the touch stream** on the USB control leg, parse its framing (reverse-engineered from the capture), and **map coordinates** from the head unit's 800×480 panel space into the CarPlay source's input space.
4. **Relay** those mapped touches into the CarPlay dongle (as its "screen" touch input).
5. Handle mode/focus, the Park-lock behavior (which it cannot defeat in scope), audio routing, and session keep-alive.

This is essentially building a **HondaLink-to-CarPlay protocol bridge** — a real firmware project on custom hardware, justified only if the capture proves touch exists *and* the Park limitation is acceptable (it probably isn't).

## 6. Recommendation (honest)

- **Don't design or buy hardware yet.** The idea rests entirely on an UNKNOWN (touch on the wire) that a **USB capture** settles cheaply, and even the best case is Park-only.
- **Do the capture** if you want a definitive yes/no — ideally folded into the root work, which reads the same answer from inside the unit.
- **Primary effort stays Path 1 (root).** It gives always-on, full-screen, touch-native CarPlay and sidesteps both the Park lock and the dual-auth bridge. The HondaLink-dongle route, best case, is a strictly worse outcome for far more work.

## Confidence
- HondaLink = HDMI video + USB (iAP2) control, Pioneer AppRadio-Mode lineage: **0.8**.
- Pioneer's platform carries touch over USB to SDK apps (AppRadio precedent): **0.85**.
- Whether *this Honda firmware* puts touch on the wire at all: **UNKNOWN (~0.3 it does)** — Honda's doc says no; the platform *could*. Only a capture decides.
- Even if touch works, HDMI CarPlay is Park-only without defeating a safety interlock: **0.9**.
- Path 1 (root) is the better route regardless: **0.85**.
