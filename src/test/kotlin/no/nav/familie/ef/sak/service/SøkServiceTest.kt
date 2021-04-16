package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.dto.PersonFraSøk
import no.nav.familie.ef.sak.api.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.integration.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.integration.dto.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.integration.dto.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.integration.dto.pdl.UkjentBosted
import no.nav.familie.ef.sak.integration.dto.pdl.Vegadresse
import no.nav.familie.ef.sak.mapper.AdresseMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.KodeverkServiceMock
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.pdlSøker
import no.nav.familie.ef.sak.repository.FagsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID


internal class SøkServiceTest {

    private val pdlSaksbehandlerClient: PdlSaksbehandlerClient = mockk()
    private val personService: PersonService = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val adresseMapper: AdresseMapper = AdresseMapper(KodeverkServiceMock().kodeverkService())
    private val behandlingService: BehandlingService = mockk()
    private val søkService = SøkService(fagsakRepository, behandlingService, personService, pdlSaksbehandlerClient, adresseMapper)

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

        val bostedsadresseFraPdl = listOf(Bostedsadresse(LocalDate.now(),
                                                         null,
                                                         Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now()),
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
        assertThat(søkService.søkEtterPersonerMedSammeAdresse(UUID.randomUUID())).isEqualTo(person)
    }
}