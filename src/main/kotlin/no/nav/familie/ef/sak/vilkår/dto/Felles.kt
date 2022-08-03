package no.nav.familie.ef.sak.vilkår.dto

data class DokumentasjonDto(val harSendtInn: Boolean, val vedlegg: List<VedleggDto>)

data class VedleggDto(val id: String, val navn: String)
