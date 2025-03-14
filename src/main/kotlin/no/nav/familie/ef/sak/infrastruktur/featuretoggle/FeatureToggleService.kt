package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    val unleashNextSerivce: UnleashNextService,
) {
    fun isEnabled(toggle: Toggle): Boolean = unleashNextSerivce.isEnabled(toggle)
}

enum class Toggle(
    val toggleId: String,
    val beskrivelse: String? = null,
) {
    // Release
    FRONTEND_KONVERTER_DELMALBLOKK_TIL_HTML_FELT("familie.ef.sak.konverter-delmalblokk-til-html-input", "Release"),
    FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL(
        "familie.ef.sak.vis-markere-godkjenne-vedtak-oppgave-modal",
        "Release",
    ),
    KONTROLLER_NÆRINGSINNTEKT("familie.ef.sak.kontroller-naeringsinntekt"),

    // Operational
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
    AUTOMATISKE_BREV_INNHENTING_AKTIVITETSPLIKT(
        "familie.ef.sak.automatiske-brev-innhenting-aktivitetsplikt",
        "Operational - sesongavhengig",
    ),
    FRONTED_VIS_TILDEL_OPPGAVE_BEHANDLING(
        "familie.ef.sak.frontend-tildel-oppgave-knapp",
        "Operational - kun preprod",
    ),

    // Permission
    MIGRERING_BARNETILSYN("familie.ef.sak.migrering.barnetilsyn", "Permission"),
    OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST(
        "familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost",
        "Permission",
    ),
    BEHANDLING_KORRIGERING("familie.ef.sak.behandling-korrigering", "Permission"),
    FRONTEND_SATSENDRING("familie.ef.sak.frontend-vis-satsendring", "Permission"),
    TILLAT_MIGRERING_7_ÅR_TILBAKE("familie.ef.sak.tillat-migrering-7-ar-tilbake", "Permission"),
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle", "Permission"),
    HENLEGG_BEHANDLING_UTEN_OPPGAVE("familie.ef.sak.henlegg-behandling-uten-oppgave", "Permission"),
    VELG_ÅRSAK_VED_KLAGE_OPPRETTELSE("familie.ef.sak.klagebehandling-arsak", "Permission"),
    VIS_SAMVÆRSKALKULATOR("familie.ef.sak.frontend-vis-samverskalkulator", "Permission"),
    VIS_AUTOMATISK_INNTEKTSENDRING("familie.ef.sak.frontend-vis-automatisk-inntektsendring", "Permission"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle = toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
    }
}

internal fun Toggle.mapUnleashContextFields(): Map<String, String> =
    when (this) {
        Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL -> mapOf(
            UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler()
        )

        else -> emptyMap()
    }
