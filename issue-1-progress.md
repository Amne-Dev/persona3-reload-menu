# Issue #1 progress — Essential menu compatibility

GitHub issue: [Amne-Dev/persona3-reload-menu#1](https://github.com/Amne-Dev/persona3-reload-menu/issues/1)

## 2026-09-07 — Investigation started

Status: in progress

### Reported behavior

- With Essential installed, the P3R title screen loads but normal destinations such as Singleplayer are not readily accessible.
- Essential-related entries can trigger a transition that appears to loop or remain as a solid blue overlay.
- Making the window taller reveals more entries, suggesting the title layout overflows vertically.
- Flashback's added menu entry reportedly works.

### Evidence read from the GitHub issue

- Screenshot 1 shows Essential's internal/debug widgets (`essential_world_host`, `essential_social`, `essential_debug`, reserved entries, and others) rendered as P3R primary menu entries in a long right-side column. The normal title actions have been displaced outside the intended menu area.
- Screenshot 2 shows a completely blue frame after activating an Essential entry, matching a transition that has not reached its terminal state.
- The report uses Minecraft 1.21.11.

### Initial hypotheses to verify

1. The title-screen widget collector accepts every nonblank `ClickableWidget`, including hidden/internal widgets contributed by Essential, and then forces them invisible while recreating their appearance in the P3R list.
2. The title layout assumes a modest number of primary entries and does not constrain or intentionally overflow extra third-party widgets.
3. The generic `Screen`/`setScreen` transition hook may intercept a destination opened by a transition callback, causing re-entry, or may apply to Essential overlay/wrapper screens that should retain their own lifecycle.
4. Original widget callbacks may still be preserved because the implementation retains real widget instances, but this must be confirmed for all supported versions.

### Plan

1. Trace title widget discovery, visibility changes, activation, layout, screen-change interception, and transition completion on every supported target.
2. Compare those assumptions with Essential's public source/API behavior and the exact issue screenshots.
3. Introduce a general compatibility decision layer for title widgets and screen transitions; use optional Essential detection only if generic ownership/state checks are insufficient.
4. Add responsive overflow behavior without redesigning unrelated screens and preserve original button instances/callbacks.
5. Build every target, inspect mixin/runtime logs, and validate normal and synthetic third-party menu entries across short and wide layouts.

## 2026-09-07 — Root cause confirmed

Status: implementation in progress

### Essential interaction traced

- Essential does not replace `TitleScreen` for this feature. Its `MixinGuiMainMenu_ProxyButtons` adds a `ScreenWithProxiesHandler` at the end of title-screen initialization.
- That handler adds `EssentialProxyElement` button objects for host/invite, social, wardrobe, pictures, settings, account, notification flags, and the player model. Their placeholder messages (`<essential_...>`) are stable proxy identifiers, not labels intended for users.
- Essential keeps the actual Elementa controls and the vanilla proxy widgets synchronized. Setting an Essential proxy's `visible` flag or bounds tells Essential that another menu mod has taken ownership.
- Essential's proxy action is handled by its overridden mouse-input method and delegated to the matching Elementa component. Treating every proxy as a normal title action and invoking only vanilla `onPress` does not preserve that input contract.

### Confirmed P3R faults

1. The title collector mutated every button's visibility before checking whether P3R should own it. On 1.20.1, 1.21.11, and 26.2 it forced all buttons hidden; on 1.21.8 it forced all buttons visible and moved all collected backing widgets into P3R row bounds.
2. Every nonblank `AbstractButton` was accepted. This promoted Essential's internal proxy identifiers to user-facing P3R entries and handed their visibility/position lifecycle away from Essential.
3. The row-density calculation had a hard minimum spacing. Once enough proxy entries were collected, `total = (count - 1) * step` exceeded the available height and rows left the viewport; increasing window height exposed them, exactly as reported.
4. P3R retains normal third-party button objects/callbacks, so ordinary callback copying was not the problem. Essential is exceptional because its proxy input behavior intentionally lives outside the ordinary `onPress` callback.
5. There is no global `setScreen` interception in the affected implementations. The apparent loop is repeated OUT/IN animation around a proxy action that P3R cannot dispatch correctly, not a recursive `setScreen` hook. The modern transition phase guard already prevents nested `startOut`; the 1.20.1 implementation lacked that guard.

### Fix being applied

- Inspect original visibility before mutating a newly discovered widget.
- Detect the optional `essential`/`essential-container` Fabric packaging and its proxy base type reflectively, with no compile-time Essential dependency. Essential proxies remain visible, keep their original bounds, and receive mouse input through their own handlers before overlapping P3R rows. A live 1.21.11 launch verified that the official pinned Essential 1.4.1.1 artifact registers as `essential-container` and embeds `essential-loader`.
- Continue adopting ordinary visible third-party title buttons using their original widget objects and callbacks.
- Window unusually long P3R action lists to the available vertical bounds. Selection stores the original source index, follows the visible window, and mouse-wheel/keyboard navigation keeps every entry reachable.
- Guard 1.20.1 against transition re-entry and put transition completion in `finally` on every target so an action cannot leave the global wipe/input state latched.

## 2026-09-07 — Build and runtime validation

Status: complete

- `compileJava` passed for 1.20.1, 1.21.8, 1.21.11, and 26.2 (Java 21 for the first three build environments; Java 25 for 26.2).
- Full `build` passed for all four targets, including remapped JAR generation. There are no repository test sources, so Gradle reports `test NO-SOURCE`.
- A live 1.21.11 run loaded the official Essential 1.4.1.1 pinned artifact far enough to verify `essential-container 1.0.0`, embedded `essential-loader 1.3.2`, and the full Essential stage `1.4.1.1`.
- Essential cannot reach `TitleScreen` in this Loom development environment: without Fabric Language Kotlin, its runtime remapper fails on missing `kotlin.jvm.functions.Function0`; with the compatible Kotlin runtime added, Essential itself fails during initialization with a `ClassFormatError` for a duplicate `UtilitiesKt.width` method. The stack is entirely inside Essential/Elementa runtime-remapping code and occurs before any title-screen mixin or P3R transition runs.
- Because that upstream development-remapper failure prevents an end-to-end Essential screenshot here, final validation will distinguish build/source-level Essential verification from live no-Essential UI checks rather than claiming an unavailable visual result.

### Final verification after the overflow correction

- Rebuilt every supported target after the final source change:
  - Minecraft 1.20.1: `BUILD SUCCESSFUL` in 34 s.
  - Minecraft 1.21.8: `BUILD SUCCESSFUL` in 31 s.
  - Minecraft 1.21.11: `BUILD SUCCESSFUL` in 27 s.
  - Minecraft 26.2: `BUILD SUCCESSFUL` in 17 s.
- `git diff --check` passed for all Issue #1 source files and this log. The only output was the repository's existing LF-to-CRLF warning.
- Ran 72 layout assertions over 1920x1080 at GUI scale 2, 2560x1440 at GUI scale 2, 1280x720 at GUI scales 2 and 3, 3440x1440 ultrawide at GUI scale 2, and a narrower 1280x1024 case at GUI scale 2. Counts of 1, 5, 20, and 100 entries were checked with first, middle, and last selections. Every selected source index remained in the visible window and every row span remained within the computed top/bottom bounds.
- Live-smoked Minecraft 1.20.1 at 1920x1080 with automatic GUI scale. The P3R title screen reached a stable rendered frame with the normal primary and footer actions present and within the viewport.
- A 1280x720 / GUI-scale-3 development launch reached client/resource initialization but shut down before a second screenshot could be captured; that case is therefore covered by coordinate assertions rather than claimed as a completed visual test.
- A no-Essential 1.21.11 run is currently blocked before `TitleScreen` by the pre-existing `LoadingOverlayMixin` error `Can only blur once per frame`. This is separate from the title compatibility files changed for Issue #1.

### Final implementation summary

- P3R now decides widget ownership before changing visibility or bounds.
- Essential proxy widgets are identified by their optional runtime type only when `essential` or `essential-container` is loaded. They are excluded from the P3R action list and left in Essential's coordinate, visibility, render, and input lifecycle.
- Passthrough widgets receive their native `mouseClicked` implementation before P3R row hit-testing. This preserves Essential's Elementa forwarding contract instead of incorrectly reducing it to `onPress`.
- Ordinary visible buttons injected by other mods are still adopted as P3R entries using their original widget objects and callbacks; no label allowlist is used for third-party actions.
- Oversized action collections use a selection-centered visible window. The capacity is derived from the actual vertical region and the same minimum row step used by rendering. Keyboard, mouse hover/click, and mouse-wheel navigation all retain source indices, so hidden overflow entries remain reachable without coordinate drift.
- Transition actions clear or resume their state in `finally`, and 1.20.1 rejects re-entrant transition starts. A failing or non-navigating action can no longer leave the blue wipe/input latch active.
- No compile-time Essential dependency was introduced. The same behavior is implemented in the 1.20.1, shared 1.21.8, 1.21.11, and 26.2 source sets; only the existing Minecraft-version API differences required separate method signatures.

## 2026-09-07 — Opt-in Essential development launch

Status: complete

- Added `-Essential` to `run.ps1`, for example: `.\run.ps1 -Version 1.21.11 -Essential`.
- The flag supports every run-script target through the official Essential 1.4.1.1 Fabric platform identifiers: 1.20.1, 1.21.8, 1.21.11, and 26.2.
- Essential is downloaded from `downloads.essential.gg` into the ignored `.gradle/essential-cache` directory. The script verifies that the archive contains `fabric.mod.json` with mod ID `essential-container` before using it.
- The verified jar is staged in the selected project's `run/mods` only for the requested launch and removed in `finally` when Gradle exits. Existing user-provided Essential jars are detected and left untouched.
- PowerShell parsing/help output passed. A 26.2 `--dry-run` verified download caching, archive validation, Gradle argument forwarding, staging, and cleanup.
- A real 26.2 launch verified Fabric Loader discovered `essential-container 1.0.0` and embedded `essential-loader 1.3.2`. Essential then hit its known development-remapper limitation (`Failed to find tiny mappings file`) before Minecraft startup; the staged jar was still removed correctly after the failed Gradle run.

## 2026-09-08 — Essential title integration and responsive footer

Status: complete

- Updated the modern development run configurations so Essential receives Loom's development mappings and can remap itself. Real Essential launches now reach the title screen on 1.21.11 and 26.2; `run.ps1 -Essential` still stages and removes the pinned container safely.
- Adopted Essential's user-facing proxy actions into the P3R title composition without replacing their native callbacks. Social, Wardrobe, Pictures, Essential Settings, Account, Invite Friends, and Host World therefore remain functional instead of being hidden.
- Moved the icon-style Essential actions into the footer and retained the host/invite actions in the main menu.
- Replaced the two-tier footer with one measured row. Its scale is derived from the sum of the rendered label widths, available screen width, responsive side margins, and compressed inter-item gaps. The same calculated rectangles drive rendering and input hitboxes.
- Reversed the final footer order at the user's request. On an Essential title screen it now runs from Essential Settings toward Quit Game, with the exact middle entries determined by the proxies available for that Minecraft version.
- Kept Essential's player proxy on the left. When Essential exposes a live player entity its own renderer remains authoritative; when its development renderer cannot initialize, modern versions use Minecraft's native interactive `PlayerSkinWidget` as a compatibility fallback in the same responsive left-side bounds.
- Minecraft 1.21.8 required its older `SkinManager.lookupInsecure` API; 1.21.11 and 26.2 use `SkinManager.createLookup`. No layout constants are resolution-specific.

### Footer and player validation

- Recompiled 1.20.1, 1.21.8, 1.21.11, and 26.2 successfully after the final footer ordering change.
- Live-tested 1.21.11 with Essential at 1936x1056, 1600x700, and 870x519 window bounds, including automatic GUI scale and explicit GUI scale 2.
- Confirmed the footer remains a single line at the tested wide/full-size layouts and scales down from measured content rather than wrapping.
- Confirmed the reversed order in a live Essential frame: Essential Settings, Pictures, Wardrobe, Social, Accessibility Settings, Language, Account, Quit Game.
- Confirmed the native player preview is visible on the left and recalculates its position and size after window changes.

## 2026-09-08 — Known option-screen crash fixed

Status: complete

### Root cause

- The keybind-row label customization used a core Mixin `@Redirect` on the vanilla text draw call in every supported source set.
- With MixinExtras 0.5.4 active, loading the nested vanilla `KeyBindsList.KeyEntry` class sent that redirect through `FactoryRedirectWrapperMixinTransformer`, which failed with `ClassCastException: java.util.ArrayList cannot be cast to AnnotationNode`.
- Accessibility and other vanilla option pages can resolve the keybind entry class while initializing shared option-screen code, so the crash appeared to affect ordinary menu options even before the Key Binds list was visibly opened.

### Fix

- Removed the keybind-row `@Redirect` injectors.
- Core-Mixin `@ModifyArg` hooks now capture the label and its vanilla Y coordinate while replacing only the original label argument with an empty component.
- A narrow `@Inject(at = @At("TAIL"))` draws the captured label through the existing fitted-text helper after vanilla has positioned the real change/reset buttons.
- This preserves the P3R fitted label, row-state colors, button callbacks, focus behavior, and shared visual/input coordinates while avoiding MixinExtras' failing factory-redirect transformation path.

### Runtime validation

- Compiled all four supported targets successfully after the injector change.
- Reproduced the former 1.21.11 + Essential path at 1920x1080 / automatic GUI scale: Accessibility Settings opened and the client remained alive.
- Navigated Accessibility Settings → Controls → Key Binds. The full P3R keybind list rendered with labels, binding buttons, reset buttons, and the left scrollbar aligned inside the panel.
- Sent a real mouse-wheel event over the list and captured the changed scroll position; scrolling remained functional.
- Repeated the former 26.2 + Essential crash path at 1920x1080 / automatic GUI scale. Accessibility Settings rendered and the client remained alive instead of failing during `KeyBindsList.KeyEntry` transformation.

## 2026-09-08 — Options integration cleanup and slider value tooltips

Status: complete

### Changes

- The P3R options-root collector now recognizes Essential's injected proxy by its stable `getEssentialId()` value, `settings`, keeps that widget invisible, and excludes it from the visual list, selection state, keyboard navigation, and hit testing.
- This is intentionally limited to the options root. The Essential Settings action previously integrated into the title-screen footer remains available.
- Configuration-page sliders retain their compact P3R labels, but now attach the current formatted value (the portion after the vanilla label separator) to Minecraft's real widget tooltip. The tooltip therefore follows the existing slider hitbox and refreshes after mouse dragging, clicking, or keyboard changes.
- Tooltip objects are refreshed only when their formatted value changes, avoiding per-frame allocations.

### Validation

- Compiled all supported targets successfully: 1.20.1, 1.21.8, 1.21.11, and 26.2.
- Live-tested 1.21.11 with Essential at a maximized 1936x1056 window using automatic GUI scale.
- Hovered the real Sensitivity slider and captured its native `100%` tooltip over the displayed control; the slider remained interactive and its visual track shared the widget's hover region.
- The follow-up options-root screenshot run was interrupted, so Essential-button removal is covered by the exact proxy-ID path verified from Essential 1.4.1.1's runtime class plus compilation across all source sets.

### Version handling

- 1.20.1 uses Yarn's `Tooltip.of(Text)` API.
- 1.21.8, 1.21.11, and 26.2 use Mojang mappings' `Tooltip.create(Component)` API; 26.2 retains its render-state extraction hook.

## 2026-09-09 — Contrast hardening and Night Mode

Status: complete

### Root cause

- Hover animations changed selected labels to a dark foreground while retaining Minecraft's dark text shadow. The foreground and shadow therefore collapsed into a muddy black-on-black edge during the transition and after selection.
- Bright panel, selection, and footer colors were embedded directly in several renderers, so there was no single theme boundary where a low-light palette could be applied consistently.

### Changes

- The shared fitted-text renderers now classify the final foreground color by luminance. Dark text is drawn without Minecraft's dark shadow; light text retains the shadow that improves readability on the navy surfaces.
- Removed the extra explicit shadow pass from the options-root hover state once its text transitions to the dark selected color. The selected FOV label/value follows the same rule.
- Added a persisted `night_mode` setting, defaulting to off, in every supported configuration implementation.
- Added `NIGHT MODE: ON/OFF` as a keyboard- and mouse-operable row in P3R Menu Settings. Changes apply immediately and are saved to `config/p3rmenu/settings.properties`.
- Centralized the configuration selection surface, selected foreground, panel, and footer colors in `P3RSettingsShell` / `P3RGraphics`. Night Mode replaces high-luminance white/slate surfaces with deep navy variants while keeping the P3R cyan, red, and pink accents.
- Updated options, buttons, sliders, selection lists, keybind entries, wallpaper selection, and the P3R settings screen to consume the semantic palette. Render positions and widget hitboxes were not translated or duplicated.

### Validation

- Live-tested Minecraft 1.21.11 at a maximized 1920x1080 desktop using automatic GUI scale.
- Verified the day-theme selected row has crisp dark text without a dark offset shadow.
- Enabled Night Mode through the real P3R Menu Settings row and verified the settings panel, selected row, options panel, and footer update immediately. The selected row remains readable in white on navy, and a hovered options item renders without the former black-on-black shadow.
- Verified persistence by observing `night_mode=true` written immediately to the version's development configuration.
- Measured representative contrast ratios: day selected text 18.63:1, night selected text 14.07:1, night footer primary 17.73:1, and night footer secondary 7.98:1.
- Built all supported targets after the final changes: Minecraft 1.20.1, 1.21.8, 1.21.11, and 26.2 all completed with `BUILD SUCCESSFUL`.
- `git diff --check` reported no whitespace errors; its output consisted only of the repository's existing LF-to-CRLF conversion warnings.

### Version handling

- 1.20.1 uses the Yarn-named `P3RSettingsShell` and `P3RConfig` APIs.
- 1.21.8, 1.21.11, and 26.2 carry the equivalent palette and persistence behavior in their version-specific `P3RGraphics` and `P3RConfig` source sets. No behavior-specific version workaround was necessary beyond the repository's existing mapped API split.
