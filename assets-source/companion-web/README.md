# Companion WebView bundle

This folder contains the offline Three.js/VRM renderer embedded by Android's `WebViewAssetLoader`. The exact `three` and `@pixiv/three-vrm` versions are pinned in `package.json`; the generated output is checked into `app/src/main/assets/companion/` so the app does not need network access at runtime.

## Rebuild

From this directory, run:

```bash
npm install && npm run build
```

Vite writes `index.html` and the bundled `companion.js` directly to `../../app/src/main/assets/companion/`. Commit the generated files along with any renderer source changes.

The bundle exposes `window.companion` with `loadVrm`, `setMouth`, `setEmotion`, `setSpeaking`, `pause`, `resume`, and `dispose`. VRM loading supports VRM 0.x and 1.0 through `@pixiv/three-vrm`'s `VRMLoaderPlugin`; all scripts and libraries are bundled locally with no CDN requests.
