<?php get_header(); ?>

<!-- ============================================
     HERO -- Three.js aurora + pitch
     ============================================ -->
<section class="pb-hero" id="pb-hero">
	<canvas class="pb-hero__canvas" aria-hidden="true" data-engine="three.js r160"></canvas>
	<div class="pb-hero__grid" aria-hidden="true"></div>
	<div class="pb-hero__inner pb-hero__inner--center">
		<p class="pb-hero__eyebrow"><span class="pb-pulse-dot"></span>OPEN FOR PROJECTS</p>
		<h1 class="pb-hero__title">Web Apps, AI Agents &amp; 3D Experiences</h1>
		<p class="pb-hero__sub">Solo studio. 12 years shipping. Fixed-price builds from $40 to $20k+. AI-powered, performance-tuned, owned end-to-end.</p>
		<div class="pb-hero__ctas">
			<a class="pb-btn pb-btn--primary" href="#pricing" data-magnetic="">See Pricing <span aria-hidden="true">&rarr;</span></a>
			<a class="pb-btn pb-btn--ghost" href="<?php echo esc_url( home_url( '/portfolio/' ) ); ?>">View Work</a>
		</div>
	</div>
	<div class="pb-hero__scroll-hint" aria-hidden="true"><span></span></div>
</section>

<!-- ============================================
     INTRO -- Video + Dmitriy portrait + CTAs
     ============================================ -->
<section class="pb-section pb-old-hero" id="intro" data-pb-reveal="">
	<div class="pb-old-hero__wrap">
		<video autoplay muted loop playsinline class="pb-old-hero__vid">
			<source src="<?php echo esc_url( home_url( '/wp-content/uploads/2022/09/ai-development-photon-bounce.mp4' ) ); ?>" type="video/mp4">
		</video>
		<div class="pb-old-hero__shade"></div>
		<div class="pb-old-hero__inner">
			<figure class="pb-old-hero__avatar">
				<img src="<?php echo esc_url( home_url( '/wp-content/uploads/2024/03/photon-bounce-team-d-809x1024.png' ) ); ?>"
				     alt="Dmitriy - Photon Bounce Studio Lead" loading="lazy">
			</figure>
			<h2>Marketing, Web Development, Digital Design &amp; Beyond</h2>
			<div class="pb-hero__ctas pb-hero__ctas--wizard">
				<a class="pb-btn pb-btn--primary" href="#pricing" data-magnetic="" data-pb-wizard='{"cat":"web","type":"simple","pages":7}' data-pb-track="wizard-website">Plan my Website <span aria-hidden="true">&rarr;</span></a>
				<a class="pb-btn pb-btn--primary" href="#pricing" data-magnetic="" data-pb-wizard='{"cat":"ai","type":"ai","pages":3}' data-pb-track="wizard-ai">Plan my AI Agent <span aria-hidden="true">&rarr;</span></a>
				<a class="pb-btn pb-btn--ghost" href="#pricing" data-magnetic="" data-pb-wizard='{"cat":"brand","type":"micro","pages":1}' data-pb-track="wizard-brand">Plan my Brand</a>
			</div>
			<p>Hello and thanks for visiting! My name is Dmitriy and I am highly proficient and experienced in Web Development, SEO optimization, Social Media Strategy, UI/UX Design, and Artificial Intelligence applications. Additionally, I have skills in 3D Modeling, AR, VR, App Development, Graphic Design, Corporate Branding, and Business Strategy.</p>
			<div class="pb-old-hero__btns">
				<a class="pb-btn pb-btn--ghost" href="mailto:<?php echo esc_attr( pb_aurora_email() ); ?>">Get in Touch</a>
				<button type="button" class="pb-btn pb-btn--ghost pb-btn--sm" id="pb-play-voice" aria-label="Play AI voice welcome">
					<span aria-hidden="true">&#128266;</span> Hear AI Voice
				</button>
			</div>
		</div>
	</div>
</section>

<!-- ============================================
     REELS -- Video proof right after intro
     ============================================ -->
