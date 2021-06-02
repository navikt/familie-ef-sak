package no.nav.familie.ef.sak.no.nav.familie.ef.sak.util

import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.integration.dto.pdl.KjønnType
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo

fun opprettGrunnlagsdata() = GrunnlagsdataDomene(
        Søker(adressebeskyttelse = null,
              bostedsadresse = emptyList(),
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
              telefonnummer = emptyList(),
              tilrettelagtKommunikasjon = emptyList(),
              innflyttingTilNorge = emptyList(),
              utflyttingFraNorge = emptyList(),
              vergemaalEllerFremtidsfullmakt = emptyList()
        ),
        emptyList(),
        Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
        emptyList()
)