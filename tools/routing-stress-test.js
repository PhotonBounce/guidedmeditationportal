'use strict';
function any(input, kws) { return kws.some(function(k) { return input.includes(k); }); }
function anyWord(input, kws) {
  return kws.some(function(k) {
    return new RegExp('\\b' + k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b').test(input);
  });
}

function route(lower, lastTopic) {
  // 1. crisis — always first
  if(any(lower,["suicidal","suicide","want to die","dont want to live","end it all","end my life",
    "kill myself","self harm","self-harm","hurt myself","no reason to live","better off dead",
    "take my own life","take my life","life isnt worth living","not worth living",
    "dont want to be here anymore","dont want to exist",
    "unalive","cutting myself","overdose","cant go on like this",
    "feel like a burden","like such a burden","being a burden","am a burden","i'm a burden","i am a burden",
    "better off without me","world would be better without me",
    "nothing to live for","nothing left to live for",
    "want to disappear","wish i could disappear","just want to disappear",
    "wish i was dead","wish i were dead",
    "no point going on","no point carrying on","no point in carrying on",
    "can't carry on","cant carry on"])) return "crisis";
  // 2. followUp
  if(lastTopic.length > 0 && (any(lower,["please","okay","go on","continue","next",
    "tell me more","walk me through","guide me","show me","how do i","teach me",
    "what do i do","let's do it","lets do it","let's go","lets go","go ahead",
    "sounds good","i'd like that","i would like that","absolutely","of course",
    "why not"]) || anyWord(lower,["yes","sure","ok"]))) return "followUp";
  // 3. greeting
  if(any(lower,["hello","good morning","good afternoon","good evening","good night",
    "howdy","greetings","what's up","whats up"]) ||
    anyWord(lower,["hi","hey","yo","sup"])) return "greeting";
  // 3.5: [topic] timer phrases — before all topic routes
  if(any(lower,["sleep timer","nap timer","meditation timer","meditate timer",
    "breathing timer","breathwork timer","relaxation timer","yoga timer",
    "anxiety timer","focus timer","energy timer"])) return "timer";
  // 4. baby+sleep compound (before bare sleep)
  if(any(lower,["baby sleep","baby won't sleep","baby can't sleep","baby keeps waking",
    "infant sleep","toddler sleep","toddler won't sleep","child won't sleep"])) return "baby";
  // 5. sleep paralysis (before bare sleep)
  if(any(lower,["sleep paralysis","paralyzed in sleep","paralyzed while sleeping",
    "awake but can't move","awake but cant move","frozen while sleeping","frozen when waking",
    "frozen on waking","frozen waking up","cant move when waking","can't move when waking",
    "woke up couldn't move","woke up cant move"])) return "sleepParalysis";
  // 6. sleep
  if(any(lower,["sleep","insomnia","cant sleep","can't sleep","falling asleep",
    "bedtime","tired","fatigue","exhausted","exhaustion","wide awake","cant switch off","can't switch off",
    "racing mind","racing thoughts","napping","night shift","shift work",
    "mind won't stop","mind wont stop","nightmare","nightmares","bad dreams",
    "jet lag","jet lagged","jet-lagged","restless","restlessness",
    "woke up at","keep waking","3am","4am","middle of the night",
    "drift off","drifting off","can't drift",
    "counting sheep","wired at night","wired tonight","can't wind down","cant wind down",
    "light sleeper","heavy sleeper","sleep hygiene","body clock","circadian","night terrors",
    "night sweats","hot flashes","hot flush","menopause","perimenopause",
    "can't turn off","cant turn off","turn my brain off",
    "brain won't stop","brain wont stop",
    "never feel rested","never fully rested","wake up exhausted",
    "wake up tired","wake up unrested"]) ||
    anyWord(lower,["nap"])) return "sleep";
  // 7. focus
  if(any(lower,["focus","study","concentrat","productivity","procrastinat","writing","brain fog","adhd",
    "foggy","mental clarity","sharp","clear mind","attention","distract",
    "multitasking","information overload",
    "doom scrolling","doomscrolling","mindless scrolling","phone addiction",
    "screen addiction","endless scrolling","too much screen",
    "mental block","writer's block","writers block","creative block",
    "brain freeze","can't think straight","cant think straight",
    "mind blank","mind went blank","mind has gone blank",
    "analysis paralysis","decision paralysis","overthinking decisions",
    "hyperfocus","hyperfocusing","can't switch tasks",
    "cant switch tasks","task switching"]) ||
    anyWord(lower,["read","code"])) return "focus";
  // 8. energy
  if(any(lower,["energy","energise","energize","wake up","waking up","uplift","motivat",
    "active","exercise","workout","morning boost","morning energy","sluggish",
    "lethargic","cold shower","cold water","ice bath","wim hof",
    "afternoon slump","afternoon crash","2pm slump","post-lunch dip",
    "pick me up","need a boost","feeling flat","flat today",
    "drained","wiped out","run down","worn out","no drive"])) return "energy";
  // 9. relax
  if(any(lower,["relax","calm","stress","anxiety","anxious","breathe","unwind","nervous","panic",
    "overthink","overthinking","can't stop thinking","cant stop thinking",
    "intrusive thoughts","ruminating","social anxiety","public speaking",
    "presentation nerves","exam nerves","stage fright","interview nerves","interview anxiety",
    "nerves before","performance anxiety","performance pressure",
    "pounding heart","heart racing","mind keeps wandering","can't stop my mind",
    "cant stop my mind","racing heart","trauma","traumatic","ptsd","post-traumatic",
    "triggered","hyperventilat","chest tight","tight chest","can't breathe","cant breathe",
    "fear","scared","frightened","phobia","afraid",
    "worry","worried","worrying","i worry","constant worry",
    "dissociation","dissociating","dissociated","derealization","depersonalization",
    "feel unreal","feeling unreal","not feeling real","feel detached","ocd","obsessive",
    "compulsive thoughts","freaking out","freak out","losing my mind","losing it",
    "can't cope with","cant cope with","spiralling","spiraling",
    "dwell on","dwelling on","can't stop dwelling","keep dwelling",
    "negative thoughts","negative thinking","negative self-talk",
    "thought spiral","thought spirals","racing thoughts",
    "dread","dreading","sense of dread",
    "hypervigilant","hypervigilance",
    "sunday scaries","anticipatory anxiety",
    "drinking to cope","drink to cope","alcohol to cope","drink to forget",
    "drinking to forget","using alcohol","using drink",
    "catastrophiz","catastrophising","what if thoughts",
    "fight or flight","fight-or-flight","adrenaline spike",
    "on edge","jittery","jitters","butterflies in",
    "trembling","can't stop shaking","cant stop shaking"]) ||
    anyWord(lower,["rest","tense"])) return "relax";
  // 10. tinnitus
  if(any(lower,["tinnitus","ringing in","ear ring","hearing","buzz in my ear"])) return "tinnitus";
  // 11. baby (non-sleep)
  if(any(lower,["baby","infant","newborn","toddler","new parent","new mum","new mom",
    "first time parent","new baby"])) return "baby";
  // 11.5: "how long" meditat queries — must fire before bare meditation route
  if(any(lower,["how long to meditate","how long should i meditate",
    "how many minutes to meditate","how long for meditation","how long should i practice"])) return "timer";
  if(any(lower,["how long have i been meditating","how long have i been practicing",
    "how many times have i meditated","how many sessions have i done"])) return "stats";
  // 12. meditation (track names + traditions)
  if(any(lower,["meditat","mindful","yoga","chakra","mantra",
    "vipassana","tonglen","soham","thien","sumara","muraqaba",
    "hesychasm","dhikr","hitbodedut","zhan zhuang","buddho",
    "sufi","tibetan","qigong","stoic","stoicism","spiritual"]) ||
    anyWord(lower,["zen"])) return "meditation";
  // 13. techniques
  if(any(lower,["technique","breathwork","breathing","body scan","loving-kindness","loving kindness",
    "visualization","how do i meditate","how to meditate","types of meditation","autogenic",
    "box breath","4-7-8","physiological sigh","progressive muscle","metta",
    "self-compassion","self compassion","compassion practice","kind to myself",
    "self esteem","self-esteem","low confidence","build confidence","self-worth",
    "self worth","confidence","stretching","morning routine","bored","boredom"])) return "techniques";
  // 14. sadness
  if(any(lower,["sad","grief","grieving","heartbreak","heartbroken","lonely","alone","loneliness",
    "depressed","depression","cry","crying","sobbing","weeping","in tears","tearing up",
    "burst into tears","bawling","upset","miserable","unhappy","low mood",
    "empty inside","feel empty","feeling empty",
    "hopeless","helpless","despair","despairing","in despair","feel desperate","feeling desperate",
    "devastated","feel devastated",
    "hollow","disconnected","meaningless","no motivation","nothing matters",
    "gaslighting","gaslit","being gaslit","emotional abuse",
    "bipolar","manic","mania","manic episode","hypomania",
    "breakup","broke up","split up",
    "feeling lost","lost and","i feel lost","feel so lost","blue today","can't find","lost myself",
    "bereaved","bereavement","loss of","lost someone",
    "passed away","death of","missing them","miss them so",
    "isolated","feeling isolated","so isolated",
    "got fired","just fired","lost my job","lost their job","laid off",
    "made redundant","partner left me","been left",
    "relationship","divorce","divorc","separation","separated from",
    "i feel low","feel so low","feeling so low","feeling very low",
    "life feels pointless","feels pointless","feel pointless",
    "mood swings","bad mood","my mood","mental health",
    "i'm suffering","need to vent","need to talk","venting",
    "longing","miss him","miss her",
    "betrayed","betrayal","feel betrayed","been betrayed",
    "cheated on","been cheated","trust issues","can't trust",
    "feel like a burden","i'm a burden","am a burden",
    "miscarriage","stillbirth","pregnancy loss","child loss","infertility","lost my baby","lost our baby",
    "feel rejected","feel abandoned","abandoned","rejection","been rejected",
    "feel invisible","feel unseen","feel unloved","unlovable","not loved","no one cares",
    "not great","not so great","not feeling great","not doing great",
    "feeling off","bit off","not myself","off today","not okay","not ok today",
    "not doing ok","not doing well",
    "unwell","not well","not feeling well","feeling unwell",
    "not fine","i'm not fine","im not fine",
    "feeling low","feel low","so low","really low","been feeling low",
    "feeling blue","feel blue","so blue",
    "feeling down","feel down","been feeling down","really down","so down",
    "mourning","in mourning","heartache","missing my ex","miss my ex",
    "no purpose","lack of purpose","feel purposeless","existential",
    "winter blues","lack of sunlight","low in winter",
    "seasonal affective","seasonal depression","sad disorder",
    "widowed","widow","ghosted",
    "empty nest","empty nester","kids moved out","children left home",
    "feel like giving up","want to give up","ready to give up",
    "thinking of giving up","about to give up"]) ||
    anyWord(lower,["numb","died","vent"])) return "sadness";
  // 15. shameGuilt
  if(any(lower,["shame","ashamed","guilt","guilty","embarrassed","humiliated",
    "self-blame","self blame","blame myself","blaming myself",
    "i keep beating myself up","beating myself up",
    "hate my body","body image","feel ugly","look ugly","feel so ugly","i look ugly",
    "feel worthless","worthless","i'm worthless","im worthless","not good enough",
    "feel like a failure","i'm a failure","im a failure","i am a failure",
    "feel inadequate","i feel inadequate","feel unworthy","i feel unworthy",
    "forgive myself","self-forgiveness","self forgiveness",
    "perfectionism","perfectionist","never good enough","hard on myself",
    "i'm not perfect","im not perfect","i am not perfect",
    "nobody's perfect","nobody is perfect","nothing is perfect",
    "too hard on myself","self-critical","self critical",
    "body image issues","body image problem","negative body image",
    "struggle with my body","hate how i look","hate my appearance",
    "eating disorder","disordered eating",
    "anorexia","anorexic","bulimia","bulimic","binge eating","binge and purge",
    "imposter syndrome","impostor syndrome",
    "feel like a fraud","feel like such a fraud",
    "feeling like a fraud","feeling like such a fraud",
    "feel like an imposter","feel like such an imposter",
    "feeling like an imposter"])) return "shameGuilt";
  // 16. overwhelm
  if(any(lower,["overwhelm","overwhelmed","burnout","burnt out","burned out","burning out","too much",
    "cant cope","can't cope","too busy","overloaded","swamped","falling apart",
    "breaking point","can't take","work stress","work anxiety","work is killing me",
    "job stress","feeling stuck","feel stuck","stuck in a rut","stuck in life",
    "deadline","under pressure","work pressure","pressure at work",
    "work-life balance","work life balance","no time for myself","no time for me",
    "people pleaser","people-pleaser","cant say no","can't say no",
    "always putting others first","never put myself first","spread too thin",
    "running on empty","meltdown","having a meltdown","on the edge","at my limit",
    "hit my limit","can't handle it","cant handle it","can't handle this",
    "cant handle this","can't keep up","cant keep up","hostile work",
    "single parent","single mum","single mom","single dad",
    "sole parent","solo parenting","solo parent"]) ||
    anyWord(lower,["toxic"])) return "overwhelm";
  // 17. anger
  if(any(lower,["angry","furious","frustrated","frustration","rage",
    "irritated","irritable","annoyed","wound up","agitated",
    "pissed off","livid","seething","seeing red",
    "lost my temper","losing my temper","lose my temper",
    "about to explode","about to snap","lost it","blow up",
    "want to scream","could scream","need to scream",
    "snapped at","keep snapping","lashing out",
    "resentment","resentful","resentment toward","full of resentment",
    "want to punch","feel like punching","slamming",
    "passive aggressive","passive-aggressive",
    "short fuse","quick temper","bad temper",
    "bitter","bitterness","bitter toward","bitter about",
    "contempt","contemptuous",
    "jealous","jealousy","envy","envious"]) ||
    anyWord(lower,["mad","anger","angered"])) return "anger";
  // 18. vip
  if(any(lower,["vip","upgrade","pro plan","subscription","pricing","plans","cost","buy","purchase"])) return "vip";
  // 19. recommend
  if(any(lower,["recommend","suggest","what should","which sound","best sound",
    "what sound","pick a sound","help me choose"])) return "recommend";
  // 20. positive
  if(any(lower,["thank","thanks","awesome","perfect","love it","amazing","nice"])) return "positive";
  // 21. timer
  if(any(lower,["timer","sleep timer","how long should","how long to meditate","how long for","duration","how many minutes"])) return "timer";
  // 22. alarm
  if(any(lower,["set alarm","set an alarm","morning alarm","alarm for","alarm at",
    "alarm clock","wake alarm","daily alarm","wake me up","schedule alarm"])) return "alarm";
  // 23. play (playRequest — before ambient)
  if(any(lower,["play it","play that","play my","play my fav","play favourite","play favorite",
    "start it","queue it","play something","play now","can you play",
    "put on a","put on some","start playing"])) return "play";
  // 23.5. favorites — before ambient ("saved tracks" contains "tracks" which ambient matches)
  if(any(lower,["my favorites","my favourites","saved tracks","what i saved","what i've saved",
    "favorite tracks","favourite tracks","my saved"])) return "favorites";
  // 24. ambient (track/library)
  if(any(lower,["track","tracks","library","guided","which track","what track","play list","playlist"])) return "ambient";
  // 25. pain
  if(any(lower,["headache","migraine","ache","sore","tension headache","physical","body tension",
    "muscle tension","stiff","tension","chronic pain","chronic illness","fibromyalgia","arthritis","back pain",
    "lower back","neck pain","neck tension","shoulder pain","joint pain",
    "sciatica","period pain","menstrual cramps","cramps",
    "jaw pain","jaw tension","jaw clenching","teeth grinding","grind my teeth","grinding my teeth","bruxism",
    "inflammation","inflammatory","repetitive strain","carpal tunnel","frozen shoulder",
    "painful","pains","in pain"]) || anyWord(lower,["pain"])) return "pain";
  // 26. gratitude
  if(any(lower,["grateful","gratitude","journal","journaling","reflect","reflection",
    "intention","intentions","thankful","thankfulness",
    "appreciate","appreciation","count my blessings","count your blessings",
    "what am i grateful","things i'm grateful","blessings"])) return "gratitude";
  // 27. help
  if(any(lower,["what can you do","how do you work","your features","about spirit",
    "what are you","what is spirit","how can you help"])) return "help";
  // 28. inspire
  if(any(lower,["inspire me","inspiration","quote","affirmation","motivate me",
    "encourage me","daily tip","today's practice","what should i practice",
    "technique of the day","something to try"])) return "inspire";
  // 29. programs
  if(any(lower,["journey","journeys","program","programs","course","guided course",
    "structured","7 day","7-day","5 day","5-day","challenge"])) return "programs";
  // 30. stats
  if(any(lower,["my stats","my streak","my progress","sessions","total time","day streak",
    "how long have i","my history","minutes meditated"])) return "stats";
  // 31. favorites
  if(any(lower,["my favorites","my favourites","saved tracks","what i saved","what i've saved",
    "favorite tracks","favourite tracks","my saved"])) return "favorites";
  // 32. general
  return "general";
}

var tests = [
  // False-positive prevention
  ["i woke up anxious", "sleep", "NOT:followUp", "woke: no ok at word boundary"],
  ["yesterday i meditated", "relax", "NOT:followUp", "yesterday: no yes at word boundary"],
  ["im unsure about this", "relax", "NOT:followUp", "unsure: no sure at word boundary"],
  ["codependency issues", "", "NOT:focus", "codependency: no code at word boundary"],
  ["the bread was good", "", "NOT:focus", "bread: no read at word boundary"],
  ["feeling intense emotions", "", "NOT:relax", "intense: no tense at word boundary"],
  ["interested in breathwork", "", "techniques", "interested has breathwork — routes to techniques"],
  ["frozen in place", "", "NOT:meditation", "frozen: no zen at word boundary"],
  ["made a mistake today", "", "NOT:anger", "made: no mad at word boundary"],
  ["number of sessions", "", "stats", "number + sessions → stats (sessions keyword wins)"],
  ["studying for exams", "", "NOT:sadness", "study → focus, not sadness via died"],

  // Crisis additions (R39)
  ["i want to take my own life", "", "crisis", "crisis: take my own life"],
  ["life isnt worth living", "", "crisis", "crisis: life isnt worth living"],
  ["i am cutting myself", "", "crisis", "crisis: cutting myself"],
  ["i want to unalive myself", "", "crisis", "crisis: unalive"],
  ["cant go on like this anymore", "", "crisis", "crisis: cant go on like this"],

  // FollowUp fix (R40) — greeting now clears lastTopic
  ["yes please", "sleep", "followUp", "yes + sleep context → followUp"],
  ["ok sounds good", "relax", "followUp", "ok + relax context → followUp"],

  // Energy additions (R41)
  ["cold shower in the morning", "", "energy", "cold shower → energy"],
  ["wim hof breathing", "", "energy", "wim hof → energy"],
  ["ice bath after workout", "", "energy", "ice bath → energy"],

  // Relax additions (R41)
  ["performance anxiety before my show", "", "relax", "performance anxiety → relax"],
  ["performance pressure is high", "", "relax", "performance pressure → relax"],

  // Techniques additions (R41)
  ["i need more confidence", "", "techniques", "confidence → techniques"],
  ["morning routine ideas", "", "techniques", "morning routine → techniques"],
  ["stretching practice", "", "techniques", "stretching → techniques"],

  // R43 additions
  ["ocd is ruining my life", "", "relax", "ocd → relax (intrusive thoughts cluster)"],
  ["obsessive thoughts wont stop", "", "relax", "obsessive → relax"],
  ["i have a deadline tomorrow", "", "overwhelm", "deadline → overwhelm"],
  ["so much work pressure", "", "overwhelm", "work pressure → overwhelm"],
  ["im so bored", "", "techniques", "bored → techniques"],
  ["boredom is killing me", "", "techniques", "boredom → techniques"],
  ["i miss him so much", "", "sadness", "miss him → sadness"],
  ["i had a miscarriage", "", "sadness", "miscarriage → sadness"],
  ["struggling with infertility", "", "sadness", "infertility → sadness"],
  ["pregnancy loss is devastating", "", "sadness", "pregnancy loss → sadness"],

  // R45: vent word-boundary fix
  ["i have an event this weekend", "", "NOT:sadness", "event should not trigger sadness via vent"],
  ["lets go on an adventure", "", "NOT:sadness", "adventure should not trigger sadness via vent"],
  ["i want to vent about today", "", "sadness", "standalone vent → sadness"],
  ["i have been venting", "", "sadness", "venting → sadness"],
  // R45: pain word-boundary fix
  ["i enjoy painting as therapy", "", "NOT:pain", "painting does not trigger pain handler"],
  ["my back pain is bad", "", "pain", "pain standalone → handlePain"],
  ["this is so painful", "", "pain", "painful → handlePain"],
  // R46: anger word-boundary fix
  ["that feels dangerous to me", "", "NOT:anger", "danger should not trigger anger via anger substring"],
  ["i feel endangered at work", "", "NOT:anger", "endanger should not trigger anger"],
  ["i feel so much anger", "", "anger", "standalone anger → handleAnger"],
  ["i was really angered by that", "", "anger", "angered → handleAnger"],

  // R44: nap word-boundary fix — snap should NOT route to sleep
  ["i'm about to snap", "", "NOT:sleep", "snap does not contain nap at word boundary"],
  ["i need a nap", "", "sleep", "nap (standalone) → sleep"],
  ["napping this afternoon", "", "sleep", "napping → sleep"],

  // R55: "not perfect" false positive + play route gaps
  ["i'm not perfect", "", "NOT:positive", "not perfect must not trigger positive"],
  ["nobody's perfect", "", "NOT:positive", "nobody's perfect must not trigger positive"],
  ["can you play something for me", "", "play", "can you play → playRequest"],
  ["put on some rain sounds", "", "play", "put on some → playRequest"],
  ["start playing something", "", "play", "start playing → playRequest"],

  // R54: "not great" / "not okay" — must NOT hit positive route
  ["i'm not feeling so great today", "", "NOT:positive", "not great must not trigger positive"],
  ["i'm feeling great today", "", "general", "feeling great → general (isWell branch says 'lovely to hear')"],
  ["not feeling ok", "", "NOT:positive", "not ok → sadness, not positive"],
  ["i'm not doing well", "", "NOT:positive", "not doing well → sadness"],
  ["not myself lately", "", "NOT:positive", "not myself → sadness"],
  ["i'm feeling off today", "", "NOT:positive", "feeling off → sadness"],

  // R69: new routes in simulation — tinnitus/baby/meditation/alarm/ambient/gratitude/help/inspire/programs/favorites
  ["i have tinnitus", "", "tinnitus", "tinnitus → tinnitus"],
  ["ringing in my ears is constant", "", "tinnitus", "ringing in → tinnitus"],
  ["my baby won't sleep at all", "", "baby", "baby won't sleep → baby (compound before bare sleep)"],
  ["i'm a new parent", "", "baby", "new parent → baby"],
  ["i'm a new parent and exhausted", "", "sleep", "new parent + exhausted → sleep (exhausted fires at sleep route 6, baby at route 11)"],
  ["tell me about vipassana", "", "meditation", "vipassana → meditation"],
  ["i want to learn mindfulness", "", "meditation", "mindful → meditation"],
  ["i need a meditation timer", "", "timer", "meditation timer → timer (early intercept before meditation route)"],
  ["set my alarm for 7am", "", "alarm", "set alarm → alarm"],
  ["wake me up at 6", "", "alarm", "wake me up → alarm"],
  ["show me my track library", "", "ambient", "library → ambient"],
  ["i feel grateful today", "", "gratitude", "grateful → gratitude"],
  ["i want to start a gratitude journal", "", "gratitude", "journal → gratitude"],
  ["what can you do", "", "help", "what can you do → help"],
  ["give me an inspiring quote", "", "inspire", "quote → inspire"],
  ["do you have a 7 day program", "", "programs", "7 day → programs"],
  ["how many sessions have i done", "", "stats", "stats early intercept via meditat check"],
  ["show me my saved tracks", "", "favorites", "saved tracks → favorites (favorites guard before ambient)"],
  ["show me all the tracks", "", "ambient", "tracks alone → ambient"],

  // R65: pain route expansion — tension/neck/sciatica/cramps
  ["i have so much tension in my body", "", "pain", "tension → pain (PMR)"],
  ["my lower back is killing me", "", "pain", "lower back → pain"],
  ["i have terrible neck pain", "", "pain", "neck pain → pain"],
  ["my shoulder pain won't go away", "", "pain", "shoulder pain → pain"],
  ["i have sciatica", "", "pain", "sciatica → pain"],
  ["period cramps are so bad today", "", "pain", "cramps → pain"],

  // R68: focus sim sync (writing added, duplicate distract removed) + multitasking/info overload
  ["i struggle with writing because i lose focus", "", "focus", "writing → focus"],
  ["i'm terrible at multitasking", "", "focus", "multitasking → focus"],
  ["i'm suffering from information overload", "", "focus", "information overload → focus"],

  // R70: shameGuilt sim sync + night sweats/menopause → sleep + jaw/bruxism → pain
  ["my body image is terrible", "", "shameGuilt", "body image (bare) → shameGuilt"],
  ["i'm always beating myself up", "", "shameGuilt", "beating myself up → shameGuilt"],
  ["i feel so ugly", "", "shameGuilt", "feel ugly → shameGuilt"],
  ["i feel completely worthless", "", "shameGuilt", "i'm worthless → shameGuilt"],
  ["i wake up drenched in night sweats", "", "sleep", "night sweats → sleep"],
  ["menopause is disrupting my sleep", "", "sleep", "menopause → sleep"],
  ["i grind my teeth at night", "", "pain", "teeth grinding → pain"],
  ["i have constant jaw tension", "", "pain", "jaw tension → pain"],

  // R71: timer preemption fixes — "sleep timer" used to route to sleep, "breathing timer" to techniques
  ["can you set a sleep timer for 30 minutes", "", "timer", "sleep timer → timer not sleep"],
  ["i need a nap timer", "", "timer", "nap timer → timer not sleep"],
  ["set a breathing timer for 5 minutes", "", "timer", "breathing timer → timer not techniques"],
  ["can you start a yoga timer", "", "timer", "yoga timer → timer not meditation"],
  ["set a timer for 10 minutes", "", "timer", "set a timer → timer"],

  // R72: sadness gaps (helpless/despair/devastated) + anger gaps (lashing out/screaming/resentful) + relax (drink to cope)
  ["i feel completely helpless", "", "sadness", "helpless → sadness"],
  ["i'm in complete despair", "", "sadness", "despair → sadness"],
  ["i'm absolutely devastated", "", "sadness", "devastated → sadness"],
  ["i just want to scream", "", "anger", "want to scream → anger"],
  ["i keep lashing out at everyone", "", "anger", "lashing out → anger"],
  ["i snapped at my partner today", "", "anger", "snapped at → anger"],
  ["i'm full of resentment toward my boss", "", "anger", "resentful → anger"],
  ["i've been drinking to cope", "", "relax", "drinking to cope → relax"],
  ["i drink to forget my problems", "", "relax", "drink to forget → relax"],

  // R73: stem fixes (concentrat/motivat) + energy (drained/wiped out) + overwhelm (burning out)
  ["i'm having trouble concentrating", "", "focus", "concentrating → focus via concentrat stem"],
  ["i have real trouble with concentration", "", "focus", "concentration → focus via concentrat stem"],
  ["i can't motivate myself at all", "", "energy", "motivate → energy via motivat stem"],
  ["i've been feeling so drained lately", "", "energy", "drained → energy"],
  ["i'm completely wiped out", "", "energy", "wiped out → energy"],
  ["i'm totally run down", "", "energy", "run down → energy"],
  ["i think i'm burning out at work", "", "overwhelm", "burning out → overwhelm"],

  // R74: sleep (can't turn off), pain (inflammation/carpal tunnel), sadness (gaslit), overwhelm (toxic)
  ["i just can't turn off my brain at night", "", "sleep", "can't turn off → sleep"],
  ["my thoughts won't stop when i try to sleep", "", "sleep", "thoughts won't stop → sleep"],
  ["i have chronic inflammation", "", "pain", "inflammation → pain"],
  ["i think i have carpal tunnel syndrome", "", "pain", "carpal tunnel → pain"],
  ["my frozen shoulder is really painful", "", "pain", "frozen shoulder → pain"],
  ["i think i'm being gaslit by my partner", "", "sadness", "gaslit → sadness"],
  ["the gaslighting at work is destroying me", "", "sadness", "gaslighting → sadness"],
  ["my workplace is completely toxic", "", "overwhelm", "toxic workplace → overwhelm"],

  // R67: crisis (overdose/not worth living/take my life) + relax (dread/hypervig) + overwhelm
  ["i want to take my life", "", "crisis", "take my life → crisis"],
  ["my life is not worth living", "", "crisis", "not worth living → crisis"],
  ["i dont want to exist anymore", "", "crisis", "dont want to exist → crisis"],
  ["i took an overdose", "", "crisis", "overdose → crisis"],
  // R75: crisis safety additions (burden/disappear/better off without me/nothing to live for)
  ["i feel like such a burden to my family", "", "crisis", "feel like a burden → crisis"],
  ["everyone would be better off without me", "", "crisis", "better off without me → crisis"],
  ["i have nothing to live for anymore", "", "crisis", "nothing to live for → crisis"],
  ["i just want to disappear forever", "", "crisis", "want to disappear → crisis"],
  ["i wish i was dead", "", "crisis", "wish i was dead → crisis"],
  ["i just can't carry on anymore", "", "crisis", "cant carry on → crisis"],
  // R75: sadness (betrayal/cheated/trust issues)
  ["i was cheated on by my partner", "", "sadness", "cheated on → sadness"],
  ["i feel so betrayed right now", "", "sadness", "betrayed → sadness"],
  ["i have serious trust issues", "", "sadness", "trust issues → sadness"],

  ["i have the sunday scaries so bad", "", "relax", "sunday scaries → relax"],
  ["i dread going to work every day", "", "relax", "dread → relax"],
  ["i feel hypervigilant all the time", "", "relax", "hypervigilant → relax"],
  ["i'm having a nervous breakdown", "", "relax", "nervous breakdown → relax (nervous hits relax route 9 before overwhelm 16)"],
  ["i just cant keep up with everything", "", "overwhelm", "cant keep up → overwhelm"],

  // R79: sadness (sobbing/weeping/giving up), focus (analysis paralysis/hyperfocus)
  ["i've been sobbing all afternoon", "", "sadness", "sobbing → sadness"],
  ["i'm completely in tears right now", "", "sadness", "in tears → sadness"],
  ["i keep tearing up for no reason", "", "sadness", "tearing up → sadness"],
  ["i feel like giving up on everything", "", "sadness", "feel like giving up → sadness"],
  ["i'm ready to give up i don't know what to do", "", "sadness", "ready to give up → sadness"],
  ["i keep getting analysis paralysis with decisions", "", "focus", "analysis paralysis → focus"],
  ["i tend to hyperfocus and forget to take breaks", "", "focus", "hyperfocus → focus"],
  ["i struggle with task switching when working", "", "focus", "task switching → focus"],

  // R78: relax (catastrophizing, fight-or-flight, on edge, jittery, butterflies), anger (passive-aggressive, bitter, jealousy), shameGuilt (imposter)
  ["i keep catastrophizing everything", "", "relax", "catastrophizing → relax"],
  ["my body went into fight or flight mode", "", "relax", "fight or flight → relax"],
  ["i feel so on edge today", "", "relax", "on edge → relax"],
  ["i've been jittery all morning", "", "relax", "jittery → relax"],
  ["i have butterflies in my stomach before this", "", "relax", "butterflies in → relax"],
  ["i'm trembling i'm so anxious", "", "relax", "trembling → relax"],
  ["i keep being passive aggressive and i hate it", "", "anger", "passive aggressive → anger"],
  ["i have such a short fuse lately", "", "anger", "short fuse → anger"],
  ["i feel so bitter about how things turned out", "", "anger", "bitter → anger"],
  ["i have a lot of contempt for what they did", "", "anger", "contempt → anger"],
  ["i feel jealous of my friends and it bothers me", "", "anger", "jealous → anger"],
  ["i feel like such a fraud at work", "", "shameGuilt", "feel like a fraud → shameGuilt"],
  ["i think i have imposter syndrome", "", "shameGuilt", "imposter syndrome → shameGuilt"],

  // R77: sim recommend fix, exhaustion, empty nest, single parent
  ["what sound should i listen to", "", "recommend", "what sound → recommend (sim was missing)"],
  ["help me choose a track", "", "recommend", "help me choose → recommend (sim was missing)"],
  ["i'm suffering from mental exhaustion", "", "sleep", "exhaustion → sleep"],
  ["the empty nest is hitting me hard", "", "sadness", "empty nest → sadness"],
  ["its hard being a single parent sometimes", "", "overwhelm", "single parent → overwhelm"],
  ["im basically solo parenting while he travels", "", "overwhelm", "solo parenting → overwhelm"],
  ["being a single parent is really hard", "", "overwhelm", "single parent → overwhelm"],

  // R76: burden fix (like such a burden), eating disorders, bipolar, sleep (wake up tired), gratitude expansions
  ["i feel like such a burden to my family", "", "crisis", "like such a burden → crisis"],
  ["i struggle with bulimia", "", "shameGuilt", "bulimia → shameGuilt"],
  ["i think i might be anorexic", "", "shameGuilt", "anorexic → shameGuilt"],
  ["i have a binge eating problem", "", "shameGuilt", "binge eating → shameGuilt"],
  ["i've been diagnosed with bipolar disorder", "", "sadness", "bipolar → sadness"],
  ["i wake up tired no matter how much i sleep", "", "sleep", "wake up tired → sleep"],
  ["i never feel rested anymore", "", "sleep", "never feel rested → sleep"],
  ["i want to appreciate what i have more", "", "gratitude", "appreciate → gratitude"],
  ["i want to count my blessings today", "", "gratitude", "count my blessings → gratitude"],

  // R66: sadness sim sync — pre-R62 keywords were missing; + widowed/ghosted
  ["we just broke up", "", "sadness", "broke up → sadness"],
  ["i've been feeling so isolated", "", "sadness", "isolated → sadness"],
  ["my dad passed away last week", "", "sadness", "passed away → sadness"],
  ["i got laid off yesterday", "", "sadness", "laid off → sadness"],
  ["we're going through a divorce", "", "sadness", "divorce → sadness"],
  ["my partner left me", "", "sadness", "partner left me → sadness"],
  ["my mood is all over the place", "", "sadness", "mood swings → sadness"],
  ["i feel so empty inside", "", "sadness", "feel empty → sadness"],
  ["my life feels pointless lately", "", "sadness", "life feels pointless → sadness"],
  ["i was recently widowed", "", "sadness", "widowed → sadness"],
  ["i got ghosted and it hurt", "", "sadness", "ghosted → sadness"],

  // R64: sleep simulation sync + new sleep keywords + night terrors
  ["i have terrible night terrors", "", "sleep", "night terrors → sleep"],
  ["i'm wide awake at 2am", "", "sleep", "wide awake → sleep"],
  ["i have chronic fatigue", "", "sleep", "fatigue → sleep"],
  ["i work the night shift", "", "sleep", "night shift → sleep"],
  ["jet lag is destroying me", "", "sleep", "jet lag → sleep"],
  ["i'm just counting sheep again", "", "sleep", "counting sheep → sleep"],
  ["i can't wind down after work", "", "sleep", "can't wind down → sleep"],
  ["i need tips on sleep hygiene", "", "sleep", "sleep hygiene → sleep"],
  ["my body clock is all over the place", "", "sleep", "body clock → sleep"],
  ["wired at night but exhausted in the day", "", "sleep", "wired at night → sleep (not energy)"],

  // R63: anger vocabulary expansion
  ["i'm so pissed off right now", "", "anger", "pissed off → anger"],
  ["i'm absolutely livid", "", "anger", "livid → anger"],
  ["i'm seething", "", "anger", "seething → anger"],
  ["i lost my temper today", "", "anger", "lost my temper → anger"],
  ["i'm about to explode", "", "anger", "about to explode → anger"],
  ["i completely lost it", "", "anger", "lost it → anger"],

  // R62: distract→focus, seasonal affective→sadness, afternoon slump→energy
  ["i'm so distracted today", "", "focus", "distracted → focus (was in sim not Kotlin — now fixed)"],
  ["i have seasonal affective disorder", "", "sadness", "seasonal affective → sadness"],
  ["seasonal depression is hitting hard", "", "sadness", "seasonal depression → sadness"],
  ["i always get low in winter", "", "sadness", "low in winter → sadness"],
  ["afternoon slump is real today", "", "energy", "afternoon slump → energy"],
  ["need a pick me up", "", "energy", "pick me up → energy"],
  ["feeling completely flat today", "", "energy", "feeling flat → energy"],

  // R61: focus expansion + overwhelm sim sync
  ["i have a mental block", "", "focus", "mental block → focus"],
  ["i have writer's block today", "", "focus", "writer's block → focus"],
  ["my mind has gone blank", "", "focus", "mind has gone blank → focus"],
  ["i'm swamped at work", "", "overwhelm", "swamped → overwhelm"],
  ["i'm falling apart", "", "overwhelm", "falling apart → overwhelm"],
  ["work is killing me", "", "overwhelm", "work is killing me → overwhelm"],
  ["i'm always putting others first", "", "overwhelm", "always putting others first → overwhelm"],

  // R60: "worth" substring preemption fix — worthless must reach shameGuilt not techniques
  ["i feel worthless", "", "shameGuilt", "worthless → shameGuilt (not techniques via worth substring)"],
  ["i feel unworthy", "", "shameGuilt", "unworthy → shameGuilt"],
  ["my self-worth is low", "", "techniques", "self-worth → techniques (positive case preserved)"],
  ["i want to build self worth", "", "techniques", "self worth → techniques"],

  // R59: body image → shameGuilt, negative thoughts → relax
  ["i have body image issues", "", "shameGuilt", "body image issues → shameGuilt"],
  ["i have an eating disorder", "", "shameGuilt", "eating disorder → shameGuilt (compassion response)"],
  ["negative body image is really affecting me", "", "shameGuilt", "negative body image → shameGuilt"],
  ["i keep having negative thoughts", "", "relax", "negative thoughts → relax"],
  ["my thoughts are spiralling", "", "relax", "thought spirals → relax (spiralling keyword)"],
  ["i have racing thoughts at night", "", "sleep", "racing thoughts → sleep (sleep route fires before relax)"],
  ["negative thinking is hard to stop", "", "relax", "negative thinking → relax"],

  // R58: emotional vocabulary gaps in sadness
  ["i've been feeling low lately", "", "sadness", "feeling low → sadness"],
  ["i feel so low today", "", "sadness", "so low → sadness"],
  ["i've been feeling blue lately", "", "sadness", "feeling blue → sadness"],
  ["i feel so down right now", "", "sadness", "so down → sadness"],
  ["i've been feeling down all week", "", "sadness", "feeling down → sadness"],
  ["i feel down", "", "sadness", "feel down → sadness"],

  // R57: isWell false positive fix + dwell rumination
  ["i'm not feeling well", "", "sadness", "not feeling well → sadness, not isWell in general"],
  ["i'm feeling unwell", "", "sadness", "unwell → sadness (not well substring-match)"],
  ["i'm not fine", "", "NOT:positive", "not fine → sadness, not positive"],
  ["i keep dwelling on the past", "", "relax", "dwelling on → relax (rumination)"],
  ["i keep dwelling on what happened", "", "relax", "dwelling on → relax"],

  // R56: followUp sim sync + relax sim sync
  ["go ahead", "sleep", "followUp", "go ahead with lastTopic → followUp"],
  ["sounds good", "relax", "followUp", "sounds good with lastTopic → followUp"],
  ["absolutely", "overwhelm", "followUp", "absolutely with lastTopic → followUp"],
  ["of course", "anger", "followUp", "of course with lastTopic → followUp"],
  ["i'd like that", "sadness", "followUp", "i'd like that with lastTopic → followUp"],
  ["why not", "focus", "followUp", "why not with lastTopic → followUp"],
  ["i have ptsd", "", "relax", "ptsd → relax"],
  ["i'm dealing with trauma", "", "relax", "trauma → relax"],
  ["i'm feeling triggered right now", "", "relax", "triggered → relax"],
  ["i can't breathe", "", "relax", "can't breathe → relax"],
  ["i can't stop thinking", "", "relax", "can't stop thinking → relax"],

  // R53: relax + overwhelm gaps
  ["i'm freaking out", "", "relax", "freaking out → relax"],
  ["i'm losing my mind", "", "relax", "losing my mind → relax"],
  ["i'm spiralling", "", "relax", "spiralling → relax"],
  ["i'm having a meltdown", "", "overwhelm", "meltdown → overwhelm"],
  ["i've hit my limit", "", "overwhelm", "hit my limit → overwhelm"],
  ["i can't handle this anymore", "", "overwhelm", "cant handle this → overwhelm"],

  // R52: greeting gaps + followUp agreement phrases
  ["good afternoon spirit", "", "greeting", "good afternoon → greeting (was missing)"],
  ["howdy", "", "greeting", "howdy → greeting"],
  ["whats up", "", "greeting", "whats up → greeting"],
  ["greetings", "", "greeting", "greetings → greeting"],
  ["morning routine tips", "", "NOT:greeting", "morning routine must NOT trigger greeting"],
  ["my morning energy is low", "", "NOT:greeting", "morning energy must NOT trigger greeting (goes to energy)"],

  // R51: timer route narrowing — "how long have I" must go to stats, not timer
  ["how long have i been meditating", "", "stats", "how long have i been meditating → stats (early intercept before meditation route)"],
  ["how long should i meditate", "", "timer", "how long should i meditate → timer (early intercept before meditation route)"],
  ["how many minutes should i do", "", "timer", "how many minutes → timer"],
  ["how long have i been practicing", "", "stats", "how long have I practicing → stats"],

  // Core routing
  ["cant sleep tonight", "", "sleep", "core sleep"],
  ["feeling anxious all day", "", "relax", "core relax"],
  ["help me focus on work", "", "focus", "core focus"],
  ["so sad today", "", "sadness", "core sadness"],
  ["overwhelmed with everything", "", "overwhelm", "core overwhelm"],
  ["feeling so angry", "", "anger", "core anger"],
  ["need more energy", "", "energy", "core energy"],
  ["feeling guilty about that", "", "shameGuilt", "core shameGuilt"],
  ["can you recommend something", "", "recommend", "core recommend"],
  ["what is my streak", "", "stats", "core stats"],
  ["show me a breathing technique", "", "techniques", "core techniques"],
];

var pass = 0, fail = 0;
tests.forEach(function(t) {
  var input = t[0], lastT = t[1], expected = t[2], desc = t[3];
  var actual = route(input, lastT);
  var ok = expected.startsWith("NOT:") ? (actual !== expected.slice(4)) : (actual === expected);
  if(!ok) { console.log("FAIL: \"" + input + "\" -> " + actual + " (expected " + expected + ") | " + desc); fail++; }
  else pass++;
});
console.log(pass + "/" + (pass+fail) + " passed" + (fail > 0 ? " — " + fail + " FAILURES ABOVE" : " (all OK)"));
