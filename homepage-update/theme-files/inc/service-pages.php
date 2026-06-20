<?php
/**
 * Photon-Bounce — SEO service landing pages.
 *
 * Creates 6 indexable, lead-capturing service pages (web, AI agents, 3D/WebGL,
 * SEO, AEO, brand) automatically on first load after deploy — no wp-admin needed —
 * and renders them through a dedicated template via `template_include`, so they
 * never fall back to the homepage layout. FAQ schema is emitted per page for AEO.
 *
 * Loaded from inc/schema.php (require_once at file end), so it activates wherever
 * the theme loads without editing functions.php.
 */
if ( ! defined( 'ABSPATH' ) ) { exit; }

/** Single source of truth for every service page. */
function pb_aurora_service_pages() {
	static $s = null;
	if ( $s !== null ) { return $s; }

	$s = [
		'web-development' => [
			'h1'      => 'Custom Web App & Website Development',
			'title'   => 'Web Development | Photon Bounce',
			'eyebrow' => 'WEB BUILDS',
			'meta'    => 'Fixed-price web development — from a one-page launch to a full SaaS app. Hand-built, fast (Lighthouse 90+), SEO-ready, and 100% owned by you.',
			'intro'   => 'Fast, fixed-price websites and web apps — from a one-page launch to a full SaaS platform. Hand-built, performance-tuned, SEO-mapped, and yours to keep: you get the source and full ownership, no lock-in and no mandatory monthly fee.',
			'bullets' => [
				'One-page sites, marketing sites, e-commerce, and full SaaS dashboards',
				'Mobile-first, accessible, and tuned for Core Web Vitals (Lighthouse 90+)',
				'On-page SEO and schema baked in from day one',
				'Fixed price with a written scope — no hourly drift or surprise invoices',
				'Full handoff: you own the code and the source files',
			],
			'prices'  => [
				[ 'Micro Page', '$40', 'One-page launch, mobile-first, lead form' ],
				[ 'Simple Site', '$115', '3–5 pages, contact, analytics, SEO basics' ],
				[ 'Full Site', '$300', 'Up to 10 pages, CMS, animation, on-page SEO' ],
				[ 'Pro Site + WebGL', '$600', 'Full site plus a signature 3D / interactive hero' ],
				[ 'SaaS / App', '$750', 'Auth, dashboard, custom backend, payments' ],
			],
			'faqs'    => [
				[ 'How much does a custom website cost?', 'Sites are fixed-price: a one-page Micro build is $40, a full multi-page site is $300, and a SaaS app with auth and a backend starts at $750. You get a written scope before any work starts.' ],
				[ 'How long does a website take?', 'A Micro page ships in a few days, a full site in about two weeks, and a SaaS build in 4–8 weeks. You get weekly demo links so you always see progress.' ],
				[ 'Do I own the code?', 'Yes, 100%. You receive the full source and a handoff package — no proprietary lock-in and no required monthly fee.' ],
				[ 'Can you work with my existing site or CMS?', 'Yes — fixes, redesigns, and feature additions to existing WordPress, Webflow, or custom-code sites are scoped exactly like new builds. A quick audit first tells me what the existing code needs.' ],
				[ 'What is included in the handoff package?', 'Full source code, database export if applicable, all login credentials, a short documentation file, and any brand or style assets used. Nothing stays on my end that belongs to you.' ],
			],
		],
		'ai-agents' => [
			'h1'      => 'AI Agents & Chatbot Development',
			'title'   => 'AI Agent & Chatbot Development | Photon Bounce',
			'eyebrow' => 'AI BUILDS',
			'meta'    => 'Custom AI agents and chatbots for your business — LLM + RAG trained on your content, voice or text, from $190. Fixed price, fully owned.',
			'intro'   => 'Custom AI agents and chatbots that actually know your business — built on modern LLMs with retrieval (RAG) over your own content, in text or voice. From a drop-in concierge bot to a full custom agent wired into your tools and data.',
			'bullets' => [
				'LLM agents with RAG over your docs, site, and knowledge base',
				'Text or voice — web widget, WhatsApp, or phone',
				'Lead capture, booking, and support automation built in',
				'Connected to your tools (CRM, calendar, email, APIs)',
				'Private deployment options — your data stays yours',
			],
			'prices'  => [
				[ 'Prompt Pack', '$25', 'Tuned prompts + a starter playbook' ],
				[ 'Concierge Bot', '$190', 'Drop-in site chatbot trained on your content' ],
				[ 'Custom Agent', '$565', 'LLM agent with RAG + tool/data integrations' ],
				[ 'Agent + App', '$1,185', 'Agent plus a full app/dashboard around it' ],
			],
			'faqs'    => [
				[ 'How much does an AI chatbot cost?', 'A drop-in Concierge bot trained on your content is $190; a Custom Agent with retrieval and tool integrations is $565; an agent bundled with a full app is $1,185. All fixed-price.' ],
				[ 'Can the bot use my own documents?', 'Yes — that is the point. It uses retrieval (RAG) over your site, documents, and knowledge base so answers are grounded in your real content, not generic guesses.' ],
				[ 'Can it talk out loud / take voice?', 'Yes. Agents can run as text, or as voice with speech-in and natural speech-out (ElevenLabs), on the web or over the phone/WhatsApp.' ],
				[ 'How do you train the bot on my content?', 'Your documents, site pages, and knowledge base are chunked and indexed into a vector store (RAG). The agent queries that index before every reply, so every answer is grounded in your actual content rather than generic LLM knowledge.' ],
				[ 'What language models does it use?', 'Typically GPT-4o-mini for speed and cost, or GPT-4o for complex reasoning. We can also run on Claude, Gemini, or a self-hosted model if your data-residency requirements call for it.' ],
			],
		],
		'3d-webgl' => [
			'h1'      => '3D & WebGL Website Development',
			'title'   => '3D & WebGL Web Experiences | Photon Bounce',
			'eyebrow' => '3D · WEBGL',
			'meta'    => 'Interactive 3D and WebGL websites — Three.js heroes, product configurators, and physics demos that run in the browser. From $600.',
			'intro'   => 'Interactive 3D experiences that run in any browser, no plugin or install. Three.js / WebGL heroes, product configurators, particle and physics demos, and AR research apps — the kind of thing that makes a brand look years ahead of its competitors.',
			'bullets' => [
				'Three.js / WebGL heroes and full interactive scenes',
				'Product configurators and 3D model viewers',
				'Particle systems and real-time physics demos',
				'AR / mixed-reality research apps (Unity, WebXR)',
				'Performance-tuned to stay smooth on phones',
			],
			'prices'  => [
				[ 'Pro Site + WebGL', '$600', 'Full site with a signature 3D / WebGL hero' ],
				[ 'Custom 3D / AR', '$1,500+', 'Configurators, physics demos, AR research apps' ],
			],
			'faqs'    => [
				[ 'How much does a 3D / WebGL website cost?', 'A full site with a signature WebGL hero is $600; larger custom 3D work — configurators, physics demos, AR apps — is scoped from $1,500. Fixed price after a short discovery call.' ],
				[ 'Will it run on mobile?', 'Yes. Every 3D build is performance-tuned and tier-scaled so it stays smooth on phones and low-end devices, with graceful fallbacks.' ],
				[ 'Do you build AR/VR too?', 'Yes — WebXR in-browser AR, plus Unity-based AR/VR research apps for HoloLens, Quest, and mobile.' ],
				[ 'Can I embed 3D into my existing site?', '3D components are delivered as a standalone JS embed or iframe that drops into any site — WordPress, Webflow, Squarespace, or custom — without rebuilding the rest of your page.' ],
				[ 'What 3D file formats do you accept?', 'glTF / GLB is the preferred web format. We also accept FBX, OBJ, STL, and native Blender files and handle the optimization, compression, and export pipeline.' ],
			],
		],
		'seo' => [
			'h1'      => 'SEO Services for Founders & Small Business',
			'title'   => 'SEO Services | Photon Bounce',
			'eyebrow' => 'SEO',
			'meta'    => 'Practical SEO that ranks you for buyer-intent searches — technical fixes, schema, content, and internal linking. Audits from $30, Growth from $115/mo.',
			'intro'   => 'SEO with no fluff and no long lock-in contracts. Technical fixes, schema, a real keyword/intent map, on-page optimization, and content that targets the searches your buyers actually make — so you rank for the queries that turn into leads.',
			'bullets' => [
				'Technical audit: crawlability, speed, Core Web Vitals, indexation',
				'Keyword + intent map focused on commercial, lead-driving searches',
				'On-page optimization, internal linking, and JSON-LD schema',
				'Local SEO (Google Business Profile) for nearby buyers',
				'Plain monthly reporting — you see exactly what moved',
			],
			'prices'  => [
				[ 'SEO Audit', '$30', 'Technical + on-page audit, keyword map, action list' ],
				[ 'Starter', '$75', 'Audit + on-page fixes, schema, sitemap, linking' ],
				[ 'Growth', '$115/mo', 'Ongoing content, links, monitoring, reporting' ],
				[ 'Authority', '$275/mo', 'Aggressive content + outreach for competitive niches' ],
			],
			'faqs'    => [
				[ 'How much does SEO cost?', 'A one-off technical audit is $30, a Starter on-page package is $75, and ongoing Growth SEO is $115/mo. No long lock-in — month to month.' ],
				[ 'How long until I see results?', 'Technical and on-page wins can show in weeks; competitive content rankings typically take 3–6 months. The audit tells you which wins are fastest for your site.' ],
				[ 'Do you do local SEO?', 'Yes — Google Business Profile setup and local schema are part of the work, which is often the fastest path to nearby, high-intent leads.' ],
				[ 'Do you do link building?', 'Growth and Authority tiers include targeted outreach and link acquisition. Starter focuses on technical and on-page fixes first — link building only delivers full value once the foundation is clean.' ],
				[ 'What does the monthly SEO report include?', 'Keyword ranking changes, page-level traffic from Search Console, Core Web Vitals snapshot, and the exact tasks completed that month — one clear page, not an auto-generated wall of numbers.' ],
			],
		],
		'aeo' => [
			'h1'      => 'AEO — Get Cited by ChatGPT, Perplexity & Google AI',
			'title'   => 'Answer Engine Optimization (AEO) | Photon Bounce',
			'eyebrow' => 'AEO',
			'meta'    => 'Answer Engine Optimization so AI assistants cite your business. FAQ + speakable schema, LLM-friendly content. Audit $40, Setup $100.',
			'intro'   => 'Answer Engine Optimization gets your business surfaced and cited inside AI assistants — ChatGPT, Perplexity, Google AI Overviews. We structure your content with FAQ and speakable schema and clear, quotable answers so the engines pull you in by name.',
			'bullets' => [
				'FAQ + speakable schema so AI engines can quote you cleanly',
				'Quotable, factual answer blocks for your key buyer questions',
				'llm.txt + crawler access tuned for GPTBot, PerplexityBot, etc.',
				'Citation tracking across ChatGPT, Perplexity, and Gemini',
				'Works alongside classic SEO — same content, more surfaces',
			],
			'prices'  => [
				[ 'AEO Audit', '$40', 'Schema + speakable + FAQ + citation audit' ],
				[ 'AEO Setup', '$100', 'FAQ/speakable schema + LLM-friendly content blocks' ],
				[ 'AEO Retainer', '$90/mo', 'Track citations across engines and iterate' ],
			],
			'faqs'    => [
				[ 'What is AEO?', 'Answer Engine Optimization is the practice of structuring your content so AI assistants like ChatGPT and Perplexity cite your business when people ask related questions — the AI-era complement to SEO.' ],
				[ 'How much does AEO cost?', 'An AEO audit is $40, a full setup (FAQ + speakable schema and LLM-friendly content) is $100, and ongoing citation tracking is $90/mo.' ],
				[ 'Is AEO different from SEO?', 'They overlap but differ: SEO ranks you in search results; AEO gets you quoted inside AI answers. The same well-structured content can win both.' ],
				[ 'How do I know if AI engines are citing me?', 'The AEO Retainer tracks citations monthly by querying ChatGPT, Perplexity, and Gemini with the questions most relevant to your business and recording whether your brand appears in each answer.' ],
				[ 'Does my site need structured data already?', 'No — FAQ, speakable, and entity schema are all added as part of the AEO Setup. We also verify that AI crawler access (GPTBot, PerplexityBot, ClaudeBot) is not accidentally blocked in your robots.txt.' ],
			],
		],
		'branding' => [
			'h1'      => 'Brand Identity & Design',
			'title'   => 'Brand Identity & Logo Design | Photon Bounce',
			'eyebrow' => 'BRAND',
			'meta'    => 'Brand identity and logo design for founders — logo, palette, type, and guidelines. Logo from $30, full identity systems from $350.',
			'intro'   => 'A brand identity that looks like you mean business — logo, color, type, and the guidelines to keep it consistent everywhere. From a clean single logo to a full identity system with usage rules, templates, and assets.',
			'bullets' => [
				'Logo design with usable file formats for web and print',
				'Color palette and typography system',
				'Brand guidelines so everything stays consistent',
				'Social, deck, and document templates',
				'Pairs cleanly with a matching website build',
			],
			'prices'  => [
				[ 'Logo', '$30', 'A clean, original logo in usable formats' ],
				[ 'Mini Identity', '$115', 'Logo, palette, type, and basic guidelines' ],
				[ 'Full Identity', '$350', 'Full system: guidelines, templates, assets' ],
			],
			'faqs'    => [
				[ 'How much does a logo cost?', 'A standalone logo is $30. A Mini Identity (logo, palette, type, basic guidelines) is $115, and a Full Identity system is $350.' ],
				[ 'What files do I get?', 'Production-ready files for web and print (vector + raster), plus a short guideline doc so your brand stays consistent everywhere.' ],
				[ 'Can you do my brand and website together?', 'Yes, and that is usually the best value — a brand build pairs directly with a website build so the identity ships live, not just in a PDF.' ],
				[ 'How long does a brand project take?', 'A Logo turnaround is 2–4 days. A Mini Identity is about a week. A Full Identity with templates, usage guidelines, and social assets typically takes 2–3 weeks.' ],
				[ 'Do I own the copyright to my logo?', 'Yes, outright. Copyright transfers to you with the final delivery. No license fees, no royalties, and no restriction on where or how you use it — print, web, merchandise, whatever you need.' ],
			],
		],
	];
	return $s;
}

