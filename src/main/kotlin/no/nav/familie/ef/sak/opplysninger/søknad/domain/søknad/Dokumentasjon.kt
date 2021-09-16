package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

data class Dokumentasjon(val harSendtInnTidligere: Boolean, val dokumenter: List<Dokument>)
