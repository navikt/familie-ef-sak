package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.kontrakter.ef.søknad.*
import java.time.YearMonth

object OvergangsstønadMapper {

    fun tilAktivitetDto(søknad: SøknadOvergangsstønad, aktivitetsplikt: String?): AktivitetDto {
        val aktivitet = søknad.aktivitet.verdi
        return AktivitetDto(aktivitetsplikt = aktivitetsplikt,
                            arbeidssituasjon = aktivitet.hvordanErArbeidssituasjonen.verdi,
                            arbeidsforhold = tilArbeidsforholdDto(aktivitet.arbeidsforhold?.verdi),
                            selvstendig = tilSelvstendigDto(aktivitet.firmaer?.verdi),
                            aksjeselskap = tilAksjeselskapDto(aktivitet.aksjeselskap?.verdi),
                            virksomhet = tilVirksomhetDto(aktivitet.virksomhet?.verdi),
                            arbeidssøker = tilArbeidssøkerDto(aktivitet.arbeidssøker?.verdi),
                            underUtdanning = tilUnderUtdanningDto(aktivitet.underUtdanning?.verdi),
                            situasjon = tilAnnenSituasjon(søknad.situasjon.verdi))
    }

    fun tilSagtOppEllerRedusertStilling(situasjon: Situasjon): SagtOppEllerRedusertStillingDto? {
        val sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling ?: return null
        return SagtOppEllerRedusertStillingDto(sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling.svarId,
                                               årsak = situasjon.oppsigelseReduksjonÅrsak?.verdi,
                                               dato = situasjon.oppsigelseReduksjonTidspunkt?.verdi,
                                               dokumentasjon = tilDokumentasjonDto(situasjon.oppsigelseDokumentasjon))
    }

    fun tilStønadsperiode(stønadsstart: Stønadsstart): StønadsperiodeDto {
        val søkerStønadFra = if (stønadsstart.fraMåned != null && stønadsstart.fraÅr != null)
            YearMonth.of(stønadsstart.fraÅr!!.verdi, stønadsstart.fraMåned!!.verdi) else null
        return StønadsperiodeDto(
                // TODO - resterendeAvHovedperiode:
                //  her må vi kanskje vite om det er en førstegangssøkere eller historikk fra tidligere søknader
                resterendeAvHovedperiode = "Ikke implementert - Hovedperiode (36 mnd) minus tidligere brukt periode",
                søkerStønadFra = søkerStønadFra
        )
    }

    private fun tilVirksomhetDto(virksomhet: Virksomhet?): VirksomhetDto? =
            virksomhet?.let {
                VirksomhetDto(virksomhetsbeskrivelse = it.virksomhetsbeskrivelse.verdi,
                              dokumentasjon = tilDokumentasjonDto(it.dokumentasjon))
            }

    private fun tilAksjeselskapDto(aksjeselskaper: List<Aksjeselskap>?): List<AksjeselskapDto> =
            aksjeselskaper?.map {
                AksjeselskapDto(navn = it.navn.verdi,
                                arbeidsmengde = it.arbeidsmengde?.verdi)
            } ?: emptyList()

    private fun tilArbeidsforholdDto(arbedsgivere: List<Arbeidsgiver>?): List<ArbeidsforholdDto> =
            arbedsgivere?.map {
                ArbeidsforholdDto(arbeidsgivernavn = it.arbeidsgivernavn.verdi,
                                  arbeidsmengde = it.arbeidsmengde?.verdi,
                                  fastEllerMidlertidig = it.fastEllerMidlertidig.verdi,
                                  sluttdato = it.sluttdato?.verdi)
            } ?: emptyList()

    private fun tilSelvstendigDto(selvstendig: List<Selvstendig>?): List<SelvstendigDto> =
            selvstendig?.map {
                SelvstendigDto(firmanavn = it.firmanavn.verdi,
                               organisasjonsnummer = it.organisasjonsnummer.verdi,
                               etableringsdato = it.etableringsdato.verdi,
                               arbeidsmengde = it.arbeidsmengde?.verdi,
                               hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt.verdi)
            } ?: emptyList()

