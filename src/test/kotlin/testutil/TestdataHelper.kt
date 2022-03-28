package no.nav.familie.ef.sak.testutil

import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.kontrakter.ef.søknad.Aktivitet
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.Bosituasjon
import no.nav.familie.kontrakter.ef.søknad.Innsendingsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Personalia
import no.nav.familie.kontrakter.ef.søknad.Situasjon
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsplaner
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import java.util.UUID

fun søknad(personalia: Søknadsfelt<Personalia> = mockk(),
           innsendingsdetaljer: Søknadsfelt<Innsendingsdetaljer> = mockk(),
           sivilstandsdetaljer: Søknadsfelt<Sivilstandsdetaljer> = mockk(),
           medlemskapsdetaljer: Søknadsfelt<Medlemskapsdetaljer> = mockk(),
           bosituasjon: Søknadsfelt<Bosituasjon> = mockk(),
           sivilstandsplaner: Søknadsfelt<Sivilstandsplaner>? = mockk(),
           barn: Søknadsfelt<List<Barn>> = mockk(),
           aktivitet: Søknadsfelt<Aktivitet> = mockk(),
           situasjon: Søknadsfelt<Situasjon> = mockk(),
           stønadsstart: Søknadsfelt<Stønadsstart> = mockk()) =
        SøknadOvergangsstønad(personalia,
                              innsendingsdetaljer,
                              sivilstandsdetaljer,
                              medlemskapsdetaljer,
                              bosituasjon,
                              sivilstandsplaner,
                              barn,
                              aktivitet,
                              situasjon,
                              stønadsstart)


fun søknadsBarnTilBehandlingBarn(barn: Set<SøknadBarn>, behandlingId: UUID = UUID.randomUUID()): List<BehandlingBarn> = barn.map {
    BehandlingBarn(behandlingId = behandlingId,
                   søknadBarnId = it.id,
                   personIdent = it.fødselsnummer,
                   navn = it.navn,
                   fødselTermindato = it.fødselTermindato)
}