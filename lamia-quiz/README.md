# LAMIA Hands-Free Learning Hub 🎓

An ultra-lightweight, 0-dependency, high-contrast educational quiz application designed for hands-free navigation using **LAMIA (AI Assistive Head & Gesture Controller)**.

---

## 🎮 How It Works with LAMIA Phone D-PAD

When the LAMIA Android app is connected to your PC, TV, or laptop as a standard **Bluetooth HID Keyboard**, head tilts and facial gestures control this quiz hands-free:

| Gesture / Input | Action in Quiz | Feedback |
| :--- | :--- | :--- |
| **⬆️ Head Tilt UP** | Highlight Option **A** | Glowing Cyan Border + Tone |
| **⬅️ Head Tilt LEFT** | Highlight Option **B** | Glowing Cyan Border + Tone |
| **➡️ Head Tilt RIGHT** | Highlight Option **C** | Glowing Cyan Border + Tone |
| **⬇️ Head Tilt DOWN** | Highlight Option **D** | Glowing Cyan Border + Tone |
| **👄 Mouth Open / Jaw Drop** | Confirm Selection (`ENTER`) | Instant Audio + Explanatory Banner |
| **⌨️ TAB Key** | Auto-Cycle to next option | Sequential navigation |

---

## 🌟 Key Accessibility & Performance Features

1. **Auto-Tab & Auto-Focus:**
   * Every new question immediately highlights **Option A** without needing a mouse click.
   * Prominent **Neon Cyan Glow (`border: 3.5px`, `box-shadow`)** and animated **● FOCUSED** badge guarantee high visibility even on large TV screens or under low vision.

2. **Auto-Scan Assistive Mode (Optional):**
   * Toggleable **Auto-Scan** button (`⏱️ Auto-Scan`) automatically advances the highlight across options every 2.5 seconds with an animated countdown bar.
   * Enables users who can only perform **one single gesture** (e.g., mouth open / jaw drop) to answer questions with zero neck movement!

3. **Instant Zero-Latency Feedback:**
   * **Synthesized Web Audio:** High-fidelity tones (navigation blip, success triad arpeggio, low buzz) generated entirely on-device via `AudioContext` with 0ms lag and zero external audio file downloads.
   * **AAC Text-to-Speech:** Uses native `window.speechSynthesis` to read questions, options, and explanations aloud for auditory learning.

4. **10 Multi-Subject Educational Questions:**
   * Covers Mathematics, Animals, English Antonyms, Physics (Forces), Chemistry (Formulas), Astronomy, Biology (Anatomy), General Science (States of Matter), Geography (Oceans), and Grammar (Parts of Speech).

---

## 🚀 How to Run

Simply double-click `index.html` or open it in any web browser (Chrome, Edge, Firefox, Android Chrome, Smart TV Browser).  
No installation, no npm, and no web server required!
