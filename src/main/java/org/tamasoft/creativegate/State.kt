package org.tamasoft.creativegate

object State {
    var isFilling = false

    // True while BuildUtil fires its permission-probing BlockPlaceEvent, so our own listeners ignore it.
    var isProbing = false
}