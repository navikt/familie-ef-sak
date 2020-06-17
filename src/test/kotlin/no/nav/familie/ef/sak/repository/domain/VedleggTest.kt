package no.nav.familie.ef.sak.repository.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class VedleggTest{

    private val id = UUID.randomUUID()
    private val sporbar = Sporbar()

    @Test
    internal fun `2 vedlegg med ulike innehold er ulike`() {
        assertNotEquals(lagVedlegg(12), lagVedlegg(13))
    }

    @Test
    internal fun `2 vedlegg med samme innehold er like`() {
        assertEquals(lagVedlegg(12), lagVedlegg(12))
    }

    private fun lagVedlegg(data: Byte) = Vedlegg(id, id, sporbar, byteArrayOf(data), "navn")
}
