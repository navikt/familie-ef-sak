package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.SummertInfotrygdPeriodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype.TILMELDT_SOM_REELL_ARBEIDSSØKER
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class InfotrygdPeriodeValideringService(
    private val infotrygdService: InfotrygdService,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun validerKanOppretteBehandlingGittInfotrygdData(fagsak: Fagsak) {
        if (!behandlingService.finnesBehandlingForFagsak(fagsak.id)) {
            when (fagsak.stønadstype) {
                StønadType.OVERGANGSSTØNAD ->
                    validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(
                        fagsak.hentAktivIdent(),
                        fagsak.stønadstype,
                    )
                StønadType.BARNETILSYN, StønadType.SKOLEPENGER ->
                    validerHarIkkeÅpenSakIInfotrygd(fagsak)
            }
        }
    }

    fun validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(
        personIdent: String,
        stønadType: StønadType,
    ) {
        feilHvis(stønadType != StønadType.OVERGANGSSTØNAD) {
            "Har ikke støtte for å sjekke migrering av stønadstypen $stønadType"
        }
        if (trengerMigrering(personIdent)) {
            throw ApiFeil(
                "Det eksisterer perioder i infotrygd for denne personen. " +
                    "Vennligst søk opp personen og migrer før du journalfører denne journalposten",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    fun validerHarIkkeÅpenSakIInfotrygd(fagsak: Fagsak) {
        validerSakerIInfotrygd(fagsak.hentAktivIdent(), fagsak.stønadstype)
    }

    private fun trengerMigrering(personIdent: String): Boolean =
        gjeldendeInfotrygdOvergangsstønadPerioder(personIdent)
            .any { it.harBeløp() && it.harNyerePerioder() }

    private fun gjeldendeInfotrygdOvergangsstønadPerioder(personIdent: String) =
        infotrygdService
            .hentDtoPerioder(personIdent)
            .overgangsstønad.summert

    fun hentPeriodeForMigrering(
        personIdent: String,
        stønadType: StønadType,
        kjøremåned: YearMonth = YearMonth.now(),
    ): SummertInfotrygdPeriodeDto {
        validerSakerIInfotrygd(personIdent, stønadType)
        val dtoPerioder = infotrygdService.hentDtoPerioder(personIdent)
        val perioder =
            when (stønadType) {
                StønadType.OVERGANGSSTØNAD -> dtoPerioder.overgangsstønad
                StønadType.BARNETILSYN -> dtoPerioder.barnetilsyn
                StønadType.SKOLEPENGER -> error("Har ikke støtte for å migrere skolepenger")
            }
        validerHarKunEnIdentPåPerioder(perioder, personIdent)
        return periodeFremEllerBakITiden(perioder, kjøremåned)
    }

    private fun periodeFremEllerBakITiden(
        perioder: InfotrygdStønadPerioderDto,
        kjøremåned: YearMonth,
    ): SummertInfotrygdPeriodeDto {
        val gjeldendePerioder = perioder.summert
        val perioderFremITiden = gjeldendePerioder.filter { it.stønadsperiode.tomDato >= kjøremåned.atDay(1) }
        if (perioderFremITiden.isNotEmpty()) {
            return gjeldendePeriodeFremITiden(perioderFremITiden, kjøremåned)
        }

        if (gjeldendePerioder.isEmpty()) {
            throw MigreringException(
                "Har ikke noen perioder å migrere",
                MigreringExceptionType.MANGLER_PERIODER,
            )
        }
        val perioderMedBeløp = gjeldendePerioder.filter { it.månedsbeløp != 0 }
        if (perioderMedBeløp.isEmpty()) {
            throw MigreringException(
                "Har ikke noen perioder med beløp å migrere",
                MigreringExceptionType.MANGLER_PERIODER_MED_BELØP,
            )
        }
        val sistePeriode = perioderMedBeløp.maxByOrNull { it.stønadsperiode.fom } ?: error("Finner ikke noen perioder")

        return sisteMånedenPåPeriodeBakITiden(sistePeriode)
    }

    /**
     * Perioder frem i tiden migreres fra neste måned
     */
    private fun gjeldendePeriodeFremITiden(
        perioderFremITiden: List<SummertInfotrygdPeriodeDto>,
        kjøremåned: YearMonth,
    ): SummertInfotrygdPeriodeDto {
        val gjeldendePerioder = slåSammenFremtidligePerioderHvisLike(perioderFremITiden)
        if (gjeldendePerioder.size > 1) {
            throw MigreringException(
                "Har fler enn 1 (${gjeldendePerioder.size}) aktiv periode",
                MigreringExceptionType.FLERE_AKTIVE_PERIODER,
            )
        }
        val periode = gjeldendePerioder.single()
        validerFomDato(periode)
        validerTomDato(periode)
        if (periode.månedsbeløp < 1) {
            throw MigreringException(
                "Kan ikke migrere perioder frem i tiden med månedsbløp=${periode.månedsbeløp}",
                MigreringExceptionType.MANGLER_PERIODER_MED_BELØP_FREM_I_TIDEN,
            )
        }
        return periode.copy(
            stønadsperiode =
                periode.stønadsperiode.copy(
                    fom =
                        maxOf(
                            kjøremåned,
                            periode.stønadsperiode.fom,
                        ),
                ),
        )
    }

    private fun slåSammenFremtidligePerioderHvisLike(perioderFremITiden: List<SummertInfotrygdPeriodeDto>): List<SummertInfotrygdPeriodeDto> =
        perioderFremITiden
            .sortedBy { it.stønadsperiode }
            .fold<SummertInfotrygdPeriodeDto, MutableList<SummertInfotrygdPeriodeDto>>(mutableListOf()) { acc, periode ->
                val last = acc.removeLastOrNull()
                if (last == null) {
                    acc.add(periode)
                } else if (perioderErSammenhengendeMedSammeAktivitetOgMånedsbeløp(last, periode)) {
                    acc.add(last.copy(stønadsperiode = last.stønadsperiode union periode.stønadsperiode))
                } else {
                    acc.add(last)
                    acc.add(periode)
                }
                acc
            }.toList()

    /**
     * Då vi mapper aktivitet for perioder som er arbeidssøker er det viktig at de er like, og trenger ikke å sjekke periodetype
     */
    private fun perioderErSammenhengendeMedSammeAktivitetOgMånedsbeløp(
        last: SummertInfotrygdPeriodeDto,
        periode: SummertInfotrygdPeriodeDto,
    ) = last.stønadsperiode påfølgesAv periode.stønadsperiode &&
        sammeAktivitetEllerIkkeArbeidssøker(last, periode) &&
        last.månedsbeløp == periode.månedsbeløp

    private fun sammeAktivitetEllerIkkeArbeidssøker(
        last: SummertInfotrygdPeriodeDto,
        periode: SummertInfotrygdPeriodeDto,
    ) = last.aktivitet == periode.aktivitet ||
        (last.aktivitet != TILMELDT_SOM_REELL_ARBEIDSSØKER && periode.aktivitet != TILMELDT_SOM_REELL_ARBEIDSSØKER)

    /**
     * Henter siste måneden for en periode bak i tiden
     * Hvis det kun er 1 måned, så valideres det att fom-dato ikke er annet enn 1 i måneden
     */
    private fun sisteMånedenPåPeriodeBakITiden(periode: SummertInfotrygdPeriodeDto): SummertInfotrygdPeriodeDto {
        val stønadTom = periode.stønadTom
        val stønadFom = periode.stønadFom
        val tomMåned = YearMonth.of(stønadTom.year, stønadTom.month)
        val nyFomDato = tomMåned.atDay(1)
        validerTomDato(periode)
        if (stønadFom > nyFomDato) {
            throw MigreringException(
                "Startdato er annet enn første i måneden, dato=$stønadFom",
                MigreringExceptionType.FEIL_FOM_DATO,
            )
        }
        if (periode.månedsbeløp == 0) {
            throw MigreringException(
                "Beløp er 0 på siste perioden, har ikke støtte for det ennå. fom=$stønadFom",
                MigreringExceptionType.BELØP_0,
            )
        }
        return periode.copy(stønadsperiode = periode.stønadsperiode.copy(fom = tomMåned))
    }

    private fun validerFomDato(periode: SummertInfotrygdPeriodeDto) {
        if (periode.stønadFom.dayOfMonth != 1) {
            throw MigreringException(
                "Startdato er annet enn første i måneden, dato=${periode.stønadFom}",
                MigreringExceptionType.FEIL_FOM_DATO,
            )
        }
    }

    private fun validerTomDato(periode: SummertInfotrygdPeriodeDto) {
        val dato = periode.stønadTom
        if (YearMonth.of(dato.year, dato.month).atEndOfMonth() != dato) {
            throw MigreringException(
                "Sluttdato er annet enn siste i måneden, dato=$dato",
                MigreringExceptionType.FEIL_TOM_DATO,
            )
        }

        val datogrenseForMigrering = hentDatogrenseForMigrering()

        if (dato.isBefore(datogrenseForMigrering)) {
            throw MigreringException(
                "Kan ikke migrere når forrige utbetaling i infotrygd er før $datogrenseForMigrering, dato=$dato",
                MigreringExceptionType.ELDRE_PERIODER,
            )
        }
    }

    private fun validerSakerIInfotrygd(
        personIdent: String,
        stønadType: StønadType,
    ): List<InfotrygdSak> {
        val sakerForStønad =
            infotrygdService.hentSaker(personIdent).saker.filter { it.stønadType == stønadType }
        validerFinnesIkkeÅpenSak(sakerForStønad)
        validerSakerInneholderKunEnIdent(sakerForStønad, personIdent)
        return sakerForStønad
    }

    private fun validerHarKunEnIdentPåPerioder(
        perioder: InfotrygdStønadPerioderDto,
        personIdent: String,
    ) {
        perioder.perioder.find { it.personIdent != personIdent }?.let {
            logger.warn("Det finnes perioder med ulike fødselsnummer i infotrygd")
            secureLogger.warn("Det finnes perioder med ulike fødselsnummer i infotrygd - fnrInfotrygd=${it.personIdent} fnrGjeldende=$personIdent ")
        }
    }

    private fun validerSakerInneholderKunEnIdent(
        sakerForOvergangsstønad: List<InfotrygdSak>,
        personIdent: String,
    ) {
        sakerForOvergangsstønad.find { it.personIdent != personIdent }?.let {
            logger.warn("Det finnes perioder med ulike fødselsnummer i infotrygd")
            secureLogger.warn("Det finnes perioder med ulike fødselsnummer i infotrygd - fnrInfotrygd=${it.personIdent} fnrGjeldende=$personIdent ")
        }
    }

    private fun validerFinnesIkkeÅpenSak(sakerForOvergangsstønad: List<InfotrygdSak>) {
        sakerForOvergangsstønad
            .filter {
                it.type != InfotrygdSakType.KLAGE &&
                    it.type != InfotrygdSakType.KLAGE_TILBAKEBETALING &&
                    it.type != InfotrygdSakType.ANKE
            }.find { it.resultat == InfotrygdSakResultat.ÅPEN_SAK }
            ?.let {
                throw MigreringException(
                    "Har åpen sak. ${lagSakFeilinfo(it)}",
                    MigreringExceptionType.ÅPEN_SAK,
                )
            }
    }

    private fun hentDatogrenseForMigrering(): LocalDate {
        if (featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE)) {
            return LocalDate.of(2009, 1, 1)
        }
        return LocalDate.of(2019, 1, 1)
    }

    private fun lagSakFeilinfo(sak: InfotrygdSak): String =
        "saksblokk=${sak.saksblokk} saksnr=${sak.saksnr} " +
            "registrertDato=${sak.registrertDato} mottattDato=${sak.mottattDato}"
}

private fun SummertInfotrygdPeriodeDto.harNyerePerioder(): Boolean {
    val antallÅrUtenGjeldendeInfotrygperioder: Long = 5
    return this.stønadsperiode.tomDato > LocalDate.now().minusYears(antallÅrUtenGjeldendeInfotrygperioder)
}

private fun SummertInfotrygdPeriodeDto.harBeløp(): Boolean = this.månedsbeløp > 0