<section class="pb-section pb-reels" id="reels" data-pb-reveal="" aria-label="Featured app videos">
	<div class="pb-section__head">
		<p class="pb-eyebrow">FEATURED REELS</p>
		<h2 class="pb-aurora-text">Two of our apps, in motion.</h2>
		<p>Short walkthroughs of work we ship. Click play -- they load only when you ask.</p>
	</div>
	<div class="pb-reels__grid">
		<figure class="pb-reel">
			<div class="pb-reel__frame">
				<iframe loading="lazy" src="https://www.youtube-nocookie.com/embed/KG6BteGxw4Y?rel=0" title="OCCUPANT KILLER - Hybrid Voxel Warfare Game" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen="" referrerpolicy="strict-origin-when-cross-origin"></iframe>
			</div>
			<figcaption class="pb-reel__cap">
				<h3>OCCUPANT KILLER</h3>
				<p>Hybrid voxel-warfare game &middot; Three.js + custom physics</p>
			</figcaption>
		</figure>
		<figure class="pb-reel">
			<div class="pb-reel__frame">
				<iframe loading="lazy" src="https://www.youtube-nocookie.com/embed/cGlrp5GQOgE?rel=0" title="AI Hospice Assistant" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen="" referrerpolicy="strict-origin-when-cross-origin"></iframe>
			</div>
			<figcaption class="pb-reel__cap">
				<h3>AI Hospice Assistant</h3>
				<p>LLM + voice patient-support pilot &middot; React Native + RAG</p>
			</figcaption>
		</figure>
	</div>
</section>

<!-- ============================================
     CAPABILITIES
     ============================================ -->
<section class="pb-section pb-services pb-narrate-host is-narrated" id="services">
	<div class="pb-section__head">
		<h2 class="pb-aurora-text">Capabilities</h2>
		<p>Four pillars, one studio. Every pixel, every endpoint, every shader -- owned end-to-end.</p>
	</div>
	<div class="pb-services__grid">
		<article class="pb-service" data-tilt="" style="--pb-i:0"><div class="pb-service__chrome"></div><h3>SAAS / Web / SEO / SMM</h3><p>Responsive sites, performance-tuned, SEO-mapped, social-amplified. From marketing pages to full SaaS dashboards.</p></article>
		<article class="pb-service" data-tilt="" style="--pb-i:1"><div class="pb-service__chrome"></div><h3>UI / UX / Branding</h3><p>Identity systems, illustrations, technical diagrams, UI concepts -- paired with hands-on UX research and product design.</p></article>
		<article class="pb-service" data-tilt="" style="--pb-i:2"><div class="pb-service__chrome"></div><h3>AI Application Dev</h3><p>LLM agents, RAG, embeddings, fine-tuning, NLP, computer vision, generative imagery, recommender systems, automation.</p></article>
		<article class="pb-service" data-tilt="" style="--pb-i:3"><div class="pb-service__chrome"></div><h3>3D &middot; AR/VR &middot; Physics</h3><p>Three.js / WebGL pipelines, AR research apps, mixed reality, particle and physics demos for science teams.</p></article>
	</div>
</section>

<!-- ============================================
     PORTFOLIO -- Floaters + archive grid combined
     ============================================ -->
