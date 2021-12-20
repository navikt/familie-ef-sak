package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import org.springframework.stereotype.Service


@Service
class GrunnlagsdataRegisterService(private val pdlClient: PdlClient,
                                   private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
                                   private val infotrygdService: InfotrygdService) {

    fun hentGrunnlagsdataFraRegister(personIdent: String,
                                     barneforeldreFraSøknad: List<String>): GrunnlagsdataDomene {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(pdlBarn, personIdent, barneforeldreFraSøknad)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(pdlSøker)

        val medlUnntak = personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        return GrunnlagsdataDomene(
                søker = mapSøker(pdlSøker, dataTilAndreIdenter),
                annenForelder = mapAnnenForelder(barneForeldre),
                medlUnntak = medlUnntak,
                barn = mapBarn(pdlBarn),
                tidligereVedtaksperioder = hentTidligereVedtaksperioder(personIdent)
        )
    }

    // TODO endre om til å bruke identer fra pdlSøker, då dette blir et ekstra kall for å hente identer
    private fun hentTidligereVedtaksperioder(personIdent: String): TidligereVedtaksperioder {
        return infotrygdService.hentPerioder(personIdent).let {
            val infotrygd = TidligereInnvilgetVedtak(
                    harTidligereOvergangsstønad = it.overgangsstønad.isNotEmpty(),
                    harTidligereBarnetilsyn = it.barnetilsyn.isNotEmpty(),
                    harTidligereSkolepenger = it.skolepenger.isNotEmpty(),
            )
            TidligereVedtaksperioder(infotrygd = infotrygd)
        }
    }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlBarn> {
        return pdlSøker.forelderBarnRelasjon
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
    }

    private fun hentPdlBarneForeldre(barn: Map<String, PdlBarn>,
                                     personIdent: String,
                                     barneforeldrePersonIdentFraSøknad: List<String>): Map<String, PdlAnnenForelder> {
        return barn.flatMap { it.value.forelderBarnRelasjon }
                .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldrePersonIdentFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }
    }

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
                           pdlSøker.fullmakt.map { it.motpartsPersonident } +
                           pdlSøker.vergemaalEllerFremtidsfullmakt.mapNotNull { it.vergeEllerFullmektig.motpartsPersonident }
        if (andreIdenter.isEmpty()) return emptyMap()
        return pdlClient.hentPersonKortBolk(andreIdenter)
    }

}
