// ================= LAMIA LEARN CORE CONTROLLER =================

class LamiaQuizApp {
  constructor() {
    this.questions = QUIZ_QUESTIONS;
    this.currentIndex = 0;
    this.score = 0;
    this.currentFocusedOption = 0; // 0: UP (A), 1: LEFT (B), 2: RIGHT (C), 3: DOWN (D)
    this.isInputLocked = false;
    this.isVoiceEnabled = true;
    this.isAutoScanEnabled = false;

    // Auto-Scan interval & timer
    this.scanIntervalMs = 2500;
    this.scanIntervalTimer = null;
    this.scanProgressTimer = null;
    this.scanStartTime = 0;

    // Audio Context (Synthesized 0ms Web Audio)
    this.audioCtx = null;

    // DOM Elements
    this.progressBar = document.getElementById("progressBar");
    this.questionCounter = document.getElementById("questionCounter");
    this.subjectBadge = document.getElementById("subjectBadge");
    this.scoreDisplay = document.getElementById("scoreDisplay");
    this.questionText = document.getElementById("questionText");
    this.feedbackBanner = document.getElementById("feedbackBanner");
    this.feedbackText = document.getElementById("feedbackText");
    this.feedbackIcon = document.getElementById("feedbackIcon");
    this.quizView = document.getElementById("quizView");
    this.resultsView = document.getElementById("resultsView");
    this.finalScoreText = document.getElementById("finalScoreText");
    this.restartBtn = document.getElementById("restartBtn");

    this.toggleSpeechBtn = document.getElementById("toggleSpeechBtn");
    this.speechStatus = document.getElementById("speechStatus");
    this.toggleScanBtn = document.getElementById("toggleScanBtn");
    this.scanStatus = document.getElementById("scanStatus");
    this.scanTimerContainer = document.getElementById("scanTimerBarContainer");
    this.scanTimerBar = document.getElementById("scanTimerBar");

    this.optionCards = [
      document.getElementById("opt-0"),
      document.getElementById("opt-1"),
      document.getElementById("opt-2"),
      document.getElementById("opt-3")
    ];

    this.optionTexts = [
      document.getElementById("opt-text-0"),
      document.getElementById("opt-text-1"),
      document.getElementById("opt-text-2"),
      document.getElementById("opt-text-3")
    ];

    this.init();
  }

  init() {
    this.setupEventListeners();
    this.loadQuestion(0);
  }

