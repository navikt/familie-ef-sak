package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Aksjeselskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Arbeidsgiver
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Arbeidssøker
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Barn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Selvstendig
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.TidligereUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.UnderUtdanning
import no.nav.familie.ef.sak.vilkår.AksjeselskapDto
import no.nav.familie.ef.sak.vilkår.AktivitetDto
import no.nav.familie.ef.sak.vilkår.ArbeidsforholdDto
import no.nav.familie.ef.sak.vilkår.ArbeidssøkerDto
import no.nav.familie.ef.sak.vilkår.SelvstendigDto
import no.nav.familie.ef.sak.vilkår.SærligeTilsynsbehovDto
import no.nav.familie.ef.sak.vilkår.TidligereUtdanningDto
import no.nav.familie.ef.sak.vilkår.UnderUtdanningDto
import no.nav.familie.ef.sak.vilkår.VirksomhetDto

object AktivitetMapper {

    fun tilDto(aktivitet: Aktivitet, situasjon: Situasjon, barn: Set<Barn>): AktivitetDto {
        return AktivitetDto(arbeidssituasjon = aktivitet.hvordanErArbeidssituasjonen.verdier,
                            arbeidsforhold = tilArbeidforholdDto(aktivitet.arbeidsforhold),
                            selvstendig = tilSelvstendigDto(aktivitet.firmaer),
                            aksjeselskap = tilAksjeselskapDto(aktivitet.aksjeselskap),
                            arbeidssøker = tilArbeidssøkerDto(aktivitet.arbeidssøker),
                            underUtdanning = tilUnderUtdanningDto(aktivitet.underUtdanning),
                            virksomhet = aktivitet.virksomhet?.let {
                                VirksomhetDto(virksomhetsbeskrivelse = it.virksomhetsbeskrivelse)
                            },
                            tidligereUtdanninger = tilTidligereUtdanningDto(aktivitet.tidligereUtdanninger),
                            gjelderDeg = situasjon.gjelderDetteDeg.verdier,
                            særligeTilsynsbehov = tilSærligeTilsynsbehovDto(barn),
                            datoOppstartJobb = situasjon.oppstartNyJobb
        )
    }

    private fun tilArbeidforholdDto(arbeidsgivere: Set<Arbeidsgiver>?): List<ArbeidsforholdDto> {
        return arbeidsgivere?.map {
            ArbeidsforholdDto(arbeidsgivernavn = it.arbeidsgivernavn,
                              arbeidsmengde = it.arbeidsmengde,
                              fastEllerMidlertidig = it.fastEllerMidlertidig,
                              sluttdato = it.sluttdato,
                              harSluttdato = it.harSluttdato)
        } ?: emptyList()

    }

    private fun tilSelvstendigDto(firmaer: Set<Selvstendig>?): List<SelvstendigDto> {
        return firmaer?.map {
            SelvstendigDto(firmanavn = it.firmanavn,
                           arbeidsmengde = it.arbeidsmengde,
                           organisasjonsnummer = it.organisasjonsnummer,
                           etableringsdato = it.etableringsdato,
                           hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt)
        } ?: emptyList()
    }

    private fun tilAksjeselskapDto(aksjeselskap: Set<Aksjeselskap>?): List<AksjeselskapDto> {
        return aksjeselskap?.map {
            AksjeselskapDto(navn = it.navn,
                            arbeidsmengde = it.arbeidsmengde)
        } ?: emptyList()
    }

    private fun tilArbeidssøkerDto(arbeidssøker: Arbeidssøker?): ArbeidssøkerDto? {
        return arbeidssøker?.let {
            ArbeidssøkerDto(registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav,
                            villigTilÅTaImotTilbudOmArbeid = it.villigTilÅTaImotTilbudOmArbeid,
                            kanDuBegynneInnenEnUke = it.kanDuBegynneInnenEnUke,
                            kanDuSkaffeBarnepassInnenEnUke = it.kanDuSkaffeBarnepassInnenEnUke,
                            hvorØnskerDuArbeid = it.hvorØnskerDuArbeid,
                            ønskerDuMinst50ProsentStilling = it.ønskerDuMinst50ProsentStilling
            )
        }
    }

    private fun tilUnderUtdanningDto(utdanning: UnderUtdanning?): UnderUtdanningDto? {
        return utdanning?.let {
            UnderUtdanningDto(
                    skoleUtdanningssted = utdanning.skoleUtdanningssted,
                    linjeKursGrad = utdanning.linjeKursGrad,
                    fra = utdanning.fra,
                    til = utdanning.til,
                    offentligEllerPrivat = utdanning.offentligEllerPrivat,
                    heltidEllerDeltid = utdanning.heltidEllerDeltid,
                    hvaErMåletMedUtdanningen = utdanning.hvaErMåletMedUtdanningen,
                    hvorMyeSkalDuStudere = utdanning.hvorMyeSkalDuStudere,
                    utdanningEtterGrunnskolen = utdanning.utdanningEtterGrunnskolen
            )
        }
    }

    private fun tilTidligereUtdanningDto(tidligereUtdanning: Set<TidligereUtdanning>): List<TidligereUtdanningDto> {
        return tidligereUtdanning.map {
            TidligereUtdanningDto(linjeKursGrad = it.linjeKursGrad,
                                  fra = it.fra,
                                  til = it.til
            )
        }
    }

    private fun tilSærligeTilsynsbehovDto(barn: Set<Barn>): List<SærligeTilsynsbehovDto> {
        return barn.filter { it.særligeTilsynsbehov != null }.map {
            SærligeTilsynsbehovDto(id = it.id,
                                   navn = it.navn,
                                   erBarnetFødt = it.erBarnetFødt,
                                   fødselTermindato = it.fødselTermindato,
                                   særligeTilsynsbehov = it.særligeTilsynsbehov)
        }
    }
}