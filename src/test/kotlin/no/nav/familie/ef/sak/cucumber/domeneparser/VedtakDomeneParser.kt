package no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.hentUtgiftUUID
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.DataTableUtil.forHverBehandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.førsteDagenIMånedenEllerDefault
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Periodetype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

object VedtakDomeneParser {

    fun mapVedtakOvergangsstønad(dataTable: DataTable): List<Vedtak> {
        return mapVedtak(dataTable) { vedtak, rader ->
            if (vedtak.resultatType != ResultatType.OPPHØRT) {
                val perioder = mapPerioderForOvergangsstønad(vedtak.resultatType, rader)
                vedtak.copy(
                    perioder = PeriodeWrapper(perioder),
                    inntekter = InntektWrapper(lagDefaultInntektsperiode(perioder))
                )
            } else {
                vedtak
            }
        }
    }

    fun mapAktivitetForBarnetilsyn(dataTable: DataTable): Map<UUID, SvarId?> {
        return dataTable.forHverBehandling { behandlingId, rader ->
            val rad = rader.first()
            val arbeidAktivitet = parseArbeidAktivitet(rad)
            behandlingId to arbeidAktivitet
        }.toMap()
    }

    fun mapVedtakForBarnetilsyn(dataTable: DataTable): List<Vedtak> {
        return mapVedtak(dataTable) { vedtak, rader ->
            val perioder = when (vedtak.resultatType) {
                ResultatType.INNVILGE -> mapPerioderForBarnetilsyn(vedtak.resultatType, rader)
                ResultatType.SANKSJONERE -> {
                    val perioderForBarnetilsyn = mapPerioderForBarnetilsyn(vedtak.resultatType, rader)
                    validerSanksjon(perioderForBarnetilsyn)
                    perioderForBarnetilsyn
                }
                else -> emptyList()
            }
            vedtak.copy(
                barnetilsyn = BarnetilsynWrapper(perioder, null),
                kontantstøtte = KontantstøtteWrapper(emptyList()), // overskreves i egen "Gitt"
                tilleggsstønad = TilleggsstønadWrapper(false, emptyList(), null)
            ) // overskreves i egen "Gitt"
        }
    }

    fun opphørSkolepengerUtenPerioder(behandlingId: UUID) = Vedtak(
        resultatType = ResultatType.OPPHØRT,
        skolepenger = SkolepengerWrapper(emptyList(), null),
        behandlingId = behandlingId
    )

    fun mapVedtakForSkolepenger(dataTable: DataTable): List<Vedtak> {
        val gyldigeKolonner = listOf(
            Domenebegrep.BEHANDLING_ID,
            VedtakDomenebegrep.RESULTAT_TYPE,
            Domenebegrep.FRA_OG_MED_DATO,
            Domenebegrep.TIL_OG_MED_DATO,
            VedtakDomenebegrep.ID_UTGIFT,
            VedtakDomenebegrep.STUDIETYPE,
            VedtakDomenebegrep.STUDIEBELASTNING,
            VedtakDomenebegrep.DATO_FAKTURA,
            VedtakDomenebegrep.UTGIFTER,
            VedtakDomenebegrep.BELØP
        )
        return mapVedtak(dataTable, gyldigeKolonner) { vedtak, rader ->
            val perioder = when (vedtak.resultatType) {
                ResultatType.OPPHØRT,
                ResultatType.INNVILGE -> mapPerioderForSkolepenger(rader)
                ResultatType.SANKSJONERE -> {
                    val perioderForBarnetilsyn = mapPerioderForSkolepenger(rader)
                    validerSanksjonSkolepenger(perioderForBarnetilsyn)
                    perioderForBarnetilsyn
                }
                else -> emptyList()
            }
            vedtak.copy(skolepenger = SkolepengerWrapper(perioder, null))
        }
    }

    private fun mapVedtak(
        dataTable: DataTable,
        gyldigeKolonner: List<Domenenøkkel> = emptyList(),
        vedtakDecorator: (vedtak: Vedtak, rader: List<Map<String, String>>) -> Vedtak = { vedtak, _ -> vedtak }
    ): List<Vedtak> {
        validerKolonner(dataTable, gyldigeKolonner)
        return dataTable.forHverBehandling { _, rader ->
            val rad = rader.first()
            val resultatType = parseResultatType(rad) ?: ResultatType.INNVILGE
            vedtakDecorator.invoke(mapVedtakDomene(rad, resultatType), rader)
        }
    }

