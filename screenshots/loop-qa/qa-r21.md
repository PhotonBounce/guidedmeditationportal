# DOM QA Report — R21 — 2026-06-20

## main.js — Retry button on API error messages

When the API fails and an error message is shown, a `⟳ Retry` button now
appears inside the error bubble:

```javascript
if (cls === 'err') {
  var _retryBtn = document.createElement('button');
  _retryBtn.className = 'pb-brain__retry';
  _retryBtn.innerHTML = '&#8635; Retry'; // ⟳
  _retryBtn.addEventListener('click', function() {
    var _lastUserMsg = log.querySelectorAll('.pb-brain__msg--me');
    var _lastUserMsg = _lastUserMsg[_lastUserMsg.length - 1];
    var _lastUserText = _lastUserMsg ? _lastUserMsg.textContent.trim() : '';
    div.remove();                    // remove error message
    if (_lastUserMsg) _lastUserMsg.remove(); // remove duplicate user msg
    for (var _ri = chatMsgs.length - 1; _ri >= 0; _ri--) {
      if (chatMsgs[_ri].cls === 'user') { chatMsgs.splice(_ri, 1); break; }
    }
    saveChat();
    if (_lastUserText && input && form) {
      input.value = _lastUserText;
      form.dispatchEvent(new Event('submit', { bubbles: true }));
    }
  });
  div.appendChild(_retryBtn);
}
```

Flow when user clicks ⟳ Retry:
1. Error message div removed from DOM
2. Last user message div removed from DOM (prevents duplicate on re-submit)
3. Last user message also spliced from `chatMsgs` + sessionStorage updated
4. Input filled with the last user message text
5. `submit` event dispatched — full cycle runs (typing indicator, fetch, etc.)

This makes error recovery one-click instead of requiring the user to retype.
The retry cleans up correctly so the chat history stays coherent.

## main.css — .pb-brain__retry styles

```css
.pb-brain__retry {
  display:inline-flex; align-items:center; gap:4px;
  margin-top:7px; background:none;
  border:1px solid rgba(220,80,80,.4); border-radius:4px;
  color:rgba(255,190,190,.8); cursor:pointer;
  font-size:11px; font-style:normal; padding:3px 8px; transition:.15s;
}
.pb-brain__retry:hover {
  border-color:rgba(220,80,80,.8); color:#ffd0d0;
  background:rgba(220,50,50,.12);
}
```

`font-style:normal` needed because `.pb-brain__msg--err` has `font-style:italic`
which would inherit into the button — the retry button should not be italic.

## brainstorm.php — 3 new intent handlers

### 0d-pre17-a) AI image generation / Midjourney / DALL-E / Stable Diffusion
Keywords: ai image, ai generated image, midjourney, dall-e, stable diffusion, ai art,
image generation, text to image, ai artwork, generative art, comfyui, image ai,
ai illustration, generate images, ai images for site

Response:
- DALL-E 3 / Midjourney: hero images, illustrations, backgrounds, icons; style-consistent
  sets from a single prompt; from $50 for an asset session
- Stable Diffusion / ComfyUI: fine-tuned models, LoRA-trained characters, local inference
- Real-ESRGAN upscaling: print-quality (300 dpi) versions
- Optimization: all AI images → WebP, lazy-loaded, alt text for SEO/accessibility
- Not suitable for: faces (accuracy), logos (use vector), product photography (use real)
- Closes: "hero art, illustrations, icons, backgrounds, or full asset library?"

### 0d-pre17-b) Email marketing / newsletter / Mailchimp / ConvertKit
Keywords: email marketing, newsletter, mailchimp, convertkit, drip email, klaviyo,
email list, email campaign, email automation, email sequence, opt-in form, lead magnet,
email funnel, email signup, mailing list

Response:
- Opt-in form: custom-styled, connects to Mailchimp/ConvertKit/Klaviyo/Drip via API;
  lead magnet delivered in confirmation email
- Pop-up / exit-intent: timed or scroll-triggered; GDPR double-opt-in included
- Form → CRM: HubSpot, Airtable, or Notion via webhook
- Welcome sequence: 5-email series built in ESP; from $150 add-on
- ESP recommendations: Mailchimp (free 500), Kit/ConvertKit (creators), Klaviyo
  (e-commerce/Shopify), Drip (SaaS)
- Closes: "existing ESP + what's the lead magnet?"

### 0d-pre17-c) Custom ML / AI model training / fine-tuning / RAG
Keywords: train a model, custom ai model, machine learning, ml model, computer vision,
image recognition, nlp, fine-tune, fine tuning, hugging face, tensorflow, pytorch,
ai model, train gpt, rag pipeline, retrieval augmented, vector database, embeddings,
semantic search, custom llm

Response:
- LLM fine-tuning: GPT-3.5/4o or Llama 3/Mistral via QLoRA; from $500
- Embeddings + RAG: Pinecone/Weaviate/Chroma; semantic Q&A over corpus; from $565
- Computer vision: YOLOv8/ResNet detection & classification; custom dataset training
- Image classification: MobileNet/EfficientNet transfer learning; 500+ labeled images
- Custom AI concierge: system prompt + guardrails + widget; no fine-tuning; from $190
- Deployment: FastAPI or Vercel Edge Function; auth-protected; streaming
- Closes: "classification, generation, semantic search, or extraction?"

## QA results (25/25 all pass)
| Check | Result |
|-------|--------|
| _retryBtn element created | OK |
| pb-brain__retry class | OK |
| ⟳ Retry HTML | OK |
| last user msg removed | OK |
| chatMsgs splice | OK |
| saveChat on retry | OK |
| form re-submitted | OK |
| cls err guard | OK |
| pb-brain__retry CSS | OK |
| retry border red | OK |
| font-style:normal | OK |
| retry hover background | OK |
| AI image intent keywords | OK |
| DALL-E | OK |
| Real-ESRGAN | OK |
| WebP optimization | OK |
| Email marketing keywords | OK |
| ConvertKit | OK |
| lead magnet | OK |
| ESP recommendations | OK |
| Custom ML keywords | OK |
| LLM fine-tuning | OK |
| RAG / Embeddings | OK |
| YOLOv8 | OK |
| FastAPI | OK |
