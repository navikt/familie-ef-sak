package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.SøkService
import no.nav.familie.ef.sak.fagsak.dto.PersonFraSøk
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UkjentBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID


internal class SøkServiceTest {

    private val pdlSaksbehandlerClient = mockk<PdlSaksbehandlerClient>()
    private val personService = mockk<PersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val adresseMapper: AdresseMapper = AdresseMapper(KodeverkServiceMock().kodeverkService())
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val søkService = SøkService(fagsakPersonService,
                                        behandlingService,
                                        personService,
                                        pdlSaksbehandlerClient,
                                        adresseMapper,
                                        fagsakService)

    @Test
    fun `skal finne personIdent, navn og adresse gitt bostedsadresse`() {

        val vegadresse = Vegadresse("23",
                                    "A",
                                    "",
                                    "Adressenavn",
                                    "",
                                    "",
                                    "0000",
                                    null,
                                    1L)

        val bostedsadresseFraPdl = listOf(Bostedsadresse(null,
                                                         LocalDate.now(),
                                                         LocalDate.now(),
                                                         null,
                                                         null,
                                                         vegadresse,
                                                         UkjentBosted(""),
                                                         null,
                                                         Metadata(historisk = false)))

        val navnFraPdl = listOf(Navn("Fornavn", "Mellomnavn", "Etternavn", Metadata(false)))

        val personFraPdl = PersonSøkResultat(hits = listOf(PersonSøkTreff(
                person = PdlPersonFraSøk(listOf(Folkeregisteridentifikator("123456789")),
                                         bostedsadresseFraPdl,
                                         navnFraPdl))),
                                             totalHits = 1,
                                             pageNumber = 1,
                                             totalPages = 1)

        every {
            pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(any())
        } returns personFraPdl
        every {
            behandlingService.hentAktivIdent(any())
        } returns "ident"
        every {
            personService.hentSøker(any())
        } returns pdlSøker(bostedsadresse = bostedsadresseFraPdl)

        val forventetResultat = PersonFraSøk(personIdent = "123456789",
                                             visningsadresse = "Adressenavn 23 A, 0000 Oslo",
                                             "Fornavn Mellomnavn Etternavn")

        val person = SøkeresultatPerson(listOf(forventetResultat), 1, 1, 1)
        assertThat(søkService.søkEtterPersonerMedSammeAdressePåBehandling(UUID.randomUUID())).isEqualTo(person)
    }
}