<section class="pb-section" id="portfolio" data-pb-reveal="">
	<div class="pb-section__head">
		<h2 class="pb-aurora-text">Portfolio</h2>
		<p>Explore examples of our work -- web apps, AI integrations, 3D/AR experiences, and brand systems.</p>
		<button type="button" class="pb-floaters__sound" data-pb-floaters-sound aria-pressed="false" aria-label="Toggle ambient sound">
			<span class="pb-floaters__sound-icon" aria-hidden="true">&#128264;</span>
			<span class="pb-floaters__sound-lab">Ambient sound: off</span>
		</button>
	</div>

	<?php if ( function_exists( 'pb_aurora_portfolio_floaters_render' ) ) : ?>
	<?php pb_aurora_portfolio_floaters_render( 8, true ); ?>
	<?php endif; ?>

	<div class="pb-archive-grid" style="margin-top:32px;">
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/construction-company-website-blog-development/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2024/10/Website-and-Advanced-Blog-Development.png' ) ); ?>" alt="Construction Company Website &amp; Blog Development" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Branding &amp; Web Development</p>
				<h3 class="pb-arch-card__title">Construction Company Website &amp; Blog Development</h3>
				<p class="pb-arch-card__excerpt">Urban Shore Builders -- comprehensive website and advanced blog development.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/nft-website/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2023/04/website-design.jpg' ) ); ?>" alt="NFT Website" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Branding &amp; Web Development</p>
				<h3 class="pb-arch-card__title">NFT Website</h3>
				<p class="pb-arch-card__excerpt">ResistBeurocracy.io -- crypto integration, smart contracts, and minting.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/martial-arts-krav-maga-video-capture/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2023/04/krav-maga-thumb.png' ) ); ?>" alt="Martial Arts Krav Maga Video Capture" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Video Production</p>
				<h3 class="pb-arch-card__title">Martial Arts - Krav Maga Video Capture</h3>
				<p class="pb-arch-card__excerpt">Seminar video capture and production for a professional K.A.M.I. trainer.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/particle-nucleus-activity/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2023/03/Screenshot_20230320_115326_Chrome.jpg' ) ); ?>" alt="Particle Nucleus Activity" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">3D / VR / AR</p>
				<h3 class="pb-arch-card__title">Particle Nucleus Activity</h3>
				<p class="pb-arch-card__excerpt">Dynamic activity of a nucleus within a particle -- creative animation.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/augmented-reality-lungs-research-app/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2023/03/Screenshot_20230320_114316_Chrome-e1679328218254.jpg' ) ); ?>" alt="Augmented Reality - Lungs Research App" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">3D / VR / AR</p>
				<h3 class="pb-arch-card__title">Augmented Reality - Lungs Research App</h3>
				<p class="pb-arch-card__excerpt">Unity3D-built AR app for HoloLens and Oculus -- lungs research.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/3d-animation/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2023/03/Screenshot_20230320_114403_Chrome.jpg' ) ); ?>" alt="3D Animation" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">3D / VR / AR</p>
				<h3 class="pb-arch-card__title">3D Animation</h3>
				<p class="pb-arch-card__excerpt">Social media marketing animated scene -- 3D Studio Max + Vray.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/ar-augmented-reality-app/' ) ); ?>" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/2022/09/Screenshot-2022-09-29-3.21.14-PM-e1664479559915.png' ) ); ?>" alt="AR Augmented Reality App" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">3D / VR / AR</p>
				<h3 class="pb-arch-card__title">AR Augmented Reality App</h3>
				<p class="pb-arch-card__excerpt">Unity3D AR app to study an AC motor up close.</p>
				<span class="pb-arch-card__cta">View case &rarr;</span>
			</div>
		</a>
	</div>
	<div style="text-align:center;margin-top:32px;">
		<a class="pb-btn pb-btn--primary" href="<?php echo esc_url( home_url( '/portfolio/' ) ); ?>">View All Projects <span aria-hidden="true">&rarr;</span></a>
	</div>
</section>

<!-- ============================================
     MY APPS — first-party apps strip (appended; original content untouched)
     ============================================ -->
