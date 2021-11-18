package no.nav.familie.ef.sak.vilkår.dto

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class OvergangsstønadDto(val aktivitet: AktivitetDto,
                              val sagtOppEllerRedusertStilling: SagtOppEllerRedusertStillingDto?,
                              val stønadsperiode: StønadsperiodeDto)

data class AktivitetDto(val arbeidssituasjon: List<String>,
                        val arbeidsforhold: List<ArbeidsforholdSøknadDto>,
                        val selvstendig: List<SelvstendigDto>,
                        val aksjeselskap: List<AksjeselskapDto>,
                        val arbeidssøker: ArbeidssøkerDto?,
                        val underUtdanning: UnderUtdanningDto?,
                        val virksomhet: VirksomhetDto?,
                        val tidligereUtdanninger: List<TidligereUtdanningDto>,
                        val gjelderDeg: List<String>,
                        val særligeTilsynsbehov: List<SærligeTilsynsbehovDto>,
                        val datoOppstartJobb: LocalDate?)

data class SagtOppEllerRedusertStillingDto(val sagtOppEllerRedusertStilling: String?,
                                           val årsak: String?,
                                           val dato: LocalDate?)

data class ArbeidsforholdSøknadDto(val arbeidsgivernavn: String,
                                   val arbeidsmengde: Int?,
                                   val fastEllerMidlertidig: String?,
                                   val harSluttdato: Boolean?,
                                   val sluttdato: LocalDate?)

data class SelvstendigDto(val firmanavn: String,
                          val organisasjonsnummer: String,
                          val etableringsdato: LocalDate,
                          val arbeidsmengde: Int?,
                          val hvordanSerArbeidsukenUt: String)

data class AksjeselskapDto(val navn: String,
                           val arbeidsmengde: Int?)

data class VirksomhetDto(val virksomhetsbeskrivelse: String)

data class ArbeidssøkerDto(val registrertSomArbeidssøkerNav: Boolean,
                           val villigTilÅTaImotTilbudOmArbeid: Boolean,
                           val kanDuBegynneInnenEnUke: Boolean,
                           val kanDuSkaffeBarnepassInnenEnUke: Boolean?,
                           val hvorØnskerDuArbeid: String,
                           val ønskerDuMinst50ProsentStilling: Boolean)

data class UnderUtdanningDto(val skoleUtdanningssted: String,
                             val linjeKursGrad: String,
                             val fra: LocalDate,
                             val til: LocalDate,
                             val offentligEllerPrivat: String?,
                             val heltidEllerDeltid: String?,
                             val hvorMyeSkalDuStudere: Int?,
                             val hvaErMåletMedUtdanningen: String?,
                             val utdanningEtterGrunnskolen: Boolean)

data class UtdanningDto(val linjeKursGrad: String,
                        val nårVarSkalDuVæreElevStudent: PeriodeDto)

data class SituasjonDto(val sykdom: DokumentasjonDto?,
                        val barnsSykdom: DokumentasjonDto?,
                        val manglendeBarnepass: DokumentasjonDto?,
                        val barnMedSærligeBehov: DokumentasjonDto?)

data class StønadsperiodeDto(val resterendeAvHovedperiode: String,
                             val søkerStønadFra: YearMonth?)

data class TidligereUtdanningDto(val linjeKursGrad: String,
                                 val fra: YearMonth,
                                 val til: YearMonth)

data class SærligeTilsynsbehovDto(val id: UUID,
                                  val navn: String?,
                                  val erBarnetFødt: Boolean,
                                  val fødselTermindato: LocalDate?,
                                  val særligeTilsynsbehov: String?)
