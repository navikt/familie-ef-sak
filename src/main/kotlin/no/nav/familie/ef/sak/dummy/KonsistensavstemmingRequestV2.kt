package no.nav.familie.ef.sak.dummy

import java.time.LocalDateTime

//TODO denne skal flytte til kontrakter
data class KonsistensavstemmingRequestV2(val fagsystem: String,
                                         val periodeIdn: List<PeriodeIdnForFagsak>,
                                         val avstemmingstidspunkt: LocalDateTime)

data class PeriodeIdnForFagsak(val fagsakId: String,
                               val periodeIdn: Set<Long>) {
    override fun toString(): String = "OppdragId(periodeIdn=$periodeIdn)"
}