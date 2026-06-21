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
  if(any(lower,["hello","good morning","good evening","good night"]) ||
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
    "feel unreal","feel detached","ocd","obsessive","compulsive thoughts"]) ||
    anyWord(lower,["rest","tense"])) return "relax";
  if(any(lower,["sad","grief","grieving","heartbreak","heartbroken","lonely","alone","loneliness",
    "depressed","depression","cry","crying","upset","miserable","unhappy","low mood","empty inside",
    "hopeless","hollow","disconnected","meaningless","no motivation","nothing matters",
    "bereaved","bereavement","loss of","lost someone","longing","miss him","miss her",
    "miscarriage","stillbirth","pregnancy loss","child loss","infertility","lost my baby","lost our baby"]) ||
    anyWord(lower,["numb","died"])) return "sadness";
  if(any(lower,["shame","ashamed","guilt","guilty","embarrassed","humiliated","self-blame",
    "blame myself","hate my body","feel worthless","not good enough",
    "low confidence","build confidence"])) return "shameGuilt";
  if(any(lower,["overwhelm","overwhelmed","burnout","burnt out","burned out","too much","cant cope",
    "too busy","work stress","work anxiety","feeling stuck","feel stuck",
    "deadline","under pressure","work pressure","pressure at work"])) return "overwhelm";
  if(any(lower,["angry","anger","furious","frustrated","frustration","rage","irritated","annoyed"]) ||
    anyWord(lower,["mad"])) return "anger";
  if(any(lower,["vip","upgrade","pro plan","subscription","pricing","plans","cost","buy","purchase"])) return "vip";
  if(any(lower,["recommend","suggest","what should i play","pick something"])) return "recommend";
  if(any(lower,["my stats","my streak","my progress","sessions","total time","day streak"])) return "stats";
  if(any(lower,["technique","breathwork","body scan","box breathing","physiological sigh",
    "self-compassion","self esteem","low confidence","build confidence","worth",
    "confidence","stretching","morning routine","bored","boredom"])) return "techniques";
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

  // R44: nap word-boundary fix — snap should NOT route to sleep
  ["i'm about to snap", "", "NOT:sleep", "snap does not contain nap at word boundary"],
  ["i need a nap", "", "sleep", "nap (standalone) → sleep"],
  ["napping this afternoon", "", "sleep", "napping → sleep"],

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
