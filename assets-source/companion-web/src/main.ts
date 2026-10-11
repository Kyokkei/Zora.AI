import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import {
  VRMLoaderPlugin,
  VRMUtils,
  type VRM,
} from '@pixiv/three-vrm';

type EmotionName = 'happy' | 'sad' | 'angry' | 'relaxed' | 'surprised' | 'neutral';
type MouthValues = [number, number, number, number, number];

type CompanionApi = {
  loadVrm(url: string): Promise<void>;
  setMouth(aa: number, ih: number, ou: number, ee: number, oh: number): void;
  setEmotion(name: EmotionName, weight: number): void;
  setSpeaking(speaking: boolean): void;
  pause(): void;
  resume(): void;
  dispose(): void;
  onError?: (message: string) => void;
};

declare global {
  interface Window {
    companion: CompanionApi;
    AndroidBridge?: {
      onError?: (message: string) => void;
      onReady?: () => void;
    };
  }
}

const root = document.getElementById('app');
if (!root) throw new Error('Companion canvas root is missing');

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(28, 1, 0.1, 50.0);
camera.position.set(0, 1.35, 1.25);

const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.setClearAlpha(0);
renderer.outputColorSpace = THREE.SRGBColorSpace;
root.appendChild(renderer.domElement);

// Studio Lighting matching PC test bench
scene.add(new THREE.HemisphereLight(0xffffff, 0x444455, 2.0));
const keyLight = new THREE.DirectionalLight(0xffffff, 2.2);
keyLight.position.set(1.0, 2.0, 1.5).normalize();
scene.add(keyLight);
const fillLight = new THREE.DirectionalLight(0x88bbff, 1.2);
fillLight.position.set(-1.0, 1.5, -2.0).normalize();
scene.add(fillLight);

// Controls for camera framing
const controls = new OrbitControls(camera, renderer.domElement);
controls.enabled = false;
controls.enableDamping = false;
controls.target.set(0, 1.20, 0);
controls.update();

const loader = new GLTFLoader();
loader.register((parser) => new VRMLoaderPlugin(parser));

let currentVrm: VRM | null = null;
let rafId = 0;
let running = false;
let disposed = false;
let speaking = false;
let mouthTarget: MouthValues = [0, 0, 0, 0, 0];
const mouthCurrent: MouthValues = [0, 0, 0, 0, 0];
let emotion: EmotionName = 'neutral';
let emotionWeight = 0;
let blinkUntil = 0;
let nextBlinkAt = performance.now() + 4000;
let previousFrameAt = performance.now();

// Humanoid Raw Bones
let headBone: THREE.Object3D | null = null;
let chestBone: THREE.Object3D | null = null;
let rawLeftUpperArm: THREE.Object3D | null = null;
let rawRightUpperArm: THREE.Object3D | null = null;
let rawLeftLowerArm: THREE.Object3D | null = null;
let rawRightLowerArm: THREE.Object3D | null = null;

let headBaseRotation = new THREE.Euler();
let chestBaseScale = new THREE.Vector3(1, 1, 1);

// Natural Resting Pose Angles (verified from PC test bench)
const targetArmZ = 1.30;
const targetArmForwardX = 0.15;
const targetElbowY = 0.00;

const clamp01 = (value: number): number => Math.max(0, Math.min(1, Number.isFinite(value) ? value : 0));
const randomBlinkDelay = (): number => 3000 + Math.random() * 2000;

function resize(): void {
  const width = Math.max(1, window.innerWidth || root.clientWidth);
  const height = Math.max(1, window.innerHeight || root.clientHeight);
  renderer.setSize(width, height, true);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
}

function clearCurrentVrm(): void {
  if (!currentVrm) return;
  scene.remove(currentVrm.scene);
  VRMUtils.deepDispose(currentVrm.scene);
  currentVrm = null;
  headBone = null;
  chestBone = null;
  rawLeftUpperArm = null;
  rawRightUpperArm = null;
  rawLeftLowerArm = null;
  rawRightLowerArm = null;
}

function applyPoseDirect(): void {
  if (rawLeftUpperArm) {
    rawLeftUpperArm.rotation.set(targetArmForwardX, 0, targetArmZ);
  }
  if (rawRightUpperArm) {
    rawRightUpperArm.rotation.set(targetArmForwardX, 0, -targetArmZ);
  }
  if (rawLeftLowerArm) {
    rawLeftLowerArm.rotation.set(0, targetElbowY, 0);
  }
  if (rawRightLowerArm) {
    rawRightLowerArm.rotation.set(0, -targetElbowY, 0);
  }
}

function frameModel(): void {
  const headPos = headBone ? headBone.getWorldPosition(new THREE.Vector3()) : new THREE.Vector3(0, 1.35, 0);
  controls.target.set(headPos.x, headPos.y - 0.15, headPos.z);
  // Match PC test bench Grok Ani bust framing
  camera.position.set(headPos.x, headPos.y - 0.05, headPos.z + 1.25);
  camera.near = 0.1;
  camera.far = 50.0;
  camera.updateProjectionMatrix();
  controls.update();
}

function findBones(vrm: VRM): void {
  const humanoid = vrm.humanoid;
  if (!humanoid) return;

  // CRITICAL: Disable autoUpdateHumanBones so VRMHumanoid doesn't overwrite raw bones with T-pose every frame!
  humanoid.autoUpdateHumanBones = false;

  headBone = humanoid.getRawBoneNode('head') ?? humanoid.getNormalizedBoneNode('head') ?? null;
  chestBone = humanoid.getRawBoneNode('chest')
    ?? humanoid.getRawBoneNode('upperChest')
    ?? humanoid.getNormalizedBoneNode('chest')
    ?? humanoid.getNormalizedBoneNode('upperChest')
    ?? null;

  rawLeftUpperArm = humanoid.getRawBoneNode('leftUpperArm') ?? null;
  rawRightUpperArm = humanoid.getRawBoneNode('rightUpperArm') ?? null;
  rawLeftLowerArm = humanoid.getRawBoneNode('leftLowerArm') ?? null;
  rawRightLowerArm = humanoid.getRawBoneNode('rightLowerArm') ?? null;

  if (headBone) headBaseRotation.copy(headBone.rotation);
  if (chestBone) chestBaseScale.copy(chestBone.scale);

  applyPoseDirect();
}

