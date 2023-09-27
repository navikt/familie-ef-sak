package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.getunleash.strategy.Strategy
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.unleash.DefaultUnleashService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("mock-featuretoggle")
@Configuration
class FeatureToggleMock {

    @Bean
    fun defaultUnleashService(strategies: List<Strategy>): DefaultUnleashService {
        return mockk()
    }

    @Bean
    @Primary
    fun featureToggleService(): FeatureToggleService {
        val mockk = mockk<FeatureToggleService>()
        every { mockk.isEnabled(any()) } returns true
        every { mockk.isEnabled(Toggle.TILLAT_MIGRERING_5_ÅR_TILBAKE) } returns false
        every { mockk.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns false
        every { mockk.isEnabled(Toggle.SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS) } returns false
        every { mockk.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE) } returns false
        return mockk
    }
}