<section class="pb-section pb-myapps" id="my-apps" data-pb-reveal="">
	<div class="pb-section__head">
		<h2 class="pb-aurora-text">My Apps</h2>
		<p>Products I design, build and ship end-to-end &mdash; live and playable right now.</p>
	</div>
	<div class="pb-myapps__strip">
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/occupantkiller/' ) ); ?>" target="_blank" rel="noopener" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/photon-apps/occupantkiller.jpg' ) ); ?>" alt="OccupantKiller browser FPS" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Browser Game &middot; 3D / WebGL</p>
				<h3 class="pb-arch-card__title">OccupantKiller</h3>
				<p class="pb-arch-card__excerpt">Browser-native tactical FPS in Three.js &mdash; 39 weapons, FPV drones, armored vehicles, 19 maps. Runs 100% in-browser, no install.</p>
				<span class="pb-arch-card__cta">Play it &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/ausis/' ) ); ?>" target="_blank" rel="noopener" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/photon-apps/ausis.jpg' ) ); ?>" alt="Ausis audio app" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Mobile App &middot; Audio / AI</p>
				<h3 class="pb-arch-card__title">Ausis</h3>
				<p class="pb-arch-card__excerpt">Sleep, focus &amp; workout soundscapes &mdash; 35+ sources, a layered Mix Studio, brainwave entrainment and an AI sound advisor. $1.99 lifetime.</p>
				<span class="pb-arch-card__cta">Visit microsite &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/guidedmeditation/' ) ); ?>" target="_blank" rel="noopener" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/photon-apps/guidedmed.jpg' ) ); ?>" alt="Guided Meditation Portal" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Mobile App &middot; Wellness / AI</p>
				<h3 class="pb-arch-card__title">Guided Meditation Portal</h3>
				<p class="pb-arch-card__excerpt">23 guided practices, an on-device &ldquo;Spirit&rdquo; AI companion and gentle alarms. Privacy-first, one-time $2, zero subscriptions or tracking.</p>
				<span class="pb-arch-card__cta">Visit microsite &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/govdao/' ) ); ?>" target="_blank" rel="noopener" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/photon-apps/govdao.jpg' ) ); ?>" alt="GovDAO on-chain governance" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Web3 &middot; On-chain Governance</p>
				<h3 class="pb-arch-card__title">GovDAO</h3>
				<p class="pb-arch-card__excerpt">An on-chain governance kernel &mdash; proposals, equal-weight voting, timelocked execution, treasury caps and emergency guardians, with a mobile client.</p>
				<span class="pb-arch-card__cta">Visit microsite &rarr;</span>
			</div>
		</a>
		<a class="pb-arch-card" href="<?php echo esc_url( home_url( '/friendai/' ) ); ?>" target="_blank" rel="noopener" data-pb-ripple="">
			<figure class="pb-arch-card__thumb"><img src="<?php echo esc_url( home_url( '/wp-content/uploads/photon-apps/friendai.jpg' ) ); ?>" alt="Friendai AI companion" loading="lazy"></figure>
			<div class="pb-arch-card__body">
				<p class="pb-arch-card__cat">Mobile App &middot; AI / Eldercare</p>
				<h3 class="pb-arch-card__title">Friendai</h3>
				<p class="pb-arch-card__excerpt">A warm, hands-free AI companion for seniors and people living with Alzheimer&rsquo;s or dementia &mdash; always-available chat, one-tap caregiver calling, EN + RU.</p>
				<span class="pb-arch-card__cta">Visit microsite &rarr;</span>
			</div>
		</a>
	</div>
</section>

<!-- ============================================
     PROCESS
     ============================================ -->
<section class="pb-section pb-process" id="process" data-pb-reveal="">
	<div class="pb-section__head">
		<h2 class="pb-aurora-text">Process</h2>
		<p>How a Photon Bounce engagement actually moves. No agency theater. No black-box hand-offs.</p>
	</div>
	<ol class="pb-process__rail">
		<li class="pb-process__step" style="--pb-i:0">
			<span class="pb-process__num">01</span>
			<h3>Discovery</h3>
			<p>30-min free call. Goals, audience, constraints, success metrics. You leave with a one-pager scope.</p>
		</li>
		<li class="pb-process__step" style="--pb-i:1">
			<span class="pb-process__num">02</span>
			<h3>Scope &amp; Estimate</h3>
			<p>Fixed-price phases, written deliverables. No hourly drift, no surprise invoices, no committee.</p>
		</li>
		<li class="pb-process__step" style="--pb-i:2">
			<span class="pb-process__num">03</span>
			<h3>Build in Public</h3>
			<p>Weekly Friday demo links. Async updates. You feel progress; you do not have to chase it.</p>
		</li>
		<li class="pb-process__step" style="--pb-i:3">
			<span class="pb-process__num">04</span>
			<h3>Launch &amp; Iterate</h3>
			<p>Hand-off package, source files, and a Care Plan if you want me on call.</p>
		</li>
	</ol>
	<div class="pb-numbers" aria-label="Studio numbers">
		<div class="pb-num"><span class="pb-num__v" data-pb-count="12">12</span><span class="pb-num__l">years shipping</span></div>
		<div class="pb-num"><span class="pb-num__v" data-pb-count="30">30</span><span class="pb-num__l">production projects</span></div>
		<div class="pb-num"><span class="pb-num__v" data-pb-count="98">98</span><span class="pb-num__l">avg Lighthouse score</span></div>
		<div class="pb-num"><span class="pb-num__v" data-pb-count="100">100</span><span class="pb-num__l">% owned end-to-end</span></div>
	</div>
</section>

