# AE2: Back to Life

[![Code: LGPL-3.0](https://img.shields.io/badge/code-LGPL--3.0-blue.svg?style=flat-square)](LICENSE)
[![Assets: CC BY-NC-SA 3.0](https://img.shields.io/badge/assets-CC%20BY--NC--SA%203.0-yellow.svg?style=flat-square)](licenses/CC-BY-NC-SA-3.0.txt)
[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-green.svg?style=flat-square)](#)
![Made with AI (Claude)](https://img.shields.io/badge/made%20with-AI%20(Claude)-8A2BE2?style=flat-square)

An unofficial port of **Applied Energistics 2 19.2** (Minecraft 1.21.1) to **Minecraft 1.12.2** (Forge): the same
items, blocks, ids, tags, recipes, textures and features as the modern AE2, on 1.12.2.

Not made or supported by the [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
team — please report problems with this port here.

**Beta:** playable, but young. Back up your worlds and report problems in the
[issue tracker](https://github.com/Xponer2/ae2-back-to-life/issues).

Downloads: [GitHub releases](https://github.com/Xponer2/ae2-back-to-life/releases).

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer.
- Optional: JEI, The One Probe or Hwyla, Patchouli (in-game guide).
- Can't be used together with AE2 Unofficial Extended Life / AE2 rv6 or their addons.

## Building

```bash
./gradlew build
```

Needs a JDK 21 to run Gradle.

## License

- Code: [LGPL-3.0-or-later](LICENSE) (with the [GPL-3.0](licenses/GPL-3.0.txt) it builds on); the API
  (`appeng.api`): [MIT](licenses/MIT-AE2-API.txt).
- Textures and models: [CC BY-NC-SA 3.0](licenses/CC-BY-NC-SA-3.0.txt), © Ridanisaurus Rid and AlgorithmX2 et al.
  — **non-commercial**.
- Text and translations: no restriction (CC0).
- Bundled DataFixerUpper: [MIT](licenses/MIT-DataFixerUpper.txt), © Microsoft. Guide click sound:
  [CC BY 4.0](licenses/CC-BY-4.0.txt), EminYILDIRIM. `appeng/thirdparty/codechicken`: LGPL-2.1-or-later, © covers1624.

This is a modified version of Applied Energistics 2 by AlgorithmX2 and the AE2 contributors; the changes are in the
git history.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
