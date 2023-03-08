package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class SigrunService(val sigrunClient: SigrunClient, val fagsakPersonService: FagsakPersonService) {

    val forrigeÅr = YearMonth.now().year - 1
    val listInntektsår = listOf(forrigeÅr, forrigeÅr - 1, forrigeÅr - 2)

    fun hentInntektSisteTreÅr(fagsakPersonId: UUID): List<PensjonsgivendeInntektVisning> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)

        val beregnetSkattegrunnlagSisteTreÅr = listInntektsår.map { sigrunClient.hentBeregnetSkatt(aktivIdent, it).mapTilPensjonsgivendeInntektVisning(it) }
        /* Venter med kall mot beregnet skattegrunnlag - trenger blant annet eget filter
        val inntektFraSvalbard = hentInntektFraSvalbardSisteTreÅr(aktivIdent)
        beregnetSkattegrunnlagSisteTreÅr.forEach {
            it.verdi += inntektFraSvalbard.find { svalbard -> svalbard.inntektsaar == it.inntektsaar }?.verdi ?: 0
        }
         */

        return beregnetSkattegrunnlagSisteTreÅr
    }

    private fun hentInntektFraSvalbardSisteTreÅr(aktivIdent: String): List<PensjonsgivendeInntektVisning> {
        val inntektFraSvalbardSisteTreÅr = listInntektsår.map {
            sigrunClient.hentSummertSkattegrunnlag(aktivIdent, it).mapSvalbardGrunnlagTilPensjonsgivendeInntektVisning(it)
        }

        return inntektFraSvalbardSisteTreÅr
    }
}

private fun List<BeregnetSkatt>.mapTilPensjonsgivendeInntektVisning(inntektsaar: Int): PensjonsgivendeInntektVisning {
    val sum = this.filter { it.tekniskNavn != "skatteoppgjoersdato" }.sumOf { it.verdi.toInt() }
    return PensjonsgivendeInntektVisning(inntektsaar, sum)
}

private fun SummertSkattegrunnlag.mapSvalbardGrunnlagTilPensjonsgivendeInntektVisning(inntektsaar: Int): PensjonsgivendeInntektVisning {
    val sum = this.svalbardGrunnlag.filter { it.tekniskNavn != "skatteoppgjoersdato" }.sumOf { it.beloep }
    return PensjonsgivendeInntektVisning(inntektsaar, sum)
}

data class PensjonsgivendeInntektVisning(
    val inntektsaar: Int,
    var verdi: Int
)
