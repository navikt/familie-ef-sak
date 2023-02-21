package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo

fun opprettGrunnlagsdata(
    bostedsadresse: List<Bostedsadresse> = emptyList(),
    innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
    utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList()
) = GrunnlagsdataDomene(
    Søker(
        adressebeskyttelse = null,
        bostedsadresse = bostedsadresse,
        dødsfall = null,
        forelderBarnRelasjon = emptyList(),
        fødsel = emptyList(),
        folkeregisterpersonstatus = emptyList(),
        fullmakt = emptyList(),
        kjønn = KjønnType.UKJENT,
        kontaktadresse = emptyList(),
        navn = Navn("", "", "", Metadata(false)),
        opphold = emptyList(),
        oppholdsadresse = emptyList(),
        sivilstand = emptyList(),
        statsborgerskap = emptyList(),
        tilrettelagtKommunikasjon = emptyList(),
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        vergemaalEllerFremtidsfullmakt = emptyList(),
        folkeregisteridentifikator = emptyList()
    ),
    listOf(
        AnnenForelderMedIdent(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            Navn("", null, "", Metadata(false)),
            "",
            emptyList(),
            TidligereVedtaksperioder(TidligereInnvilgetVedtak(harTidligereOvergangsstønad = true))
        )
    ),
    Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
    emptyList(),
    TidligereVedtaksperioder(
        TidligereInnvilgetVedtak(
            harTidligereOvergangsstønad = false,
            harTidligereBarnetilsyn = true,
            harTidligereSkolepenger = false
        )
    )
)

fun opprettBarnMedIdent(
    personIdent: String,
    fødsel: Fødsel? = null
) =
    BarnMedIdent(
        adressebeskyttelse = emptyList(),
        bostedsadresse = emptyList(),
        deltBosted = emptyList(),
        dødsfall = emptyList(),
        forelderBarnRelasjon = emptyList(),
        fødsel = listOfNotNull(fødsel),
        navn = Navn("", "", "", Metadata(false)),
        personIdent = personIdent
    )
