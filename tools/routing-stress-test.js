'use strict';
function any(input, kws) { return kws.some(function(k) { return input.includes(k); }); }
function anyWord(input, kws) {
  return kws.some(function(k) {
    return new RegExp('\\b' + k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b').test(input);
  });
}

function route(lower, lastTopic) {
  if(any(lower,["suicidal","suicide","want to die","dont want to live","end it all","end my life",
    "kill myself","self harm","self-harm","hurt myself","no reason to live","better off dead",
    "take my own life","life isnt worth living","dont want to be here anymore",
    "unalive","cutting myself","cant go on like this"])) return "crisis";
  if(lastTopic.length > 0 && (any(lower,["please","okay","go on","continue","next",
    "tell me more","walk me through","guide me","show me","how do i","teach me",
    "what do i do","let's do it","lets do it","let's go","lets go","go ahead",
    "sounds good","i'd like that","i would like that","absolutely","of course",
    "why not"]) || anyWord(lower,["yes","sure","ok"]))) return "followUp";
  if(any(lower,["hello","good morning","good afternoon","good evening","good night",
    "howdy","greetings","what's up","whats up"]) ||
    anyWord(lower,["hi","hey","yo","sup"])) return "greeting";
  if(any(lower,["sleep","insomnia","cant sleep","can't sleep","falling asleep",
    "bedtime","tired","fatigue","exhausted","wide awake","cant switch off","can't switch off",
    "racing mind","racing thoughts","napping","night shift","shift work",
    "mind won't stop","mind wont stop","nightmare","nightmares","bad dreams",
    "jet lag","jet lagged","jet-lagged","restless","restlessness",
    "woke up at","keep waking","3am","4am","middle of the night",
    "drift off","drifting off","can't drift",
    "counting sheep","wired at night","wired tonight","can't wind down","cant wind down",
    "light sleeper","heavy sleeper","sleep hygiene","body clock","circadian","night terrors"]) ||
    anyWord(lower,["nap"])) return "sleep";
  if(any(lower,["focus","study","concentrate","productivity","procrastinat","brain fog","adhd",
    "foggy","mental clarity","sharp","clear mind","attention","distract",
    "doom scrolling","doomscrolling","mindless scrolling","phone addiction",
    "screen addiction","endless scrolling","too much screen",
    "mental block","writer's block","writers block","creative block",
    "brain freeze","can't think straight","cant think straight",
    "mind blank","mind went blank","mind has gone blank",
    "distract"]) ||
    anyWord(lower,["read","code"])) return "focus";
  if(any(lower,["energy","energise","energize","wake up","waking up","uplift","motivation",
    "motivated","active","exercise","workout","morning boost","morning energy","sluggish",
    "lethargic","cold shower","cold water","ice bath","wim hof",
    "afternoon slump","afternoon crash","2pm slump","post-lunch dip",
    "pick me up","need a boost","feeling flat","flat today"])) return "energy";
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
    "thought spiral","thought spirals","racing thoughts"]) ||
    anyWord(lower,["rest","tense"])) return "relax";
  if(any(lower,["sad","grief","grieving","heartbreak","heartbroken","lonely","alone","loneliness",
    "depressed","depression","cry","crying","upset","miserable","unhappy","low mood","empty inside",
    "hopeless","hollow","disconnected","meaningless","no motivation","nothing matters",
    "bereaved","bereavement","loss of","lost someone","longing","miss him","miss her",
    "miscarriage","stillbirth","pregnancy loss","child loss","infertility","lost my baby","lost our baby",
    "need to vent","venting",
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
    "seasonal affective","seasonal depression","sad disorder"]) ||
    anyWord(lower,["numb","died","vent"])) return "sadness";
  if(any(lower,["shame","ashamed","guilt","guilty","embarrassed","humiliated","self-blame",
    "blame myself","hate my body","feel worthless","not good enough",
    "low confidence","build confidence",
    "feel like a failure","i'm a failure","im a failure","i am a failure",
    "feel inadequate","i feel inadequate","feel unworthy","i feel unworthy",
    "forgive myself","self-forgiveness","self forgiveness",
    "perfectionism","perfectionist","never good enough","hard on myself",
    "i'm not perfect","im not perfect","i am not perfect",
    "nobody's perfect","nobody is perfect","nothing is perfect",
    "too hard on myself","self-critical","self critical",
    "body image issues","body image problem","negative body image",
    "struggle with my body","hate how i look","hate my appearance",
    "eating disorder","disordered eating"])) return "shameGuilt";
  if(any(lower,["overwhelm","overwhelmed","burnout","burnt out","burned out","too much",
    "cant cope","can't cope","too busy","overloaded","swamped","falling apart",
    "breaking point","can't take","work stress","work anxiety","work is killing me",
    "job stress","feeling stuck","feel stuck","stuck in a rut","stuck in life",
    "deadline","under pressure","work pressure","pressure at work",
    "work-life balance","work life balance","no time for myself","no time for me",
    "people pleaser","people-pleaser","cant say no","can't say no",
    "always putting others first","never put myself first","spread too thin",
    "running on empty","meltdown","having a meltdown","on the edge","at my limit",
    "hit my limit","can't handle it","cant handle it","can't handle this",
    "cant handle this"])) return "overwhelm";
  if(any(lower,["angry","furious","frustrated","frustration","rage",
    "irritated","irritable","annoyed","wound up","agitated",
    "pissed off","livid","seething","seeing red",
    "lost my temper","losing my temper","lose my temper",
    "about to explode","about to snap","lost it","blow up"]) ||
    anyWord(lower,["mad","anger","angered"])) return "anger";
  if(any(lower,["vip","upgrade","pro plan","subscription","pricing","plans","cost","buy","purchase"])) return "vip";
  if(any(lower,["recommend","suggest","what should i play","pick something"])) return "recommend";
  if(any(lower,["thank","thanks","awesome","perfect","love it","amazing","nice"])) return "positive";
  if(any(lower,["timer","sleep timer","how long should","how long to meditate","how long for","duration","how many minutes"])) return "timer";
  if(any(lower,["my stats","my streak","my progress","sessions","total time","day streak","how long have i","my history","minutes meditated"])) return "stats";
  if(any(lower,["technique","breathwork","body scan","box breathing","physiological sigh",
    "self-compassion","self esteem","low confidence","build confidence","self-worth",
    "self worth","confidence","stretching","morning routine","bored","boredom"])) return "techniques";
  if(any(lower,["play it","play that","play my","play my fav","play favourite","play favorite",
    "start it","queue it","play something","play now","can you play",
    "put on a","put on some","start playing"])) return "play";
  if(any(lower,["headache","migraine","ache","sore","tension headache","physical","body tension",
    "muscle tension","stiff","chronic pain","chronic illness","fibromyalgia","arthritis","back pain",
    "painful","pains","in pain"]) || anyWord(lower,["pain"])) return "pain";
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
  ["put on some meditation", "", "play", "put on some → playRequest"],
  ["start playing something", "", "play", "start playing → playRequest"],

  // R54: "not great" / "not okay" — must NOT hit positive route
  ["i'm not feeling so great today", "", "NOT:positive", "not great must not trigger positive"],
  ["i'm feeling great today", "", "general", "feeling great → general (isWell branch says 'lovely to hear')"],
  ["not feeling ok", "", "NOT:positive", "not ok → sadness, not positive"],
  ["i'm not doing well", "", "NOT:positive", "not doing well → sadness"],
  ["not myself lately", "", "NOT:positive", "not myself → sadness"],
  ["i'm feeling off today", "", "NOT:positive", "feeling off → sadness"],

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
  ["how long have i been meditating", "", "stats", "how long have I → stats, not timer"],
  ["how long should i meditate", "", "timer", "how long should → timer"],
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
