<?php
/**
 * Photon-Bounce Aurora â€” Structured data (Schema.org / JSON-LD)
 * Emits Organization + Person on every page, BreadcrumbList,
 * ItemList for portfolio, Article for posts, Service for service pages,
 * WebSite + SearchAction on home.
 */
if ( ! defined( 'ABSPATH' ) ) { exit; }

add_action( 'wp_head', 'pb_aurora_schema_emit', 5 );
function pb_aurora_schema_emit() {
	$site_url   = home_url( '/' );
	$site_name  = get_bloginfo( 'name' );
	$site_desc  = get_bloginfo( 'description' );
	$logo_id    = get_theme_mod( 'custom_logo' );
	$logo_url   = $logo_id ? wp_get_attachment_image_url( $logo_id, 'full' ) : '';
	$email      = get_theme_mod( 'pb_contact_email', 'hello@photon-bounce.com' );
	$phone      = get_theme_mod( 'pb_contact_phone', '857-316-5054' );

	$graph = [];

	// Organization
	$graph[] = array_filter( [
		'@type'         => 'Organization',
		'@id'           => $site_url . '#organization',
		'name'          => $site_name,
		'url'           => $site_url,
		'description'   => $site_desc,
		'logo'          => $logo_url ?: null,
		'email'         => $email,
		'telephone'     => $phone,
		'sameAs'        => array_filter( [
			'https://cash.app/$photonbounce',
			get_theme_mod( 'pb_social_github', '' ),
			get_theme_mod( 'pb_social_linkedin', '' ),
			get_theme_mod( 'pb_social_twitter', '' ),
			get_theme_mod( 'pb_social_instagram', '' ),
		] ),
	] );

	// Person (Dmitriy)
	$graph[] = [
		'@type'        => 'Person',
		'@id'          => $site_url . '#founder',
		'name'         => 'Dmitriy',
		'jobTitle'     => 'Multidisciplinary Developer Â· Designer Â· AI Engineer',
		'worksFor'     => [ '@id' => $site_url . '#organization' ],
		'email'        => $email,
		'telephone'    => $phone,
		'knowsAbout'   => [
			'Web Development', 'WordPress', 'Next.js', 'React', 'three.js',
			'Artificial Intelligence', 'LLM Engineering', 'RAG', 'Computer Vision',
			'UI/UX Design', 'Branding', '3D / WebGL', 'AR/VR', 'SaaS Architecture',
		],
	];

	// WebSite + SearchAction (only on home)
	if ( is_front_page() || is_home() ) {
		$graph[] = [
			'@type'           => 'WebSite',
			'@id'             => $site_url . '#website',
			'url'             => $site_url,
			'name'            => $site_name,
			'description'     => $site_desc,
			'publisher'       => [ '@id' => $site_url . '#organization' ],
			'potentialAction' => [
				'@type'       => 'SearchAction',
				'target'      => [
					'@type'       => 'EntryPoint',
					'urlTemplate' => $site_url . '?s={search_term_string}',
				],
				'query-input' => 'required name=search_term_string',
			],
		];
	}

	// BreadcrumbList
	$crumbs = pb_aurora_breadcrumbs();
	if ( count( $crumbs ) > 1 ) {
		$items = [];
		foreach ( $crumbs as $i => $c ) {
			$items[] = [
				'@type'    => 'ListItem',
				'position' => $i + 1,
				'name'     => $c['name'],
				'item'     => $c['url'],
			];
		}
		$graph[] = [
			'@type'           => 'BreadcrumbList',
			'itemListElement' => $items,
		];
	}

	// Article on single posts
	if ( is_single() && ! is_singular( 'pb_project' ) ) {
		$post = get_post();
		if ( $post ) {
			$graph[] = array_filter( [
				'@type'         => 'Article',
				'headline'      => get_the_title( $post ),
				'datePublished' => get_post_time( 'c', true, $post ),
				'dateModified'  => get_post_modified_time( 'c', true, $post ),
				'author'        => [ '@id' => $site_url . '#founder' ],
				'publisher'     => [ '@id' => $site_url . '#organization' ],
				'description'   => wp_strip_all_tags( get_the_excerpt( $post ) ),
				'image'         => get_the_post_thumbnail_url( $post, 'large' ) ?: null,
				'mainEntityOfPage' => get_permalink( $post ),
			] );
		}
	}

	// CreativeWork on single project
	if ( is_singular( 'pb_project' ) ) {
		$post = get_post();
		if ( $post ) {
			$graph[] = array_filter( [
				'@type'        => 'CreativeWork',
				'name'         => get_the_title( $post ),
				'description'  => wp_strip_all_tags( get_the_excerpt( $post ) ),
				'image'        => get_the_post_thumbnail_url( $post, 'large' ) ?: null,
				'creator'      => [ '@id' => $site_url . '#founder' ],
				'datePublished' => get_post_time( 'c', true, $post ),
				'url'          => get_permalink( $post ),
			] );
		}
	}

	// ItemList of portfolio on home or portfolio page
	if ( is_front_page() || is_page( 'portfolio' ) ) {
		$projects = get_posts( [ 'post_type' => 'pb_project', 'posts_per_page' => 24 ] );
		if ( $projects ) {
			$els = [];
			foreach ( $projects as $i => $p ) {
				$els[] = [
					'@type'    => 'ListItem',
					'position' => $i + 1,
					'url'      => get_permalink( $p ),
					'name'     => get_the_title( $p ),
				];
			}
			$graph[] = [
				'@type'           => 'ItemList',
				'name'            => 'Selected Work',
				'numberOfItems'   => count( $els ),
				'itemListElement' => $els,
			];
		}
	}

	// Service on services page
	if ( is_page( 'services' ) ) {
		$services = [
			[ 'SAAS Â· Web Â· SEO Â· SMM', 'Marketing sites, full SaaS dashboards, e-commerce, headless CMS builds.' ],
			[ 'UI Â· UX Â· Branding', 'Identity systems, illustrations, design systems, motion direction.' ],
			[ 'AI Application Development', 'LLM agents, RAG, embeddings, fine-tuning, NLP, computer vision.' ],
			[ '3D Â· AR/VR Â· Physics', 'Three.js / WebGL pipelines, AR research apps, mixed reality, particle and physics simulation.' ],
		];
		foreach ( $services as $s ) {
			$graph[] = [
				'@type'       => 'Service',
				'serviceType' => $s[0],
				'description' => $s[1],
				'provider'    => [ '@id' => $site_url . '#organization' ],
				'areaServed'  => 'Worldwide',
			];
		}
	}

	// FAQPage on home (mirrors front-page accordion).
	if ( ( is_front_page() || is_page( [ 'services', 'contact' ] ) ) && function_exists( 'pb_aurora_faq_items' ) ) {
		$faq_items = pb_aurora_faq_items();
		$entities  = [];
		foreach ( $faq_items as $f ) {
			$entities[] = [
				'@type'          => 'Question',
				'name'           => $f['q'],
				'acceptedAnswer' => [
					'@type' => 'Answer',
					'text'  => $f['a'],
				],
			];
		}
		$graph[] = [
			'@type'      => 'FAQPage',
			'mainEntity' => $entities,
		];
	}

	$payload = [
		'@context' => 'https://schema.org',
		'@graph'   => $graph,
	];

	echo "\n<script type=\"application/ld+json\">" . wp_json_encode( $payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . "</script>\n";
}

function pb_aurora_breadcrumbs() {
	$crumbs = [ [ 'name' => 'Home', 'url' => home_url( '/' ) ] ];
	if ( is_front_page() ) return $crumbs;
	if ( is_page() ) {
		$ancestors = array_reverse( get_post_ancestors( get_the_ID() ) );
		foreach ( $ancestors as $aid ) {
			$crumbs[] = [ 'name' => get_the_title( $aid ), 'url' => get_permalink( $aid ) ];
		}
		$crumbs[] = [ 'name' => get_the_title(), 'url' => get_permalink() ];
	} elseif ( is_singular( 'pb_project' ) ) {
		$crumbs[] = [ 'name' => 'Portfolio', 'url' => home_url( '/portfolio/' ) ];
		$crumbs[] = [ 'name' => get_the_title(), 'url' => get_permalink() ];
	} elseif ( is_post_type_archive( 'pb_project' ) ) {
		$crumbs[] = [ 'name' => 'Portfolio', 'url' => home_url( '/portfolio/' ) ];
	} elseif ( is_single() ) {
		$crumbs[] = [ 'name' => 'Blog', 'url' => home_url( '/blog/' ) ];
		$crumbs[] = [ 'name' => get_the_title(), 'url' => get_permalink() ];
	} elseif ( is_home() ) {
		$crumbs[] = [ 'name' => 'Blog', 'url' => home_url( '/blog/' ) ];
	}
	return $crumbs;
}

// Open Graph + Twitter cards
add_action( 'wp_head', 'pb_aurora_og_tags', 6 );
function pb_aurora_og_tags() {
	$site = get_bloginfo( 'name' );
	$desc = get_bloginfo( 'description' );
	$url  = home_url( '/' );
	$img  = '';
	if ( is_singular() ) {
		$post = get_post();
		$url  = get_permalink( $post );
		$desc = wp_strip_all_tags( get_the_excerpt( $post ) ?: $desc );
		$img  = get_the_post_thumbnail_url( $post, 'large' ) ?: '';
	}
	$logo_id = get_theme_mod( 'custom_logo' );
	if ( ! $img && $logo_id ) {
		$img = wp_get_attachment_image_url( $logo_id, 'full' );
	}
	$title = wp_get_document_title();

	// Always provide a branded SVG OG image fallback when no featured image is set.
	if ( ! $img ) {
		$img = home_url( '/og-image.svg?t=' . rawurlencode( $title ) . '&s=' . rawurlencode( $desc ) );
	}

	echo "\n<meta property=\"og:site_name\" content=\"" . esc_attr( $site ) . "\">";
	echo "\n<meta property=\"og:title\" content=\"" . esc_attr( $title ) . "\">";
	echo "\n<meta property=\"og:description\" content=\"" . esc_attr( $desc ) . "\">";
	echo "\n<meta property=\"og:url\" content=\"" . esc_url( $url ) . "\">";
	echo "\n<meta property=\"og:type\" content=\"" . ( is_singular( 'post' ) ? 'article' : 'website' ) . "\">";
	echo "\n<meta property=\"og:locale\" content=\"en_US\">";
	if ( $img ) echo "\n<meta property=\"og:image\" content=\"" . esc_url( $img ) . "\">";
	echo "\n<meta name=\"twitter:card\" content=\"summary_large_image\">";
	echo "\n<meta name=\"twitter:site\" content=\"@PhotonBounce\">";
	echo "\n<meta name=\"twitter:title\" content=\"" . esc_attr( $title ) . "\">";
	echo "\n<meta name=\"twitter:description\" content=\"" . esc_attr( $desc ) . "\">";
	if ( $img ) echo "\n<meta name=\"twitter:image\" content=\"" . esc_url( $img ) . "\">";
	if ( is_singular() ) {
		echo "\n<meta property=\"article:author\" content=\"https://www.linkedin.com/in/photon-bounce/\">";
		echo "\n<meta property=\"article:publisher\" content=\"https://www.linkedin.com/in/photon-bounce/\">";
	}
	echo "\n";
}


// Meta description + canonical + robots — only when no SEO plugin already emits them.
add_action( 'wp_head', 'pb_aurora_meta_basics', 1 );
function pb_aurora_meta_basics() {
	if ( defined( 'WPSEO_VERSION' ) || defined( 'RANK_MATH_VERSION' ) || defined( 'SEOPRESS_VERSION' ) || defined( 'AIOSEO_VERSION' ) ) {
		return; // a dedicated SEO plugin owns these tags
	}
	// NOTE: the meta description is already emitted in functions.php — we only add the
	// canonical + robots tags that were missing, so the description is never duplicated.
	$canonical = '';
	if ( is_singular() ) {
		$canonical = get_permalink();
	} elseif ( is_front_page() ) {
		$canonical = home_url( '/' );
	} elseif ( is_post_type_archive() ) {
		$canonical = get_post_type_archive_link( get_post_type() );
	}
	if ( $canonical ) {
		echo "\n<link rel=\"canonical\" href=\"" . esc_url( $canonical ) . "\">";
	}
	echo "\n<meta name=\"robots\" content=\"index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1\">\n";
}

/* ================================================================
   ENHANCED SCHEMA — LocalBusiness, HowTo, Speakable
   ================================================================ */

add_action( 'wp_head', 'pb_aurora_schema_enhanced', 7 );
function pb_aurora_schema_enhanced() {
	$site_url = home_url( '/' );
	$graph    = [];

	// LocalBusiness (for local SEO + trust signals)
	$graph[] = [
		'@type'       => 'LocalBusiness',
		'@id'         => $site_url . '#localbusiness',
		'name'        => 'Photon Bounce',
		'description' => 'Custom web apps, AI integrations, 3D/AR experiences, and SaaS platforms for founders and research teams.',
		'url'         => $site_url,
		'telephone'   => '+1-857-316-5054',
		'email'       => 'pb@photon-bounce.com',
		'priceRange'  => '$$$',
		'areaServed'  => 'Worldwide',
		'address'     => [
			'@type'           => 'PostalAddress',
			'addressLocality' => 'Boston',
			'addressRegion'   => 'MA',
			'addressCountry'  => 'US',
		],
		'geo' => [
			'@type'     => 'GeoCoordinates',
			'latitude'  => '42.3601',
			'longitude' => '-71.0589',
		],
		'openingHoursSpecification' => [
			'@type'     => 'OpeningHoursSpecification',
			'dayOfWeek' => [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday' ],
			'opens'     => '09:00',
			'closes'    => '18:00',
		],
		'sameAs' => array_filter( [
			'https://cash.app/$photonbounce',
			get_theme_mod( 'pb_social_linkedin', 'https://www.linkedin.com/in/photon-bounce/' ),
			get_theme_mod( 'pb_social_twitter', 'https://x.com/PhotonBounce' ),
			get_theme_mod( 'pb_social_youtube', 'https://www.youtube.com/channel/UCWXPtwnZcE5UVzbNv2R0yqg' ),
		] ),
	];

	// HowTo for the 4-step process (homepage only)
	if ( is_front_page() || is_home() ) {
		$graph[] = [
			'@type'       => 'HowTo',
			'name'        => 'How to work with Photon Bounce',
			'description' => 'Our 4-step process from discovery to launch.',
			'totalTime'   => 'P4W',
			'step'        => [
				[
					'@type' => 'HowToStep',
					'name'  => 'Discovery',
					'text'  => '30-min free call. Goals, audience, constraints, success metrics. You leave with a one-pager scope.',
					'url'   => $site_url . '#process',
				],
				[
					'@type' => 'HowToStep',
					'name'  => 'Scope & Estimate',
					'text'  => 'Fixed-price phases, written deliverables. No hourly drift, no surprise invoices, no committee.',
					'url'   => $site_url . '#process',
				],
				[
					'@type' => 'HowToStep',
					'name'  => 'Build in Public',
					'text'  => "Weekly Friday demo links. Async updates. You feel progress; you don't have to chase it.",
					'url'   => $site_url . '#process',
				],
				[
					'@type' => 'HowToStep',
					'name'  => 'Launch & Iterate',
					'text'  => 'Hand-off package, source files, and a Care Plan if you want me on call.',
					'url'   => $site_url . '#process',
				],
			],
		];

		// Speakable markup (voice search / AI citation) — broadened selectors
		$graph[] = [
			'@type'     => 'WebPage',
			'@id'       => $site_url,
			'speakable' => [
				'@type'       => 'SpeakableSpecification',
				'cssSelector' => [
					'.pb-hero__title',
					'.pb-hero__sub',
					'.pb-section__head h2',
					'.pb-section__head p',
					'.pb-faq__q',
					'.pb-faq__a',
					'[data-speakable]',
				],
			],
		];

		// Service schema for homepage capabilities
		$services = [
			[ 'SAAS / Web / SEO / SMM', 'Responsive sites, performance-tuned, SEO-mapped, social-amplified. From marketing pages to full SaaS dashboards.' ],
			[ 'UI / UX / Branding', 'Identity systems, illustrations, technical diagrams, UI concepts — paired with hands-on UX research and product design.' ],
			[ 'AI Application Development', 'LLM agents, RAG, embeddings, fine-tuning, NLP, computer vision, generative imagery, recommender systems, automation.' ],
			[ '3D / AR/VR / Physics', 'Three.js / WebGL pipelines, AR research apps, mixed reality, particle and physics demos for science teams.' ],
		];
		foreach ( $services as $s ) {
			$graph[] = [
				'@type'       => 'Service',
				'serviceType' => $s[0],
				'description' => $s[1],
				'provider'    => [ '@id' => $site_url . '#organization' ],
				'areaServed'  => 'Worldwide',
				'offers'      => [
					'@type' => 'AggregateOffer',
					'priceCurrency' => 'USD',
				],
			];
		}
	}

	if ( empty( $graph ) ) {
		return;
	}

	echo "\n<script type=\"application/ld+json\">" . wp_json_encode( [
		'@context' => 'https://schema.org',
		'@graph'   => $graph,
	], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . "</script>\n";
}

// SEO service landing pages (loaded here so they activate without editing functions.php).
require_once __DIR__ . '/service-pages.php';

// Home FAQ accordion + FAQPage schema items.
require_once __DIR__ . '/faq.php';
