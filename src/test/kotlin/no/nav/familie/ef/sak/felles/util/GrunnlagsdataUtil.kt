package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo

fun opprettGrunnlagsdata(
    bostedsadresse: List<Bostedsadresse> = emptyList(),
    innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
    utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
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
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        vergemaalEllerFremtidsfullmakt = emptyList(),
        folkeregisteridentifikator = emptyList(),
    ),
    emptyList(),
    Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
    emptyList(),
    TidligereVedtaksperioder(
        TidligereInnvilgetVedtak(
            harTidligereOvergangsstønad = false,
            harTidligereBarnetilsyn = true,
            harTidligereSkolepenger = false,
        ),
    ),
    false,
    null,
)

fun opprettBarnMedIdent(
    personIdent: String,
    bostedsadresse: List<Bostedsadresse> = emptyList(),
    fødsel: Fødsel? = null,
    deltBosted: List<DeltBosted> = emptyList(),
) = BarnMedIdent(
    adressebeskyttelse = emptyList(),
    bostedsadresse = bostedsadresse,
    deltBosted = deltBosted,
    dødsfall = emptyList(),
    forelderBarnRelasjon = emptyList(),
    fødsel = listOfNotNull(fødsel),
    navn = Navn("", "", "", Metadata(false)),
    personIdent = personIdent,
    folkeregisterpersonstatus = null,
)
