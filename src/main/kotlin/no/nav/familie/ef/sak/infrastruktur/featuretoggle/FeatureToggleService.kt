package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(val unleashService: UnleashService) {
    fun isEnabled(toggle: Toggle): Boolean {
        return unleashService.isEnabled(toggle.toggleId)
    }

    fun isEnabled(
        toggle: Toggle,
        defaultValue: Boolean,
    ): Boolean {
        return unleashService.isEnabled(toggle.toggleId, defaultValue)
    }
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    // Release
    G_BEREGNING_INKLUDER_SATT_PÅ_VENT(
        "familie.ef.sak.inkluder-satt-pa-vent-gomregning",
        "Usikker på om vi ønsker denne eller ikke. Ta en vurdering før 2024?",
    ),
    DEAKTIVERE_MIKROFRONTEND_FOR_INAKTIVE_BRUKERE("familie.ef.sak.deaktiver-mikrofrontend-for-inaktive-brukere"),

    // Operational
    AUTOMATISK_MIGRERING("familie.ef.sak.automatisk-migrering", "Kan denne slettes?"),
    G_BEREGNING("familie.ef.sak.g-beregning", "Operational"),
    G_BEREGNING_SCHEDULER("familie.ef.sak.g-beregning-scheduler", "Operational"),
    SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS("familie.ef.sak.bruk-nye-maxsatser", "Operational"),
    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER(
        "familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler",
        "Operational- kun preprod",
    ),
    FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR(
        "familie.ef.sak.frontend-automatisk-utfylle-vilkar",
        "Operational - kun preprod",
    ),
    FRONTEND_TILBAKEKREVING_UNDER_4X_RETTSGEBYR("familie.ef.sak.frontend.tilbakekreving-under-4x-rettsgebyr",
        "Release"),
    AUTOMATISKE_BREV_INNHENTING_KARAKTERUTSKRIFT(
        "familie.ef.sak.automatiske-brev-innhenting-karakterutskrift",
        "Operational - sesongavhengig",
    ),

    // Permission
    MIGRERING_BARNETILSYN("familie.ef.sak.migrering.barnetilsyn", "Permission"),
    G_BEREGNING_TILLAT_MANUELL_OPPRETTELSE_AV_G_TASK("familie.ef.sak.tillat-opprettelse-av-g-task", "Permission"),
    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST(
        "familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost",
        "Permission",
    ),
    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),
    FRONTEND_SATSENDRING("familie.ef.sak.frontend-vis-satsendring", "Permission"),
    TILLAT_MIGRERING_5_ÅR_TILBAKE("familie.ef.sak.tillat-migrering-5-ar-tilbake", "Permission"),
    TILLAT_MIGRERING_7_ÅR_TILBAKE("familie.ef.sak.tillat-migrering-7-ar-tilbake", "Permission"),
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle", "Permission"),
    HENLEGG_BEHANDLING_UTEN_OPPGAVE("familie.ef.sak.henlegg-behandling-uten-oppgave", "Permission"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