function applyEmotion(): void {
  const manager = currentVrm?.expressionManager;
  if (!manager) return;

  const presets: EmotionName[] = ['happy', 'sad', 'angry', 'relaxed', 'surprised'];
  for (const preset of presets) manager.setValue(preset, preset === emotion ? emotionWeight : 0);
  manager.setValue('neutral', emotion === 'neutral' ? emotionWeight : 0);
}

function updateBlink(now: number): void {
  const manager = currentVrm?.expressionManager;
  if (!manager) return;
  if (now >= nextBlinkAt && now >= blinkUntil) {
    blinkUntil = now + 140;
    nextBlinkAt = now + randomBlinkDelay();
  }
  const blinkWeight = now < blinkUntil ? Math.sin(((blinkUntil - now) / 140) * Math.PI) : 0;
  manager.setValue('blink', Math.max(0, blinkWeight));
}

function animate(now: number): void {
  if (!running || disposed) return;
  const dt = Math.max(0, Math.min((now - previousFrameAt) / 1000, 0.1));
  previousFrameAt = now;

  for (let i = 0; i < mouthCurrent.length; i++) {
    mouthCurrent[i] += (mouthTarget[i] - mouthCurrent[i]) * (1 - Math.exp(-dt * 18));
  }
  const manager = currentVrm?.expressionManager;
  if (manager) {
    manager.setValue('aa', mouthCurrent[0]);
    manager.setValue('ih', mouthCurrent[1]);
    manager.setValue('ou', mouthCurrent[2]);
    manager.setValue('ee', mouthCurrent[3]);
    manager.setValue('oh', mouthCurrent[4]);
  }

  // Idle head drift
  if (headBone) {
    headBone.rotation.x = headBaseRotation.x + Math.sin(now * 0.00055) * 0.018;
    headBone.rotation.y = headBaseRotation.y + Math.sin(now * 0.00031) * 0.028;
  }
  // Breathing scale
  if (chestBone) {
    const breath = speaking ? 0.006 : 0.012;
    const scaleY = chestBaseScale.y * (1 + Math.sin(now * 0.0018) * breath);
    chestBone.scale.y = scaleY;
  }

  // Gentle arm breathing sway
  if (rawLeftUpperArm && rawRightUpperArm) {
    const sway = Math.sin(now * 0.0018) * 0.015;
    rawLeftUpperArm.rotation.set(targetArmForwardX, 0, targetArmZ + sway);
    rawRightUpperArm.rotation.set(targetArmForwardX, 0, -targetArmZ - sway);
  }
  if (rawLeftLowerArm && rawRightLowerArm) {
    rawLeftLowerArm.rotation.set(0, targetElbowY, 0);
    rawRightLowerArm.rotation.set(0, -targetElbowY, 0);
  }

  updateBlink(now);
  if (currentVrm) currentVrm.update(dt);
  controls.update();
  renderer.render(scene, camera);
  rafId = requestAnimationFrame(animate);
}

function startLoop(): void {
  if (disposed || running) return;
  running = true;
  previousFrameAt = performance.now();
  rafId = requestAnimationFrame(animate);
}

function stopLoop(): void {
  running = false;
  if (rafId) cancelAnimationFrame(rafId);
  rafId = 0;
}

const api: CompanionApi = {
  async loadVrm(url: string): Promise<void> {
    if (disposed) throw new Error('Companion has been disposed');
    try {
      const gltf = await loader.loadAsync(url);
      const vrm = gltf.userData.vrm as VRM | undefined;
      if (!vrm) throw new Error('The selected file is not a supported VRM 0.x or VRM 1.0 model');
      const loadedVrm = vrm;
      VRMUtils.rotateVRM0(loadedVrm);
      clearCurrentVrm();
      currentVrm = loadedVrm;
      scene.add(loadedVrm.scene);
      findBones(loadedVrm);
      frameModel();
      applyEmotion();
      renderer.render(scene, camera);
      console.log('✅ VRM successfully loaded and natural pose applied in companion web!');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      api.onError?.(message);
      window.AndroidBridge?.onError?.(message);
      throw error;
    }
  },
  setMouth(aa, ih, ou, ee, oh): void {
    mouthTarget = [aa, ih, ou, ee, oh].map(clamp01) as MouthValues;
  },
  setEmotion(name, weight): void {
    if (!['happy', 'sad', 'angry', 'relaxed', 'surprised', 'neutral'].includes(name)) return;
    emotion = name;
    emotionWeight = clamp01(weight);
    applyEmotion();
  },
  setSpeaking(value): void {
    speaking = value;
  },
  pause(): void {
    stopLoop();
  },
  resume(): void {
    startLoop();
  },
  dispose(): void {
    if (disposed) return;
    disposed = true;
    stopLoop();
    window.removeEventListener('resize', resize);
    observer.disconnect();
    clearCurrentVrm();
    controls.dispose();
    renderer.dispose();
    renderer.domElement.remove();
  },
};

const observer = new ResizeObserver(resize);
observer.observe(root);
window.addEventListener('resize', resize, { passive: true });
resize();
window.companion = api;
startLoop();
console.log('Companion JS initialized and ready!');
window.AndroidBridge?.onReady?.();