<!-- Matrix separator: process &rarr; testimonials -->
<div class="pb-matrix-wrap" aria-hidden="true">
	<canvas class="pb-matrix-sep"></canvas>
	<div class="pb-matrix__label">Compiling testimonials&hellip;</div>
</div>

<!-- ============================================
     TESTIMONIALS + TRUST LOGOS
     ============================================ -->
<section class="pb-section pb-testimonials" id="testimonials" data-pb-reveal="">
	<div class="pb-section__head">
		<h2 class="pb-aurora-text">Receipts</h2>
		<p>Selected feedback from founders, agency partners, and research teams.</p>
	</div>
	<div class="pb-testimonials__rail">
		<figure class="pb-testimonial" style="--pb-i:0">
			<blockquote>"Dmitriy shipped our entire SaaS marketing site in three weeks -- design system, AI demo, blog, billing flow. Lighthouse hit 99 on launch day."</blockquote>
			<figcaption><span class="pb-testimonial__name">Founder</span> &middot; early-stage SaaS &middot; Boston</figcaption>
		</figure>
		<figure class="pb-testimonial" style="--pb-i:1">
			<blockquote>"The AR research app he built became part of our published paper. Mocap pipeline, particle visualizer, the whole thing -- clean code and clear comms."</blockquote>
			<figcaption><span class="pb-testimonial__name">Research Lead</span> &middot; medical AR study</figcaption>
		</figure>
		<figure class="pb-testimonial" style="--pb-i:2">
			<blockquote>"We white-labeled him on three brand projects. Designs landed first revision every time. Fastest contractor we have ever shared a Figma file with."</blockquote>
			<figcaption><span class="pb-testimonial__name">Creative Director</span> &middot; NY agency</figcaption>
		</figure>
		<figure class="pb-testimonial" style="--pb-i:3">
			<blockquote>"He fine-tuned a private LLM on our 8,000-page knowledge base, deployed it self-hosted, and trained our team on it. Two weeks. No drama."</blockquote>
			<figcaption><span class="pb-testimonial__name">CTO</span> &middot; industrial automation</figcaption>
		</figure>
	</div>
</section>

<section class="pb-section pb-logos" data-pb-reveal="" aria-label="Trusted by">
	<p class="pb-eyebrow pb-logos__eyebrow">TRUSTED BY FOUNDERS, AGENCIES &amp; RESEARCHERS</p>
	<div class="pb-logos__strip">
		<span class="pb-logo">SaaS &middot; Y Combinator alum</span>
		<span class="pb-logo">NY brand agency</span>
		<span class="pb-logo">University AR study</span>
		<span class="pb-logo">Industrial automation</span>
		<span class="pb-logo">Crypto exchange (closed)</span>
		<span class="pb-logo">Indie game studio</span>
		<span class="pb-logo">Healthcare R&amp;D</span>
	</div>
</section>

<!-- Matrix separator: testimonials &rarr; pricing -->
<div class="pb-matrix-wrap" aria-hidden="true">
	<canvas class="pb-matrix-sep"></canvas>
	<div class="pb-matrix__label">Calculating engagement vectors&hellip;</div>
</div>

<!-- ============================================
     PRICING
     ============================================ -->
<?php if ( function_exists( 'pb_aurora_pricing_render' ) ) : ?>
<?php pb_aurora_pricing_render(); ?>
<?php endif; ?>

<?php if ( function_exists( 'pb_aurora_build_render' ) ) : ?>
<?php pb_aurora_build_render(); ?>
<?php endif; ?>

<!-- Matrix separator: build &rarr; resources -->
<div class="pb-matrix-wrap pb-matrix-wrap--cover" aria-hidden="true" style="background-image:url('<?php echo esc_url( home_url( '/wp-content/uploads/2024/06/car-pro-detailing.jpg' ) ); ?>');">
	<canvas class="pb-matrix-sep"></canvas>
	<div class="pb-matrix__label">Loading field guides&hellip;</div>
</div>

<!-- ============================================
     FREE GUIDES
     ============================================ -->
