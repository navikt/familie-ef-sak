package no.nav.familie.ef.sak.vilkår

import java.time.YearMonth

data class PeriodeDto(val fra: YearMonth?, val til: YearMonth?)

data class DokumentasjonDto(val harSendtInn: Boolean, val vedlegg: List<VedleggDto>)

data class VedleggDto(val id: String, val navn: String)
