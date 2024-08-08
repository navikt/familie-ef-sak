package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.felles.util.Timer.loggTid
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Personopplysninger
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import org.springframework.stereotype.Service

@Service
class GrunnlagsdataRegisterService(
    private val personService: PersonService,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val tidligereVedtaksperioderService: TidligereVedtaksperioderService,
    private val arbeidsforholdService: ArbeidsforholdService,
    private val kontantstøtteService: KontantstøtteService,
    private val fullmaktService: FullmaktService,
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
        val harAvsluttetArbeidsforhold =
            arbeidsforholdService.finnesAvsluttetArbeidsforholdSisteAntallMåneder(
                grunnlagsdataFraPdl.gjeldendeIdentForSøker(),
            )
        val harKontantstøttePerioder = kontantstøtteService.finnesKontantstøtteUtbetalingerPåBruker(personIdent).finnesUtbetaling

        return GrunnlagsdataDomene(
            søker = mapSøker(grunnlagsdataFraPdl.søker, grunnlagsdataFraPdl.andrePersoner),
            annenForelder = mapAnnenForelder(grunnlagsdataFraPdl.barneForeldre, tidligereVedtaksperioderAnnenForelder),
            medlUnntak = medlUnntak,
            barn = mapBarn(grunnlagsdataFraPdl.barn),
            tidligereVedtaksperioder = tidligereVedtaksperioder,
            harAvsluttetArbeidsforhold = harAvsluttetArbeidsforhold,
            harKontantstøttePerioder = harKontantstøttePerioder,
        )
    }

    fun hentPersonopplysninger(personIdent: String): Personopplysninger {
        val grunnlagsdataFraPdl = hentGrunnlagsdataFraPdl(personIdent, emptyList())
        return Personopplysninger(
            søker = mapSøker(grunnlagsdataFraPdl.søker, grunnlagsdataFraPdl.andrePersoner),
            annenForelder = mapAnnenForelder(grunnlagsdataFraPdl.barneForeldre, emptyMap()),
            barn = mapBarn(grunnlagsdataFraPdl.barn),
        )
    }

    private fun hentGrunnlagsdataFraPdl(
        personIdent: String,
        barneforeldreFraSøknad: List<String>,
    ): GrunnlagsdataFraPdl {
        val søker = personService.hentSøker(personIdent)
        val søkerMedFullmakt = søker.copy(fullmakt = fullmaktService.hentFullmakt(personIdent))
        val barn = hentPdlBarn(søkerMedFullmakt)
        val andreForeldre = hentPdlBarneForeldre(barn, personIdent, barneforeldreFraSøknad)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(søkerMedFullmakt)

        return GrunnlagsdataFraPdl(
            søker = søkerMedFullmakt,
            barn = barn,
            barneForeldre = andreForeldre,
            andrePersoner = dataTilAndreIdenter,
        )
    }

    private fun hentTidligereVedtaksperioderAnnenForelder(
        barneForeldre: Map<String, PdlAnnenForelder>,
    ): Map<String, TidligereVedtaksperioder> =
        loggTid("antall=${barneForeldre.size}") {
            barneForeldre.entries.associate { (ident, annenForelder) ->
                val folkeregisteridentifikatorer = annenForelder.folkeregisteridentifikator
                ident to tidligereVedtaksperioderService.hentTidligereVedtaksperioder(folkeregisteridentifikatorer)
            }
        }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlPersonForelderBarn> =
        pdlSøker.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .let { personService.hentPersonForelderBarnRelasjon(it) }

    private fun hentPdlBarneForeldre(
        barn: Map<String, PdlPersonForelderBarn>,
        personIdent: String,
        barneforeldrePersonIdentFraSøknad: List<String>,
    ): Map<String, PdlAnnenForelder> =
        barn
            .flatMap { it.value.forelderBarnRelasjon }
            .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .plus(barneforeldrePersonIdentFraSøknad)
            .distinct()
            .let { personService.hentAndreForeldre(it) }

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter =
            pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
                (pdlSøker.fullmakt?.map { it.motpartsPersonident } ?: emptyList()) +
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

fun GrunnlagsdataFraPdl.gjeldendeIdentForSøker() =
    this.søker.folkeregisteridentifikator
        .gjeldende()
        .ident
