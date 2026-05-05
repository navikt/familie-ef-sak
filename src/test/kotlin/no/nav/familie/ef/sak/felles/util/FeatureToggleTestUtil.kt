package no.nav.familie.ef.sak.felles.util

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle

fun mockFeatureToggleService(enabled: Boolean = true): FeatureToggleService {
    val mockk = mockk<FeatureToggleService>()
    every { mockk.isEnabled(any()) } returns enabled
    every { mockk.isEnabled(Toggle.SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS) } returns false
    every { mockk.isEnabled(Toggle.INNVILGE_KUN_OPPHØR_OG_SANKSJON) } returns false
    return mockk
}