    private fun tilArbeidssøkerDto(arbeidssøker: Arbeidssøker?): ArbeidssøkerDto? =
            arbeidssøker?.let {
                ArbeidssøkerDto(registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav.verdi,
                                villigTilÅTaImotTilbudOmArbeid = it.villigTilÅTaImotTilbudOmArbeid.verdi,
                                kanDuBegynneInnenEnUke = it.kanDuBegynneInnenEnUke.verdi,
                                kanDuSkaffeBarnepassInnenEnUke = it.kanDuSkaffeBarnepassInnenEnUke?.verdi,
                                hvorØnskerDuArbeid = it.hvorØnskerDuArbeid.verdi,
                                ønskerDuMinst50ProsentStilling = it.ønskerDuMinst50ProsentStilling.verdi,
                                ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon =
                                tilDokumentasjonDto(it.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon)
                )
            }

    private fun tilAnnenSituasjon(situasjon: Situasjon): SituasjonDto =
            SituasjonDto(sykdom = tilDokumentasjonDto(situasjon.sykdom),
                         barnsSykdom = tilDokumentasjonDto(situasjon.barnsSykdom),
                         manglendeBarnepass = tilDokumentasjonDto(situasjon.manglendeBarnepass),
                         barnMedSærligeBehov = tilDokumentasjonDto(situasjon.barnMedSærligeBehov))

    private fun tilDokumentasjonDto(dokument: Søknadsfelt<Dokumentasjon>?) =
            dokument?.verdi?.let {
                DokumentasjonDto(it.harSendtInnTidligere.verdi,
                                 it.dokumenter.map { dokument -> VedleggDto(dokument.id, dokument.navn) })
            }

    private fun tilUnderUtdanningDto(underUtdanning: UnderUtdanning?): UnderUtdanningDto? =
            underUtdanning?.let {
                UnderUtdanningDto(skoleUtdanningssted = it.skoleUtdanningssted.verdi,
                                  utdanning = tilUtdanningDto(it.gjeldendeUtdanning?.verdi),
                                  offentligEllerPrivat = it.offentligEllerPrivat.verdi,
                                  heltidEllerDeltid = it.heltidEllerDeltid.verdi,
                                  hvorMyeSkalDuStudere = it.hvorMyeSkalDuStudere?.verdi,
                                  hvaErMåletMedUtdanningen = it.hvaErMåletMedUtdanningen?.verdi,
                                  utdanningEtterGrunnskolen = it.utdanningEtterGrunnskolen.verdi,
                                  tidligereUtdanninger = it.tidligereUtdanninger?.verdi?.map(this::tilUtdanningDto)
                                                         ?: emptyList())
            }

    private fun tilUtdanningDto(utdanning: TidligereUtdanning): UtdanningDto {
        return UtdanningDto(linjeKursGrad = utdanning.linjeKursGrad.verdi,
                            nårVarSkalDuVæreElevStudent = tilPeriodeÅrMånedDto(utdanning.nårVarSkalDuVæreElevStudent.verdi))
    }

    private fun tilUtdanningDto(utdanning: GjeldendeUtdanning?): UtdanningDto? {
        utdanning ?: return null
        return UtdanningDto(linjeKursGrad = utdanning.linjeKursGrad.verdi,
                            nårVarSkalDuVæreElevStudent = tilPeriodeÅrMånedDto(utdanning.nårVarSkalDuVæreElevStudent.verdi))
    }

    private fun tilPeriodeÅrMånedDto(periode: Datoperiode): PeriodeDto =
            PeriodeDto(fra = YearMonth.of(periode.fra.year, periode.fra.month),
                       til = YearMonth.of(periode.til.year, periode.til.month))

    private fun tilPeriodeÅrMånedDto(periode: MånedÅrPeriode): PeriodeDto =
            PeriodeDto(fra = YearMonth.of(periode.fraÅr, periode.fraMåned),
                       til = YearMonth.of(periode.tilÅr, periode.tilMåned))
}
