# The HDMI / HondaLink input and touch (2026-10-21)

Jonah pointed out the HDMI input is the **HondaLink** input. The question: does it pass factory-touchscreen taps back to the phone, which would make a dongle-on-HDMI (Path 2) touch-capable?

## Finding: no touch over this input (STRONGLY SUPPORTED → effectively CONFIRMED for this gen)

Two independent sources, one of them Honda, say the factory touchscreen does **not** control the connected device over the HDMI/HondaLink path:

- **Honda's 2016 HR-V press material (S5-class, hondanews):** HDMI playback works only in Park; the connection uses **both USB and HDMI** (the HondaLink iOS cable kit = Lightning Digital AV adapter + Lightning-to-USB + HDMI); and it explicitly "**does not allow control of the connected device through the touchscreen**."
- **cars.com hands-on with the Fit (S21):** quotes Honda's Carl Pulley — "**you have to use the device itself**." The dash can't drive the phone; only the phone drives the dash. Video reverts to the HDMI menu the moment you shift to Drive.

So even though the cable includes USB, this generation's HondaLink does **not** implement AppRadio-style two-way touch. "HondaLink Next Generation" here is (a) phone apps whose output (e.g. planned routes) shows on the screen, plus (b) a plain HDMI A/V playback feature — **not** a mirrored, touch-controlled phone desktop.

## Does the "dongle impersonates the HondaLink phone" idea open a door?
For a **plain HDMI AI-box**, no: Honda says the screen can't control the HDMI *playback* source, so there's nothing to feed. For a dongle that instead **impersonates the HondaLink phone** and makes the unit enter app-projection mode, the honest answer is **UNKNOWN** — because Pioneer's platform *does* carry touch over USB in "AppRadio Mode", and Honda's firmware is built on it. Honda may have stripped that channel (Honda's doc, Reading A) or merely gated it to HondaLink-SDK apps (Reading B). Only a **USB capture** of a live HondaLink session decides it. Even in the best case the Park video lock makes it Park-only. Full analysis in [hdmi-dongle-design.md](hdmi-dongle-design.md).

(Contrast: Pioneer's aftermarket AppRadio units *did* send touch over USB — "AppRadio Mode", unlocked for all apps by the "AppRadio Unchained" mod. That's a Pioneer *aftermarket* product; the Pioneer-built *Honda* DA exposes only the HDMI playback feature to consumers. Same platform, different firmware policy — which is exactly why the touch channel might still be latent.)

## A more sophisticated variant: impersonate the HondaLink phone?
A plain HDMI AI-box is display-only here (above). A cleverer idea (Jonah) is a dongle that impersonates a HondaLink *phone* so the unit enters app-projection mode and sends touch the way Pioneer's AppRadio Mode does. Whether this unit's firmware actually emits touch on the USB leg is **UNKNOWN** and only a USB capture settles it; and even in the best case the **Park video lock** makes it Park-only. Full analysis: [hdmi-dongle-design.md](hdmi-dongle-design.md).

## Consequence for the project
- **Path 2 (HDMI AI-box) is confirmed dead for CarPlay-with-touch on this unit.** It could only ever give a Park-only, phone-controlled video window. Keep it filed as "not viable", not "fallback".
- **Path 1 (root the unit + app that forwards the factory touch layer) is the only route to real touch.** All effort goes there (see [root-access.md](root-access.md), [carplay-feasibility.md](carplay-feasibility.md)).

## Open (low-value) checks, if ever curious
- In-car: with HDMI selected and a phone connected, confirm the touchscreen does nothing to the phone (expected, per above). Tier 0.
- Whether the USB leg enumerates anything interesting when HondaLink is active (could be probed later once ADB exists). Low priority.

## Sources added
- **S21** — [cars.com, "How Useful Is the Honda Fit's HDMI Port?"](https://www.cars.com/articles/how-useful-is-the-honda-fits-hdmi-port-1420681239522/): touchscreen can't control the source; "you have to use the device itself"; Park-only.
