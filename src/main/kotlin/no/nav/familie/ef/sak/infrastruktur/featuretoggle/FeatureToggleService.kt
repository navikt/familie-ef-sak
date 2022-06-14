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

    VALIDERE_BESLUTTERPDF_ER_NULL("familie.ef.sak.skal-validere-beslutterpdf-er-null"),
    SYNKRONISER_PERSONIDENTER("familie.ef.sak.synkroniser-personidenter"),
    BARNETILSYN("familie.ef.sak.barnetilsyn"),
    SKOLEPENGER("familie.ef.sak.skolepenger"),
    OPPRETT_OPPGAVER_TERMINBARN("familie.ef.iverksett.opprett-oppgaver-terminbarn"),
    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST("familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost"),
    KAN_LEGGE_TIL_NYE_BARN_PÅ_REVURDERING("familie.ef.sak.kan-legge-til-nye-barn-paa-revurdering"),

    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    FRONTEND_BEHANDLE_BARNETILSYN("familie.ef.sak.frontend-behandle-barnetilsyn-i-ny-losning"),
    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_VIS_OPPDATERING_REGISTEROPPLYSNINGER("familie.ef.sak.frontend-vis-oppdatering-av-registeropplysninger"),
    FRONTEND_VIS_SANKSJON_EN_MÅNED("familie.ef.sak.frontend-vis-sanksjon-en-maned"),
    FRONTEND_VISE_OPPRETT_NY_BEHANDLING_BARNETILSYN("familie.ef.sak.frontend-skal-vise-opprett-ny-behandling-knapp-barnetilsyn"),
    FRONTEND_JOURNALFØRING_KAN_LEGGE_TIL_TERMINBARN("familie.ef.sak.frontend-journalforing-kan-legge-til-terminbarn"),
    FRONTEND_SKOLEPENGER_REVURDERING("familie.ef.sak.frontend-skal-vise-opprett-ny-behandling-knapp-skolepenger"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
