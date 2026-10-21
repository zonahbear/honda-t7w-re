# Safety & recovery

The HR-V is a daily driver. These rules beat any research goal.

## Hard rules

- No firmware flashing, bootloader changes, or "Display Repro" runs without Jonah's explicit approval for that specific action.
- Don't bypass the Park/speed video lock or any other safety interlock. If an app needs to show video while driving, use the head unit's own permission mechanism (on S1's unit, that's the HondaPermissions whitelist), and only after Jonah has decided.
- Keep the backup camera, factory audio, and B-CAN/F-CAN communication working at all times.
- Treat every downloaded file as untrusted. Never run vendor `.exe` updaters, and analyze offline only.
- Never assume an update can be reversed.

## Risk tiers for in-car experiments

| Tier | Examples | Approval |
|---|---|---|
| 0, observe | Photograph diag/settings screens, read versions, look for menus | None needed |
| 1, passive USB | Plug in a USB stick with media or a file the unit only lists (no install) | None needed; note results |
| 2, app-level, reversible | Install a known third-party APK (if the unit allows it), change an app whitelist entry | **Jonah approves each time.** Uninstall path must be known first. |
| 3, system-level | Display Repro, OTA/update files, recovery mode, anything touching boot or system partitions | **Explicit approval + written recovery plan + spare unit strongly advised** |
| 4, hardware | Removal, UART/JTAG, flash reading | Future phase; needs its own plan |

## Recovery knowledge

- **UNKNOWN for T7W:** recovery mode, how to restore a factory image, whether a bad app can boot-loop the UI.
- Practical fallback: used 2016-18 HR-V EX units (39100-T7W-A013-M1, S11) sell on eBay for well under the cost of a dealer replacement. Buying a **bench spare** before any Tier 3 work is strongly recommended, and it would also enable Tier 4 work without touching the car.
- Radio anti-theft code: record it (it's printed on a card or available from Honda) **before** any work that might cut power to the unit. Don't store it in this repo.
