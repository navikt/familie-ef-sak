package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektForSkatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class SigrunService(
    val sigrunClient: SigrunClient,
    val fagsakPersonService: FagsakPersonService,
) {
    fun hentInntektForAlleÅrMedInntekt(fagsakPersonId: UUID): List<PensjonsgivendeInntektVisning> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)

        val tilOgMedÅr = if (YearMonth.now().month.value < 6) YearMonth.now().year - 2 else YearMonth.now().year - 1
        val inntektsår = tilOgMedÅr downTo 2017

        val pensjonsgivendeInntektList =
            inntektsår.map {
                sigrunClient.hentPensjonsgivendeInntekt(aktivIdent, it).mapTilPensjonsgivendeInntektVisning(it)
            }
        val førsteÅretMedInntektIndex = pensjonsgivendeInntektList.indexOfLast { it.totalInntektOverNull() } + 1

        return pensjonsgivendeInntektList.subList(0, førsteÅretMedInntektIndex)
    }

    // TODO: Fjern
    fun hentInntektForÅr(fagsakPersonId: UUID, inntektsår: Int): PensjonsgivendeInntektVisning {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return sigrunClient.hentPensjonsgivendeInntekt(aktivIdent, inntektsår).mapTilPensjonsgivendeInntektVisning(inntektsår)
    }
}

private fun PensjonsgivendeInntektResponse.mapTilPensjonsgivendeInntektVisning(inntektsår: Int): PensjonsgivendeInntektVisning {
    val fastlandInntekt = this.pensjonsgivendeInntekt?.firstOrNull { it.skatteordning == Skatteordning.FASTLAND }
    val svalbardInntekt = this.pensjonsgivendeInntekt?.firstOrNull { it.skatteordning == Skatteordning.SVALBARD }

    return PensjonsgivendeInntektVisning(
        this.inntektsaar ?: inntektsår,
        fastlandInntekt?.næringsinntekt() ?: 0,
        fastlandInntekt?.pensjonsgivendeInntektAvLoennsinntekt ?: 0,
        SvalbardPensjonsgivendeInntekt(
            svalbardInntekt?.næringsinntekt() ?: 0,
            svalbardInntekt?.pensjonsgivendeInntektAvLoennsinntekt ?: 0,
        ),
    )
}

fun PensjonsgivendeInntektForSkatteordning.næringsinntekt() = (this.pensjonsgivendeInntektAvNaeringsinntekt ?: 0) + (this.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage ?: 0)

fun PensjonsgivendeInntektVisning.totalInntektOverNull() = (this.næring + this.person + (this.svalbard?.næring ?: 0) + (this.svalbard?.person ?: 0)) > 0

data class PensjonsgivendeInntektVisning(
    val inntektsår: Int,
    var næring: Int,
    var person: Int,
    val svalbard: SvalbardPensjonsgivendeInntekt? = null,
) {
    fun totalNæringsinntekt() = næring + (svalbard?.næring ?: 0)
}

data class SvalbardPensjonsgivendeInntekt(
    val næring: Int,
    val person: Int,
)
