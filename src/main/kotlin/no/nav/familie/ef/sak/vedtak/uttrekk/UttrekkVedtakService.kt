package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class UttrekkVedtakService(
        private val uttrekkVedtakRepository: UttrekkVedtakRepository
) {

    fun hentArbeidssøkere(årMåned: YearMonth = YearMonth.now()): List<UttrekkArbeidssøker> {
        val startdato = årMåned.atDay(1)
        val sluttdato = årMåned.atEndOfMonth()
        val arbeidssøkere = uttrekkVedtakRepository.hentArbeidssøkere(startdato, sluttdato)
        return arbeidssøkere.filter { harPeriodeSomArbeidssøker(it, startdato, sluttdato) }
    }

    private fun harPeriodeSomArbeidssøker(it: UttrekkArbeidssøker,
                                          startdato: LocalDate,
                                          sluttdato: LocalDate) =
            it.perioder.perioder.any {
                it.datoFra <= startdato
                && it.datoTil >= sluttdato
                && erArbeidssøker(it)
            }

    private fun erArbeidssøker(it: Vedtaksperiode) =
            (it.aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER || it.aktivitet == AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)

}