    private fun validerKolonner(
        dataTable: DataTable,
        gyldigeKolonner: List<Domenenøkkel>
    ) {
        if (gyldigeKolonner.isNotEmpty()) {
            val kolonneVerdier = gyldigeKolonner.map { it.nøkkel() }.toSet()
            dataTable.row(0)
                .filter { !kolonneVerdier.contains(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { error("Ugyldige kolonner: $it") }
        }
    }

    private fun mapVedtakDomene(
        rad: Map<String, String>,
        resultatType: ResultatType
    ) =
        Vedtak(
            behandlingId = behandlingIdTilUUID[parseInt(Domenebegrep.BEHANDLING_ID, rad)]!!,
            resultatType = resultatType,
            opphørFom = parseValgfriÅrMåned(VedtakDomenebegrep.OPPHØRSDATO, rad),
            internBegrunnelse = if (resultatType == ResultatType.SANKSJONERE) "Ok" else null
        )

    private fun validerSanksjon(perioder: List<Barnetilsynperiode>) {
        feilHvisIkke(perioder.size == 1) {
            "Antall rader for sanksjonering må være 1, per behandlingId"
        }
        val periode = perioder.single()
        feilHvis(YearMonth.from(periode.datoFra) != YearMonth.from(periode.datoTil)) {
            "Sanksjon strekker seg ikke 1 måned: ${periode.datoFra} - ${periode.datoTil}"
        }
    }

    private fun validerSanksjonSkolepenger(perioder: List<SkoleårsperiodeSkolepenger>) {
        feilHvisIkke(perioder.size == 1) {
            "Antall rader for sanksjonering må være 1, per behandlingId"
        }
        /*val periode = perioder.single()
        feilHvis(YearMonth.from(periode.datoFra) != YearMonth.from(periode.datoTil)) {
            "Sanksjon strekker seg ikke 1 måned: ${periode.datoFra} - ${periode.datoTil}"
        }*/
    }

    private fun mapPerioderForOvergangsstønad(
        resultatType: ResultatType,
        rader: List<Map<String, String>>
    ): List<Vedtaksperiode> {
        return rader.map { rad ->
            val sanksjonsårsak = sanksjonsårsak(rad, resultatType)
            Vedtaksperiode(
                datoFra = parseFraOgMed(rad),
                datoTil = parseTilOgMed(rad),
                aktivitet = parseAktivitetType(rad) ?: AktivitetType.BARN_UNDER_ETT_ÅR,
                periodeType = parseVedtaksperiodeType(rad) ?: VedtaksperiodeType.HOVEDPERIODE,
                sanksjonsårsak = sanksjonsårsak
            )
        }
    }

    /**
     * Bruker sanksjonsårsak hvis den er definiert
     * Setter default årsak hvis ikke den er definiert og [resultatType] er [ResultatType.SANKSJONERE]
     */
    private fun sanksjonsårsak(
        rad: Map<String, String>,
        resultatType: ResultatType
    ): Sanksjonsårsak? {
        return parseSanksjonsårsak(rad)
            ?: if (resultatType == ResultatType.SANKSJONERE) Sanksjonsårsak.NEKTET_TILBUDT_ARBEID else null
    }

    private fun mapPerioderForBarnetilsyn(
        resultatType: ResultatType,
        rader: List<Map<String, String>>
    ): List<Barnetilsynperiode> {
        return rader.map { rad ->
            val behandlingId = behandlingIdTilUUID[parseInt(Domenebegrep.BEHANDLING_ID, rad)]!!
            val sanksjonsårsak = sanksjonsårsak(rad, resultatType)
            val barn = mapBarn(behandlingId, rad) ?: parseValgfriInt(VedtakDomenebegrep.ANTALL_BARN, rad)?.let {
                IntRange(1, it).map { UUID.randomUUID() }
            } ?: emptyList()
            val midlertidigOpphør = parseValgfriBoolean(VedtakDomenebegrep.ER_MIDLERTIDIG_OPPHØR, rad)
            Barnetilsynperiode(
                datoFra = parseFraOgMed(rad),
                datoTil = parseTilOgMed(rad),
                utgifter = parseValgfriInt(VedtakDomenebegrep.UTGIFTER, rad) ?: 0,
                barn = barn,
                erMidlertidigOpphør = midlertidigOpphør
                    ?: (sanksjonsårsak != null),
                sanksjonsårsak = sanksjonsårsak,
                periodetype = if (midlertidigOpphør == true) Periodetype.OPPHØR else Periodetype.ORDINÆR
            )
        }
    }

    fun mapBarn(behandlingId: UUID, rad: Map<String, String>): List<UUID>? {
        return parseValgfriString(VedtakDomenebegrep.BARN, rad)?.let { barnListeString ->
            barnListeString.split(",")
                .map { it.trim() }
                .map { IdTIlUUIDHolder.hentEllerOpprettBarn(behandlingId, it) }
        }
    }

    private fun mapPerioderForSkolepenger(rader: List<Map<String, String>>): List<SkoleårsperiodeSkolepenger> {
        val skoleårsperioder = mutableMapOf<Skoleår, SkoleårsperiodeSkolepenger>()
        rader.forEach { rad ->
            val datoFra = parseFraOgMed(rad)
            val skoleår = Skoleår(Månedsperiode(datoFra, datoFra))
            val delårsperiode = mapDelårsperiodeSkolepenger(rad, datoFra)
            val utgift = mapSkolepengerUtgift(rad)

            val skoleårsperiode: SkoleårsperiodeSkolepenger = skoleårsperioder
                .getOrDefault(skoleår, SkoleårsperiodeSkolepenger(emptyList(), emptyList()))
            skoleårsperioder[skoleår] = skoleårsperiode.copy(
                perioder = (skoleårsperiode.perioder.toSet() + delårsperiode).toList(),
                utgiftsperioder = skoleårsperiode.utgiftsperioder + utgift
            )
        }
        return skoleårsperioder.values.toList()
    }

    private fun mapDelårsperiodeSkolepenger(
        rad: Map<String, String>,
        datoFra: LocalDate
    ): DelårsperiodeSkoleårSkolepenger {
        return DelårsperiodeSkoleårSkolepenger(
            studietype = parseEnum(VedtakDomenebegrep.STUDIETYPE, rad),
            periode = Månedsperiode(datoFra, parseTilOgMed(rad)),
            studiebelastning = parseValgfriInt(VedtakDomenebegrep.STUDIEBELASTNING, rad) ?: 100
        )
    }

    private fun mapSkolepengerUtgift(rad: Map<String, String>): SkolepengerUtgift {
        return SkolepengerUtgift(
            id = hentUtgiftUUID(parseValgfriInt(VedtakDomenebegrep.ID_UTGIFT, rad) ?: 1),
            utgiftsdato = parseValgfriÅrMånedEllerDato(
                VedtakDomenebegrep.DATO_FAKTURA,
                rad
            ).førsteDagenIMånedenEllerDefault(),
            utgifter = parseValgfriInt(VedtakDomenebegrep.UTGIFTER, rad) ?: 0,
            stønad = parseValgfriInt(VedtakDomenebegrep.BELØP, rad) ?: 0
        )
    }

    fun mapOgSettPeriodeMedBeløp(
        vedtak: List<Vedtak>,
        dataTable: DataTable,
        oppdaterVedtak: (Vedtak, List<PeriodeMedBeløp>) -> Vedtak
    ): List<Vedtak> {
        val beløpsperioder = dataTable.forHverBehandling { behandlingId, rader ->
            behandlingId to rader.map { rad ->
                PeriodeMedBeløp(
                    Månedsperiode(parseFraOgMed(rad), parseTilOgMed(rad)),
                    beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad) ?: 0
                )
            }
        }.toMap()
        return vedtak.map {
            val perioder = beløpsperioder[it.behandlingId] ?: emptyList()
            oppdaterVedtak(it, perioder)
        }
    }

    private fun lagDefaultInntektsperiode(perioder: List<Vedtaksperiode>) =
        perioder.firstOrNull()?.let {
            listOf(
                Inntektsperiode(
                    periode = Månedsperiode(it.datoFra, LocalDate.MAX),
                    inntekt = BigDecimal.ZERO,
                    samordningsfradrag = BigDecimal.ZERO
                )
            )
        } ?: emptyList()

    fun mapInntekter(dataTable: DataTable): Map<UUID, InntektWrapper> {
        return dataTable.forHverBehandling { behandlingId, rader ->
            val inntektsperioder = rader.fold(mutableListOf<Inntektsperiode>()) { acc, rad ->
                val datoFra = parseFraOgMed(rad)
                val nyPeriode = acc.lastOrNull()?.periode?.copy(tom = YearMonth.from(datoFra.minusDays(1)))
                if (nyPeriode != null) {
                    acc.removeLastOrNull()?.copy(periode = nyPeriode)?.let { acc.add(it) }
                }
                acc.add(
                    Inntektsperiode(
                        periode = Månedsperiode(datoFra, LocalDate.MAX),
                        inntekt = BigDecimal(parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad) ?: 0),
                        samordningsfradrag = BigDecimal(parseValgfriInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad) ?: 0
                        )
                    )
                )
                acc
            }
            behandlingId to InntektWrapper(inntektsperioder)
        }.toMap()
    }

    fun mapBehandlingForHistorikkEndring(dataTable: DataTable): List<ForventetHistorikk> {
        return dataTable.asMaps().map {
            BehandlingForHistorikkEndringMapper().mapRad(it)
        }
    }

    class ForventetHistorikk(
        val behandlingId: UUID,
        val historikkEndring: HistorikkEndring?,
        val stønadFra: LocalDate,
        val stønadTil: LocalDate,
        val inntekt: Int?,
        val beløp: Int?,
        val periodeType: VedtaksperiodeType?,
        val aktivitetType: AktivitetType?,
        val kontantstøtte: Int?,
        val tilleggsstønad: Int?,
        val antallBarn: Int?,
        val utgifter: Int?,
        val arbeidAktivitet: SvarId?,
        val erSanksjon: Boolean?,
        val sanksjonsårsak: Sanksjonsårsak?,
        val vedtaksdato: LocalDate?,
        val erOpphør: Boolean
    )

    class BehandlingForHistorikkEndringMapper {

        fun mapRad(rad: Map<String, String>): ForventetHistorikk {
            val aktivitetType = parseAktivitetType(rad)
            return ForventetHistorikk(
                behandlingId = behandlingIdTilUUID[parseInt(Domenebegrep.BEHANDLING_ID, rad)]!!,
                historikkEndring = parseEndringType(rad)?.let { endringType ->
                    val vedtakstidspunkt =
                        parseValgfriDato(VedtakDomenebegrep.ENDRET_I_VEDTAKSDATO, rad)?.atStartOfDay()
                            ?: LocalDateTime.MIN
                    HistorikkEndring(
                        type = endringType,
                        behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad)]!!,
                        vedtakstidspunkt = vedtakstidspunkt
                    )
                },
                stønadFra = parseFraOgMed(rad),
                stønadTil = parseTilOgMed(rad),
                inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad),
                beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad),
                periodeType = parseVedtaksperiodeType(rad),
                aktivitetType = aktivitetType,
                kontantstøtte = parseValgfriInt(VedtakDomenebegrep.KONTANTSTØTTE, rad),
                tilleggsstønad = parseValgfriInt(VedtakDomenebegrep.TILLEGGSSTØNAD, rad),
                antallBarn = parseValgfriInt(VedtakDomenebegrep.ANTALL_BARN, rad),
                utgifter = parseValgfriInt(VedtakDomenebegrep.UTGIFTER, rad),
                arbeidAktivitet = parseArbeidAktivitet(rad),
                erSanksjon = parseValgfriBoolean(VedtakDomenebegrep.ER_SANKSJON, rad),
                sanksjonsårsak = parseSanksjonsårsak(rad),
                vedtaksdato = parseValgfriDato(VedtakDomenebegrep.VEDTAKSDATO, rad),
                erOpphør = parseValgfriBoolean(VedtakDomenebegrep.ER_OPPHØR, rad) ?: false
            )
        }
    }
}

