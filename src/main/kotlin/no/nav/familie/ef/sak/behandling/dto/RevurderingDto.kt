package no.nav.familie.ef.sak.behandling.dto

import java.time.LocalDate
import java.util.UUID

data class RevurderingDto(val fagsakId: UUID, val behandlingsårsak: String, val kravMottatt: LocalDate)
