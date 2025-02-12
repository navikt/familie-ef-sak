package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class NæringsinntektNotatService(
    private val brevClient: BrevClient,
    private val pdlClient: PdlClient,
) {
    fun lagNotat(næringsinntektDataForBeregning: NæringsinntektDataForBeregning): ByteArray {
        val søker = pdlClient.hentSøker(næringsinntektDataForBeregning.personIdent)
        val næringsinntektIngenEndringPdfRequest =
            NæringsinntektIngenEndringPdfRequest(
                næringsinntektDataForBeregning.fagsak.eksternId.toString(),
                næringsinntektDataForBeregning.personIdent,
                søker.navn.gjeldende().visningsnavn(),
                "Vedtaksløsningen",
                "4489",
                næringsinntektDataForBeregning.forventetInntektIFjor,
                næringsinntektDataForBeregning.fjoråretsNæringsinntekt,
                næringsinntektDataForBeregning.fjoråretsPersonInntekt,
                YearMonth.now().year - 1,
            )

        return brevClient.genererNæringsinntektUtenEndringNotat(næringsinntektIngenEndringPdfRequest)
    }
}
