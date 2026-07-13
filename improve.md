# Zora Roleplay Presentation Improvement Plan

## Summary

Polish the roleplay chat experience in four connected areas:

1. Give AI roleplay text semantic colors for spoken dialogue, actions, and
   in-character thoughts.
2. Add optional clear liquid-glass bubbles for user messages, AI messages, or
   both, with global defaults and a per-session override.
3. Add a concise, replayable onboarding guide for the Roleplay UI.
4. Replace the cramped Community import-first sheet with the rich character
   profile sheet used by local characters.

Keep the work UI-focused. Do not change chat sync, API ownership, Community
authentication, model reasoning visibility, or the existing local-first privacy
model.

## 1. Semantic Roleplay Text

### Formatting contract

In Roleplay UI, style AI-authored character text with a small conventional
grammar:

- `"Spoken dialogue"` uses the pink speech color.
- `*Physical action or scene action*` uses the blue action color and italics.
- `(In-character private thought)` uses the neutral thought color.
- Unmarked AI text falls back to spoken dialogue so old messages still gain the
  intended pink appearance.

This means fictional character thoughts only. Never request, expose, store, or
label hidden model chain-of-thought.

Add a short roleplay-output formatting instruction when generating new roleplay
responses so future messages consistently use the grammar. The renderer must
remain tolerant when a model ignores it or returns unmatched delimiters.

### Rendering

- Add a dedicated parser such as `formatRoleplayMessage` that produces an
  `AnnotatedString` while preserving the existing lightweight bold, italic,
  code, math, paragraph, and table handling.
- Apply semantic coloring only to AI messages while Roleplay UI mode is active.
- Keep regular assistant mode and user-message formatting unchanged.
- Preserve raw message text for copy, edit, export, retry, storage, and TTS.
- Use true black or a near-black neutral for thoughts in light mode. In dark mode
  use the corresponding high-contrast neutral instead of unreadable black.
- Recommended roleplay colors:
  - speech: existing Zora pink/accent
  - action: readable cyan-blue in both themes
  - thought: theme primary neutral
- Malformed or nested delimiters must fall back to ordinary text without losing
  characters or crashing composition.

## 2. Liquid-Glass Chat Bubbles

### User controls

Add a `BubbleGlassMode` option set:

- `Off`
- `User messages`
- `AI messages`
- `Both`

Global control:

- Place a segmented control in Roleplay Settings under a new `Chat appearance`
  section.
- Persist it in DataStore as `roleplay_bubble_glass_mode_v1`.
- Default to `Off` so existing installs keep their current appearance.

Per-session control:

- Add `Message bubbles` inside Persona Settings > `UI`.
- Choices are `Use global`, `Off`, `User`, `AI`, and `Both`.
- Treat this as a session appearance override, including multi-AI sessions; it
  does not belong to an individual group member.

### Visual treatment

- Rework the shared `MessageBubble` container so solid and glass styles use the
  same dimensions, grouping radii, click targets, inline actions, and image
  layout.
- Glass bubbles use a low-alpha surface that reveals the selected chat
  background, a thin high-light border, a subtle inner highlight, and restrained
  shadow elevation.
- Keep text contrast stable over custom images by applying a minimum tint/scrim.
- Do not blur message text or images. Avoid an expensive per-bubble real-time
  backdrop blur; use a polished translucent glass treatment that behaves
  consistently across Android 8+.
- Respect light mode, dark mode, pure black/white backgrounds, and custom image
  backgrounds.

### Persistence

- Add `bubbleGlassOverride: BubbleGlassMode?` to `ChatSession` and
  `ChatSessionEntity`.
- Increment Room from version 11 to 12 and add the nullable session column with a
  safe default.
- Map the field in `ChatRepository`.
- Include it in session JSON import/export with a safe default for older files.
- Config-only Community imports should use the global setting until the user
  chooses a local override.

## 3. Roleplay Onboarding

Create a first-run, full-screen onboarding pager shown the first time a user
enters Roleplay UI, not on every general app launch.

Suggested pages:

1. `Your private roleplay space`: chats and API keys stay on the device.
2. `Discover`: switch between Your AI and Community; browsing/importing is
   public, while Google sign-in is only for publishing ownership.
3. `Create and customize`: explain Create, Persona Settings, backgrounds, and
   the per-session UI tab.
4. `Start chatting`: point to provider API setup, message actions, and the chat
   composer.

Behavior:

- Use `HorizontalPager` with Back, Next, Skip, and Start controls plus page dots.
- Use existing app visuals or small real UI previews; do not create a marketing
  landing page.
