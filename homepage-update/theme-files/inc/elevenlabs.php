<?php
/**
 * Photon-Bounce — ElevenLabs voice welcome + browser TTS fallback.
 *
 * Free tier: uses Web Speech API (browser built-in) with a premium-sounding voice.
 * Paid tier: swap PB_ELEVENLABS_KEY with your ElevenLabs API key.
 *
 * Settings in Customizer: pb_elevenlabs_key, pb_elevenlabs_voice_id, pb_voice_welcome_msg
 */
if ( ! defined( 'ABSPATH' ) ) { exit; }

add_action( 'customize_register', static function ( $wp_customize ) {
	$wp_customize->add_section( 'pb_voice', [
		'title'    => __( 'Voice Welcome', 'photon-bounce-aurora' ),
		'panel'    => 'pb_aurora',
		'priority' => 40,
	] );
	$wp_customize->add_setting( 'pb_elevenlabs_key', [ 'default' => '', 'sanitize_callback' => 'sanitize_text_field' ] );
	$wp_customize->add_control( 'pb_elevenlabs_key', [ 'section' => 'pb_voice', 'label' => 'ElevenLabs API Key', 'type' => 'text' ] );
	$wp_customize->add_setting( 'pb_elevenlabs_voice_id', [ 'default' => 'EXAVITQu4vr4xnSDxMaL', 'sanitize_callback' => 'sanitize_text_field' ] );
	$wp_customize->add_control( 'pb_elevenlabs_voice_id', [ 'section' => 'pb_voice', 'label' => 'Voice ID (default: Sarah -- free-tier premade)', 'type' => 'text' ] );
	$wp_customize->add_setting( 'pb_voice_welcome_msg', [ 'default' => "Welcome to Photon Bounce. We build custom web apps, AI agents and 3D experiences - fixed-price, fast, and fully owned by you. Tell me what you want to build and I will point you to the right service or set up a quick call.", 'sanitize_callback' => 'sanitize_textarea_field' ] );
	$wp_customize->add_control( 'pb_voice_welcome_msg', [ 'section' => 'pb_voice', 'label' => 'Welcome message', 'type' => 'textarea' ] );
	$wp_customize->add_setting( 'pb_voice_enabled', [ 'default' => true, 'sanitize_callback' => 'rest_sanitize_boolean' ] );
	$wp_customize->add_control( 'pb_voice_enabled', [ 'section' => 'pb_voice', 'label' => 'Enable voice welcome', 'type' => 'checkbox' ] );
} );

