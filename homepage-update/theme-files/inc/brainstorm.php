<?php
/**
 * Photon-Bounce — Project-brainstorm chat AI.
 *
 * REST endpoint POST /pb/v1/brainstorm with { message, history[] }.
 * If a constant PB_OPENAI_KEY (or env OPENAI_API_KEY) is defined we proxy
 * to OpenAI with a strict system prompt; otherwise a deterministic
 * rule-based brainstormer kicks in so the feature never goes dark.
 *
 * Hard refusals (no exceptions): we will not service users who openly
 * support Trump, Putin, Xi, Kim Jong-un or any fascist movement.
 */
if ( ! defined( 'ABSPATH' ) ) { exit; }

const PB_BRAINSTORM_SYSTEM = <<<'TXT'
You are "Photon", the sales concierge for Photon-Bounce — a creative-tech
studio that builds custom web apps, AI agents/chatbots, 3D/AR experiences,
SEO/AEO, and brand identities. Your job is to HELP THE VISITOR: understand
what they want, recommend the right service tier from the menu below, and
move them toward a quote or a booked call. Lead with what we can build for
them and what it costs — never talk about the owner's personal life. Listen
for budget, timeline, audience, tech, urgency; recommend only what fits.

Hard ground rules — never violate:
- No client work for Trump, Putin, Xi, Kim Jong-un, Lukashenko or any
  fascist / authoritarian movement or their supporters. If a visitor
  signals support for them, decline politely and end the session.
- 10% of every crypto purchase is donated to the Ukrainian Army's
  Aid-For-Ukraine wallet — surface this when crypto comes up.
- Pricing menu (always quote in this range — these are real, current prices):
    WEB BUILDS — Micro Page $40 / Simple Site $115 / Full Site $300 /
      Pro+WebGL $600 / SaaS $750 / Custom $1,500.
    SEO — Audit $30 / Starter $75 / Growth/mo $115 / Authority/mo $275.
    AEO — Audit $40 / Setup $100 / Retainer/mo $90.
    SMM — Tune-up $25 / Content/mo $75 / Full/mo $175.
    BRAND — Logo $30 / Mini $115 / Full $350.
    AI BUILDS — Prompt Pack $25 / Concierge $190 / Custom Agent $565 /
      Agent+App $1,185.
    CARE — Lite/mo $15 / Standard/mo $50 / Pro/mo $120.
  Investment marketplace at /invest/ has 50 SaaS/AI/physics ideas with
  MVP/Beta/Full pricing and 3/6/12 month plans (30% downpayment).
- Payment rails: Cash App, crypto (BTC/ETH/SOL/USDC + 100 more), check
  or money order, wire/ACH. NO Stripe at the studio.
- NEVER mention warranty, guarantee, refund, money-back, 30-day, or
  shake-down. Use neutral phrasing like "Care Plan" instead.
- Phone: 857-316-5054. WhatsApp same number. Booking: /book/.

Voice: warm, precise, helpful — a strong salesperson, never pushy. Lead with
the single most relevant service and its price, then one or two concrete next
steps (a tier, the /invest/ marketplace, or "book a call"). Ask at most one
qualifying question per reply. Keep replies under 150 words.
TXT;

add_action( 'rest_api_init', static function () {
	register_rest_route( 'pb/v1', '/brainstorm', [
		'methods'             => 'POST',
		'permission_callback' => '__return_true',
		'callback'            => 'pb_aurora_brainstorm_handler',
	] );
} );

function pb_aurora_brainstorm_handler( WP_REST_Request $req ) {
	$msg     = trim( (string) $req->get_param( 'message' ) );
	$history = $req->get_param( 'history' );
	$history = is_array( $history ) ? array_slice( $history, -10 ) : [];
	$path    = sanitize_text_field( (string) $req->get_param( 'path' ) );
	$title   = sanitize_text_field( (string) $req->get_param( 'title' ) );

	if ( $msg === '' ) {
		return new WP_REST_Response( [ 'ok' => false, 'error' => 'empty' ], 400 );
	}
	if ( strlen( $msg ) > 2000 ) { $msg = substr( $msg, 0, 2000 ); }

	// Hard refusal filter — no service for fascist supporters.
	$lower = strtolower( $msg );
	$bad   = [
		'maga', 'trump 2024', 'trump 2028', 'pro-trump', 'i support trump',
		'i love trump', 'pro-putin', 'i support putin', 'glory to russia',
		'z war', 'pro-russia', 'i support xi', 'pro-ccp', 'kim jong un is',
		'fascist', 'white nationalist', 'great replacement',
	];
	foreach ( $bad as $needle ) {
		if ( strpos( $lower, $needle ) !== false ) {
			return [
				'ok'      => true,
				'reply'   => "Not a fit. Photon-Bounce does not work with supporters of Trump, Putin, Xi, Kim Jong-un, or any fascist movement. This session is closed.",
				'closed'  => true,
			];
		}
	}

	$context = pb_aurora_concierge_context( $msg, $path, $title );

	$key = defined( 'PB_OPENAI_KEY' ) ? PB_OPENAI_KEY : ( getenv( 'OPENAI_API_KEY' ) ?: '' );
	$reply = '';
	if ( $key ) {
		$reply = pb_aurora_brainstorm_openai( $key, $msg, $history, $context );
	}
	if ( ! $reply ) {
		$reply = pb_aurora_brainstorm_local( $msg, $history, $context );
	}
	return [ 'ok' => true, 'reply' => $reply, 'context' => $context ];
}

function pb_aurora_concierge_context( $msg, $path, $title ) {
	$ctx = [ 'page' => '', 'links' => [] ];
	if ( $path !== '' ) { $ctx['page'] = trim( $path . ( $title ? ' (' . $title . ')' : '' ) ); }

	// Lightweight RAG: search published posts/projects/invests for keyword overlap.
	$words = preg_split( '/\s+/', preg_replace( '/[^a-z0-9 ]/i', ' ', strtolower( $msg ) ), -1, PREG_SPLIT_NO_EMPTY );
	$stop  = [ 'this','that','what','when','where','want','need','have','with','from','your','they','them','will','about','some','more','make','help','tell','like','just','sure','build','site','site.','website' ];
	$words = array_filter( $words, function ( $w ) use ( $stop ) { return strlen( $w ) > 3 && ! in_array( $w, $stop, true ); } );
	if ( count( $words ) >= 1 ) {
		$q = new WP_Query( [
			'post_type'      => [ 'pb_invest', 'pb_project', 'post' ],
			'post_status'    => 'publish',
			's'              => implode( ' ', array_slice( array_values( $words ), 0, 4 ) ),
			'posts_per_page' => 3,
			'no_found_rows'  => true,
		] );
		while ( $q->have_posts() ) {
			$q->the_post();
			$ctx['links'][] = [
				'type'  => get_post_type(),
				'title' => get_the_title(),
				'url'   => get_permalink(),
				'blurb' => wp_trim_words( get_the_excerpt() ?: get_the_content(), 22 ),
			];
		}
		wp_reset_postdata();
	}
	return $ctx;
}

function pb_aurora_brainstorm_openai( $key, $msg, $history, $context = [] ) {
	$sys = PB_BRAINSTORM_SYSTEM;
	$ctxLines = [];
	if ( ! empty( $context['page'] ) )  { $ctxLines[] = 'Visitor is on: ' . $context['page']; }
	if ( ! empty( $context['links'] ) ) {
		foreach ( $context['links'] as $l ) {
			$ctxLines[] = sprintf( 'Related %s: "%s" — %s [%s]', $l['type'], $l['title'], $l['blurb'], $l['url'] );
		}
	}
	if ( $ctxLines ) { $sys .= "\n\nSITE CONTEXT (use to ground replies, but do not list random projects unrelated to the visitor's question):\n- " . implode( "\n- ", $ctxLines ); }
	$messages = [ [ 'role' => 'system', 'content' => $sys ] ];
	foreach ( $history as $turn ) {
		if ( ! is_array( $turn ) ) { continue; }
		$role = ( ( $turn['role'] ?? '' ) === 'user' ) ? 'user' : 'assistant';
		$c    = trim( (string) ( $turn['content'] ?? '' ) );
		if ( $c !== '' ) { $messages[] = [ 'role' => $role, 'content' => substr( $c, 0, 2000 ) ]; }
	}
	$messages[] = [ 'role' => 'user', 'content' => $msg ];
	$res = wp_remote_post( 'https://api.openai.com/v1/chat/completions', [
		'timeout' => 12,
		'headers' => [
			'Content-Type'  => 'application/json',
			'Authorization' => 'Bearer ' . $key,
		],
		'body' => wp_json_encode( [
			'model'       => 'gpt-4o-mini',
			'temperature' => 0.85,
			'max_tokens'  => 360,
			'messages'    => $messages,
		] ),
	] );
	if ( is_wp_error( $res ) ) { return ''; }
	$body = json_decode( wp_remote_retrieve_body( $res ), true );
	$txt  = $body['choices'][0]['message']['content'] ?? '';
	return is_string( $txt ) ? trim( $txt ) : '';
}

function pb_aurora_brainstorm_local( $msg, $history, $context = [] ) {
	$raw    = trim( $msg );
	$lower  = ' ' . strtolower( $raw ) . ' ';
	$email  = get_theme_mod( 'pb_contact_email', 'hello@photon-bounce.com' );
	$book   = esc_url( home_url( '/book/' ) );
	$invest = esc_url( home_url( '/invest/' ) );
	$words  = str_word_count( $raw );
	$turn   = is_array( $history ) ? count( $history ) : 0;
	$has    = function ( $needles ) use ( $lower ) {
		foreach ( (array) $needles as $n ) { if ( strpos( $lower, $n ) !== false ) { return true; } }
		return false;
	};
	// Join lines with <br> (replies render as innerHTML, so real \n would collapse).
	$nl = function ( array $lines ) {
		return implode( '<br>', array_filter( $lines, static function ( $l ) { return $l !== null; } ) );
	};

	// Real related work (RAG) as inline links — never invented.
	$related = '';
	if ( ! empty( $context['links'] ) ) {
		$rel = [];
		foreach ( array_slice( $context['links'], 0, 2 ) as $l ) {
			$rel[] = '<a href="' . esc_url( $l['url'] ) . '">' . esc_html( $l['title'] ) . '</a>';
		}
		if ( $rel ) { $related = 'Related work on this site: ' . implode( ' &middot; ', $rel ) . '.'; }
	}

	// 0) Russian / Cyrillic input — detect and respond in Russian before any other handler.
	if ( preg_match( '/[\x{0400}-\x{04FF}]/u', $raw ) ) {
		$ru_greet = $has( [ 'привет', 'здравствуй', 'добрый', 'хай', 'хей', 'здарова', 'что могу', 'расскажи', 'помоги' ] );
		$ru_price = $has( [ 'сколько стоит', 'цена', 'стоимость', 'расценки', 'прайс', 'бюджет', 'дорого', 'дешево' ] );
		$ru_web   = $has( [ 'сайт', 'лендинг', 'веб', 'интернет-магазин', 'магазин' ] );
		$ru_ai    = $has( [ 'чатбот', 'чат-бот', 'бот', 'агент', 'искусственный', 'голосовой' ] );
		if ( $ru_price ) {
			return $nl( [
				"Вот краткий прайс-лист (фиксированные цены):",
				'',
				"&bull; <strong>Сайты</strong> &mdash; Микро $40 &middot; Простой $115 &middot; Полный $300 &middot; Pro+WebGL $600 &middot; SaaS $750",
				"&bull; <strong>AI</strong> &mdash; Пакет подсказок $25 &middot; Чат-бот $190 &middot; Агент $565 &middot; Агент+приложение $1 185",
				"&bull; <strong>SEO</strong> &mdash; Аудит $30 &middot; Старт $75 &middot; Рост $115/мес",
				"&bull; <strong>Брендинг</strong> &mdash; Логотип $30 &middot; Мини $115 &middot; Полный $350",
				'',
				"Расскажите о проекте и бюджете &mdash; подберу нужный уровень.",
			] );
		}
		if ( $ru_web ) {
			return $nl( [
				"Для сайта ближайший вариант: <strong>Простой сайт</strong> &mdash; $115 &mdash; 3–5 страниц, контактная форма, SEO, аналитика. Полный сайт от $300, лендинг от $40.",
				'',
				"Какой бюджет и есть ли жёсткий дедлайн?",
			] );
		}
		if ( $ru_ai ) {
			return $nl( [
				"Чат-бот «Консьерж» (обученный на вашем контенте) &mdash; $190. Кастомный AI-агент с RAG и интеграциями &mdash; $565. Агент + полное приложение &mdash; $1 185.",
				'',
				"Расскажите о проекте: что должен уметь бот и на каком канале работать?",
			] );
		}
		// Generic Russian fallback
		return $nl( [
			"Понял вас! Я Фотон &mdash; консьерж студии Photon-Bounce.",
			'',
			"Что создаём: <strong>сайт</strong>, <strong>AI-агент</strong>, <strong>3D / WebGL</strong>, <strong>SEO</strong> или <strong>брендинг</strong>? Укажите проект и бюджет &mdash; сразу предложу уровень и цену.",
		] );
	}

	// 0a) Studio / capabilities.
	if ( $has( [ 'who are you', 'what can you do', 'what do you do', 'about photon', 'about the studio', 'tell me about yourself', 'who is this', 'what is this', 'what is photon', 'are you ai', 'are you a bot', 'are you real' ] ) ) {
		return $nl( [
			"I&rsquo;m <strong>Photon</strong>, the AI concierge for <a href=\"/\">Photon-Bounce Studio</a> &mdash; a one-person creative-tech studio run by Dmitriy.",
			'',
			"We ship: <strong>custom websites &amp; web apps</strong>, <strong>AI agents &amp; chatbots</strong>, <strong>3D / WebGL experiences</strong>, <strong>SEO / AEO</strong>, and <strong>brand identities</strong> &mdash; fixed prices, source files yours to keep. No subscriptions, no lock-in.",
			'',
			"What are you trying to build?",
		] );
	}

	// 0b) Portfolio / show me your work.
	if ( $has( [ 'portfolio', 'examples', 'show me', 'past work', 'what have you built', 'case stud', 'your work', 'samples', 'previous work', 'your projects', 'what apps', 'your apps', 'what games', 'show me work' ] ) ) {
		return $nl( [
			"Portfolio lives at <a href=\"/portfolio/\">/portfolio/</a> &mdash; projects span web, AI, 3D and brand.",
			'',
			"Live apps you can try right now: <strong><a href=\"/occupantkiller/\">OccupantKiller</a></strong> (browser FPS), <strong><a href=\"/ausis/\">Ausis</a></strong> (audio AI), <strong><a href=\"/friendai/\">FriendAI</a></strong> (senior companion), <strong><a href=\"/guidedmeditation/\">Guided Meditation</a></strong>, and <strong><a href=\"/govdao/\">GovDAO</a></strong> (Web3 governance).",
			'',
			"Want me to walk you through one, or suggest the closest match to your project?",
		] );
	}

	// 0c) Payment / crypto.
	if ( $has( [ 'crypto', 'bitcoin', ' btc', ' eth ', ' sol ', ' usdc', 'cash app', 'cashapp', 'wire transfer', ' ach ', 'how do i pay', 'payment method', 'accept payments', 'do you take', 'paypal', 'venmo', 'stripe', 'how to pay' ] ) ) {
		return $nl( [
			"We accept: <strong>Cash App</strong>, <strong>crypto</strong> (BTC, ETH, SOL, USDC + 100 more via Coinbase Commerce), <strong>check or money order</strong>, and <strong>wire / ACH</strong>. No Stripe or PayPal.",
			'',
			"<em>Note:</em> 10% of every crypto payment is donated to the Ukrainian Army Aid-For-Ukraine wallet &mdash; automatically, at our end.",
			'',
			"What project are we scoping?",
		] );
	}

	// 0d-pre) Tech stack / what technologies.
	if ( $has( [ 'tech stack', 'what technologies', 'what do you use', 'what language', 'what framework', 'built with', 'built in', 'coding language', 'development stack', 'programming language' ] ) ) {
		return $nl( [
			"Stack depends on the project:",
			'',
			"&bull; <strong>Web</strong> &mdash; PHP / WordPress, React / Next.js, vanilla JS, Three.js / WebGL",
			"&bull; <strong>AI</strong> &mdash; GPT-4o / Claude / Gemini via API, LangChain, Pinecone / pgvector for RAG",
			"&bull; <strong>Mobile</strong> &mdash; Kotlin (Android), React Native (cross-platform)",
			"&bull; <strong>3D</strong> &mdash; Three.js, WebXR, Blender, Unity (AR/VR)",
			"&bull; <strong>Backend</strong> &mdash; Node.js, Python, PostgreSQL, REST / JSON",
			'',
			"Every handoff includes source code + docs &mdash; no proprietary tooling that locks you in. What are you building?",
		] );
	}

	// 0d-pre2) Testimonials / reviews.
	if ( $has( [ 'testimonials', 'reviews', 'client feedback', 'clients say', 'success stories', 'references', 'worked with you before', 'what do clients think' ] ) ) {
		return $nl( [
			"Honest answer: the live apps are the best testimonials. You can play <strong><a href=\"/occupantkiller/\">OccupantKiller</a></strong> right now, use <strong><a href=\"/ausis/\">Ausis</a></strong>, or talk to me &mdash; I&rsquo;m the AI concierge Dmitriy built.",
			'',
			"Portfolio with case notes is at <a href=\"/portfolio/\">/portfolio/</a>. For direct references, email <strong>hello@photon-bounce.com</strong> and I can connect you with past clients who agreed to chat.",
		] );
	}

	// 0d-pre3) Solo / team size.
	if ( $has( [ 'do you work alone', 'is this a team', 'who will work on my project', 'how many developers', 'is it just you', 'one person', 'freelancer', 'agency or freelance' ] ) ) {
		return $nl( [
			"It&rsquo;s Dmitriy &mdash; solo, end-to-end. No project managers, no hand-offs between developers, no communication overhead.",
			'',
			"The upside: one person owns the whole build, the quality is consistent, and you always know who to call. The tradeoff: I run 1–2 client slots at a time, so availability matters &mdash; ask early if you have a tight deadline.",
		] );
	}

	// 0d-pre4) Platform-builder alternatives (Shopify / Squarespace / Webflow).
	if ( $has( [ 'shopify', 'squarespace', 'webflow', ' wix', 'godaddy website', 'platform builder', 'no-code', 'nocode', 'template site', 'vs shopify', 'vs squarespace', 'vs webflow' ] ) ) {
		return $nl( [
			"Good question. Platform builders (Shopify, Squarespace, Webflow) are fast and cheap if your needs fit their templates &mdash; but you pay ongoing monthly fees, own nothing, and hit walls the moment you need custom logic.",
			'',
			"Photon-Bounce builds custom, so you get: full source ownership, no monthly platform fee, and zero limits on what the site can do. A <strong>Simple Site</strong> at $115 is the custom alternative to a template site. A <strong>Full Site</strong> at $300 matches what you&rsquo;d pay Squarespace or Webflow in 3–4 months.",
			'',
			"Want a custom build, or are you open to migrating from one of those platforms?",
		] );
	}

	// 0d-pre5) NDA / confidentiality.
	if ( $has( [ ' nda', 'non-disclosure', 'confidentiality', 'is my idea safe', 'will you share my idea', 'keep it secret', 'sign an nda', 'intellectual property', 'ip agreement' ] ) ) {
		return $nl( [
			"Happy to sign an NDA before any detailed conversation. Email <strong>hello@photon-bounce.com</strong> or book a free call at <a href=\"/book/\">/book/</a> and we can have a mutual NDA signed same day.",
			'',
			"By default I treat every client project as confidential and don&rsquo;t discuss details publicly without permission. IP for all custom work transfers to you on final payment.",
		] );
	}

	// 0d-pre6a) Non-technical client.
	if ( $has( [ "i'm not technical", 'i am not technical', 'not a developer', 'not a programmer', "i don't code", 'i cannot code', 'non-technical', 'no coding experience', 'not good with technology', 'i just have an idea', 'just an idea', 'never built a website', "don't understand tech" ] ) ) {
		return $nl( [
			"That&rsquo;s completely fine &mdash; most clients come in without any technical background. You describe <em>what</em> the product needs to do and who it&rsquo;s for; I handle every line of code, every design decision, and every server configuration.",
			'',
			"During the project you get: a plain-English scope doc, weekly Friday demo links so you can click through the real thing, and async video walkthroughs of any decision that affects you. No jargon, no gatekeeping.",
			'',
			"What is the idea? Tell me in plain terms &mdash; even \"I want a website that does X\" is enough to start.",
		] );
	}

	// 0d-pre6b) Post-launch support / maintenance.
	if ( $has( [ 'after launch', 'post launch', 'after delivery', 'what happens after', 'support after', 'ongoing support', 'maintenance plan', 'who maintains', 'updates after', 'will you help after', 'future changes', 'what do i do if something breaks' ] ) ) {
		return $nl( [
			"Two options after delivery:",
			'',
			"1. <strong>Care Plan &mdash; $50/mo</strong>: hosting, security patches, plugin/dependency updates, uptime monitoring, and up to 2 h/mo of content or code edits. Most clients take this &mdash; it means nothing breaks quietly in the background.",
			"2. <strong>Ad-hoc hourly support</strong>: if you just need occasional help, I can quote one-off tasks as they come up.",
			'',
			"The handoff package (full source + docs) means you&rsquo;re never locked to me &mdash; any developer can take over if you ever want that. Would you like to include a Care Plan in your scope?",
		] );
	}

	// 0d-pre6c) Revision / iteration policy.
	if ( $has( [ 'revisions', 'revision rounds', 'revision policy', 'how many changes', 'how many edits', 'can i change things', 'can i request changes', 'iteration', 'feedback rounds', 'how many times can i change', 'change my mind' ] ) ) {
		return $nl( [
			"Every fixed-price project includes <strong>two full revision rounds</strong>:",
			"&bull; Round 1 &mdash; after the first complete draft is delivered",
			"&bull; Round 2 &mdash; after Round 1 feedback is implemented",
			'',
			"A revision round covers the <em>entire</em> deliverable in one pass &mdash; not per-file or per-element micro-edits spread over weeks. That keeps iteration fast.",
			'',
			"If you know ahead of time that your project is likely to need more back-and-forth (unclear requirements, multiple stakeholders, evolving brand), we can agree on a 3-round or retainer arrangement before work starts.",
		] );
	}

	// 0d-pre6) Deadline / urgency.
	if ( $has( [ 'urgent', ' asap', 'as soon as possible', 'i need it by', 'need it next week', 'need it this week', 'can you start today', 'can you start tomorrow', 'rush order', 'rush project', 'really fast', 'how soon can', 'what is your earliest' ] ) ) {
		return $nl( [
			"Rush timelines are doable for most project types:",
			'',
			"&bull; <strong>Micro Page</strong> &mdash; as fast as 24&ndash;48 h",
			"&bull; <strong>Full Site</strong> &mdash; 1&ndash;2 weeks with rush add-on (+20%)",
			"&bull; <strong>AI Concierge bot</strong> &mdash; 3&ndash;5 business days",
			"&bull; <strong>Logo</strong> &mdash; 1&ndash;2 days",
			'',
			"Book a quick call at <a href=\"/book/\">/book/</a> with your deadline and I&rsquo;ll confirm whether it fits the current slot. What&rsquo;s the project and the date you need it live?",
		] );
	}

	// 0d-pre7) Visitor already has designs / mockups.
	if ( $has( [ 'i have a figma', 'i have designs', 'i have a design', 'have designs already', 'already designed', 'have a mockup', 'have mockups', 'existing mockup', 'have wireframes', 'have a prototype', 'have a sketch file' ] ) ) {
		return $nl( [
			"Perfect &mdash; bring your Figma, XD, Sketch, or even a hand-drawn mockup and I&rsquo;ll build from it pixel-for-pixel. Working from existing designs actually <em>speeds up</em> the build and cuts scope ambiguity.",
			'',
			"Just share the file (or a PDF export) when you book. If the mockup is light on mobile layouts or edge states, I&rsquo;ll flag any gaps before starting. What is the project type?",
		] );
	}

	// 0d-pre8) Free / very low budget.
	if ( $has( [ 'for free', 'do it for free', 'pro bono', 'no budget', 'zero budget', 'no money', 'very low budget', 'low budget', 'limited budget', "can't afford", 'too expensive', 'need it cheap', 'need it free' ] ) ) {
		return $nl( [
			"Honest answer: the lowest entry point is $40 for a <strong>Micro Page</strong> &mdash; one polished, performant single page.",
			'',
			"If that&rsquo;s still out of reach right now, here&rsquo;s what I&rsquo;d suggest:",
			"&bull; <strong>$50/mo Care Plan</strong> &mdash; includes hosting, maintenance, and up to 2 h/mo of updates, so you build up your site over time.",
			"&bull; <strong>Free 15-min consultation</strong> at <a href=\"/book/\">/book/</a> &mdash; sometimes the scope is smaller than expected once we talk through it.",
			'',
			"What are you trying to build? Let&rsquo;s see if there&rsquo;s a path that fits.",
		] );
	}

	// 0d-pre9a) Guarantee / refund / satisfaction policy.
	if ( $has( [ 'guarantee', 'money back', 'refund', 'satisfaction guarantee', 'what if i\'m not happy', "what if it doesn't work", 'not satisfied', 'can i get a refund', 'what if i don\'t like it', 'risk', 'protected' ] ) ) {
		return $nl( [
			"Here&rsquo;s the honest policy:",
			'',
			"&bull; Every project starts with a <strong>written scope</strong> you approve before any work begins &mdash; so you&rsquo;re never paying for a surprise.",
			"&bull; <strong>Two full revision rounds</strong> are included. If the delivered work doesn&rsquo;t match the approved scope, I fix it at no charge.",
			"&bull; If we&rsquo;re genuinely stuck after two rounds, I&rsquo;ll offer a partial refund on the milestone that missed the mark &mdash; no ghosting, no argument.",
			'',
			"What I don&rsquo;t offer: refunds for &ldquo;I changed my mind&rdquo; after delivery, since the work product is already built. That&rsquo;s why the upfront scope doc matters. Any other questions?",
		] );
	}

	// 0d-pre9b) White-label / agency sub-contractor work.
	if ( $has( [ 'white label', 'whitelabel', 'white-label', 'agency work', 'sub-contractor', 'subcontractor', 'work under my brand', 'work under my agency', 'resell your services', 'build for my client', 'under my name', 'agency partner', 'reseller' ] ) ) {
		return $nl( [
			"White-label and agency sub-contracting is totally fine &mdash; it&rsquo;s a significant chunk of the work here. How it works:",
			'',
			"&bull; I build under your agency brand / NDA. No Photon-Bounce branding in the deliverable unless you want it.",
			"&bull; Communication style matches your client-facing standard (formal, async, whatever).",
			"&bull; Pricing is the same fixed-rate menu. No agency markup tier from my side &mdash; your margin is yours to set.",
			'',
			"An NDA can be signed before any discussion. What&rsquo;s the project you want to hand off?",
		] );
	}

	// 0d-pre9c) GDPR / data protection / privacy / security.
	if ( $has( [ 'gdpr', 'data protection', 'privacy policy', 'hipaa', 'cookie consent', 'cookie compliance', 'data residency', 'where is my data', 'data stored', 'is it secure', 'security compliance', 'pii', 'user data', 'ccpa', 'cookie banner', 'data privacy' ] ) ) {
		return $nl( [
			"Compliance built in, not bolted on:",
			'',
			"&bull; <strong>GDPR / CCPA</strong>: cookie consent banner, data processing agreement template, and a privacy policy stub are included in every site build.",
			"&bull; <strong>Data storage</strong>: nothing is stored on my servers by default &mdash; form data goes to your CRM or inbox; AI conversations are ephemeral (not logged).",
			"&bull; <strong>Security</strong>: HTTPS always, security headers (CSP, HSTS), WordPress hardening (hidden login, salted keys, file-permission lockdown).",
			"&bull; <strong>HIPAA</strong>: medical clients need a BAA and a HIPAA-hosting provider (Compliancy Group, Aptible). I can configure the stack; you sign the agreements.",
			'',
			"Any specific compliance requirement? I&rsquo;ll tell you upfront if it needs specialist legal counsel vs. a technical implementation.",
		] );
	}

	// 0d-pre9d) Email marketing as a content service (campaigns, newsletters — not technical setup).
	if ( $has( [ 'email marketing', 'email newsletter', 'email campaign', 'email list', 'drip campaign', 'email automation strategy', 'email sequence', 'welcome email', 'newsletter content', 'write my emails', 'email copywriting', 'email funnel', 'email broadcast' ] ) ) {
		return $nl( [
			"Email marketing as a <em>content service</em> (writing, strategy, campaigns) falls under the <strong>SMM / Content</strong> tier &mdash; $75/mo for a monthly calendar that covers email sequences or newsletters alongside social.",
			'',
			"If you need the <em>technical setup</em> &mdash; Mailchimp / Klaviyo / SendGrid account config, automation flows, list segmentation, pixel firing &mdash; that&rsquo;s an integration task bundled into your site or SaaS build, not a separate line item.",
			'',
			"Which side do you need: the writing + strategy, or the technical plumbing?",
		] );
	}

	// 0d-pre9-m) Marketplace / multi-vendor platform.
	if ( $has( [ 'marketplace', 'multi-vendor', 'two-sided platform', 'airbnb like', 'etsy like', 'uber for', 'buy and sell platform', 'p2p platform', 'peer-to-peer', 'rental marketplace', 'service marketplace', 'vendor marketplace', 'classified ads', 'listing marketplace' ] ) ) {
		return $nl( [
			"Two-sided marketplaces are the most complex builds on the menu. Here&rsquo;s what that means for scope:",
			'',
			"&bull; A <strong>basic directory or listing site</strong> (post-to-browse, no payments): <strong>SaaS / App</strong> &mdash; $750",
			"&bull; A <strong>marketplace with payments, escrow, reviews, and user dashboards</strong>: custom quote, typically $1,500&ndash;$3,500 depending on feature depth",
			'',
			"The key variables are: do you need escrow/split payouts (Stripe Connect), user verification, a review system, and admin moderation tools? Tell me what your platform does and I&rsquo;ll sketch a tier in one message.",
		] );
	}

	// 0d-pre9-n) Nonprofit / charity / NGO.
	if ( $has( [ 'non-profit', 'nonprofit', 'not for profit', ' ngo ', ' charity', 'charitable', '501c3', '501 c3', 'fundraising site', 'donation page', 'volunteer organization', 'foundation site', 'community org' ] ) ) {
		return $nl( [
			"Nonprofits are welcome here &mdash; the same fixed-price menu applies. A few practical notes:",
			'',
			"&bull; <strong>Donation pages</strong>: Stripe Checkout or PayPal Giving Fund can be wired in; I don&rsquo;t take a cut, and neither does Stripe for nonprofits with fee-waiver approval.",
			"&bull; <strong>Grant requirements</strong>: if your funder requires WCAG 2.1 AA accessibility, that&rsquo;s standard in every build at no extra charge.",
			"&bull; <strong>Recommended starting point</strong>: Simple Site ($115) covers 3&ndash;5 pages, donation form, contact, and full SEO setup &mdash; everything a lean org needs to be credible online.",
			'',
			"What does your org do and who are you trying to reach?",
		] );
	}

	// 0d-pre9-o) Directory / job board / listing site.
	if ( $has( [ 'directory site', 'listing site', 'job board', 'jobs site', 'business directory', 'local directory', 'review site', 'property listings', 'real estate listing', 'freelancer directory', 'vendor directory', 'professional directory', 'resource directory', 'link directory' ] ) ) {
		return $nl( [
			"Directories and listing sites are <strong>SaaS / App</strong> builds &mdash; from $750 &mdash; because they need a database, a submission form, admin moderation, and search / filter UI.",
			'',
			"Common extras that affect scope:",
			"&bull; <strong>Paid listings / featured spots</strong> &mdash; Stripe integration",
			"&bull; <strong>User accounts</strong> &mdash; add ~$150&ndash;$200",
			"&bull; <strong>Map view</strong> &mdash; Google Maps or Mapbox API",
			"&bull; <strong>Review system</strong> &mdash; adds meaningful complexity",
			'',
			"Tell me: is it a job board, a local business directory, a property site, or something else? I&rsquo;ll give you a more precise figure.",
		] );
	}

	// 0d-pre9-y) CMS / WordPress training and content editing help.
	if ( $has( [ 'how do i edit', 'how do i update', 'how do i add', 'how do i change', 'cms training', 'wordpress training', 'how to use wordpress', 'how to update my site', 'can you teach me', 'show me how to', 'how to edit content', 'post a blog', 'add a page', 'change my homepage', 'update images', 'content management' ] ) ) {
		return $nl( [
			"Every build comes with a <strong>handoff session</strong> &mdash; a 30-min screen-share walkthrough of how to manage your own site. Here&rsquo;s what&rsquo;s covered:",
			'',
			"&bull; Adding / editing pages and posts in the WordPress block editor (Gutenberg)",
			"&bull; Swapping images, logos, and featured photos",
			"&bull; Managing navigation menus and widget areas",
			"&bull; Updating pricing, testimonials, or portfolio items",
			"&bull; Viewing Google Analytics / Search Console basics",
			'',
			"For clients who want more: <strong>extended CMS training</strong> (additional 1-hr session, screen-recorded for your team) is available for $75.",
			'',
			"Is this for you or for a team that will be updating the site?",
		] );
	}

	// 0d-pre9-z) Photography / creative visual assets.
	if ( $has( [ 'product photography', 'brand photography', 'lifestyle photos', 'photos for my site', 'photo editing', 'photo retouching', 'headshots', 'need photos', 'stock photos', 'custom photography', 'shoot my products', 'photography service', 'image creation', 'ai generated images', 'ai images for site' ] ) ) {
		return $nl( [
			"Photography and visual assets aren&rsquo;t an in-house service, but here&rsquo;s how clients typically handle it:",
			'',
			"&bull; <strong>Stock photos</strong> &mdash; I can source from Unsplash, Pexels, or licensed Shutterstock on your behalf (included in any build)",
			"&bull; <strong>AI-generated images</strong> &mdash; Midjourney or DALL-E prompts for custom hero imagery, patterns, or product mockups; add ~$50&ndash;$75 for a full image set",
			"&bull; <strong>Existing photos</strong> &mdash; I&rsquo;ll retouch, crop, and optimize any images you send (WebP conversion, lazy-loading, alt-text for SEO)",
			"&bull; <strong>Referral</strong> &mdash; if you need real product or brand photography, I work with a photographer in my network and can intro you",
			'',
			"What kind of imagery are you missing right now?",
		] );
	}

	// 0d-pre9-z2) Print / physical / merchandise design.
	if ( $has( [ 'business cards', 'flyer design', 'print design', 'brochure', 'banner design', 'merchandise design', 't-shirt design', 'poster design', 'print materials', 'physical materials', 'packaging design', 'label design', 'sticker design', 'billboard', 'physical product design', 'rack card' ] ) ) {
		return $nl( [
			"Print and merchandise design is available as part of the <strong>Brand</strong> service. Here&rsquo;s the scope:",
			'',
			"&bull; <strong>Business cards</strong> &mdash; double-sided, print-ready PDF/AI at 300 dpi; $25 flat",
			"&bull; <strong>Flyer / poster</strong> &mdash; one-sided or folded; from $35",
			"&bull; <strong>Brochure (tri-fold or bi-fold)</strong> &mdash; from $55",
			"&bull; <strong>Merch (t-shirt / tote / mug)</strong> &mdash; vector artwork for print-on-demand platforms (Printful, Printify); from $45 per item",
			"&bull; <strong>Packaging / labels</strong> &mdash; scoped per complexity; start at $75",
			'',
			"All files delivered in both web-ready (PNG/JPG) and print-ready (PDF/AI/EPS) formats.",
			"What do you need designed?",
		] );
	}

	// 0d-pre9-p) Availability / when can you start.
	if ( $has( [ 'when can you start', 'when are you available', "what's your availability", 'what is your availability', 'how soon can you', 'are you available', 'available now', 'how long is your waitlist', 'lead time', 'earliest start', 'kickoff date', 'next opening', 'are you booking', 'taking on clients' ] ) ) {
		return $nl( [
			"Availability depends on the current queue, but here&rsquo;s how the intake works:",
			'',
			"&bull; <strong>Micro &amp; Simple Site</strong> &mdash; typically start within 3&ndash;5 business days of a signed quote",
			"&bull; <strong>E-commerce &amp; SaaS / App builds</strong> &mdash; kick off within 1&ndash;2 weeks; custom scope needs a 30-min discovery call first",
			"&bull; <strong>Monthly retainers</strong> (Care Plan, SMM) &mdash; open most months, but limited slots &mdash; first-come first-served",
			'',
			"The fastest path is to <strong>share your scope in a message and I&rsquo;ll respond within 24 hours</strong> with a start-date estimate.",
			'',
			"Want to book a 15-min call to align on timing?",
		] );
	}

	// 0d-pre9-q) Contact form / lead capture page.
	if ( $has( [ 'contact form', 'lead form', 'lead capture', 'capture leads', 'opt-in form', 'landing page form', 'sign up form', 'email capture', 'coming soon page', 'waitlist page', 'lead magnet page', 'squeeze page', 'lead generation page', 'sign-up form', 'signup form' ] ) ) {
		return $nl( [
			"Contact and lead-capture forms are included in every build &mdash; no extras, no plugin fees.",
			'',
			"Here&rsquo;s what&rsquo;s covered at each tier:",
			"&bull; <strong>Micro Page ($40)</strong> &mdash; perfect for a single opt-in, waitlist, or &ldquo;coming soon&rdquo; page",
			"&bull; <strong>Simple Site ($115+)</strong> &mdash; includes a full contact page, SMTP delivery via your domain, and Mailchimp / ConvertKit connect",
			"&bull; <strong>Any tier</strong> &mdash; GDPR/CCPA cookie consent, honeypot spam protection, and redirect-after-submit included",
			'',
			"Need a dedicated landing page to drive paid traffic? That&rsquo;s a <strong>Micro Page</strong> &mdash; fast to build and easy to A/B test.",
		] );
	}

	// 0d-pre38-a) Dog groomer / pet groomer / mobile dog groomer / pet salon website.
	if ( $has( [ 'dog groomer website', 'dog grooming website', 'pet groomer website', 'pet grooming website', 'mobile dog groomer website', 'mobile grooming website', 'pet salon website', 'dog salon website', 'cat groomer website', 'grooming salon website', 'pet stylist website', 'dog wash website', 'dog grooming parlour website', 'animal grooming website' ] ) ) {
		return $nl( [
			"Dog grooming websites need to convert anxious pet owners into repeat customers &mdash; trust and convenience are the two conversion levers:",
			'',
			"&bull; <strong>Service menu with breed-size pricing</strong> &mdash; full groom / bath &amp; brush / hand strip / scissor finish / nail trim / wash &amp; dry; priced by size (small / medium / large / giant); coat type surcharges (matted / double coat); from <strong>$300</strong>",
			"&bull; <strong>Online booking</strong> &mdash; WP Amelia or SimplyBook.me; breed + service + coat condition; 24h reminder SMS; automated rebooking suggestion at 6&ndash;8 weeks; from <strong>$350</strong>",
			"&bull; <strong>Breed speciality pages</strong> &mdash; one per breed you&rsquo;re known for (Doodles / Cocker Spaniels / Schnauzers / Bichons / Poodles); coat-specific grooming style notes; targets &ldquo;[breed] groomer [town]&rdquo; keyword; from <strong>$150</strong> per page",
			"&bull; <strong>Before and after gallery</strong> &mdash; client photos with consent; filterable by breed; most-viewed and most-converting content on pet grooming sites; shows quality and coat transformation",
			"&bull; <strong>Mobile grooming page</strong> &mdash; service radius map; &ldquo;no salon noise or strange dogs &mdash; less stressful for anxious pets&rdquo; framing; van setup photo; premium over salon pricing justified here; from <strong>$200</strong>",
			"&bull; <strong>Trust signals</strong> &mdash; City &amp; Guilds or iPET Network Level 3 dog grooming qualification; PetPlan Business or Dognanny insurance; pet first aid certificate; British Dog Groomers&rsquo; Association membership",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce gift cards; email delivery; birthdays and Christmas; from <strong>$150</strong>",
			"&bull; <strong>From $450</strong> service menu + booking; <strong>$900+</strong> with breed pages + before/after gallery + mobile grooming page",
			'',
			"Salon, mobile, or both? How many groomers? Do you specialise in a particular breed or style?",
		] );
	}

	// 0d-pre38-b) Life coach / executive coach / business coach / NLP practitioner website.
	if ( $has( [ 'life coach website', 'life coaching website', 'executive coach website', 'business coach website', 'executive coaching website', 'leadership coach website', 'career coach website', 'nlp practitioner website', 'nlp coach website', 'mindset coach website', 'performance coach website', 'online life coach website', 'confidence coach website', 'wellbeing coach website' ] ) ) {
		return $nl( [
			"Coaching websites are different from therapy sites in one important way: coaching is unregulated, so your credibility comes from results, not credentials &mdash; case studies and testimonials do more work than certificates:",
			'',
			"&bull; <strong>ICF or EMCC membership</strong> &mdash; ICF (International Coaching Federation) and EMCC (European Mentoring &amp; Coaching Council) are the nearest equivalent to regulated credentials in an unregulated industry; display prominently; ICF credential levels (ACC / PCC / MCC) signal training hours",
			"&bull; <strong>Core offer page</strong> &mdash; signature programme (12-week / 6-month / 3 x 1:1 sessions); target client avatar at specific-enough detail (&ldquo;ambitious female professional, 35&ndash;50, navigating a career transition&rdquo; beats &ldquo;anyone who wants to grow&rdquo;); before/after transformation arc; clear price or &ldquo;from &pound;X&rdquo;; from <strong>$350</strong>",
			"&bull; <strong>Discovery call booking</strong> &mdash; Calendly; 30&ndash;60 min free call; pre-call questionnaire (what are you working toward? what have you already tried?); auto-reply with the form; from <strong>$250</strong>",
			"&bull; <strong>Case studies and testimonials</strong> &mdash; coaches can share full client names and companies with consent (unlike therapists); video testimonials; LinkedIn recommendation screenshot; specific transformation result (&ldquo;promoted within 4 months&rdquo; beats &ldquo;very helpful coach&rdquo;); from <strong>$200</strong> per story",
			"&bull; <strong>Lead magnet</strong> &mdash; &ldquo;5 steps to [outcome]&rdquo; PDF or 5-day email challenge; Mailchimp or ConvertKit sequence (5&ndash;7 emails); from <strong>$300</strong>",
			"&bull; <strong>Group programme or cohort</strong> &mdash; WooCommerce or Kajabi; waitlist page for next intake; often higher-margin than 1:1; from <strong>$400</strong>",
			"&bull; <strong>Podcast / content hub</strong> &mdash; episode archive or YouTube embed; positions you as thought leader; organic traffic from long-tail queries",
			"&bull; <strong>From $500</strong> offer page + booking + lead magnet; <strong>$1,000+</strong> with case studies + group programme + content hub",
			'',
			"1:1 / group / online / in-person / corporate? What&rsquo;s your niche &mdash; career / leadership / confidence / life direction?",
		] );
	}

	// 0d-pre38-c) Event planner / wedding planner / party planner / corporate event manager website.
	if ( $has( [ 'event planner website', 'wedding planner website', 'party planner website', 'corporate events website', 'event management website', 'event coordinator website', 'event organiser website', 'wedding coordinator website', 'wedding stylist website', 'event styling website', 'luxury events website', 'corporate event planner website', 'event production website', 'birthday party planner website' ] ) ) {
		return $nl( [
			"Event planner websites are often confused with venue or photographer sites &mdash; the planner&rsquo;s value is the co-ordination and relationships, not the location or images:",
			'',
			"&bull; <strong>Service tiers page</strong> &mdash; Full planning (venue search through on-the-day management) / Partial planning (you&rsquo;ve booked the main suppliers, I co-ordinate the rest) / Day-of co-ordination (your plan, my execution) / Styling &amp; d&eacute;cor only; clear scope and price range per tier; sets expectations pre-enquiry; from <strong>$400</strong>",
			"&bull; <strong>Portfolio</strong> &mdash; event categories (weddings / corporate launch / birthday / charity gala / private dinner / team away day); role callout per project (full planning vs styling vs day-of); narrative + detail shots rather than a photographer-style gallery; from <strong>$400</strong>",
			"&bull; <strong>Real event case studies</strong> &mdash; brief + challenge + planner&rsquo;s specific contribution + result; the planner&rsquo;s role IS the story; from <strong>$200</strong> per case study",
			"&bull; <strong>Enquiry form</strong> &mdash; event type + date + guest count + location + budget range + current stage of planning; auto-routes to CRM (Airtable or HoneyBook); from <strong>$250</strong>",
			"&bull; <strong>Supplier network page</strong> &mdash; preferred vendors (venues / caterers / florists / photographers / bands / AV / marquees); reciprocal links; signals to clients that you have established industry relationships",
			"&bull; <strong>Corporate events page</strong> &mdash; B2B tone; product launch / conference / gala dinner / team away day / brand activation; invoice and BACS payment; form with company name + objectives + budget",
			"&bull; <strong>From $550</strong> service tiers + portfolio + enquiry form; <strong>$1,100+</strong> with case studies + corporate page + supplier network",
			'',
			"Weddings only, corporate only, or both? Full planning, day-of, or styling?",
		] );
	}

	// 0d-pre39-a) Flooring installer / floor layer / carpet fitter / flooring company website.
	if ( $has( [ 'flooring installer website', 'flooring company website', 'carpet fitter website', 'floor layer website', 'carpet fitting website', 'hardwood floor website', 'laminate flooring website', 'vinyl flooring website', 'parquet floor website', 'carpet installer website', 'wood floor website', 'flooring contractor website', 'floor fitting website', 'floor installer website', 'lvt flooring website', 'amtico website', 'karndean website', 'engineered wood flooring website' ] ) ) {
		return $nl( [
			"Flooring customers decide on material first, then search for an installer &mdash; your site needs to rank for both:",
			'',
			"&bull; <strong>Flooring type pages</strong> &mdash; one per material (carpet / hardwood / engineered wood / LVT / laminate / parquet / polished concrete / Amtico / Karndean / herringbone); species / grade / construction notes; underfloor heating compatibility; from <strong>$150</strong> per page",
			"&bull; <strong>Room calculator</strong> &mdash; input room dimensions &rarr; estimated m&sup2; with waste factor (10% for LVT and laminate, 15% for herringbone and carpet seaming) &rarr; material quantity; optional labour rate for rough total; from <strong>$350</strong>",
			"&bull; <strong>Before / after gallery</strong> &mdash; filterable by room type (bedroom / kitchen / hallway / open-plan / staircase / office) and material; transformation proof is the highest-converting flooring content; from <strong>$250</strong>",
			"&bull; <strong>Showroom / sample page</strong> &mdash; Matterport 3D tour embed or sample-request form; reduces decision anxiety before the site visit; from <strong>$200</strong>",
			"&bull; <strong>Trade supply page</strong> &mdash; contractor pricing for developers and property managers; bulk order enquiry; sample service; B2B tone; from <strong>$200</strong>",
			"&bull; <strong>Aftercare and maintenance guides</strong> &mdash; cleaning guide per material (LVT, hardwood, carpet); warranty info; reinforces expertise and reduces support calls",
			"&bull; <strong>From $500</strong> type pages + calculator + gallery; <strong>$900+</strong> with showroom page + trade supply + aftercare content",
			'',
			"Residential, commercial, or both? Which materials do you specialise in? Supply-and-fit or fit-only?",
		] );
	}

	// 0d-pre39-b) Cleaning company / office cleaner / domestic cleaner / end-of-tenancy cleaning website.
	if ( $has( [ 'cleaning company website', 'cleaning service website', 'office cleaning website', 'commercial cleaning website', 'domestic cleaning website', 'house cleaning website', 'end of tenancy cleaning website', 'deep cleaning website', 'industrial cleaning website', 'contract cleaning website', 'office cleaner website', 'carpet cleaning website', 'window cleaning website', 'oven cleaning website', 'home cleaning website', 'professional cleaning website', 'cleaning business website' ] ) ) {
		return $nl( [
			"Cleaning sites convert on contract frequency and trust &mdash; these are the pages that win enquiries:",
			'',
			"&bull; <strong>Service pages</strong> &mdash; one per service type (office contract cleaning / domestic regular / end-of-tenancy / carpet extraction / window cleaning / oven cleaning / event cleaning / post-construction clean); scope and frequency options per service; from <strong>$200</strong> per page",
			"&bull; <strong>Online quote form</strong> &mdash; property type + approximate square footage + frequency (one-off / weekly / fortnightly / monthly) + access method (key-hold / alarm code) + optional add-ons; email quote within 24h; from <strong>$250</strong>",
			"&bull; <strong>DBS-checked staff page</strong> &mdash; enhanced DBS note; uniformed and ID-badged engineers; key-holding and alarm-code procedures; public liability and employer&rsquo;s liability insurance amounts displayed; the most-read page for contract cleaning prospects",
			"&bull; <strong>COSHH / eco-cleaning section</strong> &mdash; COSHH-assessed product list; eco option (Force of Nature / Delphis Eco); biodegradable consumables; critical trust signal for food-prep, healthcare, and school contracts",
			"&bull; <strong>End-of-tenancy guarantee</strong> &mdash; re-clean within 72 hours if deposit is deducted due to cleaning; the strongest differentiator in tenant cleaning; from <strong>$150</strong> to add to EOT service page",
			"&bull; <strong>Checklist downloads</strong> &mdash; end-of-tenancy checklist PDF; office handover checklist; helps the client plan AND positions the company as the authority; from <strong>$150</strong>",
			"&bull; <strong>Case studies</strong> &mdash; contract wins (office name / sq ft / frequency / scope if client permits); before and after carpet extraction photos; from <strong>$150</strong> per case study",
			"&bull; <strong>From $550</strong> service pages + quote form + DBS page; <strong>$1,000+</strong> with EOT guarantee + checklists + case studies",
			'',
			"Domestic, commercial contract, or both? Do you cover end-of-tenancy, carpet cleaning, or specialist cleans?",
		] );
	}

	// 0d-pre39-c) Locksmith / emergency locksmith / security company / safe installer / CCTV installer website.
	if ( $has( [ 'locksmith website', 'emergency locksmith website', 'locksmith near me website', 'key cutter website', 'security company website', 'lock company website', 'safe installer website', 'cctv installer website', 'access control website', 'security systems website', 'door entry system website', 'master key system website', 'commercial locksmith website', 'residential locksmith website', 'locksmith business website' ] ) ) {
		return $nl( [
			"Locksmith sites have two trust barriers &mdash; emergency pricing suspicion and &ldquo;is this engineer legitimate?&rdquo; &mdash; here&rsquo;s how to clear both:",
			'',
			"&bull; <strong>Emergency call-out page</strong> &mdash; 24/7 availability; typical response time (20&ndash;30 min radius); NON-DESTRUCTIVE entry emphasis (85% of lock-outs can be opened without drilling &mdash; most customers don&rsquo;t know this and it&rsquo;s your strongest differentiator); fixed call-out rate displayed (avoid &ldquo;from &pound;X&rdquo; which triggers bait-and-switch suspicion); from <strong>$200</strong>",
			"&bull; <strong>Service pages</strong> &mdash; residential (lock change / upgrade / duplicate key / window lock) / commercial (master key system / access control / panic hardware / mortice) / auto (vehicle lock-out / transponder key programming) / emergency (24/7 lock-out / boarding up after break-in); from <strong>$150</strong> per page",
			"&bull; <strong>Cylinder and lock standards page</strong> &mdash; BS EN 1303 / TS 007 3-star / Secured by Design; Ultion / Mul-T-Lock / ASSA ABLOY / Chubb cylinder recommendations; spec comparison; customers who research online know these terms and this page earns the quote",
			"&bull; <strong>Security survey enquiry</strong> &mdash; free security survey &rarr; written quotation &rarr; install pipeline; conversion path for CCTV and access control upsell; from <strong>$200</strong>",
			"&bull; <strong>Trust signals</strong> &mdash; Master Locksmith Association (MLA) approved company badge (fewer than 2,000 in the UK &mdash; display prominently); DBS-checked engineers; public liability insurance amount; no call-out fee (if applicable); genuine Google reviews embed",
			"&bull; <strong>CCTV and access control page</strong> &mdash; CCTV survey + Hikvision / Dahua / Avigilon systems; Paxton / HID access control; door entry and intercom; from <strong>$300</strong>",
			"&bull; <strong>Area pages</strong> &mdash; &ldquo;[area] locksmith&rdquo; + response time + local reviews; up to five radius towns; Local Business schema; from <strong>$100</strong> per page",
			"&bull; <strong>From $500</strong> emergency page + service pages + trust signals; <strong>$1,000+</strong> with CCTV / access control + cylinder standards + area pages",
			'',
			"Residential, commercial, or both? Do you cover auto / vehicle lock-outs? CCTV and access control, or locks only?",
		] );
	}

				// 0d-pre54-a) Estate agent / lettings agent / property developer / property management.
	if ( $has( [ 'estate agent website', 'letting agent website', 'property developer website', 'property management website', 'property website', 'estate agency website', 'real estate website', 'property listing website', 'house sales website', 'property portal website', 'hmo management website', 'buy to let management website', 'property investment website', 'new homes developer website', 'property finder website', 'block management website' ] ) ) {
		return $nl( [
			"Estate agent and property websites must handle a fundamental three-audience problem: vendors wanting the highest price, buyers wanting the lowest price, and landlords/tenants with entirely different priorities &mdash; each needs their own journey through the site:",
			'',
			"&bull; <strong>Property listing and search</strong>: the technical core of any estate agent website; Rightmove and Zoopla data feed integration (CML/REAPIT/Alto/Jupix feed); search by area, price, beds, type; save property and email alert signup; ValPal or similar instant online valuation widget; live listings convert browsers to enquiries; From \$400.",
			"&bull; <strong>Rightmove and Zoopla advertising pages</strong>: UK estate agents are judged by their Rightmove Featured Agent status and Zoopla Premier Agent badges; a page explaining your portal presence builds trust with vendors; From \$100.",
			"&bull; <strong>Instant online valuation (IOV)</strong>: ValPal, Hometrack, or Sprift-powered valuation widget; captures vendor contact details; the #1 lead-generation tool for estate agents; vendors who use an IOV are 4&times; more likely to book a market appraisal; From \$200.",
			"&bull; <strong>Vendor guides and market reports</strong>: &ldquo;How to prepare your home for sale&rdquo;; &ldquo;What is my home worth in [area]?&rdquo;; area price trend data from Land Registry; positions the agent as the local expert before the vendor even contacts them; From \$150.",
			"&bull; <strong>Landlord services page</strong>: fully managed vs rent collection vs let-only fee comparison; Tenant Fees Act 2019 compliance (banned fees clearly stated); deposit protection scheme membership (TDS, DPS, myDeposits); Client Money Protection (CMP) scheme membership display (legally required); ARLA Propertymark member logo; HMO licensing explained; From \$200.",
			"&bull; <strong>Client Money Protection (CMP) compliance</strong>: since April 2019 all letting agents in England must belong to a CMP scheme and display their membership certificate; non-compliance is a criminal offence with up to \$30,000 fine; a dedicated compliance page (CMP + redress scheme + deposit scheme) is legally required; From \$100.",
			"&bull; <strong>Tenant area</strong>: how to apply for a property; referencing process (Goodlord, Vouch, or similar); tenant responsibilities; reporting repairs; deposit protection; right to rent checks; EPC minimum rating C (proposed 2028); From \$150.",
			"&bull; <strong>Property management / block management page</strong>: service charge collection; Section 20 consultation; ground rent demands; building insurance procurement; contractor management; a dedicated page converts landlord inquiries that national agencies handle poorly; From \$150.",
			"&bull; <strong>Redress scheme membership</strong>: all UK estate agents and letting agents must belong to a government-approved redress scheme (Property Ombudsman or Property Redress Scheme); logo and membership number must appear on the website; non-display is an offence; From \$80.",
			"&bull; <strong>Area pages / local market insight</strong>: hyper-local content for each neighbourhood served (&ldquo;Harrogate property market&rdquo;); average sold prices; school catchments; commute times; outranks national portals for long-tail local searches; From \$150/page.",
			'',
			"From \$600 for a Rightmove-connected estate agent site with IOV &mdash; \$1,500+ with live listings, area reports, landlord portal, and full compliance pages.",
			'',
			"Sales, lettings, or both? Geographic area? Residential, commercial, or HMO specialist? Existing CRM (Reapit, Alto, Jupix)?",
		] );
	}

	// 0d-pre54-b) Hotel / boutique hotel / B&B / holiday cottage / serviced apartment / glamping.
	if ( $has( [ 'hotel website', 'boutique hotel website', 'bed and breakfast website', 'bb website', 'holiday cottage website', 'holiday let website', 'holiday rental website', 'serviced apartment website', 'glamping website', 'guest house website', 'airbnb website', 'self catering website', 'lodge website', 'resort website', 'country house hotel website', 'wedding venue website' ] ) ) {
		return $nl( [
			"Hotel and holiday accommodation websites have one overriding job: convert the guest who has already found you &mdash; either on Booking.com, TripAdvisor, or via Google &mdash; to book direct instead of through the OTA so you keep the 15-25% commission:",
			'',
			"&bull; <strong>Direct booking engine</strong>: the most important element on any accommodation website; Booking.com, Expedia, and Airbnb take 15-25% commission; a channel manager with a booking engine (Beds24, Lodgify, Hostelworld, ResNexus, Little Hotelier, Booking.com Pulse, Rezovation) embedded on the site converts direct bookings at zero commission; rate parity clause awareness; From \$300.",
			"&bull; <strong>Best rate guarantee banner</strong>: legally confirming that the direct price will always match or beat OTA price (within rate parity constraints); converts OTA lookers to direct bookers; some payment systems can trigger an automatic 10% &ldquo;book direct&rdquo; discount; From \$100.",
			"&bull; <strong>Rooms and accommodation pages</strong>: each room or cottage as its own page with professional photography; dimensions; maximum occupancy; bed configuration; view; ensuite vs shared facilities; accessibility information; prices per night/week; minimum stay; high-quality copy that sells the experience not just lists facts; From \$200.",
			"&bull; <strong>Photography and virtual tour</strong>: the most ROI-positive investment for any accommodation website; professional hospitality photography; 360&deg; virtual tour (Matterport); drone exterior; seasonal shots; photography increases direct conversion rates by up to 40% vs stock or phone images; From \$250 (photography budget separate).",
			"&bull; <strong>Experiences and local area guide</strong>: what to do in [area]; recommended restaurants; local attractions; seasonal events; walking routes; this content both ranks for destination searches and reduces guest anxiety about visiting a new area; From \$150.",
			"&bull; <strong>Gift vouchers</strong>: online gift voucher sale via GiftPro, Giftpak, or WooCommerce; high-margin, zero-inventory revenue; especially effective around Christmas, Valentine&rsquo;s Day, Mother&rsquo;s Day; From \$200.",
			"&bull; <strong>Weddings and events page</strong>: if the property hosts weddings or private events; capacity; catering options; accommodation for guests; exclusive hire; this page often generates the highest average transaction value on the entire site; From \$200.",
			"&bull; <strong>Spa and dining menus</strong>: downloadable PDF menus updated seasonally; spa treatment list and prices; afternoon tea; private dining; upsell opportunities embedded at the time of booking; From \$150.",
			"&bull; <strong>TripAdvisor and Google Reviews integration</strong>: schema markup for hotel reviews; TripAdvisor certificate of excellence widget; Google star rating in search results; social proof reduces booking anxiety; From \$100.",
			"&bull; <strong>Accessibility statement</strong>: required under the Equality Act 2010; step-free access; hearing loops; adapted bathrooms; parking; specific requirements page converts guests who cannot book blind; From \$100.",
			'',
			"From \$600 for a direct-booking hotel site with channel manager integration &mdash; \$1,500+ with virtual tour, gift vouchers, weddings page, and spa/dining.",
			'',
			"Hotel, B&B, self-catering cottage, or glamping? Number of rooms/units? Existing channel manager or booking system? Weddings or events?",
		] );
	}

	// 0d-pre54-c) Event photographer / wedding photographer / videographer / commercial photographer.
	if ( $has( [ 'photographer website', 'photography website', 'wedding photographer website', 'event photographer website', 'videographer website', 'commercial photographer website', 'portrait photographer website', 'family photographer website', 'newborn photographer website', 'product photographer website', 'aerial photographer website', 'drone photographer website', 'corporate photographer website', 'music photographer website', 'sports photographer website', 'fashion photographer website' ] ) ) {
		return $nl( [
			"Photography websites live or die on two things: the speed at which the portfolio loads and how quickly a potential client can imagine YOU telling their story &mdash; the biggest mistake photographers make is a slow site with beautiful photos, or a fast site with generic copy:",
			'',
			"&bull; <strong>Portfolio gallery (optimised)</strong>: the centrepiece of any photographer website; WebP or AVIF compressed images (no JPEG for web); lazy loading; gallery organised by genre (weddings, events, portraits, commercial) not chronologically; Squarespace, Format, SmugMug, or custom WordPress with Envira Gallery; 15-25 curated hero images per gallery not 150 mediocre ones; full-bleed layout; From \$300.",
			"&bull; <strong>Image optimisation pipeline</strong>: a slow portfolio is the death of a photography website; full-resolution images served as-is can exceed 6MB per photo; WebP conversion + responsive srcset + CDN delivery (Cloudflare or BunnyCDN); Google PageSpeed target 90+ on mobile; From \$200.",
			"&bull; <strong>Packages and pricing page</strong>: wedding photography pricing is notoriously opaque; couples who cannot find pricing bounce to the next photographer; clear packages (coverage hours, albums, engagement session, second shooter); elopement vs full day; corporate day rates vs half-day; From \$150.",
			"&bull; <strong>Inquiry form and availability calendar</strong>: HoneyBook, Dubsado, or Calendly integration; check-availability without requiring a call; captures name, date, venue, style preferences, budget range; automated inquiry response email; wedding photographers lose 40% of leads to slow follow-up; From \$200.",
			"&bull; <strong>Testimonials and styled real weddings</strong>: full-day real wedding blog posts (&ldquo;Sophie &amp; James at [Venue Name]&rdquo;) rank for &ldquo;[venue] wedding photographer&rdquo; searches; structured as storytelling editorial not just a gallery dump; 800+ words including venue name, florist, dress designer, cake maker for full cross-referencing SEO; From \$150/post.",
			"&bull; <strong>About page with story</strong>: clients hire the photographer not the camera; personality, approach, what happens on the day, how to feel comfortable in front of a lens; this converts the undecided lead who is choosing between three photographers on identical pricing; From \$150.",
			"&bull; <strong>Style guide / FAQ page</strong>: what to wear for a portrait session; what happens if it rains on your wedding day; how long until you receive your images; what is the delivery format; how many images to expect; anticipates every pre-booking anxiety; From \$100.",
			"&bull; <strong>Location pages</strong>: &ldquo;Wedding photographer in [City]&rdquo;; &ldquo;Event photographer [Region]&rdquo;; each as a separate page; photographers who cover multiple cities need location pages for each market; ranks before larger directories for specific searches; From \$100/page.",
			"&bull; <strong>Client galleries / delivery portal</strong>: Pixieset, Pic-Time, or Shootproof for private client gallery delivery; download, print ordering, album design; embedded on the photographer&rsquo;s own domain for brand consistency; also displays client work as a proof-of-expertise portfolio; From \$200.",
			"&bull; <strong>Styled shoot collaborations page</strong>: industry partnerships with venues, florists, stylists; tagged features in wedding blogs (Junebug Weddings, Styled Shoots UK, Wedding Wire); establishes authority in the luxury wedding market; From \$100.",
			'',
			"From \$550 for a portfolio-led photographer site with inquiry form &mdash; \$1,200+ with real-wedding blog posts, location pages, client gallery portal, and image optimisation.",
			'',
			"Wedding, event, portrait, or commercial specialism? How many locations do you cover? Existing booking/CRM system (HoneyBook, Dubsado)?",
		] );
	}

// 0d-pre53-a) Solicitor / law firm / barrister / legal practice / conveyancer.
	if ( $has( [ 'solicitor website', 'law firm website', 'lawyer website', 'legal practice website', 'conveyancer website', 'conveyancing website', 'barrister website', 'family law website', 'employment law website', 'immigration lawyer website', 'personal injury website', 'criminal solicitor website', 'commercial law website', 'property lawyer website', 'wills solicitor website', 'probate solicitor website' ] ) ) {
		return $nl( [
			"Legal websites must thread a very specific needle: they must project authority and expertise while remaining completely accessible to a lay client who is often frightened, confused, or in the middle of a crisis:",
			'',
			"&bull; <strong>SRA-regulated solicitor notices</strong>: the Solicitors Regulation Authority (SRA) requires that all regulated law firms display their SRA number, the SRA logo, and a link to the SRA register; chambers websites must display Bar Standards Board (BSB) regulated status; compliance is mandatory and missing notices can result in regulatory action; From \$100.",
			"&bull; <strong>Practice area pages</strong>: conveyancing (residential and commercial); family law (divorce, child arrangements, TOLATA, cohabitants); employment law (unfair dismissal, settlement agreements, discrimination); personal injury (RTA, EL/PL, clinical negligence); immigration (EEA applications, skilled worker visas, indefinite leave to remain); wills and probate (LPA, estate administration, inheritance tax planning); commercial (contracts, company formation, shareholder disputes); each as its own page with a jargon-free explanation of what the process involves and what the client needs to bring; From \$200.",
			"&bull; <strong>Fixed-fee pricing pages</strong>: the SRA&rsquo;s Price Transparency Rules (effective December 2018) require all SRA-regulated law firms to publish prices for residential conveyancing, employment tribunal, motoring offences, immigration (excluding asylum), wills, probate, and debt recovery; failure to publish is an SRA compliance breach; a clean pricing page also converts better than vague &ldquo;call for a quote&rdquo; copy; From \$200.",
			"&bull; <strong>Free initial consultation CTA</strong>: law is a high-consideration purchase; a 30-minute free consultation call is the primary conversion goal; book via Calendly or a dedicated legal intake form (name, matter type, urgency); a prominent CTA on every page; From \$150.",
			"&bull; <strong>Meet the team / solicitor profiles</strong>: SRA-regulated solicitor (years of qualification); specialist accreditations (Law Society Conveyancing Quality Scheme CQS; Children Panel; Personal Injury Accreditation; Resolution-accredited family lawyer); LinkedIn; approach; approachability converts; From \$150.",
			"&bull; <strong>Testimonials and case studies</strong>: heavily anonymised or aggregated (GDPR and professional conduct rules); focus on outcome, timeline, and client experience not specific case details; Google Reviews schema markup; the legal field has a trust gap that social proof bridges; From \$150.",
			"&bull; <strong>Legal guides / FAQ hub</strong>: &ldquo;How long does conveyancing take?&rdquo;; &ldquo;What is an LPA and do I need one?&rdquo;; &ldquo;Can I apply for a spouse visa myself?&rdquo;; content marketing positions the firm as the authority in the client&rsquo;s search result before they even think about picking up the phone; From \$150.",
			"&bull; <strong>Legal Aid page</strong>: if the firm holds a Legal Aid Agency contract; eligibility explained clearly; practice areas covered (family, immigration, mental health, crime); clients accessing legal aid are particularly price-sensitive and need this information upfront; From \$100.",
			"&bull; <strong>Complaints procedure page</strong>: SRA and Legal Ombudsman require solicitors to publish a complaints procedure; must include the Legal Ombudsman contact details and 8-week deadline; a mandatory page that also demonstrates client-centricity; From \$80.",
			'',
			"From \$700 for an SRA-compliant, practice-area-led law firm website &mdash; \$1,500+ with conveyancing calculator, case tracking portal, full FAQ hub, and legal aid section.",
			'',
			"SRA-regulated firm or Bar (BSB-regulated chambers)? Which practice areas? Legal aid contract? England/Wales, Scotland (Law Society of Scotland), or NI?",
		] );
	}

	// 0d-pre53-b) Mortgage broker / independent financial adviser (IFA) / wealth manager.
	if ( $has( [ 'mortgage broker website', 'mortgage adviser website', 'ifa website', 'independent financial adviser website', 'financial adviser website', 'wealth management website', 'financial planner website', 'pension adviser website', 'investment adviser website', 'equity release website', 'buy to let mortgage website', 'remortgage website', 'first time buyer website', 'financial coach website', 'chartered financial planner website', 'whole of market broker website' ] ) ) {
		return $nl( [
			"Mortgage broker and IFA websites operate in one of the most heavily regulated sectors for digital marketing in the UK &mdash; FCA rules on financial promotions, risk warnings, and fair presentation are not optional, and breaches can result in fines or FCA enforcement:",
			'',
			"&bull; <strong>FCA registration and financial promotion compliance</strong>: FCA firm reference number (FRN) must appear on all communications including the website; standard risk warnings required on all pages discussing investments (e.g., &ldquo;Your home may be repossessed if you do not keep up repayments on your mortgage&rdquo;); FCA financial promotion sign-off required for any claim about performance, returns, or rates; missing FCA notices are an immediate compliance breach; From \$100.",
			"&bull; <strong>Mortgage calculator</strong>: borrowing capacity (salary multiple + deposit inputs); monthly payment estimator (rate, term, loan amount); stamp duty land tax (SDLT) calculator; repayment vs interest-only comparison; calculators are the single most-used page on any mortgage broker website and drive enormous organic traffic from people in the early research phase; From \$350.",
			"&bull; <strong>Mortgage types pages</strong>: first-time buyer; home mover; remortgage; buy-to-let; let-to-buy; bridging finance; equity release (regulated by the Equity Release Council); self-employed mortgage; contractor mortgage; Help to Buy / shared ownership; each as its own page with a clear client-journey explanation; From \$200.",
			"&bull; <strong>Affordability and broker process</strong>: what documents you&rsquo;ll need (payslips, bank statements, ID, credit report); what the broker does at each stage; Decision in Principle (DIP) explained; timeline from application to completion; managing client expectations converts browsers to enquiries; From \$150.",
			"&bull; <strong>Protection insurance pages</strong>: life insurance; critical illness cover; income protection; buildings and contents; fee-income from protection is the biggest cross-sell for a mortgage broker; dedicated pages convert this audience; From \$150.",
			"&bull; <strong>IFA / financial planning pages</strong>: pension consolidation and reviews; SIPP; retirement planning; inheritance tax (IHT) planning; ISA and investment portfolio review; Chartered Financial Planner (CFP) or Chartered status signals trust to high-net-worth clients; From \$200.",
			"&bull; <strong>Online mortgage enquiry form</strong>: first-time buyer vs remortgage; property value; deposit; employment type; credit history self-assessment; passes pre-qualification information to the broker before the first call; reduces cold calls; From \$200.",
			"&bull; <strong>Reviews and client stories</strong>: Trustpilot / Google Reviews aggregation; Vouched For for IFAs specifically (the dominant IFA review platform); star rating schema markup; testimonials convert mortgage leads more than almost any other asset; From \$100.",
			"&bull; <strong>Lender panel page</strong>: showing which lenders you have access to (Halifax, Santander, NatWest, specialist lenders like Kensington, Together, Precise) establishes that you are whole-of-market; From \$100.",
			'',
			"From \$650 for an FCA-compliant mortgage broker website with calculators &mdash; \$1,500+ with mortgage tool suite, IFA services, protection pages, and client portal.",
			'',
			"FCA-authorised directly or as an Appointed Representative? Mortgage-only, IFA, or both? Specialist niches (self-employed, equity release, expat mortgages)?",
		] );
	}

	// 0d-pre53-c) Architect / architectural practice / building designer / structural engineer.
	if ( $has( [ 'architect website', 'architectural practice website', 'building designer website', 'architecture firm website', 'architect studio website', 'structural engineer website', 'planning consultant website', 'interior architect website', 'landscape architect website', 'architect portfolio website', 'riba architect website', 'permitted development website', 'house extension architect website', 'new build architect website', 'conservation architect website', 'architectural technologist website' ] ) ) {
		return $nl( [
			"Architect websites must do something unusual: they must sell a highly visual, deeply technical service to clients who are often making the largest purchase of their lives and who feel completely out of their depth &mdash; the best architectural websites balance stunning visual portfolio with reassuring process transparency:",
			'',
			"&bull; <strong>Portfolio / case studies gallery</strong>: the single most important page; residential extensions; new builds; commercial fit-outs; heritage and conservation projects; planning-sensitive schemes; before/after; drone photography; floor plans alongside lifestyle photography; each project needs a planning location (e.g., &ldquo;House Extension, Richmond, London Borough&rdquo;), brief description, and key stats (area, budget range, planning authority); this page both ranks for local project searches and converts visitors who found you via Instagram; From \$350.",
			"&bull; <strong>ARB-registered architect notices</strong>: the Architects Registration Board (ARB) protects the title &ldquo;architect&rdquo; in the UK &mdash; only ARB-registered individuals may use it; ARB registration number should appear on the website; RIBA membership (Royal Institute of British Architects) is additional chartered status; RIBA Chartered Practice membership signals quality assurance; these notices establish legitimacy and are often the difference between a call and a bounce; From \$100.",
			"&bull; <strong>Services and RIBA stages page</strong>: RIBA Plan of Work stages 0&ndash;7 (Strategic Definition through to In Use); which stages the practice covers; full design and build oversight vs design-only; planning application services; interior specification; project management and contract administration; clients do not know what architects actually do &mdash; a clear &ldquo;what we do at each stage&rdquo; page is one of the highest-converting pages on an architect website; From \$200.",
			"&bull; <strong>Planning application pages</strong>: permitted development vs full planning permission explained; planning authority search (England/Wales/Scotland/NI rules differ); pre-application consultation; listed building consent; conservation area consents; clients who understand the process are more likely to contact you before they call a builder; From \$200.",
			"&bull; <strong>Fees and process page</strong>: RIBA fee guidance (percentage of construction cost vs lump sum vs hourly); what affects the fee; payment milestones tied to RIBA stages; a fee calculator embed (even a rough one reduces fee-sticker shock); VAT on architect fees (standard-rated vs zero-rated for new build); From \$150.",
			"&bull; <strong>Residential project types pages</strong>: single-storey extension; double-storey extension; loft conversion; basement excavation; new build; barn conversion; listed building renovation; each as its own page for local SEO (&ldquo;loft conversion architect [city]&rdquo;); From \$150/page.",
			"&bull; <strong>Local planning authority knowledge</strong>: demonstrate specific knowledge of the local authority (e.g., &ldquo;We have submitted over 40 planning applications to the London Borough of Richmond&rdquo;); planning success rate; committee appearances; local officers known by name; hyper-local trust that a national firm cannot replicate; From \$100.",
			"&bull; <strong>3D visualisation and CGI page</strong>: photorealistic renders; planning CGIs; VR walkthroughs; drone surveys; increasingly expected by clients making a six-figure investment decision; From \$150.",
			"&bull; <strong>Sustainability and PassivHaus page</strong>: BREEAM; PassivHaus; Fabric First; SAP calculations; Part L compliance; EPC improvements; sustainability is a growing decision factor for planning authorities and homeowners; From \$150.",
			'',
			"From \$650 for an ARB/RIBA-compliant architect portfolio site &mdash; \$1,500+ with project-type pages, 3D visualisation gallery, planning authority content, and fee calculator.",
			'',
			"ARB-registered architect or architectural technologist? RIBA Chartered Practice? Residential only or commercial/heritage too? Which local planning authorities do you work in?",
		] );
	}

// 0d-pre52-a) Funeral director / funeral home / funeral parlour / crematorium.
	if ( $has( [ 'funeral director website', 'funeral home website', 'funeral parlour website', 'funeral service website', 'crematorium website', 'funeral plans website', 'funeral chapel website', 'memorial website', 'bereavement website', 'funeral arranger website', 'funeral celebrant website', 'graveside service website', 'funeral cost website', 'direct cremation website', 'natural burial website', 'green funeral website' ] ) ) {
		return $nl( [
			"Funeral director websites serve two completely different audiences simultaneously: a family in acute grief searching at 2am for immediate help, and someone pre-planning calmly weeks in advance &mdash; the design must never confuse the two:",
			'',
			"&bull; <strong>Immediate contact / 24-hour line</strong>: the most critical element on any funeral website; prominently displayed phone number with a 24/7 availability statement; families need instant reassurance you can help right now; From \$100.",
			"&bull; <strong>What to do when someone dies</strong>: a calm step-by-step guide; when to call a funeral director; registering the death; obtaining the death certificate; this page ranks for the highest-intent searches on the site; From \$200.",
			"&bull; <strong>Services pages</strong>: traditional funeral with burial; cremation; direct cremation (unattended); natural/green/woodland burial; humanist/civil/religious ceremonies; repatriation; each as a separate page with a clear price guide; CMA regulations require price transparency for funeral directors in England &amp; Wales (since September 2021); From \$200.",
			"&bull; <strong>CMA price transparency compliance</strong>: the Competition and Markets Authority legally requires all funeral directors in England and Wales to publish standardised price lists online; non-compliance risks investigation and fines; a fully compliant price page is not optional; From \$150.",
			"&bull; <strong>Pre-paid funeral plans page</strong>: FCA-regulated since 2022; providers include Safe Hands, Golden Charter, Dignity Plans, Co-op Funeralcare; locks in today&rsquo;s price; national market for pre-planning enquiries; From \$200.",
			"&bull; <strong>Memorial and tribute pages</strong>: online obituary submission form; live-stream link for remote family members; memorial garden / Book of Remembrance; condolence messages; charitable donations in lieu of flowers; increasingly expected by families; From \$250.",
			"&bull; <strong>Meet the team</strong>: professional yet warm photography; NAFD / SAIF / BIFD membership logos; local roots; families choose a funeral director almost entirely on trust; From \$150.",
			"&bull; <strong>NAFD / SAIF membership</strong>: National Association of Funeral Directors (NAFD) or Society of Allied and Independent Funeral Directors (SAIF); code of practice; independent vs chain distinction; From \$100.",
			"&bull; <strong>Bereavement support resources</strong>: links to Cruse Bereavement Care, WAY Widowed and Young; local support groups; positions the funeral director as a long-term community partner; From \$150.",
			"&bull; <strong>Area coverage / local landmark</strong>: clear map and service area; local cemetery and crematorium names; ranks for &ldquo;funeral director [area]&rdquo; which is a very high-value local search; From \$100.",
			'',
			"From \$600 for a compassionate, CMA-compliant funeral director site &mdash; \$1,400+ with pre-paid plans, live-stream, online tribute, and full pricing.",
			'',
			"Sole practitioner or group? Any specialism (direct cremation, natural burial, repatriation)? England/Wales (CMA required) or Scotland/NI?",
		] );
	}

	// 0d-pre52-b) Optician / optometrist / contact lens clinic / eyewear boutique.
	if ( $has( [ 'optician website', 'optometrist website', 'eye test website', 'eyewear website', 'glasses website', 'contact lens website', 'optical practice website', 'eye care website', 'spectacles website', 'sunglasses website', 'laser eye surgery website', 'dry eye clinic website', 'myopia management website', 'children optician website', 'optical boutique website', 'independent optician website' ] ) ) {
		return $nl( [
			"Independent optician websites must convert both the routine NHS eye test customer and the fashion-conscious customer choosing designer frames &mdash; two completely different value propositions needing two different pages:",
			'',
			"&bull; <strong>Online appointment booking</strong>: the single most important page; NHS vs private; contact lens check; OCT scan; children&rsquo;s eye tests; Acuity Scheduling, Optinet, or similar real-time slot picker; patients decide based on booking convenience; From \$250.",
			"&bull; <strong>Frames and eyewear gallery / online shop</strong>: brand showcases (Lindberg, Silhouette, Tom Ford, Ray-Ban); price ranges; virtual try-on API (Ditto or similar); or WooCommerce for prescription glasses online; frames drive footfall as much as eye tests; From \$400.",
			"&bull; <strong>Eye tests and services pages</strong>: standard NHS eye test (GOS voucher information); private eye test; children&rsquo;s sight test (free under 16); OCT scan; visual fields; driving standard checks; each as its own page; From \$200.",
			"&bull; <strong>GOC registration</strong>: General Optical Council registration number for each optometrist and dispensing optician is a legal requirement to practice; registration number should appear on the team page with a GOC register verify link; From \$100.",
			"&bull; <strong>NHS GOS voucher information</strong>: NHS sight test entitlement (children, over-60, diabetes, glaucoma risk, income-based); GOS voucher values for spectacles and contact lenses; clear explanation converts NHS patients who don&rsquo;t know they&rsquo;re entitled; From \$150.",
			"&bull; <strong>Contact lens subscription / home delivery</strong>: Acuvue, CooperVision, Alcon brands; monthly or annual supply; online lens ordering for existing patients; subscription repeat-delivery; recurring revenue and strong patient retention; From \$300.",
			"&bull; <strong>Specialist clinics pages</strong>: dry eye clinic (TearLab, LipiFlow); myopia management (Orthokeratology, MiSight); low vision; keratoconus; children&rsquo;s vision therapy; these high-value pages have very low search competition; From \$150/page.",
			"&bull; <strong>Laser eye surgery referral page</strong>: if offering pre-assessment or referral to a LASIK/LASEK/SMILE clinic; co-management pathway; patient journey; From \$150.",
			"&bull; <strong>Lens technology page</strong>: single vision, varifocal, occupational; anti-reflection, photochromic (Transitions), blue light filter; lens comparisons convert upgrade decisions; From \$150.",
			'',
			"From \$600 for a bookings-led NHS optician site &mdash; \$1,400+ with online frame shop, contact lens subscription, OCT and dry eye clinic pages.",
			'',
			"Independent practice or small group? NHS GOS or predominantly private? Any specialist area (myopia management, dry eye, low vision)?",
		] );
	}

	// 0d-pre52-c) Nutritionist / dietitian / health coach / weight management clinic.
	if ( $has( [ 'nutritionist website', 'dietitian website', 'nutrition coach website', 'health coach website', 'weight management website', 'wellness coach website', 'nutrition consultant website', 'sports nutritionist website', 'clinical nutritionist website', 'registered dietitian website', 'functional medicine website', 'gut health website', 'eating disorder website', 'diabetes dietitian website', 'nutrition therapy website', 'wellness practitioner website' ] ) ) {
		return $nl( [
			"Nutrition and dietitian websites must immediately establish the critical professional distinction: a <strong>Registered Dietitian</strong> (HCPC-protected title, NHS-recognised) vs a <strong>nutritionist</strong> (unprotected title in the UK, hugely variable qualifications) &mdash; failure to do so leads to mistrust:",
			'',
			"&bull; <strong>HCPC / UKVRN / BDA / BANT registration</strong>: HCPC registration number for Registered Dietitians (legally protected title); UK Voluntary Register of Nutritionists (UKVRN) RNutr or ANutr for qualified nutritionists; BDA (British Dietetic Association) membership; BANT (British Association for Applied Nutrition and Nutritional Therapy) membership; credentials above the fold on every page; From \$100.",
			"&bull; <strong>Areas of specialism pages</strong>: gut health (IBS, IBD, SIBO, microbiome); weight management; sports and performance nutrition; eating disorders (ARFID, BED &mdash; only for qualified specialists); type 2 diabetes; PCOS; pregnancy and fertility; paediatric nutrition; oncology; each condition as its own page ranking for &ldquo;[condition] dietitian&rdquo; searches; From \$150/page.",
			"&bull; <strong>Services and packages page</strong>: initial consultation (60-90 mins); follow-up sessions; programme packages (6-week gut reset, 12-week weight management); corporate nutrition; online via Zoom; clear per-session and package pricing; From \$200.",
			"&bull; <strong>Booking integration</strong>: Calendly or Practice Better (specialist nutrition software); intake form including current medication (essential for RD scope-of-practice); From \$250.",
			"&bull; <strong>Recipe hub / free resources</strong>: free recipe downloads (IBS-friendly meal plan, 7-day gut health reset) gated behind an email opt-in; builds the mailing list; From \$200.",
			"&bull; <strong>Blog / evidence-based nutrition content</strong>: &ldquo;Does the gut microbiome affect mental health?&rdquo;; &ldquo;Is intermittent fasting safe for women?&rdquo;; high Google traffic from health-anxious searches; From \$100/post.",
			"&bull; <strong>Corporate wellness page</strong>: staff webinars; canteen menu consultation; executive health programmes; B2B income stream with very different conversion process; From \$150.",
			"&bull; <strong>Media and press page</strong>: TV/radio/podcast appearances; national press quotes; establishes authority that separates a Registered Dietitian from unregulated influencer nutritionists; From \$100.",
			"&bull; <strong>NHS and GP referral pathway</strong>: for Registered Dietitians taking NHS referrals alongside private work; EMIS/SystmOne integration; Choosing Wisely guidance; From \$150.",
			'',
			"From \$550 for a credentials-first nutrition practice site &mdash; \$1,200+ with condition pages, recipe hub, booking integration, and corporate wellness section.",
			'',
			"Registered Dietitian (HCPC) or nutritionist (UKVRN/BANT)? Any specialism (gut health, eating disorders, sports)? NHS contract or private only?",
		] );
	}

// 0d-pre51-a) Veterinary practice / vet surgeon / animal hospital / pet clinic website.
	if ( $has( [ 'vet website', 'veterinary website', 'veterinary practice website', 'animal hospital website', 'pet clinic website', 'vet surgery website', 'vet clinic website', 'animal clinic website', 'exotic vet website', 'equine vet website', 'farm vet website', 'large animal vet website', 'emergency vet website', 'vet nurse website', 'veterinary surgeon website', 'pet hospital website' ] ) ) {
		return $nl( [
			"Veterinary practice websites must simultaneously serve pet owners in emergencies (fast phone number, out-of-hours cover) and convert prospective clients choosing a vet for the first time &mdash; these two modes require completely different design priorities:",
			'',
			"&bull; <strong>Emergency / out-of-hours contact</strong>: the most important element on any vet website; red/prominent phone number; out-of-hours number if different; 24/7 emergency line partner (e.g. Vets Now) if used; visible above the fold on mobile where 90% of emergency searches happen. This is the #1 reason people visit a vet site in a panic; From \$100.",
			"&bull; <strong>Services page</strong>: routine consultations; vaccinations; neutering/spaying; dental cleaning; emergency and trauma; diagnostic imaging (X-ray, ultrasound); in-house laboratory; physiotherapy/hydrotherapy if offered; pharmacy; specialist referrals; From \$150.",
			"&bull; <strong>Online booking / appointment request</strong>: Vetstoria, PetsApp, or RxWorks online booking plugin; new patient registration form; existing patient appointment request; pet species + reason for visit; the single biggest operational efficiency win for a busy practice; From \$250.",
			"&bull; <strong>Meet the team page</strong>: RCVS-registered vets with membership numbers (verifiable on the RCVS register); vet nurses; reception; photos and specialisms (&lsquo;Certificate in Small Animal Surgery&rsquo;, &lsquo;Advanced Practitioner in Dermatology&rsquo;); clients choose a vet as much as a practice; From \$150.",
			"&bull; <strong>RCVS Practice Standards page</strong>: RCVS Practice Standards Scheme accreditation (General / Veterinary Hospital / Referral); RCVS logo; what it means for patient care; RVN qualifications; From \$100.",
			"&bull; <strong>Pet health information / symptom guides</strong>: &ldquo;Is my dog in pain?&rdquo;, &ldquo;When to call an emergency vet&rdquo;, &ldquo;Cat vaccination schedule UK&rdquo; &mdash; high-traffic content that builds trust and captures new-patient traffic from Google; reduces non-urgent phone calls; From \$150.",
			"&bull; <strong>Pet health plans / preventive care membership</strong>: monthly direct-debit health plan (vaccinations, flea/worming, annual health check) bundled as a subscription; increases client retention; specific plan levels for kittens, puppies, adult, senior; From \$200.",
			"&bull; <strong>Species-specific pages</strong>: if the practice handles exotic species (rabbits, birds, reptiles, small mammals), guinea pigs etc. separate pages rank for &ldquo;rabbit vet near me&rdquo; which has very low competition; From \$100/page.",
			"&bull; <strong>Pharmacy/online shop</strong>: prescription flea treatment, worming, joint supplements; Viovet or VetShop partnership; or own WooCommerce pharmacy with prescription verification workflow; From \$300.",
			'',
			"From \$500 for an emergency-first vet site &mdash; \$1,100+ with online booking, health plans, pet health library, and online pharmacy.",
			'',
			"Single site or multi-site group? Species focus (small animal, equine, farm, exotic)? 24/7 cover or partner out-of-hours?",
		] );
	}

	// 0d-pre51-b) Dentist / dental practice / cosmetic dentist / orthodontist / implant centre.
	if ( $has( [ 'dentist website', 'dental practice website', 'dental website', 'cosmetic dentist website', 'orthodontist website', 'dental implants website', 'dental surgery website', 'teeth whitening website', 'invisible braces website', 'invisalign website', 'dental clinic website', 'emergency dentist website', 'private dentist website', 'nhs dentist website', 'dental plan website', 'smile clinic website' ] ) ) {
		return $nl( [
			"Dental websites must build trust before the first appointment &mdash; dental anxiety is one of the most common phobias, so reducing fear through transparency, friendly photography, and clear process descriptions converts hesitant browsers into booked patients:",
			'',
			"&bull; <strong>New patient welcome page</strong>: &ldquo;Taking new patients?&rdquo; is the #1 question; clear NHS/private status; what to expect at a first appointment; registration process; what to bring; this page ranks for &ldquo;dentist taking new patients [area]&rdquo; which is searched by thousands of anxious patients every week; From \$200.",
			"&bull; <strong>Online appointment booking</strong>: Dentally, Software of Excellence, or Exact with online booking module; emergency appointment slot prominent; new patient vs existing; From \$250.",
			"&bull; <strong>Treatments page with before/afters</strong>: teeth whitening; composite bonding; veneers; Invisalign/Smilelign; implants; crowns; dentures; each page should include before/after photos (with patient consent); GDC-compliant treatment descriptions; From \$200.",
			"&bull; <strong>Meet the dentists page</strong>: GDC registration number for each dentist (legally required on marketing material in the UK); photo; qualifications; special interests (&lsquo;interest in cosmetic dentistry&rsquo;, &lsquo;caring for nervous patients&rsquo;); GDC number must be displayed and verifiable; From \$150.",
			"&bull; <strong>GDC registration and CQC/HTM 01-05 compliance</strong>: GDC registration numbers legally required; CQC registration number for England; HTM 01-05 infection control compliance statement; reassures patients about standards; From \$100.",
			"&bull; <strong>Nervous patients page</strong>: dental anxiety is the #1 barrier to booking; dedicated page explaining sedation options (relative analgesia/happy air, IV sedation), gentle approach, what patients say; converts a very high-intent anxious searcher; From \$150.",
			"&bull; <strong>Dental finance / payment plans page</strong>: Chrysalis Finance or Medenta 0% finance; monthly payment calculator; this is the biggest objection to cosmetic treatment (&ldquo;I can&rsquo;t afford veneers&rdquo;) and removing it with transparent finance options converts well; From \$150.",
			"&bull; <strong>Dental plan (preventive membership) page</strong>: monthly direct-debit plan covering 2 check-ups, 2 hygienist visits, X-rays, emergency cover; Denplan or DPAS partnership; creates predictable recurring revenue and retains patients; From \$150.",
			"&bull; <strong>Emergency dentist page</strong>: separate page targeting &ldquo;emergency dentist [area]&rdquo; &mdash; toothache, broken tooth, lost filling, abscess; emergency appointment phone number; out-of-hours guidance; this page ranks quickly and converts distressed patients immediately; From \$100.",
			'',
			"From \$600 for a trust-first practice site &mdash; \$1,300+ with online booking, treatment pages with before/afters, finance calculator, and nervous patient guide.",
			'',
			"NHS, private, or mixed? Any cosmetic/aesthetic specialism (veneers, Invisalign, implants)? Nervous patient focus?",
		] );
	}

	// 0d-pre51-c) Physiotherapist / sports therapist / osteopath / chiropractor / manual therapist.
	if ( $has( [ 'physiotherapist website', 'physio website', 'physiotherapy website', 'sports therapist website', 'osteopath website', 'osteopathy website', 'chiropractor website', 'chiropractic website', 'sports injury website', 'sports massage website', 'manual therapy website', 'rehabilitation website', 'acupuncture website', 'sports physio website', 'musculoskeletal physio website', 'private physio website' ] ) ) {
		return $nl( [
			"Physiotherapy and manual therapy websites convert on condition expertise and practitioner trust &mdash; the client is in pain and searching for someone who understands their specific problem:",
			'',
			"&bull; <strong>Condition / injury pages</strong>: one page per major condition or injury; back pain; knee pain; shoulder impingement; sciatica; sports injuries; ACL rehabilitation; plantar fasciitis; tennis elbow; neck pain; rotator cuff; each page ranks for &ldquo;[condition] physiotherapy [area]&rdquo; and converts directly from search intent; From \$150/page.",
			"&bull; <strong>Online appointment booking</strong>: Power Diary, Cliniko, or Physitrack online booking; new vs returning patient; condition/service type; practitioner preference; slot availability in real-time; the single biggest operational improvement for any solo or group practice; From \$250.",
			"&bull; <strong>Practitioner profiles</strong>: HCPC registration number (mandatory for physiotherapists in the UK); BSc/MSc Physiotherapy; specialist interests; years of experience; treatment approach (manual therapy, exercise therapy, sports rehabilitation, acupuncture); From \$150.",
			"&bull; <strong>HCPC / CSP / GOsC / GCC registration</strong>: Health and Care Professions Council (HCPC) for physiotherapists; General Osteopathic Council (GOsC) for osteopaths; General Chiropractic Council (GCC) for chiropractors; registration number with verify link is a legal requirement and trust signal; From \$100.",
			"&bull; <strong>Services and treatment techniques page</strong>: manual therapy (joint mobilisation, manipulation); soft tissue work; dry needling/acupuncture; exercise therapy; taping (Kinesio/RockTape); electrotherapy (ultrasound, TENS); biomechanical assessments; gait analysis; From \$150.",
			"&bull; <strong>Online exercise programmes / patient portal</strong>: Physitrack or PhysiApp patient portal; prescribed home exercises with video; progress tracking; telehealth video appointments; increasingly expected by patients post-pandemic; From \$200.",
			"&bull; <strong>Sports team / corporate partnership page</strong>: if the practice treats local sports teams or has corporate wellbeing partnerships; first aid cover; pitch-side physiotherapy; on-site corporate physio; B2B conversion completely different to B2C; From \$150.",
			"&bull; <strong>Blog / injury guides</strong>: &ldquo;Should I ice or heat a muscle injury?&rdquo;; &ldquo;How long does a sprained ankle take to heal?&rdquo;; &ldquo;Exercises for lower back pain&rdquo;; high Google traffic; positions clinician as expert; From \$100/post.",
			"&bull; <strong>Telehealth / video appointments page</strong>: remote consultation for advice, exercise programme review, and post-op monitoring; serves national market; patients who have moved away from your area; From \$150.",
			'',
			"From \$500 for a condition-led practice site &mdash; \$1,100+ with condition pages, online booking, exercise portal, and sports/corporate page.",
			'',
			"Solo practitioner or group practice? Any sports specialism? HCPC physiotherapist, osteopath (GOsC), or chiropractor (GCC)?",
		] );
	}

	// 0d-pre50-a) Accountant / bookkeeper / chartered accountant / tax advisor website.
	if ( $has( [ 'accountant website', 'chartered accountant website', 'bookkeeper website', 'accounting firm website', 'tax advisor website', 'tax accountant website', 'small business accountant website', 'self employed accountant website', 'cpa website', 'payroll website', 'management accountant website', 'accountancy practice website', 'financial accountant website', 'vat accountant website', 'company accounts website', 'annual accounts website' ] ) ) {
		return $nl( [
			"Accountancy websites convert on trust, credentials, and clarity about who you serve &mdash; a prospective client needs to quickly determine &ldquo;do you work with businesses like mine?&rdquo; before they make contact:",
			'',
			"&bull; <strong>Specialism/niche homepage messaging</strong>: the fastest-converting accountancy sites are explicit about who they serve &mdash; e.g. &ldquo;We look after creative freelancers and small agencies&rdquo; or &ldquo;Specialist accountants for landlords and property investors&rdquo;. Generalist messaging converts poorly because every firm says the same thing; From \$200.",
			"&bull; <strong>Services pages</strong>: one page per service; self-assessment/personal tax returns; limited company accounts; VAT returns; payroll; bookkeeping; management accounts; R&amp;D tax credits; CIS; MTD compliance; HMRC investigations. Each page should answer &ldquo;what&rsquo;s included?&rdquo;, &ldquo;who is this for?&rdquo;, and &ldquo;what does it cost?&rdquo;; From \$150/page.",
			"&bull; <strong>Fixed-fee pricing page</strong>: the single biggest conversion lever in accountancy; &ldquo;what will my accountant cost?&rdquo; is the second-most-Googled accountancy question; publishing a tiered price guide (sole trader / limited company / SME) eliminates tyre-kickers and dramatically improves enquiry quality; From \$200.",
			"&bull; <strong>ICAEW / ACCA / CIMA registration</strong>: professional body membership logo prominently displayed; regulated accountant vs unregulated bookkeeper distinction matters to clients; links to verify registration on official sites; From \$100.",
			"&bull; <strong>Making Tax Digital (MTD) page</strong>: UK businesses are legally required to submit VAT under MTD; a page explaining what MTD means, what changes are coming (MTD for Income Tax), and how you help migrates anxious Google searches directly to your enquiry form; From \$150.",
			"&bull; <strong>Onboarding process / &lsquo;how it works&rsquo; page</strong>: step-by-step from first contact to year-end accounts; software used (Xero/QuickBooks/Sage/FreeAgent partnership/certification logos); typical turnaround times; what documents the client provides; From \$200.",
			"&bull; <strong>Software partner logos</strong>: Xero Silver/Gold/Platinum Partner, QuickBooks ProAdvisor, FreeAgent Partner logos are trust signals because they imply training and volume &mdash; they convert above pure text credentials; From \$100.",
			"&bull; <strong>Tax deadline calendar / blog</strong>: &ldquo;SA100 deadline&rdquo;, &ldquo;corporation tax payment window&rdquo;, &ldquo;VAT return due&rdquo; &mdash; high-traffic, evergreen content that attracts business owners in a panic; each post funnels to your contact form; From \$100/post.",
			"&bull; <strong>Client testimonials with business context</strong>: &ldquo;Jane, freelance graphic designer, saved 3 hours a month&rdquo; is more powerful than &ldquo;great service&rdquo;; context matches the reader to the testimonial; From \$100.",
			'',
			"From \$500 for a credentials-led practice site &mdash; \$1,000+ with niche messaging, fixed-fee pricing page, MTD guide, and onboarding process.",
			'',
			"Do you specialise by client type (sole traders, SMEs, landlords, creatives) or service type (tax, payroll, R&amp;D)? UK-only or international clients?",
		] );
	}

	// 0d-pre50-b) Solicitor / law firm / conveyancer / family lawyer / legal services website.
	if ( $has( [ 'solicitor website', 'law firm website', 'legal website', 'conveyancer website', 'conveyancing website', 'family lawyer website', 'family law website', 'employment lawyer website', 'immigration lawyer website', 'personal injury lawyer website', 'commercial lawyer website', 'property lawyer website', 'will writer website', 'legal services website', 'barrister website', 'criminal lawyer website' ] ) ) {
		return $nl( [
			"Legal websites face a dual challenge: they must communicate authority and expertise to command premium fees, while also feeling accessible to clients who may be frightened, grieving, or in dispute &mdash; the tone must be both confident and human:",
			'',
			"&bull; <strong>Practice area pages</strong>: one comprehensive page per area; family law; conveyancing; wills and probate; employment law; personal injury; commercial; immigration; criminal defence; each page should be genuinely informative (not just a service list) &mdash; a 1,000-word page explaining what divorce proceedings involve converts far better than a 100-word list of what you offer; From \$200/page.",
			"&bull; <strong>SRA / Law Society registration</strong>: Solicitors Regulation Authority number and regulated-firm badge must appear on every page (footer or header); it&rsquo;s a regulatory requirement and a conversion signal; verification link to the SRA register reassures clients checking credentials; From \$100.",
			"&bull; <strong>Initial consultation booking</strong>: most firms offer a free or fixed-fee initial 30-minute consultation; a simple form (matter type + brief description + preferred contact method) converts significantly better than just a phone number; Calendly for lawyers with own availability calendar; From \$200.",
			"&bull; <strong>Fixed-fee transparency page</strong>: the SRA mandates publication of prices for six specific areas (conveyancing, probate, employment tribunal, motoring offences, immigration, debt recovery); non-compliance risks SRA referral; compliance-first approach and clear fee guides also convert better than &ldquo;call for a quote&rdquo;; From \$250.",
			"&bull; <strong>Team / lawyer profiles</strong>: named lawyers with photo, year of qualification, specialisms, languages spoken, professional body memberships (Law Society, Resolution, ACTAPS); clients choose a lawyer as much as a firm; profiles drive direct enquiries to specific fee earners; From \$150.",
			"&bull; <strong>Testimonials and case studies</strong>: legal testimonials must be carefully worded (&ldquo;excellent advice and support&rdquo; is fine; promising outcomes is not allowed); anonymous case studies showing the type of matter handled and general outcome build confidence without outcome guarantees; From \$150.",
			"&bull; <strong>Client guides / FAQs</strong>: &ldquo;How long does conveyancing take?&rdquo;, &ldquo;What is the divorce process in the UK?&rdquo;, &ldquo;What happens at an employment tribunal?&rdquo; &mdash; informational guides that rank for high-intent searches and position the firm as a trusted adviser before the first call; From \$150.",
			"&bull; <strong>Legal jargon glossary</strong>: accessible plain-English explanations of common legal terms; clients appreciate not feeling talked down to; ranks well for long-tail search; From \$100.",
			"&bull; <strong>Complaints procedure page</strong>: legally required; professional handling of this page actually increases confidence (transparent firms feel safer to instruct); From \$50.",
			'',
			"From \$600 for a regulated firm site with SRA compliance &mdash; \$1,400+ with practice area pages, fixed-fee transparency, lawyer profiles, and client guides.",
			'',
			"Which practice areas? Any regulatory compliance requirements beyond SRA? Primarily consumer or commercial clients?",
		] );
	}

	// 0d-pre50-c) Estate agent / letting agent / property management company website.
	if ( $has( [ 'estate agent website', 'letting agent website', 'property agent website', 'estate agency website', 'letting agency website', 'property management website', 'property company website', 'real estate agent website', 'landlord service website', 'property management company website', 'buy to let website', 'holiday let website', 'short term rental website', 'property investment website', 'sales and lettings website', 'residential property website' ] ) ) {
		return $nl( [
			"Estate agent and lettings websites must do two things simultaneously: convince vendors/landlords to instruct you, and convince buyers/tenants to search with you &mdash; these are different audiences with conflicting motivations, and most agency sites fail to serve either well:",
			'',
			"&bull; <strong>Property search / listings page</strong>: a functioning property search with filtering (bedrooms, price, area, type); even a small independent agent should have this; it creates a reason to return to the site; MLS/Rightmove/Zoopla feed integration via the agent&rsquo;s existing CRM; or a custom property database; From \$400.",
			"&bull; <strong>Valuation landing page</strong>: the single highest-value page for an agent; &ldquo;What is my home worth?&rdquo; is one of the most searched phrases in UK property; instant online valuation (API from HM Land Registry price data) or &ldquo;Book a free valuation&rdquo; form; the primary lead-generation tool for seller/vendor acquisition; From \$250.",
			"&bull; <strong>Landlord services page</strong>: separate page for landlords not owner-sellers; full management vs rent-only; tenant-find only; guaranteed rent; HMO management; fee schedules (percentage of rent vs fixed fee); legal compliance services (EPC, gas safe, EICR, Right to Rent); From \$200.",
			"&bull; <strong>Why choose us / differentiators</strong>: independent vs corporate; local expertise; average days to sell/let (number); percentage of asking price achieved; Google review rating; Rightmove/Zoopla partner logos; ARLA Propertymark / NAEA Propertymark membership logos; From \$150.",
			"&bull; <strong>ARLA / NAEA / The Property Ombudsman membership</strong>: client money protection scheme logo; ARLA Propertymark member badge; The Property Ombudsman or Property Redress Scheme logo (legally required for lettings agents in England); these reduce risk perception for cautious landlords and vendors; From \$100.",
			"&bull; <strong>Area guides</strong>: one page per area covered; local amenities; average house prices; transport links; schools; these pages rank well for &ldquo;houses for sale in [area]&rdquo; and &ldquo;flats to rent in [area]&rdquo; and are underused by most independent agents; From \$200/area.",
			"&bull; <strong>Staff profiles / team page</strong>: vendors and landlords instruct the person, not the company; named negotiators and managers with photos and years of local experience convert significantly better than anonymous &ldquo;our team&rdquo; copy; From \$150.",
			"&bull; <strong>Testimonials / Google reviews feed</strong>: Google review rating and count embedded; vendors need social proof before inviting you to value their home; From \$150.",
			"&bull; <strong>Blog / market updates</strong>: &ldquo;[Area] property market report Q1 2025&rdquo;; &ldquo;Average rental yield in [town]&rdquo;; positions the firm as local market authority; strong SEO value for area + property type searches; From \$150/post.",
			'',
			"From \$600 for a listings + valuation site &mdash; \$1,400+ with full property search, landlord page, area guides, and market reports.",
			'',
			"Sales, lettings, or both? Any specialist focus (HMO, holiday lets, commercial)? Independent or franchise?",
		] );
	}

	// 0d-pre49-a) Childminder / nursery / childcare / after-school club / day nursery website.
	if ( $has( [ 'childminder website', 'nursery website', 'childcare website', 'day nursery website', 'after-school club website', 'childcare provider website', 'childminder site', 'pre-school website', 'preschool website', 'creche website', 'childcare setting website', 'childminding website', 'wrap around care website', 'out of school club website', 'holiday club website', 'daycare website' ] ) ) {
		return $nl( [
			"Childcare and nursery websites serve two equally important audiences: parents (who need confidence, safeguarding reassurance, and booking clarity) and Ofsted (whose registration number and grade should be immediately visible). Both must be satisfied before an enquiry is made:",
			'',
			"&bull; <strong>Ofsted grade and registration number</strong>: the single biggest trust signal for UK childcare; display prominently on the homepage and footer; link to the Ofsted inspection report on the official website (opens in new tab). Outstanding or Good grade in a badge/banner converts immediately. From \$100.",
			"&bull; <strong>Availability and session booking page</strong>: term-time session availability (mornings/afternoons/full days); funded 15/30 hours eligibility and how to apply; a waitlist sign-up form for popular time slots; breakfast club/after-school/holiday club options; From \$250.",
			"&bull; <strong>Fees and funding calculator</strong>: parents hate mystery pricing in childcare; even a simple grid of session fees per day + funded hours eligibility check removes the biggest friction point and reduces phone enquiries asking only about price; From \$200.",
			"&bull; <strong>Safeguarding and DBS page</strong>: DBS check status for all staff; safeguarding policy PDF download; first aid certificates; food hygiene; Paediatric First Aid; these are non-negotiable for parent confidence and are legally required information in some formats; From \$150.",
			"&bull; <strong>A day in the life / daily routine page</strong>: parents choosing a setting want to visualise their child&rsquo;s day; EYFS learning framework references; outdoor play; mealtimes; sleep routines for babies; ratio of staff to children by age group; From \$200.",
			"&bull; <strong>Parent testimonials and gallery</strong>: real photos of the setting (not stock); children shown with explicit parental media consent note; Ofsted parent view link; From \$150.",
			"&bull; <strong>Staff profiles page</strong>: parents form an attachment to the specific carer before their child starts; named staff with photo, qualifications (Level 3 CCLD, EYPS, QTS), years of experience, and a short personal note; From \$150.",
			"&bull; <strong>Online registration / enquiry form</strong>: child&rsquo;s name + DOB + required sessions + start date + contact; integrated with email; From \$150.",
			"&bull; <strong>Blog / newsletter archive</strong>: monthly updates showing recent activities, new resources, upcoming events; builds trust with prospective parents and keeps Google indexing fresh content; From \$100.",
			'',
			"From \$500 for a settings-led trust site &mdash; \$900+ with fee calculator, session booking, and Ofsted badge.",
			'',
			"Registered childminder or group setting? Do you take funded 15/30 hours? Any specialist provision (SEN, bilingual, Forest School)?",
		] );
	}

	// 0d-pre49-b) Music teacher / music school / music tutor / singing teacher / instrument lessons.
	if ( $has( [ 'music teacher website', 'music tutor website', 'music school website', 'music lessons website', 'singing teacher website', 'guitar teacher website', 'piano teacher website', 'drum teacher website', 'violin teacher website', 'music lesson website', 'music studio website', 'instrument lessons website', 'music academy website', 'online music lessons website', 'vocal coach website', 'music theory website' ] ) ) {
		return $nl( [
			"Music teacher websites need to communicate ability and personality before a parent books a trial lesson &mdash; the teacher IS the product, so the journey from &ldquo;found on Google&rdquo; to &ldquo;booked a trial&rdquo; should take under 60 seconds:",
			'',
			"&bull; <strong>Teacher profile / about page</strong>: performing experience; qualifications (ABRSM/RCM/Trinity teaching diploma; DBS check; music degree); teaching style (relaxed/classical/exam-focused/genre-specific); instruments and ages taught; own performing video if possible. The single most-visited page &mdash; From \$200.",
			"&bull; <strong>Online lesson booking</strong>: Calendly, Acuity, or BookWhen; weekly availability in real-time; trial lesson booking separate from regular lesson booking; automatic Zoom/Google Meet link for online lessons; From \$250.",
			"&bull; <strong>Lesson options and fees page</strong>: by instrument; by duration (20/30/45/60 min); in-person/online/hybrid; adult vs child rates; ABRSM/Trinity exam preparation packages; group vs individual; From \$150.",
			"&bull; <strong>Student progress videos / audio samples</strong>: short clips of students performing (with parental permission); recital highlights; nothing converts a parent faster than hearing a real pupil who started 6 months ago; From \$150.",
			"&bull; <strong>Exam results page</strong>: ABRSM/Trinity pass rates and distinctions; builds enormous credibility for exam-focused parents; even a simple &ldquo;100% pass rate across 47 candidates in 2024&rdquo; is powerful; From \$100.",
			"&bull; <strong>FAQ page</strong>: &ldquo;What age can my child start?&rdquo;; &ldquo;Do we need a piano at home?&rdquo;; &ldquo;Do you teach adults?&rdquo;; &ldquo;What happens if we need to cancel?&rdquo;; &ldquo;Do you teach complete beginners?&rdquo; &mdash; reduces phone calls and reassures anxious parents; From \$150.",
			"&bull; <strong>Genre and style showcase</strong>: classical vs jazz vs pop vs rock vs musical theatre; some parents specifically want their child to learn &lsquo;fun songs&rsquo; not just scales; targeting these genre searches (&ldquo;jazz piano teacher London&rdquo;) captures high-intent traffic; From \$100.",
			"&bull; <strong>Local SEO page</strong>: &ldquo;Piano teacher in [area]&rdquo;; &ldquo;Guitar lessons near [town]&rdquo;; one page per instrument per location for multi-teacher schools; Google Business profile integrated; From \$150.",
			"&bull; <strong>Online lessons page</strong>: post-pandemic, many teachers now teach nationally via Zoom; this page opens up a national market &mdash; &ldquo;piano lessons anywhere in the UK&rdquo;; From \$150.",
			'',
			"From \$450 for a tutor-led profile site &mdash; \$850+ with real-time booking, exam results, student showcase, and local SEO.",
			'',
			"Solo teacher or music school with multiple teachers? Which instruments? Primarily children, adults, or both? Exam-focused or recreational?",
		] );
	}

	// 0d-pre49-c) Personal trainer / PT studio / fitness instructor / online coach / gym website.
	if ( $has( [ 'personal trainer website', 'pt website', 'fitness trainer website', 'personal training website', 'fitness instructor website', 'online coach website', 'online coaching website', 'fitness coach website', 'gym website', 'pt studio website', 'fitness studio website', 'bootcamp website', 'nutrition coach website', 'strength coach website', 'crossfit website', 'pilates instructor website' ] ) ) {
		return $nl( [
			"Personal trainer websites convert on transformation proof and personality &mdash; before a client commits to 3 months of training they need to believe both that you can get them results AND that they&rsquo;ll enjoy working with you:",
			'',
			"&bull; <strong>Before/after transformation gallery</strong>: the highest-converting element on any PT website; real client results with written permission; body composition changes; performance milestones (&ldquo;ran first 5k&rdquo;, &ldquo;deadlifted 2x bodyweight&rdquo;); From \$200.",
			"&bull; <strong>Services and packages page</strong>: 1-2-1 in-person; group PT; online coaching (asynchronous or live Zoom); nutrition coaching; hybrid packages; 4/8/12-week programmes; monthly recurring vs block booking. Price clearly or with a &lsquo;from&rsquo; anchor &mdash; mystery pricing increases bounce; From \$200.",
			"&bull; <strong>Online coaching page</strong>: the scalable income stream; how it works (weekly check-ins, app, training programme delivery, WhatsApp support); which app (Trainerize, MyPTHub, TrueCoach); national and international clients; From \$150.",
			"&bull; <strong>Free consultation / taster session booking</strong>: the primary CTA for in-person clients; Calendly or Acuity; 20-minute free call to discuss goals; the single highest-converting action a PT website can offer; From \$200.",
			"&bull; <strong>Client testimonials with results</strong>: written testimonials alongside the transformation photo; specific numbers (&ldquo;lost 12kg in 16 weeks&rdquo;, &ldquo;increased bench press from 60 to 100kg&rdquo;); avoids vague &ldquo;great trainer&rdquo; reviews which convert poorly; From \$150.",
			"&bull; <strong>About / credentials page</strong>: Level 2 Gym Instructor + Level 3 Personal Trainer (CIMSPA/REPs registered); specialist qualifications (pre/postnatal, sports performance, nutrition, corrective exercise); current DBS if working with minors; public liability insurance; From \$150.",
			"&bull; <strong>Specialist niche page</strong>: pre/postnatal fitness; over-50s; powerlifting/strength; weight loss; running coaching; sport-specific conditioning; niching dramatically improves Google ranking and inquiry quality; From \$150.",
			"&bull; <strong>Location and facilities page</strong>: if studio-based: equipment list, parking, changing facilities, location; if mobile PT: areas covered; if online-only: emphasise app and check-in process; From \$100.",
			"&bull; <strong>Blog / content marketing</strong>: &ldquo;How many times a week should I train?&rdquo;, &ldquo;Best exercises for weight loss at home&rdquo;, &ldquo;What to eat before a workout&rdquo; &mdash; high-traffic informational content that attracts prospects at the top of funnel; From \$150/post.",
			'',
			"From \$500 for a trainer-led conversion site &mdash; \$950+ with before/after gallery, online coaching page, package builder, and booking.",
			'',
			"In-person, online, or hybrid? Any specialist niche (pre/postnatal, over-50s, sport-specific)? Solo trainer or team?",
		] );
	}

	// 0d-pre48-a) Florist / wedding florist / flower shop / event florist website.
	if ( $has( [ 'florist website', 'flower shop website', 'florist shop website', 'wedding florist website', 'event florist website', 'floral designer website', 'flower delivery website', 'local florist website', 'florist near me website', 'bouquet website', 'funeral flowers website', 'sympathy flowers website', 'corporate flowers website', 'flower subscription website', 'dried flower website', 'flower studio website' ] ) ) {
		return $nl( [
			"Florist websites convert on visual impact and instant ordering &mdash; the photography IS the product, and the booking/order journey must be frictionless:",
			'',
			"&bull; <strong>Gallery with occasion tabs</strong>: weddings; funerals; corporate; birthday; new baby; get well; anniversary; seasonal. Professional photography essential &mdash; phone photos lose orders. Each image should show the finished arrangement in situ (not just on a white background). From $250.",
			"&bull; <strong>Online ordering / e-commerce</strong>: WooCommerce or Shopify; product pages per arrangement style with size/price variants; same-day delivery option prominently displayed; local delivery postcode checker; From $400.",
			"&bull; <strong>Wedding consultation page</strong>: the single highest-value page for a florist site; bride-specific landing page with bridal bouquets, table centres, ceremony arches, buttonholes; consultation booking form (date + venue + style + approximate budget); From $250.",
			"&bull; <strong>Seasonal collections / occasion pages</strong>: Valentine&rsquo;s Day, Mother&rsquo;s Day, Christmas, and Sympathy separate pages; keyword-rich; Google seasonal search spikes; From $100/page.",
			"&bull; <strong>Funeral flowers page</strong>: sympathetic tone; tribute types (coffin sprays, wreaths, posies, hearts, letters); direct order or phone; free delivery to funeral directors often converts. This page generates significant revenue and is under-invested in by most florists. From $200.",
			"&bull; <strong>Flower subscriptions</strong>: weekly or fortnightly bouquet delivery; Stripe recurring billing; gifting option (&ldquo;send flowers for 3 months&rdquo;); growing revenue stream. From $250.",
			"&bull; <strong>Corporate accounts page</strong>: reception flowers, event centrepieces, office deliveries, account invoicing; B2B buyer different mindset from B2C; From $150.",
			"&bull; <strong>Real wedding gallery / blog</strong>: one post per wedding (couple consent); each post ranks for &ldquo;[venue name] florist&rdquo; and &ldquo;[area] wedding florist&rdquo;; From $100/post.",
			"&bull; <strong>Google Business integration</strong>: link from website to Google Reviews; &ldquo;florist near me&rdquo; searches convert best via Google Maps, so the site should reinforce the local address. From $100.",
			'',
			"From \$450 for a gallery-led shop site &mdash; \$900+ with e-commerce ordering, wedding consultation landing page, and subscription billing.",
			'',
			"What&rsquo;s the primary focus &mdash; retail orders, weddings, or corporate? Delivery only, or walk-in shop too?",
		] );
	}

	// 0d-pre48-b) Interior designer / interior decorator / home stager / design consultant website.
	if ( $has( [ 'interior designer website', 'interior design website', 'interior decorator website', 'home stager website', 'home staging website', 'interior design studio website', 'interior design consultant website', 'residential interior designer website', 'commercial interior designer website', 'kitchen designer website', 'bathroom designer website', 'home renovation website', 'soft furnishings website', 'colour consultant website', 'e-design website', 'virtual interior design website' ] ) ) {
		return $nl( [
			"Interior design websites sell a vision before a relationship &mdash; the portfolio must make the buyer feel their home could look like that, before they pick up the phone:",
			'',
			"&bull; <strong>Portfolio / project pages</strong>: one page per project with before/after photography, room type, brief/challenge/solution narrative; minimum 8 projects live; RIBA/BIID accreditation logo if applicable. Photography is the single biggest investment &mdash; bad photos kill good work. From $300.",
			"&bull; <strong>Style quiz or consultation request</strong>: 5-question style quiz (modern/traditional/eclectic/maximalist/Scandi etc.) that leads to a tailored package recommendation; or simply a &ldquo;Book a discovery call&rdquo; form (project type + budget range + timeline). Discovery calls convert at 40&ndash;60&% for well-qualified leads. From $250.",
			"&bull; <strong>Services and process page</strong>: full design (concept to completion); soft furnishings only; e-design / virtual design service (flat-fee, remote); one-off colour consultation; hourly design advice. Process breakdown (step 1-5) reduces &ldquo;I didn&rsquo;t know what I was getting into&rdquo; cancellations. From $200.",
			"&bull; <strong>E-design / virtual service</strong>: flat-fee remote service (popular post-pandemic); questionnaire + mood board + supplier list + room plan; can serve national market not just local; from $150.",
			"&bull; <strong>Investment / pricing page</strong>: publishing even a price range (e.g. &ldquo;projects typically start at \$5,000&rdquo;) filters out low-budget enquiries and improves lead quality dramatically. Second-most-visited page; from $200.",
			"&bull; <strong>Supplier and trade partnerships</strong>: &ldquo;Access to trade-only pricing from [brand names]&rdquo; is a major differentiator; BIID / NEC3 contract template mention reassures commercial clients; from $150.",
			"&bull; <strong>Press and features</strong>: if the designer has been featured in House Beautiful, Homes &amp; Gardens, AD, or similar, a press page dramatically increases perceived credibility. From $150.",
			"&bull; <strong>Interior design blog</strong>: &ldquo;How much does interior design cost?&rdquo;, &ldquo;The difference between an interior designer and decorator&rdquo;, &ldquo;2025 colour trends&rdquo; &mdash; high-traffic informational content that funnels to enquiries. From $150/post.",
			'',
			"From \$550 for a portfolio-led site &mdash; \$1,100+ with e-design service, style quiz, investment page, and blog.",
			'',
			"Residential, commercial, or both? Do you offer virtual design, full service, or both?",
		] );
	}

	// 0d-pre48-c) Architect / architectural practice / planning consultant website.
	if ( $has( [ 'architect website', 'architectural practice website', 'architecture firm website', 'architectural design website', 'planning consultant website', 'planning architect website', 'residential architect website', 'commercial architect website', 'listed building architect website', 'conservation architect website', 'extension architect website', 'loft conversion architect website', 'new build architect website', 'landscape architect website', 'architectural drawings website', 'aps architect website' ] ) ) {
		return $nl( [
			"Architect websites convert on portfolio credibility and clearly articulated process &mdash; clients are committing to a long-term professional relationship, so trust signals and expertise proof are everything:",
			'',
			"&bull; <strong>Project portfolio</strong>: one page per project with drone/professional photography; project type + location + scope; planning reference number (signals planning success rate); RIBA stage completed. Filter by type (residential extension / loft / new build / commercial / heritage / landscape). From $400.",
			"&bull; <strong>Services and RIBA stages</strong>: explain what you actually do at each RIBA Stage (0-7): feasibility, concept design, planning, technical design, construction, handover. Most clients don&rsquo;t understand what an architect does beyond &ldquo;draws plans&rdquo; &mdash; explaining the full service justifies the fee. From $250.",
			"&bull; <strong>ARB / RIBA registration</strong>: ARB (Architects Registration Board) registration is a legal requirement to use the title &lsquo;architect&rsquo; in the UK; RIBA chartered member logo; both should be displayed prominently with verify links. From $100.",
			"&bull; <strong>Planning application success rate</strong>: &ldquo;98% planning approval rate&rdquo; or &ldquo;[N] successful planning applications&rdquo; is a powerful conversion signal on the homepage or services page. From $150.",
			"&bull; <strong>Free initial consultation CTA</strong>: most architects offer a free 30-minute feasibility call; this is the primary lead-gen action; Calendly or MS Bookings embed; project type + address + brief. From $200.",
			"&bull; <strong>Planning and permitted development guide</strong>: &ldquo;Do I need planning permission for my extension?&rdquo; is the single most searched question by homeowners considering works; a comprehensive guide page ranks well and positions the practice as expert advisers. From $200.",
			"&bull; <strong>Heritage and conservation page</strong>: listed buildings (Grade I / II* / II) require different consents (LBC); conservation area constraints; IHBC/SPAB membership; niche but high-fee and rarely well-served online. From $200.",
			"&bull; <strong>Testimonials and planning references</strong>: planning reference numbers link directly to the local authority&rsquo;s public planning portal &mdash; clients can verify approval themselves. Unique credibility signal no other profession can replicate. From $150.",
			"&bull; <strong>Fees and process page</strong>: percentage-of-construction-cost model (typically 8&ndash;15%) or fixed-stage fees; publishing even a guide removes the &ldquo;how much does an architect cost&rdquo; Google search and keeps traffic on your site. From $150.",
			'',
			"From \$600 for a portfolio-led practice site &mdash; \$1,400+ with RIBA stage explainer, planning guide, heritage page, and Calendly booking.",
			'',
			"Primarily residential or commercial? Any heritage / conservation specialisation? UK or international practice?",
		] );
	}

	// 0d-pre47-a) Dog groomer / pet groomer / mobile dog groomer / dog salon website.
	if ( $has( [ 'dog groomer website', 'dog grooming website', 'pet groomer website', 'pet grooming website', 'mobile dog groomer website', 'mobile grooming website', 'dog salon website', 'dog parlour website', 'pet salon website', 'puppy groomer website', 'cat groomer website', 'dog wash website', 'hand strip groomer website', 'show dog groomer website', 'dog grooming van website', 'dog spa website' ] ) ) {
		return $nl( [
			"Dog grooming websites convert on trust signals and zero-friction booking &mdash; pet owners hand over something precious, so every page needs to build confidence:",
			'',
			"&bull; <strong>Meet the groomer page</strong>: qualifications (City &amp; Guilds Level 3 / iPET Network / BII); years of experience; breeds specialised in; City &amp; Guilds pet first-aid certification; face photo with a dog &mdash; the human connection closes bookings. From $200.",
			"&bull; <strong>Online booking</strong>: breed + size + coat type + services required + date; deposit to secure slot; Shortcuts, Petlinx, or Calendly. Reduces phone tag entirely. From $300.",
			"&bull; <strong>Services and pricing</strong>: full groom, bath &amp; dry, hand strip, puppy introduction, nail clip, teeth brushing, de-shedding treatment; price banded by size (small/medium/large/giant); coat surcharges for matted coats. From $250.",
			"&bull; <strong>Before/after gallery</strong>: breed-specific transformations; healed coat photos; breed-standard clips (Bichon, Poodle, Schnauzer, Cocker, Westie) &mdash; this is the primary conversion asset for new clients deciding on style. From $200.",
			"&bull; <strong>FAQ / first visit page</strong>: &ldquo;What if my dog is anxious?&rdquo; &ldquo;Do you do puppies under 6 months?&rdquo; &ldquo;What if my dog is matted?&rdquo; &ldquo;Are you insured?&rdquo; &mdash; reduces pre-appointment anxiety calls and positions you as an expert. From $150.",
			"&bull; <strong>Breed-specific pages</strong>: &ldquo;Cockapoo grooming&rdquo;, &ldquo;Golden Retriever grooming&rdquo;, &ldquo;Doodle grooming&rdquo; &mdash; each ranks for breed-specific searches which have high buyer intent. From $100/page.",
			"&bull; <strong>Mobile grooming map</strong>: service area postcode list; &ldquo;no trailing leads or waiting rooms&rdquo; USP (stress-reduction for anxious dogs); Google Maps embed. From $150.",
			"&bull; <strong>Insurance and licensing</strong>: public liability (PetPlan Sanctuary / Cliverton); local authority registration (required in many LAs since Animal Welfare (Licensing) Act 2018). From $100.",
			'',
			"From \$400 for a clean booking-focused groomer site &mdash; \$800+ with breed gallery, breed pages, and mobile service map.",
			'',
			"Any niche to call out &mdash; hand stripping, show preparation, anxious or elderly dogs?",
		] );
	}

	// 0d-pre47-b) Spa / wellness centre / massage therapist / holistic therapist / retreat website.
	if ( $has( [ 'spa website', 'day spa website', 'wellness centre website', 'wellness center website', 'massage therapist website', 'massage therapy website', 'holistic therapist website', 'holistic therapy website', 'beauty spa website', 'med spa website', 'medical spa website', 'skin clinic website', 'facialist website', 'reflexology website', 'aromatherapy website', 'reiki website', 'sound healing website', 'retreat website', 'wellbeing website', 'relaxation centre website', 'spa salon website', 'wellness studio website' ] ) ) {
		return $nl( [
			"Spa and wellness websites convert on sensory atmosphere and frictionless booking &mdash; the digital experience must match the in-person promise:",
			'',
			"&bull; <strong>Online booking</strong>: therapist + treatment + duration + date; Fresha (zero commission), Phorest, Mindbody, or Zenoti; deposit option. The booking button must be visible above the fold on mobile. From $350.",
			"&bull; <strong>Treatments menu</strong>: grouped by category (massage / facials / body / holistic / medical aesthetic); duration + price; contraindications where relevant; photo per treatment. From $250.",
			"&bull; <strong>Gift vouchers</strong>: WooCommerce or Treatwell vouchers; custom amounts or fixed packages; &ldquo;gift an experience&rdquo; copywriting; seasonal (Christmas, Mother&rsquo;s Day, Valentine&rsquo;s, birthdays); single highest-ROI page for spa websites. From $200.",
			"&bull; <strong>Meet the therapists</strong>: ITEC / VTCT / CIBTAC / CIDESCO qualifications; specialist expertise per therapist; face photos in uniform &mdash; reduces first-visit anxiety for new clients. From $150/profile.",
			"&bull; <strong>Memberships / packages</strong>: monthly subscription (Stripe recurring billing); pre-paid treatment bundles (e.g. 6 massages for the price of 5); improves retention and forward revenue visibility. From $250.",
			"&bull; <strong>Medical aesthetics page</strong> (if applicable): CQC registration required for injectables (Botox, fillers); prescriber details; GPhC-regulated pharmacy; consultation-first policy &mdash; must be stated clearly. From $200.",
			"&bull; <strong>Wellbeing blog / resources</strong>: &ldquo;5 signs you need a deep tissue massage&rdquo;; &ldquo;difference between Swedish and hot stone&rdquo; &mdash; ranks for informational searches that funnel to bookings. From $150/post.",
			"&bull; <strong>GDPR consent &amp; health forms</strong>: consultation forms (medical history, contraindications); digital e-signature via Jotform or Typeform; complies with ICO requirements and CNHC codes of practice. From $150.",
			'',
			"From \$550 for a focused booking-led wellness site &mdash; \$1,100+ with gift vouchers, memberships, and medical aesthetics compliance page.",
			'',
			"Is this a multi-therapist centre or solo practitioner? Any CQC-regulated treatments (injectables, laser)?",
		] );
	}

	// 0d-pre47-c) Event venue / wedding venue / conference venue / function room website.
	if ( $has( [ 'event venue website', 'wedding venue website', 'conference venue website', 'function room website', 'party venue website', 'event space website', 'banqueting suite website', 'corporate event venue website', 'private hire venue website', 'barn wedding venue website', 'manor house wedding website', 'hotel wedding venue website', 'micro wedding venue website', 'wedding barn website', 'meeting room website', 'events hall website', 'reception venue website', 'event hall website' ] ) ) {
		return $nl( [
			"Event venue websites convert on emotional imagery, capacity/layout trust signals, and a smooth enquiry-to-quote flow:",
			'',
			"&bull; <strong>Virtual tour / gallery</strong>: professional photography is the #1 investment for a venue site; 360&deg; tour (Matterport from ~&pound;300/session); ceremony + reception + outdoor + catering kitchen &mdash; buyers make shortlist decisions on imagery alone. From $300.",
			"&bull; <strong>Capacity and layout page</strong>: theatre / cabaret / boardroom / banquet / ceremony configurations; capacity per layout; floor plan PDF download; outdoor capacity; parking spaces. From $200.",
			"&bull; <strong>Packages and pricing</strong>: wedding packages (ceremony + reception + catering); corporate day delegate rates (DDR); evening-only hire; minimum spend; exclusivity policy. Venues that hide pricing generate fewer qualified enquiries. From $250.",
			"&bull; <strong>Enquiry &amp; availability form</strong>: event type + date + estimated guest count + catering requirements + budget range + hear-about; CRM integration (HubSpot, Zoho, or Salesforce); auto-reply with PDF brochure attach. From $300.",
			"&bull; <strong>Preferred suppliers page</strong>: caterers, florists, photographers, bands, AV companies, accommodation nearby &mdash; generates referral traffic and positions venue as full-service. From $150.",
			"&bull; <strong>Wedding gallery / real weddings blog</strong>: one post per real wedding (with couple consent); each post ranks for &ldquo;[venue name] wedding&rdquo;; long-tail SEO goldmine. From $100/post.",
			"&bull; <strong>Corporate / private hire page</strong>: HDMI / AV / PA / stage / breakout rooms / catering / Wi-Fi speeds; corporate invoice process; day hire vs half-day. From $200.",
			"&bull; <strong>Accessibility statement</strong>: step-free access; hearing loop; accessible parking; baby-changing; dietary options &mdash; legally required under Equality Act 2010 and increasingly a buyer filter. From $100.",
			"&bull; <strong>Local authority licences</strong>: premises licence for civil ceremonies (must be registered with local registrar); Temporary Events Notice limit (499 people without full premises licence); alcohol licence displayed if applicable. From $100.",
			'',
			"From \$700 for a gallery-led venue site &mdash; \$1,400+ with virtual tour embed, real-weddings blog, enquiry CRM integration, and preferred supplier directory.",
			'',
			"Is this primarily weddings, corporate, or mixed? Licensed for civil ceremonies? Do you have in-house catering?",
		] );
	}

	// 0d-pre46-a) Hairdresser / barber / hair salon / barbershop / colourist website.
	if ( $has( [ 'hairdresser website', 'hair salon website', 'barber website', 'barbershop website', 'hair stylist website', 'hair colourist website', 'hair colourist website', 'hair extensions website', 'blow dry bar website', 'keratin treatment website', 'afro hair salon website', 'men\'s hair salon website', 'ladies hairdresser website', 'mobile hairdresser website', 'wedding hair website', 'bridal hair website' ] ) ) {
		return $nl( [
			"Hair salon and barbershop websites convert on style portfolio and frictionless booking &mdash; the chair-fill rate is the core metric, so every page should direct to booking:",
			'',
			"&bull; <strong>Team and style portfolio</strong> &mdash; one page per stylist; high-quality before-and-after photos of haircuts, colour, and styling; filter by service type (balayage / highlights / men&rsquo;s cut / bridal / afro); from <strong>$250</strong>",
			"&bull; <strong>Online booking</strong> &mdash; Treatwell, Fresha (free), or Shortcuts/Phorest integrated with your rota; service + stylist selector; deposit option (reduces no-shows); from <strong>$300</strong>",
			"&bull; <strong>Services and pricing page</strong> &mdash; list every service with price range and duration; hidden prices lose enquiries; short hair / long hair pricing distinction; from <strong>$200</strong>",
			"&bull; <strong>Colour services page</strong> &mdash; balayage / highlights / ombre / toner / colour correction / keratin; consultation required note for colour correction; from <strong>$150</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce; Christmas / Mother&rsquo;s Day / birthday; &ldquo;gift a blow-dry&rdquo; or full package; from <strong>$150</strong>",
			"&bull; <strong>Bridal/special occasion page</strong> &mdash; trial session + day-of service; bridal party packages; from <strong>$150</strong>",
			"&bull; <strong>Instagram feed embed</strong> &mdash; real-time style portfolio; most hair clients discover via Instagram so pulling the feed into the website closes the discovery-to-booking gap; from <strong>$150</strong>",
			"&bull; <strong>From $550</strong> team portfolio + booking + pricing + Instagram; <strong>$1,000+</strong> with colour page + gift vouchers + bridal page",
			'',
			"Hair salon or barbershop? Mixed clientele or men&rsquo;s/women&rsquo;s only? Any specialist services (afro hair, extensions, keratin, colour correction)?",
		] );
	}

	// 0d-pre46-b) Plumber / heating engineer / boiler installation / gas engineer website.
	if ( $has( [ 'plumber website', 'plumbing website', 'heating engineer website', 'boiler installation website', 'gas engineer website', 'gas safe engineer website', 'boiler repair website', 'central heating website', 'underfloor heating website', 'bathroom installation website', 'kitchen plumbing website', 'emergency plumber website', 'drain unblocking website', 'bathroom fitter website', 'wet room website', 'plumbing company website' ] ) ) {
		return $nl( [
			"Plumber and heating engineer websites convert on emergency availability, Gas Safe credentials, and fixed-price transparency &mdash; the no-show-fee culture of some plumbers makes trust the primary conversion barrier:",
			'',
			"&bull; <strong>Emergency call-out page</strong> &mdash; 24/7 availability; response time; fixed call-out charge stated upfront; burst pipe / no hot water / boiler breakdown / blocked drain triage; most-visited page; from <strong>$200</strong>",
			"&bull; <strong>Services pages</strong> &mdash; boiler installation / boiler service / boiler repair / central heating / underfloor heating / bathroom installation / wet room / kitchen plumbing / drain unblocking / leak detection; from <strong>$150</strong> per page",
			"&bull; <strong>Gas Safe registration</strong> &mdash; Gas Safe Register number displayed prominently (legally required for all gas work in the UK); verify-online link to Gas Safe register; Boiler Plus compliance note (2018 regulations); from <strong>$100</strong>",
			"&bull; <strong>Boiler brands and certifications</strong> &mdash; Worcester Bosch Accredited Installer / Vaillant Advanced Installer / Ideal Installer; manufacturer extended warranty only available through accredited installers (up to 12 years); critical upsell; from <strong>$150</strong>",
			"&bull; <strong>Finance options page</strong> &mdash; 0% purchase plan or low-deposit monthly (Barclays Partner Finance / Novuna); most boiler replacements (&pound;2,500&ndash;&pound;4,000) are big-ticket; finance integration doubles conversion on quote pages; from <strong>$200</strong>",
			"&bull; <strong>Free boiler quote form</strong> &mdash; property type + boiler make + age + fault description + postcode; acknowledge 24h response; from <strong>$200</strong>",
			"&bull; <strong>Area pages</strong> &mdash; &ldquo;[area] plumber&rdquo; / &ldquo;[area] boiler installation&rdquo;; local reviews; from <strong>$100</strong> per page",
			"&bull; <strong>From $600</strong> emergency page + services + Gas Safe badge + quote form; <strong>$1,200+</strong> with finance page + area pages + boiler brand certs",
			'',
			"Gas Safe registered? What brands do you install (Worcester/Vaillant/Ideal/Baxi)? Emergency callouts only, or planned installs and bathrooms too?",
		] );
	}

	// 0d-pre46-c) Electrician / electrical contractor / NICEIC registered / EV charger installer website.
	if ( $has( [ 'electrician website', 'electrical contractor website', 'electrical company website', 'niceic electrician website', 'napit electrician website', 'domestic electrician website', 'commercial electrician website', 'industrial electrician website', 'ev charger installation website', 'solar panel installation website', 'fuse box replacement website', 'electrical installation website', 'part p electrician website', 'rewiring website', 'smart home electrician website', 'emergency electrician website' ] ) ) {
		return $nl( [
			"Electrician websites convert on competency scheme registration, fixed quote transparency, and emergency availability &mdash; homeowners are risk-averse with electrical work, so credentials lead the page:",
			'',
			"&bull; <strong>NICEIC / NAPIT registration badge</strong> &mdash; displayed on homepage and every service page; click-through to verify on the NICEIC or NAPIT register; Part P compliance self-certification (Building Regulations) explained; mandatory trust signal; from <strong>$100</strong>",
			"&bull; <strong>Services pages</strong> &mdash; fuse box / consumer unit replacement; full rewires; EV charger installation (OZEV-approved required for OLEV grant); solar PV and battery storage; smart home / Hive / Nest; outdoor lighting; commercial periodic inspection; from <strong>$150</strong> per page",
			"&bull; <strong>EV charger installation page</strong> &mdash; OZEV-approved installer (required for any government grant claims); OLEV grant amounts (currently &pound;350 for homeowners); brands (Hypervolt / Ohme / Myenergi Zappi / Andersen A2); fastest-growing residential electrical service; from <strong>$200</strong>",
			"&bull; <strong>Electrical certificate page</strong> &mdash; EICR (Electrical Installation Condition Report) for landlords (mandatory 5-year interval since July 2020); fixed fee quoted upfront; speeds up landlord decision; from <strong>$150</strong>",
			"&bull; <strong>Free quote form</strong> &mdash; job type + number of circuits + property type + postcode + urgency; 24h response; from <strong>$200</strong>",
			"&bull; <strong>Emergency electrician page</strong> &mdash; 24/7; trips / power outage / sparking sockets / burning smell; fixed call-out rate; most urgent + most valuable traffic; from <strong>$200</strong>",
			"&bull; <strong>Area pages</strong> &mdash; &ldquo;[area] electrician&rdquo; / &ldquo;[area] EV charger installation&rdquo;; local reviews embed; from <strong>$100</strong> per page",
			"&bull; <strong>From $600</strong> NICEIC badge + services + quote form + emergency page; <strong>$1,200+</strong> with EV page + EICR page + area pages",
			'',
			"NICEIC or NAPIT registered? Domestic, commercial, or both? Do you install EV chargers or solar? OZEV-approved?",
		] );
	}

	// 0d-pre45-a) Estate agent / letting agent / property management company website.
	if ( $has( [ 'estate agent website', 'estate agency website', 'letting agent website', 'letting agency website', 'property management website', 'property agent website', 'property finder website', 'residential estate agent website', 'commercial estate agent website', 'new homes estate agent website', 'independent estate agent website', 'high street estate agent website', 'online estate agent website', 'property developer website', 'buy to let website', 'property investment website', 'landlord letting website' ] ) ) {
		return $nl( [
			"Estate agent and letting agent websites convert on local market credibility, instant valuation, and a frictionless applicant journey &mdash; Rightmove/Zoopla integration is mandatory but your own site must do more than duplicate their listings:",
			'',
			"&bull; <strong>Property search and listings</strong> &mdash; IDX/property feed from Rightmove or Zoopla syndicated back to your own site; filters (bedrooms / price / type / area); featured properties rotator; from <strong>$400</strong>",
			"&bull; <strong>Instant valuation tool</strong> &mdash; ValPal / GetAgent / Hometrack embed; captures name + email + phone + address; highest-converting vendor lead magnet; from <strong>$250</strong>",
			"&bull; <strong>Book a valuation CTA</strong> &mdash; Calendly or custom form; in-person or video; dedicated landing page with sold price evidence in the area; from <strong>$200</strong>",
			"&bull; <strong>Area guides</strong> &mdash; one page per area covered; schools / transport / restaurants / parks / property price trend; ranks for &ldquo;[area] estate agents&rdquo; and positions you as the local expert; from <strong>$150</strong> per guide",
			"&bull; <strong>Landlord services page</strong> &mdash; full vs let-only management; fees disclosed (Consumer Rights Act 2015 requires fee transparency for letting agents); EPC / gas safety / EICR compliance note; from <strong>$200</strong>",
			"&bull; <strong>Sold and let results</strong> &mdash; Land Registry sold prices widget (public data); Google Reviews embed; average days to sell / average % of asking price; from <strong>$200</strong>",
			"&bull; <strong>Regulatory compliance</strong> &mdash; TPO (The Property Ombudsman) or PRS membership (legally required for letting agents since Oct 2014); NAEA Propertymark logo; client money protection scheme name (required since Apr 2019); ICO registration for GDPR; from <strong>$150</strong>",
			"&bull; <strong>From $700</strong> listings + valuation tool + book valuation + compliance; <strong>$1,400+</strong> with area guides + landlord page + results",
			'',
			"Sales, lettings, or both? Residential or commercial? What areas do you cover? Do you use Rightmove and/or Zoopla?",
		] );
	}

	// 0d-pre45-b) Wedding photographer / portrait photographer / event photographer / videographer website.
	if ( $has( [ 'wedding photographer website', 'wedding photography website', 'portrait photographer website', 'event photographer website', 'commercial photographer website', 'fashion photographer website', 'videographer website', 'wedding videographer website', 'newborn photographer website', 'family photographer website', 'product photographer website', 'headshot photographer website', 'boudoir photographer website', 'documentary photographer website', 'lifestyle photographer website', 'photographer website' ] ) ) {
		return $nl( [
			"Photography websites convert on emotional impact first, practical details second &mdash; a prospective couple decides in 8 seconds whether your style matches their vision:",
			'',
			"&bull; <strong>Portfolio galleries</strong> &mdash; curated not comprehensive (30 best images convert better than 300 average ones); category tabs (wedding / portrait / commercial / events); lazy-loaded; from <strong>$300</strong>",
			"&bull; <strong>Full wedding gallery blog posts</strong> &mdash; one full-shoot post per client (with consent); ranks for &ldquo;[venue name] wedding photographer&rdquo; and &ldquo;[area] wedding photography&rdquo;; each post is a landing page for a venue; from <strong>$100</strong> per post",
			"&bull; <strong>Investment / pricing page</strong> &mdash; the second-most-visited page on photographer sites; hiding prices loses enquiries; package tiers with inclusions listed; from <strong>$250</strong>",
			"&bull; <strong>About page</strong> &mdash; story of why you shoot; style philosophy; your face (couples hire a person, not a lens); what to expect on the day; from <strong>$200</strong>",
			"&bull; <strong>Enquiry and booking form</strong> &mdash; date + venue + event type + budget + how they found you; HoneyBook / Pixieset / Studio Ninja CRM for contract and invoice; from <strong>$250</strong>",
			"&bull; <strong>Venue-specific SEO pages</strong> &mdash; &ldquo;Photography at [venue name]&rdquo;; 10&ndash;30 venues in your area; long-tail search with high buyer intent; from <strong>$100</strong> per page",
			"&bull; <strong>Instagram feed embed</strong> &mdash; real-time gallery of recent work; social proof; from <strong>$150</strong>",
			"&bull; <strong>Client gallery portal</strong> &mdash; Pixieset or Pic-Time private delivery; linked from confirmation email; from <strong>$200</strong>",
			"&bull; <strong>From $600</strong> portfolio + enquiry + pricing; <strong>$1,200+</strong> with venue pages + full-shoot blog + client portal",
			'',
			"What type of photography? What areas and venues? Do you shoot video too? Destination weddings?",
		] );
	}

	// 0d-pre45-c) Music teacher / piano teacher / music school / music tutor / guitar teacher website.
	if ( $has( [ 'music teacher website', 'piano teacher website', 'guitar teacher website', 'violin teacher website', 'singing teacher website', 'music school website', 'music tutor website', 'music lessons website', 'drum teacher website', 'cello teacher website', 'trumpet teacher website', 'saxophone teacher website', 'music academy website', 'music studio website', 'online music teacher website', 'music tutoring website' ] ) ) {
		return $nl( [
			"Music teacher and music school websites convert on tutor credibility, timetable clarity, and a low-friction trial lesson booking &mdash; parents of young pupils are the primary decision-maker:",
			'',
			"&bull; <strong>Tutor profile pages</strong> &mdash; qualifications (ABRSM / Trinity College London / conservatoire training); Grade 8 performance or above; DBS checked; instruments and genres; experience range (beginner to diploma); from <strong>$150</strong> per profile",
			"&bull; <strong>Lesson types and pricing</strong> &mdash; 30 / 45 / 60-minute options; in-person vs online; group vs 1:1; block booking discount; trial lesson offer; from <strong>$200</strong>",
			"&bull; <strong>Book a trial lesson</strong> &mdash; Calendly or Acuity; instrument selector + pupil age + experience level; from <strong>$200</strong>",
			"&bull; <strong>Exam preparation page</strong> &mdash; ABRSM / Trinity College London / Rock School grades 1&ndash;8 + diplomas; syllabi years; what to expect on exam day; from <strong>$200</strong>",
			"&bull; <strong>Pupil achievements</strong> &mdash; grade pass rates; merit and distinction pupils named with consent; inspires new enquiries; from <strong>$150</strong>",
			"&bull; <strong>Video performance samples</strong> &mdash; short YouTube clips of pupil performances (parental consent required for under-18s); the most persuasive content on a music teacher site; from <strong>$150</strong>",
			"&bull; <strong>Safeguarding statement</strong> &mdash; DBS certificate date; safeguarding policy link; parental consent for photos/videos; essential for teaching under-18s; from <strong>$100</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce; Christmas / birthday; &ldquo;gift a trial lesson&rdquo;; from <strong>$150</strong>",
			"&bull; <strong>From $550</strong> tutor profile + pricing + booking + safeguarding; <strong>$1,100+</strong> with exam prep + achievements + gift vouchers",
			'',
			"Solo teacher or a school with multiple tutors? Which instruments? In-person, online, or both? Primarily children or adults?",
		] );
	}

	// 0d-pre44-a) Tattoo studio / tattoo artist / piercing studio website.
	if ( $has( [ 'tattoo studio website', 'tattoo artist website', 'tattoo parlour website', 'tattoo shop website', 'piercing studio website', 'body piercing website', 'tattoo removal website', 'laser tattoo removal website', 'traditional tattoo website', 'japanese tattoo website', 'watercolour tattoo website', 'blackwork tattoo website', 'realism tattoo website', 'semi-permanent makeup tattoo website', 'microblading tattoo website', 'fine line tattoo website' ] ) ) {
		return $nl( [
			"Tattoo studio websites convert on portfolio quality and consultation bookings &mdash; the artist&rsquo;s style is the product, not the studio name:",
			'',
			"&bull; <strong>Artist portfolio pages</strong> &mdash; one page per artist; high-resolution healed tattoo photos (NOT fresh tattoos &mdash; fresh tattoos look more dramatic but mislead clients on the healed result); style categories (traditional / Japanese / blackwork / fine line / watercolour / realism / neo-traditional / lettering / geometric); from <strong>$250</strong> per artist",
			"&bull; <strong>Consultation booking</strong> &mdash; online consultation form capturing reference images, placement, size, budget, skin tone, and timeline; Calendly or custom form; deposit at consultation stage (&pound;50&ndash;&pound;100 credited against the tattoo); from <strong>$300</strong>",
			"&bull; <strong>Style guide pages</strong> &mdash; &ldquo;What is blackwork?&rdquo; / &ldquo;How long does a sleeve take?&rdquo; / &ldquo;What to expect at your appointment&rdquo;; educates first-time clients; reduces admin emails; from <strong>$150</strong>",
			"&bull; <strong>Aftercare page</strong> &mdash; written and video aftercare instructions; reduces infections and callbacks; &ldquo;is my tattoo healing correctly?&rdquo; FAQ; from <strong>$150</strong>",
			"&bull; <strong>Compliance</strong> &mdash; UK regulation: tattooing is regulated under the Skin Piercing and Tattooing Act; local authority licence number for each tattooist; single-use needle and sterile equipment statement; age restriction statement (18+ required by law); from <strong>$150</strong>",
			"&bull; <strong>Flash sale / available designs</strong> &mdash; artist flash sheets available to book immediately (no custom design wait); lower price point; converts walk-in traffic; WooCommerce or simple form; from <strong>$200</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce; birthday and Christmas gifting; &ldquo;gift a consultation&rdquo; option; from <strong>$150</strong>",
			"&bull; <strong>From $550</strong> portfolio + consultation booking + compliance; <strong>$1,100+</strong> with style guides + aftercare + flash sale page",
			'',
			"Solo artist or a studio with multiple artists? Walk-in flash, appointment-only, or both? Do you offer removal or just application?",
		] );
	}

	// 0d-pre44-b) Childminder / nursery / preschool / day nursery / after-school club website.
	if ( $has( [ 'childminder website', 'nursery website', 'day nursery website', 'preschool website', 'pre-school website', 'after school club website', 'breakfast club website', 'holiday club website', 'childcare website', 'nanny agency website', 'au pair agency website', 'creche website', 'early years website', 'toddler group website', 'mother and toddler website', 'out of school care website', 'wrap around care website' ] ) ) {
		return $nl( [
			"Childcare and nursery websites convert on safety trust and immediate availability &mdash; Ofsted rating and photo proof of the environment are the two conversion anchors:",
			'',
			"&bull; <strong>About the setting page</strong> &mdash; Ofsted registration number and most recent inspection outcome (must display Outstanding / Good / Requires Improvement / Inadequate prominently); photos of the setting (rooms, outdoor space, meals, activities); opening hours; age range accepted; from <strong>$250</strong>",
			"&bull; <strong>Online enquiry and waiting list</strong> &mdash; child name + date of birth + start date + session type + funding eligibility; WooCommerce / Gravity Forms; deposit to secure a place; from <strong>$300</strong>",
			"&bull; <strong>Government-funded places page</strong> &mdash; 15-hour universal entitlement (all 3&ndash;4-year-olds); 30-hour extended entitlement (working parents); 15 hours from 9 months (April 2024 expansion); Tax-Free Childcare scheme; most-asked parent question and it must be answered clearly; from <strong>$200</strong>",
			"&bull; <strong>Team profiles</strong> &mdash; key person system explanation; staff headshots + Level 3 qualification + DBS-checked statement; paediatric first-aid certification date; builds parental trust; from <strong>$150</strong> per profile",
			"&bull; <strong>Curriculum and activities page</strong> &mdash; EYFS (Early Years Foundation Stage) framework; phonics / maths / creative play / outdoor learning; forest school where applicable; from <strong>$200</strong>",
			"&bull; <strong>Parent testimonials</strong> &mdash; Google Reviews widget; specific quotes about settling-in and communication; most persuasive content for anxious parents of young children",
			"&bull; <strong>Safeguarding and policies page</strong> &mdash; safeguarding policy link; SENCO contact; complaints procedure; GDPR notice for child photos; from <strong>$150</strong>",
			"&bull; <strong>From $600</strong> setting page + enquiry form + funding page + Ofsted badge; <strong>$1,200+</strong> with team profiles + curriculum + testimonials",
			'',
			"Childminder, nursery, pre-school, or after-school? Are you Ofsted registered? Which age ranges do you take? Do you accept funded hours?",
		] );
	}

	// 0d-pre44-c) Building contractor / builder / construction company / property developer website.
	if ( $has( [ 'builder website', 'building contractor website', 'construction company website', 'general contractor website', 'property developer website', 'house builder website', 'extension builder website', 'loft conversion website', 'renovation contractor website', 'refurbishment contractor website', 'groundworks contractor website', 'commercial contractor website', 'fit-out contractor website', 'main contractor website', 'building company website', 'construction firm website' ] ) ) {
		return $nl( [
			"Building contractor websites convert on project proof and accreditation trust &mdash; a polished before-and-after gallery with credentials visible on the homepage moves the needle most:",
			'',
			"&bull; <strong>Project portfolio</strong> &mdash; before and after photos; project type (extension / loft / new build / refurb / commercial fit-out); location and approximate value; client quote where consented; from <strong>$300</strong>",
			"&bull; <strong>Services pages</strong> &mdash; one per service type (extensions / loft conversions / new builds / renovations / groundworks / commercial / fit-out / structural repairs); scope description + planning note + typical timeline + indicative price range; from <strong>$150</strong> per page",
			"&bull; <strong>Planning and process page</strong> &mdash; how you handle planning permission / permitted development / building control sign-off; project stages (feasibility &rarr; design &rarr; contract &rarr; build &rarr; snagging &rarr; sign-off); sets expectations and reduces pre-sales phone calls; from <strong>$200</strong>",
			"&bull; <strong>Accreditation badges</strong> &mdash; FMB (Federation of Master Builders) membership; NHBC / Premier Guarantee / Build Zone warranty provider (for new builds); CHAS or Constructionline (for commercial tenders); Gas Safe / NICEIC subcontractor list; from <strong>$150</strong>",
			"&bull; <strong>Free quote form</strong> &mdash; project type + location + size (m&sup2;) + planning status + budget range + timeline; acknowledge 48-hour response; from <strong>$200</strong>",
			"&bull; <strong>Testimonials and case studies</strong> &mdash; one case study per project type; homeowner photo with project photo; Google Reviews widget; builds confidence for high-value projects where trust is the main barrier; from <strong>$200</strong> per case study",
			"&bull; <strong>Compliance</strong> &mdash; VAT registration number (for B2B clients); public liability (&pound;5m minimum for most commercial); employers&rsquo; liability (legally required for employed labour); waste carrier licence from <strong>$150</strong>",
			"&bull; <strong>From $600</strong> portfolio + services + quote form + accreditation; <strong>$1,200+</strong> with case studies + planning page + compliance",
			'',
			"Residential or commercial? New build, extensions, or general refurb? Main contractor or specialist sub-contractor? Are you FMB or NHBC registered?",
		] );
	}

	// 0d-pre43-a) Vet clinic / veterinary practice / animal hospital / pet care website.
	if ( $has( [ 'vet website', 'vet clinic website', 'veterinary practice website', 'veterinary clinic website', 'animal hospital website', 'pet care website', 'veterinary surgery website', 'small animal vet website', 'exotic vet website', 'emergency vet website', 'referral vet website', 'equine vet website', 'farm vet website', 'cat clinic website', 'dog clinic website', 'rabbit vet website' ] ) ) {
		return $nl( [
			"Veterinary practice websites convert on clinical trust and after-hours availability &mdash; RCVS registration and emergency cover are the two conversion anchors:",
			'',
			"&bull; <strong>Services pages</strong> &mdash; one per service (vaccinations / neutering / dental / surgery / diagnostics / physiotherapy / hydrotherapy / acupuncture / oncology / ophthalmology); species tabs where multi-species; what to expect description; from <strong>$200</strong> per page",
			"&bull; <strong>Online booking</strong> &mdash; VetDesk / RxWorks / Provet Cloud appointment module; routine vs urgent vs emergency triage; species selector; from <strong>$300</strong>",
			"&bull; <strong>RCVS compliance</strong> &mdash; RCVS-accredited practice logo (Practice Standards Scheme); named vet with RCVS registration number; out-of-hours cover statement (RCVS Code requires 24/7 emergency access &mdash; must state who provides OOH cover); from <strong>$150</strong>",
			"&bull; <strong>Pet health library</strong> &mdash; condition guides (diabetes / arthritis / dental disease / obesity / flea/tick/worm prevention); seasonal reminders; builds organic search and client trust between appointments; from <strong>$250</strong>",
			"&bull; <strong>Pet health plans / preventive care club</strong> &mdash; monthly direct debit covering vaccinations / flea/worm treatment / annual check-up; Vetsure / VetEnvoy / PetsApp; highest-retention product in veterinary; from <strong>$300</strong>",
			"&bull; <strong>Emergency page</strong> &mdash; OOH number prominent; triage guide (&ldquo;is this an emergency?&rdquo;); nearest emergency hospital if not 24/7; pet first-aid guide (choking / bleeding / poisoning / RTA); from <strong>$150</strong>",
			"&bull; <strong>Team profiles</strong> &mdash; vet and nurse headshots + RCVS number + specialism + favourite species; builds rapport before the appointment; from <strong>$150</strong> per profile",
			"&bull; <strong>From $600</strong> services + booking + RCVS compliance; <strong>$1,200+</strong> with health library + health plan + emergency page",
			'',
			"Small animal only, or also exotics / equine / farm? Do you offer 24/7 on-site, or use an OOH provider? RCVS-accredited?",
		] );
	}

	// 0d-pre43-b) Restaurant / café / pub / bar / hospitality website.
	if ( $has( [ 'restaurant website', 'cafe website', 'café website', 'pub website', 'bar website', 'bistro website', 'brasserie website', 'fine dining website', 'pizza restaurant website', 'indian restaurant website', 'chinese restaurant website', 'italian restaurant website', 'takeaway website', 'food truck website', 'coffee shop website', 'tea room website', 'catering company website', 'wedding caterer website', 'hospitality website' ] ) ) {
		return $nl( [
			"Restaurant and hospitality websites convert on food photography and friction-free table booking &mdash; these two things move the needle more than anything else:",
			'',
			"&bull; <strong>Menu page</strong> &mdash; HTML menu (not a PDF &mdash; PDF menus are not crawlable or accessible); allergen filters (required under Natasha&rsquo;s Law for pre-packed foods; best practice for all); specials updated weekly; from <strong>$250</strong>",
			"&bull; <strong>Online reservation</strong> &mdash; ResDiary / OpenTable / SevenRooms / Tock; table size + date + time + dietary notes; deposit option for large parties; from <strong>$300</strong>",
			"&bull; <strong>Food and interior photography</strong> &mdash; professional food shoot 1&ndash;2 hours; hero image carousel; signature dishes; the single highest-ROI investment for restaurant marketing; from <strong>$300</strong> (coordinating shoot, not taking photos)",
			"&bull; <strong>Private dining and events page</strong> &mdash; capacity / AV / room hire fee / catering packages; enquiry form with date + number of guests + occasion; bespoke menus; from <strong>$200</strong>",
			"&bull; <strong>Takeaway / delivery integration</strong> &mdash; direct online ordering (Square, Slerp) vs marketplace (Deliveroo/Uber Eats/Just Eat); direct ordering has no 30% commission; from <strong>$300</strong>",
			"&bull; <strong>Google Business integration</strong> &mdash; menu link synced to Google Business Profile; Reserve with Google button if using ResDiary/OpenTable; opening hours auto-synced; from <strong>$150</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce digital vouchers; Mother&rsquo;s Day / Christmas / birthday gifting; from <strong>$200</strong>",
			"&bull; <strong>From $600</strong> menu + reservation + Google integration; <strong>$1,200+</strong> with food photography coordination + private dining + delivery ordering",
			'',
			"Dine-in, takeaway, or both? Do you take bookings or walk-ins only? Private dining / functions or purely restaurant covers?",
		] );
	}

	// 0d-pre43-c) Landscaper / garden designer / groundsworker / garden maintenance website.
	if ( $has( [ 'landscaper website', 'landscaping website', 'garden designer website', 'garden design website', 'groundsworker website', 'garden maintenance website', 'gardener website', 'lawn care website', 'tree surgeon website', 'arborist website', 'hedge trimming website', 'patio installation website', 'decking installation website', 'artificial grass website', 'turf laying website', 'fencing contractor website', 'driveway installer website', 'outdoor lighting website' ] ) ) {
		return $nl( [
			"Landscaping and garden design websites convert on before-and-after project photography and local trust signals &mdash; the portfolio is the product:",
			'',
			"&bull; <strong>Portfolio / project gallery</strong> &mdash; before and after paired images; garden type filters (formal / cottage / contemporary / low-maintenance / wildlife / roof terrace / commercial); location and approximate budget bracket; the highest-converting content for landscapers; from <strong>$300</strong>",
			"&bull; <strong>Service pages</strong> &mdash; one per service (garden design / full build / planting scheme / lawn care / tree surgery / patio / decking / fencing / driveways / irrigation / lighting / ongoing maintenance); from <strong>$150</strong> per page",
			"&bull; <strong>Design process page</strong> &mdash; consultation &rarr; concept plan &rarr; planting schedule &rarr; build &rarr; aftercare; sets expectations and justifies design fees; from <strong>$200</strong>",
			"&bull; <strong>Free consultation CTA</strong> &mdash; Calendly 30-min site survey; pre-form (garden size / budget range / style preference / timeline); from <strong>$200</strong>",
			"&bull; <strong>Compliance</strong> &mdash; BALI (British Association of Landscape Industries) membership badge; LANTRA-trained note; public liability insurance amount (minimum &pound;1m); waste carrier licence (Environment Agency) &mdash; required for removing soil or plant waste; from <strong>$150</strong>",
			"&bull; <strong>Local SEO pages</strong> &mdash; &ldquo;[city] garden designer&rdquo; / &ldquo;[county] landscaper&rdquo;; Local Business schema; NAP consistency; Google Business photos linked to portfolio; from <strong>$150</strong> per location page",
			"&bull; <strong>Seasonal content</strong> &mdash; spring planting guide / autumn prep / winter garden care; positions company as ongoing expert; email newsletter opt-in; from <strong>$150</strong>",
			"&bull; <strong>From $550</strong> portfolio + service pages + consultation CTA; <strong>$1,100+</strong> with design process + BALI compliance + local SEO pages",
			'',
			"Design only, build only, or full design-and-build? Residential, commercial, or both? Hard landscaping (patios/drives) or soft (planting)?",
		] );
	}

	// 0d-pre42-a) Dentist / dental practice / dental clinic / orthodontist website.
	if ( $has( [ 'dentist website', 'dental practice website', 'dental clinic website', 'nhs dentist website', 'private dentist website', 'orthodontist website', 'cosmetic dentist website', 'dental implants website', 'dental hygienist website', 'teeth whitening website', 'invisalign website', 'braces website', 'emergency dentist website', 'childrens dentist website', 'smile clinic website', 'dental surgery website' ] ) ) {
		return $nl( [
			"Dental practice websites convert on trust and availability &mdash; GDC registration and emergency access are the two conversion anchors:",
			'',
			"&bull; <strong>Treatment pages</strong> &mdash; one per treatment (check-up / hygienist / composite bonding / veneers / whitening / implants / Invisalign / fixed braces / extractions / root canal / emergency / children&rsquo;s dentistry); what to expect description; price or NHS charge band; from <strong>$200</strong> per page",
			"&bull; <strong>Online booking</strong> &mdash; Dentally / Software of Excellence / Exact; NHS vs private slot types; new patient registration form; 24-hour SMS reminder; from <strong>$300</strong>",
			"&bull; <strong>GDC and CQC compliance</strong> &mdash; GDC registration number for every named dentist (required by GDC Standards for Dental Professionals); CQC registration number and most recent inspection outcome (England); practice indemnity insurance; patient feedback and complaints procedure link",
			"&bull; <strong>NHS and private fees page</strong> &mdash; NHS charge bands (Band 1: &pound;26.80 / Band 2: &pound;73.50 / Band 3: &pound;319.10 in England); private fee guide; mixed NHS and private options explained; the most-asked patient question and you should answer it clearly; from <strong>$200</strong>",
			"&bull; <strong>Cosmetic before / after gallery</strong> &mdash; composite bonding / Invisalign / whitening / veneers; patient consent photographs; paired images labelled by treatment; from <strong>$200</strong>",
			"&bull; <strong>Emergency appointments page</strong> &mdash; same-day slots vs 111 referral pathway; emergency call-out fee; knocked-out tooth first-aid guide (re-implant within 30 minutes, milk transport); ranks strongly for &ldquo;[area] emergency dentist&rdquo;; from <strong>$150</strong>",
			"&bull; <strong>Patient testimonials</strong> &mdash; Google Reviews widget; condition-specific quotes (anxiety / implants / children&rsquo;s first visit); video testimonials where available; builds rapport pre-appointment",
			"&bull; <strong>From $600</strong> treatment pages + booking + GDC compliance + NHS fees; <strong>$1,200+</strong> with cosmetic gallery + emergency page + CQC badge",
			'',
			"NHS, private, or mixed? General dentistry, cosmetic, or orthodontics focus? Do you offer sedation for anxious patients?",
		] );
	}

	// 0d-pre42-b) Optician / optical practice / optometrist / contact lens specialist website.
	if ( $has( [ 'optician website', 'optical practice website', 'optometrist website', 'contact lens specialist website', 'glasses website', 'prescription glasses website', 'sunglasses website', 'eye test website', 'eye examination website', 'childrens eye test website', 'dry eye clinic website', 'myopia management website', 'ophthalmologist website', 'vision therapy website', 'spectacle website', 'eyewear website' ] ) ) {
		return $nl( [
			"Optical practice websites convert on GOC trust signals and the frame selection experience &mdash; these are your two highest-converting pages:",
			'',
			"&bull; <strong>Eye examination page</strong> &mdash; GOC registered optometrist credentials; standard sight test price (&pound;30&ndash;&pound;55 typical); OCT scan premium add-on; diabetes / glaucoma / AMD screening; children&rsquo;s NHS-funded sight tests; what to bring and how long it takes; from <strong>$200</strong>",
			"&bull; <strong>Online appointment booking</strong> &mdash; Optinet / Acuity / Phorest; test type selector (routine / contact lens fit / OCT / emergency / children); practitioner preference; from <strong>$300</strong>",
			"&bull; <strong>GOC compliance</strong> &mdash; GOC (General Optical Council) registration number for every named optometrist (required); CQC registration if applicable; patient feedback and complaints link; professional indemnity statement",
			"&bull; <strong>Frames and lenses page</strong> &mdash; designer brands stocked (Ray-Ban / Oakley / Tom Ford / Lindberg / Silhouette); lens types (single vision / bifocal / varifocal / photochromic / thinned); coatings (UV / anti-reflective / blue light filter / scratch-resistant); price ranges; from <strong>$250</strong>",
			"&bull; <strong>Virtual try-on</strong> &mdash; Ditto or Fittingbox embed; reduces frame-selection anxiety and returns; link to brand virtual try-on tools if native embed isn&rsquo;t feasible; from <strong>$200</strong>",
			"&bull; <strong>Contact lens page</strong> &mdash; daily / monthly / toric / multifocal / extended-wear; reorder subscription link (Contactlensesuk / Vision Direct affiliate); contact lens check-up booking; from <strong>$200</strong>",
			"&bull; <strong>Myopia management page</strong> &mdash; growing specialist area; Ortho-K / MiSight contact lenses / low-dose atropine drops; evidence base summary; parent-focused content; from <strong>$200</strong>",
			"&bull; <strong>From $550</strong> exam page + booking + GOC compliance; <strong>$1,100+</strong> with frames page + virtual try-on + myopia management",
			'',
			"Independent or franchise? Specialisms: myopia management, low vision, dry eye, or sports eyewear? Do you handle hearing aids too?",
		] );
	}

	// 0d-pre42-c) Chiropractor / physiotherapist / osteopath / sports therapist website.
	if ( $has( [ 'chiropractor website', 'physiotherapist website', 'physio website', 'osteopath website', 'sports therapist website', 'massage therapist website', 'sports massage website', 'back pain clinic website', 'neck pain website', 'sports injury website', 'rehabilitation clinic website', 'acupuncturist website', 'manual therapist website', 'musculoskeletal clinic website', 'msk clinic website', 'sports clinic website', 'chiropractic website' ] ) ) {
		return $nl( [
			"Musculoskeletal clinic websites convert best on condition pages, not treatment pages &mdash; patients search for their problem, not the modality:",
			'',
			"&bull; <strong>Condition pages (not treatment pages)</strong> &mdash; build pages for conditions (lower back pain / sciatica / neck pain / shoulder pain / knee pain / hip pain / sports injury / post-operative rehab / headaches and migraines / pregnancy-related pain); patients search &ldquo;lower back pain treatment&rdquo;, not &ldquo;chiropractic adjustment&rdquo;; from <strong>$150</strong> per condition page",
			"&bull; <strong>Online booking</strong> &mdash; Cliniko / Power Diary / Jane App; appointment type + practitioner + location; new patient vs follow-up distinction; from <strong>$300</strong>",
			"&bull; <strong>Regulatory registration</strong> &mdash; GCC (General Chiropractic Council) number for chiropractors (required by law under the Chiropractors Act 1994); HCPC registration number for physiotherapists (required by law); GOSC for osteopaths (required by law); Complementary and Natural Healthcare Council for massage and sports therapists; display registration numbers prominently",
			"&bull; <strong>Practitioner profiles</strong> &mdash; professional headshots + registration number + areas of specialism + CPD interests + any sport or activity they treat regularly; builds rapport before the appointment; from <strong>$150</strong> per profile",
			"&bull; <strong>Condition-matched testimonials</strong> &mdash; display lower back testimonials on the lower back page, shoulder on the shoulder page; maximises trust signal relevance",
			"&bull; <strong>Self-help video content</strong> &mdash; exercise rehabilitation guides for common conditions hosted on YouTube; &ldquo;when to seek professional help&rdquo; guidance; builds organic search and patient trust between appointments",
			"&bull; <strong>Insurance and BUPA / AXA page</strong> &mdash; list accepted private health insurers; direct billing note where applicable; employer healthcare scheme note; from <strong>$150</strong>",
			"&bull; <strong>From $500</strong> condition pages + booking + registration compliance; <strong>$1,000+</strong> with practitioner profiles + insurance page + self-help content",
			'',
			"Chiropractor, physio, osteopath, or sports therapist? Sole practitioner or a clinic? Sports injury focus or general MSK?",
		] );
	}

	// 0d-pre41-a) Solicitor / law firm / barrister / legal services website.
	if ( $has( [ 'solicitor website', 'law firm website', 'barrister website', 'lawyer website', 'legal services website', 'conveyancing solicitor website', 'family law website', 'employment law website', 'criminal defence website', 'personal injury website', 'immigration solicitor website', 'probate solicitor website', 'will writing website', 'commercial law website', 'litigation website', 'legal firm website', 'law practice website' ] ) ) {
		return $nl( [
			"Law firm websites must balance SRA regulatory compliance with genuine conversion — here&rsquo;s what drives new client enquiries:",
			'',
			"&bull; <strong>Practice area pages</strong> &mdash; one per specialism (conveyancing / family / employment / criminal defence / personal injury / immigration / probate and estate administration / commercial / wills and LPAs / dispute resolution); fees by practice area where required; from <strong>$200</strong> per page",
			"&bull; <strong>SRA compliance</strong> &mdash; SRA badge (required by SRA Code of Conduct &mdash; must display Solicitors Regulation Authority name and registration number); regulated disclaimer; client money protection statement; Lexcel or ISO 9001 quality mark if held",
			"&bull; <strong>Fixed fee and price transparency</strong> &mdash; SRA requires published price information for family (divorce), immigration, conveyancing, motoring offences, employment, and wills; transparent pricing or a Gravity Forms quote-request flow; from <strong>$250</strong>",
			"&bull; <strong>Client portal</strong> &mdash; Osprey Approach / Clio / LEAP / Action Step; secure document upload; matter progress updates; reduces admin calls; from <strong>$400</strong>",
			"&bull; <strong>Case studies / testimonials</strong> &mdash; client consent essential; anonymise employment and family cases; full name and outcome acceptable for personal injury and commercial transactions; from <strong>$150</strong> per case study",
			"&bull; <strong>Accreditations</strong> &mdash; Lexcel / CQS conveyancing quality scheme / Resolution (family) / APIL membership (personal injury) / Law Society Diversity & Inclusion Charter; trust badges build immediate credibility on first visit",
			"&bull; <strong>Legal resource hub</strong> &mdash; &ldquo;What is a Section 21 notice?&rdquo; / &ldquo;Employment tribunal process explained&rdquo; long-form guides; organic search traffic and client education; from <strong>$300</strong>",
			"&bull; <strong>From $600</strong> practice pages + SRA compliance + price transparency; <strong>$1,200+</strong> with client portal + resource hub + case studies",
			'',
			"Which practice areas? High street general or specialist firm? Legal aid, privately funded, or fixed fee?",
		] );
	}

	// 0d-pre41-b) Accountant / bookkeeper / chartered accountant / tax advisor website.
	if ( $has( [ 'accountant website', 'accounting firm website', 'bookkeeper website', 'chartered accountant website', 'tax advisor website', 'management accountant website', 'small business accountant website', 'self assessment website', 'vat specialist website', 'payroll services website', 'xero accountant website', 'quickbooks accountant website', 'cloud accountant website', 'cpa website', 'financial controller website', 'accounting practice website', 'tax return website' ] ) ) {
		return $nl( [
			"Accounting firm sites convert two types of client &mdash; compliance clients (annual accounts, tax returns) and advisory clients (management accounts, growth planning) &mdash; speak to both:",
			'',
			"&bull; <strong>Services pages</strong> &mdash; one per service (annual accounts / self-assessment / corporation tax / VAT returns / payroll / bookkeeping / management accounts / R&amp;D tax credits / business advisory / cloud accounting setup); frame compliance as &ldquo;we keep you legal&rdquo; and advisory as &ldquo;we grow your business&rdquo;; from <strong>$200</strong> per page",
			"&bull; <strong>Software partner badges</strong> &mdash; Xero Platinum / Gold / Silver partner badge (the most influential trust signal for small business accounting clients choosing a cloud-first firm); QuickBooks ProAdvisor; FreeAgent certified; from <strong>$150</strong>",
			"&bull; <strong>Free consultation booking</strong> &mdash; Calendly 30-min; pre-call form (business type / estimated turnover band / current accounting software / main pain point); auto-confirm with what to bring; from <strong>$200</strong>",
			"&bull; <strong>Pricing page</strong> &mdash; packages (e.g. Start / Grow / Scale or Sole Trader / Limited Company / Group); monthly retainer price or &ldquo;from &pound;X per month&rdquo;; accounting software subscription bundled or not; reduces price-enquiry calls; from <strong>$250</strong>",
			"&bull; <strong>ICAEW / ACCA / CIMA / ICAS membership</strong> &mdash; professional body badge (required for regulated services); professional indemnity insurance amount; AML supervision statement (accountants are supervised for anti-money laundering under the Money Laundering Regulations &mdash; this must be disclosed); from <strong>$150</strong>",
			"&bull; <strong>Tax and deadline calendar page</strong> &mdash; Companies House confirmation statement / HMRC self-assessment / corporation tax / VAT deadlines; helps clients understand the timing pressure and appreciate the value of a proactive accountant; builds retention",
			"&bull; <strong>Client portal</strong> &mdash; Xero Practice Manager / Karbon / SENTA / TaxCalc Cloud; secure document upload and e-signature; from <strong>$350</strong>",
			"&bull; <strong>From $550</strong> services + software badges + consultation booking; <strong>$1,100+</strong> with pricing page + client portal + tax calendar",
			'',
			"Sole traders, limited companies, or both? Which software do you use? Cloud-first or traditional practice?",
		] );
	}

	// 0d-pre41-c) Mortgage broker / IFA / financial adviser / protection broker website.
	if ( $has( [ 'mortgage broker website', 'mortgage advisor website', 'independent financial adviser website', 'ifa website', 'financial advisor website', 'remortgage website', 'first time buyer mortgage website', 'buy to let mortgage website', 'protection broker website', 'life insurance broker website', 'critical illness cover website', 'income protection website', 'whole of market broker website', 'equity release website', 'mortgage adviser website', 'financial planning website' ] ) ) {
		return $nl( [
			"Mortgage broker and IFA websites must earn trust through FCA credentials before a visitor will share their financial situation &mdash; here&rsquo;s the conversion funnel:",
			'',
			"&bull; <strong>Mortgage types page</strong> &mdash; first-time buyer / remortgage / buy-to-let / Help to Buy / shared ownership / self-employed / adverse credit / equity release; brief description and typical timeline per type; from <strong>$250</strong>",
			"&bull; <strong>FCA authorisation disclosure</strong> &mdash; required by FCA rules; must state that the firm is authorised and regulated by the Financial Conduct Authority, include the FCA firm reference number (FRN), and link to the FCA register; professional indemnity insurance note; from <strong>$150</strong>",
			"&bull; <strong>Affordability calculator</strong> &mdash; indicative borrowing amount (salary multiplier up to 4.5&times; + LTV/deposit slider); captures lead intent data; &ldquo;our broker will be in touch to find you the best deal&rdquo; CTA; from <strong>$350</strong>",
			"&bull; <strong>Fee transparency</strong> &mdash; FCA rules require clear statement of broker fees; state the broker fee (typical &pound;300&ndash;&pound;695) OR &ldquo;fee-free, we are paid by the lender by way of procuration fee / commission&rdquo;; hiding fees triggers FCA compliance risk; from <strong>$150</strong>",
			"&bull; <strong>Case studies</strong> &mdash; first-time buyer with gifted deposit / self-employed sole trader with one year of accounts / adverse credit with settled defaults / buy-to-let portfolio remortgage; real scenario without identifying details; from <strong>$200</strong> per case study",
			"&bull; <strong>Protection page</strong> &mdash; life insurance / critical illness / income protection / buildings and contents; many clients don&rsquo;t know brokers handle protection as well as mortgages; from <strong>$200</strong>",
			"&bull; <strong>Initial consultation booking</strong> &mdash; Calendly; fact-find form (purchase or remortgage + property value + deposit amount + employment type + any adverse credit history); from <strong>$250</strong>",
			"&bull; <strong>From $550</strong> mortgage types + FCA disclosure + affordability calculator; <strong>$1,100+</strong> with fee transparency page + case studies + protection page",
			'',
			"Whole of market or tied? Residential, buy-to-let, or both? Do you handle protection products too?",
		] );
	}

	// 0d-pre40-a) Beautician / beauty salon / nail salon / nail technician / lash tech website.
	if ( $has( [ 'beautician website', 'beauty salon website', 'nail salon website', 'nail technician website', 'beauty therapist website', 'lash technician website', 'lash extensions website', 'eyebrow technician website', 'makeup artist website', 'brow artist website', 'spray tan website', 'skin clinic website', 'aesthetics clinic website', 'semi-permanent makeup website', 'microblading website', 'waxing salon website', 'beauty studio website' ] ) ) {
		return $nl( [
			"Beauty and nail salon websites convert on trust and a clear treatment menu &mdash; here&rsquo;s what drives bookings:",
			'',
			"&bull; <strong>Treatment menu</strong> &mdash; full list with treatment description, duration, and price; bundle packages (e.g. gel nails + eyebrow shape + spray tan); patch-test note where required (lash/brow tints, wax, skin peels); from <strong>$300</strong>",
			"&bull; <strong>Online booking</strong> &mdash; Fresha (free to use), Timely, or Booksy; treatment + therapist preference; deposit at booking (reduces no-shows by approximately 60%); 24-hour SMS reminder; automated rebooking suggestion; from <strong>$350</strong>",
			"&bull; <strong>Credentials and compliance</strong> &mdash; VTCT / CIBTAC / NVQ Level 3 qualification; public liability insurance amount; patch-test appointment (£10 credited against treatment); consent-form link for skin and lash treatments; trust signals that justify premium pricing",
			"&bull; <strong>Before / after gallery</strong> &mdash; consent client photos; labelled by treatment (lash extensions / nail art / brow lamination / skin result); before and after paired images; from <strong>$250</strong>",
			"&bull; <strong>Pricing page</strong> &mdash; transparent pricing builds trust in beauty specifically; hiding prices costs enquiries; include duration so clients can plan their visit; from <strong>$200</strong>",
			"&bull; <strong>Aesthetics / skin clinic page</strong> &mdash; if you offer injectable aesthetics (Botox, fillers, skin peels, RF, LED), this needs its own page; UK law (April 2024) requires a prescribing clinician for injectable aesthetics &mdash; this must be stated on the site; from <strong>$300</strong>",
			"&bull; <strong>Loyalty and gift cards</strong> &mdash; WooCommerce gift vouchers; digital loyalty stamp card (Floomby or Noqu); email delivery; birthday and Christmas push; from <strong>$200</strong>",
			"&bull; <strong>From $500</strong> treatment menu + booking + gallery; <strong>$900+</strong> with credentials + loyalty scheme + aesthetics page",
			'',
			"Nails only, full beauty, or aesthetics too? Home-visit, salon, or mobile? Solo therapist or a team?",
		] );
	}

	// 0d-pre40-b) Personal trainer / PT studio / online fitness coach / gym / fitness studio website.
	if ( $has( [ 'personal trainer website', 'pt website', 'personal training website', 'fitness coach website', 'online fitness coach website', 'online personal trainer website', 'gym website', 'fitness studio website', 'bootcamp website', 'weight loss coach website', 'strength coach website', 'crossfit gym website', 'yoga studio website', 'pilates studio website', 'fitness instructor website', 'sports coach website' ] ) ) {
		return $nl( [
			"Personal trainer websites convert on transformation proof and a clear entry point &mdash; the lead magnet and the before/after gallery do the heavy lifting:",
			'',
			"&bull; <strong>Training offer page</strong> &mdash; 1:1 PT (face-to-face or online) / small group training / classes / 8-week transformation programme; clear price or &ldquo;from &pound;X&rdquo;; what&rsquo;s included (programme, check-ins, nutrition guidance); from <strong>$300</strong>",
			"&bull; <strong>Transformation gallery</strong> &mdash; before and after with client name + stats (kg lost / deadlift PB / 5k time); client consent required; the highest-converting content on PT sites; video testimonials where available; from <strong>$250</strong>",
			"&bull; <strong>Discovery call booking</strong> &mdash; Calendly 15-min free consultation; pre-call form (current fitness level / goals / training history / injuries); auto-reply with what to expect; from <strong>$200</strong>",
			"&bull; <strong>Lead magnet</strong> &mdash; 4-week beginner programme PDF / &ldquo;What to eat around your workouts&rdquo; guide / 7-day challenge; Mailchimp or ConvertKit email sequence; builds list and warms leads; from <strong>$300</strong>",
			"&bull; <strong>REPs / CIMSPA registration</strong> &mdash; Register of Exercise Professionals Level 3 minimum; CIMSPA membership; public liability and professional indemnity insurance (required for gym hire); DBS certificate for under-18 clients; trust signals that justify premium pricing",
			"&bull; <strong>Nutrition and programming page</strong> &mdash; macro coaching / meal plan / MyFitnessPal integration; only if Level 4 qualified in nutrition (Level 3 PTs cannot give medical nutrition advice); from <strong>$200</strong>",
			"&bull; <strong>Online coaching portal</strong> &mdash; True Coach or PT Distinction for programme delivery and check-ins; or custom members area; scales beyond 1:1 time; from <strong>$400</strong>",
			"&bull; <strong>From $500</strong> offer page + transformation gallery + booking; <strong>$1,000+</strong> with lead magnet + online coaching portal + nutrition page",
			'',
			"Face-to-face, online, or both? 1:1 only or group classes too? Specific niche (weight loss, sports performance, post-natal, over-50s)?",
		] );
	}

	// 0d-pre40-c) Driving school / driving instructor / intensive course / driving tuition website.
	if ( $has( [ 'driving school website', 'driving instructor website', 'driving lessons website', 'driving tuition website', 'intensive driving course website', 'pass plus website', 'automatic driving lessons website', 'manual driving lessons website', 'dvsa approved website', 'driving academy website', 'driving school near me website', 'driving lesson website', 'crash course driving website' ] ) ) {
		return $nl( [
			"Driving school websites have three conversion levers: local SEO, pass rate, and a frictionless booking path:",
			'',
			"&bull; <strong>Lesson types and prices</strong> &mdash; manual vs automatic; hourly rate displayed (hiding it loses enquiries); block booking discount (e.g. 10 lessons = &pound;X); intensive / crash course (5-day or weekly intensive pass); from <strong>$250</strong>",
			"&bull; <strong>Pass rate and reviews</strong> &mdash; DVSA first-attempt test pass rate if available; honest framing (&ldquo;above the national average of 45.7%&rdquo; is current UK average); Google / Trustpilot reviews widget; most persuasive conversion signal for learners choosing an instructor; from <strong>$200</strong>",
			"&bull; <strong>Online booking</strong> &mdash; Appointy / Scheduling+ / Acuity; pick-up postcode + preferred test centre; lesson type; from <strong>$300</strong>",
			"&bull; <strong>Intensive / crash course page</strong> &mdash; course dates + accommodation option near test centre; higher-margin product than hourly lessons; enquiry form with target pass date; from <strong>$150</strong>",
			"&bull; <strong>Local SEO pages</strong> &mdash; &ldquo;[city] driving lessons&rdquo; + &ldquo;[postcode area] driving instructor&rdquo; + &ldquo;[town] driving school&rdquo;; NAP consistency; Local Business schema; first position for location query is the primary revenue driver for small driving schools; from <strong>$200</strong>",
			"&bull; <strong>Theory test preparation</strong> &mdash; links to DVSA official mock tests; hazard perception tips; &ldquo;is my theory test still valid?&rdquo; note (2 years from pass date); from <strong>$150</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce gift cards; email delivery; Christmas and 17th-birthday push are the two peak gifting moments; from <strong>$150</strong>",
			"&bull; <strong>From $500</strong> lesson types + pass rate + booking; <strong>$900+</strong> with intensive page + local SEO pages + gift vouchers",
			'',
			"Manual, automatic, or both? One instructor or a school with several? Which test centres do you cover?",
		] );
	}

	// 0d-pre37-a) Music school / music lessons / instrument teacher / music tuition website.
	if ( $has( [ 'music school website', 'music lessons website', 'guitar teacher website', 'piano teacher website', 'guitar lessons website', 'piano lessons website', 'drum lessons website', 'violin lessons website', 'singing lessons website', 'voice lessons website', 'music tuition website', 'music academy website', 'instrument lessons website', 'online music lessons website', 'music teacher website', 'music studio website' ] ) ) {
		return $nl( [
			"Music school and private teacher websites need to convert both parents (children&rsquo;s lessons) and adult learners:",
			'',
			"&bull; <strong>Instrument / lesson-type pages</strong> &mdash; one per instrument (guitar / piano / drums / violin / singing / bass / ukulele / saxophone / keyboard / cello); includes grade ladder (beginner through Grade 8 / diploma); in-person and online lesson options; from <strong>$200</strong> per page",
			"&bull; <strong>Trial lesson booking</strong> &mdash; WP Amelia or Calendly; reduced-rate first slot (&pound;15 for 30 min is the industry standard trial offer); highest lead-to-student converter on music sites; auto-confirm with pre-lesson notes; from <strong>$250</strong>",
			"&bull; <strong>Timetable and lesson booking</strong> &mdash; recurring slot (weekly / fortnightly); 30 / 45 / 60-minute duration; teacher preference (multi-teacher); Stripe card-on-file or direct debit; from <strong>$400</strong>",
			"&bull; <strong>Teacher profiles</strong> &mdash; headshots + qualifications (LRAM / LLCM / BMus / Dip ABRSM / PGCE music); performance background (orchestral / touring / recording); exam board experience (ABRSM / RSL / Trinity); DBS certificate for under-18s",
			"&bull; <strong>Grade exam results</strong> &mdash; Distinction and Merit tally by grade; recent ABRSM / RSL / Trinity pass rate; major purchase signal for parents choosing a teacher for their child",
			"&bull; <strong>Online lessons page</strong> &mdash; Zoom / Teams-compatible setup; equipment advice (instrument, mic, stand); latency note (fine for one-to-one, not for ensemble); global reach; from <strong>$150</strong>",
			"&bull; <strong>From $600</strong> instrument pages + trial booking + teacher profiles; <strong>$1,100+</strong> with timetable system + online lesson page + exam results showcase",
			'',
			"One teacher or a school with several? Which instruments? ABRSM / RSL / Trinity, or ungraded?",
		] );
	}

	// 0d-pre37-b) Escape room / immersive experience / adventure gaming / laser tag / axe throwing website.
	if ( $has( [ 'escape room website', 'escape room booking website', 'immersive experience website', 'adventure gaming website', 'laser tag website', 'axe throwing website', 'virtual reality experience website', 'vr experience website', 'puzzle room website', 'mystery room website', 'immersive gaming website', 'team building venue website', 'family entertainment website', 'indoor adventure website' ] ) ) {
		return $nl( [
			"Escape room and adventure experience websites have one job: fill slots and push group bookings:",
			'',
			"&bull; <strong>Room / experience pages</strong> &mdash; one per theme (mystery / horror / adventure / sci-fi / heist / family); difficulty rating (novice &ndash; expert); min/max players; suitability tags (corporate / birthday / date night / stag/hen / family); record times; from <strong>$250</strong> per room",
			"&bull; <strong>Live slot booking</strong> &mdash; FareHarbor / Xola / Peek Pro / Checkfront; real-time availability; group size selector; Stripe payment at booking; automated reminder emails 24h before; from <strong>$450</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce gift cards; email delivery; monetary or experience-specific; Christmas + birthday push; from <strong>$200</strong>",
			"&bull; <strong>Corporate and team-building page</strong> &mdash; private hire (whole venue / single room); invoice BACS payment for businesses; post-game debrief option; bespoke challenge design; enquiry form with date + group size + catering; from <strong>$250</strong>",
			"&bull; <strong>Birthday / hen / stag packages</strong> &mdash; tiered packages (room-only / + prosecco / + pizza + prosecco); promotional codes; group discount logic (10+ players = 15% off); from <strong>$200</strong>",
			"&bull; <strong>Record board</strong> &mdash; fastest completion times per room; encourages rebooking; drives social media sharing; cheap to build, high engagement",
			"&bull; <strong>Multi-location</strong> &mdash; location selector on homepage; one sub-page per venue with own booking calendar; from <strong>$400</strong>",
			"&bull; <strong>From $600</strong> rooms + booking + vouchers; <strong>$1,200+</strong> corporate page + packages + record board + multi-location",
			'',
			"How many rooms? Solo venue or multiple locations? Is corporate bookings a priority?",
		] );
	}

	// 0d-pre37-c) Franchise / franchisor / franchise opportunity / become a franchisee website.
	if ( $has( [ 'franchise website', 'franchisor website', 'franchise opportunity website', 'franchise for sale website', 'franchise recruitment website', 'franchise business website', 'buy a franchise website', 'start a franchise website', 'become a franchisee website', 'franchise network website', 'franchise opportunity uk', 'franchise model website', 'franchisee website' ] ) ) {
		return $nl( [
			"Franchise websites serve two very different audiences: recruiting new franchisees and supporting existing ones. Which side you&rsquo;re on changes everything:",
			'',
			"<strong>Franchisor (recruiting prospects)</strong>",
			"&bull; <strong>Franchise opportunity page</strong> &mdash; investment range (licence fee + working capital); territory map; earnings potential (realistic, not inflated); what&rsquo;s included (training / support / exclusive territory / brand / tech stack); from <strong>$500</strong>",
			"&bull; <strong>Discovery day / enquiry flow</strong> &mdash; multi-step qualification form (current occupation, liquid capital, location, motivation); discovery day booking with Calendly; pre-NDA information pack triggered on email; from <strong>$350</strong>",
			"&bull; <strong>Franchisee testimonials and case studies</strong> &mdash; real franchisee stories (not corporate PR); revenue ranges if willing to share; day-in-the-life videos; most persuasive asset in franchise recruitment",
			"&bull; <strong>Franchise awards and credentials</strong> &mdash; bfa membership (British Franchise Association); Franchise Direct ratings; British Franchise Awards nominations; trust signals for prospects doing due diligence",
			"&bull; <strong>Franchisee portal</strong> &mdash; training materials, marketing assets, operations manuals, brand guidelines behind a login; from <strong>$400</strong>",
			"&bull; <strong>From $800</strong> opportunity page + discovery day + testimonials; <strong>$1,500+</strong> with territory map + portal + full recruitment funnel",
			'',
			"<strong>Franchisee (individual territory site)</strong>",
			"&bull; Local franchisee site under main brand&rsquo;s domain architecture (/location or subdomain); local NAP consistency; local testimonials and photos; service area schema; CTA routes to national booking system or local number; from <strong>$300</strong>",
			'',
			"Are you the franchisor recruiting new franchisees, or an individual franchisee needing a local site?",
		] );
	}

	// 0d-pre36-a) Estate agent / lettings agent / property management company website.
	if ( $has( [ 'estate agent website', 'lettings agent website', 'property management website', 'estate agency website', 'letting agent website', 'property agent website', 'property developer website', 'new homes website', 'residential sales website', 'commercial property website', 'property sales website', 'landlord website', 'property management company website', 'block management website', 'hmo management website' ] ) ) {
		return $nl( [
			"Estate agent and letting agent websites must convert vendors and landlords, not just browsers &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Property search and listings</strong> &mdash; WP-Property, Easy Property Listings, or custom CPT; search by price / beds / type / area; saved searches with email alerts; floor plan + virtual tour embed; Rightmove/Zoopla feed integration (CRM-dependent); from <strong>$600</strong>",
			"&bull; <strong>Valuation lead capture</strong> &mdash; &ldquo;Get a free valuation&rdquo; is your highest-value conversion; instant online estimate (Property Data API or Hometrack) + scheduled in-person valuation booking (Calendly); Gravity Forms with postcode lookup; from <strong>$350</strong>",
			"&bull; <strong>Landlord and vendor pages</strong> &mdash; separate landing pages per audience; landlord page covers yield, compliance (EICR / EPC / gas safety / HMO licence), and tenant-find vs full management tiers; vendor page covers sales process timeline and fees; from <strong>$300</strong>",
			"&bull; <strong>Tenant portal / repair request</strong> &mdash; maintenance ticket form; tenancy document access (AST / deposit certificate / inventory); integrates with Arthur or Fixflo; from <strong>$300</strong>",
			"&bull; <strong>Branch pages</strong> &mdash; one per office; Local Business schema; area guide content (&ldquo;best streets in [town]&rdquo; long-form); school catchment links; commute times; strong for local SEO",
			"&bull; <strong>CRM integration</strong> &mdash; Reapit / Jupix / Dezrez / Alto (Zoopla) / Property Hive (WP plugin); discuss with your CRM provider before build to confirm data export format",
			"&bull; <strong>From $700</strong> listings + valuation + branch page; <strong>$1,400+</strong> with tenant portal + landlord/vendor pages + CRM feed + area guides",
			'',
			"Residential sales, lettings, or both? How many branches? Which CRM are you on?",
		] );
	}

	// 0d-pre36-b) Dance studio / dance school / ballet school / performing arts academy website.
	if ( $has( [ 'dance studio website', 'dance school website', 'ballet school website', 'performing arts website', 'dance academy website', 'dance lessons website', 'ballet website', 'ballroom dancing website', 'contemporary dance website', 'tap dance website', 'street dance website', 'salsa dance website', 'dance class website', 'latin dance website', 'swing dance website', 'irish dance website', 'cheerleading website' ] ) ) {
		return $nl( [
			"Dance studio websites need to convert parents for children&rsquo;s classes and adults for recreational and competitive training:",
			'',
			"&bull; <strong>Class timetable</strong> &mdash; WP Amelia or The Booking Factory; filter by age group / style / level / day; capacity per slot; add-to-Google-Calendar link; from <strong>$400</strong>",
			"&bull; <strong>Online enrolment + direct debit</strong> &mdash; WooCommerce Subscriptions for monthly class packages; GoCardless for DD (UK); or Stripe recurring; trial class booking with Stripe deposit; from <strong>$400</strong>",
			"&bull; <strong>Style pages</strong> &mdash; one per discipline (ballet / tap / jazz / contemporary / hip-hop / ballroom / Latin / Irish / acro / street); level ladder from beginner to performance; FAQ; from <strong>$200</strong> per page",
			"&bull; <strong>Show and performance archive</strong> &mdash; annual show gallery; video reel; student testimonials; inspires prospective students and reassures parents about performance opportunities",
			"&bull; <strong>Exam and festival results</strong> &mdash; ISTD / RAD / IDTA exam grades; festival trophy results; competitive credentials are a major purchase signal for competitive families",
			"&bull; <strong>Teacher profiles</strong> &mdash; headshots + training background + performance history + syllabus qualifications; reduces anonymous-studio anxiety, especially for children&rsquo;s classes",
			"&bull; <strong>Uniform and merchandise shop</strong> &mdash; WooCommerce; branded kit + show costume sales; from <strong>$300</strong>",
			"&bull; <strong>Safeguarding and DBS notice</strong> &mdash; enhanced DBS note; safeguarding policy PDF; GDPR media consent statement; required for any studio with under-18 students",
			"&bull; <strong>From $600</strong> timetable + enrolment + style pages; <strong>$1,100+</strong> with show archive + shop + exam results + teacher profiles",
			'',
			"Children&rsquo;s classes, adult recreational, competitive, or all three? How many disciplines?",
		] );
	}

	// 0d-pre36-c) Wedding photographer / event photographer / portrait photographer / commercial photographer website.
	if ( $has( [ 'wedding photographer website', 'wedding photography website', 'event photographer website', 'portrait photographer website', 'commercial photographer website', 'photographer portfolio website', 'photography portfolio website', 'headshot photographer website', 'family photographer website', 'newborn photographer website', 'boudoir photographer website', 'product photographer website', 'architectural photographer website', 'food photographer website', 'brand photographer website', 'lifestyle photographer website' ] ) ) {
		return $nl( [
			"Photographer websites are portfolio-first but must convert enquiries &mdash; the two goals can conflict if gallery weight hurts load speed or buries the CTA:",
			'',
			"&bull; <strong>Portfolio / gallery</strong> &mdash; Justified Image Grid or Modula; WebP with lazy load; full-screen lightbox; curated (30&ndash;50 images, not your archive); load time is conversion time; from <strong>$400</strong>",
			"&bull; <strong>Enquiry and booking flow</strong> &mdash; Gravity Forms or HoneyBook / Studio Ninja integration; wedding form: date + venue + guest count + how they found you; portrait: session type + preferred date; auto-reply with pricing guide PDF; from <strong>$300</strong>",
			"&bull; <strong>Real wedding / real shoot pages</strong> &mdash; one page per featured commission (venue + suppliers + short narrative + 15&ndash;20 select images); each targets &ldquo;[venue name] wedding photographer&rdquo; keyword; from <strong>$150</strong> per page",
			"&bull; <strong>Packages and pricing page</strong> &mdash; transparent or enquiry-only (both approaches work; transparent reduces volume but increases close rate); wedding: coverage hours + album + second shooter; portrait: duration + digital files + prints",
			"&bull; <strong>Testimonials and reviews</strong> &mdash; Google Reviews widget; one pull quote per gallery section; video testimonials where available; emotional buying decision &mdash; social proof closes it",
			"&bull; <strong>Style and approach page</strong> &mdash; documentary vs posed; natural light vs studio; editing style (film emulation / moody / bright + airy); helps couples self-select and reduces mismatched enquiries",
			"&bull; <strong>Location SEO</strong> &mdash; &ldquo;[city] wedding photographer&rdquo; landing pages; &ldquo;[venue] wedding photography&rdquo; from real-shoot pages; from <strong>$100</strong> per page",
			"&bull; <strong>From $500</strong> portfolio + enquiry form + packages; <strong>$1,000+</strong> with real-shoot pages + location SEO + CRM integration + testimonial architecture",
			'',
			"Wedding / portrait / commercial / events? Primary region? Do you use Studio Ninja, HoneyBook, or Sprout Studio?",
		] );
	}

	// 0d-pre35-a) Subscription box / DTC subscription brand / product subscription / mystery box website.
	if ( $has( [ 'subscription box website', 'subscription website', 'subscription box', 'monthly box website', 'dtc subscription website', 'direct to consumer website', 'product subscription website', 'mystery box website', 'gift box subscription website', 'beauty box website', 'snack box website', 'book subscription website', 'hobby box website', 'kids subscription box', 'pet subscription box website' ] ) ) {
		return $nl( [
			"Subscription box websites live or die by conversion rate, churn, and average order value &mdash; here&rsquo;s how to build one that performs:",
			'',
			"&bull; <strong>WooCommerce Subscriptions</strong> &mdash; monthly, quarterly, annual billing; pause and skip options (pause > cancel for churn reduction); gift subscription option; trial or first-box discount; Stripe + PayPal; from <strong>$600</strong>",
			"&bull; <strong>Box contents page / what&rsquo;s inside</strong> &mdash; current month&rsquo;s box reveal with product photography; spoiler count (e.g. &ldquo;6&ndash;8 full-size products&rdquo;); retail value callout (&ldquo;&pound;80 value for &pound;29.99&rdquo;); past box archive as proof of quality",
			"&bull; <strong>Quiz or personalisation flow</strong> &mdash; 3&ndash;5 question style quiz (skin type, dietary preference, interests) before checkout; feeds curation logic or WooCommerce product variations; reduces perceived risk; from <strong>$350</strong>",
			"&bull; <strong>Referral programme</strong> &mdash; ReferralHero or WooCommerce Coupons; subscriber gets a unique link; reward per paid referral (free box, discount, bonus product); from <strong>$300</strong>",
			"&bull; <strong>Subscriber portal</strong> &mdash; WooCommerce Subscriptions self-service; skip a month, update address, swap plan, view past boxes; self-service eliminates support tickets and reduces churn; from <strong>$300</strong>",
			"&bull; <strong>Waitlist and scarcity</strong> &mdash; sold-out months create FOMO; email capture with launch notification; countdown timer for next box cutoff; from <strong>$150</strong>",
			"&bull; <strong>Trust and social proof</strong> &mdash; Trustpilot widget; UGC gallery (Instagram unboxing photos via Taggbox or Curator); influencer mention logos; key subscriber-count milestone badge",
			"&bull; <strong>From $700</strong> subscriptions, box reveal, and subscriber portal; <strong>$1,300+</strong> with quiz personalisation, referral programme, and influencer UGC gallery",
			'',
			"What&rsquo;s the niche, what&rsquo;s the price point, and how many active subscribers are you targeting in year one?",
		] );
	}

	// 0d-pre35-b) Mortgage broker / IFA / financial planner / regulated financial advice website.
	if ( $has( [ 'mortgage broker website', 'mortgage adviser website', 'mortgage advisor website', 'ifa website', 'independent financial adviser website', 'financial planner website', 'financial planning website', 'wealth management website', 'financial advice website', 'remortgage website', 'buy to let mortgage website', 'first time buyer website', 'equity release website', 'protection adviser website', 'pension adviser website', 'regulated financial advice website' ] ) ) {
		return $nl( [
			"Mortgage broker and IFA websites operate under strict FCA regulation and must balance lead generation with compliance &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>FCA compliance essentials</strong> &mdash; FCA number and &ldquo;authorised and regulated by the Financial Conduct Authority&rdquo; in footer; risk warnings on relevant pages (e.g. &ldquo;your home may be repossessed if you do not keep up repayments on your mortgage&rdquo;); FSCS membership logo; cookie and privacy policy; not optional",
			"&bull; <strong>Service pages</strong> &mdash; one per specialism (residential mortgage, remortgage, buy-to-let, first-time buyer, equity release, protection, pension); each with explainer content, FAQ schema, and &ldquo;book a free consultation&rdquo; CTA; from <strong>$350</strong> per page or <strong>$700</strong> for first four",
			"&bull; <strong>Mortgage calculator</strong> &mdash; repayment vs interest-only calculator; LTV and rate inputs; indicative monthly payment output; strong lead magnet; note: must carry &ldquo;for illustrative purposes only&rdquo; disclaimer; from <strong>$350</strong>",
			"&bull; <strong>Free consultation booking</strong> &mdash; Calendly or custom WP booking; phone, video, or in-office option; pre-qualifying questions (purchase / remortgage / BTL, approximate value, deposit); from <strong>$250</strong>",
			"&bull; <strong>Client portal or secure form</strong> &mdash; document upload (payslips, bank statements, ID); Gravity Forms or Formstack with GDPR consent; replaces unencrypted email; from <strong>$300</strong>",
			"&bull; <strong>Case studies</strong> &mdash; anonymised client scenarios (adverse credit, self-employed, large loan, BTL portfolio); builds confidence for clients with complex situations",
			"&bull; <strong>From $650</strong> compliance-compliant site with service pages and booking; <strong>$1,200+</strong> with calculator, portal, case studies, and full service range",
			'',
			"Are you a mortgage broker, an IFA, or both? Whole-of-market or restricted panel?",
		] );
	}

	// 0d-pre35-c) Therapist / counsellor / psychologist / mental health practice / psychiatry website.
	if ( $has( [ 'therapist website', 'counsellor website', 'counselor website', 'psychologist website', 'psychotherapist website', 'mental health website', 'cbt website', 'cognitive behavioural therapy website', 'anxiety therapist website', 'depression therapist website', 'trauma therapist website', 'bereavement counsellor website', 'relationship therapist website', 'couples therapy website', 'eating disorder therapist website', 'adhd therapist website', 'private psychiatry website', 'psychiatric clinic website' ] ) ) {
		return $nl( [
			"Therapy and counselling websites must convey warmth, safety, and professional credibility simultaneously &mdash; here&rsquo;s how to build one that converts enquiries:",
			'',
			"&bull; <strong>Design language</strong> &mdash; calm palette (soft neutrals, muted greens, warm blues); photography of the therapist in their actual consulting room (not stock); no clinical or medical imagery; no aggressive CTAs; generous whitespace; accessibility is non-negotiable (18px+ body text, high contrast, keyboard navigation)",
			"&bull; <strong>Speciality pages</strong> &mdash; one per presenting issue (anxiety, depression, trauma / PTSD, OCD, eating disorders, bereavement, relationship difficulties, self-esteem, addiction); each targets its own keyword cluster; from <strong>$300</strong> per page",
			"&bull; <strong>Therapist profile</strong> &mdash; BACP / UKCP / BPS accreditation; modality (CBT, EMDR, psychodynamic, person-centred, integrative, ACT); years of experience; supervised hours; professional indemnity insurance; membership number; building trust is everything",
			"&bull; <strong>Online session booking</strong> &mdash; Calendly or Jane App; session type (video / phone / in-person); 50-minute or 80-minute slot; weekly or fortnightly; first session fee is often lower; Stripe payment; from <strong>$250</strong>",
			"&bull; <strong>Therapy process page</strong> &mdash; &ldquo;what happens in the first session&rdquo; walkthrough; reduces fear of the unknown; most under-used conversion tool in therapy sites",
			"&bull; <strong>Fees and insurance</strong> &mdash; transparent pricing (typical private therapy &pound;60&ndash;&pound;150 per session); BUPA / AXA / Cigna / WPA insurance panel membership if applicable; EAP work",
			"&bull; <strong>Crisis resources</strong> &mdash; Samaritans, Crisis Text Line, MIND; required both ethically and practically; position these clearly without alarming general enquirers",
			"&bull; <strong>From $500</strong> profile and booking; <strong>$1,000+</strong> with speciality pages, fee schedule, therapy process guide, and insurance information",
			'',
			"One therapist or a group practice? Face-to-face, online, or both? Any specialist modality or presenting issue focus?",
		] );
	}

	// 0d-pre34-a) Hotel / boutique hotel / guesthouse / B&B / self-catering / apart-hotel website.
	if ( $has( [ 'hotel website', 'boutique hotel website', 'guesthouse website', 'bed and breakfast website', 'b&b website', 'self-catering website', 'holiday cottage website', 'holiday let website', 'apart-hotel website', 'serviced apartment website', 'lodge website', 'inn website', 'hotel booking website', 'accommodation website', 'hostel website', 'hotel direct booking' ] ) ) {
		return $nl( [
			"Hotel and accommodation websites have one overriding goal: capture direct bookings and reduce dependence on Booking.com and Airbnb commission (typically 15&ndash;25%). Here&rsquo;s how:",
			'',
			"&bull; <strong>Direct booking engine</strong> &mdash; Checkfront, Beds24, Lodgify, or WP Hotel Booking plugin; real-time availability calendar; rate per room type; discount for direct (&ldquo;best rate guaranteed&rdquo; badge); from <strong>$500</strong>",
			"&bull; <strong>Room pages</strong> &mdash; one per room type; full-screen photo gallery; bed configuration, capacity, view, floor; en-suite vs shared facilities; pet-friendly or accessible room flags; from <strong>$350</strong>",
			"&bull; <strong>Packages and offers</strong> &mdash; seasonal packages (romantic break, family half-term, Christmas); WooCommerce or booking plugin add-ons (breakfast, spa treatment, flowers, champagne); from <strong>$300</strong>",
			"&bull; <strong>Local area guide</strong> &mdash; attractions, restaurants, walks, public transport within 10 miles; strengthens &ldquo;[town] hotel&rdquo; SEO; positions the property as a local expert not just a bed",
			"&bull; <strong>Events and weddings</strong> &mdash; if the property hosts private events: enquiry form with preferred dates, guest count, catering needs; links through to the full wedding page if applicable",
			"&bull; <strong>Review integration</strong> &mdash; TripAdvisor badge; Google rating pull; Booking.com score if positive; Trustpilot; social proof above the fold",
			"&bull; <strong>Channel manager note</strong> &mdash; syncing availability across Booking.com, Airbnb, Expedia with zero double-booking requires a channel manager (Lodgify, Cloudbeds, SiteMinder); I integrate with your chosen platform",
			"&bull; <strong>From $600</strong> with booking engine and room pages; <strong>$1,300+</strong> with packages, area guide, events, and channel manager integration",
			'',
			"How many rooms or properties, and do you want to move guests from OTA platforms to direct bookings?",
		] );
	}

	// 0d-pre34-b) Day spa / luxury spa / wellness retreat / thermal baths / spa hotel page.
	if ( $has( [ 'day spa website', 'luxury spa website', 'spa website', 'wellness retreat website', 'spa retreat website', 'spa hotel website', 'thermal spa website', 'spa resort website', 'wellness centre website', 'holistic wellness website', 'spa day website', 'spa break website', 'spa treatments website', 'massage therapy website', 'hydrotherapy website', 'flotation therapy website', 'cryotherapy website' ] ) ) {
		return $nl( [
			"Day spa and wellness retreat websites need to evoke the experience before the visitor even books &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Treatment menu</strong> &mdash; one page per treatment category (massages, facials, body wraps, hydrotherapy, thermal circuit, holistic); duration, price, contraindications; booking CTA on each; from <strong>$400</strong>",
			"&bull; <strong>Online booking</strong> &mdash; treatment type, therapist preference (gender option if relevant), date and time slot, add-ons (upgrade to 90 min, aromatherapy oil selection, herbal tea on arrival); Stripe deposit; confirmation email with pre-treatment advice; from <strong>$400</strong>",
			"&bull; <strong>Spa day and break packages</strong> &mdash; half-day, full-day, overnight; &ldquo;Build your own spa day&rdquo; selector (treatment selection + lunch option + access level); from <strong>$350</strong>",
			"&bull; <strong>Gift vouchers</strong> &mdash; monetary value or treatment-specific; email delivery; WooCommerce or dedicated gift voucher plugin; popular for Christmas, Mother&rsquo;s Day, birthdays; from <strong>$250</strong>",
			"&bull; <strong>Membership</strong> &mdash; monthly wellness membership (spa access + treatment allowance + member rate); WooCommerce Subscriptions; cancel anytime; from <strong>$350</strong>",
			"&bull; <strong>Photography and video</strong> &mdash; serene imagery of treatment rooms, thermal pools, relaxation lounges; 30&ndash;60 second ambient video hero; professional photography budget = highest-ROI spend for a spa",
			"&bull; <strong>Wellness blog</strong> &mdash; skincare tips, seasonal treatments, therapist interviews; email capture with &ldquo;seasonal wellness guide&rdquo; lead magnet",
			"&bull; <strong>From $600</strong> treatment menu and booking; <strong>$1,200+</strong> with packages, gift vouchers, membership, and wellness blog",
			'',
			"Day spa, destination retreat, or a spa hotel adding a dedicated spa page?",
		] );
	}

	// 0d-pre34-c) Personal trainer / PT / fitness coach / nutritionist / online coaching website.
	if ( $has( [ 'personal trainer website', 'personal training website', 'fitness coach website', 'pt website', 'fitness coaching website', 'online coaching website', 'strength coach website', 'conditioning coach website', 'sports coach website', 'nutritionist website', 'nutrition coach website', 'dietitian website', 'weight loss coach website', 'body transformation website', 'online personal trainer website', 'health coach website' ] ) ) {
		return $nl( [
			"Personal trainer and fitness coach websites are lead-generation tools first and foremost &mdash; here&rsquo;s what converts:",
			'',
			"&bull; <strong>Transformation gallery</strong> &mdash; before-and-after client results with consent; weight, body composition, or strength benchmarks; most powerful trust signal for fitness audiences; includes client quote and timeline",
			"&bull; <strong>Coaching packages</strong> &mdash; tiered offers (12-week transformation, 6-week starter, monthly rolling); what&rsquo;s included (sessions per week, nutrition plan, WhatsApp check-ins, app access); Stripe payment; from <strong>$300</strong>",
			"&bull; <strong>Online booking</strong> &mdash; free 20-min discovery call (Calendly); 1-to-1 session booking with session credit system; from <strong>$250</strong>",
			"&bull; <strong>Client app integration</strong> &mdash; Trainerize, My PT Hub, or TrueCoach embed or link; workout and nutrition programme delivery via app; reduces manual WhatsApp admin; from <strong>$150</strong> integration page",
			"&bull; <strong>Specialist niche pages</strong> &mdash; one per audience (pre/postnatal, over-50s strength, sport-specific conditioning, weight loss, powerlifting, marathon prep); each targets its own keyword cluster; from <strong>$200</strong> per page",
			"&bull; <strong>Credentials and regulation</strong> &mdash; REPs (UK) or NASM/ACE (US) registration; Level 3 or 4 PT qualification; first aid certificate; insurance provider (e.g. Insure4Sport); regulated nutrition advice disclaimer if nutritionist",
			"&bull; <strong>Freebie lead magnet</strong> &mdash; PDF workout plan, meal prep guide, or 7-day challenge; Mailchimp or ConvertKit email sequence after download",
			"&bull; <strong>From $400</strong> PT site with packages and booking; <strong>$800+</strong> with niche pages, client app integration, and lead magnet sequence",
			'',
			"In-person, online, or hybrid coaching? Any specialist niche (pre/postnatal, sport-specific, over-50s)?",
		] );
	}

	// 0d-pre33-a) Art gallery / commercial gallery / museum / cultural venue / exhibition space.
	if ( $has( [ 'art gallery website', 'commercial gallery website', 'museum website', 'gallery website', 'contemporary art website', 'fine art gallery website', 'art dealer website', 'art consultant website', 'exhibition website', 'cultural venue website', 'sculpture gallery website', 'print gallery website', 'art fair website', 'public gallery website', 'photography gallery website' ] ) ) {
		return $nl( [
			"Art gallery and museum websites need to present work with the same care the gallery brings to its physical space &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Artist and collection pages</strong> &mdash; one CPT for artists (bio, CV, selected shows); one CPT for works (medium, dimensions, year, edition, price on enquiry vs listed); filterable by medium, year, artist; from <strong>$500</strong>",
			"&bull; <strong>Exhibition archive</strong> &mdash; past, current, and upcoming shows; opening date and private view time; press release PDF; install shots gallery; from <strong>$300</strong>",
			"&bull; <strong>Enquiry and purchase flow</strong> &mdash; &ldquo;Enquire about this work&rdquo; form per artwork; price on request vs listed price toggle; collector enquiry routes to gallery director; from <strong>$250</strong>",
			"&bull; <strong>Online viewing room</strong> &mdash; private OVR for art fair week or collector previews; password-protected page with full artwork grid, zoom, enquiry; from <strong>$350</strong>",
			"&bull; <strong>Art fair presence</strong> &mdash; dedicated landing page per fair (Frieze, Art Basel, TEFAF, The Armory); booth number, participating artists, install preview; activates SEO around fair week",
			"&bull; <strong>Press and publications</strong> &mdash; reviews, catalogue PDFs, editorial coverage; signals institutional credibility to collectors and curators",
			"&bull; <strong>Newsletter / collector list</strong> &mdash; Mailchimp or FluentCRM; segmented by interest (photography, painting, sculpture); private view invitations; from <strong>$200</strong> set-up",
			"&bull; <strong>From $600</strong> gallery site with artist and exhibition CPTs; <strong>$1,200+</strong> with OVR, art fair pages, and collector segmentation",
			'',
			"Is this a commercial gallery representing artists, a public or non-profit museum, or an artist&rsquo;s own studio website?",
		] );
	}

	// 0d-pre33-b) Brewery / craft beer / distillery / cidery / winery website.
	if ( $has( [ 'brewery website', 'craft brewery website', 'microbrewery website', 'craft beer website', 'distillery website', 'craft distillery website', 'gin distillery website', 'whisky distillery website', 'rum distillery website', 'vodka distillery website', 'winery website', 'vineyard website', 'cidery website', 'meadery website', 'taproom website', 'tasting room website' ] ) ) {
		return $nl( [
			"Brewery, distillery, and winery websites serve three audiences at once: trade buyers, direct consumers, and visitors to the taproom or cellar door &mdash; here&rsquo;s how that scopes:",
			'',
			"&bull; <strong>Product range</strong> &mdash; WooCommerce; one product per SKU (style, ABV, volume, tasting notes, food pairing, allergen info); filterable by type; case discounts; from <strong>$500</strong>",
			"&bull; <strong>Age gate</strong> &mdash; full-screen overlay on first visit; date-of-birth entry or 18+ / 21+ confirmation click; stores consent in a session cookie; from <strong>$150</strong>",
			"&bull; <strong>Taproom and tours</strong> &mdash; brewery tour or tasting session booking; Calendly or custom WP; session capacity; Stripe deposit; from <strong>$300</strong>",
			"&bull; <strong>Trade enquiry</strong> &mdash; separate form for on-trade (pubs, restaurants, bars) and off-trade (retailers, wholesalers); minimum order, delivery area, price list PDF download; from <strong>$200</strong>",
			"&bull; <strong>Story and process</strong> &mdash; founders page; equipment photography; grain-to-glass or field-to-bottle narrative; key differentiator vs large producers; essential for premium positioning",
			"&bull; <strong>Stockist locator</strong> &mdash; embedded Google Map + searchable list of stockists; updated by the client; from <strong>$250</strong>",
			"&bull; <strong>Subscription / mixed case club</strong> &mdash; WooCommerce Subscriptions; monthly or quarterly; member early access to new releases; from <strong>$350</strong>",
			"&bull; <strong>Compliance</strong> &mdash; Drinkaware / Drink Aware NI logo (UK); Drinkwise (Australia); standard disclaimer &ldquo;please drink responsibly&rdquo;; no targeting under-18s",
			"&bull; <strong>From $550</strong> product range with age gate; <strong>$1,200+</strong> with taproom booking, trade enquiry, and subscription club",
			'',
			"Is the primary revenue online direct-to-consumer, trade wholesale, or cellar-door visitors?",
		] );
	}

	// 0d-pre33-c) Driving school / driver training / DVSA / advanced driving / fleet training.
	if ( $has( [ 'driving school website', 'driving instructor website', 'driver training website', 'driving lessons website', 'driving tuition website', 'intensive driving course website', 'pass plus website', 'advanced driving website', 'fleet driver training website', 'fleet training website', 'automatic driving lessons website', 'motorway lessons website', 'dvsa website', 'driving test website', 'young driver website' ] ) ) {
		return $nl( [
			"Driving school websites are conversion machines &mdash; the visitor already knows what they want, so the site just needs to close the booking quickly:",
			'',
			"&bull; <strong>Online lesson booking</strong> &mdash; lesson type (manual / automatic / intensive / Pass Plus / motorway); duration (1h, 1.5h, 2h); pick-up area (postcode); preferred instructor if multi-instructor; Stripe upfront payment or card-on-file; from <strong>$350</strong>",
			"&bull; <strong>Instructor profiles</strong> &mdash; photo, DVSA ADI badge number, experience years, areas covered, availability indicator; confidence signal for anxious learners and parents",
			"&bull; <strong>Intensive course packages</strong> &mdash; page per package (20h / 30h / 40h crash course); includes estimated pass timeline, what&rsquo;s included, test booking guidance; Stripe single payment; from <strong>$250</strong>",
			"&bull; <strong>Theory test prep</strong> &mdash; links to official DVSA resources; embedded DVSA hazard perception practice or third-party widget; blog posts by topic (rules of the road, road signs quiz)",
			"&bull; <strong>Pass rates and reviews</strong> &mdash; first-time pass rate prominently displayed (stat + source); Google Reviews widget; Trustpilot badge if applicable; highest-converting trust signal",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce gift card plugin; popular for birthdays and Christmas; email delivery; from <strong>$200</strong>",
			"&bull; <strong>Area SEO pages</strong> &mdash; one per town or postcode: &ldquo;driving lessons [town]&rdquo;, &ldquo;driving instructor [postcode area]&rdquo;; Local Business schema; from <strong>$80</strong> per page",
			"&bull; <strong>From $450</strong> single instructor with booking and area pages; <strong>$900+</strong> multi-instructor with intensive packages, gift vouchers, and area SEO",
			'',
			"Single instructor or a school with multiple instructors? Is intensive / crash-course booking a priority?",
		] );
	}

	// 0d-pre32-a) Interior design studio / decorator / staging / soft furnishings website.
	if ( $has( [ 'interior design studio website', 'interior design website', 'interior designer website', 'interior decorator website', 'home staging website', 'soft furnishings website', 'interior stylist website', 'interior consultancy website', 'home decor website', 'interior renovation website', 'space planning website', 'kitchen designer website', 'bathroom designer website' ] ) ) {
		return $nl( [
			"Interior design studio websites are a portfolio-first sales tool &mdash; here&rsquo;s how to build one that converts enquiries:",
			'',
			"&bull; <strong>Project portfolio</strong> &mdash; CPT with categories (residential / commercial / hospitality / staging); hero photo full-bleed per project; before-and-after sliders; room type tags (kitchen, living, bedroom, bathroom); client location; from <strong>$500</strong>",
			"&bull; <strong>Virtual mood board / style quiz</strong> &mdash; Typeform or Gravity Forms quiz (style preference: Scandi / mid-century / maximalist / industrial / coastal / Japandi); outputs a &ldquo;your style profile&rdquo; summary + recommended services; lead capture before result; from <strong>$350</strong>",
			"&bull; <strong>Consultation booking</strong> &mdash; in-home vs virtual option; duration (30-min discovery call, 2-hour room consultation, full-day staging); Stripe deposit; from <strong>$300</strong>",
			"&bull; <strong>Press &amp; features</strong> &mdash; logos of editorial coverage (House Beautiful, Elle D&eacute;cor, Homes &amp; Gardens, Livingetc); strong social proof for premium clients",
			"&bull; <strong>Trade account notice</strong> &mdash; access to trade pricing at Romo, GP &amp; J Baker, Sanderson, Colefax; signals sourcing expertise to discerning clients",
			"&bull; <strong>Services page</strong> &mdash; full interior design (concept to install), consultancy (room by room), e-design (digital only), home staging; clear scope and process timeline for each; manages expectations before enquiry",
			"&bull; <strong>From $600</strong> portfolio and consultation site; <strong>$1,100+</strong> with style quiz, press page, and e-design packages",
			'',
			"Residential, commercial, or a mix? Do you offer e-design for remote clients?",
		] );
	}

	// 0d-pre32-b) Accountancy firm / chartered accountant / tax adviser / bookkeeper website.
	// Note: fires after 0d-pre26-b (general accountant site) for more specific sub-specialities.
	if ( $has( [ 'tax adviser website', 'tax advisor website', 'tax consultant website', 'tax planning website', 'corporation tax website', 'vat specialist website', 'r&d tax credits website', 'r&d tax relief website', 'capital gains website', 'inheritance tax website', 'tax investigation website', 'wealth management website', 'personal tax website', 'self assessment website', 'bookkeeping website', 'bookkeeper website', 'management accounts website', 'payroll website', 'payroll bureau website' ] ) ) {
		return $nl( [
			"Tax advisory and specialist accountancy websites need to establish authority quickly and drive enquiry &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Speciality service pages</strong> &mdash; one per service (R&amp;D Tax Credits, Capital Gains Tax planning, Inheritance Tax, VAT specialist, Tax Investigation Defence, Corporation Tax); each targets its own high-intent keyword; schema FAQ on each page; from <strong>$350</strong> per page or <strong>$600</strong> for first three",
			"&bull; <strong>Tax calculator tools</strong> &mdash; CGT calculator (property gain vs shares), dividend vs salary optimiser, R&amp;D credits estimator; interactive tools are strong lead magnets and linkable assets; from <strong>$400</strong> per tool",
			"&bull; <strong>Regulated credentials</strong> &mdash; ICAEW / ACCA / CIOT / ATT practising certificate; FCA-authorised statement if investment advice is given; trust signals in footer and About page",
			"&bull; <strong>Case study format</strong> &mdash; sector + challenge + outcome (e.g. &ldquo;Tech startup recovered &pound;120k R&amp;D relief in year one&rdquo;); no client names needed &mdash; sector and result suffice; from <strong>$200</strong> per case study",
			"&bull; <strong>Budget/Autumn Statement commentary</strong> &mdash; fast-turnaround blog posts after each Budget; signals active expertise; generates backlinks from local media",
			"&bull; <strong>Secure document exchange</strong> &mdash; FuseBase or SmartVault integration; client uploads tax docs; avoids unencrypted email attachments; from <strong>$250</strong> add-on",
			"&bull; <strong>From $550</strong> with three speciality pages and case studies; <strong>$1,100+</strong> with calculators, secure portal, and full service range",
			'',
			"Which tax speciality brings most of your clients &mdash; R&amp;D relief, CGT, IHT, or VAT? And are you targeting businesses or private individuals?",
		] );
	}

	// 0d-pre32-c) Wedding venue / banqueting hall / events venue / manor house website.
	if ( $has( [ 'wedding venue website', 'wedding venue', 'banqueting hall website', 'events venue website', 'manor house website', 'barn venue website', 'marquee venue website', 'wedding barn website', 'exclusive use venue website', 'country house venue website', 'wedding hall website', 'wedding hotel website', 'function suite website', 'reception venue website' ] ) ) {
		return $nl( [
			"Wedding and events venue websites are a high-stakes purchase &mdash; couples spend months researching and the visual experience is everything:",
			'',
			"&bull; <strong>Hero gallery</strong> &mdash; full-screen autoplay (WebP, mobile-optimised); ceremony and reception rooms; grounds and gardens; real wedding photography (not stock); non-negotiable first impression",
			"&bull; <strong>Venue hire packages</strong> &mdash; tiered pricing table (Weekday / Friday / Saturday / Sunday / exclusive use); minimum spend vs hire fee distinction; catering options (in-house, approved suppliers, BYO corkage); seasonal pricing note",
			"&bull; <strong>Virtual tour</strong> &mdash; Matterport 360&deg; embed of ceremony room, main reception room, bridal suite; couples who can&rsquo;t travel to view still book; from <strong>$200</strong> add-on (requires Matterport scan supplied by client)",
			"&bull; <strong>Date availability checker</strong> &mdash; public-facing calendar (manually updated or iCal sync) showing booked and available dates; reduces &ldquo;is [date] free?&rdquo; enquiries; from <strong>$300</strong>",
			"&bull; <strong>Enquiry form</strong> &mdash; preferred date, estimated guest count, ceremony and reception or reception only, daytime or evening; captures key qualifying info; auto-CRM routing to venue co-ordinator",
			"&bull; <strong>Supplier directory</strong> &mdash; approved or preferred suppliers (photographers, florists, bands, DJs, hair and make-up, cake makers, stationery); reciprocal links drive organic traffic",
			"&bull; <strong>Real weddings gallery</strong> &mdash; one page per featured wedding; photo story; couple testimonial; links to photographer and suppliers; SEO goldmine for long-tail searches",
			"&bull; <strong>From $700</strong> gallery, packages, enquiry form; <strong>$1,400+</strong> with virtual tour, date availability, real weddings, and supplier directory",
			'',
			"Is this a standalone wedding venue, a hotel with a function suite, or a marquee-hire business?",
		] );
	}

	// 0d-pre31-a) Tattoo studio / tattoo artist / piercing / body art website.
	if ( $has( [ 'tattoo studio website', 'tattoo artist website', 'tattoo shop website', 'tattoo parlour website', 'tattoo booking website', 'tattoo portfolio website', 'custom tattoo website', 'tattoo flash website', 'piercing website', 'body art website', 'tattoo artist portfolio', 'ink studio website' ] ) ) {
		return $nl( [
			"Tattoo studio websites are all about portfolio, trust, and capturing the booking &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Artist portfolios</strong> &mdash; one profile page per artist; galleries filterable by style (traditional / blackwork / realism / watercolour / fine-line / Japanese / neo-trad / geometric); high-res WebP photos with Lightbox; artist bio and speciality styles; from <strong>$300</strong> per artist or <strong>$500</strong> for multi-artist studio",
			"&bull; <strong>Online booking</strong> &mdash; consultation request form: style reference upload, body placement, rough size, existing tattoo info, preferred artist; Stripe 10&ndash;20% deposit to hold the slot; auto-confirmation email with aftercare PDF attached; from <strong>$350</strong>",
			"&bull; <strong>Flash sale / available designs</strong> &mdash; filterable grid of ready-to-book flash designs with price and size; &ldquo;Claim this design&rdquo; booking form; drives quick bookings in quieter periods",
			"&bull; <strong>Walk-in availability indicator</strong> &mdash; editable banner (&ldquo;Walk-ins welcome today until 5pm&rdquo;) or WP Notification Bar plugin; low effort, high footfall impact",
			"&bull; <strong>Aftercare &amp; FAQ page</strong> &mdash; written + illustrated; reduces aftercare support queries; reinforces professionalism",
			"&bull; <strong>Minimum price policy</strong> &mdash; clearly stated (e.g. &pound;80 minimum); sets expectations and filters tyre-kickers",
			"&bull; <strong>Age verification notice</strong> &mdash; 18+ required; under-18 with parental consent policy if applicable; brief statement in footer and booking form",
			"&bull; <strong>From $500</strong> solo artist; <strong>$1,000+</strong> multi-artist studio with flash shop and deposit booking",
			'',
			"How many artists are in the studio, and is online deposit booking a priority?",
		] );
	}

	// 0d-pre31-b) Veterinary practice / pet clinic / animal hospital website.
	if ( $has( [ 'vet website', 'veterinary website', 'vets website', 'animal hospital website', 'pet clinic website', 'veterinary practice website', 'veterinary surgery website', 'vet clinic website', 'exotic vet website', 'veterinary specialist website', 'animal clinic website', 'pet hospital website', 'vet practice website', 'veterinary nurse website' ] ) ) {
		return $nl( [
			"Veterinary practice websites need to earn client trust immediately and make appointment booking frictionless:",
			'',
			"&bull; <strong>Online appointment booking</strong> &mdash; appointment type (new patient, existing patient, vaccination, emergency triage, dental, nurse consult); species selector (dogs, cats, rabbits, exotics); preferred vet; Calendly or custom WP booking with VetConnect / Animana / ezyVet integration; from <strong>$350</strong>",
			"&bull; <strong>Species and service pages</strong> &mdash; one page per species or service type (canine, feline, rabbit and small animal, equine if applicable, preventive care, dentistry, surgery, diagnostics); each with targeted keywords (&ldquo;rabbit vet [town]&rdquo;); from <strong>$400</strong>",
			"&bull; <strong>Team profiles</strong> &mdash; vet and nurse bios with RCVS credentials, certificates of higher training (CHTs), clinical interests; owners bond with individual vets; essential for retention",
			"&bull; <strong>Repeat prescription request</strong> &mdash; form: patient name, owner details, medication name, quantity; routes to practice management system or email; reduces phone call volume; from <strong>$200</strong>",
			"&bull; <strong>Pet health articles / blog</strong> &mdash; seasonal content (tick season, summer heat, fireworks anxiety, dental month); builds organic search traffic; signals clinical expertise",
			"&bull; <strong>Emergency out-of-hours information</strong> &mdash; 24/7 phone number or named OOH provider prominently displayed above the fold; critical for new clients in distress",
			"&bull; <strong>RCVS Practice Standards</strong> &mdash; accreditation logo and scheme tier (Core / Advanced / Tier 3) displayed; immediate trust signal",
			"&bull; <strong>From $500</strong> small practice; <strong>$1,100+</strong> multi-branch with online booking, prescription request, and full species SEO",
			'',
			"Single or multi-branch practice? Do you see exotics or mainly dogs and cats?",
		] );
	}

	// 0d-pre31-c) Coworking space / serviced offices / hot desking / flexible workspace website.
	if ( $has( [ 'coworking space website', 'coworking website', 'serviced office website', 'hot desking website', 'flexible workspace website', 'shared office website', 'coworking membership website', 'virtual office website', 'business hub website', 'innovation hub website', 'startup hub website', 'managed office website', 'office rental website', 'desk rental website' ] ) ) {
		return $nl( [
			"Coworking and serviced office websites need to showcase community, show real pricing, and close the trial visit or membership sign-up:",
			'',
			"&bull; <strong>Membership tier pages</strong> &mdash; Hot desk / Dedicated desk / Private office / Enterprise suite; per-day, monthly, and annual pricing table; amenities checklist (printing, meeting rooms, phone booths, kitchen, events, parking); from <strong>$400</strong>",
			"&bull; <strong>Space tour / booking</strong> &mdash; Calendly for in-person tours; or virtual tour embed (Matterport 360&deg; walkthrough); showing up is the highest-converting action; from <strong>$250</strong>",
			"&bull; <strong>Meeting room booking</strong> &mdash; hourly room hire (non-members); WP Amelia or Checkfront; capacity and A/V specs; Stripe payment at booking; from <strong>$400</strong>",
			"&bull; <strong>Member portal</strong> &mdash; login; book desks and rooms; view invoices; access community Slack or Discourse link; door code delivery; from <strong>$600</strong>",
			"&bull; <strong>Community page</strong> &mdash; member spotlight grid (photo + company + sector); upcoming events calendar; member blog; humanises the space and drives referrals",
			"&bull; <strong>Virtual office packages</strong> &mdash; registered address, mail handling, call forwarding, meeting room credits; useful for remote companies needing a UK business address; from <strong>$200</strong> add-on page",
			"&bull; <strong>Location SEO</strong> &mdash; &ldquo;coworking space [city]&rdquo; and &ldquo;serviced office [city]&rdquo; keyword clusters; Google Business + Google Maps embed; Local Business + Event schema for member events",
			"&bull; <strong>From $500</strong> brochure with tour booking; <strong>$1,200+</strong> with member portal, room booking, and community features",
			'',
			"Are you a single-location space or a multi-site network, and do members pay per visit or on monthly plans?",
		] );
	}

	// 0d-pre30-a) Optician / optometrist / eyewear / contact lens / ophthalmology practice.
	if ( $has( [ 'optician website', 'optometrist website', 'eyewear website', 'glasses website', 'spectacles website', 'contact lens website', 'optometry website', 'optician practice website', 'eye test booking', 'ophthalmologist website', 'eye care website', 'vision centre website', 'eye clinic website', 'optical practice website' ] ) ) {
		return $nl( [
			"Optician and eyewear websites need to balance clinical trust with a retail shopping experience &mdash; here&rsquo;s what works:",
			'',
			"&bull; <strong>Online eye test booking</strong> &mdash; Calendly or custom WP booking; test type (full eye exam, contact lens check, children&rsquo;s, dry eye, OCT scan); optometrist selection; online pre-appointment questionnaire; email + SMS reminders; from <strong>$300</strong>",
			"&bull; <strong>Frame try-on gallery</strong> &mdash; filterable product grid (shape, colour, brand, gender, price); photo-accurate product images; virtual try-on integration via Ditto or GlassesOn iframe; from <strong>$400</strong>",
			"&bull; <strong>Online shop</strong> &mdash; WooCommerce; lens type upsell at checkout (single vision / varifocal / reading / blue light); prescription upload PDF or JPEG; frame + lens bundle pricing; Stripe + PayPal; from <strong>$600</strong>",
			"&bull; <strong>Prescription recall</strong> &mdash; automated email when a patient&rsquo;s prescription is due for renewal (typically 2 years); Mailchimp or FluentCRM; from <strong>$250</strong>",
			"&bull; <strong>NHS information</strong> &mdash; sight test eligibility checker (under 16, over 60, benefits, diabetes, glaucoma risk); GOS voucher info; accepted private schemes",
			"&bull; <strong>Trust signals</strong> &mdash; GOC registration number in footer; College of Optometrists / ABDO / FODO membership; reviews widget; awards",
			"&bull; <strong>From $450</strong> booking-only site; <strong>$1,100+</strong> with online shop, virtual try-on, and recall emails",
			'',
			"Is the primary goal appointment bookings, online frame and lens sales, or both?",
		] );
	}

	// 0d-pre30-b) Property management / letting agent / HMO management / block management.
	if ( $has( [ 'property management website', 'property manager website', 'hmo management website', 'landlord services website', 'rental management website', 'block management website', 'property management company website', 'residential management website', 'property maintenance website', 'property concierge website', 'estate management website', 'facility management website' ] ) ) {
		return $nl( [
			"Property management websites need to serve two distinct audiences &mdash; landlords looking for a managing agent, and tenants needing maintenance access:",
			'',
			"&bull; <strong>Landlord lead-gen pages</strong> &mdash; one page per service type (HMO management, AST lettings management, block management, holiday let management); percentage fee calculator; enquiry form; from <strong>$400</strong>",
			"&bull; <strong>Tenant portal</strong> &mdash; login-gated (WP user roles); maintenance report with photo upload; rent payment history; key documents (tenancy agreement, EPC, gas safety); from <strong>$500</strong>",
			"&bull; <strong>Maintenance request form</strong> &mdash; issue category (plumbing / electrical / pest / structural / appliance); urgency level; photo upload; auto-routes to assigned contractor or inbox; from <strong>$300</strong>",
			"&bull; <strong>Compliance hub</strong> &mdash; EPC / EICR / gas safety / legionella risk; expiry-date reminder emails; required for HMO licence conditions",
			"&bull; <strong>CMP badge</strong> &mdash; Client Money Protection membership displayed prominently; required under The Tenant Fees Act 2019; immediate trust signal for landlords",
			"&bull; <strong>Portfolio showcase</strong> &mdash; number of properties managed, client testimonials, contractor network page; reassures landlords that maintenance is handled professionally",
			"&bull; <strong>From $500</strong> brochure and maintenance form; <strong>$1,100+</strong> with full tenant portal and compliance hub",
			'',
			"Do you manage AST residential lettings, HMOs, blocks of flats, or commercial property?",
		] );
	}

	// 0d-pre30-c) Language school / tutoring centre / English language teaching / adult education.
	if ( $has( [ 'language school website', 'english language school', 'english teaching website', 'tutoring centre website', 'tutoring website', 'online tutoring website', 'language course website', 'esl website', 'tefl website', 'language academy website', 'adult education website', 'language learning website', 'private tutor website', 'exam tuition website', 'gcse tuition website', 'a level tuition website' ] ) ) {
		return $nl( [
			"Language school and tutoring websites need to convert anxious parents and adult learners fast &mdash; here&rsquo;s the standard build:",
			'',
			"&bull; <strong>Course pages</strong> &mdash; one per level or exam (GCSE English, A-Level Maths, IELTS, Cambridge First, Business English, Conversational Spanish); includes teacher bio, class size, timetable, price, outcomes; from <strong>$350</strong>",
			"&bull; <strong>Online booking and enrolment</strong> &mdash; course type, start date, student level, trial lesson option; Stripe payment at booking; from <strong>$350</strong>",
			"&bull; <strong>Level placement test</strong> &mdash; Typeform or Gravity Forms; 10&ndash;15 questions; auto-calculates A1&ndash;C2 level; triggers email with recommended course; from <strong>$300</strong>",
			"&bull; <strong>Student learning portal</strong> &mdash; login-gated; homework files, lesson recordings, vocabulary lists, progress tracker; from <strong>$500</strong>",
			"&bull; <strong>Teacher profiles</strong> &mdash; photo, qualifications (PGCE, CELTA, native or near-native), specialist subjects, availability; key trust signal for parents booking 1-to-1",
			"&bull; <strong>Accreditation badges</strong> &mdash; British Council inspected, English UK member, Ofsted, Cambridge Exam Centre; display in header or footer",
			"&bull; <strong>Visa information page</strong> &mdash; UKVI-recognised sponsor status, CAS number guidance; important for IELTS and academic English bookings from international students",
			"&bull; <strong>From $450</strong> brochure and enrolment form; <strong>$1,000+</strong> with portal, placement test, and multilingual site via WPML",
			'',
			"Is this a bricks-and-mortar language school, a 1-to-1 tutoring practice, or primarily online teaching?",
		] );
	}

	// 0d-pre29-a) Commercial / studio photography (headshots, product, corporate — not portfolio sites).
	if ( $has( [ 'product photography website', 'commercial photographer', 'headshot photographer', 'corporate photographer', 'studio photography website', 'food photographer', 'fashion photographer website', 'property photographer website', 'real estate photographer', 'drone photography website', 'event photographer website', 'newborn photographer website', 'boudoir photographer', 'photo studio website' ] ) ) {
		return $nl( [
			"Commercial and studio photography sites are lead-generation builds focused on enquiry and booking &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Service speciality pages</strong> &mdash; one page per niche (headshots, product shots, food, drone, events&hellip;); each targeting its own keyword cluster; 3&ndash;5 portfolio examples per page as social proof; from <strong>$350</strong>",
			"&bull; <strong>Online session booking</strong> &mdash; Calendly Pro embed or custom WP booking; session type + duration + studio or location option + Stripe deposit; from <strong>$300</strong>",
			"&bull; <strong>Client gallery delivery</strong> &mdash; password-protected gallery per client using Pic-Time, Pixieset, or ShootProof embed; download with expiry date; favourite selection; from <strong>$150</strong> integration setup",
			"&bull; <strong>E-commerce prints</strong> &mdash; WooCommerce product pages for fine art prints; Printful or local lab fulfilment; from <strong>$400</strong>",
			"&bull; <strong>Commercial licensing page</strong> &mdash; stock-style usage tiers (editorial, commercial, exclusive); enquiry form for multi-image packages; important for product and fashion photographers",
			"&bull; <strong>Before/after retouching slider</strong> &mdash; demonstrates post-production quality; high conversion impact for headshot and portrait photographers",
			"&bull; <strong>From $500</strong> for a speciality photography service site; <strong>$1,000+</strong> with online booking, client delivery portal, and print shop",
			'',
			"What is your primary specialism, and do you need client gallery delivery, online booking, or print sales?",
		] );
	}

	// 0d-pre29-b) Florist / flower shop / wedding flowers / corporate floristry site.
	if ( $has( [ 'florist website', 'flower shop website', 'floristry website', 'wedding florist website', 'flower delivery website', 'florist online shop', 'florist booking', 'corporate floristry', 'event flowers website', 'bouquet website', 'flower subscription website', 'flower arrangement website' ] ) ) {
		return $nl( [
			"Florist websites are conversion-critical &mdash; most orders happen within 48 hours of a calendar event, so speed and clarity matter:",
			'',
			"&bull; <strong>Online shop</strong> &mdash; WooCommerce; product categories (bouquets, seasonal arrangements, condolence, corporate); photo-first grid; delivery date selector at checkout; Stripe + PayPal; from <strong>$500</strong>",
			"&bull; <strong>Delivery zones</strong> &mdash; postcode-based delivery fee calculator at cart; local same-day vs next-day vs collection options; flat rate or tiered by distance; from <strong>$250</strong>",
			"&bull; <strong>Subscription / flower club</strong> &mdash; WooCommerce Subscriptions; weekly or monthly bouquet; pause/cancel self-service; gifting option (send to a different address); from <strong>$300</strong>",
			"&bull; <strong>Wedding / event consultation</strong> &mdash; multi-step enquiry form: date &rarr; venue &rarr; style inspiration &rarr; approximate budget; Pinterest board or image upload for mood; feeds email or Dubsado",
			"&bull; <strong>Seasonal availability notice</strong> &mdash; banner or modal announcing peak-period cutoffs (Valentine&rsquo;s Day, Mother&rsquo;s Day, Christmas Eve); urgency drives conversions",
			"&bull; <strong>Google Business + local SEO</strong> &mdash; &ldquo;florist near me&rdquo; and &ldquo;flower delivery [town]&rdquo; keyword clusters; Local Business schema; Google Shopping feed for product listings",
			"&bull; <strong>From $550</strong> for an online shop with delivery zones; <strong>$1,000+</strong> with subscription flower club, wedding enquiry, and Google Shopping",
			'',
			"Is the primary business walk-in retail, online delivery, weddings and events, or a combination?",
		] );
	}

	// 0d-pre29-c) Pharmacy / chemist / health products / online dispensary site.
	if ( $has( [ 'pharmacy website', 'chemist website', 'dispensary website', 'online pharmacy', 'health products website', 'compounding pharmacy', 'pharmacy online shop', 'independent pharmacy website', 'prescription service website', 'health supplement website', 'wellbeing shop website' ] ) ) {
		return $nl( [
			"Pharmacy and health-product sites have strict regulatory requirements &mdash; here&rsquo;s what&rsquo;s achievable and what&rsquo;s not:",
			'',
			"&bull; <strong>What I can build</strong> &mdash; OTC (over-the-counter) product e-commerce with WooCommerce; appointment booking for health checks, flu jabs, travel clinics; service pages; NHS contractor info; prescription collection notification system",
			"&bull; <strong>Online prescription sales</strong> &mdash; <em>POM (prescription-only medicines) cannot be sold directly via an e-commerce site</em> without a registered online pharmacy (GPhC registration in UK; DEA in US); I integrate with your existing licensed dispensing workflow, I do not build the dispensing system itself",
			"&bull; <strong>OTC / health product shop</strong> &mdash; WooCommerce with age-verification popup for restricted products; stock sync via CSV or WooCommerce REST API import; from <strong>$500</strong>",
			"&bull; <strong>Appointment booking</strong> &mdash; flu jab, travel vaccines, blood pressure check, weight management, ear wax removal, minor ailments; Calendly or custom WP booking; from <strong>$300</strong>",
			"&bull; <strong>Compliance</strong> &mdash; GPhC logo and registration number in footer; privacy policy; GDPR cookie consent; no medical claims in product descriptions beyond permitted health claims; disclaimer on health content pages",
			"&bull; <strong>Click-and-collect</strong> &mdash; WooCommerce &ldquo;local pickup&rdquo; option at checkout; order ready SMS/email notification; from <strong>$200</strong>",
			"&bull; <strong>From $500</strong> for an independent pharmacy site with services and click-and-collect; <strong>$1,100+</strong> with full OTC e-commerce and appointment booking",
			'',
			"Are you a UK-registered independent pharmacy, a health supplement retailer, or a travel health clinic?",
		] );
	}

	// 0d-pre28-a) Architecture / interior design / landscape design / design studio site.
	if ( $has( [ 'architecture website', 'architect website', 'interior design website', 'interior designer website', 'landscape design website', 'design studio website', 'architectural firm', 'interior architecture', 'space design website', 'architecture portfolio', 'architectural visualization', 'interior decorator website', 'design agency portfolio' ] ) ) {
		return $nl( [
			"Architecture and interior design sites are portfolio-led builds where the visual presentation is everything &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Project portfolio</strong> &mdash; custom post type: full-screen photos, category tags (residential/commercial/hospitality), location, year, area sqm; Masonry or editorial grid layout; filterable; from <strong>$500</strong>",
			"&bull; <strong>Full-screen photography</strong> &mdash; hero with parallax scroll; WebP optimisation; lazy-load with blurhash placeholder; Lightbox with keyboard nav; EXIF and credit line support",
			"&bull; <strong>3D / CGI integration</strong> &mdash; Sketchfab embed for interactive 3D models; Matterport virtual tour embed for completed spaces; before/after slider for renovation projects",
			"&bull; <strong>Services page</strong> &mdash; phased breakdown (concept, design development, planning, construction documentation, project management); fee structure (% of build cost vs fixed fee vs hourly) explained clearly",
			"&bull; <strong>Awards and press</strong> &mdash; dedicated page; RIBA, AIA, Dezeen features, Architizer A+ etc.; schema markup for awards",
			"&bull; <strong>Enquiry form</strong> &mdash; project type, location, area, approximate budget, timeline; routed to the lead designer; from <strong>$200</strong>",
			"&bull; <strong>From $600</strong> for a solo architect or interior designer; <strong>$1,400+</strong> for a studio with 3D integration, Matterport tours, and award pages",
			'',
			"Is this a solo practitioner, a small studio, or a larger firm? And what scale of projects are you showcasing?",
		] );
	}

	// 0d-pre28-b) Sports club / team / association / leisure centre website.
	if ( $has( [ 'sports club website', 'football club website', 'tennis club website', 'cricket club website', 'rugby club website', 'swimming club website', 'athletics club', 'sports team website', 'leisure centre website', 'golf club website', 'cycling club website', 'sports association website', 'martial arts club website', 'gym club website', 'sports coaching website' ] ) ) {
		return $nl( [
			"Sports club and team websites are community-first builds &mdash; here&rsquo;s what&rsquo;s typically included:",
			'',
			"&bull; <strong>Membership signup and renewal</strong> &mdash; WooCommerce Subscriptions; annual fee, family rate, junior rate, concessions; PayPal + Stripe; automated renewal reminders; from <strong>$400</strong>",
			"&bull; <strong>Fixtures and results</strong> &mdash; upcoming matches calendar; live score or post-match result entry; league table auto-calculated; export to iCal; The Events Calendar plugin or custom CPT; from <strong>$300</strong>",
			"&bull; <strong>Player / member profiles</strong> &mdash; login-gated; stats dashboard; season history; squad management; from <strong>$400</strong>",
			"&bull; <strong>Volunteer and team management</strong> &mdash; role sign-up forms; availability poll; WhatsApp / email group links; team-sheet PDF download",
			"&bull; <strong>News / match reports</strong> &mdash; standard WP posts; category by team or division; auto-posted to Facebook + Twitter via Zapier; shareable match graphics",
			"&bull; <strong>Sponsorship page</strong> &mdash; tiered packages (kit sponsor, pitch-side board, match sponsor); logo wall; enquiry form; important revenue stream for amateur clubs",
			"&bull; <strong>From $450</strong> for a small club with fixtures + membership; <strong>$1,100+</strong> for a multi-team association with player profiles and league tables",
			'',
			"What sport, how many teams / age groups, and is membership renewal the main priority?",
		] );
	}

	// 0d-pre28-c) Recruitment agency / headhunter / staffing / job board site.
	if ( $has( [ 'recruitment website', 'recruitment agency website', 'staffing website', 'headhunter website', 'job board website', 'jobs website', 'employment agency website', 'talent acquisition website', 'executive search website', 'temp agency website', 'it recruitment website', 'hr recruitment website' ] ) ) {
		return $nl( [
			"Recruitment and staffing agency sites serve two distinct audiences &mdash; candidates and employers &mdash; and need to serve both well:",
			'',
			"&bull; <strong>Job listings board</strong> &mdash; custom post type: role title, location, salary range, job type (perm/contract/temp), sector, skills; filterable search; Apply button (CV upload form); from <strong>$450</strong>",
			"&bull; <strong>CV / resume upload</strong> &mdash; speculative register-your-CV form; file upload (PDF, DOCX); auto-tagged by sector; emails to relevant consultant; Gravity Forms + GDPR consent tick",
			"&bull; <strong>Candidate portal</strong> &mdash; login-gated account; saved jobs; application status; WP user roles; from <strong>$350</strong>",
			"&bull; <strong>Employer / client page</strong> &mdash; post-a-vacancy enquiry form; sector specialisms; client logos; case studies (&ldquo;we placed 47 engineers at [Company]&rdquo;)",
			"&bull; <strong>Indeed / LinkedIn job sync</strong> &mdash; WP Job Manager plugin can export feed to Indeed; LinkedIn via Direct Jobs API (requires LinkedIn partner approval); from <strong>$250</strong> add-on",
			"&bull; <strong>Sector pages</strong> &mdash; one per specialism (Technology, Finance, Healthcare&hellip;); keyword-rich content; targets &ldquo;[sector] recruitment [city]&rdquo;; strong SEO play",
			"&bull; <strong>From $550</strong> for a boutique agency with job board; <strong>$1,200+</strong> for a multi-sector agency with candidate portal, employer area, and Indeed sync",
			'',
			"What sectors do you recruit in, and do you need a live job board or just a lead-capture site?",
		] );
	}

	// 0d-pre27-a) Childcare / nursery / daycare / preschool / after-school site.
	if ( $has( [ 'childcare website', 'nursery website', 'daycare website', 'preschool website', 'after-school website', 'childminder website', 'kindergarten website', 'primary school website', 'school website', 'early years website', 'montessori website', 'childcare centre', 'kids club website', 'creche website' ] ) ) {
		return $nl( [
			"Childcare and nursery websites are trust-critical builds &mdash; here&rsquo;s what&rsquo;s typically included:",
			'',
			"&bull; <strong>Ofsted / regulatory badge</strong> &mdash; inspection rating prominently displayed; current certificate PDF linked; automatically builds trust with prospective parents",
			"&bull; <strong>Enrolment enquiry form</strong> &mdash; child&rsquo;s name and date of birth, required start date, days required, dietary or medical notes; routed to the setting manager by email; from <strong>$250</strong>",
			"&bull; <strong>Waiting list management</strong> &mdash; WPForms waiting list with auto-confirm email and admin dashboard; from <strong>$200</strong>",
			"&bull; <strong>Virtual tour</strong> &mdash; 360&deg; photo gallery or embedded video walkthrough; parents who cannot visit in person often make decisions based on this alone",
			"&bull; <strong>Parent portal link</strong> &mdash; login button linking to your management system (Famly, Kinderly, Tapestry, Brightwheel); authentication handled by the platform",
			"&bull; <strong>Safeguarding page</strong> &mdash; named DSL, safeguarding policy PDF download, GDPR notice; required by Ofsted and EYFS framework",
			"&bull; <strong>Term dates / calendar</strong> &mdash; annual term-date table; holiday club and inset day listing; auto-updated via The Events Calendar plugin",
			"&bull; <strong>From $400</strong> for a solo childminder; <strong>$800+</strong> for a multi-room nursery with waiting list and parent portal link",
			'',
			"How many children do you have capacity for, and are you Ofsted-registered (UK) or state-licensed (US)?",
		] );
	}

	// 0d-pre27-b) Cleaning / home services / trades / maintenance company site.
	if ( $has( [ 'cleaning website', 'cleaning company website', 'home services website', 'tradesman website', 'plumber website', 'electrician website', 'handyman website', 'painter decorator website', 'landscaping website', 'gardening website', 'window cleaning website', 'pest control website', 'maintenance website', 'cleaning business website' ] ) ) {
		return $nl( [
			"Cleaning and home-services sites are conversion-focused builds &mdash; the goal is to turn visitors into booked jobs:",
			'',
			"&bull; <strong>Instant quote form</strong> &mdash; property type, size, service frequency, postcode; calculates a real-time estimate and captures lead email; highest-converting element for cleaning companies; from <strong>$350</strong>",
			"&bull; <strong>Online booking</strong> &mdash; service type + frequency (one-off, weekly, fortnightly) + date/time slot; Stripe upfront payment or card-on-file for recurring; automated SMS + email reminders; from <strong>$400</strong>",
			"&bull; <strong>Before / after gallery</strong> &mdash; filterable by job type; WebP lazy-loaded; social proof that converts fence-sitters",
			"&bull; <strong>Service area pages</strong> &mdash; one optimised page per town or postcode zone; &ldquo;cleaning company [area]&rdquo; keyword targeting; Local Business JSON-LD; Google map embed",
			"&bull; <strong>Google Reviews widget</strong> &mdash; live pull of your Google Business reviews; star rating badge in header; most leads cite reviews as the deciding factor",
			"&bull; <strong>Team / vetting page</strong> &mdash; DBS-checked / police-vetted badges; named operatives with photo; reduces cancellations from trust concerns",
			"&bull; <strong>Franchise / multi-location</strong> &mdash; if you have branches, one subdomain or subfolder per location; all feeding the same booking engine; from <strong>$250</strong> per location page add-on",
			"&bull; <strong>From $450</strong> for a solo trader; <strong>$900+</strong> for a team with online booking, instant quote, and multi-area SEO pages",
			'',
			"Is this domestic cleaning, commercial cleaning, or a trade service? And do you want instant online booking or lead-capture only?",
		] );
	}

	// 0d-pre27-c) Funeral home / memorial / celebrant / bereavement services site.
	if ( $has( [ 'funeral home website', 'funeral director website', 'memorial website', 'celebrant website', 'bereavement website', 'funeral services website', 'cremation website', 'burial services website', 'funeral parlour website', 'death doula website', 'obituary website', 'grief counselling website' ] ) ) {
		return $nl( [
			"Funeral and memorial service sites require a particularly sensitive and calm design approach &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Design language</strong> &mdash; muted palette, generous white space, serif typography, no aggressive CTAs; the site must feel calming and dignified; from <strong>$600</strong>",
			"&bull; <strong>Service pages</strong> &mdash; burial, cremation, direct cremation, celebration of life, repatriation; each with clear pricing and what is included &mdash; price transparency is now required in England and Wales (FCA regulation 2021)",
			"&bull; <strong>Pre-need / pre-planning</strong> &mdash; funeral plan enquiry form; downloadable pre-arrangement guide; links to Funeral Planning Authority or NAFD-registered plan providers",
			"&bull; <strong>Online obituary / tribute page</strong> &mdash; password-protected per family; photo gallery, memory wall (comments), candle lighting; families can share with friends; from <strong>$300</strong>",
			"&bull; <strong>Out-of-hours contact</strong> &mdash; 24/7 phone number prominently displayed; click-to-call; never hidden behind a contact form alone",
			"&bull; <strong>Bereavement resources</strong> &mdash; grief support links (Cruse, Sue Ryder); practical checklist (what to do when someone dies); builds authority and helps bereaved families",
			"&bull; <strong>Accessibility</strong> &mdash; WCAG 2.1 AA mandatory; large body text (18px+); high-contrast mode; print stylesheet for families who print service programmes",
			"&bull; <strong>From $600</strong> for a single-location funeral home; <strong>$1,200+</strong> with tribute pages, pre-need planning, and multi-location",
			'',
			"Is this a funeral home, a celebrant, a memorial artist, or a bereavement counsellor?",
		] );
	}

	// 0d-pre26-a) Car dealership / automotive / vehicle sales site.
	if ( $has( [ 'car dealership website', 'automotive website', 'vehicle dealership', 'car sales website', 'used car dealership', 'new car dealership', 'car lot website', 'auto dealership', 'vehicle sales site', 'car showroom website', 'used cars website', 'motorbike dealership', 'truck dealership' ] ) ) {
		return $nl( [
			"Automotive and car dealership sites are specialised builds &mdash; here&rsquo;s what&rsquo;s typically included:",
			'',
			"&bull; <strong>Vehicle inventory</strong> &mdash; custom post type: VIN, make, model, year, mileage, price, condition (new/used/certified), colour, photos; advanced filter search; staff-editable via WP admin; from <strong>$500</strong>",
			"&bull; <strong>Finance calculator</strong> &mdash; interactive JS widget; deposit, term, APR inputs; estimated monthly payment output; links to your finance partner enquiry form",
			"&bull; <strong>Trade-in enquiry</strong> &mdash; multi-step form: registration/VIN &rarr; mileage &rarr; condition &rarr; contact; email alert to sales team on submission",
			"&bull; <strong>Test drive booking</strong> &mdash; specific vehicle pre-selected; date/time picker; Stripe <strong>&pound;0</strong> pre-auth to reduce no-shows; SMS confirmation; from <strong>$300</strong>",
			"&bull; <strong>Video walkarounds</strong> &mdash; per-vehicle YouTube/Vimeo embed or self-hosted; auto-plays muted on listing page",
			"&bull; <strong>Local SEO</strong> &mdash; &ldquo;[make] dealership [city]&rdquo; keyword clusters; Google Business profile; Vehicle schema markup for rich results in search",
			"&bull; <strong>OEM / franchise note</strong> &mdash; franchise dealers (Ford, VW, BMW approved networks) often need to use an OEM-mandated platform; I build for <em>independent</em> dealers &mdash; no OEM restrictions apply",
			"&bull; <strong>From $700</strong> for an independent used-car lot; <strong>$1,500+</strong> with full finance calculator, trade-in, test-drive booking, and video walkarounds",
			'',
			"How many vehicles in stock, and is this new, used, or specialist inventory?",
		] );
	}

	// 0d-pre26-b) Accountant / bookkeeper / tax professional / CPA / financial advisor.
	if ( $has( [ 'accountant website', 'bookkeeper website', 'accountancy website', 'tax professional', 'cpa website', 'chartered accountant', 'bookkeeping website', 'tax advisor website', 'payroll service website', 'accounting firm website', 'tax return website', 'financial accountant' ] ) ) {
		return $nl( [
			"Accountancy and bookkeeping sites are straightforward builds with a few specific requirements &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Service pages</strong> &mdash; dedicated pages for tax, bookkeeping, payroll, accounts filing, VAT returns, management accounts, R&amp;D tax credits; each page targets its own keyword cluster",
			"&bull; <strong>Client portal link</strong> &mdash; login button linking to Xero, QuickBooks, FreeAgent, TaxDome, or Iris; authentication handled by the platform &mdash; not built in-house",
			"&bull; <strong>Secure document exchange</strong> &mdash; restricted WP media library (client role login) or Dropbox Business folder per client; encrypted upload form; from <strong>$200</strong>",
			"&bull; <strong>Discovery call booking</strong> &mdash; Calendly embed for a free 15-min consultation; highest-converting CTA for accounting practices",
			"&bull; <strong>GDPR / data handling</strong> &mdash; privacy policy, cookie consent, data processing agreement; all forms encrypt in transit; no sensitive financial data stored in WP database",
			"&bull; <strong>Professional body badges</strong> &mdash; ICAEW, ACCA, CIMA, AAT (UK); AICPA, CPA (US); structured in footer + About page; builds instant trust",
			"&bull; <strong>From $450</strong> for a 5-page sole-practitioner site; <strong>$900+</strong> for a firm with multiple service lines and a client portal link",
			'',
			"How many partners or team members, and which accounting software do your clients use?",
		] );
	}

	// 0d-pre26-c) Pet business / grooming studio / veterinary clinic / pet shop.
	if ( $has( [ 'pet business website', 'pet grooming website', 'dog grooming website', 'veterinary website', 'vet website', 'pet shop website', 'pet supplies website', 'pet care website', 'animal shelter website', 'dog training website', 'cattery website', 'kennels website', 'dog walker website', 'pet photography' ] ) ) {
		return $nl( [
			"Pet business websites cover a wide range &mdash; groomers, vets, pet shops, and shelters each have slightly different needs:",
			'',
			"&bull; <strong>Online booking</strong> &mdash; pet type + service + groomer/vet + date/time + Stripe deposit; automated SMS and email reminders; cancel/reschedule self-service; from <strong>$350</strong>",
			"&bull; <strong>Team + service menu</strong> &mdash; groomer/vet bios, certifications, specialisms, direct-book button; service menu with duration, price, and breed-size surcharges",
			"&bull; <strong>Before/after gallery</strong> &mdash; grooming transformations filterable by breed; WebP lazy-loaded; doubles as social proof and SEO content",
			"&bull; <strong>Pet records portal (vet only)</strong> &mdash; login-gated page per client linking to your practice management software (ezyVet, VetsPetPortal, Vet24); authentication handled by the platform, not in-house",
			"&bull; <strong>E-commerce (pet shop)</strong> &mdash; WooCommerce; product categories; weight-based or variable shipping; Stripe + PayPal; subscription auto-ship for food/treats; from <strong>$500</strong>",
			"&bull; <strong>Google Reviews + schema</strong> &mdash; live review widget; Local Business JSON-LD; trust signals are especially critical for pet care where owners are emotionally invested",
			"&bull; <strong>From $400</strong> for a solo grooming studio; <strong>$600</strong> for a vet practice; <strong>$900+</strong> for a pet shop with full e-commerce",
			'',
			"Is this a grooming studio, vet practice, pet shop, or a combination?",
		] );
	}

	// 0d-pre25-a) Beauty salon / spa / hair / nail / aesthetics studio site.
	if ( $has( [ 'beauty salon', 'hair salon', 'nail salon', 'spa website', 'beauty website', 'aesthetics website', 'barbershop website', 'salon website', 'lash studio', 'brow studio', 'beauty studio', 'makeup artist website', 'tattoo studio website', 'massage therapist website' ] ) ) {
		return $nl( [
			"Beauty and salon sites are high-converting when built around online booking &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Online booking</strong> &mdash; service menu + provider selection + date/time picker + Stripe deposit or full payment; Fresha or Vagaro embed (<strong>$0/mo</strong> on their free plan) or custom-built from <strong>$350</strong>; automated SMS + email reminders reduce no-shows by ~30%",
			"&bull; <strong>Service menu</strong> &mdash; grouped by category (hair, nails, skincare&hellip;), duration + price; staff-editable via WP admin; from <strong>$300</strong>",
			"&bull; <strong>Team / stylist pages</strong> &mdash; photo, bio, specialities, Instagram embed, direct-book button; each stylist can have their own landing page for SEO",
			"&bull; <strong>Gallery</strong> &mdash; before/after slider; filterable by treatment; lazy-loaded WebP; linked to Instagram feed via Smash Balloon",
			"&bull; <strong>Gift vouchers</strong> &mdash; WooCommerce gift cards; purchasable online, redeemable at checkout; from <strong>$200</strong>",
			"&bull; <strong>Local SEO</strong> &mdash; &ldquo;[treatment] near me&rdquo; and &ldquo;[treatment] [town]&rdquo; keyword clusters; Google Business Reviews widget; Local Business schema",
			"&bull; <strong>From $450</strong> for a solo stylist site; <strong>$900+</strong> for a multi-stylist salon with online booking and gift vouchers",
			'',
			"How many team members, and do you already use a booking system like Fresha or Vagaro?",
		] );
	}

	// 0d-pre25-b) Music / band / artist / DJ / musician website.
	if ( $has( [ 'music website', 'band website', 'musician website', 'dj website', 'artist website music', 'album website', 'tour dates', 'gig listings', 'music streaming', 'soundcloud', 'spotify artist', 'music portfolio', 'record label website', 'singer website' ] ) ) {
		return $nl( [
			"Music and artist sites need to look as good as they sound &mdash; here&rsquo;s a typical build:",
			'',
			"&bull; <strong>Hero + embedded player</strong> &mdash; full-screen video or parallax photo; Spotify/Apple Music/SoundCloud embed; WaveSurfer.js custom waveform player; from <strong>$350</strong>",
			"&bull; <strong>Tour dates / gig listings</strong> &mdash; Bandsintown or Songkick widget for auto-updating tour dates; or custom CPT with Eventbrite ticket link; from <strong>$200</strong>",
			"&bull; <strong>Music / release archive</strong> &mdash; album discography with artwork, track listings, streaming links (Spotify, Apple Music, YouTube Music, Deezer); auto-updated via MusicBrainz or manual entry",
			"&bull; <strong>Video</strong> &mdash; YouTube / Vimeo channel embed; music video archive with lazy-load",
			"&bull; <strong>Merch store</strong> &mdash; WooCommerce + Printful for apparel, vinyl, posters; no upfront stock cost; from <strong>$400</strong>",
			"&bull; <strong>Press kit (EPK)</strong> &mdash; password-protected page with bio, hi-res photos, logo, rider, past press; shareable URL for promoters",
			"&bull; <strong>Mailing list</strong> &mdash; Mailchimp or Klaviyo signup with lead-magnet (free download); best channel for direct fan communication",
			"&bull; <strong>From $500</strong> for an artist bio/EPK site; <strong>$1,000+</strong> for full discography, merch store, and mailing list",
			'',
			"Solo artist, band, or label? And is the priority booking gigs, selling music, or growing fans?",
		] );
	}

	// 0d-pre25-c) Travel / tourism / tour operator / activity booking site.
	if ( $has( [ 'travel website', 'tourism website', 'tour operator', 'activity booking', 'travel agency', 'holiday website', 'tour booking', 'travel blog', 'excursion website', 'adventure tourism', 'eco tourism', 'safari website', 'boat charter', 'travel booking site' ] ) ) {
		return $nl( [
			"Travel and tour operator sites are built around booking and trust &mdash; here&rsquo;s what&rsquo;s typical:",
			'',
			"&bull; <strong>Tour / activity listings</strong> &mdash; custom CPT: overview, itinerary accordion, included/excluded list, difficulty rating, photo gallery, pricing tiers; staff-editable; from <strong>$450</strong>",
			"&bull; <strong>Online booking + payment</strong> &mdash; date picker, group size selector, Stripe or PayPal; deposit or full payment at time of booking; from <strong>$400</strong>",
			"&bull; <strong>FareHarbor / Bokun / Rezdy integration</strong> &mdash; if you already use a channel manager for OTA distribution (Viator, GetYourGuide), I embed their widget; keeps availability in sync automatically",
			"&bull; <strong>Destination pages</strong> &mdash; one page per destination; local tips, map, weather widget, testimonials; targets &ldquo;[activity] in [destination]&rdquo; keywords; strong SEO play for tour operators",
			"&bull; <strong>TripAdvisor / Google Reviews widget</strong> &mdash; live review pull; trust signals are critical for travel; from <strong>$150</strong>",
			"&bull; <strong>Travel blog / itinerary content</strong> &mdash; long-form SEO content strategy; targets informational keywords to drive organic top-of-funnel traffic",
			"&bull; <strong>Multi-currency + multi-language</strong> &mdash; WooCommerce currency switcher + WPML for international operators; from <strong>$350</strong>",
			"&bull; <strong>From $650</strong> for a 5-tour operator site; <strong>$1,500+</strong> for a full booking engine with channel-manager integration",
			'',
			"How many tours or activities, and do you already use a booking platform like FareHarbor or Bokun?",
		] );
	}

	// 0d-pre24-a) Gym / fitness studio / personal trainer / yoga site.
	if ( $has( [ 'gym website', 'fitness website', 'personal trainer', 'yoga studio', 'crossfit', 'pilates site', 'bootcamp', 'personal training', 'fitness class', 'gym membership', 'fitness studio', 'spin class', 'martial arts website' ] ) ) {
		return $nl( [
			"Fitness and gym websites are a very common build &mdash; here&rsquo;s what&rsquo;s typically included:",
			'',
			"&bull; <strong>Class schedule</strong> &mdash; weekly timetable; filter by instructor or class type; live availability; MindBody or Glofox embed available as a <strong>$200</strong> add-on; or built custom; from <strong>$350</strong>",
			"&bull; <strong>Online booking</strong> &mdash; class or PT session booking with Stripe deposit; cancel/reschedule self-service; automated SMS and email reminders via Twilio or Mailchimp",
			"&bull; <strong>Membership tiers</strong> &mdash; monthly and annual subscriptions via WooCommerce Subscriptions or MemberPress; member portal login; free-trial logic; from <strong>$400</strong>",
			"&bull; <strong>Trainer profiles</strong> &mdash; photo, bio, specialisms, video intro, and a direct-book button on each profile",
			"&bull; <strong>Video on demand (VOD)</strong> &mdash; locked-behind-membership workout library; Bunny.net or Vimeo hosting; watch progress tracking; from <strong>$600</strong>",
			"&bull; <strong>Local SEO</strong> &mdash; Local Business JSON-LD; Google Business integration; map pack visibility; class schema markup for rich results",
			"&bull; <strong>From $500</strong> for a studio site with booking; <strong>$1,200+</strong> for a full member portal with VOD library",
			'',
			"Is the primary offer group classes, one-to-one PT, online coaching, or a combination?",
		] );
	}

	// 0d-pre24-b) Real estate / estate agent / property listing site.
	if ( $has( [ 'real estate website', 'property listing', 'estate agent website', 'realtor website', 'homes for sale', 'property website', 'letting agent', 'idx integration', 'mls integration', 'real estate site', 'property for sale', 'estate agency', 'letting website', 'property search' ] ) ) {
		return $nl( [
			"Real estate and estate agent websites have a specific stack &mdash; here&rsquo;s how it typically comes together:",
			'',
			"&bull; <strong>Property listings</strong> &mdash; custom post type with photo gallery, floor plan, map pin, price, beds/baths/sq ft; fully staff-editable via WP admin; from <strong>$450</strong>",
			"&bull; <strong>MLS / IDX integration</strong> &mdash; US realtors can embed a live MLS feed via IDX Broker or Showcase IDX (<strong>$60&ndash;$80/mo</strong> third-party licence); pulls live active listings automatically without manual data entry",
			"&bull; <strong>Advanced property search</strong> &mdash; filter by location, price range, bedrooms, bathrooms, property type, new-build flag; saved-search + email alert on new match; from <strong>$300</strong>",
			"&bull; <strong>Mortgage calculator</strong> &mdash; interactive JS widget; monthly payment, total cost, deposit breakdown; no third-party fees",
			"&bull; <strong>Valuation / appraisal lead form</strong> &mdash; multi-step form feeding your CRM or email; typically the highest-converting page on any agent site",
			"&bull; <strong>Local area pages</strong> &mdash; one optimised page per suburb or neighbourhood; Local Business schema; Google Maps embed; targets &ldquo;[area] estate agent&rdquo; keyword cluster",
			"&bull; <strong>From $600</strong> for a 10-listing agency site; <strong>$1,500+</strong> for IDX feed integration with saved-search email alerts",
			'',
			"Are you a solo agent, a team, or an agency? And are you in the US (MLS/IDX) or UK/EU?",
		] );
	}

	// 0d-pre24-c) Wedding / event planning / venue / coordinator site.
	if ( $has( [ 'wedding website', 'wedding planner', 'event planning website', 'venue website', 'event coordinator', 'bridal website', 'wedding photography', 'wedding videographer', 'wedding site', 'event venue', 'event management website', 'party planner', 'corporate events website' ] ) ) {
		return $nl( [
			"Wedding and event sites have strong visual expectations &mdash; here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>Hero + gallery</strong> &mdash; full-screen video hero (Bunny.net hosted for speed); filterable photo gallery by wedding style or season; before/after slider; from <strong>$400</strong>",
			"&bull; <strong>Package pages</strong> &mdash; clearly priced tiers with inclusions checklist; FAQ accordion; testimonials with photo; structured data for rich results in Google Search",
			"&bull; <strong>Enquiry / availability form</strong> &mdash; multi-step: date &rarr; event type &rarr; guest count &rarr; budget &rarr; contact; Gravity Forms with conditional logic; feeds CRM, Dubsado, or HoneyBook",
			"&bull; <strong>Online contract + deposit</strong> &mdash; HelloSign e-signature for booking contract; Stripe 30% deposit; triggers automated welcome email sequence",
			"&bull; <strong>Client portal</strong> &mdash; login-gated page with planning checklist, mood board upload, and event timeline; WP user roles; from <strong>$400</strong>",
			"&bull; <strong>Real wedding showcase</strong> &mdash; blog-style posts with rich imagery; Pinterest-optimised Open Graph; strong long-tail SEO (&ldquo;outdoor barn wedding Essex&rdquo; type keywords)",
			"&bull; <strong>From $550</strong> for a 6-page planner or photographer site; <strong>$1,400+</strong> for a venue with online booking, e-sign contract, deposit, and client portal",
			'',
			"Are you a planner, photographer, videographer, or a venue? Do you need online contracts and deposits?",
		] );
	}

	// 0d-pre23-a) Nonprofit / charity / NGO / donation site.
	if ( $has( [ 'nonprofit', 'non-profit', 'charity website', 'ngo', 'donation', 'fundraising', 'charity site', 'nonprofit site', 'volunteer', 'donation website', 'crowdfunding', '501c3', 'charitable', 'foundation website', 'giving campaign' ] ) ) {
		return $nl( [
			"Nonprofit and charity websites share the same WordPress stack with a few key additions &mdash; here&rsquo;s what&rsquo;s typical:",
			'',
			"&bull; <strong>Donation system</strong> &mdash; GiveWP plugin (most popular open-source donation plugin); single and recurring donations; Stripe + PayPal gateway; PDF receipt emailed automatically; from <strong>$350</strong> with donation integration",
			"&bull; <strong>Campaign pages</strong> &mdash; fundraising thermometer / progress bar toward a goal; donor wall (optionally anonymised); time-limited campaign countdown",
			"&bull; <strong>Volunteer management</strong> &mdash; signup form with shift selection; volunteer coordinator dashboard; email confirmation + reminder automation",
			"&bull; <strong>Google Ad Grants</strong> &mdash; nonprofits qualify for <strong>$10,000/month</strong> in free Google Ads; I can set up the account, campaigns, and landing pages to unlock this",
			"&bull; <strong>Annual report / impact page</strong> &mdash; data visualisation of impact metrics, financial summary, board of directors; grant-document friendly layout",
			"&bull; <strong>Discounts</strong> &mdash; TechSoup for discounted software; Cloudflare nonprofits; Mailchimp Nonprofit 15% discount",
			'',
			"Is the primary goal donations, volunteer recruitment, event attendance, or all three?",
		] );
	}

	// 0d-pre23-b) Education / eLearning / online course / LMS.
	if ( $has( [ 'online course', 'elearning', ' lms ', 'learning management system', 'course website', 'teach online', 'sell courses', 'membership learning', 'education platform', 'tutoring site', 'student portal', 'school website', 'learning portal', 'course platform', 'learndash', 'tutor lms', 'thinkific', 'teachable alternative', 'online education' ] ) ) {
		return $nl( [
			"eLearning and online course sites are a common build &mdash; here&rsquo;s the plugin decision matrix:",
			'',
			"&bull; <strong>LearnDash</strong> &mdash; most powerful WP LMS; course builder, quizzes, certificates, progress tracking, group management, SCORM 1.2; from <strong>$199/yr</strong> + setup; best for multi-course or corporate training",
			"&bull; <strong>Tutor LMS</strong> &mdash; free core; clean UI; good for creators launching their first course; from <strong>$350</strong> total",
			"&bull; <strong>BuddyBoss</strong> &mdash; LMS + community; social profiles, messaging, groups, and courses in one; great for coaching programmes or cohort-based learning; from <strong>$700</strong>",
			"&bull; <strong>Video hosting</strong> &mdash; Bunny.net (fast, cheap) or Vimeo for course videos; <em>never YouTube</em> (adverts + distracting autoplay on student pages)",
			"&bull; <strong>Checkout</strong> &mdash; WooCommerce for one-time purchase, subscription, or bundle; Stripe + PayPal supported",
			"&bull; <strong>Certificates</strong> &mdash; LearnDash generates PDF certificates on course completion; custom design included",
			"&bull; <strong>From $450</strong> for a single-course site; <strong>$1,200+</strong> for a full multi-course LMS with memberships and community",
			'',
			"How many courses, and is access one-time purchase, subscription, or membership-based?",
		] );
	}

	// 0d-pre23-c) Healthcare / medical practice / telemedicine / therapist website.
	if ( $has( [ 'healthcare website', 'medical website', 'doctor website', 'clinic website', 'telemedicine', 'telehealth', 'patient portal', 'hipaa', 'medical practice', 'dentist website', 'therapist website', 'mental health site', 'healthcare site', 'gp website', 'psychologist website', 'appointment booking healthcare' ] ) ) {
		return $nl( [
			"Medical and healthcare websites use the same WordPress stack but have specific requirements &mdash; important notes up front:",
			'',
			"&bull; <strong>Appointment booking</strong> &mdash; Calendly embed (HIPAA-compliant plan available) or SimplePractice for therapists; custom WP booking form from <strong>$350</strong>; no PHI (Protected Health Information) stored server-side",
			"&bull; <strong>Telemedicine</strong> &mdash; Doxy.me embed (HIPAA-compliant video); Zoom Healthcare (requires a signed BAA with Zoom); <em>not built in-house</em> &mdash; I connect the platform to your site",
			"&bull; <strong>HIPAA compliance</strong> &mdash; contact forms must not log or store PHI; SSL required; no GA4 on patient-facing pages without consent; I handle the technical layer but your team must verify compliance with a qualified officer",
			"&bull; <strong>Patient portal</strong> &mdash; login page linking to your EHR (Epic, Athenahealth, SimplePractice); authentication handled by those platforms, not built here",
			"&bull; <strong>Medical copy</strong> &mdash; I provide layout and structure; your clinical team writes and approves all medical content; no medical claims written by me",
			"&bull; <strong>ADA / WCAG 2.1 AA</strong> &mdash; essential for healthcare; audited and remediated as standard",
			"&bull; <strong>From $600</strong> for a practice website; <strong>$250 add-on</strong> for a HIPAA form configuration audit",
			'',
			"What type of practice, and do you need online booking, telemedicine video, or a patient-portal link?",
		] );
	}

	// 0d-pre22-a) Photography portfolio / creative portfolio / visual artist site.
	if ( $has( [ 'photography portfolio', 'photographer website', 'photography site', 'photo portfolio', 'portfolio website', 'artist portfolio', 'creative portfolio', 'visual portfolio', 'portfolio gallery', 'photographer site', 'art portfolio', 'creative website', 'freelance portfolio' ] ) ) {
		return $nl( [
			"Photography and creative portfolio sites are a regular build &mdash; here&rsquo;s how it scopes:",
			'',
			"&bull; <strong>Gallery system</strong> &mdash; filterable by category (weddings, portraits, landscapes); lazy-loaded images with WebP conversion; full-screen lightbox with keyboard navigation; from <strong>$350</strong>",
			"&bull; <strong>Performance</strong> &mdash; images are the main content so performance is critical: Imagify/ShortPixel compression, responsive srcset, critical CSS; Core Web Vitals pass",
			"&bull; <strong>Client proofing</strong> &mdash; private password-protected gallery per client; download link with expiry; client selects favourite images; via Envira Gallery or custom build",
			"&bull; <strong>Print shop</strong> &mdash; sell prints via WooCommerce + Printful integration; photographer sets their own markup over Printful base cost",
			"&bull; <strong>Booking system</strong> &mdash; session type selection (wedding/portrait/commercial), date picker, deposit payment via Stripe; contract signing via HelloSign API; from <strong>$400</strong>",
			"&bull; <strong>SEO for photographers</strong> &mdash; location + speciality keywords; image alt text schema; local business JSON-LD; Google Images optimisation",
			'',
			"What&rsquo;s the main goal &mdash; showcase work, book clients, sell prints, or deliver to existing clients?",
		] );
	}

	// 0d-pre22-b) Restaurant / café / food business website.
	if ( $has( [ 'restaurant website', 'cafe website', 'food business site', 'restaurant menu', 'online menu', 'restaurant booking', 'table reservation', 'opentable', 'resy', 'restaurant site', 'food truck', 'bakery website', 'bar website', 'bistro website', 'hospitality website', 'takeaway website' ] ) ) {
		return $nl( [
			"Restaurant and hospitality websites are a core build &mdash; here&rsquo;s what&rsquo;s typically included:",
			'',
			"&bull; <strong>Menu</strong> &mdash; filterable by dietary requirement (vegan, gluten-free, nut-free); PDF version for download; seasonal content easily updated by staff via WordPress admin; from <strong>$300</strong>",
			"&bull; <strong>Table reservations</strong> &mdash; OpenTable or Resy embed (they handle capacity and confirmation); or a custom Gravity Forms booking form with email confirmation + SMS reminder",
			"&bull; <strong>Online ordering</strong> &mdash; WooCommerce or Square Online for click-and-collect / delivery; integrates with Deliverect for multi-channel order management",
			"&bull; <strong>Event / private dining</strong> &mdash; enquiry form for groups, menus, and weddings; deposit collection via Stripe",
			"&bull; <strong>Google Business Profile</strong> &mdash; site structured to pass address, hours, and phone to Google; Local Business JSON-LD; triggers the knowledge panel and map pack",
			"&bull; <strong>Instagram feed</strong> &mdash; live feed embedded in the footer (Smash Balloon plugin); no API token management required",
			'',
			"Is this dine-in, takeaway, delivery, or a mix? And do you need table reservations or just an enquiry form?",
		] );
	}

	// 0d-pre22-c) Law firm / solicitor / professional services firm.
	if ( $has( [ 'law firm website', 'lawyer website', 'solicitor website', 'attorney website', 'legal website', 'law firm site', 'legal services site', 'accountant website', 'professional services site', 'consultant website', 'financial advisor website', 'advisory firm' ] ) ) {
		return $nl( [
			"Professional services firm websites (law, accountancy, consultancy) are a well-understood build &mdash; a few things to note:",
			'',
			"&bull; <strong>What&rsquo;s included</strong> &mdash; practice area pages with SEO-optimised copy, attorney/partner bios, contact + enquiry form, embedded map, client testimonials (anonymised if needed), ADA-compliant design",
			"&bull; <strong>Intake form</strong> &mdash; multi-step form collecting matter type, jurisdiction, timeline; routed to the right team member via conditional email notifications; from <strong>$250</strong>",
			"&bull; <strong>Client portal link</strong> &mdash; link to Clio, MyCase, or PracticePanther from the nav; authentication handled by those platforms (not built in-house)",
			"&bull; <strong>Legal disclaimer</strong> &mdash; standard &ldquo;Not legal advice&rdquo; footer disclaimer added to all pages; no substantive legal content written &mdash; I provide the layout and you supply the copy",
			"&bull; <strong>Compliance</strong> &mdash; GDPR/CCPA cookie consent; no personal data stored server-side beyond form submissions",
			"&bull; <strong>Authority signals</strong> &mdash; bar association membership logos, publication links, awards, case result summaries (redacted) in a structured schema markup",
			"&bull; <strong>From $700</strong> for a professional 6&ndash;8 page firm site",
			'',
			"How many attorneys or partners, which practice areas, and do you have existing copy or do you need copywriting guidance?",
		] );
	}

	// 0d-pre21-a) Podcast / audio production / show notes site.
	if ( $has( [ 'podcast production', 'audio editing', 'show notes', 'podcast website', 'podcast site', 'podcast player', 'podcast hosting', 'audio recording', 'podcast episode', 'podcast rss', 'distribute podcast', 'apple podcasts', 'spotify podcast' ] ) ) {
		return $nl( [
			"Podcast production support and podcast websites are both in scope &mdash; here&rsquo;s the breakdown:",
			'',
			"&bull; <strong>Podcast website</strong> &mdash; episode archive, custom HTML5 player (WaveSurfer.js waveform), show notes, transcript, guest bio, RSS feed valid for Apple Podcasts + Spotify; from <strong>$500</strong>",
			"&bull; <strong>Distribution</strong> &mdash; submit once to Apple Podcasts, Spotify, Amazon Music, iHeartRadio via Buzzsprout or Transistor; not managed in-house but I&rsquo;ll set it up",
			"&bull; <strong>Transcript integration</strong> &mdash; paste Whisper AI or Rev transcript; auto-formats as collapsible show notes; boosts SEO with long-form keyword content per episode",
			"&bull; <strong>Audio production referral</strong> &mdash; sound design, mixing, and mastering is out of my scope but I can connect you with freelance engineers from my network",
			"&bull; <strong>Audiogram generator</strong> &mdash; static waveform clip export for social media (optional; custom build from <strong>$200</strong>)",
			'',
			"Is this a podcast site, a podcast player embedded in an existing site, or both?",
		] );
	}

	// 0d-pre21-b) SaaS onboarding flow / activation / product-led growth.
	if ( $has( [ 'saas onboarding', 'user onboarding', 'onboarding flow', 'onboarding wizard', 'onboarding checklist', 'activation flow', 'product-led growth', 'plg', 'user activation', 'welcome flow', 'new user flow', 'onboarding tour', 'first run experience', 'in-app onboarding' ] ) ) {
		return $nl( [
			"SaaS onboarding and activation flows are a speciality &mdash; here&rsquo;s how it typically scopes:",
			'',
			"&bull; <strong>Welcome wizard</strong> &mdash; multi-step setup (2&ndash;5 steps) collecting user preferences; skippable; progress bar; persisted to user profile; from <strong>$350</strong>",
			"&bull; <strong>Checklist</strong> &mdash; Intercom-style contextual checklist; items checked off as user completes actions; completion triggers upgrade prompt or confetti; from <strong>$250</strong>",
			"&bull; <strong>Guided tour</strong> &mdash; Shepherd.js or custom tooltip overlay highlighting key UI elements sequentially; skippable; once-per-user; from <strong>$200</strong>",
			"&bull; <strong>Empty-state CTAs</strong> &mdash; friendly empty states with a clear first action instead of a blank screen; copy + illustration; included in SaaS build",
			"&bull; <strong>Activation metric</strong> &mdash; define the &lsquo;aha moment&rsquo; (e.g. first project created, first export, first invite); instrument it in GA4 or Mixpanel; from <strong>$150</strong>",
			"&bull; <strong>Email drip on sign-up</strong> &mdash; 3&ndash;5 email welcome sequence triggered by registration; hooks into Mailchimp, Klaviyo, or Postmark; from <strong>$200</strong>",
			'',
			"What&rsquo;s the &lsquo;aha moment&rsquo; for your product &mdash; the first action that makes users understand its value?",
		] );
	}

	// 0d-pre21-c) Multilingual / i18n / translated website.
	if ( $has( [ 'multilingual', 'multi-language', 'translate website', 'website translation', 'i18n', 'internationalization', 'localisation', 'localization', 'french version', 'spanish version', 'german version', 'wpml', 'polylang', 'translatepress', 'rtl support', 'arabic website', 'chinese website' ] ) ) {
		return $nl( [
			"Multilingual websites are fully in scope &mdash; here&rsquo;s the plugin decision matrix:",
			'',
			"&bull; <strong>WPML</strong> &mdash; industry standard; per-language pages; WooCommerce multilingual add-on; from <strong>$99/yr</strong>; recommended for 3+ languages or e-commerce",
			"&bull; <strong>Polylang</strong> &mdash; lighter; free tier covers 2 languages; good for blogs and informational sites",
			"&bull; <strong>TranslatePress</strong> &mdash; visual front-end editor; translate directly on the page; AI auto-translate add-on; from <strong>$89/yr</strong>",
			"&bull; <strong>Translation workflow</strong> &mdash; machine translation (DeepL API) for first draft, then human review; or hand over to a translator with a PO file export",
			"&bull; <strong>RTL support</strong> &mdash; Arabic, Hebrew, Urdu, Farsi; CSS direction:rtl; RTL-compatible theme; tested in Chrome + Firefox",
			"&bull; <strong>SEO</strong> &mdash; hreflang tags for each language; separate slug structure (yourdomain.com/fr/, /de/); Yoast SEO Premium handles this automatically",
			"&bull; <strong>Included cost</strong> &mdash; the plugin + setup + one translated page as proof of concept; content translation is usually billed separately per word",
			'',
			"How many languages, which ones, and is the content already translated or does it need translation services too?",
		] );
	}

	// 0d-pre20-a) API documentation site / developer docs / technical docs.
	if ( $has( [ 'api documentation', 'developer docs', 'technical docs', 'api docs', 'doc site', 'docs website', 'documentation site', 'swagger', 'openapi', 'docusaurus', 'nextra', 'readme.io', 'gitbook', 'developer portal', 'docs portal', 'api reference' ] ) ) {
		return $nl( [
			"Developer and API documentation sites are a common build &mdash; here&rsquo;s the landscape:",
			'',
			"&bull; <strong>Docusaurus</strong> (React + MDX) &mdash; fast, SEO-optimised, versioned docs, GitHub Pages deploy; best for open-source and dev teams; free to host",
			"&bull; <strong>Nextra</strong> (Next.js + MDX) &mdash; full Next.js power + Markdown; custom themes; Vercel deploy; good for teams already in Next.js",
			"&bull; <strong>GitBook</strong> &mdash; hosted, no deploy config, collaborative editing, GitHub sync; <strong>$6.70/user/mo</strong>",
			"&bull; <strong>Readme.io</strong> &mdash; auto-generates reference docs from OpenAPI/Swagger spec; built-in API try-it console; paid",
			"&bull; <strong>OpenAPI/Swagger integration</strong> &mdash; auto-generated reference pages from your .yaml spec; code samples in 10+ languages; included in custom builds",
			"&bull; <strong>WordPress KB</strong> &mdash; KnowledgeBase plugin + custom theme; searchable, client-editable by non-devs; from <strong>$400</strong>",
			'',
			"Who maintains the docs &mdash; devs via Markdown/Git, or non-technical team via CMS? And do you need a live API try-it console?",
		] );
	}

	// 0d-pre20-b) Browser extension / Chrome extension / Firefox add-on.
	if ( $has( [ 'browser extension', 'chrome extension', 'firefox addon', 'browser plugin', 'chrome plugin', 'extension development', 'web extension', 'manifest v3', 'browser addon', 'chrome web store', 'firefox extension', 'edge extension', 'browser add-on' ] ) ) {
		return $nl( [
			"Browser extension development is in scope &mdash; here&rsquo;s the breakdown:",
			'',
			"&bull; <strong>Chrome Extension (MV3)</strong> &mdash; content scripts, service workers, popup UI, context menu, keyboard shortcuts; Chrome Web Store publish (~<strong>$5</strong> one-time developer fee)",
			"&bull; <strong>Firefox Add-on</strong> &mdash; same WebExtensions API; publish to addons.mozilla.org; free",
			"&bull; <strong>Cross-browser (Plasmo)</strong> &mdash; React + TypeScript + HMR; one codebase targets Chrome, Firefox, Edge, and Safari",
			"&bull; <strong>Content scripts</strong> &mdash; inject JS/CSS into any page; DOM read/write; message passing to background service worker; tab communication",
			"&bull; <strong>Storage</strong> &mdash; chrome.storage.sync for cross-device settings; chrome.storage.local for large or offline data",
			"&bull; <strong>Common builds</strong> &mdash; page summariser, price tracker, screenshot/annotation tool, GPT sidebar, custom note-taker, URL logger; from <strong>$500</strong>",
			"&bull; <strong>Review timeline</strong> &mdash; Chrome Web Store review 1&ndash;3 days; MV3 required (MV2 deprecated mid-2025)",
			'',
			"What does the extension do on the page, and does it need a backend or server-side sync?",
		] );
	}

	// 0d-pre20-c) Desktop app / Electron / Tauri / native desktop.
	if ( $has( [ 'desktop app', 'electron', 'tauri', 'desktop application', 'windows app', 'mac app', 'native desktop', 'desktop software', 'cross platform desktop', 'desktop gui', 'nwjs', 'windows desktop app', 'mac desktop app', 'desktop program' ] ) ) {
		return $nl( [
			"Desktop app work is in scope &mdash; here&rsquo;s the decision framework:",
			'',
			"&bull; <strong>Electron</strong> &mdash; Node.js + Chromium; largest ecosystem; VS Code, Slack, and Figma use it; ships on Windows, Mac, and Linux from one codebase; large bundle (~150 MB); from <strong>$2,500</strong>",
			"&bull; <strong>Tauri</strong> &mdash; Rust + system WebView; tiny bundle (~3 MB); faster startup than Electron; same-codebase multi-platform; Rust adds build complexity; from <strong>$2,500</strong>",
			"&bull; <strong>Native Mac (SwiftUI)</strong> &mdash; cleanest macOS integration; Mac App Store listing; best choice for Mac-only; from <strong>$3,500+</strong>",
			"&bull; <strong>Native Windows (.NET / WinUI)</strong> &mdash; Microsoft Store listing; system tray, COM, Win32 APIs; from <strong>$3,500+</strong>",
			"&bull; <strong>PWA (Edge / Chrome)</strong> &mdash; installable on Windows 11 and macOS; no App Store; offline support; included in web builds at no extra cost",
			'',
			"<strong>Electron vs Tauri</strong>: Electron = easier dev + large bundle; Tauri = small + fast + Rust learning curve.",
			'',
			"Windows, Mac, or both? And what&rsquo;s the key feature &mdash; file system access, system tray, native notifications, or hardware integration?",
		] );
	}

	// 0d-pre19-a) Subscription box / recurring physical orders / subscription e-commerce.
	if ( $has( [ 'subscription box', 'subscription service', 'recurring order', 'recurring product', 'monthly box', 'curated box', 'product subscription', 'recurring shipment', 'subscription e-commerce', 'subscription physical', 'monthly subscription', 'box subscription', 'subscribe and save' ] ) ) {
		return $nl( [
			"Physical subscription box e-commerce is a well-scoped build &mdash; here&rsquo;s how it works:",
			'',
			"&bull; <strong>WooCommerce Subscriptions</strong> &mdash; recurring billing via Stripe; free trial periods; pause, cancel, upgrade/downgrade from customer account; from <strong>$350</strong> + WooCommerce Subscriptions licence",
			"&bull; <strong>Billing cycles</strong> &mdash; weekly, monthly, quarterly, annually; prorated upgrades and downgrades handled automatically",
			"&bull; <strong>Box management</strong> &mdash; variant selection per shipment, skip-a-month, gifting, address change from customer portal",
			"&bull; <strong>Fulfilment</strong> &mdash; ShipStation or EasyPost integration for bulk label generation on billing date; tracking number emailed automatically",
			"&bull; <strong>Churn prevention</strong> &mdash; dunning emails on failed payment; automatic retry schedule; account reactivation flow",
			"&bull; <strong>Analytics</strong> &mdash; subscriber MRR, churn rate, and LTV visible in WooCommerce dashboard",
			'',
			"What&rsquo;s in the box, how often does it ship, and do customers customise their selection?",
		] );
	}

	// 0d-pre19-b) Event ticketing / registration / virtual events.
	if ( $has( [ 'event ticketing', 'ticket sales', 'event registration', 'ticket website', 'sell tickets', 'virtual event', 'online event', 'event site', 'webinar site', 'conference website', 'summit website', 'ticket booking', 'event management site', 'events calendar', 'event page' ] ) ) {
		return $nl( [
			"Event and ticketing sites are a regular build &mdash; here&rsquo;s the scope:",
			'',
			"&bull; <strong>Ticketing</strong> &mdash; WooCommerce free/paid ticket products; QR code on confirmation email; printable + mobile-friendly PDF ticket",
			"&bull; <strong>Registration</strong> &mdash; Gravity Forms with conditional logic, time-slot selection, capacity limits per session",
			"&bull; <strong>Virtual events</strong> &mdash; Zoom or Google Meet embed; gated livestream; member-only access to recordings after the event",
			"&bull; <strong>Multi-event calendar</strong> &mdash; The Events Calendar plugin; filterable by date, location, and type; iCal / Google Calendar export",
			"&bull; <strong>Pricing</strong> &mdash; early-bird pricing (time-limited), promo codes, group discounts, deposit + balance payments",
			"&bull; <strong>Refund policy</strong> &mdash; Stripe refund rules enforced automatically (e.g. full refund &gt;30 days, 50% within 7 days, no refund same-week)",
			"&bull; <strong>From $500</strong> single-event page &mdash; <strong>$800+</strong> multi-event calendar with registration and ticketing",
			'',
			"Single event or recurring calendar, in-person or virtual, and are tickets free or paid?",
		] );
	}

	// 0d-pre19-c) Mobile app vs PWA vs React Native vs Flutter.
	if ( $has( [ 'mobile app', 'native app', 'ios app', 'android app', 'react native', 'flutter', ' pwa ', 'progressive web app', 'app development', 'mobile app vs website', 'build an app', 'ios development', 'android development', 'hybrid app', 'cross platform app', 'native vs pwa', 'mobile development', 'app vs website' ] ) ) {
		return $nl( [
			"Here&rsquo;s the decision framework for mobile app vs PWA &mdash; it comes down to App Store presence and hardware APIs:",
			'',
			"&bull; <strong>PWA (Progressive Web App)</strong> &mdash; runs in the browser, installable from Safari/Chrome, offline support, push notifications; no App Store listing; limited iOS support for some device APIs; <em>included in Next.js and WP builds at no extra cost</em>",
			"&bull; <strong>React Native</strong> &mdash; shared iOS+Android codebase; near-native performance; App Store + Play Store listing; OTA updates without app store review; from <strong>$2,500+</strong>",
			"&bull; <strong>Flutter</strong> &mdash; beautiful custom UI; single Dart codebase for iOS, Android, and web; fast on older devices; from <strong>$2,500+</strong>",
			"&bull; <strong>Native Swift / Kotlin</strong> &mdash; maximum hardware access (Bluetooth, NFC, ARKit, CarPlay); highest performance animations; from <strong>$5,000+ per platform</strong> &mdash; referred to specialist partners",
			'',
			"<strong>Choose PWA when</strong>: content site, dashboard, SaaS tool with no hardware API needs.",
			"<strong>Choose React Native / Flutter when</strong>: you need an App Store listing, push-to-App-Store, Bluetooth/NFC/camera/background location, or in-app purchases.",
			'',
			"Do you need an App Store listing, and which device hardware features are required?",
		] );
	}

	// 0d-pre18-a) WordPress Multisite / multi-site network / multi-tenant.
	if ( $has( [ 'wordpress multisite', 'multisite', 'multi-site', 'multiple websites', 'site network', 'subdomain network', 'subdirectory sites', 'wpmu', 'multi-tenant wordpress', 'manage multiple sites', 'mainwp', 'managewp', 'network of sites', 'wp network', 'subsite' ] ) ) {
		return $nl( [
			"WordPress Multisite (network mode) is fully in scope &mdash; here&rsquo;s how it scopes:",
			'',
			"&bull; <strong>Network setup</strong> &mdash; subdomain mode (site1.yourdomain.com) or subdirectory mode (yourdomain.com/site1); wildcard DNS required for subdomains; from <strong>$150</strong> setup",
			"&bull; <strong>Shared codebase</strong> &mdash; one theme, one plugin install serves all sites; per-site Customizer overrides for colours, logo, content",
			"&bull; <strong>User management</strong> &mdash; super admin vs. site admin roles; single SSO login across all sites; role sync add-on",
			"&bull; <strong>Content sharing</strong> &mdash; shared media library, cross-site taxonomy, content mirroring between subsites",
			"&bull; <strong>MainWP dashboard</strong> &mdash; manage plugin/theme/core updates across 100s of sites from one screen; automated update schedules",
			"&bull; <strong>Hosting requirement</strong> &mdash; Kinsta and WP Engine support Multisite natively; standard shared hosting usually cannot",
			'',
			"How many sites, are they same or different designs, and is user management centralised?",
		] );
	}

	// 0d-pre18-b) Digital products / downloadable files / Easy Digital Downloads.
	if ( $has( [ 'digital product', 'downloadable product', 'sell ebook', 'sell pdf', 'sell template', 'sell digital download', 'sell presets', 'sell fonts', 'digital downloads', 'easy digital downloads', ' edd ', 'sell files', 'digital goods', 'sell software', 'digital storefront', 'gumroad alternative', 'digital store', 'sell downloads', 'license key' ] ) ) {
		return $nl( [
			"Selling digital products is a common build &mdash; here&rsquo;s how it scopes:",
			'',
			"&bull; <strong>Easy Digital Downloads (EDD)</strong> &mdash; purpose-built for files; per-download pricing, purchase logs, per-email download limits, software licence key add-on; from <strong>$350</strong> standalone",
			"&bull; <strong>WooCommerce + virtual/downloadable</strong> &mdash; good when you also sell physical goods; same cart, same customer account",
			"&bull; <strong>File security</strong> &mdash; download links expire (signed S3 URL or WP nonce); files stored outside web root; single-use or count-limited links",
			"&bull; <strong>Delivery</strong> &mdash; email confirmation with download link; customer dashboard for re-downloads; PDF personalisation (name + order ID stamp) add-on",
			"&bull; <strong>No transaction tax</strong> &mdash; Stripe direct (no 9% Gumroad fee); Paddle for EU VAT handling",
			"&bull; <strong>Membership gating</strong> &mdash; lock downloads behind a paywall via MemberPress or Restrict Content Pro",
			'',
			"Files only, or a mix of digital + physical? And do you need software licence keys?",
		] );
	}

	// 0d-pre18-c) Print-on-demand / merchandise / Printful / Printify.
	if ( $has( [ 'print on demand', 'printful', 'printify', 'pod store', 'custom merch', 'merchandise store', 'sell t-shirts', 'sell hoodies', 'sell mugs', 'sell merchandise', 'dropship merch', 'custom apparel', 'branded merchandise', 'merch store', 'sell merch', 'print and ship', 'white label merch' ] ) ) {
		return $nl( [
			"Print-on-demand merch stores are a clean, no-inventory build &mdash; here&rsquo;s how it works:",
			'',
			"&bull; <strong>Printful or Printify</strong> &mdash; WooCommerce plugin syncs products, variants, and pricing automatically; no stock held; fulfilled and shipped directly to the customer",
			"&bull; <strong>Product range</strong> &mdash; t-shirts, hoodies, mugs, phone cases, posters, tote bags, hats &mdash; all managed from one dashboard",
			"&bull; <strong>Mockup generation</strong> &mdash; Printful&rsquo;s mockup generator produces product images automatically; no photo shoot needed",
			"&bull; <strong>Custom branding</strong> &mdash; inside label, packing slip branding, thank-you card insert (Printful Pro)",
			"&bull; <strong>Order flow</strong> &mdash; customer orders &rarr; WooCommerce webhook &rarr; Printful fulfils &rarr; tracking number emailed automatically",
			"&bull; <strong>Margin</strong> &mdash; set your own retail price; Printful deducts its base cost before payout; typically 30&ndash;40% margin",
			"&bull; <strong>From $600</strong> for a basic merch store with custom domain, SSL, WooCommerce, and Printful integration",
			'',
			"What products, and is this a merch add-on to an existing site or a standalone store?",
		] );
	}

	// 0d-pre17-a) AI image generation / Midjourney / DALL-E / Stable Diffusion.
	if ( $has( [ 'ai image', 'ai generated image', 'midjourney', 'dall-e', 'stable diffusion', 'ai art', 'image generation', 'text to image', 'ai artwork', 'generative art', 'comfyui', 'image ai', 'ai illustration', 'generate images', 'ai images for site' ] ) ) {
		return $nl( [
			"AI image generation is part of the Photo &amp; Asset workflow &mdash; here&rsquo;s what&rsquo;s available:",
			'',
			"&bull; <strong>DALL&bull;E 3 / Midjourney</strong> &mdash; hero images, illustrations, backgrounds, icon sets; consistent style from a single style prompt; from <strong>$50</strong> for an asset session",
			"&bull; <strong>Stable Diffusion / ComfyUI</strong> &mdash; fine-tuned models, LoRA-trained characters, local inference; for brand-consistent repeatable generation",
			"&bull; <strong>Upscaling</strong> &mdash; Real-ESRGAN for print-quality (300 dpi) versions of any AI image",
			"&bull; <strong>Optimization</strong> &mdash; all AI images converted to WebP, lazy-loaded, correct alt text for accessibility and SEO",
			"&bull; <strong>Not suitable for</strong>: faces (accuracy issues), logos (use vector), product photography (use real photos)",
			'',
			"Is this for hero art, site illustrations, icon set, backgrounds, or a full asset library?",
		] );
	}

	// 0d-pre17-b) Email marketing / newsletter / Mailchimp / ConvertKit.
	if ( $has( [ 'email marketing', 'newsletter', 'mailchimp', 'convertkit', 'drip email', 'klaviyo', 'email list', 'email campaign', 'email automation', 'email sequence', 'email list building', 'opt-in form', 'lead magnet', 'email funnel', 'email signup', 'mailing list', 'kitcom' ] ) ) {
		return $nl( [
			"Email marketing integration is fully supported &mdash; here&rsquo;s how it scopes:",
			'',
			"&bull; <strong>Opt-in form</strong> &mdash; custom-styled, connected to Mailchimp / ConvertKit / Klaviyo / Drip via API; lead magnet delivery in the confirmation email",
			"&bull; <strong>Pop-up / exit-intent</strong> &mdash; timed or scroll-triggered modal; GDPR double-opt-in checkbox included",
			"&bull; <strong>Form &rarr; CRM</strong> &mdash; form data &rarr; HubSpot, Airtable, or Notion via webhook",
			"&bull; <strong>Welcome sequence</strong> &mdash; 5-email welcome series built in your ESP; copywriting extra; from <strong>$150 add-on</strong>",
			"&bull; <strong>ESP recommendations</strong> &mdash; Mailchimp (free to 500 contacts) &middot; Kit / ConvertKit (creators) &middot; Klaviyo (e-commerce / Shopify) &middot; Drip (SaaS)",
			'',
			"Do you already have an ESP, and what&rsquo;s the lead magnet or incentive for signing up?",
		] );
	}

	// 0d-pre17-c) Custom ML / AI model training / fine-tuning / RAG.
	if ( $has( [ 'train a model', 'custom ai model', 'machine learning', 'ml model', 'computer vision', 'image recognition', 'natural language processing', ' nlp ', 'fine-tune', 'fine tuning', 'hugging face', 'tensorflow', 'pytorch', 'ai model', 'train gpt', 'rag pipeline', 'retrieval augmented', 'vector database', 'embeddings', 'semantic search', 'custom llm' ] ) ) {
		return $nl( [
			"Custom ML and AI model work is in scope &mdash; here&rsquo;s the current capability:",
			'',
			"&bull; <strong>LLM fine-tuning</strong> &mdash; GPT-3.5 / GPT-4o fine-tuning for domain-specific tone; open models (Llama 3, Mistral) via QLoRA on GPU; from <strong>$500</strong>",
			"&bull; <strong>Embeddings + RAG</strong> &mdash; vector DB (Pinecone, Weaviate, Chroma); semantic search over your documents; Q&amp;A bot over a corpus; from <strong>$565</strong>",
			"&bull; <strong>Computer vision</strong> &mdash; YOLOv8 / ResNet object detection &amp; classification; custom training on a labeled dataset; OpenCV preprocessing",
			"&bull; <strong>Image classification</strong> &mdash; MobileNet / EfficientNet transfer learning; accurate from ~500 labeled images",
			"&bull; <strong>Custom AI concierge</strong> &mdash; no fine-tuning needed; specialized system prompt + guardrails + chat widget; from <strong>$190</strong>",
			"&bull; <strong>Deployment</strong> &mdash; FastAPI endpoint or Vercel Edge Function; auth-protected; streaming responses",
			'',
			"What&rsquo;s the problem to solve &mdash; classification, generation, semantic search, or extraction?",
		] );
	}

	// 0d-pre16-a) Stripe / subscription billing / SaaS pricing model.
	if ( $has( [ 'stripe integration', 'stripe setup', 'payment gateway', 'stripe connect', 'subscription billing', 'recurring billing', 'billing portal', 'stripe customer portal', 'usage-based pricing', 'freemium model', 'tiered pricing setup', 'metered billing', 'stripe webhooks', 'stripe subscription', 'payment integration', 'accept payments' ] ) ) {
		return $nl( [
			"Stripe is the standard for SaaS billing &mdash; here&rsquo;s how it scopes:",
			'',
			"&bull; <strong>Products &amp; prices</strong> &mdash; monthly / annual subscriptions, one-time payments, usage-based metering &mdash; all set up in Stripe + synced to WP user meta",
			"&bull; <strong>Customer portal</strong> &mdash; Stripe&rsquo;s hosted portal for plan changes, cancellation, invoice history &mdash; zero custom UI needed",
			"&bull; <strong>Webhooks</strong> &mdash; payment.succeeded, subscription.updated, subscription.deleted &rarr; update WP roles/features automatically",
			"&bull; <strong>Coupon codes &amp; trials</strong> &mdash; referral discounts, % / fixed coupons, 7/14/30-day trials &mdash; all native to Stripe",
			"&bull; <strong>Alternatives</strong> &mdash; PayPal, Square, LemonSqueezy, or Paddle if EU tax compliance matters",
			'',
			"Covered in the SaaS / App build ($750+). What plan structure &mdash; monthly / annual / per-seat / usage-based?",
		] );
	}

	// 0d-pre16-b) UX research / usability testing / design audit.
	if ( $has( [ 'ux research', 'usability testing', 'user testing', 'user research', 'user interviews', 'usability audit', 'heuristic evaluation', 'accessibility audit', 'a11y audit', 'design audit', 'ux audit', 'ui ux review', 'ux review', 'design review', 'wcag' ] ) ) {
		return $nl( [
			"UX and accessibility services are in scope as a project phase or standalone:",
			'',
			"&bull; <strong>Heuristic evaluation</strong> &mdash; 10 Nielsen heuristics; report with severity ratings (critical / major / minor); from <strong>$100</strong>",
			"&bull; <strong>Usability testing</strong> &mdash; task-based remote sessions; 5-user rule; findings report + prioritised fix list",
			"&bull; <strong>Accessibility audit (WCAG 2.1 AA)</strong> &mdash; axe DevTools automated scan + manual screen reader check (NVDA / VoiceOver); from <strong>$150</strong>",
			"&bull; <strong>Design audit</strong> &mdash; UX patterns, contrast ratios, typography scale, spacing consistency; Figma annotations",
			"&bull; <strong>Prototype testing</strong> &mdash; Figma interactive prototype &rarr; test before any code is written",
			'',
			"Is the goal to validate a new design, audit an existing site, or improve conversion?",
		] );
	}

	// 0d-pre16-c) Cybersecurity / security audit / pen test / hardening.
	if ( $has( [ 'security audit', 'penetration testing', 'pen test', 'pentest', 'vulnerability scan', 'owasp', 'xss protection', 'sql injection', 'website security', 'security review', 'csrf protection', 'secure code review', 'security hardening', 'brute force protection', 'malware scan', 'hack prevention' ] ) ) {
		return $nl( [
			"Security is built into every Photon Bounce build &mdash; and formal audits are available:",
			'',
			"&bull; <strong>OWASP Top 10 code review</strong> &mdash; XSS, SQL injection, CSRF, broken auth, insecure direct object refs; report with severity ratings",
			"&bull; <strong>WordPress hardening</strong> &mdash; brute-force protection, XML-RPC disabled, file permission lockdown, hidden login URL, login 2FA",
			"&bull; <strong>Dependency scan</strong> &mdash; npm audit + WP plugin vulnerability check; replace flagged packages",
			"&bull; <strong>Security headers</strong> &mdash; HTTPS / HSTS, CSP, X-Frame-Options, Permissions-Policy, Referrer-Policy; tested with securityheaders.com",
			"&bull; <strong>Full pen test (OWASP ZAP + manual)</strong> &mdash; from <strong>$300</strong> standalone; from <strong>$150</strong> added to any build",
			'',
			"Note: for PCI DSS, SOC 2, or HIPAA compliance you need a certified QSA / auditor &mdash; I can refer one.",
			"What&rsquo;s the concern &mdash; an existing site with a possible breach, or a new build you want locked down before launch?",
		] );
	}

	// 0d-pre15-a) Blockchain / Web3 / smart contracts / NFTs / dApps.
	if ( $has( [ 'blockchain', 'web3', 'smart contract', ' nft ', 'nft marketplace', 'defi', 'cryptocurrency', 'crypto wallet', 'dapp', 'decentralized', 'solidity', 'ethereum', 'polygon', ' token ', 'web3 integration', 'metamask', 'walletconnect', 'ipfs', 'solana', 'base chain', 'layer 2' ] ) ) {
		return $nl( [
			"Web3 and blockchain projects are in scope &mdash; here&rsquo;s the current capability:",
			'',
			"&bull; <strong>Smart contracts</strong> &mdash; Solidity (ERC-20, ERC-721, ERC-1155); Hardhat dev environment; strongly recommend an audit before mainnet deploy",
			"&bull; <strong>dApp frontend</strong> &mdash; ethers.js / wagmi + Next.js; MetaMask + WalletConnect integration; ENS name resolution",
			"&bull; <strong>NFT marketplace</strong> &mdash; custom OpenSea-style or simple storefront; IPFS / Pinata for metadata and media",
			"&bull; <strong>Token gating</strong> &mdash; restrict content or features to wallet holders; WP plugin or custom API middleware",
			"&bull; <strong>Crypto payments</strong> &mdash; Coinbase Commerce or direct wallet address on any WP / Next.js build",
			'',
			"Quotes from <strong>$750+</strong>; smart contract audit adds cost but is strongly recommended.",
			"What chain &mdash; Ethereum, Polygon, Solana, Base, or something else?",
		] );
	}

	// 0d-pre15-b) No-code / low-code tools / Webflow / Bubble / Framer.
	if ( $has( [ 'no-code', 'low-code', 'nocode', 'webflow', 'bubble.io', 'framer', 'squarespace', ' wix ', 'no code tool', 'should i use webflow', 'webflow vs wordpress', 'webflow vs custom', 'framer vs', 'vs squarespace', 'vs webflow', 'bubble vs', 'vs wordpress', 'which platform' ] ) ) {
		return $nl( [
			"Photon Bounce builds custom (WordPress or Next.js / React), not Webflow, Wix, or Squarespace &mdash; but here&rsquo;s an honest comparison:",
			'',
			"&bull; <strong>Webflow / Framer</strong> &mdash; great for design-heavy marketing sites with a designer who knows them; limited custom functionality; harder to hand off to devs",
			"&bull; <strong>Squarespace / Wix</strong> &mdash; fastest for solo businesses with simple needs; very limited custom logic; platform lock-in",
			"&bull; <strong>WordPress</strong> &mdash; best for CMS-heavy sites with client-editable content; huge plugin ecosystem; Photon Bounce&rsquo;s primary stack",
			"&bull; <strong>Next.js / React</strong> &mdash; best for apps, dashboards, interactive tools, real user accounts, or high-performance sites",
			'',
			"Custom is worth it when you need: custom functionality, real performance, no platform ceiling, or you&rsquo;re scaling beyond what a template handles.",
			"What does the site need to do that a template can&rsquo;t handle?",
		] );
	}

	// 0d-pre15-c) Data visualization / charts / dashboards / analytics.
	if ( $has( [ 'data visualization', 'data viz', ' chart ', 'graphs', 'dashboard analytics', 'interactive chart', 'd3.js', 'chart.js', 'recharts', 'data dashboard', 'reporting dashboard', 'analytics dashboard', 'charting library', 'visualize data', 'echarts', 'leaflet', 'mapbox' ] ) ) {
		return $nl( [
			"Data visualization is fully in scope &mdash; here&rsquo;s the library decision matrix:",
			'',
			"&bull; <strong>Chart.js</strong> &mdash; fastest for common types (line, bar, pie, doughnut, radar); lightweight; included free in SaaS / App builds",
			"&bull; <strong>D3.js</strong> &mdash; maximum flexibility; custom layouts, force-directed graphs, geographic maps; complex builds from <strong>$400</strong>",
			"&bull; <strong>Recharts / Nivo</strong> &mdash; React-native; great for Next.js dashboards; clean declarative API",
			"&bull; <strong>Leaflet.js / Mapbox GL</strong> &mdash; geographic data + choropleth maps; custom tile layers; works offline with cached tiles",
			"&bull; <strong>Apache ECharts</strong> &mdash; heavy-duty; large data volumes; excellent for financial or operational dashboards",
			"&bull; <strong>Live data</strong> &mdash; pulls from REST API or WebSocket; updates in real time without page reload",
			'',
			"How much data, how often does it update, and does it need interactive filtering?",
		] );
	}

	// 0d-pre14-a) Web scraping / workflow automation / API integration.
	if ( $has( [ 'web scraping', 'data scraping', 'scrape website', 'scrape data', 'automate workflow', 'workflow automation', 'zapier alternative', 'make.com', 'api integration', 'connect apis', 'data pipeline', 'webhook integration', 'third-party api', 'integrate crm', 'integrate stripe', 'integrate api', 'n8n', 'automation script' ] ) ) {
		return $nl( [
			"Automation and API integration are fully in scope &mdash; here&rsquo;s how it typically breaks down:",
			'',
			"&bull; <strong>Web scraping</strong> &mdash; Python (Beautiful Soup / Playwright) or Node.js (Puppeteer) depending on JavaScript rendering and anti-bot measures; from <strong>$150</strong>",
			"&bull; <strong>Workflow automation</strong> &mdash; Zapier, Make.com, or self-hosted n8n; trigger &rarr; action chains; custom webhook endpoint build included",
			"&bull; <strong>API integration</strong> &mdash; REST or GraphQL; OAuth 2, API key, and JWT auth patterns; any public or private API",
			"&bull; <strong>Data pipeline</strong> &mdash; ETL script (fetch &rarr; transform &rarr; store); cron-triggered or webhook-triggered; outputs to DB / CSV / Google Sheets / Airtable",
			"&bull; <strong>Common APIs bundled at no extra cost</strong> &mdash; Stripe, WooCommerce webhooks, Mailchimp, SendGrid, Google Sheets, HubSpot",
			'',
			"What are you trying to connect or automate?",
		] );
	}

	// 0d-pre14-b) Social media strategy / content calendar.
	if ( $has( [ 'social media strategy', 'content calendar', 'social media content', 'instagram strategy', 'tiktok strategy', 'linkedin content', 'twitter strategy', 'social media marketing', 'content plan', 'posting schedule', 'social content plan', 'content creation strategy', 'social media help' ] ) ) {
		return $nl( [
			"Social media strategy isn&rsquo;t the primary service, but Photon Bounce&rsquo;s work plugs into it:",
			'',
			"&bull; <strong>Brand identity</strong> &mdash; every social profile designed in one kit: headers, profile image, story templates, post grid look; part of the Brand service",
			"&bull; <strong>Content repurposing engine</strong> &mdash; SEO articles automatically formatted into social-ready excerpt cards (Open Graph + schema); saves manual work",
			"&bull; <strong>AI content toolkit</strong> &mdash; custom Claude or GPT system prompt tuned to your brand voice; drop in a topic, get a post in your style",
			"&bull; <strong>What&rsquo;s not covered</strong> &mdash; scheduling, community management, or posting &mdash; for that, use Buffer or Hootsuite",
			'',
			"Are you looking for the brand design layer, the content strategy layer, or a writing toolkit?",
		] );
	}

	// 0d-pre14-c) Podcast website / audio player / voice app.
	if ( $has( [ 'podcast website', 'podcast player', 'audio player', 'voice app', 'podcast hosting', 'podcast setup', 'audio content', 'audio streaming', 'podcast directory', 'spotify embed', 'soundcloud embed', 'podcast page', 'rss feed podcast', 'show notes', 'wavesurfer' ] ) ) {
		return $nl( [
			"Podcast and audio integration are available &mdash; here&rsquo;s the full picture:",
			'',
			"&bull; <strong>Podcast website</strong> &mdash; custom HTML5 player or Spotify / SoundCloud embed; episode archive with show notes + transcripts; RSS feed for Apple / Spotify directories; from <strong>$500</strong> in the Web service",
			"&bull; <strong>Waveform player</strong> &mdash; WaveSurfer.js for immersive audio landing pages; scrub + play; responsive; can visualize any hosted audio file",
			"&bull; <strong>Podcast hosting</strong> &mdash; not in-house; recommend Buzzsprout, Transistor, or Anchor (free) &mdash; I build the site that lists and embeds your episodes",
			"&bull; <strong>Voice UX</strong> &mdash; browser SpeechSynthesis + SpeechRecognition APIs (like this chatbot); bundled into SaaS builds; Alexa / Google Assistant skills out of current scope",
			'',
			"Is this a podcast site, an embedded audio player, or a voice-driven interface?",
		] );
	}

	// 0d-pre13-a) White-label / reseller / agency partnership.
	if ( $has( [ 'white label', 'white-label', 'resell', 'reseller', 'agency reselling', 'rebrand for clients', 'white label solution', 'agency white label', 'client reselling', 'under my brand', 'under our brand', 'agency partnership', 'dev partnership', 'whitelabel' ] ) ) {
		return $nl( [
			"White-label and agency partnerships are available:",
			'',
			"&bull; <strong>Deliverables</strong> &mdash; shipped without Photon Bounce branding; all code, repos, and assets transfer cleanly to you or your client",
			"&bull; <strong>Agency rate</strong> &mdash; volume discount available for 3+ projects/year; DM or email for rate card",
			"&bull; <strong>Branding</strong> &mdash; WP admin footer + email notifications can match your agency domain and logo",
			"&bull; <strong>Communication</strong> &mdash; I can communicate directly with your client, or stay behind the scenes &mdash; your call",
			"&bull; <strong>NDA</strong> &mdash; available on request; standard mutual or one-way",
			"&bull; <strong>No licensing fees</strong> &mdash; no per-project or per-seat charge; clean open transfer",
			'',
			"How many projects per year are you looking at, and what&rsquo;s the typical stack?",
		] );
	}

	// 0d-pre13-b) CRO / conversion rate optimization.
	if ( $has( [ 'conversion rate', ' cro ', 'conversion optimization', 'a/b testing', 'ab testing', 'split testing', 'landing page optimization', 'heatmap', 'click tracking', 'funnel analysis', 'bounce rate', 'optimize conversions', 'improve conversions', 'conversion funnel', 'low conversion' ] ) ) {
		return $nl( [
			"Conversion rate optimization is available as a project add-on or standalone:",
			'',
			"&bull; <strong>A/B testing</strong> &mdash; VWO or Optimizely integration; or lightweight vanilla JS split test with cookie-based assignment and GA4 event tracking",
			"&bull; <strong>Heatmap &amp; session recording</strong> &mdash; Microsoft Clarity (free) or Hotjar; installed, configured, and dashboard set up",
			"&bull; <strong>Funnel analysis</strong> &mdash; GA4 conversion events + goal funnels; Looker Studio dashboard; reveals drop-off steps",
			"&bull; <strong>Landing page variant</strong> &mdash; full new LP variant from <strong>$150</strong>; tested against existing",
			"&bull; <strong>Speed &rarr; conversions</strong> &mdash; each 1s improvement = up to 7% conversion lift; bundle with Core Web Vitals audit",
			'',
			"What&rsquo;s the current conversion rate, and what page or funnel are you focusing on?",
		] );
	}

	// 0d-pre13-c) AR / VR / spatial computing / immersive web.
	if ( $has( [ 'augmented reality', 'ar filter', 'virtual reality', 'vr experience', 'spatial computing', 'apple vision pro', 'webxr', 'ar/vr', 'mixed reality', 'xr experience', 'metaverse', 'immersive experience', '360 video', 'three.js vr', 'aframe', 'model viewer', 'product try-on', 'ar try on' ] ) ) {
		return $nl( [
			"AR / VR and immersive web are in scope &mdash; here&rsquo;s the current capability:",
			'',
			"&bull; <strong>Web AR (model viewer / product try-on)</strong> &mdash; &lt;model-viewer&gt; with USDZ + GLB; works on iOS Safari and Android Chrome without an app download; bundled into the 3D / WebGL service",
			"&bull; <strong>WebXR in-browser VR</strong> &mdash; A-Frame or Three.js XR; runs in Meta Quest browser; from <strong>$750</strong>",
			"&bull; <strong>AR filters (Instagram / TikTok)</strong> &mdash; Spark AR (Meta) or Effect House (TikTok); from <strong>$200 / filter</strong>",
			"&bull; <strong>Apple Vision Pro (visionOS)</strong> &mdash; SwiftUI spatial apps; scoping available in 2026; DM for timeline",
			"&bull; <strong>360&deg; video integration</strong> &mdash; pannellum.js or Three.js equirectangular scene; works on desktop + mobile gyro",
			'',
			"Is this product visualization, an AR filter, or a full immersive experience?",
		] );
	}

	// 0d-pre12-a) SaaS user onboarding / activation flow.
	if ( $has( [ 'onboarding flow', 'user onboarding', 'welcome flow', 'getting started flow', 'activation flow', 'onboarding checklist', 'onboarding wizard', 'new user flow', 'product tour', 'guided tour', 'user activation', 'first-time user', 'setup wizard' ] ) ) {
		return $nl( [
			"User onboarding is included in the SaaS / App build &mdash; here&rsquo;s how it typically scopes:",
			'',
			"&bull; <strong>Welcome wizard</strong> &mdash; multi-step modal on first login; collects preferences; saves to user meta; progress bar",
			"&bull; <strong>Onboarding checklist</strong> &mdash; floating widget with completion % (like Intercom); each task links to the relevant screen",
			"&bull; <strong>Email drip sequence</strong> &mdash; D0 welcome, D3 feature tips, D7 spotlight; triggered via WP Cron or Zapier; MJML-templated",
			"&bull; <strong>Empty-state prompts</strong> &mdash; custom illustrations + CTA for &ldquo;add your first item&rdquo; screens",
			"&bull; <strong>Product tour</strong> &mdash; Shepherd.js sequential tooltips; skip / resume state stored in localStorage",
			'',
			"What does a user need to do to reach their first &ldquo;aha moment&rdquo;?",
		] );
	}

	// 0d-pre12-b) Maintenance / support / monthly retainer.
	if ( $has( [ 'maintenance plan', 'support retainer', 'monthly retainer', 'ongoing support', 'website maintenance', 'update plan', 'care plan', 'post-launch support', 'site support', 'bug fixes ongoing', 'manage my site', 'keep my site updated', 'site maintenance', 'hosting management' ] ) ) {
		return $nl( [
			"Monthly care plans are available after launch:",
			'',
			"&bull; <strong>Basic &mdash; $99/mo</strong> &mdash; WP core / plugin / security updates, daily backups, uptime monitoring",
			"&bull; <strong>Growth &mdash; $199/mo</strong> &mdash; everything in Basic + up to 4 content edits/mo, Core Web Vitals check, priority email support",
			"&bull; <strong>Pro &mdash; $349/mo</strong> &mdash; everything in Growth + 2 dev hours/mo (bug fixes, small features), A/B test setup, monthly analytics report",
			'',
			"All plans billed monthly, cancel any time. What do you currently have for maintenance?",
		] );
	}

	// 0d-pre12-c) Video / animation / explainer video.
	if ( $has( [ 'explainer video', 'animated video', 'product video', 'demo video', 'screen recording', 'lottie animation', 'svg animation', 'video editing', 'video production', 'motion graphics', 'product demo', 'walkthrough video', 'promo video', 'rive animation', 'hero animation' ] ) ) {
		return $nl( [
			"Video and animation options &mdash; here&rsquo;s what&rsquo;s available:",
			'',
			"<strong>UI / web animations (bundled into builds):</strong>",
			"&bull; <strong>Lottie</strong> &mdash; JSON animations that drop into any web page; lightweight, loop-able, responsive",
			"&bull; <strong>Rive</strong> &mdash; interactive state-machine animations for heroes, loaders, empty states",
			"&bull; <strong>CSS / GSAP</strong> &mdash; scroll-triggered reveals, parallax, morphing shapes",
			'',
			"<strong>Standalone video (referred to trusted editors):</strong>",
			"&bull; Screen recording + voiceover walkthrough &mdash; from <strong>$150</strong>",
			"&bull; Animated explainer (whiteboard / motion) &mdash; partner studio from <strong>$400&ndash;$800</strong>",
			"&bull; Product demo with CTA overlay (embeddable) &mdash; from <strong>$99</strong>",
			'',
			"Is this for a UI animation inside the site, or a standalone video?",
		] );
	}

	// 0d-pre11-a) Community / forum / online community platform.
	if ( $has( [ 'community site', 'online community', 'forum site', 'discussion board', 'membership community', 'discord-like', 'reddit-like', 'peer community', 'community platform', 'social network', 'q&a platform', 'knowledge base community', 'private community', 'members-only forum', 'support forum', 'user forum' ] ) ) {
		return $nl( [
			"Community and forum sites are a good fit &mdash; here&rsquo;s how they typically scope:",
			'',
			"&bull; <strong>Basic forum (Q&amp;A / support)</strong> &mdash; bbPress on WordPress; threaded replies, category routing, user roles; bundled into <strong>SaaS / App $750</strong>",
			"&bull; <strong>Private membership community</strong> &mdash; MemberPress or Restrict Content Pro; gated content, community areas, membership tiers; <strong>SaaS / App $750+</strong>",
			"&bull; <strong>Full social / community platform</strong> &mdash; BuddyBoss or custom Next.js + real-time features (WebSockets); <strong>custom quote from $1,500</strong>",
			'',
			"Key decisions: is it open or invite-only? Do you need real-time messaging? Paid tiers? I can scope around those.",
		] );
	}

	// 0d-pre11-b) Gamification / points / leaderboards / achievements.
	if ( $has( [ 'gamification', 'points system', 'leaderboard', 'badges', 'achievements', 'reward system', 'loyalty program', 'streak tracking', 'progress tracker', 'xp system', 'level up', 'user levels', 'referral rewards', 'incentive system', 'engagement mechanics' ] ) ) {
		return $nl( [
			"Gamification layers well on top of SaaS, community, or e-learning builds. Here&rsquo;s what&rsquo;s feasible:",
			'',
			"&bull; <strong>Points &amp; XP</strong> &mdash; custom database table; action hooks award points (post, comment, purchase, referral)",
			"&bull; <strong>Badges &amp; achievements</strong> &mdash; SVG badge set + unlock logic; displayed on user profile",
			"&bull; <strong>Leaderboard</strong> &mdash; real-time or cached; global or per-cohort; CSV export for ops",
			"&bull; <strong>Streaks</strong> &mdash; last-active timestamp check; streak broken if gap &gt; N days",
			"&bull; <strong>Reward redemption</strong> &mdash; coupon codes, feature unlocks, or physical reward triggers via webhook",
			'',
			"Gamification is an add-on to any SaaS/App build &mdash; typically +$200&ndash;$400 depending on complexity.",
			"What behaviour are you trying to incentivise?",
		] );
	}

	// 0d-pre11-c) Real-time features / WebSockets / live updates.
	if ( $has( [ 'real-time', 'real time', 'websockets', 'socket.io', 'live chat', 'live updates', 'live feed', 'collaborative editing', 'google docs like', 'live dashboard', 'pusher', 'ably', 'supabase realtime', 'multiplayer', 'instant notifications', 'push notifications app' ] ) ) {
		return $nl( [
			"Real-time features are available &mdash; here&rsquo;s the typical stack depending on use case:",
			'',
			"&bull; <strong>Live notifications</strong> &mdash; Pusher or Supabase Realtime; sub-$20/mo for most traffic; integrated into any SaaS build",
			"&bull; <strong>Live chat (user &harr; user)</strong> &mdash; Socket.io on a Node.js server or Supabase channels; <strong>SaaS / App $750+</strong>",
			"&bull; <strong>Collaborative editing</strong> (Google Docs-like) &mdash; Y.js (CRDT) + Tiptap editor + WebSocket sync server; complex but doable; <strong>custom quote from $1,500</strong>",
			"&bull; <strong>Live dashboard / ticker</strong> &mdash; Server-Sent Events (SSE) for one-way streams; lower infra overhead than WebSockets",
			'',
			"What&rsquo;s updating in real time &mdash; a chat, a data feed, or collaborative content?",
		] );
	}

	// 0d-pre10-a) Admin dashboard / internal tools.
	if ( $has( [ 'admin dashboard', 'internal tool', 'back-office', 'admin panel', 'management portal', 'internal ops', 'staff portal', 'team dashboard', 'reporting dashboard', 'data dashboard', 'ops tool', 'internal system', 'employee portal', 'admin area', 'control panel', 'crm build', 'custom crm' ] ) ) {
		return $nl( [
			"Admin dashboards and internal tools are a strong fit here &mdash; they&rsquo;re basically the SaaS/App tier with a private-only audience.",
			'',
			"What typically goes into an internal dashboard build:",
			"&bull; <strong>Role-based access control</strong> &mdash; admins vs. staff vs. read-only views",
			"&bull; <strong>Data tables with filter / sort / export</strong> &mdash; CSV or PDF export included",
			"&bull; <strong>Charts and KPI cards</strong> &mdash; Chart.js or D3 connected to your data source",
			"&bull; <strong>REST or GraphQL API layer</strong> &mdash; so the frontend talks to your existing database",
			"&bull; <strong>Audit log</strong> &mdash; who changed what, when",
			'',
			"Starting at <strong>$750 (SaaS / App tier)</strong>. Custom CRM integrations (Salesforce, HubSpot) scoped separately.",
			"What data are you displaying, and who are the users?",
		] );
	}

	// 0d-pre10-b) Tech stack / framework recommendation.
	if ( $has( [ 'what tech stack', 'which framework', 'what should i use', 'react vs next', 'next.js or react', 'should i use wordpress', 'technology recommendation', 'what technology', 'which cms', 'best stack for', 'recommend a framework', 'webflow vs wordpress', 'squarespace vs wordpress', 'what do you use', 'tech recommendation', 'which platform should' ] ) ) {
		return $nl( [
			"Tech stack recommendation depends on what you&rsquo;re building and who will maintain it. Here&rsquo;s the quick cheat-sheet:",
			'',
			"&bull; <strong>WordPress</strong> &mdash; content-heavy sites, blogs, small e-commerce; owner edits content without a dev; $40&ndash;$750",
			"&bull; <strong>Next.js / React</strong> &mdash; fast interactive apps, SPAs, dashboards; needs a dev to update; $750+",
			"&bull; <strong>Headless WordPress + Next.js</strong> &mdash; best of both: WP as CMS, Next.js as frontend; SEO-optimized; $750+",
			"&bull; <strong>Shopify</strong> &mdash; serious e-commerce (1,000+ SKUs, complex fulfillment); if WooCommerce isn&rsquo;t enough",
			"&bull; <strong>WebGL / Three.js</strong> &mdash; 3D / AR / game-like experiences; standalone or embedded",
			'',
			"What&rsquo;s the core use case &mdash; content site, app, store, or something interactive?",
		] );
	}

	// 0d-pre10-c) Headless CMS / JAMstack / API-first architecture.
	if ( $has( [ 'headless cms', 'headless wordpress', 'api-first', 'decoupled frontend', 'jamstack', 'static site generator', 'nextjs headless', 'gatsby', 'sanity.io', 'contentful', 'strapi', 'payload cms', 'directus', 'decoupled cms', 'headless architecture', 'static generation', 'server-side rendering', 'ssr vs ssg', 'edge rendering' ] ) ) {
		return $nl( [
			"Headless and JAMstack builds are a specialty here. Here&rsquo;s how the stack typically looks:",
			'',
			"&bull; <strong>CMS layer</strong> &mdash; WordPress (REST or WPGraphQL), Sanity.io, or Strapi; content editors get a familiar UI",
			"&bull; <strong>Frontend</strong> &mdash; Next.js (SSR + ISR for SEO) or Astro (ultra-fast, mostly static)",
			"&bull; <strong>Hosting</strong> &mdash; Vercel or Netlify for the frontend; WP Engine / Kinsta / SiteGround for the CMS",
			"&bull; <strong>API layer</strong> &mdash; REST or GraphQL endpoints + webhook revalidation so the static site updates when content changes",
			'',
			"<strong>When to go headless:</strong> high-traffic marketing sites that need 95+ Lighthouse, complex content models, or multi-channel publishing (web + mobile + IoT).",
			"<strong>When to stay coupled:</strong> simpler sites where build complexity isn&rsquo;t worth the gain.",
			'',
			"What&rsquo;s the content model and traffic expectation?",
		] );
	}

	// 0d-pre9-v) Subscription / recurring billing (SaaS pricing model).
	if ( $has( [ 'subscription billing', 'recurring billing', 'saas pricing model', 'monthly subscription', 'stripe subscriptions', 'metered billing', 'usage-based pricing', 'per-seat pricing', 'tiered pricing model', 'subscription model', 'revenue subscription', 'billing portal', 'cancel anytime', 'free trial billing', 'freemium model' ] ) ) {
		return $nl( [
			"Subscription billing is a common feature in SaaS builds. Here&rsquo;s how it works in practice:",
			'',
			"&bull; <strong>Stripe Billing</strong> &mdash; monthly / annual plans, per-seat, metered, or tiered pricing; handled via Stripe&rsquo;s hosted checkout and billing portal",
			"&bull; <strong>Free trial &rarr; paid</strong> &mdash; trial period with automatic upgrade via Stripe; no credit card required flows also possible",
			"&bull; <strong>Webhooks</strong> &mdash; plan changes, cancellations, and failed payments all trigger server-side events your app reacts to in real time",
			"&bull; <strong>Customer portal</strong> &mdash; Stripe&rsquo;s built-in portal lets subscribers upgrade, downgrade, or cancel without contacting support",
			'',
			"This is <strong>SaaS / App tier ($750+)</strong>. Tell me what your plans and pricing tiers look like and I&rsquo;ll scope the billing integration.",
		] );
	}

	// 0d-pre9-w) Multilingual / i18n / RTL support.
	if ( $has( [ 'multiple languages', 'multilingual site', 'bilingual site', 'i18n', 'internationalization', 'translate my site', 'spanish version', 'french version', 'german version', 'arabic site', 'rtl support', 'right to left', 'polylang', 'wpml', 'translation', 'localization', 'multi-language' ] ) ) {
		return $nl( [
			"Multilingual sites are supported &mdash; here&rsquo;s the stack depending on scope:",
			'',
			"&bull; <strong>2&ndash;3 languages (WordPress)</strong> &mdash; Polylang Free or WPML; manual or machine-translated content; hreflang tags auto-set for SEO",
			"&bull; <strong>RTL languages (Arabic, Hebrew, Persian)</strong> &mdash; CSS logical properties, font swap, and WP RTL stylesheet; tested on Chrome + Safari mobile",
			"&bull; <strong>Auto-translation</strong> &mdash; DeepL or Google Translate API can be wired in for automatic translation of new content; add ~$150 to any tier",
			"&bull; <strong>Language switcher</strong> &mdash; flag dropdown or ISO-code toggle, styled to match your design",
			'',
			"How many languages, and do you already have the translated content or do you need auto-translation?",
		] );
	}

	// 0d-pre9-x) Performance / page speed / Core Web Vitals.
	if ( $has( [ 'page speed', 'performance optimization', 'core web vitals', 'lighthouse score', 'slow website', 'lcp', 'cls', 'inp', 'fid', 'first contentful paint', 'time to interactive', 'page load time', 'speed up my site', 'site is slow', 'performance audit', 'gtmetrix', 'pagespeed insights', 'webpagetest' ] ) ) {
		return $nl( [
			"Performance is a first-class concern on every build. Here&rsquo;s what&rsquo;s standard:",
			'',
			"&bull; <strong>LCP (Largest Contentful Paint)</strong> &mdash; hero images served as WebP + preloaded; critical CSS inlined",
			"&bull; <strong>CLS (Cumulative Layout Shift)</strong> &mdash; explicit dimensions on all images and embeds; no layout-shifting fonts",
			"&bull; <strong>INP (Interaction to Next Paint)</strong> &mdash; main-thread work minimized; heavy JS deferred or split",
			"&bull; <strong>Score target</strong> &mdash; 90+ on Lighthouse Performance for the homepage; service pages typically 85+",
			"&bull; <strong>Existing site audit</strong> &mdash; if you have a slow live site, I can run a PageSpeed + WebPageTest audit and give you a prioritized fix list",
			'',
			"Is this for a new build or an existing site you want optimized?",
		] );
	}

	// 0d-pre9-s) ADA / accessibility / WCAG compliance.
	if ( $has( [ 'ada compliant', 'accessible website', 'accessibility', 'screen reader', 'wcag', '508 compliance', 'disability access', 'aria labels', 'color contrast', 'keyboard navigation', 'blind user', 'visually impaired', 'accessibility audit', 'is it accessible', 'make it accessible' ] ) ) {
		return $nl( [
			"Accessibility is baked in at every tier &mdash; not an add-on. Here&rsquo;s what&rsquo;s included by default:",
			'',
			"&bull; <strong>WCAG 2.1 AA</strong> &mdash; semantic HTML, proper heading hierarchy, focus indicators, skip-nav link",
			"&bull; <strong>ARIA labels</strong> &mdash; on all interactive elements (forms, buttons, modals, sliders)",
			"&bull; <strong>Color contrast</strong> &mdash; minimum 4.5:1 for body text, 3:1 for large text",
			"&bull; <strong>Keyboard navigation</strong> &mdash; every interactive element reachable and operable by keyboard alone",
			"&bull; <strong>Screen reader testing</strong> &mdash; VoiceOver (macOS / iOS) and NVDA (Windows)",
			'',
			"Need a formal <strong>accessibility audit report</strong> (for legal/grant compliance)? That&rsquo;s available as a standalone deliverable &mdash; ask about it.",
		] );
	}

	// 0d-pre9-t) Payment plan / installments.
	if ( $has( [ 'pay in installments', 'payment plan', 'split the payment', 'split payment', 'pay monthly', 'pay over time', 'deposit and balance', '50% deposit', '50 percent deposit', 'milestone payment', 'down payment', 'can i pay later', 'partial payment', 'pay in stages' ] ) ) {
		return $nl( [
			"Yes &mdash; all builds follow a milestone payment structure:",
			'',
			"&bull; <strong>50% upfront</strong> to kick off the project (covers design + initial dev)",
			"&bull; <strong>50% on delivery</strong> before the site goes live or the APK is handed over",
			'',
			"For larger builds ($750+), a 3-milestone split is available on request:",
			"<strong>33% kick-off &rarr; 33% first-build review &rarr; 33% on launch.</strong>",
			'',
			"Payment is via Stripe (card / Apple Pay / Google Pay) or Cash App ($photonbounce). Crypto on request.",
		] );
	}

	// 0d-pre9-u) Native mobile app (iOS / Android).
	if ( $has( [ 'ios app', 'android app', 'native app', 'native mobile app', 'app store', 'google play', 'react native', 'flutter', 'native vs web', 'publish to app store', 'submit to app store', 'mobile app development', 'apk', 'ipa file', 'cross-platform app' ] ) ) {
		return $nl( [
			"Native mobile apps are in scope &mdash; here&rsquo;s how the tiers map:",
			'',
			"&bull; <strong>PWA (Progressive Web App)</strong> &mdash; installable on home screen, works offline, push notifications. Builds into the SaaS/App tier at $750. Looks and feels native on Android; iOS has slightly more limitations.",
			"&bull; <strong>React Native</strong> &mdash; true native builds for iOS + Android from one codebase. Custom quote, typically $1,500&ndash;$3,000 depending on feature depth.",
			"&bull; <strong>App Store / Google Play submission</strong> &mdash; I handle signing, screenshots, store listing, and the review process. Included in React Native scope.",
			'',
			"What&rsquo;s the app for? Describe the core feature and I&rsquo;ll recommend the right approach.",
		] );
	}

	// 0d-pre9-r) Migration / site rebuild from another platform.
	if ( $has( [ 'moving from wix', 'moving from squarespace', 'moving from shopify', 'leaving wix', 'leaving squarespace', 'leaving shopify', 'migrate from', 'site migration', 'content migration', 'migrate my site', 'rebuild my old site', 'rebuild my current site', 'replace my old site', 'switch to wordpress', 'switch from wix', 'switch from squarespace' ] ) ) {
		return $nl( [
			"Migrations are a regular part of the work here. Here&rsquo;s what happens on a typical platform switch:",
			'',
			"&bull; <strong>Content migration</strong> &mdash; pages, posts, images, redirects (301s) handled for you",
			"&bull; <strong>Domain transfer</strong> &mdash; DNS cutover coordinated so there&rsquo;s zero downtime",
			"&bull; <strong>SEO preservation</strong> &mdash; existing permalink structure mapped, canonical tags set, Search Console re-verified",
			"&bull; <strong>Design</strong> &mdash; clean break or close-match to your current look, your call",
			'',
			"Migration scope is bundled into the site build price &mdash; no &ldquo;migration surcharge.&rdquo;",
			"Which platform are you moving from, and roughly how many pages?",
		] );
	}

	// 0d-pre9) Domain / hosting questions.
	if ( $has( [ 'who hosts', 'where is it hosted', 'do you provide hosting', 'hosting included', 'do you host', 'hosting plan', 'domain name', 'do you register', 'buy a domain', 'domain included', 'ssl certificate', 'https', 'server', 'vps', 'cloud hosting', 'where will it live' ] ) ) {
		return $nl( [
			"Hosting and domains are part of the setup &mdash; here&rsquo;s how it works:",
			'',
			"&bull; <strong>Domain</strong> &mdash; if you don&rsquo;t have one, I help you register it (~$15/yr via Namecheap or Google Domains). You own it; it lives in your account.",
			"&bull; <strong>Hosting</strong> &mdash; for most sites: managed WordPress on WP Engine or shared hosting via SiteGround (~$5&ndash;15/mo). For SaaS apps: a VPS on DigitalOcean or Railway. I configure everything; you get logins.",
			"&bull; <strong>SSL (HTTPS)</strong> &mdash; always included, via Let&rsquo;s Encrypt or the host&rsquo;s built-in certificate.",
			"&bull; <strong>Care Plan ($50/mo)</strong> &mdash; includes hosting management, renewals, and updates so you never have to think about it.",
			'',
			"Want me to include hosting setup in your quote?",
		] );
	}

	// 0d-pre10) Third-party API / platform integrations.
	if ( $has( [ 'stripe', ' paypal', 'payment gateway', 'api integration', 'third party api', 'third-party api', 'zapier', 'make.com', 'hubspot', 'salesforce', 'mailchimp', 'klaviyo', 'twilio', 'sendgrid', 'airtable', 'notion api', 'webhook', 'crm integration', 'connect to my', 'integrate with', 'google analytics', 'google maps', 'facebook pixel', 'meta pixel' ] ) ) {
		return $nl( [
			"Integrations are a normal part of most builds &mdash; not a paid add-on. What&rsquo;s common in projects here:",
			'',
			"&bull; <strong>Payments</strong>: Stripe, PayPal, Square &mdash; checkout flows, subscriptions, webhooks",
			"&bull; <strong>Automation</strong>: Zapier, Make.com, n8n &mdash; trigger emails, CRM updates, Slack pings",
			"&bull; <strong>CRM / Email</strong>: HubSpot, Mailchimp, Klaviyo, SendGrid",
			"&bull; <strong>Analytics</strong>: GA4, Meta Pixel, GTM &mdash; full event setup",
			"&bull; <strong>Mapping / Comms</strong>: Google Maps, Twilio SMS, WhatsApp API",
			"&bull; <strong>Databases / No-code</strong>: Airtable, Notion API, Supabase",
			'',
			"Tell me which platform and what you need it to do &mdash; I&rsquo;ll confirm it&rsquo;s in scope and whether it affects the price.",
		] );
	}

	// 0d-pre11) Demo / live example request.
	if ( $has( [ 'can i see a demo', 'show me a demo', 'live example', 'working example', 'can you show me', 'see it live', 'link to your work', 'do you have a demo', 'demo site', 'example site', 'see the chatbot', 'try the chatbot', 'see a live', 'show me an example' ] ) ) {
		return $nl( [
			"You&rsquo;re already talking to one &mdash; I&rsquo;m the AI concierge built and deployed by Photon Bounce. For other live examples:",
			'',
			"&bull; <strong><a href=\"/occupantkiller/\">OccupantKiller</a></strong> &mdash; WebGL 3D shooter, runs in-browser with physics and particle effects",
			"&bull; <strong><a href=\"/ausis/\">Ausis</a></strong> &mdash; a live Android app (Kotlin, voice-first AI companion)",
			"&bull; <strong>This site</strong> &mdash; WordPress with a custom AI chatbot, 3D orb, speech synthesis, scroll effects",
			"&bull; <strong><a href=\"/portfolio/\">Portfolio page</a></strong> &mdash; more case studies with descriptions",
			'',
			"For a live walkthrough of what your specific project would look like, book a 15-min call at <a href=\"/book/\">/book/</a>.",
		] );
	}

	// 0d-pre12) Referral / warm lead.
	if ( $has( [ 'someone recommended', 'was referred', 'i was referred', 'my friend told', 'heard about you from', 'recommended by', 'referred by', 'a friend said', 'a colleague mentioned', 'your name came up', 'good things about you' ] ) ) {
		return $nl( [
			"Thanks for making the jump &mdash; referrals are how most of the best projects here start.",
			'',
			"What&rsquo;s the project? Tell me what you need built (site, app, AI agent, brand) and I&rsquo;ll give you a real ballpark straight away.",
		] );
	}

	// 0d) WordPress / CMS specific.
	if ( $has( [ 'wordpress', ' woocommerce', ' woo ', 'wp theme', 'wp plugin', 'cms', 'content management' ] ) ) {
		return $nl( [
			"We build on WordPress regularly &mdash; custom themes, WooCommerce stores, and plugin development all fall under the <strong>Full Site</strong> ($300) or <strong>SaaS / App</strong> ($750) tier depending on complexity.",
			'',
			"Every build includes a handoff with full source so you own it outright &mdash; no proprietary page-builder lock-in. What are you trying to do: new build, redesign, or adding features to an existing WP site?",
		] );
	}

	// 0e) Redesign / fix / improve existing site.
	if ( $has( [ 'redesign', 'refresh', 'update my site', 'fix my site', 'improve my site', 'redo my', 'revamp', 'existing site', 'already have a site', 'my current site', 'my website needs', 'slow website', 'site is slow', 'broken site' ] ) ) {
		return $nl( [
			"Redesigns and fixes are scoped the same way as new builds: I audit what&rsquo;s there, write a one-pager, and quote a fixed price.",
			'',
			"Quick fixes (speed, SEO, broken pages) usually land in the <strong>Simple Site</strong> tier ($115). A full visual + tech redesign is <strong>Full Site</strong> ($300). Which is closer to what you need?",
		] );
	}

	// 0f) Timeline / turnaround questions.
	if ( $has( [ 'how long', 'how quickly', 'turnaround', 'timeline', 'delivery time', 'when can', 'how fast', 'time to build', 'days to complete', 'weeks to complete' ] ) ) {
		return $nl( [
			"Typical timelines &mdash; from scope-sign to launch:",
			'',
			"&bull; <strong>Micro Page</strong> ($40) &mdash; 2–4 days",
			"&bull; <strong>Simple Site</strong> ($115) &mdash; 4–7 days",
			"&bull; <strong>Full Site</strong> ($300) &mdash; 1–2 weeks",
			"&bull; <strong>SaaS / App</strong> ($750) &mdash; 4–8 weeks",
			"&bull; <strong>AI Agent</strong> ($565) &mdash; 1–3 weeks",
			'',
			"Rush delivery (×1.4 price) is available if you have a hard deadline. What are you building and when do you need it?",
		] );
	}

	// 0g) Startup / new business intent.
	if ( $has( [ 'startup', 'new business', 'just starting', 'new company', 'early stage', 'side project', 'side hustle', 'bootstrap', 'pre-seed', 'seed stage', 'launch my', 'my idea' ] ) ) {
		return $nl( [
			"Welcome &mdash; this is a good place to start. For early-stage and bootstrapped projects I usually recommend:",
			'',
			"&bull; <strong>Micro Page</strong> ($40) &mdash; get online fast with a single conversion-focused page",
			"&bull; <strong>Simple Site</strong> ($115) &mdash; 3–5 pages, SEO, analytics, contact form &mdash; the full starter kit",
			"&bull; <strong>AI Concierge bot</strong> ($190) &mdash; add a trained chatbot to any of the above",
			'',
			"What&rsquo;s the business and who&rsquo;s the target customer?",
		] );
	}

	// 1) Greeting / short opener.
	if ( $turn < 2 && $has( [ ' hi ', ' hey', 'hello', ' yo ', ' sup', 'howdy', 'good morning', 'good afternoon', 'good evening', 'hola', 'greetings', 'what\'s up', 'whats up' ] ) ) {
		return "Hey &mdash; I&rsquo;m Photon, the studio concierge. Tell me what you want to build and I&rsquo;ll point you to the right service and price. Most people start with a <strong>website or store</strong>, an <strong>AI agent / chatbot</strong>, a <strong>3D / AR experience</strong>, <strong>SEO</strong>, or a <strong>brand</strong>. What&rsquo;s the project?";
	}

	// 2) Booking / live call.
	if ( $has( [ 'book', 'schedule', 'call me', ' a call', 'meeting', 'consult', 'zoom', 'phone' ] ) ) {
		return $nl( [
			"Let&rsquo;s talk live &mdash; grab a free 30-minute call at <a href=\"{$book}\">{$book}</a>, or phone / WhatsApp <strong>857-316-5054</strong>.",
			'',
			"Tell me the project and a rough budget first and I&rsquo;ll show up with a tier and a fixed price already mapped out.",
		] );
	}

	// 3) Human / contact.
	if ( $has( [ 'human', 'real person', 'someone', 'email', 'contact', 'get in touch', 'reach you' ] ) ) {
		return $nl( [
			'Easiest paths: email <strong>' . esc_html( $email ) . '</strong>, or call / WhatsApp <strong>857-316-5054</strong>. I can also book a free 30-min call at <a href="' . $book . '">' . $book . '</a>.',
			'',
			"While you&rsquo;re here, what are you building? I&rsquo;ll scope it and quote a tier right now.",
		] );
	}

	// 4) Service intent → a real tier with a benefit.
	$rec = null;
	if ( $has( [ 'landing', 'one-pager', 'one page', 'one-page', 'single page' ] ) ) {
		$rec = [ 'a Micro Page', '$40', 'a fast one-page site that loads instantly and converts' ];
	} elseif ( $has( [ 'shop', 'store', 'ecommerce', 'e-commerce', 'product page', 'sell online', 'checkout' ] ) ) {
		$rec = [ 'a Full Site', '$300', 'multi-page with a product / checkout flow, SEO-mapped' ];
	} elseif ( $has( [ 'webgl', 'three.js', 'threejs', 'shader', 'particle', '3d hero', 'animated' ] ) ) {
		$rec = [ 'a Pro Site + WebGL', '$600', 'a 3D / animated hero that makes you look years ahead' ];
	} elseif ( $has( [ 'saas', 'dashboard', 'portal', 'web app', 'login', 'accounts', ' auth' ] ) ) {
		$rec = [ 'a SaaS / App build', '$750', 'auth, dashboards and a real backend' ];
	} elseif ( $has( [ 'agent', 'chatbot', 'chat bot', ' gpt', ' rag', 'voicebot', 'concierge', 'assistant', 'ai bot' ] ) ) {
		$rec = [ 'a Custom AI Agent', '$565', 'an LLM agent trained on your content &mdash; a Concierge bot like me is $190' ];
	} elseif ( $has( [ ' seo', 'rank', 'google', 'traffic', 'search engine' ] ) ) {
		$rec = [ 'SEO Growth', '$115/mo', 'technical fixes plus content so you actually rank &mdash; a one-off Audit is $30' ];
	} elseif ( $has( [ ' aeo', 'perplexity', 'llm citation', 'ai search', 'chatgpt cite' ] ) ) {
		$rec = [ 'AEO Setup', '$100', 'so ChatGPT and Perplexity cite you by name' ];
	} elseif ( $has( [ 'logo', 'brand', 'identity', 'rebrand' ] ) ) {
		$rec = [ 'a Mini Identity', '$115', 'logo, palette, type and basic guidelines &mdash; just a logo is $30' ];
	} elseif ( $has( [ 'invest', 'startup idea', ' mvp', 'co-build', 'equity', 'my own product' ] ) ) {
		return $nl( [
			"Sounds like you want to build a product, not just a page. I keep 50 ready-to-build SaaS / AI / physics concepts at <a href=\"{$invest}\">{$invest}</a> with MVP / Beta / Full pricing and 3 / 6 / 12-month plans (30% down).",
			'',
			"Tell me your industry and budget and I&rsquo;ll suggest a couple that fit.",
		] );
	} elseif ( $has( [ 'mobile app', 'android app', 'ios app', ' flutter', 'react native', 'app store', 'google play', ' apk', 'smartphone app', 'phone app', 'native app' ] ) ) {
		$rec = [ 'an Agent + App', '$1,185', 'a full mobile app with an AI agent baked in &mdash; or a standalone SaaS / App build from $750 if you skip the AI layer' ];
	} elseif ( $has( [ 'social media', ' instagram', ' tiktok', ' smm ', 'social content', 'content calendar', 'facebook ads', 'content strategy', 'posting schedule', 'social posts' ] ) ) {
		$rec = [ 'SMM Content', '$75/mo', 'a monthly social content calendar plus scheduling &mdash; a quick one-off Tune-Up is $25' ];
	} elseif ( $has( [ 'maintenance', 'support plan', 'care plan', 'monthly retainer', 'keep it running', 'hosting support', 'site upkeep', 'ongoing support', 'website support', 'updates and fixes' ] ) ) {
		$rec = [ 'a Care Plan', '$50/mo', 'monthly updates, backups, performance checks and priority support &mdash; Lite is $15/mo' ];
	} elseif ( $has( [ ' 3d ', ' ar ', ' vr ', 'augmented reality', 'virtual reality', 'product configurator', 'model viewer', '3d experience', '3d product', 'three.js', 'threejs', 'webgl', 'shader', 'particle', '3d hero', 'animated hero', '3d animation' ] ) ) {
		$rec = [ 'a 3D / WebGL build', '$600+', 'a Three.js hero, product configurator or full AR / VR experience &mdash; runs 100% in the browser' ];
	} elseif ( $has( [ 'membership', 'subscription site', 'gated content', 'members only', 'paid community', 'paywall', 'subscribers only', 'member area', 'exclusive content', 'subscription model', 'recurring billing' ] ) ) {
		$rec = [ 'a SaaS / App build', '$750', 'auth, member tiers, gated content and Stripe recurring billing baked in' ];
	} elseif ( $has( [ ' blog', 'news site', 'editorial site', 'content site', 'publication', 'magazine site', 'article site', 'news page', 'blog post', 'writing platform', 'longform', 'media site' ] ) ) {
		$rec = [ 'a Full Site', '$300', 'WordPress with a performance-tuned editorial theme, SEO-structured for articles and search traffic' ];
	} elseif ( $has( [ 'appointment', 'booking system', 'book a call', 'book an appointment', 'reservation system', 'schedule online', 'calendar booking', 'online booking', 'booking page', 'reserve a slot', 'scheduling page', 'therapy booking', 'consultation booking' ] ) ) {
		$rec = [ 'a Simple Site', '$115', 'with a Calendly or Acuity embed &mdash; or a custom booking engine in a Full Site ($300) if you need database-level control over slots, reminders, and payments' ];
	}

	if ( $rec ) {
		return $nl( [
			"For that, the closest fit is <strong>{$rec[0]}</strong> &mdash; {$rec[1]} &mdash; {$rec[2]}.",
			$related ? '' : null,
			$related ?: null,
			'',
			"Two quick things to lock it down: what&rsquo;s your rough budget, and is there a hard deadline? Say <strong>book</strong> any time for a free 30-min call.",
		] );
	}

	// 5) Pricing without a clear service yet.
	if ( $has( [ 'how much', 'price', 'pricing', 'cost', 'budget', ' rate', 'quote', 'estimate', 'afford', 'expensive', 'cheap' ] ) ) {
		return $nl( [
			"Here&rsquo;s the quick map &mdash; fixed prices, pay by Cash App, crypto, check or wire:",
			'',
			"&bull; <strong>Websites</strong> &mdash; Micro $40 &middot; Simple $115 &middot; Full $300 &middot; Pro+WebGL $600 &middot; SaaS $750",
			"&bull; <strong>AI</strong> &mdash; Prompt pack $25 &middot; Concierge bot $190 &middot; Custom agent $565 &middot; Agent+app $1,185",
			"&bull; <strong>SEO</strong> &mdash; Audit $30 &middot; Starter $75 &middot; Growth $115/mo",
			"&bull; <strong>Brand</strong> &mdash; Logo $30 &middot; Mini $115 &middot; Full $350",
			'',
			"Which is closest to what you need? Tell me that and your budget and I&rsquo;ll narrow it to one tier.",
		] );
	}

	// 6) Thanks / small-talk close.
	if ( $words <= 6 && $has( [ 'thank', 'thanks', ' thx', 'appreciate', 'cool', 'awesome', 'nice', 'great', ' ok ', 'okay', 'perfect', 'sounds good', 'got it' ] ) ) {
		return 'Anytime! When you&rsquo;re ready, book a free 30-min call at <a href="' . $book . '">' . $book . '</a> or email <strong>' . esc_html( $email ) . '</strong>. Describe your project whenever and I&rsquo;ll map it to a price on the spot.';
	}

	// 7) Fallback — smarter for long messages vs short dead-ends.
	if ( $words >= 15 ) {
		return $nl( [
			"That sounds like something worth scoping properly. Based on what you described, it&rsquo;s likely a custom build &mdash; the fastest way to get an accurate price is a 15-min call or a written summary.",
			'',
			"&bull; <strong>Book a free call</strong>: <a href=\"{$book}\">{$book}</a>",
			"&bull; <strong>Email a brief</strong>: <strong>" . esc_html( $email ) . "</strong> &mdash; I reply within 24 h with a scope and fixed price",
			'',
			"While you decide: is it primarily a <strong>website</strong>, an <strong>app</strong>, an <strong>AI tool</strong>, or <strong>brand / content work</strong>?",
		] );
	}
	return $nl( [
		"Got it. So I can point you to the right service and price, which of these is closest?",
		'',
		"&bull; a <strong>website or store</strong> &middot; an <strong>AI agent / chatbot</strong> &middot; a <strong>3D / AR experience</strong> &middot; <strong>SEO</strong> &middot; a <strong>brand</strong>",
		$related ? '' : null,
		$related ?: null,
		'',
		"Or just describe the project in a sentence and I&rsquo;ll quote a tier. Prefer to talk? <strong>857-316-5054</strong> or book at <a href=\"{$book}\">{$book}</a>.",
	] );
}

/**
 * Render the chat orb + drawer markup. Hooked at wp_footer.
 */
function pb_aurora_brainstorm_render() {
	?>
	<button type="button" class="pb-orb" data-pb-brainstorm-open aria-label="Open AI Chatbot">
		<span class="pb-orb__logo" aria-hidden="true">
			<svg viewBox="0 0 32 32" width="22" height="22" fill="none"><circle cx="16" cy="16" r="14" stroke="currentColor" stroke-width="2"/><circle cx="16" cy="16" r="5" fill="currentColor"/></svg>
		</span>
		<span class="pb-orb__lab">AI Chatbot</span>
	</button>
	<aside class="pb-brain" id="pb-brain" role="dialog" aria-modal="true" aria-labelledby="pb-brain-title" hidden>
		<div class="pb-brain__head">
			<div>
				<h3 id="pb-brain-title">AI Chatbot</h3>
				<p class="pb-brain__sub">Push your project to the extreme. Voice or text — pick.</p>
			</div>
			<button type="button" class="pb-brain__close" data-pb-brainstorm-close aria-label="Close">×</button>
		</div>
		<div class="pb-brain__log" data-pb-brain-log aria-live="polite">
			<div class="pb-brain__msg pb-brain__msg--bot">
				Hi &mdash; I&rsquo;m Photon, the Photon-Bounce concierge. I can help you scope a project and pick the right service: <strong>web builds</strong> (landing page to full SaaS / WebGL), <strong>AI agents &amp; chatbots</strong>, <strong>3D / AR</strong>, <strong>SEO / AEO</strong>, or <strong>brand</strong>. Tell me what you&rsquo;re building and your budget, and I&rsquo;ll recommend a tier and the next step.
			</div>
		</div>
		<form class="pb-brain__form" data-pb-brain-form data-pb-rest="<?php echo esc_url( rest_url( 'pb/v1/brainstorm' ) ); ?>">
			<div class="pb-brain__chips" aria-label="Quick questions">
				<button type="button" class="pb-brain__chip" data-chip="How much does a website cost?">💻 Website pricing</button>
				<button type="button" class="pb-brain__chip" data-chip="Tell me about AI agents and chatbots">🤖 AI agents</button>
				<button type="button" class="pb-brain__chip" data-chip="Show me your portfolio and past work">🎨 Portfolio</button>
				<button type="button" class="pb-brain__chip" data-chip="What payment methods do you accept?">💳 Payment</button>
			</div>
			<textarea class="pb-brain__input" data-pb-brain-input rows="2" placeholder="What do you want to build? (Shift+Enter for newline, Ctrl+/ to toggle)" required maxlength="2000"></textarea>
			<div class="pb-brain__row">
				<button type="button" class="pb-brain__mic" data-pb-brain-mic aria-label="Talk instead of type" title="Voice input"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 1.5a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0v-7a3 3 0 0 0-3-3z"/><path d="M19 11v.5a7 7 0 0 1-14 0V11"/><line x1="12" y1="18.5" x2="12" y2="22"/><line x1="8" y1="22" x2="16" y2="22"/></svg></button>
				<button type="submit" class="pb-btn pb-btn--primary pb-btn--sm">Send</button>
			</div>
		</form>
		<p class="pb-brain__legal">No data stored beyond this session unless you book a call.</p>
	</aside>
	<?php
}
add_action( 'wp_footer', 'pb_aurora_brainstorm_render', 50 );
