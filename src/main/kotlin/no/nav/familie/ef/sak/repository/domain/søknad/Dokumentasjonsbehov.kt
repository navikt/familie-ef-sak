package no.nav.familie.ef.sak.repository.domain.søknad

data class Dokumentasjonsbehov(val label: String,
                               val id: String,
                               val harSendtInn: Boolean,
                               val opplastedeVedlegg: List<Dokument> = emptyList())
