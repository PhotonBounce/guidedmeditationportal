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
