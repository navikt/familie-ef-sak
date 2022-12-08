package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.time.LocalDate

data class FørstegangsbehandlingDto(
    val behandlingsårsak: BehandlingÅrsak,
    val kravMottatt: LocalDate,
    val barn: List<BarnSomSkalFødes> = emptyList()
)
