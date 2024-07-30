package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType.IKKE_VALGT
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import java.time.LocalDate
import java.util.UUID

data class JournalføringRequestV2(
    val dokumentTitler: Map<String, String>? = null,
    val logiskeVedlegg: Map<String, List<LogiskVedlegg>>? = null,
    val fagsakId: UUID,
    val oppgaveId: String,
    val journalførendeEnhet: String,
    val årsak: Journalføringsårsak,
    val aksjon: Journalføringsaksjon,
    val mottattDato: LocalDate? = null, // Brukes av klage
    val barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList(),
    val vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
    val nyAvsender: NyAvsender? = null,
) {
    fun gjelderKlage(): Boolean = årsak == Journalføringsårsak.KLAGE || årsak == Journalføringsårsak.KLAGE_TILBAKEKREVING

    fun tilUstrukturertDokumentasjonType(): UstrukturertDokumentasjonType =
        when (årsak) {
            Journalføringsårsak.ETTERSENDING -> UstrukturertDokumentasjonType.ETTERSENDING
            Journalføringsårsak.PAPIRSØKNAD -> UstrukturertDokumentasjonType.PAPIRSØKNAD
            Journalføringsårsak.DIGITAL_SØKNAD, Journalføringsårsak.KLAGE_TILBAKEKREVING, Journalføringsårsak.KLAGE -> IKKE_VALGT
        }

    fun skalJournalføreTilNyBehandling(): Boolean = aksjon == Journalføringsaksjon.OPPRETT_BEHANDLING
}

data class NyAvsender(
    val erBruker: Boolean,
    val navn: String?,
    val personIdent: String?,
)

enum class Journalføringsaksjon {
    OPPRETT_BEHANDLING,
    JOURNALFØR_PÅ_FAGSAK,
}

enum class Journalføringsårsak(
    val behandlingsårsak: BehandlingÅrsak,
) {
    KLAGE_TILBAKEKREVING(BehandlingÅrsak.KLAGE),
    KLAGE(BehandlingÅrsak.KLAGE),
    PAPIRSØKNAD(BehandlingÅrsak.PAPIRSØKNAD),
    DIGITAL_SØKNAD(BehandlingÅrsak.SØKNAD),
    ETTERSENDING(BehandlingÅrsak.NYE_OPPLYSNINGER),
}

data class BarnSomSkalFødes(
    val fødselTerminDato: LocalDate,
) {
    fun tilBehandlingBarn(behandlingId: UUID): BehandlingBarn =
        BehandlingBarn(
            behandlingId = behandlingId,
            søknadBarnId = null,
            personIdent = null,
            navn = null,
            fødselTermindato = this.fødselTerminDato,
        )
}

/**
 * [IKKE_VALGT] er indirekte det samme som digital søknad, der man ikke velger ustrukturert dokumentasjonstype
 */
enum class UstrukturertDokumentasjonType(
    val behandlingÅrsak: () -> BehandlingÅrsak,
) {
    PAPIRSØKNAD({ BehandlingÅrsak.PAPIRSØKNAD }),
    ETTERSENDING({ BehandlingÅrsak.NYE_OPPLYSNINGER }),
    IKKE_VALGT({ error("Kan ikke bruke behandlingsårsak fra $IKKE_VALGT") }), ;

    fun erEttersending(): Boolean = this == ETTERSENDING
}

enum class VilkårsbehandleNyeBarn {
    VILKÅRSBEHANDLE,
    IKKE_VILKÅRSBEHANDLE,
    IKKE_VALGT,
}

data class JournalføringTilNyBehandlingRequest(
    val fagsakId: UUID,
    val behandlingstype: BehandlingType,
)

fun JournalføringRequestV2.valider() {
    if (skalJournalføreTilNyBehandling()) {
        feilHvis(
            årsak == Journalføringsårsak.ETTERSENDING &&
                vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VALGT,
        ) {
            "Man må velge om man skal vilkårsbehandle nye barn på ny behandling av type ettersending"
        }
    } else {
        feilHvis(barnSomSkalFødes.isNotEmpty()) {
            "Kan ikke legge inn barn når man journalfører til en eksisterende behandling"
        }
        feilHvis(vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
            "Kan ikke vilkårsbehandle nye barn på en eksisterende behandling"
        }
    }

    feilHvis(
        årsak != Journalføringsårsak.ETTERSENDING &&
            vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT,
    ) {
        "Kan ikke sende inn vilkårsbehandleNyeBarn=$vilkårsbehandleNyeBarn når årsak=$årsak"
    }

    dokumentTitler?.let {
        brukerfeilHvis(
            it.containsValue(""),
        ) {
            "Mangler tittel på et eller flere dokumenter"
        }
    }
    feilHvis(
        årsak != Journalføringsårsak.PAPIRSØKNAD &&
            barnSomSkalFødes.isNotEmpty(),
    ) {
        "Årsak må være satt til papirsøknad hvis man sender inn barn som skal fødes"
    }
}
