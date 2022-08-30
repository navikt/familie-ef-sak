package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.ForelderBarnRelasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.FullmaktMedNavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SivilstandMedNavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Personnavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator as FolkeregisteridentifikatorPdl
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon as ForelderBarnRelasjonPdl

object GrunnlagsdataMapper {

    fun mapBarn(pdlBarn: Map<String, PdlBarn>) = pdlBarn.map {
        mapBarn(it.value, it.key)
    }

    fun mapBarn(pdlBarn: PdlBarn, personIdent: String) =
        BarnMedIdent(
            fødsel = pdlBarn.fødsel,
            adressebeskyttelse = pdlBarn.adressebeskyttelse,
            navn = pdlBarn.navn.gjeldende(),
            bostedsadresse = pdlBarn.bostedsadresse,
            dødsfall = pdlBarn.dødsfall,
            deltBosted = pdlBarn.deltBosted,
            forelderBarnRelasjon = pdlBarn.forelderBarnRelasjon.mapForelderBarnRelasjon(),
            personIdent = personIdent
        )

    fun mapAnnenForelder(
        barneForeldre: Map<String, PdlAnnenForelder>,
        tidligereVedtaksperioderAnnenForelder: Map<String, TidligereVedtaksperioder>
    ) =
        barneForeldre.map {
            AnnenForelderMedIdent(
                adressebeskyttelse = it.value.adressebeskyttelse,
                personIdent = it.key,
                fødsel = it.value.fødsel,
                bostedsadresse = it.value.bostedsadresse,
                dødsfall = it.value.dødsfall,
                navn = it.value.navn.gjeldende(),
                folkeregisteridentifikator = mapFolkeregisteridentifikator(it.value.folkeregisteridentifikator),
                tidligereVedtaksperioder = tidligereVedtaksperioderAnnenForelder.getValue(it.key)
            )
        }

    fun mapSøker(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>) = Søker(
        sivilstand = mapSivivilstand(pdlSøker, andrePersoner),
        adressebeskyttelse = pdlSøker.adressebeskyttelse.gjeldende(),
        bostedsadresse = pdlSøker.bostedsadresse,
        dødsfall = pdlSøker.dødsfall.gjeldende(),
        forelderBarnRelasjon = pdlSøker.forelderBarnRelasjon.mapForelderBarnRelasjon(),
        fullmakt = mapFullmakt(pdlSøker, andrePersoner),
        fødsel = pdlSøker.fødsel,
        folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus,
        innflyttingTilNorge = pdlSøker.innflyttingTilNorge,
        kjønn = pdlSøker.kjønn.firstOrNull()?.kjønn ?: KjønnType.UKJENT,
        kontaktadresse = pdlSøker.kontaktadresse,
        navn = pdlSøker.navn.gjeldende(),
        opphold = pdlSøker.opphold,
        oppholdsadresse = pdlSøker.oppholdsadresse,
        statsborgerskap = pdlSøker.statsborgerskap,
        telefonnummer = pdlSøker.telefonnummer,
        tilrettelagtKommunikasjon = pdlSøker.tilrettelagtKommunikasjon,
        utflyttingFraNorge = pdlSøker.utflyttingFraNorge,
        vergemaalEllerFremtidsfullmakt = mapVergemålEllerFremtidsfullmakt(pdlSøker, andrePersoner),
        folkeregisteridentifikator = mapFolkeregisteridentifikator(pdlSøker.folkeregisteridentifikator)
    )

    private fun mapFolkeregisteridentifikator(list: List<FolkeregisteridentifikatorPdl>) =
        list.map { Folkeregisteridentifikator(it.ident, it.status, it.metadata.historisk) }

    private fun List<ForelderBarnRelasjonPdl>.mapForelderBarnRelasjon() =
        this.mapNotNull {
            it.relatertPersonsIdent?.let { relatertPersonsIdent ->
                ForelderBarnRelasjon(
                    relatertPersonsIdent,
                    it.relatertPersonsRolle,
                    it.minRolleForPerson
                )
            }
        }

    /**
     * Legger inn navn fra [andrePersoner] hvis personIdent finnes
     */
    private fun mapVergemålEllerFremtidsfullmakt(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>) =
        pdlSøker.vergemaalEllerFremtidsfullmakt.map { vergemaal ->
            val personIdent = vergemaal.vergeEllerFullmektig.motpartsPersonident
            personIdent?.let { andrePersoner[it] }?.navn?.gjeldende()
                ?.let { Personnavn(etternavn = it.etternavn, fornavn = it.fornavn, mellomnavn = it.mellomnavn) }
                ?.let { vergemaal.copy(vergeEllerFullmektig = vergemaal.vergeEllerFullmektig.copy(navn = it)) }
                ?: vergemaal
        }

    private fun mapSivivilstand(
        pdlSøker: PdlSøker,
        andrePersoner: Map<String, PdlPersonKort>
    ): List<SivilstandMedNavn> {
        return pdlSøker.sivilstand.map {
            val person = andrePersoner[it.relatertVedSivilstand]
            SivilstandMedNavn(
                type = Sivilstandstype.valueOf(it.type.name),
                gyldigFraOgMed = it.gyldigFraOgMed,
                relatertVedSivilstand = it.relatertVedSivilstand,
                bekreftelsesdato = it.bekreftelsesdato,
                dødsfall = person?.dødsfall?.gjeldende(),
                metadata = it.metadata,
                navn = person?.navn?.gjeldende()?.visningsnavn()
            )
        }
    }

    private fun mapFullmakt(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>): List<FullmaktMedNavn> {
        return pdlSøker.fullmakt.map {
            FullmaktMedNavn(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
                motpartsPersonident = it.motpartsPersonident,
                navn = andrePersoner[it.motpartsPersonident]?.navn?.gjeldende()?.visningsnavn(),
                områder = it.omraader
            )
        }
    }
}
