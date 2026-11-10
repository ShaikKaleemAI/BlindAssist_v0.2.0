package com.kaleem.blindassist

/** Actions a voice (or button) command can trigger. The service implements this; tests use a fake. */
interface AssistActions {
    fun whatsAhead(); fun readThis(); fun describe(); fun find(what: String); fun navigate(place: String)
    fun lost(); fun callFamily(); fun setVerbosity(v: Verbosity); fun stop(); fun unknown()
}

/** Routes a parsed command to an action. Speech recognition + wake word are NOT implemented (see docs/SCORECARD.md). */
object CommandDispatcher {
    fun dispatch(p: Parsed, a: AssistActions) {
        when (p.cmd) {
            Cmd.WHATS_AHEAD -> a.whatsAhead()
            Cmd.READ_THIS -> a.readThis()
            Cmd.DESCRIBE -> a.describe()
            Cmd.FIND_OBJECT -> a.find(p.arg ?: "")
            Cmd.NAVIGATE -> a.navigate(p.arg ?: "")
            Cmd.LOST -> a.lost()
            Cmd.CALL_FAMILY -> a.callFamily()
            Cmd.QUIET -> a.setVerbosity(Verbosity.QUIET)
            Cmd.DETAILED -> a.setVerbosity(Verbosity.DETAILED)
            Cmd.STOP -> a.stop()
            Cmd.UNKNOWN -> a.unknown()
        }
    }
}

/** Brand-specific hints for keeping the app alive with the screen off. Paths are UNVERIFIED on real devices. */
object OemHints {
    fun hintFor(manufacturer: String?): String? = when (manufacturer?.lowercase()) {
        "samsung" -> "Samsung: set Blind Assist battery use to Unrestricted, and remove it from sleeping apps."
        "xiaomi", "redmi", "poco" -> "Xiaomi: allow Autostart for Blind Assist and set battery saver to No restrictions."
        "oppo", "realme", "oneplus" -> "Oppo: allow background activity and auto launch for Blind Assist in battery settings."
        "vivo", "iqoo" -> "Vivo: allow high background power use and autostart for Blind Assist."
        else -> null
    }
}
