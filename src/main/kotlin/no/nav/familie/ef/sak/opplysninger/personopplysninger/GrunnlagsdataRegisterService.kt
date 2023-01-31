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
    private val tidligereVedaksperioderService: TidligereVedaksperioderService
) {

    fun hentGrunnlagsdataFraRegister(
        personIdent: String,
        barneforeldreFraSøknad: List<String>
    ): GrunnlagsdataDomene {
        val pdlSøker = personService.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(pdlBarn, personIdent, barneforeldreFraSøknad)
        val tidligereVedtasksperioderAnnenForelder = hentTidligereVedtaksperioderAnnenForelder(barneForeldre)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(pdlSøker)

        val medlUnntak = personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        val tidligereVedtaksperioder =
            tidligereVedaksperioderService.hentTidligereVedtaksperioder(pdlSøker.alleIdenter())

        return GrunnlagsdataDomene(
            søker = mapSøker(pdlSøker, dataTilAndreIdenter),
            annenForelder = mapAnnenForelder(barneForeldre, tidligereVedtasksperioderAnnenForelder),
            medlUnntak = medlUnntak,
            barn = mapBarn(pdlBarn),
            tidligereVedtaksperioder = tidligereVedtaksperioder
        )
    }

    private fun hentTidligereVedtaksperioderAnnenForelder(
        barneForeldre: Map<String, PdlAnnenForelder>
    ): Map<String, TidligereVedtaksperioder> {
        return loggTid("antall=${barneForeldre.size}") {
            barneForeldre.entries.associate { (key, value) ->
                val personIdenter = value.folkeregisteridentifikator.map { it.ident }.toSet()
                key to tidligereVedaksperioderService.hentTidligereVedtaksperioder(personIdenter)
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
        barneforeldrePersonIdentFraSøknad: List<String>
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
