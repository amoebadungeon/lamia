const QUIZ_QUESTIONS = [
  {
    id: 1,
    subject: "MATHEMATICS",
    question: "What is 7 multiplied by 8 (7 × 8)?",
    options: [
      "54",
      "56",
      "48",
      "64"
    ],
    correct: 1, // B / LEFT
    explanation: "7 times 8 equals 56."
  },
  {
    id: 2,
    subject: "ANIMALS",
    question: "Which resilient animal is famously known as the 'Ship of the Desert'?",
    options: [
      "Camel",
      "Horse",
      "Elephant",
      "Cheetah"
    ],
    correct: 0, // A / UP
    explanation: "Camels can travel long distances across arid deserts without water."
  },
  {
    id: 3,
    subject: "ENGLISH",
    question: "What is the exact antonym (opposite) of the word 'Generous'?",
    options: [
      "Polite",
      "Honest",
      "Selfish",
      "Patient"
    ],
    correct: 2, // C / RIGHT
    explanation: "Selfish is the direct opposite of generous."
  },
  {
    id: 4,
    subject: "PHYSICS",
    question: "Which fundamental force pulls falling objects toward the center of the Earth?",
    options: [
      "Friction",
      "Magnetism",
      "Tension",
      "Gravity"
    ],
    correct: 3, // D / DOWN
    explanation: "Gravity attracts mass toward the center of Earth."
  },
  {
    id: 5,
    subject: "CHEMISTRY",
    question: "What is the universal chemical molecular formula for pure water?",
    options: [
      "CO₂ (Carbon Dioxide)",
      "H₂O (Water)",
      "NaCl (Salt)",
      "O₂ (Oxygen Gas)"
    ],
    correct: 1, // B / LEFT
    explanation: "Water consists of two Hydrogen atoms bonded to one Oxygen atom."
  },
  {
    id: 6,
    subject: "ASTRONOMY",
    question: "Which planet is situated closest to the Sun in our Solar System?",
    options: [
      "Venus",
      "Mars",
      "Mercury",
      "Jupiter"
    ],
    correct: 2, // C / RIGHT
    explanation: "Mercury orbits closest to the Sun, completing an orbit in 88 days."
  },
  {
    id: 7,
    subject: "BIOLOGY",
    question: "Which muscular organ pumps oxygen-rich blood throughout the human body?",
    options: [
      "Heart",
      "Brain",
      "Lungs",
      "Liver"
    ],
    correct: 0, // A / UP
    explanation: "The heart pumps blood through the circulatory system."
  },
  {
    id: 8,
    subject: "GENERAL SCIENCE",
    question: "At what temperature does pure water freeze into solid ice at standard pressure?",
    options: [
      "100° Celsius",
      "32° Celsius",
      "0° Celsius",
      "-10° Celsius"
    ],
    correct: 2, // C / RIGHT
    explanation: "Water freezes at 0 degrees Celsius (32 degrees Fahrenheit)."
  },
  {
    id: 9,
    subject: "GEOGRAPHY",
    question: "Which is the largest and deepest ocean on planet Earth?",
    options: [
      "Atlantic Ocean",
      "Indian Ocean",
      "Arctic Ocean",
      "Pacific Ocean"
    ],
    correct: 3, // D / DOWN
    explanation: "The Pacific Ocean covers over 30% of the Earth's surface area."
  },
  {
    id: 10,
    subject: "ENGLISH GRAMMAR",
    question: "Identify the action VERB in this sentence: 'The swift dolphin swims gracefully.'",
    options: [
      "Swift",
      "Swims",
      "Dolphin",
      "Gracefully"
    ],
    correct: 1, // B / LEFT
    explanation: "'Swims' describes the physical action being performed."
  }
];

if (typeof module !== 'undefined' && module.exports) {
  module.exports = QUIZ_QUESTIONS;
}
