package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType.IKKE_VALGT
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.time.LocalDate
import java.util.UUID

@Deprecated("Bruk v2")
data class JournalføringRequest(
    val dokumentTitler: Map<String, String>? = null,
    val fagsakId: UUID,
    val oppgaveId: String,
    val behandling: JournalføringBehandling,
    val journalførendeEnhet: String,
    val barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList(),
    val vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
)

data class JournalføringRequestV2(
    val dokumentTitler: Map<String, String>? = null,
    val logiskeVedlegg: Map<String, List<String>>? = null, // TODO: Må tas i bruk!
    val fagsakId: UUID,
    val oppgaveId: String,
    val journalførendeEnhet: String,
    val årsak: Journalføringsårsak,
    val aksjon: Journalføringsaksjon,
    val mottattDato: LocalDate? = null, // Brukes av klage
    val barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList(),
    val vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
) {
    fun gjelderKlage(): Boolean {
        return årsak == Journalføringsårsak.KLAGE || årsak == Journalføringsårsak.KLAGE_TILBAKEKREVING
    }

    fun tilUstrukturertDokumentasjonType(): UstrukturertDokumentasjonType {
        return when (årsak) {
            Journalføringsårsak.ETTERSENDING -> UstrukturertDokumentasjonType.ETTERSENDING
            Journalføringsårsak.PAPIRSØKNAD -> UstrukturertDokumentasjonType.PAPIRSØKNAD
            Journalføringsårsak.DIGITAL_SØKNAD, Journalføringsårsak.KLAGE_TILBAKEKREVING, Journalføringsårsak.KLAGE -> UstrukturertDokumentasjonType.IKKE_VALGT
        }
    }

    fun skalJournalføreUtenNyBehandling(): Boolean = aksjon == Journalføringsaksjon.JOURNALFØR_PÅ_FAGSAK
}

enum class Journalføringsaksjon {
    OPPRETT_BEHANDLING,
    JOURNALFØR_PÅ_FAGSAK,
}

enum class Journalføringsårsak(val behandlingsårsak: BehandlingÅrsak) {
    KLAGE_TILBAKEKREVING(BehandlingÅrsak.KLAGE),
    KLAGE(BehandlingÅrsak.KLAGE),
    PAPIRSØKNAD(BehandlingÅrsak.PAPIRSØKNAD),
    DIGITAL_SØKNAD(BehandlingÅrsak.SØKNAD),
    ETTERSENDING(BehandlingÅrsak.NYE_OPPLYSNINGER),
}

data class BarnSomSkalFødes(val fødselTerminDato: LocalDate) {

    fun tilBehandlingBarn(behandlingId: UUID): BehandlingBarn = BehandlingBarn(
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
enum class UstrukturertDokumentasjonType(val behandlingÅrsak: () -> BehandlingÅrsak) {
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

fun JournalføringRequest.valider() {
    val ustrukturertDokumentasjonType = behandling.ustrukturertDokumentasjonType
    if (skalJournalførePåEksisterendeBehandling()) {
        feilHvis(barnSomSkalFødes.isNotEmpty()) {
            "Kan ikke sende inn barn når man journalfører på en eksisterende behandling"
        }
        feilHvis(vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
            "Kan ikke vilkårsbehandle nye barn på en eksisterende behandling"
        }
    } else {
        feilHvis(
            ustrukturertDokumentasjonType == UstrukturertDokumentasjonType.ETTERSENDING &&
                behandling.behandlingstype != BehandlingType.REVURDERING,
        ) {
            "Må journalføre ettersending på ny behandling som revurdering"
        }
        feilHvis(
            ustrukturertDokumentasjonType == UstrukturertDokumentasjonType.ETTERSENDING &&
                vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VALGT,
        ) {
            "Man må velge om man skal vilkårsbehandle nye barn på ny behandling av type ettersending"
        }
    }

    feilHvis(
        ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.ETTERSENDING &&
            vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT,
    ) {
        "Kan ikke sende inn vilkårsbehandleNyeBarn=$vilkårsbehandleNyeBarn når dokumentasjonstype=$ustrukturertDokumentasjonType"
    }

    feilHvis(
        behandling.ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.PAPIRSØKNAD &&
            barnSomSkalFødes.isNotEmpty(),
    ) {
        "Årsak må være satt til papirsøknad hvis man sender inn barn som skal fødes"
    }
}

fun JournalføringRequestV2.valider() {
    if (skalJournalføreUtenNyBehandling()) {
        feilHvis(barnSomSkalFødes.isNotEmpty()) {
            "Kan ikke sende inn barn når man journalfører på en eksisterende behandling"
        }
        feilHvis(vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
            "Kan ikke vilkårsbehandle nye barn på en eksisterende behandling"
        }
    } else {
        feilHvis(
            årsak == Journalføringsårsak.ETTERSENDING &&
                vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VALGT,
        ) {
            "Man må velge om man skal vilkårsbehandle nye barn på ny behandling av type ettersending"
        }
    }

    feilHvis(
        årsak != Journalføringsårsak.ETTERSENDING &&
            vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT,
    ) {
        "Kan ikke sende inn vilkårsbehandleNyeBarn=$vilkårsbehandleNyeBarn når årsak=$årsak"
    }

    feilHvis(
        årsak != Journalføringsårsak.PAPIRSØKNAD &&
            barnSomSkalFødes.isNotEmpty(),
    ) {
        "Årsak må være satt til papirsøknad hvis man sender inn barn som skal fødes"
    }
}

fun JournalføringRequest.skalJournalførePåEksisterendeBehandling(): Boolean = this.behandling.behandlingsId != null

data class JournalføringBehandling(
    val behandlingsId: UUID? = null,
    val behandlingstype: BehandlingType? = null,
    val ustrukturertDokumentasjonType: UstrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
)