/** Seed the 6 pages once, after deploy, with no wp-admin needed. */
add_action( 'init', 'pb_aurora_seed_service_pages', 20 );
function pb_aurora_seed_service_pages() {
	if ( get_option( 'pb_service_pages_v1' ) === 'done' ) { return; }
	foreach ( pb_aurora_service_pages() as $slug => $svc ) {
		if ( ! get_page_by_path( $slug ) ) {
			wp_insert_post( [
				'post_type'    => 'page',
				'post_status'  => 'publish',
				'post_name'    => $slug,
				'post_title'   => $svc['h1'],
				'post_content' => $svc['intro'],
				'post_excerpt' => wp_html_excerpt( $svc['meta'], 158, '' ),
			] );
		}
	}
	update_option( 'pb_service_pages_v1', 'done' );
}

/** Route any service page to our dedicated template (no generic page.php exists). */
add_filter( 'template_include', 'pb_aurora_service_template', 20 );
function pb_aurora_service_template( $template ) {
	if ( is_page() ) {
		$slug = get_post_field( 'post_name', get_queried_object_id() );
		if ( isset( pb_aurora_service_pages()[ $slug ] ) ) {
			$custom = locate_template( 'template-service.php' );
			if ( $custom ) { return $custom; }
		}
	}
	return $template;
}

