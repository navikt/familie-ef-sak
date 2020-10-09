package no.nav.familie.ef.sak.repository.domain.søknad

data class Dokumentasjon(val harSendtInnTidligere: Boolean, val dokumenter: List<Dokument>)
