package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.springframework.beans.factory.DisposableBean

interface FeatureToggleService : DisposableBean {

    fun isEnabled(toggle: Toggle): Boolean {
        return isEnabled(toggle, false)
    }

    fun isEnabled(toggle: Toggle, defaultValue: Boolean): Boolean
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    WEBCLIENT("familie.ef.sak.webclient"),
    AUTOMATISK_MIGRERING("familie.ef.sak.automatisk-migrering"),
    MIGRERING("familie.ef.sak.migrering"),
    MIGRERING_BARNETILSYN("familie.ef.sak.migrering.barnetilsyn"),
    G_BEREGNING("familie.ef.sak.g-beregning"),
    G_OMREGNING_REVURDER_HOPP_OVER_VALIDER_TIDLIGERE_VEDTAK("familie.ef.sak.revurder-g-omregning-hopp-over-valider-tidligere-vedtak"),

    SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS("familie.ef.sak.bruk-nye-maxsatser"),

    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST("familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost"),

    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    PÅ_VENT_NULLSTILL_VEDTAK("familie.ef.sak.paa-vent-nullstill-vedtak"),

    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR("familie.ef.sak.frontend-automatisk-utfylle-vilkar"),
    FRONTEND_SATSENDRING("familie.ef.sak.frontend-vis-satsendring"),
    FØRSTEGANGSBEHANDLING("familie.ef.sak.opprett-forstegangsbehandling"),
    AUTOMATISK_JOURNALFØR_REVURDERING("familie.ef.sak.automatisk-journalfor-revurdering"),
    HISTORISK_PENSJON("familie.ef.sak.historisk-pensjon"),
    PERSONOPPLYSNINGER_ENDRINGER("familie.ef.sak.frontend.personopplysninger-endringer");

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
