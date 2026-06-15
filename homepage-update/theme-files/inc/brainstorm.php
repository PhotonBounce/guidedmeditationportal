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
	$lower = strtolower( $msg );

	// Detect intent → recommend a real tier from the menu (no invented projects).
	$rec = null;
	$cat = function ( $needles ) use ( $lower ) { foreach ( (array) $needles as $n ) { if ( strpos( $lower, $n ) !== false ) { return true; } } return false; };
	if ( $cat( [ 'landing', 'one-pager', 'one page', 'one-page' ] ) ) {
		$rec = [ 'Web Builds — Micro Page', '$40', '/#pricing' ];
	} elseif ( $cat( [ 'shop', 'store', 'ecommerce', 'product page', 'sell online' ] ) ) {
		$rec = [ 'Web Builds — Full Site', '$300', '/#pricing' ];
	} elseif ( $cat( [ 'webgl', 'three.js', 'shader', 'particle', '3d hero' ] ) ) {
		$rec = [ 'Web Builds — Pro Site + WebGL', '$600', '/#pricing' ];
	} elseif ( $cat( [ 'saas', 'dashboard', 'portal', 'app with auth', 'user accounts' ] ) ) {
		$rec = [ 'Web Builds — SaaS / App', '$750', '/#pricing' ];
	} elseif ( $cat( [ 'agent', 'gpt', 'rag', 'chat', 'voicebot', 'concierge' ] ) ) {
		$rec = [ 'AI Builds — Custom Agent', '$565', '/#pricing' ];
	} elseif ( $cat( [ 'seo', 'rank', 'ranking', 'google' ] ) ) {
		$rec = [ 'SEO — Growth/mo', '$115/mo', '/#pricing' ];
	} elseif ( $cat( [ 'aeo', 'chatgpt cite', 'perplexity', 'llm citation' ] ) ) {
		$rec = [ 'AEO — Setup', '$100', '/#pricing' ];
	} elseif ( $cat( [ 'logo', 'brand', 'identity', 'rebrand' ] ) ) {
		$rec = [ 'Brand — Mini Identity', '$115', '/#pricing' ];
	} elseif ( $cat( [ 'invest', 'idea', 'startup', 'mvp' ] ) ) {
		$rec = [ 'Investment marketplace — 50 ideas, MVPs from ~$150', '/invest/' ];
	}

	$lines = [];
	$lines[] = "Got it. Here is what I think fits:";
	if ( $rec ) {
		if ( count( $rec ) === 3 ) {
			$lines[] = '• Closest tier: **' . $rec[0] . '** — ' . $rec[1] . ' (see ' . home_url( $rec[2] ) . ')';
		} else {
			$lines[] = '• Closest fit: **' . $rec[0] . '** (see ' . home_url( $rec[1] ) . ')';
		}
	} else {
		$lines[] = "• Tell me one of: landing page, full site, WebGL hero, SaaS, AI agent, SEO, brand, or pick from the investment marketplace.";
	}

	// Real RAG hits only — never invented projects.
	if ( ! empty( $context['links'] ) ) {
		$lines[] = "";
		$lines[] = "Stuff already on this site that might be relevant:";
		foreach ( array_slice( $context['links'], 0, 3 ) as $l ) {
			$lines[] = '• ' . ucfirst( str_replace( 'pb_', '', $l['type'] ) ) . ': "' . $l['title'] . '" — ' . $l['url'];
		}
	}

	$lines[] = "";
	$lines[] = "Two questions to lock scope:";
	$lines[] = "1. What budget range are you in? (under $100, $100-$500, $500-$1.5k, more)";
	$lines[] = "2. Hard deadline, or flexible?";
	$lines[] = "";
	$lines[] = "Or say **book** and I will hand you a 30-min call slot.";
	return implode( "\n", $lines );
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
			<textarea class="pb-brain__input" data-pb-brain-input rows="2" placeholder="What do you want to build? (Shift+Enter for newline)" required maxlength="2000"></textarea>
			<div class="pb-brain__row">
				<button type="button" class="pb-brain__mic" data-pb-brain-mic aria-label="Talk instead of type" title="Voice input">🎙</button>
				<button type="submit" class="pb-btn pb-btn--primary pb-btn--sm">Send</button>
			</div>
		</form>
		<p class="pb-brain__legal">No data stored beyond this session unless you book a call.</p>
	</aside>
	<?php
}
add_action( 'wp_footer', 'pb_aurora_brainstorm_render', 50 );
