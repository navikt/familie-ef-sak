package no.nav.familie.ef.sak.blankett

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import java.util.UUID

object BlankettHelper {

    fun lagArkiverBlankettRequestMotInfotrygd(personIdent: String,
                                              pdf: ByteArray,
                                              enhet: String,
                                              fagsakId: String?,
                                              behandlingId: UUID): ArkiverDokumentRequest =
            lagArkiverDokumentRequest(personIdent = personIdent,
                                      pdf = pdf,
                                      fagsakId = fagsakId,
                                      behandlingId = behandlingId,
                                      enhet = enhet,
                                      dokumenttype = Dokumenttype.OVERGANGSSTØNAD_BLANKETT)

    fun lagArkiverBlankettRequestMotNyLøsning(personIdent: String,
                                              pdf: ByteArray,
                                              enhet: String,
                                              fagsakId: Long,
                                              behandlingId: UUID): ArkiverDokumentRequest =
            lagArkiverDokumentRequest(personIdent = personIdent,
                                      pdf = pdf,
                                      fagsakId = fagsakId.toString(),
                                      behandlingId = behandlingId,
                                      enhet = enhet,
                                      dokumenttype = Dokumenttype.OVERGANGSSTØNAD_BLANKETT_SAKSBEHANDLING)

    private fun lagArkiverDokumentRequest(personIdent: String,
                                          pdf: ByteArray,
                                          fagsakId: String?,
                                          behandlingId: UUID,
                                          enhet: String,
                                          dokumenttype: Dokumenttype): ArkiverDokumentRequest {
        val dokument = Dokument(pdf, Filtype.PDFA, null, "Blankett for overgangsstønad", dokumenttype)
        return ArkiverDokumentRequest(
                fnr = personIdent,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                vedleggsdokumenter = listOf(),
                fagsakId = fagsakId,
                journalførendeEnhet = enhet,
                eksternReferanseId = "$behandlingId-blankett"
        )
    }

}