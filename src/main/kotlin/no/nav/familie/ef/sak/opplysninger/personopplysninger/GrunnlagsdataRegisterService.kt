package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.felles.util.Timer.loggTid
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import org.springframework.stereotype.Service

@Service
class GrunnlagsdataRegisterService(
    private val personService: PersonService,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val tidligereVedtaksperioderService: TidligereVedtaksperioderService,
) {

    fun hentGrunnlagsdataFraRegister(
        personIdent: String,
        barneforeldreFraSøknad: List<String>,
    ): GrunnlagsdataDomene {
        val grunnlagsdataFraPdl = hentGrunnlagsdataFraPdl(personIdent, emptyList())
        val medlUnntak = personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(personIdent)
        val tidligereVedtaksperioder =
            tidligereVedtaksperioderService.hentTidligereVedtaksperioder(grunnlagsdataFraPdl.søker.folkeregisteridentifikator)
        val tidligereVedtaksperioderAnnenForelder = hentTidligereVedtaksperioderAnnenForelder(grunnlagsdataFraPdl.barneForeldre)

        return GrunnlagsdataDomene(
            søker = mapSøker(grunnlagsdataFraPdl.søker, grunnlagsdataFraPdl.andrePersoner),
            annenForelder = mapAnnenForelder(grunnlagsdataFraPdl.barneForeldre, tidligereVedtaksperioderAnnenForelder),
            medlUnntak = medlUnntak,
            barn = mapBarn(grunnlagsdataFraPdl.barn),
            tidligereVedtaksperioder = tidligereVedtaksperioder,
        )
    }

    fun hentGrunnlagsdataUtenVedtakshitorikkFraRegister(
        personIdent: String,
    ): GrunnlagsdataDomene {
        val grunnlagsdataFraPdl = hentGrunnlagsdataFraPdl(personIdent, emptyList())
        val medlUnntak = personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(personIdent)
        return GrunnlagsdataDomene(
            søker = mapSøker(grunnlagsdataFraPdl.søker, grunnlagsdataFraPdl.andrePersoner),
            annenForelder = mapAnnenForelder(grunnlagsdataFraPdl.barneForeldre, emptyMap()),
            medlUnntak = medlUnntak,
            barn = mapBarn(grunnlagsdataFraPdl.barn),
            tidligereVedtaksperioder = null,
        )
    }

    private fun hentGrunnlagsdataFraPdl(personIdent: String, barneforeldreFraSøknad: List<String>): GrunnlagsdataFraPdl {
        val søker = personService.hentSøker(personIdent)
        val barn = hentPdlBarn(søker)
        val andreForeldre = hentPdlBarneForeldre(barn, personIdent, barneforeldreFraSøknad)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(søker)

        return GrunnlagsdataFraPdl(
            søker = søker,
            barn = barn,
            barneForeldre = andreForeldre,
            andrePersoner = dataTilAndreIdenter,
        )
    }

    private fun hentTidligereVedtaksperioderAnnenForelder(
        barneForeldre: Map<String, PdlAnnenForelder>,
    ): Map<String, TidligereVedtaksperioder> {
        return loggTid("antall=${barneForeldre.size}") {
            barneForeldre.entries.associate { (ident, annenForelder) ->
                val folkeregisteridentifikatorer = annenForelder.folkeregisteridentifikator
                ident to tidligereVedtaksperioderService.hentTidligereVedtaksperioder(folkeregisteridentifikatorer)
            }
        }
    }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlPersonForelderBarn> {
        return pdlSøker.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .let { personService.hentPersonForelderBarnRelasjon(it) }
    }

    private fun hentPdlBarneForeldre(
        barn: Map<String, PdlPersonForelderBarn>,
        personIdent: String,
        barneforeldrePersonIdentFraSøknad: List<String>,
    ): Map<String, PdlAnnenForelder> {
        return barn.flatMap { it.value.forelderBarnRelasjon }
            .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .plus(barneforeldrePersonIdentFraSøknad)
            .distinct()
            .let { personService.hentAndreForeldre(it) }
    }

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
            pdlSøker.fullmakt.map { it.motpartsPersonident } +
            pdlSøker.vergemaalEllerFremtidsfullmakt.mapNotNull { it.vergeEllerFullmektig.motpartsPersonident }
        if (andreIdenter.isEmpty()) return emptyMap()
        return personService.hentPersonKortBolk(andreIdenter)
    }
}

data class GrunnlagsdataFraPdl(
    val søker: PdlSøker,
    val barn: Map<String, PdlPersonForelderBarn>,
    val barneForeldre: Map<String, PdlAnnenForelder>,
    val andrePersoner: Map<String, PdlPersonKort>,
)
