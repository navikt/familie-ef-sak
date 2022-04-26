package no.nav.familie.ef.sak.blankett

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.util.UUID

object BlankettHelper {

    @Deprecated("Brukes kun i blankettbehandling - skal fases ut etterhvert")
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
                                      dokumenttype = Dokumenttype.OVERGANGSSTØNAD_BLANKETT,
                                      stønadstype = StønadType.OVERGANGSSTØNAD)

    fun lagArkiverBlankettRequestMotNyLøsning(personIdent: String,
                                              pdf: ByteArray,
                                              enhet: String,
                                              fagsakId: Long,
                                              behandlingId: UUID,
                                              stønadstype: StønadType): ArkiverDokumentRequest =
            lagArkiverDokumentRequest(personIdent = personIdent,
                                      pdf = pdf,
                                      fagsakId = fagsakId.toString(),
                                      behandlingId = behandlingId,
                                      enhet = enhet,
                                      dokumenttype = utledSakbehandlingsblankettDokumenttype(stønadstype),
                                      stønadstype = stønadstype
            )

    private fun utledSakbehandlingsblankettDokumenttype(stønadstype: StønadType): Dokumenttype {
        return when(stønadstype) {
            StønadType.OVERGANGSSTØNAD -> Dokumenttype.OVERGANGSSTØNAD_BLANKETT_SAKSBEHANDLING
            StønadType.BARNETILSYN -> Dokumenttype.BARNETILSYN_BLANKETT_SAKSBEHANDLING
            StønadType.SKOLEPENGER -> Dokumenttype.SKOLEPENGER_BLANKETT_SAKSBEHANDLING
        }

    }

    private fun lagArkiverDokumentRequest(personIdent: String,
                                          pdf: ByteArray,
                                          fagsakId: String?,
                                          behandlingId: UUID,
                                          enhet: String,
                                          stønadstype: StønadType,
                                          dokumenttype: Dokumenttype): ArkiverDokumentRequest {
        val dokument = Dokument(pdf, Filtype.PDFA, null, "Blankett for ${stønadstype.name.lowercase()}", dokumenttype)
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