- Persist `roleplay_onboarding_completed_v1` in DataStore after Skip or Start.
- Add `Replay onboarding` in Roleplay Settings.
- Onboarding never requires Google sign-in and never blocks local creation.
- Keep copy short and provide English and Vietnamese string resources.

## 4. Community Character Profile and Import

### Discovery interaction

- Make the whole Community character row/card tappable.
- Replace the tiny import-first interaction with a full Community character
  profile sheet matching the local `CharacterDetailSheet` visual language.
- Keep a familiar download/import icon as a secondary affordance, but route it
  to the same profile sheet rather than importing immediately.

### Profile sheet

Show:

- large avatar or fallback
- character name and `@author`
- all tag chips in a wrapping row
- tagline and description/opening preview
- optional background image when available
- one large pink `Import` button fixed near the bottom

The sheet must scroll safely on short phones and avoid overlapping the bottom
navigation or system navigation area.

### Import state flow

1. Opening the profile uses already-cached Community catalog metadata and should
   feel instant.
2. Tapping `Import` downloads the config and media while showing progress inside
   the same sheet.
3. After parsing, show the duplicate warning inline in the profile sheet.
4. If no duplicate exists, offer `Import` and `Cancel`.
5. If a duplicate exists, offer `Import`, `Import as Copy`, and `Cancel`, keeping
   the existing rule that nothing is overwritten automatically.
6. On failure, retain the profile and show a retry action instead of dismissing
   it.

Reuse the existing `ImportPreviewState`, duplicate detector, and
`confirmImportPreview` path where practical. Local `.zorashare` and file-config
imports may keep their generic preview sheet; the rich profile flow is specific
to Community discovery.

Anonymous users must still be able to browse, open profiles, and import. Google
sign-in remains required only for publishing or managing owned uploads.

## Primary Code Areas

- `app/src/main/java/com/yozora/aichat/ui/CompanionChatApp.kt`
  - semantic roleplay text renderer
  - shared glass bubble visuals
  - onboarding pager
  - global and per-session controls
  - Community profile/import sheet
- `app/src/main/java/com/yozora/aichat/ui/chat/ChatViewModel.kt`
  - global preference state and updates
  - session override updates
  - onboarding completion state
  - Community detail/import state transitions
  - JSON compatibility
- `app/src/main/java/com/yozora/aichat/data/db/Entities.kt`
- `app/src/main/java/com/yozora/aichat/data/db/ChatRepository.kt`
- `app/src/main/java/com/yozora/aichat/data/db/AppDatabase.kt`
  - Room 11 to 12 migration for the per-session bubble override
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-vi/strings.xml`

## Test Plan

### Build and migration

- Run `.\gradlew.bat assembleZoraRelease`.
- Upgrade a database from Room 11 and verify every session/message remains.
- Import old config and session JSON without a glass field.

### Semantic text

- Verify speech, action, thought, mixed, multiline, markdown, malformed, and
  streaming/animated messages.
- Verify only AI roleplay messages receive semantic colors.
- Check light and dark contrast, especially thought text.
- Confirm copy/export/TTS still use the original raw message.

### Glass bubbles

- Test the global matrix: Off, User, AI, Both.
- Test every per-session override against each global choice.
- Verify solid and glass bubbles do not resize or shift when switching.
- Check dark, light, pure black/white, preset, and custom-image backgrounds.
- Check long text, image messages, grouped messages, and inline actions.

### Onboarding

- Fresh install shows onboarding on first Roleplay entry.
- Skip and Start both prevent repeat display.
- Replay onboarding works from Settings.
- Leaving halfway does not corrupt navigation or chat state.
- English and Vietnamese layouts fit small portrait screens.

### Community profile/import

- Guest users can browse, open a profile, and import.
- Profile data and images load from cache when revisited.
- Import progress, retry, duplicate warning, Import, and Import as Copy all work.
- Successful import creates the expected local character and opens no accidental
  duplicate.
- Profile sheet remains usable on short phones and in both Roleplay themes.

## Assumptions

- "Thinking" means fictional in-character inner monologue, not private model
  reasoning.
- Conventional roleplay delimiters are acceptable: quotes for speech, asterisks
  for action, and parentheses for thoughts.
- "Liquid glass" means a clear, translucent, high-contrast bubble treatment; a
  costly platform-dependent live backdrop blur is not required.
- The per-character setting is stored per chat session because bubble appearance
  affects the entire conversation, including group chats.
- This document defines the next implementation pass; it does not bump or release
  a version by itself.
