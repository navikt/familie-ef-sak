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
    MIGRERING_BARNETILSYN("familie.ef.sak.migrering.barnetilsyn"),
    G_BEREGNING("familie.ef.sak.g-beregning"),
    G_BEREGNING_SCHEDULER("familie.ef.sak.g-beregning-scheduler"),
    G_OMREGNING_REVURDER_HOPP_OVER_VALIDER_TIDLIGERE_VEDTAK("familie.ef.sak.revurder-g-omregning-hopp-over-valider-tidligere-vedtak"),
    G_BEREGNING_INKLUDER_SATT_PÅ_VENT("familie.ef.sak.inkluder-satt-pa-vent-gomregning"),
    G_BEREGNING_TILLAT_MANUELL_OPPRETTELSE_AV_G_TASK("familie.ef.sak.tillat-opprettelse-av-g-task", "Permission"),
    SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS("familie.ef.sak.bruk-nye-maxsatser"),

    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST(
        "familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost",
        "Permission",
    ),
    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    VILKÅR_GJENBRUK("familie.ef.sak.vilkaar-gjenruk"),

    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR("familie.ef.sak.frontend-automatisk-utfylle-vilkar"),
    FRONTEND_SATSENDRING("familie.ef.sak.frontend-vis-satsendring"),
    FRONTEND_VIS_INNTEKT_PERSONOVERSIKT("familie.ef.sak.frontend.vis-inntekt-personoversikt"),
    KAST_FEIL_HVIS_OPPGAVE_MANGLER_PÅ_ÅPEN_BEHANDLING("familie.ef.sak.kast-feil-hvis-oppgave-mangler-pa-apen-behandling"),
    TILLAT_MIGRERING_5_ÅR_TILBAKE("familie.ef.sak.tillat-migrering-5-ar-tilbake", "Permission"),
    TILLAT_MIGRERING_7_ÅR_TILBAKE("familie.ef.sak.tillat-migrering-7-ar-tilbake", "Permission"),
    AUTOMATISKE_BREV_INNHENTING_KARAKTERUTSKRIFT("familie.ef.sak.automatiske-brev-innhenting-karakterutskrift"),
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
