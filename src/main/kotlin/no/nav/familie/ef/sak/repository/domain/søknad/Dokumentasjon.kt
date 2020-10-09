package no.nav.familie.ef.sak.repository.domain.søknad

data class Dokumentasjon(val harSendtInnTidligere: Søknadsfelt<Boolean>, val dokumenter: List<Dokument>)
