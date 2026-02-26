@file:Suppress("ArrayInDataClass") // Unødvendig å implementere equals/hashcode metodene for en ren dto-klasse

package no.nav.familie.ef.sak.iverksett

import no.nav.familie.kontrakter.ef.iverksett.IverksettDto

data class IverksettMedBrevRequest(
    val iverksettDto: IverksettDto,
    val fil: ByteArray,
)