/** Per-page <title> for service pages. */
add_filter( 'document_title_parts', 'pb_aurora_service_title', 20 );
function pb_aurora_service_title( $parts ) {
	if ( is_page() ) {
		$slug = get_post_field( 'post_name', get_queried_object_id() );
		$svc  = pb_aurora_service_pages();
		if ( isset( $svc[ $slug ]['title'] ) ) {
			$parts['title'] = $svc[ $slug ]['title'];
			unset( $parts['site'], $parts['tagline'] );
		}
	}
	return $parts;
}

/** Service + Offer JSON-LD per service page (rich results for service searches). */
add_action( 'wp_head', 'pb_aurora_service_schema', 8 );
function pb_aurora_service_schema() {
	if ( ! is_page() ) { return; }
	$slug = get_post_field( 'post_name', get_queried_object_id() );
	$svc  = pb_aurora_service_pages();
	if ( ! isset( $svc[ $slug ] ) ) { return; }
	$s        = $svc[ $slug ];
	$site_url = home_url( '/' );
	$offers   = [];
	foreach ( $s['prices'] as $p ) {
		$raw = preg_replace( '/[^0-9.]/', '', $p[1] );
		if ( $raw ) {
			$offers[] = [
				'@type'         => 'Offer',
				'name'          => $p[0],
				'description'   => $p[2],
				'price'         => $raw,
				'priceCurrency' => 'USD',
				'availability'  => 'https://schema.org/InStock',
				'url'           => $site_url . $slug . '/',
			];
		}
	}
	$payload = array_filter( [
		'@context'    => 'https://schema.org',
		'@type'       => 'Service',
		'name'        => $s['h1'],
		'description' => $s['intro'],
		'url'         => $site_url . $slug . '/',
		'provider'    => [ '@id' => $site_url . '#organization' ],
		'areaServed'  => 'Worldwide',
		'offers'      => $offers ?: null,
	] );
	echo "\n<script type=\"application/ld+json\">" . wp_json_encode( $payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . "</script>\n";
}

/** FAQ JSON-LD per service page (AEO). */
add_action( 'wp_head', 'pb_aurora_service_faq_schema', 8 );
function pb_aurora_service_faq_schema() {
	if ( ! is_page() ) { return; }
	$slug = get_post_field( 'post_name', get_queried_object_id() );
	$svc  = pb_aurora_service_pages();
	if ( empty( $svc[ $slug ]['faqs'] ) ) { return; }
	$entities = [];
	foreach ( $svc[ $slug ]['faqs'] as $f ) {
		$entities[] = [
			'@type'          => 'Question',
			'name'           => $f[0],
			'acceptedAnswer' => [ '@type' => 'Answer', 'text' => $f[1] ],
		];
	}
	$payload = [
		'@context'   => 'https://schema.org',
		'@type'      => 'FAQPage',
		'mainEntity' => $entities,
	];
	echo "\n<script type=\"application/ld+json\">" . wp_json_encode( $payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . "</script>\n";
}

/** Render the rich service-page body (called by template-service.php). */
function pb_aurora_render_service_page( $slug ) {
	$svc = pb_aurora_service_pages();
	if ( ! isset( $svc[ $slug ] ) ) {
		echo '<section class="pb-section"><div class="pb-section__head"><h1 class="pb-aurora-text">Services</h1></div></section>';
		return;
	}
	$s     = $svc[ $slug ];
	$quote = esc_url( home_url( '/quote/' ) );
	$book  = esc_url( home_url( '/book/' ) );
	?>
	<section class="pb-section pb-service-page" id="service" data-pb-reveal>
		<div class="pb-section__head">
			<p class="pb-eyebrow"><span class="pb-pulse-dot"></span><?php echo esc_html( $s['eyebrow'] ); ?></p>
			<h1 class="pb-aurora-text"><?php echo esc_html( $s['h1'] ); ?></h1>
			<p class="pb-lead"><?php echo esc_html( $s['intro'] ); ?></p>
			<div class="pb-hero__ctas" style="justify-content:center;margin-top:18px;">
				<a class="pb-btn pb-btn--primary" href="<?php echo $quote; ?>" data-pb-track="service-quote-<?php echo esc_attr( $slug ); ?>">Get a fixed-price quote <span aria-hidden="true">&rarr;</span></a>
				<a class="pb-btn pb-btn--ghost" href="<?php echo $book; ?>">Book a free call</a>
			</div>
		</div>

		<div class="pb-service-page__grid" style="max-width:1080px;margin:40px auto 0;display:grid;gap:40px;grid-template-columns:1fr;">
			<div>
				<h2 class="pb-aurora-text" style="font-size:1.4rem;">What you get</h2>
				<ul class="pb-service-page__list" style="margin-top:14px;line-height:1.9;padding-left:1.1em;">
					<?php foreach ( $s['bullets'] as $b ) : ?>
						<li><?php echo esc_html( $b ); ?></li>
					<?php endforeach; ?>
				</ul>
			</div>

			<div>
				<h2 class="pb-aurora-text" style="font-size:1.4rem;">Pricing</h2>
				<div class="pb-service-page__prices" style="margin-top:14px;border:1px solid rgba(255,255,255,.1);border-radius:14px;overflow:hidden;">
					<?php foreach ( $s['prices'] as $i => $p ) : ?>
						<div style="display:flex;justify-content:space-between;gap:16px;align-items:baseline;padding:14px 18px;<?php echo $i ? 'border-top:1px solid rgba(255,255,255,.08);' : ''; ?>">
							<div>
								<strong style="display:block;"><?php echo esc_html( $p[0] ); ?></strong>
								<span style="opacity:.7;font-size:.9rem;"><?php echo esc_html( $p[2] ); ?></span>
							</div>
							<span class="pb-aurora-text" style="font-weight:700;white-space:nowrap;"><?php echo esc_html( $p[1] ); ?></span>
						</div>
					<?php endforeach; ?>
				</div>
				<p style="opacity:.7;font-size:.85rem;margin-top:10px;">Fixed prices. Pay by Cash App, crypto, check or wire.</p>
			</div>
		</div>

		<div class="pb-service-page__faq" style="max-width:820px;margin:56px auto 0;">
			<h2 class="pb-aurora-text" style="font-size:1.4rem;text-align:center;">Common questions</h2>
			<?php foreach ( $s['faqs'] as $f ) : ?>
				<div style="margin-top:18px;border-bottom:1px solid rgba(255,255,255,.08);padding-bottom:16px;">
					<h3 style="font-size:1.05rem;margin:0 0 6px;"><?php echo esc_html( $f[0] ); ?></h3>
					<p style="margin:0;opacity:.85;line-height:1.7;"><?php echo esc_html( $f[1] ); ?></p>
				</div>
			<?php endforeach; ?>
		</div>
	</section>

	<section class="pb-section pb-cta" id="contact" style="margin-top:24px;">
		<div class="pb-cta__inner">
			<h2 class="pb-aurora-text">Ready to start?</h2>
			<p>Tell me the project and get a fixed price — usually within 24 hours.</p>
			<div class="pb-cta__buttons">
				<a class="pb-btn pb-btn--primary" href="<?php echo $quote; ?>" data-pb-track="service-cta-<?php echo esc_attr( $slug ); ?>">Get a quote &rarr;</a>
				<a class="pb-btn pb-btn--ghost" href="<?php echo $book; ?>">Book a free call</a>
			</div>
		</div>
	</section>
	<?php
}
