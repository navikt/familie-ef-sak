package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aksjeselskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidsgiver
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssøker
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Selvstendig
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.TidligereUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.UnderUtdanning
import no.nav.familie.ef.sak.vilkår.dto.AksjeselskapDto
import no.nav.familie.ef.sak.vilkår.dto.AktivitetDto
import no.nav.familie.ef.sak.vilkår.dto.ArbeidsforholdSøknadDto
import no.nav.familie.ef.sak.vilkår.dto.ArbeidssøkerDto
import no.nav.familie.ef.sak.vilkår.dto.SelvstendigDto
import no.nav.familie.ef.sak.vilkår.dto.SærligeTilsynsbehovDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereUtdanningDto
import no.nav.familie.ef.sak.vilkår.dto.UnderUtdanningDto
import no.nav.familie.ef.sak.vilkår.dto.VirksomhetDto

object AktivitetMapper {

    fun tilDto(aktivitet: Aktivitet?, situasjon: Situasjon?, søknadBarn: Set<SøknadBarn>): AktivitetDto {
        return AktivitetDto(
                arbeidssituasjon = aktivitet?.hvordanErArbeidssituasjonen?.verdier ?: emptyList(),
                arbeidsforhold = tilArbeidforholdDto(aktivitet?.arbeidsforhold),
                selvstendig = tilSelvstendigDto(aktivitet?.firmaer),
                aksjeselskap = tilAksjeselskapDto(aktivitet?.aksjeselskap),
                arbeidssøker = tilArbeidssøkerDto(aktivitet?.arbeidssøker),
                underUtdanning = tilUnderUtdanningDto(aktivitet?.underUtdanning),
                virksomhet = aktivitet?.virksomhet?.let {
                VirksomhetDto(virksomhetsbeskrivelse = it.virksomhetsbeskrivelse)
            },
                tidligereUtdanninger = tilTidligereUtdanningDto(aktivitet?.tidligereUtdanninger),
                gjelderDeg = situasjon?.gjelderDetteDeg?.verdier ?: emptyList(),
                særligeTilsynsbehov = tilSærligeTilsynsbehovDto(søknadBarn),
                datoOppstartJobb = situasjon?.oppstartNyJobb
        )
    }

    private fun tilArbeidforholdDto(arbeidsgivere: Set<Arbeidsgiver>?): List<ArbeidsforholdSøknadDto> {
        return arbeidsgivere?.map {
            ArbeidsforholdSøknadDto(
                arbeidsgivernavn = it.arbeidsgivernavn,
                arbeidsmengde = it.arbeidsmengde,
                fastEllerMidlertidig = it.fastEllerMidlertidig,
                sluttdato = it.sluttdato,
                harSluttdato = it.harSluttdato
            )
        } ?: emptyList()

    }

    private fun tilSelvstendigDto(firmaer: Set<Selvstendig>?): List<SelvstendigDto> {
        return firmaer?.map {
            SelvstendigDto(
                firmanavn = it.firmanavn,
                arbeidsmengde = it.arbeidsmengde,
                organisasjonsnummer = it.organisasjonsnummer,
                etableringsdato = it.etableringsdato,
                hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt
            )
        } ?: emptyList()
    }

    private fun tilAksjeselskapDto(aksjeselskap: Set<Aksjeselskap>?): List<AksjeselskapDto> {
        return aksjeselskap?.map {
            AksjeselskapDto(
                navn = it.navn,
                arbeidsmengde = it.arbeidsmengde
            )
        } ?: emptyList()
    }

    private fun tilArbeidssøkerDto(arbeidssøker: Arbeidssøker?): ArbeidssøkerDto? {
        return arbeidssøker?.let {
            ArbeidssøkerDto(
                registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav,
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

    private fun tilTidligereUtdanningDto(tidligereUtdanning: Set<TidligereUtdanning>?): List<TidligereUtdanningDto> {
        return tidligereUtdanning?.map {
            TidligereUtdanningDto(
                linjeKursGrad = it.linjeKursGrad,
                fra = it.fra,
                til = it.til
            )
        } ?: emptyList()
    }

    private fun tilSærligeTilsynsbehovDto(søknadBarn: Set<SøknadBarn>): List<SærligeTilsynsbehovDto> {
        return søknadBarn.filter { it.særligeTilsynsbehov != null }.map {
            SærligeTilsynsbehovDto(
                id = it.id,
                navn = it.navn,
                erBarnetFødt = it.erBarnetFødt,
                fødselTermindato = it.fødselTermindato,
                særligeTilsynsbehov = it.særligeTilsynsbehov
            )
        }
    }
}