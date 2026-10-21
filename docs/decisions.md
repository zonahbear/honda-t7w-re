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

## Dead ends so far
- Fetching hrvforum / fitfreak / xda from the cloud sandbox: blocked. Read them locally instead.
- Searching the exact firmware strings `1.E200`, `H14M` (beyond one forum hit), `CVH-7738`: nothing public.
- usb.honda.com: aimed at newer vehicles with a "System Updates" export (HYPOTHESIS that it doesn't apply).