<section class="pb-section pb-resources" id="resources" data-pb-reveal="">
	<div class="pb-section__head">
		<p class="pb-eyebrow">FREE GUIDES</p>
		<h2 class="pb-aurora-text">Steal these. No agenda.</h2>
		<p>Two field guides we wrote for small teams. The first is free, no catch. The second asks for an email -- and that is the only thing it asks for.</p>
	</div>
	<div class="pb-resources__grid">
		<article class="pb-resource pb-resource--free">
			<div class="pb-resource__chip">FREE &mdash; INSTANT</div>
			<h3>10 Marketing Tips That Actually Move the Needle</h3>
			<p>A short, vendor-neutral list of things you can do this week to grow without burning a quarter on tools you will not use. Includes a 30-minute homepage audit you can run on yourself.</p>
			<ul class="pb-resource__meta">
				<li>4 pages</li>
				<li>PDF &middot; 8 KB</li>
				<li>No signup</li>
			</ul>
			<a class="pb-btn pb-btn--primary pb-resource__cta" href="<?php echo esc_url( home_url( '/wp-content/uploads/2026/05/photon-bounce-marketing-tips.pdf' ) ); ?>" download="" data-pb-magnet="free">
				Download free guide <span aria-hidden="true">&darr;</span>
			</a>
		</article>
		<article class="pb-resource pb-resource--gated">
			<div class="pb-resource__chip">EMAIL UNLOCK</div>
			<h3>The Small-Business Conversion Playbook 2026</h3>
			<p>Twenty-four pages on turning visitors into customers and customers into repeat buyers. Three sections (acquisition, conversion, retention), email tactics, plus a 90-minute clarity-sprint worksheet.</p>
			<ul class="pb-resource__meta">
				<li>7 pages</li>
				<li>PDF &middot; 12 KB</li>
				<li>Email required</li>
			</ul>
			<form class="pb-resource__gate" data-pb-magnet-form="" data-magnet-url="<?php echo esc_url( home_url( '/wp-content/uploads/2026/05/photon-bounce-conversion-playbook-2026.pdf' ) ); ?>">
				<label class="pb-resource__field">
					<span class="pb-sr-only">Your email</span>
					<input type="email" name="email" required="" placeholder="you@domain.com" autocomplete="email">
				</label>
				<button type="submit" class="pb-btn pb-btn--ghost">Unlock playbook <span aria-hidden="true">&rarr;</span></button>
				<p class="pb-resource__note">One email. No newsletter spam. Unsubscribe in one click.</p>
				<p class="pb-resource__success" hidden="">
					<strong>Thanks!</strong> Your guide is ready -
					<a class="pb-resource__success-link" href="#" target="_blank" rel="noopener">open the playbook (PDF) <span aria-hidden="true">&rarr;</span></a>
				</p>
				<p class="pb-resource__error" hidden="">Something hiccupped. Try once more, or email <?php echo esc_html( pb_aurora_email() ); ?> and we will send it directly.</p>
			</form>
		</article>
	</div>
</section>

<!-- Matrix separator: resources &rarr; FAQ -->
<div class="pb-matrix-wrap" aria-hidden="true">
	<canvas class="pb-matrix-sep"></canvas>
	<div class="pb-matrix__label">Querying knowledge base&hellip;</div>
</div>

<!-- ============================================
     FAQ
     ============================================ -->
<?php if ( function_exists( 'pb_aurora_faq_render' ) ) : ?>
<?php pb_aurora_faq_render(); ?>
<?php endif; ?>

<!-- ============================================
     AI CONCIERGE STRIP
     ============================================ -->
<section class="pb-section pb-ai-strip" id="ai">
	<div class="pb-ai-strip__inner">
		<div class="pb-ai-strip__copy">
			<p class="pb-eyebrow"><span class="pb-pulse-dot"></span>LIVE AI</p>
			<h2 class="pb-aurora-text">Talk to the studio's concierge.</h2>
			<p>Powered by an LLM grounded in this site's portfolio. Ask about services, projects, pricing, or just vibe-check the tech. Tap the orb in the corner.</p>
			<button class="pb-btn pb-btn--primary" type="button" data-pb-open-orb="">Open Concierge</button>
		</div>
		<div class="pb-ai-strip__viz" aria-hidden="true">
			<div class="pb-ai-strip__halo"></div>
			<div class="pb-ai-strip__core"></div>
		</div>
	</div>
</section>

<!-- ============================================
     CONTACT CTA
     ============================================ -->
