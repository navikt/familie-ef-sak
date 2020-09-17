package no.nav.familie.ef.sak.api.dto

data class NæreBoforhold(val næreBoforholdSøknad: Boolean,
                         val næreBoforholdPdl: Boolean,
                         val begrunnelse: String)