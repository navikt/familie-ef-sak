package no.nav.familie.ef.sak.no.nav.familie.ef.sak.blankett

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vilkår.VurderingService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BlankettServiceTest {
    private val brevClient: BrevClient = mockk(relaxed = true)
    private val samværsavtaleService: SamværsavtaleService = mockk(relaxed = true)
    private val årsakRevurderingService: ÅrsakRevurderingService = mockk(relaxed = true)
    private val vedtakService: VedtakService = mockk(relaxed = true)
    private val søknadService: SøknadService = mockk(relaxed = true)
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val behandlingService: BehandlingService = mockk(relaxed = true)
    private val blankettRepository: BlankettRepository = mockk(relaxed = true)
    private val vurderingService: VurderingService = mockk(relaxed = true)

    val ident = "12345678912"

    val fagsak = fagsak(identer=setOf(PersonIdent(ident)))
    val behandling = behandling(fagsak = fagsak,)
    val saksbehandling = saksbehandling(fagsak, behandling)

    @BeforeEach
    fun setUp() {
        every { personopplysningerService.hentGjeldeneNavn(any()) }.returns(mapOf(ident to "Test Navn"))
        every { behandlingService.hentSaksbehandling(behandling.id) }.returns(saksbehandling)
    }

    @Test
    fun `Skal lage blankett`() {
        val blankettService = BlankettService(vurderingService = vurderingService, brevClient = brevClient, blankettRepository = blankettRepository, behandlingService = behandlingService, søknadService = søknadService, personopplysningerService = personopplysningerService, vedtakService = vedtakService, årsakRevurderingService = årsakRevurderingService, samværsavtaleService = samværsavtaleService)

        val blankett = blankettService.lagBlankett(UUID.randomUUID())

        assertNotNull(blankett)
    }
}
