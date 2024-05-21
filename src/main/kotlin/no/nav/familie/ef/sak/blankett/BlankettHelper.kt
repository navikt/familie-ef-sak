package no.nav.familie.ef.sak.blankett

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.util.UUID

object BlankettHelper {
    fun lagArkiverBlankettRequestMotNyLøsning(
        personIdent: String,
        pdf: ByteArray,
        enhet: String,
        fagsakId: Long,
        behandlingId: UUID,
        stønadstype: StønadType,
    ): ArkiverDokumentRequest {
        val dokumenttype = utledSakbehandlingsblankettDokumenttype(stønadstype)
        val dokument = Dokument(pdf, Filtype.PDFA, null, "Blankett for ${stønadstype.name.lowercase()}", dokumenttype)
        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = enhet,
            eksternReferanseId = "$behandlingId-blankett",
        )
    }

    private fun utledSakbehandlingsblankettDokumenttype(stønadstype: StønadType): Dokumenttype {
        return when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> Dokumenttype.OVERGANGSSTØNAD_BLANKETT_SAKSBEHANDLING
            StønadType.BARNETILSYN -> Dokumenttype.BARNETILSYN_BLANKETT_SAKSBEHANDLING
            StønadType.SKOLEPENGER -> Dokumenttype.SKOLEPENGER_BLANKETT_SAKSBEHANDLING
        }
    }
}
