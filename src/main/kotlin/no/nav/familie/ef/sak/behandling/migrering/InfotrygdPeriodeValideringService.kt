package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.SummertInfotrygdPeriodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class InfotrygdPeriodeValideringService(
        private val infotrygdService: InfotrygdService
) {

    fun validerKanJournalføres(personIdent: String, stønadType: StønadType) {
        if (infotrygdService.eksisterer(personIdent, setOf(StønadType.OVERGANGSSTØNAD))) {
            try {
                hentPeriodeForMigrering(personIdent)
                // hvis hentPeriodeForMigrering returnerer noe, så betyder det att den kan migreres
                throw ApiFeil("Denne må migreres", HttpStatus.BAD_REQUEST)
            } catch (e: MigreringException) {
                if(e.type.kanGåVidereTilJournalføring) {
                    return
                } else {
                    throw ApiFeil("Denne må migreres", HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    fun hentPeriodeForMigrering(personIdent: String, kjøremåned: YearMonth = YearMonth.now()): SummertInfotrygdPeriodeDto {
        validerSakerIInfotrygd(personIdent)
        val perioder = infotrygdService.hentDtoPerioder(personIdent).overgangsstønad
        validerHarKunEnIdentPåPerioder(perioder, personIdent)
        return periodeFremEllerBakITiden(perioder, kjøremåned)
    }

    private fun periodeFremEllerBakITiden(perioder: InfotrygdStønadPerioderDto,
                                          kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        val gjeldendePerioder = perioder.summert
        val perioderFremITiden = gjeldendePerioder.filter { it.stønadTom >= førsteDagenINesteMåned(kjøremåned) }
        if (perioderFremITiden.isNotEmpty()) {
            return gjeldendePeriodeFremITiden(perioderFremITiden, kjøremåned)
        }

        if (gjeldendePerioder.isEmpty()) {
            throw MigreringException("Har ikke noen perioder å migrere",
                                     MigreringExceptionType.MANGLER_PERIODER)
        }
        val perioderMedBeløp = gjeldendePerioder.filter { it.beløp != 0 }
        if (perioderMedBeløp.isEmpty()) {
            throw MigreringException("Har ikke noen perioder med beløp å migrere",
                                     MigreringExceptionType.MANGLER_PERIODER_MED_BELØP)
        }
        val sistePeriode = perioderMedBeløp.maxByOrNull { it.stønadFom } ?: error("Finner ikke noen perioder")

        return sisteMånedenPåPeriodeBakITiden(sistePeriode)
    }

    /**
     * Perioder frem i tiden migreres fra neste måned
     */
    private fun gjeldendePeriodeFremITiden(gjeldendePerioder: List<SummertInfotrygdPeriodeDto>,
                                           kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        if (gjeldendePerioder.size > 1) {
            throw MigreringException("Har fler enn 1 (${gjeldendePerioder.size}) aktiv periode",
                                     MigreringExceptionType.FLERE_AKTIVE_PERIODER)
        }
        val periode = gjeldendePerioder.single()
        validerFomDato(periode)
        validerTomDato(periode)
        return periode.copy(stønadFom = maxOf(kjøremåned.atDay(1), periode.stønadFom))
    }

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
            throw MigreringException("Startdato er annet enn første i måneden, dato=$stønadFom",
                                     MigreringExceptionType.FEIL_FOM_DATO)
        }
        if (periode.beløp == 0) {
            throw MigreringException("Beløp er 0 på siste perioden, har ikke støtte for det ennå. fom=$stønadFom",
                                     MigreringExceptionType.BELØP_0)
        }
        return periode.copy(stønadFom = YearMonth.of(stønadTom.year, stønadTom.month).atDay(1))
    }

    private fun validerFomDato(periode: SummertInfotrygdPeriodeDto) {
        if (periode.stønadFom.dayOfMonth != 1) {
            throw MigreringException("Startdato er annet enn første i måneden, dato=${periode.stønadFom}",
                                     MigreringExceptionType.FEIL_FOM_DATO)
        }
    }

    private fun validerTomDato(periode: SummertInfotrygdPeriodeDto) {
        val dato = periode.stønadTom
        if (YearMonth.of(dato.year, dato.month).atEndOfMonth() != dato) {
            throw MigreringException("Sluttdato er annet enn siste i måneden, dato=$dato",
                                     MigreringExceptionType.FEIL_TOM_DATO)
        }
        if (dato.isBefore(LocalDate.now().minusYears(3))) {
            throw MigreringException("Kan ikke migrere når forrige utbetaling i infotrygd er mer enn 3 år tilbake i tid, dato=$dato",
                                     MigreringExceptionType.ELDRE_PERIODER)
        }
    }

    private fun validerSakerIInfotrygd(personIdent: String): List<InfotrygdSak> {
        val sakerForOvergangsstønad =
                infotrygdService.hentSaker(personIdent).saker.filter { it.stønadType == StønadType.OVERGANGSSTØNAD }
        validerFinnesIkkeÅpenSak(sakerForOvergangsstønad)
        validerSakerInneholderKunEnIdent(sakerForOvergangsstønad, personIdent)
        return sakerForOvergangsstønad
    }

    private fun validerHarKunEnIdentPåPerioder(perioder: InfotrygdStønadPerioderDto,
                                               personIdent: String) {
        perioder.perioder.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Det finnes perioder som inneholder annet fnr=${it.personIdent}. " +
                                     "Disse vedtaken må endres til aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER_VEDTAK)
        }
    }

    private fun validerSakerInneholderKunEnIdent(sakerForOvergangsstønad: List<InfotrygdSak>,
                                                 personIdent: String) {
        sakerForOvergangsstønad.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Finnes sak med annen personIdent for personen. ${lagSakFeilinfo(it)} " +
                                     "personIdent=${it.personIdent}. " +
                                     "Disse sakene må oppdateres med aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER)
        }
    }

    private fun validerFinnesIkkeÅpenSak(sakerForOvergangsstønad: List<InfotrygdSak>) {
        sakerForOvergangsstønad.find { it.resultat == InfotrygdSakResultat.ÅPEN_SAK }?.let {
            throw MigreringException("Har åpen sak. ${lagSakFeilinfo(it)}",
                                     MigreringExceptionType.ÅPEN_SAK)
        }
    }

    private fun førsteDagenINesteMåned(yearMonth: YearMonth) = yearMonth.plusMonths(1).atDay(1)

    private fun lagSakFeilinfo(sak: InfotrygdSak): String {
        return "saksblokk=${sak.saksblokk} saksnr=${sak.saksnr} " +
               "registrertDato=${sak.registrertDato} mottattDato=${sak.mottattDato}"
    }
}