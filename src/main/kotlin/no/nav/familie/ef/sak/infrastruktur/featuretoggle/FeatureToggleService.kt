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

    OPPRETT_OPPGAVER_TERMINBARN("familie.ef.iverksett.opprett-oppgaver-terminbarn"),
    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST("familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost"),
    AUTOMATISK_BEREGN_NÆRE_BOFORHOLD("familie.ef.sak.beregne-naere-boforhold"),

    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),

    ERSTATTE_SANKSJON("familie.ef.sak.erstatte-sanksjon"),

    FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER("familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler"),
    FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR("familie.ef.sak.frontend-automatisk-utfylle-vilkar"),
    FRONTEND_PREFYLL_VEDTAKSPERIODER("familie.ef.sak.frontend-prefyll-vedtaksperioder"),
    FRONTEND_VIS_VEDTAK_LEGG_TIL_RAD_KNAPP("familie.ef.sak.frontend-vis-vedtak-legg-til-rad-knapp"),
    FRONTEND_OPPRETT_KLAGE("familie.ef.sak.frontend-vis-opprett-klage"),
    FRONTEND_VERGE_FRITTSTÅENDE_BREV("familie.ef.sak.frontend-verge-frittstaende-brev")
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
