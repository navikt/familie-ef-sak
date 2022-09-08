package no.nav.familie.ef.sak.opplysninger.søknad.domain

import no.nav.familie.ef.sak.vilkår.dto.DokumentasjonDto
import no.nav.familie.ef.sak.vilkår.dto.VedleggDto

data class Dokumentasjon(val harSendtInnTidligere: Boolean, val dokumenter: List<Dokument>) {
    fun tilDto(): DokumentasjonDto =
        DokumentasjonDto(
            this.harSendtInnTidligere,
            this.dokumenter.map { dokument ->
                VedleggDto(
                    dokument.id,
                    dokument.navn
                )
            }
        )
}
