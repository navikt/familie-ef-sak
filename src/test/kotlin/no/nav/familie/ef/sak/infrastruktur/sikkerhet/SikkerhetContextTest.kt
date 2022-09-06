package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SikkerhetContextTest {

    @Test
    internal fun `skal ikke godkjenne kall fra familie-ef-mottak for andre applikasjoner`() {
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-personhendelse")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isFalse
        clearBrukerContext()

        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-integrasjoner")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isFalse
        clearBrukerContext()
    }

    @Test
    internal fun `skal gjenkjenne kall fra familie-ef-mottak`() {
        mockBrukerContext("", azp_name = "dev-gcp:teamfamilie:familie-ef-mottak")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isTrue
        clearBrukerContext()
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-mottak")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isTrue
        clearBrukerContext()
    }
}
