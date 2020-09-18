package no.nav.familie.ef.sak.api.dto

data class Samvær(val annenForeldersSamvær: String,
                  val foreldreHarSamværsavtale: String,
                  val praktiseringAvSamværet: String,
                  val harSøkerKlartMerAvOmsorgenForBarnet: Boolean,
                  val begrunnelse: String)