package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.springframework.beans.factory.DisposableBean

interface FeatureToggleService : DisposableBean {

    fun isEnabled(toggle: Toggle): Boolean {
        return isEnabled(toggle, false)
    }

    fun isEnabled(toggle: Toggle, defaultValue: Boolean): Boolean
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    AUTOMATISK_MIGRERING("familie.ef.sak.automatisk-migrering"),
    MIGRERING("familie.ef.sak.migrering"),
    MIGRERING_BARNETILSYN("familie.ef.sak.migrering.barnetilsyn"),
    G_BEREGNING("familie.ef.sak.g-beregning"),
    G_OMREGNING_REVURDER_HOPP_OVER_VALIDER_TIDLIGERE_VEDTAK("familie.ef.sak.revurder-g-omregning-hopp-over-valider-tidligere-vedtak"),

    SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS("familie.ef.sak.bruk-nye-maxsatser"),

    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST("familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost", "Permission"),
    AUTOMATISK_VURDER_NYTT_BARN_SAMME_PARTNER("familie.ef.sak.nytt-barn-samme-partner"),
    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR("familie.ef.sak.frontend-automatisk-utfylle-vilkar"),
    FRONTEND_SATSENDRING("familie.ef.sak.frontend-vis-satsendring"),
    FRONTEND_VIS_INNTEKT_PERSONOVERSIKT("familie.ef.sak.frontend.vis-inntekt-personoversikt"),
    PERSONOPPLYSNINGER_ENDRINGER("familie.ef.sak.frontend.personopplysninger-endringer");

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
