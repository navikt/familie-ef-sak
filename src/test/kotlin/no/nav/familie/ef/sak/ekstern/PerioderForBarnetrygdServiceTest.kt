package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InternPeriodeTestUtil.lagInternPeriode
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PerioderForBarnetrygdServiceTest {

    private val periodeService = mockk<PeriodeService>()

    private val service = PerioderForBarnetrygdService(periodeService)

    private val personIdent = "123"


    @Test
    internal fun `skal ikke ha med perioder som ikke har fullOvergangsstønad`() {
        every { periodeService.hentPerioderFraEfOgInfotrygd(any()) } returns listOf(lagInternPeriode(inntektsreduksjon = 1),
                                                                                    lagInternPeriode(samordningsfradrag = 1))
        assertThat(service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder).isEmpty()
    }

    @Test
    internal fun `skal returnere perioder`() {
        every { periodeService.hentPerioderFraEfOgInfotrygd(any()) } returns listOf(lagInternPeriode(beløp = 1))
        assertThat(service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder).hasSize(1)
    }

}
