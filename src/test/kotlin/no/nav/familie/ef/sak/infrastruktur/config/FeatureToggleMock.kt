package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.unleash.DefaultUnleashService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class FeatureToggleMock {

    @Profile("mock-featuretoggle")
    @Bean
    @Primary
    fun featureToggleService(): FeatureToggleService {
        val mockk = mockk<FeatureToggleService>()
        every { mockk.isEnabled(any()) } returns true
        every { mockk.isEnabled(Toggle.TILLAT_MIGRERING_5_ÅR_TILBAKE) } returns false
        every { mockk.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns false
        every { mockk.isEnabled(Toggle.SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS) } returns false
        return mockk
    }

    @Profile("mock-featuretoggle-next")
    @Bean
    @Primary
    fun featureToggleNextService(): DefaultUnleashService {
        val mockk = mockk<DefaultUnleashService>()
        every { mockk.isEnabled(any()) } returns true
        return mockk
    }
}