enum class VedtakDomenebegrep(val nøkkel: String) : Domenenøkkel {
    RESULTAT_TYPE("Vedtaksresultat"),
    TILKJENT_YTELSE_ID("Tilkjent ytelse Id"),
    PERSONIDENT("Personnummer"),
    INNTEKT("Inntekt"),
    INNTEKTSREDUKSJON("Inntektsreduksjon"),
    SAMORDNINGSFRADRAG("Samordningsfradrag"),
    BELØP("Beløp"),
    BELØP_MELLOM("Beløp mellom"),
    AKTIVITET_TYPE("Aktivitet"),
    ARBEID_AKTIVITET("Arbeid aktivitet"), // Inngangsvilkår i barnetilsyn
    VEDTAKSPERIODE_TYPE("Vedtaksperiode"),
    ENDRET_I_BEHANDLING_ID("Endret i behandlingId"),
    KILDE_BEHANDLING_ID("Kildebehandling"),
    ENDRING_TYPE("Endringstype"),
    OPPHØRSDATO("Opphørsdato"),
    UTGIFTER("Utgifter"),
    BARN("Barn"),
    ANTALL_BARN("Antall barn"),
    TILLEGGSSTØNAD("Tilleggsstønad"),
    KONTANTSTØTTE("Kontantstøtte"),
    ER_SANKSJON("Er sanksjon"),
    SANKSJONSÅRSAK("Sanksjonsårsak"),
    ID_UTGIFT("Id utgift"),
    STUDIETYPE("Studietype"),
    DATO_FAKTURA("Dato faktura"),
    STUDIEBELASTNING("Studiebelastning"),
    ER_MIDLERTIDIG_OPPHØR("Er midlertidig opphør"),
    VEDTAKSDATO("Vedtaksdato"),
    ENDRET_I_VEDTAKSDATO("Endret i vedtaksdato"),
    ER_OPPHØR("Er opphør")
    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
