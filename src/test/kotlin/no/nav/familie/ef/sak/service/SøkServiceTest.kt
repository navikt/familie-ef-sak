package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.SøkService
import no.nav.familie.ef.sak.fagsak.dto.PersonFraSøkEkstraInfo
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPersonEkstra
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlSøker
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.ukjentBostedsadresse
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
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
    private val vurderingService = mockk<VurderingService>()
    private val søkService =
        SøkService(
            fagsakPersonService,
            behandlingService,
            personService,
            pdlSaksbehandlerClient,
            adresseMapper,
            fagsakService,
            vurderingService,
        )

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(any()) } returns "ident"
        every { vurderingService.hentGrunnlagOgMetadata(any()) } returns
            Pair(
                mockVilkårGrunnlagDto(),
                HovedregelMetadata(
                    null,
                    Sivilstandstype.UGIFT,
                    false,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    mockk(),
                    mockk(),
                ),
            )
    }

    @Test
    fun `skal finne personIdent, navn og adresse gitt bostedsadresse`() {
        val vegadresse =
            Vegadresse(
                "23",
                "A",
                "",
                "Adressenavn",
                "",
                "",
                "0000",
                null,
                1L,
            )

        val bostedsadresseFraPdl =
            listOf(
                Bostedsadresse(
                    null,
                    LocalDate.now(),
                    LocalDate.now(),
                    null,
                    null,
                    vegadresse,
                    null,
                    null,
                    Metadata(historisk = false),
                ),
            )

        val navnFraPdl = listOf(Navn("Fornavn", "Mellomnavn", "Etternavn", Metadata(false)))

        val personFraPdl =
            PersonSøkResultat(
                hits =
                    listOf(
                        PersonSøkTreff(
                            person =
                                PdlPersonFraSøk(
                                    listOf(FolkeregisteridentifikatorFraSøk("123456789")),
                                    bostedsadresseFraPdl,
                                    navnFraPdl,
                                ),
                        ),
                    ),
                totalHits = 1,
                pageNumber = 1,
                totalPages = 1,
            )

        every {
            pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(any())
        } returns personFraPdl
        every {
            personService.hentSøker(any())
        } returns pdlSøker(bostedsadresse = bostedsadresseFraPdl)

        val forventetResultat =
            PersonFraSøkEkstraInfo(
                personIdent = "123456789",
                visningsadresse = "Adressenavn 23 A, 0000 Oslo",
                "Fornavn Mellomnavn Etternavn",
                fødselsdato = null,
                erSøker = false,
                erBarn = false,
            )

        val person = SøkeresultatPersonEkstra(listOf(forventetResultat))
        assertThat(søkService.søkEtterPersonerMedSammeAdressePåBehandling(UUID.randomUUID())).isEqualTo(person)
    }

    @Test
    internal fun `skal kaste feil hvis personen har ukjent adresse`() {
        every {
            personService.hentSøker(any())
        } returns pdlSøker(bostedsadresse = listOf(ukjentBostedsadresse()))

        assertThatThrownBy { søkService.søkEtterPersonerMedSammeAdressePåBehandling(UUID.randomUUID()) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("ukjent bostedsadresse")
    }
}
