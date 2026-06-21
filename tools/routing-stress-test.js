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
    "what do i do","lets do it"]) || anyWord(lower,["yes","sure","ok"]))) return "followUp";
  if(any(lower,["hello","good morning","good afternoon","good evening","good night",
    "howdy","greetings","what's up","whats up"]) ||
    anyWord(lower,["hi","hey","yo","sup"])) return "greeting";
  if(any(lower,["sleep","insomnia","nightmare","cant sleep","woke up at","keep waking","3am","4am",
    "middle of the night","drift off","drifting off","restless night","night terrors","sleepy",
    "tired","exhausted","racing mind at night","cant switch off","napping"]) ||
    anyWord(lower,["nap"])) return "sleep";
  if(any(lower,["focus","study","concentrate","productivity","procrastinat","brain fog","adhd",
    "attention","distract","doom scrolling","doomscrolling","mindless scrolling"]) ||
    anyWord(lower,["read","code"])) return "focus";
  if(any(lower,["energy","energise","energize","wake up","waking up","uplift","motivation",
    "motivated","active","exercise","workout","morning boost","morning energy","sluggish",
    "lethargic","cold shower","cold water","ice bath","wim hof"])) return "energy";
  if(any(lower,["relax","calm","stress","anxiety","anxious","breathe","unwind","nervous","panic",
    "overthink","overthinking","intrusive thoughts","ruminating","social anxiety","public speaking",
    "presentation nerves","exam nerves","stage fright","interview nerves","interview anxiety",
    "nerves before","performance anxiety","performance pressure","pounding heart","heart racing",
    "fear","scared","frightened","phobia","afraid","worry","worried","worrying","dissociation",
    "feel unreal","feel detached","ocd","obsessive","compulsive thoughts",
    "freaking out","freak out","losing my mind","losing it","can't cope with","cant cope with",
    "spiralling","spiraling"]) ||
    anyWord(lower,["rest","tense"])) return "relax";
  if(any(lower,["sad","grief","grieving","heartbreak","heartbroken","lonely","alone","loneliness",
    "depressed","depression","cry","crying","upset","miserable","unhappy","low mood","empty inside",
    "hopeless","hollow","disconnected","meaningless","no motivation","nothing matters",
    "bereaved","bereavement","loss of","lost someone","longing","miss him","miss her",
    "miscarriage","stillbirth","pregnancy loss","child loss","infertility","lost my baby","lost our baby",
    "need to vent","venting"]) ||
    anyWord(lower,["numb","died","vent"])) return "sadness";
  if(any(lower,["shame","ashamed","guilt","guilty","embarrassed","humiliated","self-blame",
    "blame myself","hate my body","feel worthless","not good enough",
    "low confidence","build confidence"])) return "shameGuilt";
  if(any(lower,["overwhelm","overwhelmed","burnout","burnt out","burned out","too much","cant cope",
    "too busy","work stress","work anxiety","feeling stuck","feel stuck",
    "deadline","under pressure","work pressure","pressure at work",
    "work-life balance","people pleaser","cant say no","can't say no","running on empty",
    "meltdown","having a meltdown","on the edge","at my limit","hit my limit",
    "can't handle it","cant handle it","can't handle this","cant handle this"])) return "overwhelm";
  if(any(lower,["angry","furious","frustrated","frustration","rage","irritated","annoyed"]) ||
    anyWord(lower,["mad","anger","angered"])) return "anger";
  if(any(lower,["vip","upgrade","pro plan","subscription","pricing","plans","cost","buy","purchase"])) return "vip";
  if(any(lower,["recommend","suggest","what should i play","pick something"])) return "recommend";
  if(any(lower,["timer","sleep timer","how long should","how long to meditate","how long for","duration","how many minutes"])) return "timer";
  if(any(lower,["my stats","my streak","my progress","sessions","total time","day streak","how long have i","my history","minutes meditated"])) return "stats";
  if(any(lower,["technique","breathwork","body scan","box breathing","physiological sigh",
    "self-compassion","self esteem","low confidence","build confidence","worth",
    "confidence","stretching","morning routine","bored","boredom"])) return "techniques";
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
