package no.nav.familie.ef.sak.infrastruktur.featuretoggle

sealed class FeatureToggle(
    val toggleId: String,
    val featureToggleBeskrivelse: FeatureToggleBeskrivelse,
) {
    data object FrontendKonverterDelmalblokkTilHtmlFelt : FeatureToggle(
        toggleId = "familie.ef.sak.konverter-delmalblokk-til-html-input",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.RELEASE
    )

    data object FrontendVisMarkereGodkjenneOppgaveModal : FeatureToggle(
        toggleId = "familie.ef.sak.vis-markere-godkjenne-vedtak-oppgave-modal",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.RELEASE
    )

    data object KontrollerNæringsinntekt : FeatureToggle(
        toggleId = "familie.ef.sak.kontroller-naeringsinntekt",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.RELEASE
    )

    data object GBeregning : FeatureToggle(
        toggleId = "familie.ef.sak.g-beregning",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object GBeregningScheduler : FeatureToggle(
        toggleId = "familie.ef.sak.g-beregning-scheduler",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object SatsendringBrukIkkeVedtattMaxsats : FeatureToggle(
        toggleId = "familie.ef.sak.bruk-nye-maxsatser",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object FrontendVisIkkePubliserteBrevmaler : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object FrontendAutomatiskUtfylleVilkar : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-automatisk-utfylle-vilkar",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object AutomatiskeBrevInnhentingAktivitetsplikt : FeatureToggle(
        toggleId = "familie.ef.sak.automatiske-brev-innhenting-aktivitetsplikt",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object FrontendVisTildelOppgaveBehandling : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-tildel-oppgave-knapp",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.OPERATIONAL
    )

    data object MigreringBarnetilsyn : FeatureToggle(
        toggleId = "familie.ef.sak.migrering.barnetilsyn",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object OpprettBehandlingFerdigstiltJournalpost : FeatureToggle(
        toggleId = "familie.ef.sak.opprett-behandling-for-ferdigstilt-journalpost",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object BehandlingKorrigering : FeatureToggle(
        toggleId = "familie.ef.sak.behandling-korrigering",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object FrontendSatsendring : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-vis-satsendring",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object TillatMigrering7ÅrTilbake : FeatureToggle(
        toggleId = "familie.ef.sak.tillat-migrering-7-ar-tilbake",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object UtviklerMedVeilederrolle : FeatureToggle(
        toggleId = "familie.ef.sak.utviklere-med-veilederrolle",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object HenleggBehandlingUtenOppgave : FeatureToggle(
        toggleId = "familie.ef.sak.henlegg-behandling-uten-oppgave",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object VelgÅrsakVedKlageOpprettelse : FeatureToggle(
        toggleId = "familie.ef.sak.klagebehandling-arsak",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object VisSamværskalkulator : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-vis-samverskalkulator",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )

    data object VisAutomatiskInntektsendring : FeatureToggle(
        toggleId = "familie.ef.sak.frontend-vis-automatisk-inntektsendring",
        featureToggleBeskrivelse = FeatureToggleBeskrivelse.PERMISSION
    )
}

enum class FeatureToggleBeskrivelse(val beskrivelse: String) {
    RELEASE("Release"), PERMISSION("Permission"), OPERATIONAL("Operational")
}

internal fun FeatureToggle.mapUnleashContextFields(): Map<String, String> {
    return when (this) {
        else -> emptyMap()
    }
}