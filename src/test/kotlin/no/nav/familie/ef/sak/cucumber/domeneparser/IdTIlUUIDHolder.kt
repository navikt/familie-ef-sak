package no.nav.familie.ef.sak.cucumber.domeneparser

import java.util.UUID

object IdTIlUUIDHolder {

    val behandlingIdTilUUID = (1..10).associateWith { UUID.randomUUID() }
    val tilkjentYtelseIdNummerTilUUID = (1..10).associateWith { UUID.randomUUID() }

    private val utgiftIder = mutableMapOf<Int, UUID>()

    fun hentUtgiftUUID(int: Int): UUID {
        return utgiftIder.getOrPut(int) { UUID.randomUUID() }
    }
}
