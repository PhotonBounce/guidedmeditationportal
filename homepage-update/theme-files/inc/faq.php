<?php
/**
 * Photon-Bounce Aurora — Home FAQ (accordion + FAQPage JSON-LD items).
 *
 * pb_aurora_faq_items() is consumed by schema.php to build the FAQPage schema.
 * pb_aurora_faq_render() is called from index.php to render the visible accordion.
 * Both are guarded against redeclaration so the live functions.php wins if it defines them.
 */
if ( ! defined( 'ABSPATH' ) ) { exit; }

if ( ! function_exists( 'pb_aurora_faq_items' ) ) {
	function pb_aurora_faq_items() {
		return [
			[
				'q' => 'How much does a website cost?',
				'a' => 'Sites are fixed-price: a one-page Micro build is $40, a full multi-page site is $300, and a SaaS app with auth and a backend starts at $750. You get a written scope before any work begins — no surprise invoices.',
			],
			[
				'q' => 'How does your fixed-price model work?',
				'a' => 'Every project starts with a free 15-minute discovery call. After that you receive a one-pager scope with a fixed price and timeline. The price does not change unless the scope does, and scope changes are always agreed in writing first.',
			],
			[
				'q' => 'What is included in every project?',
				'a' => 'Full source code, all login credentials, a handoff doc, and any brand or style assets created during the build. You own everything outright — no monthly licence fees, no proprietary lock-in.',
			],
			[
				'q' => 'How many revisions do I get?',
				'a' => 'Two full revision rounds are included in every fixed-price project. A revision round covers feedback on the entire deliverable — not per-file or per-element. Additional rounds can be added at an agreed rate before work starts.',
			],
			[
				'q' => 'Can you maintain my site after launch?',
				'a' => 'Yes — the $50/mo Care Plan includes hosting, security updates, plugin/dependency updates, uptime monitoring, and up to 2 hours of content or code edits per month.',
			],
			[
				'q' => 'Do you work with clients who are not technical?',
				'a' => 'Most clients are non-technical — founders, creators, researchers, or small-business owners. You describe what you want the product to do; I handle all the technical implementation and explain decisions in plain language.',
			],
			[
				'q' => 'Do you work with international clients?',
				'a' => 'Yes. All projects are managed remotely via async video, shared docs, and weekly demo links. Clients in North America, Europe, and Russia have all shipped projects without a single in-person meeting.',
			],
			[
				'q' => 'What payment methods do you accept?',
				'a' => 'Bank transfer, Cash App ($photonbounce), PayPal, and cryptocurrency (BTC, ETH, USDC). A 30–50 % deposit is collected before work starts; the remainder is due on delivery.',
			],
			[
				'q' => 'How long does a typical project take?',
				'a' => 'A Micro Page ships in 2–4 days, a Full Site in 1–2 weeks, an AI agent in 3–5 business days, and a SaaS app in 4–8 weeks. Every project includes a Friday demo link so you see progress each week.',
			],
			[
				'q' => 'How do I get started?',
				'a' => 'Book a free 15-minute discovery call at /book/, or just type your question in the chat below — the AI concierge can scope your project, answer pricing questions, and send you the booking link.',
			],
		];
	}
}

if ( ! function_exists( 'pb_aurora_faq_render' ) ) {
	function pb_aurora_faq_render() {
		$items = pb_aurora_faq_items();
		echo '<section class="pb-section pb-faq" id="faq" aria-labelledby="faq-heading">';
		echo '<div class="pb-container">';
		echo '<div class="pb-section__head" data-pb-reveal><h2 id="faq-heading">Frequently Asked Questions</h2></div>';
		echo '<div class="pb-faq__list">';
		foreach ( $items as $i => $item ) {
			$id = 'faq-item-' . $i;
			echo '<details class="pb-faq__item" data-pb-reveal>';
			echo '<summary class="pb-faq__q" id="' . esc_attr( $id ) . '">';
			echo esc_html( $item['q'] );
			echo '</summary>';
			echo '<div class="pb-faq__a">' . wp_kses_post( $item['a'] ) . '</div>';
			echo '</details>';
		}
		echo '</div>';
		echo '</div>';
		echo '</section>';
	}
}
