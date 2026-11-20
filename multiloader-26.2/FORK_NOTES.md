# Absolute Order — local fork handoff (Minecraft 26.2 source)

Baseline: user-supplied `Absolute-Order-Mod-main.zip`, module `multiloader-26.2`, upstream mod 2.0.0. The supplied archive contains NO 26.3 module. All historical Minecraft modules in the repository are untouched. The mod ID remains `chestseparators` so this fork replaces upstream, rather than running beside it. MIT license retained.

## Changes

1. Player inventory filters protect matching items during nearby Auto Deposit and open-container Push. With existing target `0` (displayed **Keep: All**), items in matching filtered slots never leave through these actions. A positive value retains that many of each item listed in the filter, across the group's slots (earlier slots have priority). Unfiltered stacks and unrelated items remain eligible. Client deposit/pull previews use corresponding quantity calculations.
2. The inventory-filter editor now has a compact drag/click slider with five stops, **All / 64 / 32 / 16 / 1** from left to right. **All** is distinct from **64**: All protects every matching item physically in filtered slots, whereas 64 protects at most 64 across each group/item. The `targetCount=0` legacy behavior and arbitrary saved values continue to load; moving the new slider selects one of the five stops. `SlotWhitelist.targetCount` codecs, undo/redo and clipboard copy preserve the value. Filtered Pull respects positive amounts; Shift-Pull deliberately retains upstream's override behavior, pulling all that fits.
3. Pickup slot preference is now scoped to `ItemEntity.playerTouch`; inventory actions such as closing a screen with cursor items no longer run pickup-filter routing. Existing `lockHotbarOnReorder` setting is left untouched.
4. Ender Chest filters remain saved in the client's `ender_chest.dat`, but are synchronized to the player's live server-side Ender Chest inventory on opening and editing. The server validates entries against its *actual container size* rather than assuming 27 or 54. Shift-click enforcement uses upstream's existing `IWhitelistProvider` mixins. Modded 54-slot compatibility still requires gameplay verification.
5. Refill of filtered player **main inventory** (raw slots 9..35) now merges matching unfiltered main-inventory stacks into existing home stacks, then fills empty home slots in filter item priority. It does not take from the hotbar (0..8), offhand, armor, or *any* filtered source slot; it never replaces a nonmatching item already occupying a home slot. The server runs this after a filtered-source -> unfiltered-main manual placement, after ground pickups, and before nearby Auto Deposit. The open-container Push action performs the same consolidation using normal synchronized inventory clicks before computing deposit allowances. A filtered 1 + adjacent unfiltered 64 becomes filtered 64 + unfiltered 1 before deposit; Keep: 1 still ultimately protects 1 across the group. In a chest UI, manual moves from an **unfiltered source** to an unfiltered destination do not proactively refill until Push or a subsequent ground pickup; manual moves **out of a filtered main slot** do trigger refill on the server.

Known limitation: the **hover preview** for Push is calculated from the current inventory layout, before its first-click consolidation. If you have pre-existing stray matching stacks in the main inventory, the hover preview might momentarily differ from the final result. The actual transfer uses the consolidated inventory. Recheck the preview after one Push or an inventory-slot move and report gameplay discrepancies.

## Build (WSL/Linux, **JDK 25**, network required for Gradle dependencies)

```bash
cd multiloader-26.2
chmod +x gradlew
java -version
./gradlew :common:test :fabric:build --no-daemon
```

The publishable Fabric artifact is expected under `fabric/build/libs/` with a name like `AbsoluteOrder-2.0.0-fork.1-fabric-mc26.2.jar`. Replace the old mod JAR; do NOT run both together. Install the fork on **both** the Fabric client and dedicated server for filter synchronization. This is *not* a Minecraft 26.3 build.

## Current verification

- `git diff --check`: passed locally.
- Pure Java quantity-policy checks (11 scenarios) and dedicated retention-slider stop checks: passed using available JDK 21.
- Filter-refill logic compiled against isolated Minecraft API stubs using JDK 21; 8 standalone scenarios passed (stack fill, empty home, hotbar isolation, protected sources, item/component compatibility, priority). This is **not** a real mod compile.
- Full Gradle/compile/tests: **not run successfully**. The execution environment has Java 21, not Java 25, and could not download the Gradle 9.4.0 wrapper distribution (`UnknownHostException: services.gradle.org`).
- Minecraft singleplayer/multiplayer and Ender Chest 54-slot compatibility: **not tested**.

## Manual test matrix (back up client configs and the world first)

1. Filter arrows in hotbar, Keep: All. Carry 64. Auto Deposit next to a filtered chest; none of those 64 may leave.
2. Change Keep to 1. Carry 64 arrows in the filtered slot; Auto Deposit must move at most 63 and leave 1. Repeat with Push from an open filtered chest, and compare preview to result.
3. Place extra arrows in an unfiltered slot; automatic deposit should move those freely, retaining the 1 arrow in the filtered slot. Test two slots in one filter group, testing total retention across the group.
4. Pull from a chest with an arrow filter, Keep: 1: it should pull only 1 when none is held and 0 afterward. Shift-Pull explicitly ignores the cap as before. Confirm the preview matches each case.
5. Manually place spruce logs in the hotbar and close/reopen inventory. No filter-driven relocation should occur. New logs picked up from the ground should still prefer dedicated empty slots. Test stacks and empty slots.
6. Ender Chest 27 slots: configure filters, close/reopen, shift-click into matching slots, verify wrong items are refused as configured. Repeat against your **actual 54-slot Ender Chest mod**, with filters above slot 27, after logout/reconnect, and after server restart. Repeat after changing filters, and try both directions.
7. Validate an unrelated chest and vanilla shift-click behavior, plus all applicable 3 filter-rule toggles. Verify the server and client are both running the fork, not a mixture of fork and upstream JARs.

**Rollback:** keep the old mod JAR and a copy of `config/chestseparators/` on the client, plus a server world backup. Do not deploy the 26.2 artifact onto Minecraft 26.3. For a proper 26.3 port, obtain the exact corresponding upstream 26.3 source module first; do not just change `minecraft_version` in `gradle.properties`.

## Follow-up fork revision (additional commits 0008–0009)

- `fix(inventory): refill filtered main slots before deposits` — adds server-side movement recovery, ground-pickup consolidation, nearby Auto Deposit pre-pass, and client-synchronized open-container Push pre-pass.
- `feat(filters): replace quantity button with retention slider` — five-step click/drag quantity control (All, 64, 32, 16, 1) with safe preservation of older custom quantities and unit tests.
- These patches apply **after** the first seven fork patches. They are not a port to Minecraft 26.3.
- Multiplayer Ender Chest sync (earlier test #6), 54-slot Ender Chest compatibility, and the new manual-click integration still require in-game verification. Keep production server 26.3 untouched.
