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
