package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.junit.jupiter.api.Test

internal class TidligereVedaksperioderServiceTest {

    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val infotrygdService = mockk<InfotrygdService>()

    private val service = TidligereVedaksperioderService(
        fagsakPersonService,
        fagsakService,
        behandlingService,
        tilkjentYtelseService,
        infotrygdService
    )

    @Test
    internal fun `skal sjekke hvis man har tidligere vedtak`() {
        TODO("Not yet implemented")
    }
}