<section class="pb-section pb-cta" id="contact">
	<div class="pb-cta__inner">
		<h2 class="pb-aurora-text">Got an idea? Let's build it.</h2>
		<p>Email or call -- fastest path to a focused estimate.</p>
		<div class="pb-cta__buttons">
			<a class="pb-btn pb-btn--primary" href="mailto:<?php echo esc_attr( pb_aurora_email() ); ?>" data-magnetic=""><?php echo esc_html( pb_aurora_email() ); ?></a>
			<?php $tel_b64 = base64_encode( 'tel:+18573165054' ); ?>
			<a href="#" class="pb-btn pb-btn--ghost pb-tel" data-pb-tel="<?php echo esc_attr( $tel_b64 ); ?>" data-pb-track="tel-cta" aria-label="Call the studio" rel="nofollow noindex"><svg class="pb-tel__icon" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" focusable="false"><path fill="currentColor" d="M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.3 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A18 18 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.2.2 2.4.6 3.6.1.4 0 .8-.3 1.1l-2.2 2.1Z"></path></svg><span class="pb-tel__lab">Call</span><span class="pb-tel__img" aria-hidden="true"><svg class="pb-tel__svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 168 28" width="168" height="28" role="img" aria-hidden="true" focusable="false"><rect x="0" y="0" width="168" height="28" rx="6" ry="6" fill="rgba(255,255,255,0.04)"></rect><text x="84" y="19" font-family="JetBrains Mono, monospace" font-size="15" font-weight="600" fill="currentColor" letter-spacing=".5" text-anchor="middle">857-316-5054</text></svg></span></a>
			<?php $wa_b64 = base64_encode( 'https://wa.me/18573165054?text=Hi%20from%20photon-bounce.com%20--%20interested%20in%20a%20project.' ); ?>
			<a href="#" class="pb-btn pb-btn--ghost pb-wa" data-pb-wa="<?php echo esc_attr( $wa_b64 ); ?>" data-pb-track="wa-cta" aria-label="Message the studio on WhatsApp" rel="nofollow noindex"><svg class="pb-wa__icon" viewBox="0 0 32 32" width="16" height="16" aria-hidden="true" focusable="false"><path fill="currentColor" d="M16 3C9 3 3.4 8.6 3.4 15.6c0 2.3.6 4.5 1.7 6.4L3 29l7.2-1.9a12.6 12.6 0 0 0 5.8 1.4h.0c7 0 12.6-5.6 12.6-12.6S23 3 16 3zm0 23a10.4 10.4 0 0 1-5.3-1.5l-.4-.2-4.3 1.1 1.2-4.2-.2-.4a10.4 10.4 0 1 1 19.4-5.2A10.4 10.4 0 0 1 16 26zm6-7.6c-.3-.2-1.9-.9-2.2-1-.3-.1-.5-.2-.7.2-.2.3-.8 1-1 1.2-.2.2-.4.2-.6.1a8.6 8.6 0 0 1-2.6-1.6 9.4 9.4 0 0 1-1.8-2.3c-.2-.3 0-.5.1-.7.1-.1.3-.4.5-.6.1-.2.2-.3.3-.5 0-.2 0-.4-.1-.5-.1-.2-.7-1.6-.9-2.2-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.7.4-.3.3-1 1-1 2.4 0 1.4 1 2.8 1.2 3 .2.2 2 3.1 4.9 4.3 2.9 1.2 2.9.8 3.4.8.5 0 1.7-.7 1.9-1.4.2-.7.2-1.3.2-1.4-.1-.1-.3-.2-.6-.4z"></path></svg><span class="pb-wa__lab">WhatsApp</span></a>
		</div>
	</div>
</section>

<!-- ============================================
     WIZARD NAVIGATION SCRIPT
     ============================================ -->
