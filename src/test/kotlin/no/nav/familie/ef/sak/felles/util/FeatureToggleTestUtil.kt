package no.nav.familie.ef.sak.felles.util

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService

fun mockFeatureToggleService(enabled: Boolean = true): FeatureToggleService {
    val mockk = mockk<FeatureToggleService>()
    every { mockk.isEnabled(any()) } returns enabled
    return mockk
}