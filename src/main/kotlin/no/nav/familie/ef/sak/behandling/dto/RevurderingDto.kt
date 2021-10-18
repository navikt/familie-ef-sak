package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.time.LocalDate
import java.util.UUID

data class RevurderingDto(val fagsakId: UUID, val behandlingsårsak: BehandlingÅrsak, val kravMottatt: LocalDate)
