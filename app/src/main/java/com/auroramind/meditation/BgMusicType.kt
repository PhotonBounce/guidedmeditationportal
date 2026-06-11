package com.auroramind.meditation

enum class BgMusicType(
    val displayName: String,
    val rawResId: Int
) {
    NONE("None (Silence)", 0),
    CYMBAL_MEDITATION("Cymbal Meditation", R.raw.cymbal_meditation),
    CYMBAL_MEDITATION_1("Cymbal Meditation 2", R.raw.cymbal_meditation_1),
    FOREVER_ISNT_LONG_ENOUGH("Forever Isn't Long Enough", R.raw.forever_isnt_long_enough),
    FOREVER_ISNT_LONG_ENOUGH_1("Forever Isn't Long Enough 2", R.raw.forever_isnt_long_enough_1),
    IN_THIS_ROOM("In This Room", R.raw.in_this_room),
    IN_THIS_ROOM_1("In This Room 2", R.raw.in_this_room_1),
    MARBLE_GRAVITY("Marble Gravity", R.raw.marble_gravity),
    MARBLE_GRAVITY_1("Marble Gravity 2", R.raw.marble_gravity_1),
    MEDITATION_IN_E_MAJOR("Meditation in E Major", R.raw.meditation_in_e_major),
    MEDITATION_IN_E_MAJOR_1("Meditation in E Major 2", R.raw.meditation_in_e_major_1),
    MEDITATION_IN_A_BLACKOUT("Meditation in a Blackout", R.raw.meditation_in_a_blackout),
    MEDITATION_IN_A_BLACKOUT_1("Meditation in a Blackout 2", R.raw.meditation_in_a_blackout_1),
    NIGHT_TERRORS("Night Terrors", R.raw.night_terrors),
    NIGHT_TERRORS_1("Night Terrors 2", R.raw.night_terrors_1),
    THE_RIVER_RUNS("The River Runs", R.raw.the_river_runs),
    THE_RIVER_RUNS_1("The River Runs 2", R.raw.the_river_runs_1),
    UNTITLED("Untitled", R.raw.untitled),
    UNTITLED_1("Untitled 1", R.raw.untitled_1),
    UNTITLED_2("Untitled 2", R.raw.untitled_2),
    UNTITLED_3("Untitled 3", R.raw.untitled_3),
    UNTITLED_4("Untitled 4", R.raw.untitled_4),
    UNTITLED_5("Untitled 5", R.raw.untitled_5),
    UNTITLED_6("Untitled 6", R.raw.untitled_6),
    UNTITLED_7("Untitled 7", R.raw.untitled_7);

    companion object {
        fun fromName(name: String): BgMusicType {
            return values().firstOrNull { it.name == name } ?: NONE
        }
    }
}
