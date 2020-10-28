package no.nav.familie.ef.sak.økonomi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class TestOppdragType {
    Input,
    Output,
    Oppdrag
}

data class TestOppdrag(
        val type: TestOppdragType,
        val fnr: String,
        val ytelse: String,
        val linjeId: Long? = null,
        val forrigeLinjeId: Long? = null,
        val status110: String? = null,
        val erEndring: Boolean? = null,
        val opphørsdato: LocalDate?,
        val beløp: Int? = null,
        val startPeriode: LocalDate? = null,
        val sluttPeriode: LocalDate? = null
) {
    fun tilAndelTilkjentYtelse() : AndelTilkjentYtelse? {

        return if(beløp != null && startPeriode != null && sluttPeriode != null)
            AndelTilkjentYtelse(
                    beløp = this.beløp,
                    stønadFom = startPeriode,
                    stønadTom = sluttPeriode,
                    personIdent = fnr,
                    periodeId = linjeId,
                    forrigePeriodeId = forrigeLinjeId,
                    type = ytelse.tilYtelseType()
            )
        else if(TestOppdragType.Output==type && beløp==null && startPeriode==null && sluttPeriode==null)
            AndelTilkjentYtelse.nullAndel(
                    KjedeId(ytelse,fnr),
                    PeriodeId(linjeId!!, forrigeLinjeId)
            )
        else
            null;
    }

    fun tilUtbetalingsperiode() : Utbetalingsperiode? {

        return if(startPeriode != null && sluttPeriode != null && linjeId != null)
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = erEndring ?: false,
                    opphør = opphørsdato?.let { Opphør(it) },
                    periodeId = linjeId,
                    forrigePeriodeId = forrigeLinjeId,
                    datoForVedtak = LocalDate.now(),
                    klassifisering = ytelse,
                    vedtakdatoFom = startPeriode,
                    vedtakdatoTom = sluttPeriode,
                    sats = beløp?.toBigDecimal() ?: BigDecimal.ZERO,
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = fnr,
                    behandlingId = 1
            )
        else
            null;
    }
}

class TestOppdragGroup{
    private val andelerTilkjentYtelseInn: MutableList<AndelTilkjentYtelse> = mutableListOf()
    private val andelerTilkjentYtelseUt: MutableList<AndelTilkjentYtelse> = mutableListOf()
    private val utbetalingsperioder: MutableList<Utbetalingsperiode> = mutableListOf()
    private var oppdragKode110: Utbetalingsoppdrag.KodeEndring = Utbetalingsoppdrag.KodeEndring.NY
    private var personIdent: String?=null

    fun add(to: TestOppdrag) {
        when(to.type) {
            TestOppdragType.Input-> {
                personIdent=to.fnr;
                to.tilAndelTilkjentYtelse()?.also { andelerTilkjentYtelseInn.add(it) }
            }
            TestOppdragType.Oppdrag-> {
                oppdragKode110 =  Utbetalingsoppdrag.KodeEndring.valueOf(to.status110!!)
                to.tilUtbetalingsperiode()?.also { utbetalingsperioder.add(it) }
            }
            TestOppdragType.Output->{
                to.tilAndelTilkjentYtelse()?.also { andelerTilkjentYtelseUt.add(it) }
            }
        }
    }

    val input: TilkjentYtelse by lazy {
        TilkjentYtelse (
                behandlingId = 101,
                personident = personIdent!!,
                saksnummer = "saksnr",
                saksbehandler = "saksbehandler",
                andelerTilkjentYtelse = andelerTilkjentYtelseInn,
                vedtaksdato = LocalDate.now() // Ikke påkrevd, men exception ellers
        )
    }

    val output: TilkjentYtelse by lazy {
        val utbetalingsoppdrag = Utbetalingsoppdrag(
                kodeEndring = oppdragKode110,
                fagSystem = "EFOG",
                saksnummer = "saksnr",
                aktoer = personIdent!!,
                saksbehandlerId = "saksbehandler",
                avstemmingTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
                utbetalingsperiode = utbetalingsperioder.map { it.copy(behandlingId = 101) }
        )

        TilkjentYtelse (
                id=input.id,
                behandlingId = 101,
                personident = personIdent!!,
                saksnummer = "saksnr",
                saksbehandler = "saksbehandler",
                andelerTilkjentYtelse = andelerTilkjentYtelseUt,
                utbetalingsoppdrag = utbetalingsoppdrag,
                vedtaksdato = input.vedtaksdato,
                stønadFom = andelerTilkjentYtelseUt.filter { it.stønadFom!= NULL_DATO }.minOfOrNull { it.stønadFom },
                stønadTom = andelerTilkjentYtelseInn.filter { it.stønadTom!=NULL_DATO }.maxOfOrNull {  it.stønadTom }
        )

    }
}

