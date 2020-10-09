package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Samvær(val spørsmålAvtaleOmDeltBosted: Boolean?= null,
                  val avtaleOmDeltBosted: Dokumentasjon? = null,
                  val skalAnnenForelderHaSamvær: String? = null,
                  val harDereSkriftligAvtaleOmSamvær: String? = null,
                  val samværsavtale: Dokumentasjon? = null,
                  val skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke: Dokumentasjon? = null,
                  val hvordanPraktiseresSamværet: String? = null,
                  val borAnnenForelderISammeHus: String? = null,
                  val borAnnenForelderISammeHusBeskrivelse: String? = null,
                  val harDereTidligereBoddSammen: Boolean? = null,
                  val nårFlyttetDereFraHverandre: LocalDate? = null,
                  val erklæringOmSamlivsbrudd: Dokumentasjon? = null,
                  val hvorMyeErDuSammenMedAnnenForelder: String? = null,
                  val beskrivSamværUtenBarn: String? = null
)
