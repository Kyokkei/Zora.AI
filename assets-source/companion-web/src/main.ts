import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import {
  VRMLoaderPlugin,
  VRMUtils,
  type VRM,
  type VRMPose,
} from '@pixiv/three-vrm';

type EmotionName = 'happy' | 'sad' | 'angry' | 'relaxed' | 'surprised' | 'neutral';
type MouthValues = [number, number, number, number, number];

type CompanionApi = {
  loadVrm(url: string): Promise<void>;
  setMouth(aa: number, ih: number, ou: number, ee: number, oh: number): void;
  setEmotion(name: EmotionName, weight: number): void;
  setSpeaking(speaking: boolean): void;
  wave(): void;
  react(): void;
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
renderer.domElement.addEventListener('pointerup', () => {
  react();
  wave();
});

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
let waveStartedAt = 0;
let reactionStartedAt = 0;
let mouthTarget: MouthValues = [0, 0, 0, 0, 0];
const mouthCurrent: MouthValues = [0, 0, 0, 0, 0];
let emotion: EmotionName = 'neutral';
let emotionWeight = 0;
let blinkUntil = 0;
let nextBlinkAt = performance.now() + 4000;
let previousFrameAt = performance.now();
let humanoid: VRM['humanoid'] | null = null;
let normalizedRestPose: VRMPose | null = null;
let animatedPose: VRMPose | null = null;
let chestBone: THREE.Object3D | null = null;
const chestBaseScale = new THREE.Vector3(1, 1, 1);
const upAxis = new THREE.Vector3(0, 0, 1);
const horizontalAxis = new THREE.Vector3(1, 0, 0);
const rightFingerBones: Array<keyof VRMPose> = [
  'rightThumbProximal',
  'rightIndexProximal',
  'rightMiddleProximal',
  'rightRingProximal',
  'rightLittleProximal',
];

const clamp01 = (value: number): number => Math.max(0, Math.min(1, Number.isFinite(value) ? value : 0));
const randomBlinkDelay = (): number => 3000 + Math.random() * 2000;

function resize(): void {
  const width = Math.max(1, window.innerWidth || root.clientWidth);
  const height = Math.max(1, window.innerHeight || root.clientHeight);
  renderer.setSize(width, height, true);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  if (currentVrm) frameModel();
}

function clearCurrentVrm(): void {
  if (!currentVrm) return;
  scene.remove(currentVrm.scene);
  VRMUtils.deepDispose(currentVrm.scene);
  currentVrm = null;
  humanoid = null;
  normalizedRestPose = null;
  animatedPose = null;
  chestBone = null;
}

function clonePose(pose: VRMPose): VRMPose {
  return Object.fromEntries(
    Object.entries(pose).map(([boneName, transform]) => [
      boneName,
      {
        ...transform,
        ...(transform?.position ? { position: [...transform.position] } : {}),
        ...(transform?.rotation ? { rotation: [...transform.rotation] } : {}),
      },
    ]),
  ) as VRMPose;
}

const poseEuler = new THREE.Euler();
const poseOffset = new THREE.Quaternion();
const poseRotation = new THREE.Quaternion();

function writePoseEuler(
  pose: VRMPose,
  restPose: VRMPose,
  boneName: keyof VRMPose,
  x: number,
  y: number,
  z: number,
): void {
  const baseRotation = restPose[boneName]?.rotation;
  const transform = pose[boneName];
  if (!baseRotation || !transform) return;
  poseEuler.set(x, y, z, 'XYZ');
  poseOffset.setFromEuler(poseEuler);
  poseRotation.set(...baseRotation).multiply(poseOffset);
  transform.rotation = [poseRotation.x, poseRotation.y, poseRotation.z, poseRotation.w];
}

function smoothStep(start: number, end: number, value: number): number {
  const t = clamp01((value - start) / (end - start));
  return t * t * (3 - 2 * t);
}

function frameModel(): void {
  const bounds = new THREE.Box3().setFromObject(currentVrm?.scene ?? new THREE.Group());
  if (bounds.isEmpty()) return;
  const size = bounds.getSize(new THREE.Vector3());
  const center = bounds.getCenter(new THREE.Vector3());
  const verticalFov = THREE.MathUtils.degToRad(camera.fov);
  const aspect = Math.max(0.35, camera.aspect);
  const verticalDistance = (size.y * 0.52) / Math.tan(verticalFov / 2);
  const horizontalDistance = (size.x * 0.52) / (Math.tan(verticalFov / 2) * aspect);
  const distance = Math.max(1.2, verticalDistance, horizontalDistance);
  controls.target.set(center.x, bounds.min.y + size.y * 0.53, center.z);
  camera.position.set(controls.target.x, controls.target.y, controls.target.z + distance);
  camera.near = Math.max(0.01, distance / 100);
  camera.far = Math.max(50, distance * 10);
  camera.updateProjectionMatrix();
  controls.update();
}

function findBones(vrm: VRM): void {
  humanoid = vrm.humanoid;
  if (!humanoid) return;

  humanoid.autoUpdateHumanBones = true;
  normalizedRestPose = clonePose(humanoid.getNormalizedPose());
  animatedPose = clonePose(normalizedRestPose);
  chestBone = humanoid.getRawBoneNode('chest')
    ?? humanoid.getRawBoneNode('upperChest')
    ?? null;
  if (chestBone) chestBaseScale.copy(chestBone.scale);
}

function wave(): void {
  waveStartedAt = performance.now();
  reactionStartedAt = waveStartedAt;
}

function react(): void {
  reactionStartedAt = performance.now();
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

  const reactionAge = now - reactionStartedAt;
  const reactionWeight = reactionAge >= 0 && reactionAge < 900
    ? 1 - reactionAge / 900
    : 0;
  const waveAge = now - waveStartedAt;
  const waveRaise = smoothStep(0, 360, waveAge);
  const waveLower = smoothStep(2050, 2450, waveAge);
  const raisedArm = waveRaise * (1 - waveLower);
  const waveOscillation = waveAge >= 360 && waveAge < 2050
    ? Math.sin((waveAge - 360) * 0.018)
    : 0;
  const speakingGesture = speaking ? Math.sin(now * 0.004) * 0.035 : 0;

  // Compose every pose from the imported normalized rest pose so repeated gestures cannot drift.
  if (humanoid && normalizedRestPose && animatedPose) {
    const pose = animatedPose;
    const idleSway = Math.sin(now * 0.0018) * 0.025;
    writePoseEuler(pose, normalizedRestPose, 'leftUpperArm', 0.04, 0, -1.05 + idleSway);
    writePoseEuler(
      pose,
      normalizedRestPose,
      'rightUpperArm',
      0.04 + raisedArm * 0.22,
      0,
      1.05 - raisedArm * 2.1 - speakingGesture,
    );
    writePoseEuler(pose, normalizedRestPose, 'rightLowerArm', 0, 0, -raisedArm * 1.35);
    writePoseEuler(
      pose,
      normalizedRestPose,
      'rightHand',
      waveOscillation * raisedArm * 0.35,
      waveOscillation * raisedArm * 0.55,
      waveOscillation * raisedArm * 0.45,
    );

    const headDriftX = Math.sin(now * 0.00055) * 0.018 - reactionWeight * 0.045;
    const headDriftY = Math.sin(now * 0.00031) * 0.028 + reactionWeight * 0.09;
    writePoseEuler(pose, normalizedRestPose, 'head', headDriftX, headDriftY, 0);

    const fingerCurl = 0.12 + Math.max(0, waveOscillation) * raisedArm * 0.18;
    for (const finger of rightFingerBones) {
      if (humanoid.getNormalizedBoneNode(finger)) {
        writePoseEuler(pose, normalizedRestPose, finger, fingerCurl, 0, 0);
      }
    }
    humanoid.setNormalizedPose(pose);
  }

  updateBlink(now);
  if (currentVrm) currentVrm.update(dt);
  if (chestBone) {
    const breath = speaking ? 0.006 : 0.012;
    chestBone.scale.y = chestBaseScale.y * (1 + Math.sin(now * 0.0018) * breath);
  }
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
      loadedVrm.update(0);
      loadedVrm.scene.updateMatrixWorld(true);
      frameModel();
      applyEmotion();
      wave();
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
  wave,
  react,
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