  // ================= AUDIO SYNTHESIZER =================
  initAudio() {
    if (!this.audioCtx) {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  playTone(freq, type = 'sine', durationMs = 80, gainValue = 0.1) {
    try {
      this.initAudio();
      if (!this.audioCtx) return;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq, this.audioCtx.currentTime);
      gain.gain.setValueAtTime(gainValue, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.audioCtx.currentTime + (durationMs / 1000));
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start();
      osc.stop(this.audioCtx.currentTime + (durationMs / 1000));
    } catch (e) {
      // Audio not supported or autoplay restricted
    }
  }

  playNavSound() {
    this.playTone(520, 'triangle', 50, 0.08);
  }

  playSuccessSound() {
    // Joyful major triad arpeggio
    setTimeout(() => this.playTone(523.25, 'sine', 100, 0.12), 0);   // C5
    setTimeout(() => this.playTone(659.25, 'sine', 100, 0.12), 100); // E5
    setTimeout(() => this.playTone(783.99, 'sine', 180, 0.14), 200); // G5
  }

  playErrorSound() {
    // Low dual tone buzz
    setTimeout(() => this.playTone(220, 'sawtooth', 120, 0.08), 0);
    setTimeout(() => this.playTone(185, 'sawtooth', 150, 0.08), 100);
  }

  playFanfareSound() {
    const notes = [523.25, 659.25, 783.99, 1046.50];
    notes.forEach((freq, idx) => {
      setTimeout(() => this.playTone(freq, 'triangle', 200, 0.15), idx * 140);
    });
  }

  // ================= TEXT-TO-SPEECH (AAC SHOWCASE) =================
  speak(text) {
    if (!this.isVoiceEnabled || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.05;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  }

  // ================= EVENT LISTENERS =================
  setupEventListeners() {
    // Keyboard / D-PAD HID listener
    window.addEventListener("keydown", (e) => this.handleKeyDown(e));

    // Mouse / Touch interaction on option cards
    this.optionCards.forEach((card, index) => {
      card.addEventListener("click", () => {
        if (this.isInputLocked) return;
        this.setHighlight(index);
        this.submitAnswer(index);
      });
    });

    // Toggle speech button
    this.toggleSpeechBtn.addEventListener("click", () => {
      this.isVoiceEnabled = !this.isVoiceEnabled;
      this.speechStatus.textContent = `Voice: ${this.isVoiceEnabled ? 'ON' : 'OFF'}`;
      this.toggleSpeechBtn.classList.toggle("active", this.isVoiceEnabled);
      if (!this.isVoiceEnabled && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    });

    // Toggle auto-scan button
    this.toggleScanBtn.addEventListener("click", () => {
      this.toggleAutoScan();
    });

    // Restart button
    this.restartBtn.addEventListener("click", () => {
      this.restartQuiz();
    });
  }

  // ================= D-PAD & KEYBOARD NAVIGATION =================
  handleKeyDown(e) {
    // If results view is showing, Enter or Space restarts
    if (!this.resultsView.classList.contains("hide")) {
      if (e.key === "Enter" || e.key === " " || e.key === "r" || e.key === "R") {
        e.preventDefault();
        this.restartQuiz();
      }
      return;
    }

    if (this.isInputLocked) return;

    switch (e.key) {
      // ⬆️ UP tilt -> Option A (Index 0)
      case "ArrowUp":
      case "w":
      case "W":
      case "1":
        e.preventDefault();
        this.setHighlight(0);
        this.playNavSound();
        break;

      // ⬅️ LEFT tilt -> Option B (Index 1)
      case "ArrowLeft":
      case "a":
      case "A":
      case "2":
        e.preventDefault();
        this.setHighlight(1);
        this.playNavSound();
        break;

      // ➡️ RIGHT tilt -> Option C (Index 2)
      case "ArrowRight":
      case "d":
      case "D":
      case "3":
        e.preventDefault();
        this.setHighlight(2);
        this.playNavSound();
        break;

      // ⬇️ DOWN tilt -> Option D (Index 3)
      case "ArrowDown":
      case "s":
      case "S":
      case "4":
        e.preventDefault();
        this.setHighlight(3);
        this.playNavSound();
        break;

      // Tab key -> Auto-cycle to next option
      case "Tab":
        e.preventDefault();
        if (e.shiftKey) {
          // Shift+Tab -> Previous option
          const prevIndex = (this.currentFocusedOption + 3) % 4;
          this.setHighlight(prevIndex);
        } else {
          // Tab -> Next option
          const nextIndex = (this.currentFocusedOption + 1) % 4;
          this.setHighlight(nextIndex);
        }
        this.playNavSound();
        break;

      // Mouth Open / Jaw Drop -> Enter or Space triggers OK
      case "Enter":
      case " ":
        e.preventDefault();
        this.submitAnswer(this.currentFocusedOption);
        break;

      default:
        break;
    }
  }

  // ================= HIGHLIGHT & AUTO-TAB STATE =================
  setHighlight(index) {
    this.currentFocusedOption = index;

    this.optionCards.forEach((card, idx) => {
      if (idx === index) {
        card.classList.add("focused");
      } else {
        card.classList.remove("focused");
      }
    });

    // Reset auto-scan progress bar if scanning is active
    if (this.isAutoScanEnabled) {
      this.resetScanTimer();
    }
  }

  // ================= AUTO-TAB SCANNING (ASSISTIVE MODE) =================
  toggleAutoScan() {
    this.isAutoScanEnabled = !this.isAutoScanEnabled;
    this.scanStatus.textContent = `Auto-Scan: ${this.isAutoScanEnabled ? 'ON' : 'OFF'}`;
    this.toggleScanBtn.classList.toggle("active", this.isAutoScanEnabled);

    if (this.isAutoScanEnabled) {
      this.scanTimerContainer.classList.remove("hide");
      this.startAutoScan();
    } else {
      this.scanTimerContainer.classList.add("hide");
      this.stopAutoScan();
    }
  }

  startAutoScan() {
    this.stopAutoScan();
    this.scanStartTime = performance.now();

    this.scanIntervalTimer = setInterval(() => {
      if (this.isInputLocked) return;
      const nextIndex = (this.currentFocusedOption + 1) % 4;
      this.setHighlight(nextIndex);
      this.playNavSound();
    }, this.scanIntervalMs);

    this.scanProgressTimer = setInterval(() => {
      if (this.isInputLocked) return;
      const elapsed = (performance.now() - this.scanStartTime) % this.scanIntervalMs;
      const percentage = (elapsed / this.scanIntervalMs) * 100;
      this.scanTimerBar.style.width = `${percentage}%`;
    }, 40);
  }

  resetScanTimer() {
    this.scanStartTime = performance.now();
    if (this.scanTimerBar) {
      this.scanTimerBar.style.width = "0%";
    }
    if (this.scanIntervalTimer) {
      clearInterval(this.scanIntervalTimer);
      this.scanIntervalTimer = setInterval(() => {
        if (this.isInputLocked) return;
        const nextIndex = (this.currentFocusedOption + 1) % 4;
        this.setHighlight(nextIndex);
        this.playNavSound();
      }, this.scanIntervalMs);
    }
  }

  stopAutoScan() {
    if (this.scanIntervalTimer) clearInterval(this.scanIntervalTimer);
    if (this.scanProgressTimer) clearInterval(this.scanProgressTimer);
    this.scanIntervalTimer = null;
    this.scanProgressTimer = null;
    if (this.scanTimerBar) {
      this.scanTimerBar.style.width = "0%";
    }
  }

  // ================= LOAD QUESTION =================
  loadQuestion(index) {
    this.currentIndex = index;
    const q = this.questions[index];

    // Reset card states
    this.optionCards.forEach((card) => {
      card.className = "option-card";
    });

    // Hide feedback banner
    this.feedbackBanner.className = "feedback-banner hide";

    // Set texts
    this.questionCounter.textContent = `Question ${index + 1} / ${this.questions.length}`;
    this.subjectBadge.textContent = q.subject;
    this.scoreDisplay.textContent = `Score: ${this.score} / ${this.questions.length}`;
    this.questionText.textContent = q.question;

    q.options.forEach((optText, optIdx) => {
      this.optionTexts[optIdx].textContent = optText;
    });

    // Update main progress bar
    const progressPercent = ((index + 1) / this.questions.length) * 100;
    this.progressBar.style.width = `${progressPercent}%`;

    // AUTO-TAB HIGHLIGHT: Always default highlight to Option 0 (A / UP)
    this.setHighlight(0);
    this.isInputLocked = false;

    // Speak question aloud for AAC
    if (this.isVoiceEnabled) {
      this.speak(`${q.subject}. ${q.question}`);
    }

    // Restart auto-scan if enabled
    if (this.isAutoScanEnabled) {
      this.startAutoScan();
    }
  }

  // ================= SUBMIT ANSWER =================
  submitAnswer(selectedIdx) {
    if (this.isInputLocked) return;
    this.isInputLocked = true;
    this.stopAutoScan();

    const q = this.questions[this.currentIndex];
    const isCorrect = selectedIdx === q.correct;
    const selectedCard = this.optionCards[selectedIdx];
    const correctCard = this.optionCards[q.correct];

    const directionLabels = ["A (UP)", "B (LEFT)", "C (RIGHT)", "D (DOWN)"];

    if (isCorrect) {
      this.score++;
      this.scoreDisplay.textContent = `Score: ${this.score} / ${this.questions.length}`;
      selectedCard.classList.add("correct");
      this.playSuccessSound();

      this.feedbackBanner.className = "feedback-banner success";
      this.feedbackIcon.textContent = "✓";
      this.feedbackText.textContent = `Correct! ${q.explanation}`;

      if (this.isVoiceEnabled) {
        this.speak(`Correct! ${q.explanation}`);
      }
    } else {
      selectedCard.classList.add("incorrect");
      correctCard.classList.add("correct");
      this.playErrorSound();

      this.feedbackBanner.className = "feedback-banner error";
      this.feedbackIcon.textContent = "✕";
      this.feedbackText.textContent = `Correct answer is ${directionLabels[q.correct]}: ${q.options[q.correct]}. ${q.explanation}`;

      if (this.isVoiceEnabled) {
        this.speak(`Not quite. The correct answer was ${directionLabels[q.correct]}.`);
      }
    }

    // Wait 1.8 seconds then advance to next question or complete
    setTimeout(() => {
      if (this.currentIndex + 1 < this.questions.length) {
        this.loadQuestion(this.currentIndex + 1);
      } else {
        this.showResults();
      }
    }, 1800);
  }

  // ================= COMPLETION SCREEN =================
  showResults() {
    this.stopAutoScan();
    this.quizView.classList.add("hide");
    this.resultsView.classList.remove("hide");

    this.finalScoreText.textContent = `You scored ${this.score} out of ${this.questions.length} (${Math.round((this.score / this.questions.length) * 100)}%)`;
    this.playFanfareSound();

    if (this.isVoiceEnabled) {
      this.speak(`Congratulations! You completed the quiz with ${this.score} out of ${this.questions.length} correct.`);
    }

    // Automatically focus restart button
    this.restartBtn.focus();
  }

  // ================= RESTART =================
  restartQuiz() {
    this.score = 0;
    this.currentIndex = 0;
    this.resultsView.classList.add("hide");
    this.quizView.classList.remove("hide");
    this.loadQuestion(0);
  }
}

// Instantiate on DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
  window.lamiaQuiz = new LamiaQuizApp();
});
