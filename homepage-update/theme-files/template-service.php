<?php
/**
 * Photon-Bounce — service page template.
 *
 * Used for the auto-seeded SEO service pages (web-development, ai-agents, 3d-webgl,
 * seo, aeo, branding). Routed here by template_include in inc/service-pages.php, so
 * these pages never fall back to the homepage layout.
 */
get_header();

if ( function_exists( 'pb_aurora_render_service_page' ) ) {
	pb_aurora_render_service_page( get_post_field( 'post_name', get_queried_object_id() ) );
} else {
	while ( have_posts() ) { the_post(); the_content(); }
}

get_footer();
