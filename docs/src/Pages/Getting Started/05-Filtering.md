---
title: 'Filtering'
---

As an additional layer on top of MQTT subscriptions, Hydra-MQTT enables filtering published and received 
(via subscription) messages using Regular Expressions (RegEx).
[RegEx Tester](https://www.regextester.com/) is a utility which allows you to test expressions on a block of topics.
You can copy and paste the entire `Topic Pool` below into the `Test String` field, and test your regular expressions.
Be sure to click on the `Flags` button and enable `multiline` mode.
When it highlights an __entire__ line, it is considered a match.

For examples in this section, the following topics with the structure
(`Enterprise`/`Site`/`Area`/`Line`/`Cell`/`Variable`) are used:

```bash title='Topic Pool'
DocBrownIndustries/HillValley/ClockTower/LabAssemblyLine/DeLoreanStation/FluxCapacitorCharge
DocBrownIndustries/HillValley/ClockTower/LabAssemblyLine/DeLoreanStation/PowerLevel
DocBrownIndustries/HillValley/TechOps/LabAssemblyLine/DeLoreanStation/PowerLevel
GalacticEmpire/DeathStar/TechOps/WingAssemblyLine/TIEFighterDock/PowerLevel
GalacticEmpire/DeathStar/TractorBeam/WingAssemblyLine/TIEFighterDock/LaserCannonPower
GalacticEmpire/DeathStar/TractorBeam/WingAssemblyLine/TIEFighterDock/PowerLevel
GalacticEmpire/Tatooine/JawaWorkshop/AssemblyLine/DroidRefitUnit/SalvageRate
InGenLabs/IslaNublar/TyrannosaurPit/AssemblyLine/TrexUnit/MeatConsumptionRate
InGenLabs/JurassicPark/RaptorPaddock/DNAReplicationLine/CloningChamber/DinoGrowthRate
MinistryOfMagic/Hogwarts/DarkArts/WandCraftingLine/ElderWandStation/MagicCoreOutput
MinistryOfMagic/Hogwarts/DarkArts/WandCraftingLine/ElderWandStation/PowerLevel
MinistryOfMagic/Hogwarts/Quidditch/AssemblyLine/BroomstickStation/SmoothnessFactor
MinistryOfMagic/Hogwarts/TechOps/WandCraftingLine/ElderWandStation/PowerLevel
MordorIndustries/BaradDur/OrcForge/ringSmithingLine/MountDoomKiln/LavaFlowRate
MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel1
MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel2
MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel3
MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel42
MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel897
MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
MordorIndustries/Shire/SpyOps/AssemblyLine/UnderhillUnit/HobbitDensity
StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/ArcReactorOutput
StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/PowerLevel
StarkIndustries/AvengersHQ/TechOps/IronManArmorLine/JarvisUnit/PowerLevel
StarkIndustries/Wakanda/TechOps/AssemblyLine/ShuriUnit/VibraniumFlow
```

??? example "Match topics where the `Enterprise` is `InGenLabs`"

	- `.*` means match anything for a series of characters
	
	```bash title='Patterns'
	^InGenLabs/.*
	```
	<div class="result">
	```json title='Matches'
	InGenLabs/IslaNublar/TyrannosaurPit/AssemblyLine/TrexUnit/MeatConsumptionRate
	InGenLabs/JurassicPark/RaptorPaddock/DNAReplicationLine/CloningChamber/DinoGrowthRate
	```
	</div>

??? example "Match topics which contain `/TechOps/` or `/ShieldOps/`"

	- `(Tech|Shield)` means `Tech` or `Shield`
	
	```bash title='Patterns'
	.*/(Tech|Shield)Ops/.*
	```
	<div class="result">
	```json title='Matches'
	DocBrownIndustries/HillValley/TechOps/LabAssemblyLine/DeLoreanStation/PowerLevel
	GalacticEmpire/DeathStar/TechOps/WingAssemblyLine/TIEFighterDock/PowerLevel
	MinistryOfMagic/Hogwarts/TechOps/WandCraftingLine/ElderWandStation/PowerLevel
	MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/ArcReactorOutput
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/AvengersHQ/TechOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/Wakanda/TechOps/AssemblyLine/ShuriUnit/VibraniumFlow
	```
	</div>

??? example "Match topics where the third element (`Area`) is `OrcForge`."

	- `p+` means match a character (`p`) which is repeated multiple times in succession.
	- `[^/]+` means match a character that is __not__ `/` which is repeated multiple times in succession.

	```bash title='Patterns'
	[^/]+/[^/]+/OrcForge/.*
	```
	<div class="result">
	```json title='Matches'
	DocBrownIndustries/HillValley/TechOps/LabAssemblyLine/DeLoreanStation/PowerLevel
	GalacticEmpire/DeathStar/TechOps/WingAssemblyLine/TIEFighterDock/PowerLevel
	MinistryOfMagic/Hogwarts/TechOps/WandCraftingLine/ElderWandStation/PowerLevel
	MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/ArcReactorOutput
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/AvengersHQ/TechOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/Wakanda/TechOps/AssemblyLine/ShuriUnit/VibraniumFlow
	```
	</div>

??? example "Match topics where the last element is `PowerLevel`"

	- `$` means the end of a line
	
	```bash title='Patterns'
	.*/PowerLevel$
	```
	<div class="result">
	```json title='Matches'
	DocBrownIndustries/HillValley/ClockTower/LabAssemblyLine/DeLoreanStation/PowerLevel
	DocBrownIndustries/HillValley/TechOps/LabAssemblyLine/DeLoreanStation/PowerLevel
	GalacticEmpire/DeathStar/TechOps/WingAssemblyLine/TIEFighterDock/PowerLevel
	GalacticEmpire/DeathStar/TractorBeam/WingAssemblyLine/TIEFighterDock/PowerLevel
	MinistryOfMagic/Hogwarts/DarkArts/WandCraftingLine/ElderWandStation/PowerLevel
	MinistryOfMagic/Hogwarts/TechOps/WandCraftingLine/ElderWandStation/PowerLevel
	MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/AvengersHQ/TechOps/IronManArmorLine/JarvisUnit/PowerLevel
	```
	</div>

??? example "Match topics where the last element is `PowerLevel#` wher `#` is a number between 0 and 2."

	- `[0-2]` means match any single digit between 0 and 2, inclusive.
	
	```bash title='Patterns'
	.*/PowerLevel[0-2]$
	```
	<div class="result">
	```json title='Matches'
	DocBrownIndustries/HillValley/ClockTower/LabAssemblyLine/DeLoreanStation/PowerLevel
	DocBrownIndustries/HillValley/TechOps/LabAssemblyLine/DeLoreanStation/PowerLevel
	GalacticEmpire/DeathStar/TechOps/WingAssemblyLine/TIEFighterDock/PowerLevel
	GalacticEmpire/DeathStar/TractorBeam/WingAssemblyLine/TIEFighterDock/PowerLevel
	MinistryOfMagic/Hogwarts/DarkArts/WandCraftingLine/ElderWandStation/PowerLevel
	MinistryOfMagic/Hogwarts/TechOps/WandCraftingLine/ElderWandStation/PowerLevel
	MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
	StarkIndustries/AvengersHQ/ShieldOps/IronManArmorLine/JarvisUnit/PowerLevel
	StarkIndustries/AvengersHQ/TechOps/IronManArmorLine/JarvisUnit/PowerLevel
	```
	</div>

??? example "Match topics where the last element is `PowerLevel#` wher `#` is a two digit number."

	- `\d{#}` means match a number which has `#` digits.
	
	```bash title='Patterns'
	.*/PowerLevel\d{2}$
	```
	<div class="result">
	```json title='Matches'
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel42
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel897
	```
	</div>

??? example "Match topics which contain `/RingSmithingLine/` or `/ringSmithingLine/`"

	- `[Rr]` means match any of the characters in the brackets.
	
	```bash title='Patterns'
	.*/[Rr]ingSmithingLine/.*
	```
	<div class="result">
	```json title='Matches'
	MordorIndustries/BaradDur/OrcForge/ringSmithingLine/MountDoomKiln/LavaFlowRate
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel1
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel2
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel3
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel42
	MordorIndustries/BaradDur/OrcForge/RingSmithingLine/MountDoomKiln/PowerLevel897
	MordorIndustries/BaradDur/TechOps/RingSmithingLine/MountDoomKiln/PowerLevel
	```
	</div>

??? example "Match topics where 1) Any element ends with the word `Beam` and 2) The last element contains `Cannon`"

	- `[A-Za-z0-9]+` means match a series of character within the bounds `A-Z`, `a-z`, or `0-9`
	
	```bash title='Patterns'
	.*/[A-Za-z0-9]+Beam/.*
	.*/[A-Za-z0-9]+Cannon[A-Za-z0-9]+$
	```
	<div class="result">
	```json title='Matches'
	GalacticEmpire/DeathStar/TractorBeam/WingAssemblyLine/TIEFighterDock/LaserCannonPower
	```
	</div>

