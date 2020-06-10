package no.nav.familie.ef.sak.api.gui.dto

data class Samvær(val annenForeldersSamvær: String,
                  val foreldreHarSamværsavtale: String,
                  val praktiseringAvSamværet: String,
                  val harSøkerKlartMerAvOmsorgenForBarnet: Boolean,
                  val begrunnelse: String)