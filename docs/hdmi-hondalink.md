# The HDMI / HondaLink input and touch (2026-10-21)

Jonah pointed out the HDMI input is the **HondaLink** input. The question: does it pass factory-touchscreen taps back to the phone, which would make a dongle-on-HDMI (Path 2) touch-capable?

## Finding: no touch over this input (STRONGLY SUPPORTED → effectively CONFIRMED for this gen)

Two independent sources, one of them Honda, say the factory touchscreen does **not** control the connected device over the HDMI/HondaLink path:

- **Honda's 2016 HR-V press material (S5-class, hondanews):** HDMI playback works only in Park; the connection uses **both USB and HDMI** (the HondaLink iOS cable kit = Lightning Digital AV adapter + Lightning-to-USB + HDMI); and it explicitly "**does not allow control of the connected device through the touchscreen**."
- **cars.com hands-on with the Fit (S21):** quotes Honda's Carl Pulley — "**you have to use the device itself**." The dash can't drive the phone; only the phone drives the dash. Video reverts to the HDMI menu the moment you shift to Drive.

So even though the cable includes USB, this generation's HondaLink does **not** implement AppRadio-style two-way touch. "HondaLink Next Generation" here is (a) phone apps whose output (e.g. planned routes) shows on the screen, plus (b) a plain HDMI A/V playback feature — **not** a mirrored, touch-controlled phone desktop.

## Why the "dongle impersonates the HondaLink phone" idea doesn't open a door here
The coordinator's hypothesis was that touch rides a separate USB/MHL channel a dongle could feed. The evidence says there is **no touch-back channel to impersonate** on this unit: Honda states the screen can't control the source at all. The USB leg of the kit carries power/handshake, not inbound touch. A dongle can't receive touch events the head unit never sends.

(Contrast: Pioneer's aftermarket AppRadio units *did* send touch over USB — "AppRadio Mode". But that's a Pioneer *aftermarket* product. The Pioneer-built *Honda* DA exposes only the HDMI playback feature above. Same supplier, different firmware/feature set.)

## Consequence for the project
- **Path 2 (HDMI AI-box) is confirmed dead for CarPlay-with-touch on this unit.** It could only ever give a Park-only, phone-controlled video window. Keep it filed as "not viable", not "fallback".
- **Path 1 (root the unit + app that forwards the factory touch layer) is the only route to real touch.** All effort goes there (see [root-access.md](root-access.md), [carplay-feasibility.md](carplay-feasibility.md)).

## Open (low-value) checks, if ever curious
- In-car: with HDMI selected and a phone connected, confirm the touchscreen does nothing to the phone (expected, per above). Tier 0.
- Whether the USB leg enumerates anything interesting when HondaLink is active (could be probed later once ADB exists). Low priority.

## Sources added
- **S21** — [cars.com, "How Useful Is the Honda Fit's HDMI Port?"](https://www.cars.com/articles/how-useful-is-the-honda-fits-hdmi-port-1420681239522/): touchscreen can't control the source; "you have to use the device itself"; Park-only.
