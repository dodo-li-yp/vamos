// Modified by Vamos (2026): volume-key emergency stop gesture.
package com.yongpeng.vamos.task

/** Fires once while both volume keys are held; resets only after both are released. */
class VolumeStopChord {
    private var up = false
    private var down = false
    private var fired = false

    fun update(key: Int, pressed: Boolean): Boolean {
        when (key) {
            24 -> up = pressed
            25 -> down = pressed
            else -> return false
        }
        if (!up && !down) fired = false
        return if (up && down && !fired) { fired = true; true } else false
    }
}