add_action( 'wp_footer', static function () {
	if ( ! get_theme_mod( 'pb_voice_enabled', true ) ) { return; }
	$key     = get_theme_mod( 'pb_elevenlabs_key', defined('PB_ELEVENLABS_KEY') ? PB_ELEVENLABS_KEY : '' );
	$voice   = get_theme_mod( 'pb_elevenlabs_voice_id', 'EXAVITQu4vr4xnSDxMaL' );
	$message = get_theme_mod( 'pb_voice_welcome_msg', "Welcome to Photon Bounce. We build custom web apps, AI agents and 3D experiences - fixed-price, fast, and fully owned by you. Tell me what you want to build and I will point you to the right service or set up a quick call." );
	$restUrl = esc_url_raw( rest_url( 'pb/v1/voice' ) );
	?>
<script>
(function() {
  'use strict';
  const STORAGE_KEY = 'pb_voice_played_v2';
  const MUTE_KEY = 'pb_voice_muted';
  const isMuted = localStorage.getItem(MUTE_KEY) === '1';
  const alreadyPlayed = sessionStorage.getItem(STORAGE_KEY);

  const msg = <?php echo wp_json_encode( $message ); ?>;
  const hasKey = <?php echo $key ? 'true' : 'false'; ?>;

  function speakBrowser(text) {
    if (!window.speechSynthesis) return;
    const u = new SpeechSynthesisUtterance(text);
    u.rate = 0.95;
    u.pitch = 1.05;
    const voices = window.speechSynthesis.getVoices();
    const pref = voices.find(v => /Google US English|Microsoft Zira|Samantha|Victoria/i.test(v.name))
              || voices.find(v => /en-?US|en-?GB/i.test(v.lang) && v.name.includes('Female'))
              || voices.find(v => /en/i.test(v.lang))
              || voices[0];
    if (pref) u.voice = pref;
    window.speechSynthesis.speak(u);
  }

  async function speakElevenLabs(text) {
    try {
      const r = await fetch(<?php echo wp_json_encode( $restUrl ); ?>, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text })
      });
      if (!r.ok) throw new Error('voice-err ' + r.status);
      const blob = await r.blob();
      const url = URL.createObjectURL(blob);
      const audio = new Audio(url);
      audio.volume = 1.0;
      await audio.play();
      audio.onended = () => URL.revokeObjectURL(url);
      return true;
    } catch (e) {
      console.warn('[PB Voice] ElevenLabs failed, falling back to browser TTS:', e.message);
      speakBrowser(text);
      return false;
    }
  }

  function playVoice(text) {
    if (isMuted) return;
    if (hasKey) speakElevenLabs(text);
    else speakBrowser(text);
  }

  // Manual play button -- ALWAYS works regardless of session/autoplay state
  document.addEventListener('click', function(e) {
    const btn = e.target.closest('#pb-play-voice');
    if (!btn) return;
    if (btn.disabled) return;
    btn.disabled = true;
    var originalHTML = btn.innerHTML;
    btn.innerHTML = '<span aria-hidden="true">&#128266;</span> Playing...';
    playVoice(msg);
    setTimeout(function() {
      btn.innerHTML = originalHTML;
      btn.disabled = false;
    }, 4000);
  });

  // (Auto-play removed — the welcome voice no longer triggers on scroll/click/keydown.
  //  It speaks only when the visitor presses the "Play AI voice welcome" button.)

  // Mute button injected into header
  var header = document.querySelector('.pb-header__inner');
  if (header) {
    var mbtn = document.createElement('button');
    mbtn.type = 'button';
    mbtn.className = 'pb-voice-toggle';
    mbtn.setAttribute('aria-label', 'Mute voice');
    mbtn.setAttribute('title', 'Mute AI voice');
    mbtn.innerHTML = '<span aria-hidden="true">&#128266;</span>';
    mbtn.style.cssText = 'background:none;border:none;color:inherit;font-size:18px;cursor:pointer;margin-left:8px;opacity:.7;';
    mbtn.addEventListener('click', function() {
      localStorage.setItem(MUTE_KEY, '1');
      if (window.speechSynthesis) window.speechSynthesis.cancel();
      mbtn.innerHTML = '<span aria-hidden="true">&#128263;</span>';
      mbtn.style.opacity = '.4';
    });
    header.appendChild(mbtn);
  }
})();
</script>
<?php
}, 99 );

/* REST endpoint: proxy ElevenLabs TTS */
add_action( 'rest_api_init', static function () {
	register_rest_route( 'pb/v1', '/voice', [
		'methods'             => 'POST',
		'permission_callback' => '__return_true',
		'callback'            => 'pb_aurora_voice_handler',
	] );
} );

function pb_aurora_voice_handler( WP_REST_Request $req ) {
	$key   = get_theme_mod( 'pb_elevenlabs_key', defined('PB_ELEVENLABS_KEY') ? PB_ELEVENLABS_KEY : '' );
	$voice = get_theme_mod( 'pb_elevenlabs_voice_id', 'EXAVITQu4vr4xnSDxMaL' );
	$text  = sanitize_textarea_field( (string) $req->get_param( 'text' ) );
	if ( ! $key || $text === '' ) {
		return new WP_REST_Response( [ 'ok' => false, 'error' => 'no_key_or_text' ], 400 );
    }
	$body = wp_json_encode( [
		'text'    => $text,
		'model_id'=> 'eleven_turbo_v2_5',
		'voice_settings' => [ 'stability' => 0.5, 'similarity_boost' => 0.75 ],
	] );
	$resp = wp_remote_post( 'https://api.elevenlabs.io/v1/text-to-speech/' . $voice, [
		'headers' => [
			'Content-Type'  => 'application/json',
			'xi-api-key'    => $key,
			'Accept'        => 'audio/mpeg',
		],
		'body'    => $body,
			'timeout' => 30,
	] );
	if ( is_wp_error( $resp ) ) {
		return new WP_REST_Response( [ 'ok' => false, 'error' => 'upstream' ], 502 );
	}
	$code = wp_remote_retrieve_response_code( $resp );
	if ( $code !== 200 ) {
		return new WP_REST_Response( [ 'ok' => false, 'error' => 'tts_failed', 'code' => $code ], 502 );
	}
	$audio = wp_remote_retrieve_body( $resp );
	header( 'Content-Type: audio/mpeg' );
	header( 'Content-Length: ' . strlen( $audio ) );
	echo $audio;
	exit;
}
