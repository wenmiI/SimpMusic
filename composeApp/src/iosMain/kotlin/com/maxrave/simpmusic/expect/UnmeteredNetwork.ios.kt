package com.maxrave.simpmusic.expect

import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import kotlin.concurrent.AtomicInt

/**
 * The system's own answer: a path that is neither expensive (cellular, a personal hotspot) nor
 * constrained (Low Data Mode). NWPathMonitor only reports asynchronously, so a monitor started on
 * first use keeps the latest answer; until its first report this says no, which only delays a
 * Smart downloads pass.
 */
private object PathState {
    // 1 = unmetered, 0 = not (or not known yet).
    val unmetered = AtomicInt(0)

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val ok =
                nw_path_get_status(path) == nw_path_status_satisfied &&
                    !nw_path_is_expensive(path) &&
                    !nw_path_is_constrained(path)
            unmetered.value = if (ok) 1 else 0
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_start(monitor)
    }
}

actual fun isOnUnmeteredNetwork(): Boolean = PathState.unmetered.value == 1
