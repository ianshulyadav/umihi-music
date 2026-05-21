# Plan: Unified Bottom-Sheet Player Transition System

This plan details the replacement of the page-based, double-screen player navigation structure in UmihiMusic with a single unified, morphing bottom-sheet player. This matches the design aesthetics of PixelPlay, providing a seamless slide-up and crossfade transition without looking like separate screens.

---

## Analysis & Comparison

### Current Structure:
- **Legacy Player:** UmihiMusic uses two separate components (`MiniPlayerWrapper` and `PlayerScreen`) connected via `Navigation3` backstack navigation (`PlayerScreenKey`).
- **Issues:**
  - Navigating to the player screen creates a standard page transition (slide/fade).
  - The background components and artwork do not morph smoothly; they look like two different screens.
  - No horizontal swipe-to-dismiss gesture on the miniplayer.

### Proposed Structure:
- **Unified Bottom-Sheet Player:** A single, persistent bottom sheet (`UnifiedPlayerSheet`) is embedded globally in `NavigationRoot.kt`.
- **Key Improvements:**
  - The sheet remains at the bottom of the screen (collapsed state) as the miniplayer.
  - Tapping or dragging vertically expands the sheet to full screen, smoothly morphing the background card's corner radius, margins, shadow, and position.
  - The content transitions via a centered crossfade (with layout/alpha interpolation based on the expansion fraction).
  - Integrates `MiniPlayerDismissGestureHandler` for horizontal swipe-to-dismiss.
  - Removes the legacy page-based `PlayerScreenKey` navigation entirely.

---

## Proposed Changes

### 1. State Management & ViewModel

#### [MODIFY] [PlayerState.kt](file:///d:/Downloads/UmihiMusic/app/src/main/java/ca/ilianokokoro/umihi/music/ui/screens/player/PlayerState.kt)
- Define `enum class PlayerSheetState { COLLAPSED, EXPANDED }`.
- Add `val sheetState: PlayerSheetState = PlayerSheetState.COLLAPSED` to `PlayerState`.

#### [MODIFY] [PlayerViewModel.kt](file:///d:/Downloads/UmihiMusic/app/src/main/java/ca/ilianokokoro/umihi/music/ui/screens/player/PlayerViewModel.kt)
- Add functions:
  - `expandPlayerSheet()`: sets state to `EXPANDED`.
  - `collapsePlayerSheet()`: sets state to `COLLAPSED`.
  - `togglePlayerSheetState()`: toggles between `COLLAPSED` and `EXPANDED`.
- Wire horizontal swipe-to-dismiss gesture to clear/stop playback or hide current song:
  - Add `dismissPlaylist()`: stops playback and clears the queue items.

---

### 2. Gesture Handling & Animations

#### [NEW] [MiniPlayerDismissGestureHandler.kt](file:///d:/Downloads/UmihiMusic/app/src/main/java/ca/ilianokokoro/umihi/music/ui/screens/player/components/MiniPlayerDismissGestureHandler.kt)
- Port the horizontal swipe-to-dismiss gesture handler from File 8 of the recorded code.
- Map the callbacks to UmihiMusic models/actions.

---

### 3. Unified Bottom Sheet Composable

#### [NEW] [UnifiedPlayerSheet.kt](file:///d:/Downloads/UmihiMusic/app/src/main/java/ca/ilianokokoro/umihi/music/ui/screens/player/components/UnifiedPlayerSheet.kt)
- Houses the vertical drag and tap gesture handling.
- Keeps track of the `translationY` from `collapsedOffsetY` (bottom of the screen, accounting for the bottom navigation bar height/padding) to `0f` (full screen).
- Calculates the `expansionFraction = 1f - (translationY / collapsedOffsetY)`.
- Interpolates the outer container shape (corner radius), horizontal padding/margins, and container background color (based on active album color scheme).
- Integrates `MiniPlayerDismissGestureHandler` on the miniplayer content area.
- Renders:
  - **MiniPlayer Content:** visible when `expansionFraction < 0.5f` (with alpha `1f - expansionFraction * 2f`).
  - **FullPlayer Content:** visible when `expansionFraction >= 0.5f` (with alpha `(expansionFraction - 0.5f) * 2f`), adapting Umihi's controls and layout inside.
- Implements `BackHandler` to intercept the back gesture and collapse the sheet if it's currently expanded.

---

### 4. Navigation Root & Layout

#### [MODIFY] [NavigationRoot.kt](file:///d:/Downloads/UmihiMusic/app/src/main/java/ca/ilianokokoro/umihi/music/ui/navigation/NavigationRoot.kt)
- Remove `PlayerScreenKey` route and transition logic from navigation backstack.
- Embed `UnifiedPlayerSheet` globally at the bottom of the layout inside the main root `Box`.
- Intercept the miniplayer click action to trigger `playerViewModel.expandPlayerSheet()`.
- Pass dynamic bottom navigation bar visibility and height to `UnifiedPlayerSheet` to correctly calculate `collapsedOffsetY`.

---

## Verification Plan

### Automated Tests
- Run Gradle compilation:
  ```powershell
  .\gradlew.bat compileDebugKotlin
  ```
  Ensure the app builds successfully without any errors or dependency issues.

### Manual Verification
- Deploy to device/emulator.
- Verify that playing a song displays the miniplayer at the bottom.
- Verify that clicking the miniplayer or dragging it vertically expands it into the full player screen smoothly.
- Verify that pressing the system back button while expanded collapses it back to the miniplayer.
- Verify that swiping horizontally on the miniplayer dismisses the player and stops playback.
