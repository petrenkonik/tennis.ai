/** Drill fixtures for the demo mode — copied from apps/api/src/modules/drills/drills.seed.ts. */

export interface DemoDrill {
  slug: string;
  title: string;
  objective?: string;
  setup?: string;
  howTo: string[];
  keyPoints: string[];
  progression?: string;
  repsSets?: string;
  difficulty?: string;
  targetMetrics: string[];
}

export const DEMO_DRILLS: DemoDrill[] = [
  {
    slug: 'deep-knee-load-serves',
    title: 'Deep knee load for serves',
    objective: 'Build a deeper, more explosive knee bend during the serve loading phase.',
    setup: 'Service line, basket of balls, camera on a tripod at 45° from the baseline.',
    howTo: [
      'Stand in your normal serve stance.',
      'Exaggerate the knee bend at the trophy position (pause 1 second).',
      'Drive up through the legs into contact.',
      'Hit 10 serves focusing only on leg drive.',
    ],
    keyPoints: ['Feel the weight in the heels', 'Keep the chest up at the trophy', 'Explode up, not back'],
    progression: 'Add a target cone in the service box and track first-serve percentage.',
    repsSets: '3x10 serves',
    difficulty: 'intermediate',
    targetMetrics: ['KNEE_FLEXION', 'DOM_KNEE', 'NDOM_KNEE', 'M3_TRIPLE_EXTENSION_LEG'],
  },
  {
    slug: 'hip-extension-drive',
    title: 'Hip extension drive',
    objective: 'Open the ankle–hip–shoulder line into the pro range (110–170°) at the drive phase.',
    setup: 'Baseline, wall or fence for balance support on the first sets.',
    howTo: [
      'Shadow serves pausing at the trophy.',
      'Push the front hip up and forward as you extend.',
      'Hold the extension for a beat before the swing.',
    ],
    keyPoints: ['Full leg extension before contact', 'Hips level — do not collapse'],
    progression: 'Progress from shadow to toss + full serve.',
    repsSets: '4x8 reps',
    difficulty: 'intermediate',
    targetMetrics: ['M3_TRIPLE_EXTENSION_HIP', 'M3_TRIPLE_EXTENSION_LEG'],
  },
  {
    slug: 'consistent-toss-drill',
    title: 'Consistent toss',
    objective: 'Smooth, repeatable toss at 1.5–6.0 m/s with a straight tossing arm (152–180°).',
    setup: 'A line on the court as a toss target reference.',
    howTo: [
      'Extend the tossing arm fully; release at eye level.',
      'Let the ball peak at 1.8–2.3x your height.',
      'Catch the ball in the same spot 10 times in a row.',
    ],
    keyPoints: ['Arm straight at release', 'No wrist flick', 'Ball peaks slightly in front'],
    progression: 'Toss + freeze at the trophy, then full serves.',
    repsSets: '3x10 tosses',
    difficulty: 'beginner',
    targetMetrics: ['SERVE_M2_TOSS_ARM_ANGLE', 'SERVE_M2_TOSS_ARM_SPEED', 'M5_TOSS_HEIGHT_RATIO'],
  },
  {
    slug: 'unit-turn-wall-drills',
    title: 'Unit turn against the wall',
    objective: 'Coil the shoulders 10–30° past the hips during the groundstroke unit turn.',
    setup: 'Flat wall, mark a contact-height square with tape.',
    howTo: [
      'Stand sideways; rotate shoulders while the hips stay closed.',
      'Shadow 20 slow unit turns.',
      'Hit 20 balls off the wall keeping the coil.',
    ],
    keyPoints: ['Shoulders turn first', 'Hips stay closed a beat longer', 'Chin over the front shoulder'],
    progression: 'Increase tempo, then move to live feeds.',
    repsSets: '3x20 turns',
    difficulty: 'beginner',
    targetMetrics: ['M2_UNIT_TURN', 'M4_TRUNK_ROTATION'],
  },
  {
    slug: 'contact-point-spacing',
    title: 'Contact point spacing',
    objective: 'Hit the ball 14–72 cm from the shoulder line instead of crowding or reaching.',
    setup: 'Cones or ball tube as a spacing reference on the court.',
    howTo: [
      'Place the ball at arm-length + one hand from the body.',
      'Hit 20 controlled forehands at the fixed spacing.',
      'Repeat on the backhand side.',
    ],
    keyPoints: ['Move feet, not just arm', 'Contact out in front', 'Stable head through impact'],
    progression: 'Add movement: shuffle in/out between shots.',
    repsSets: '4x15 balls',
    difficulty: 'beginner',
    targetMetrics: ['M5_CONTACT_POINT_SHOULDER_DISTANCE', 'M4_BODY_ALIGNMENT'],
  },
  {
    slug: 'wrist-acceleration-serves',
    title: 'Wrist acceleration snap',
    objective: 'Reach 10–25 m/s wrist speed at contact with a relaxed whip, not a stiff push.',
    setup: 'Service box targets; optional speed radar app.',
    howTo: [
      'Loose arm — shake-out drills between sets.',
      'Focus on the final snap: pronate fully.',
      'Aim at the T first, then wide.',
    ],
    keyPoints: ['Relaxed grip pressure', 'Full pronation', 'Follow through across the body'],
    progression: 'Track speeds weekly; target the 10–25 m/s band consistently.',
    repsSets: '3x12 serves',
    difficulty: 'advanced',
    targetMetrics: ['M5_WRIST_SPEED'],
  },
];
