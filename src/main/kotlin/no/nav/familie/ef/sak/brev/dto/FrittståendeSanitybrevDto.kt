package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.ef.sak.brev.BrevmottakereDto

data class FrittståendeSanitybrevDto(
    val pdf: ByteArray,
    val mottakere: BrevmottakereDto,
    val tittel: String,
)
