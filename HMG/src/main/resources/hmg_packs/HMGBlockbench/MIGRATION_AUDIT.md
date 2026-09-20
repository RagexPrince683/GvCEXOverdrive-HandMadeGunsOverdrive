# HMG Blockbench organization audit

Static organization date: 2026-09-19. Minecraft was not launched.

| Item | Presentation | Gameplay | Dependencies |
| --- | --- | --- | --- |
| Blockbench AKM (`AKM_Blockbench`) | Existing `cod4_ak.bbmodel` | Existing HMG Blockbench-AKM definition; recipe copied from legacy `AKM` | Existing `AKM.png` item icon and `scope.png` overlay |

The definition and model moved from `GVCguns` into this pack. The icon and scope
remain in GVCguns because legacy content still references them; byte-identical
pack-local copies make this pack self-contained. The item now appears only in the
player-facing `HMG Blockbench Guns` tab.
