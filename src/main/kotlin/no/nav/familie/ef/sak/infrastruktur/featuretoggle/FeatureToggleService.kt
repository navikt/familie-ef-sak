package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.springframework.beans.factory.DisposableBean

interface FeatureToggleService : DisposableBean {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean

}