object TestOppdragParser {

    const val KEY_TYPE = "Type"
    const val KEY_FNR = "Fnr"
    const val KEY_YTELSE = "Ytelse"
    const val KEY_LINJE_ID = "LID"
    const val KEY_FORRIGE_LINJE_ID = "Pre-LID"
    const val KEY_STATUS_OPPDRAG = "Status oppdrag"
    const val KEY_ER_ENDRING = "Er endring"

    val RESERVERED_KEYS =
            listOf(KEY_TYPE, KEY_FNR, KEY_YTELSE, KEY_LINJE_ID, KEY_FORRIGE_LINJE_ID, KEY_STATUS_OPPDRAG, KEY_ER_ENDRING)

    fun parse(url: URL) : List<TestOppdrag> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)

        return rows.map { row ->
            val datoKeysMedBeløp = row.keys
                    .filter { key -> !RESERVERED_KEYS.contains(key) }
                    .filter { datoKey -> (row.get(datoKey))?.trim('x')?.toIntOrNull()!=null }
                    .sorted()

            val opphørYearMonth = row.keys
                    .filter { key -> !RESERVERED_KEYS.contains(key) }
                    .sorted()
                    .firstOrNull { datoKey -> (row.get(datoKey))?.contains('x') ?: false }
                    ?.let { YearMonth.parse(it) }

            val firstYearMonth=datoKeysMedBeløp.firstOrNull()?.let { YearMonth.parse(it) }
            val lastYearMonth=datoKeysMedBeløp.lastOrNull()?.let { YearMonth.parse(it) }
            val beløp = datoKeysMedBeløp.firstOrNull()?.let { row.get(it)?.trim('x') }?.toIntOrNull()

            TestOppdrag(
                    type = row.get(KEY_TYPE)?.let { TestOppdragType.valueOf(it) } !!,
                    fnr = row.get(KEY_FNR)!!,
                    ytelse = row.get(KEY_YTELSE)!!,
                    linjeId = row.get(KEY_LINJE_ID)?.let{ emptyAsNull(it) }?.let { Integer.parseInt(it).toLong() },
                    forrigeLinjeId = row.get(KEY_FORRIGE_LINJE_ID)?.let{ emptyAsNull(it) }?.let { Integer.parseInt(it).toLong() },
                    status110 = row.get(KEY_STATUS_OPPDRAG)?.let{ emptyAsNull(it) },
                    erEndring = row.get(KEY_ER_ENDRING)?.let{ it.toBoolean() },
                    beløp = beløp,
                    opphørsdato = opphørYearMonth?.atDay(1),
                    startPeriode = firstYearMonth?.atDay(1),
                    sluttPeriode = lastYearMonth?.atEndOfMonth()
            )
        }

    }

    fun parseToTestOppdragGroup(url: URL) : List<TestOppdragGroup> {

        val result: MutableList<TestOppdragGroup> = mutableListOf()

        var newGroup = true;

        parse(url).forEach{ to ->
            when(to.type) {
                TestOppdragType.Input-> {
                    if(newGroup) {
                        result.add(TestOppdragGroup())
                        newGroup=false;
                    }
                }
                else -> {
                    newGroup=true;
                }
            }
            result.last().add(to)
        }

        return result
    }

    private inline fun emptyAsNull(s: String) : String? =
        if(s.length==0) null else s


}

object TestOppdragRunner {
    fun run(url: URL) {
        val grupper = TestOppdragParser.parseToTestOppdragGroup(url)

        var forrigeTilkjentYtelse: TilkjentYtelse? =null

        grupper.forEachIndexed { indeks,gruppe->
            val faktisk = lagTilkjentYtelseMedUtbetalingsoppdrag(gruppe.input, forrigeTilkjentYtelse)
            Assertions.assertEquals(gruppe.output, faktisk, "Feiler for gruppe med indeks " + indeks)
            forrigeTilkjentYtelse = faktisk
        }
    }

    fun lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelse:TilkjentYtelse,forrigeTilkjentYtelse:TilkjentYtelse? = null) =
            UtbetalingsoppdragGenerator
                    .lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelse,forrigeTilkjentYtelse)

}
