# Decisions & rationale

Format: date, decision, why, alternatives considered.

## 2026-10-21 D1: Treat "US T7W = Honda Connect Android box" as the lead hypothesis, not a fact
- **Why:** the only HR-V-specific technical evidence (S1, S2) is from a 2016 HR-V of probably non-US market, which runs Fujitsu Ten / Tegra / Android 4.0.4. The brief explicitly says not to assume US equals international.
- **Consequence:** the first experiments are cheap discriminators (next-steps #1), not tool-building for an assumed format.

## 2026-10-21 D2: Pure-native CarPlay (no extra hardware) rejected as a goal
- **Why:** CarPlay receivers need an Apple MFi auth coprocessor plus the licensed stack, and no open implementation gets around that. A dongle that holds the licensed part, driven by our own app on the factory OS ("A′"), is the closest-to-native option that keeps the factory screen.
- **Alternatives:** HDMI box (C), which is limited to Park-only video with no touch, so it stays a fallback only. External receiver plus modified firmware (B) needs firmware we don't have.

## 2026-10-21 D3: Android Auto evaluated separately; true native AA kept alive
- **Why:** AA has open receivers (aasdk / OpenDroidAuto) that already run on Honda Connect. No dongle needed if the T7W allows apps.

## 2026-10-21 D4: Tools stay stdlib-only and read-only
- **Why:** they have to run before any setup, on untrusted inputs. `inspect_firmware.py` does only hashes, signatures, entropy, strings and ZIP listing. `extract_firmware.py` handles only ZIP/tar/gzip, with traversal, link and size guards. Anything broader goes to binwalk.
- **Rejected:** format-specific parsers for T7W packages. No sample exists, and the brief forbids placeholder tools.

## 2026-10-21 D5: Repo built in the cloud sandbox, mirrored to project files
- **Why:** no GitHub repo existed at first. Update 2026-10-21: Jonah created `zonahbear/honda-t7w-re` and this history was pushed there. GitHub is now canonical; the project-files copy is a stale snapshot.

## 2026-10-21 D6: US EX unit confirmed Pioneer; prior art re-pointed
- **What:** Honda's parts catalog (S16) marks the 2016 HR-V EX display/audio unit `39100-T7W-A01…` as (PIONEER). This is a different supplier from S1's Fujitsu Ten MY15ADA (a non-US HR-V).
- **Consequence:** S1/S2 (Fujitsu Ten, Tegra, Android 4.0.4) drop from "lead" to "weaker analogy". The real analogues are the US Pioneer Honda Android units — 2016 Pilot, 2017 Civic — which are rooted (S17, S18). The Pioneer unit lacks the Fujitsu Ten framework S1 relied on, so that integration must be re-solved.

## 2026-10-21 D7: Focus pivoted to root access; goal is CarPlay-with-touch (Path 1)
- **Why (Jonah):** the end goal is CarPlay on the factory screen *with touch*. That needs the factory panel to drive the session, which needs our code on the unit, which needs root (no app-install UI exists — hardware.md survey).
- **Chosen:** Path 1 (root → Carlinkit-class dongle for Apple auth + a custom app that shows H.264 and forwards factory touch). Path 2 (HDMI AI-box) is fallback and probably can't deliver touch on this unit (HDMI input display-only on the sibling Fit, S5).
- **Rejected for now:** pure-native CarPlay (no dongle) — still needs Apple MFi + licensed stack (D2 stands).
- **Rule kept:** no flashing / exploit execution / recovery writes without Jonah's approval. Read-only ADB is fine; running DirtyCow is not, until approved.

## 2026-10-21 D8: HDMI/HondaLink path is display-only — Path 2 closed
- **Why:** Jonah noted the HDMI input is the HondaLink input. Checked whether HondaLink passes factory touch back over USB (which would revive Path 2). Honda's own HR-V material and a Fit hands-on both say the touchscreen cannot control the HDMI source (S21). No touch-back channel to impersonate.
- **Consequence:** Path 2 (HDMI AI-box) cannot give CarPlay-with-touch here. Path 1 (root) is the sole route. See docs/hdmi-hondalink.md.

## Dead ends so far
- Fetching hrvforum / fitfreak / xda from the cloud sandbox: blocked. Read them locally instead.
- Searching the exact firmware strings `1.E200`, `H14M` (beyond one forum hit), `CVH-7738`: nothing public.
- usb.honda.com: aimed at newer vehicles with a "System Updates" export (HYPOTHESIS that it doesn't apply).
