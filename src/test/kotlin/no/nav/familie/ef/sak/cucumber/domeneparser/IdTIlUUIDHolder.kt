package no.nav.familie.ef.sak.cucumber.domeneparser

import java.util.UUID

object IdTIlUUIDHolder {
    val behandlingIdTilUUID = (1..10).associateWith { UUID.randomUUID() }
    val tilkjentYtelseIdNummerTilUUID = (1..10).associateWith { UUID.randomUUID() }

    private val utgiftIder = mutableMapOf<Int, UUID>()

    fun behandlingIdFraUUID(id: UUID) = behandlingIdTilUUID.entries.single { it.value == id }.key

    /**
     * behandlingId to ident, to barnId
     */
    val barnIder = mutableMapOf<UUID, MutableMap<String, UUID>>()

    fun hentUtgiftUUID(int: Int): UUID = utgiftIder.getOrPut(int) { UUID.randomUUID() }

    fun hentEllerOpprettBarn(
        behandlingId: UUID,
        ident: String,
    ): UUID {
        val behandlingBarn = barnIder.getOrPut(behandlingId) { mutableMapOf() }
        return behandlingBarn.getOrPut(ident) { UUID.randomUUID() }
    }

    fun hentBarn(
        behandlingId: UUID,
        id: UUID,
    ): String {
        val behandlingBarn = barnIder.getOrPut(behandlingId) { mutableMapOf() }
        return behandlingBarn.entries.single { it.value == id }.key
    }
}
