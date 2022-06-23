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
    G_BEREGNING("familie.ef.sak.g-beregning"),
    G_OMREGNING_REVURDER_HOPP_OVER_VALIDER_TIDLIGERE_VEDTAK("familie.ef.sak.revurder-g-omregning-hopp-over-valider-tidligere-vedtak"),
    OMBERENING_LIVE_RUN("familie.ef.sak.omberegning.live.run"),
    OMBEREGNING("familie.ef.sak.omberegning"),

    SYNKRONISER_PERSONIDENTER("familie.ef.sak.synkroniser-personidenter"),
    SKOLEPENGER("familie.ef.sak.skolepenger"),
    SKOLEPENGER_OPPHØR("familie.ef.sak.skolepenger-opphor"),
    OPPRETT_OPPGAVER_TERMINBARN("familie.ef.iverksett.opprett-oppgaver-terminbarn"),
    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST("familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost"),

    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_JOURNALFØRING_KAN_LEGGE_TIL_TERMINBARN("familie.ef.sak.frontend-journalforing-kan-legge-til-terminbarn"),
    FRONTEND_SKOLEPENGER_REVURDERING("familie.ef.sak.frontend-skal-vise-opprett-ny-behandling-knapp-skolepenger"),
    FRONTEND_FILTRERE_BARN_BARNETILSYN("familie.ef.sak.frontend.filtrere-barn-barnetilsyn")
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
