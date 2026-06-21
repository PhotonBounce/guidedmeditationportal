package com.auroramind.meditation

import java.util.Calendar

/**
 * A small bank of mindfulness quotes shown on the home screen — fresh daily
 * content is a proven engagement driver (gives users a reason to open the app
 * even on days they don't have time for a full session).
 *
 * The quote is deterministic per calendar day, so it stays stable within a day
 * and rotates at midnight.
 */
object Quotes {

    private val bank = listOf(
        "Breathe. You are exactly where you need to be." to "Spirit",
        "Feelings are visitors. Let them come and go." to "Mooji",
        "You should sit in meditation for twenty minutes a day — unless you're too busy; then you should sit for an hour." to "Zen proverb",
        "The present moment is the only moment available to us." to "Thich Nhat Hanh",
        "Quiet the mind, and the soul will speak." to "Ma Jaya Sati Bhagavati",
        "Within you there is a stillness and a sanctuary to which you can retreat at any time." to "Hermann Hesse",
        "Meditation is not evasion; it is a serene encounter with reality." to "Thich Nhat Hanh",
        "You can't stop the waves, but you can learn to surf." to "Jon Kabat-Zinn",
        "Almost everything will work again if you unplug it for a few minutes — including you." to "Anne Lamott",
        "Be where you are; otherwise you will miss your life." to "Buddha",
        "Calm mind brings inner strength and self-confidence." to "Dalai Lama",
        "The little things? The little moments? They aren't little." to "Jon Kabat-Zinn",
        "Smile, breathe, and go slowly." to "Thich Nhat Hanh",
        "Nothing can bring you peace but yourself." to "Ralph Waldo Emerson",
        "Wherever you are, be there totally." to "Eckhart Tolle",
        "Silence is not empty. It is full of answers." to "Unknown",
        "Let go of the thoughts that don't make you strong." to "Karen Salmansohn",
        "True silence is the rest of the mind, and is to the spirit what sleep is to the body." to "William Penn",
        "Rest is not idleness — it is the soil where clarity grows." to "Spirit",
        "Each breath is a fresh beginning." to "Spirit",
        "Peace comes from within. Do not seek it without." to "Buddha",
        "When you own your breath, nobody can steal your peace." to "Unknown",
        "The quieter you become, the more you can hear." to "Ram Dass",
        "Surrender to what is. Let go of what was." to "Sonia Ricotti",
        "Do not let the behavior of others destroy your inner peace." to "Dalai Lama",
        "Slow down and everything you are chasing will come around and catch you." to "John De Paola",
        "In the midst of movement and chaos, keep stillness inside of you." to "Deepak Chopra",
        "The soul usually knows what to do to heal itself. The challenge is to silence the mind." to "Caroline Myss",
        "Meditation is the tongue of the soul and the language of our spirit." to "Jeremy Taylor",
        "Empty your mind, be formless, shapeless — like water." to "Bruce Lee",
        "One conscious breath in and out is a meditation." to "Eckhart Tolle",
        "Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor." to "Thich Nhat Hanh",
        "You are the sky. Everything else is just the weather." to "Pema Chödrön",
        "Tension is who you think you should be. Relaxation is who you are." to "Chinese proverb",
        "The thing about meditation: you become more and more you." to "David Lynch",
        "Drink your tea slowly and reverently, as if it is the axis on which the world revolves." to "Thich Nhat Hanh",
        "Don't believe everything you think." to "Unknown",
        "If you want to conquer the anxiety of life, live in the moment." to "Amit Ray",
        "Your goal is not to battle with the mind, but to witness the mind." to "Swami Muktananda",
        "To a mind that is still, the whole universe surrenders." to "Lao Tzu",
        "The present moment is filled with joy and happiness. If you are attentive, you will see it." to "Thich Nhat Hanh",
        "Meditation is not about feeling a certain way. It's about feeling the way you feel." to "Dan Harris",
        "Breathe in deeply to bring your mind home to your body." to "Thich Nhat Hanh",
        "What you resist persists. What you embrace dissolves." to "Unknown",
        "Stillness is where creativity and solutions to problems are found." to "Eckhart Tolle",
        "Every time we get lost in thought and come back, we strengthen the muscle of awareness." to "Sharon Salzberg",
        "Be kind whenever possible. It is always possible." to "Dalai Lama",
        "You have a treasure within you that is infinitely greater than anything the world can offer." to "Eckhart Tolle",
        "Take rest; a field that has rested gives a bountiful crop." to "Ovid",
        "Between stimulus and response there is a space. In that space is our freedom." to "Viktor Frankl",
        "The only way out is through, and the way through is gentle." to "Spirit",
        "Within you is the dawn of a new day." to "Spirit",
        "Peace is not absence of noise. It is presence of stillness within the noise." to "Spirit",
        "We cannot direct the wind, but we can adjust the sails." to "Unknown",
        "The mind is like water. When it's turbulent, it's difficult to see. When it's calm, everything becomes clear." to "Prasad Mahes",
        "Walk as if you are kissing the earth with your feet." to "Thich Nhat Hanh",
        "Awareness is the greatest agent for change." to "Eckhart Tolle",
        "Nature does not hurry, yet everything is accomplished." to "Lao Tzu",
        "Act without expectation." to "Lao Tzu",
        "All that we are is the result of what we have thought." to "Buddha",
        "Muddy water, let stand, becomes clear." to "Lao Tzu",
        "The wound is the place where the light enters you." to "Rumi",
        "Out beyond ideas of wrongdoing and rightdoing, there is a field. I'll meet you there." to "Rumi",
        "The present moment always will have been." to "Spirit",
        "Not all those who wander are lost — and not all those who sit still are idle." to "Spirit",
        "Before enlightenment, chop wood, carry water. After enlightenment, chop wood, carry water." to "Zen proverb",
    )

    /** Returns today's quote as (text, author). Stable per calendar day. */
    fun today(): Pair<String, String> {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return bank[dayOfYear % bank.size]
    }
}
