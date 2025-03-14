package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("mock-featuretoggle")
@Configuration
class FeatureToggleMock {
    @Bean
    @Primary
    fun featureToggleService(): FeatureToggleService {
        val mockk = mockk<FeatureToggleService>()
        every { mockk.isEnabled(any()) } returns true
        every { mockk.isEnabled(FeatureToggle.TillatMigrering7ÅrTilbake) } returns false
        every { mockk.isEnabled(FeatureToggle.SatsendringBrukIkkeVedtattMaxsats) } returns false
        every { mockk.isEnabled(FeatureToggle.UtviklerMedVeilederrolle) } returns false
        return mockk
    }
}