<script>
(function () {
	'use strict';
	function fire(el, type) { el.dispatchEvent(new Event(type, { bubbles: true })); }
	document.addEventListener('click', function (e) {
		var btn = e.target.closest('[data-pb-wizard]');
		if (!btn) return;
		var spec = null;
		try { spec = JSON.parse(btn.getAttribute('data-pb-wizard') || ''); } catch (_) { return; }
		if (!spec) return;
		e.preventDefault();
		var target = document.querySelector('#pricing');
		if (!target) { window.location.hash = '#pricing'; return; }
		var hdr = document.querySelector('.pb-header, header.site-header');
		var hdrH = hdr ? hdr.offsetHeight : 0;
		var top = target.getBoundingClientRect().top + window.scrollY - hdrH - 12;
		window.scrollTo({ top: top, behavior: 'smooth' });
		setTimeout(function () {
			if (spec.cat) {
				var tab = document.querySelector('[data-pb-cat-tabs] [data-cat="' + spec.cat + '"]');
				if (tab) tab.click();
			}
			var calc = document.querySelector('[data-pb-calc]');
			if (calc) {
				if (spec.type) {
					var sel = calc.querySelector('[data-calc-type]');
					if (sel) { sel.value = spec.type; fire(sel, 'change'); fire(sel, 'input'); }
				}
				if (typeof spec.pages === 'number') {
					var pg = calc.querySelector('[data-calc-pages]');
					if (pg) { pg.value = String(spec.pages); fire(pg, 'input'); fire(pg, 'change'); }
				}
				if (Array.isArray(spec.ai) && spec.ai.length) {
					var aiSel = calc.querySelector('[data-calc-ai-feat]');
					if (aiSel) {
						aiSel.value = spec.ai[0];
						fire(aiSel, 'change');
					}
				}
				calc.classList.add('pb-calc--flash');
				setTimeout(function () { calc.classList.remove('pb-calc--flash'); }, 2400);
			}
		}, 480);
	}, false);
})();
</script>

<!-- ============================================
     STICKY BAR + EXIT MODAL
     ============================================ -->
<aside class="pb-stickbar" id="pb-stickbar" aria-label="Get a quote">
	<div class="pb-stickbar__inner">
		<div class="pb-stickbar__msg">
			<span class="pb-stickbar__pulse" aria-hidden="true"></span>
			<strong>Need this for your brand?</strong>
			<span class="pb-stickbar__sub">Quote in 24 h &middot; 1-2 client slots open this quarter.</span>
		</div>
		<div class="pb-stickbar__cta">
			<a class="pb-btn pb-btn--primary pb-btn--sm" href="<?php echo esc_url( home_url( '/quote/' ) ); ?>" data-pb-track="stickbar-quote">Get a quote &rarr;</a>
			<button class="pb-stickbar__close" type="button" data-pb-stickbar-close="" aria-label="Dismiss">&times;</button>
		</div>
	</div>
</aside>

<div class="pb-exit" id="pb-exit" hidden="" role="dialog" aria-modal="true" aria-labelledby="pb-exit-h">
	<div class="pb-exit__backdrop" data-pb-exit-close=""></div>
	<div class="pb-exit__panel" role="document">
		<button class="pb-exit__close" type="button" data-pb-exit-close="" aria-label="Close">&times;</button>
		<p class="pb-eyebrow"><span class="pb-pulse-dot"></span>BEFORE YOU GO</p>
		<h2 id="pb-exit-h" class="pb-aurora-text">Get the studio playbook.</h2>
		<p class="pb-lead">A free, no-fluff PDF: how I scope, price, and ship a $20k+ web build solo in 30 days. Plus 1 short field-note per month -- that's it.</p>
		<form class="pb-exit__form" data-pb-lead-form="" data-pb-rest="<?php echo esc_url( rest_url( 'pb/v1/lead' ) ); ?>" novalidate="">
			<input type="hidden" name="kind" value="exit-intent">
			<input type="hidden" name="source" value="exit-modal">
			<label class="screen-reader-text" for="pb-exit-email">Email</label>
			<input id="pb-exit-email" type="email" name="email" required="" autocomplete="email" placeholder="you@domain.com">
			<input type="text" name="website" tabindex="-1" autocomplete="off" aria-hidden="true" class="pb-field--honeypot">
			<button class="pb-btn pb-btn--primary" type="submit" data-pb-track="exit-submit">Send me the playbook &rarr;</button>
			<p class="pb-exit__msg" data-pb-lead-msg="" role="status" aria-live="polite"></p>
			<p class="pb-exit__legal">No spam. Unsubscribe anytime. Read by ~1,200 founders + designers.</p>
		</form>
	</div>
</div>

<?php get_footer(); ?>
