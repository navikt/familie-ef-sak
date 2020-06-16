package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.kontrakter.ef.søknad.*

object OvergangsstønadMapper {

    fun tilAktivitetDto(søknad: Søknad, aktivitetsplikt: String?): AktivitetDto {
        val aktivitet = søknad.aktivitet.verdi
        return AktivitetDto(aktivitetsplikt = aktivitetsplikt,
                            arbeidssituasjon = aktivitet.hvordanErArbeidssituasjonen.verdi,
                            arbeidsforhold = tilArbeidsforholdDto(aktivitet.arbeidsforhold?.verdi),
                            selvstendig = tilSelvstendigDto(aktivitet.selvstendig?.verdi),
                            aksjeselskap = tilAksjeselskapDto(aktivitet.aksjeselskap?.verdi),
                            virksomhet = tilVirksomhetDto(aktivitet.virksomhet?.verdi),
                            arbeidssøker = tilArbeidssøkerDto(aktivitet.arbeidssøker?.verdi),
                            underUtdanning = tilUnderUtdanningDto(aktivitet.underUtdanning?.verdi),
                            situasjon = tilAnnenSituasjon(søknad.situasjon.verdi))
    }

    fun tilSagtOppEllerRedusertStilling(situasjon: Situasjon): SagtOppEllerRedusertStillingDto? {
        val sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling ?: return null
        return SagtOppEllerRedusertStillingDto(sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling.verdi,
                                               årsak = situasjon.oppsigelseReduksjonÅrsak?.verdi,
                                               dato = situasjon.oppsigelseReduksjonTidspunkt?.verdi,
                                               vedlegg = tilVedleggDto(situasjon.oppsigelseDokumentasjon))
    }

    private fun tilVirksomhetDto(virksomhet: Virksomhet?): VirksomhetDto? =
            virksomhet?.let { VirksomhetDto(virksomhetsbeskrivelse = it.virksomhetsbeskrivelse.verdi) }

    private fun tilAksjeselskapDto(aksjeselskaper: List<Aksjeselskap>?): List<AksjeselskapDto> =
            aksjeselskaper?.map {
                AksjeselskapDto(navn = it.navn.verdi,
                                arbeidsmengde = it.arbeidsmengde.verdi)
            } ?: emptyList()

    private fun tilArbeidsforholdDto(arbedsgivere: List<Arbeidsgiver>?): List<ArbeidsforholdDto> =
            arbedsgivere?.map {
                ArbeidsforholdDto(arbeidsgivernavn = it.arbeidsgivernavn.verdi,
                                  arbeidsmengde = it.arbeidsmengde.verdi,
                                  fastEllerMidlertidig = it.fastEllerMidlertidig.verdi,
                                  sluttdato = it.sluttdato?.verdi)
            } ?: emptyList()

    private fun tilSelvstendigDto(selvstendig: Selvstendig?): SelvstendigDto? =
            selvstendig?.let {
                SelvstendigDto(firmanavn = it.firmanavn.verdi,
                               organisasjonsnummer = it.organisasjonsnummer.verdi,
                               etableringsdato = it.etableringsdato.verdi,
                               arbeidsmengde = it.arbeidsmengde.verdi,
                               hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt.verdi)
            }

    private fun tilArbeidssøkerDto(arbeidssøker: Arbeidssøker?): ArbeidssøkerDto? =
            arbeidssøker?.let {
                ArbeidssøkerDto(registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav.verdi,
                                villigTilÅTaImotTilbudOmArbeid = it.villigTilÅTaImotTilbudOmArbeid.verdi,
                                kanDuBegynneInnenEnUke = it.kanDuBegynneInnenEnUke.verdi,
                                kanDuSkaffeBarnepassInnenEnUke = it.kanDuSkaffeBarnepassInnenEnUke.verdi,
                                hvorØnskerDuArbeid = it.hvorØnskerDuArbeid.verdi,
                                ønskerDuMinst50ProsentStilling = it.ønskerDuMinst50ProsentStilling.verdi
                )
            }

    private fun tilAnnenSituasjon(situasjon: Situasjon): SituasjonDto =
            SituasjonDto(sykdom = tilVedleggDto(situasjon.sykdom),
                         barnsSykdom = tilVedleggDto(situasjon.barnsSykdom),
                         manglendeBarnepass = tilVedleggDto(situasjon.manglendeBarnepass),
                         barnMedSærligeBehov = tilVedleggDto(situasjon.barnMedSærligeBehov))

    private fun tilVedleggDto(dokument: Søknadsfelt<List<Dokument>>?) =
            dokument?.verdi?.map { VedleggDto(it.tittel) } ?: emptyList()

    private fun tilUnderUtdanningDto(underUtdanning: UnderUtdanning?): UnderUtdanningDto? =
            underUtdanning?.let {
                UnderUtdanningDto(skoleUtdanningssted = it.skoleUtdanningssted.verdi,
                                  utdanning = tilUtdanningDto(it.utdanning.verdi),
                                  offentligEllerPrivat = it.offentligEllerPrivat.verdi,
                                  heltidEllerDeltid = it.heltidEllerDeltid.verdi,
                                  hvorMyeSkalDuStudere = it.hvorMyeSkalDuStudere?.verdi,
                                  hvaErMåletMedUtdanningen = it.hvaErMåletMedUtdanningen?.verdi,
                                  utdanningEtterGrunnskolen = it.utdanningEtterGrunnskolen.verdi,
                                  tidligereUtdanninger = it.tidligereUtdanninger?.verdi?.map(this::tilUtdanningDto)
                                                         ?: emptyList())
            }

    private fun tilUtdanningDto(utdanning: Utdanning): UtdanningDto =
            UtdanningDto(linjeKursGrad = utdanning.linjeKursGrad.verdi,
                         nårVarSkalDuVæreElevStudent = tilPeriodeDto(utdanning.nårVarSkalDuVæreElevStudent.verdi))

    private fun tilPeriodeDto(periode: Periode): PeriodeDto =
            PeriodeDto(fraMåned = periode.fraMåned,
                       fraÅr = periode.fraÅr,
                       tilMåned = periode.tilMåned,
                       tilÅr = periode.tilÅr)
}
