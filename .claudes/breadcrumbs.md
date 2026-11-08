# Breadcrumbs

## 2026-11-08 · wired CarPlay client on Linux
- Done: survey (docs/carplay-client-linux.md, research S15-S18, decision D6). LIVI (github.com/f-io/LIVI) is the Linux candidate: native wired CarPlay via usbmux, MFi chip required (I²C bus 2 / power GPIO 21 defaults, or LIVI Link).
- Not done: LIVI build. The cloud sandbox refused running third-party build scripts. Deps install fine on Ubuntu 24.04.
- Don't redo: candidate survey; xcertplay/DiPlay key-file route is rejected (D6).
- Open: does the T7W's iPod support mean an MFi chip on its I²C bus? Can a LIVI/Rust core run on old Android (T7W likely < 9)?
- Next exact action: on CachyOS `cd ~/src/LIVI/native && cargo xtask build-native`, Android phone test, then iPhone test.
