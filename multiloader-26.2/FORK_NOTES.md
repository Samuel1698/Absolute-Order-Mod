# Absolute Order — local fork handoff (Minecraft 26.2 source)

Baseline: user-supplied `Absolute-Order-Mod-main.zip`, module `multiloader-26.2`, upstream mod 2.0.0. The supplied archive contains NO 26.3 module. All historical Minecraft modules in the repository are untouched. The mod ID remains `chestseparators` so this fork replaces upstream, rather than running beside it. MIT license retained.

## Changes

1. Player inventory filters protect matching items during nearby Auto Deposit and open-container Push. With existing target `0` (displayed **Keep: All**), items in matching filtered slots never leave through these actions. A positive value retains that many of each item listed in the filter, across the group's slots (earlier slots have priority). Unfiltered stacks and unrelated items remain eligible. Client deposit/pull previews use corresponding quantity calculations.
2. Added a compact **Keep** button to the **inventory** filter editor (not container filters). Left click adds 1, Shift-click subtracts 1, Ctrl-click adds 16, Ctrl+Shift-click resets to Keep: All. Existing `SlotWhitelist.targetCount` codecs already support the field; undo/redo and clipboard copy now preserve it. Filtered Pull respects positive amounts; Shift-Pull deliberately retains upstream's override behavior, pulling all that fits.
3. Pickup slot preference is now scoped to `ItemEntity.playerTouch`; inventory actions such as closing a screen with cursor items no longer run pickup-filter routing. Existing `lockHotbarOnReorder` setting is left untouched.
4. Ender Chest filters remain saved in the client's `ender_chest.dat`, but are synchronized to the player's live server-side Ender Chest inventory on opening and editing. The server validates entries against its *actual container size* rather than assuming 27 or 54. Shift-click enforcement uses upstream's existing `IWhitelistProvider` mixins. Modded 54-slot compatibility still requires gameplay verification.

## Build (WSL/Linux, **JDK 25**, network required for Gradle dependencies)

```bash
cd multiloader-26.2
chmod +x gradlew
java -version
./gradlew :common:test :fabric:build --no-daemon
```

The publishable Fabric artifact is expected under `fabric/build/libs/` with a name like `AbsoluteOrder-2.0.0-noire.1-fabric-mc26.2.jar`. Replace the old mod JAR; do NOT run both together. Install the fork on **both** the Fabric client and dedicated server for filter synchronization. This is *not* a Minecraft 26.3 build.

## Current verification

- `git diff --check`: passed locally.
- Pure Java quantity-policy checks (11 scenarios): passed using available JDK 21.
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
