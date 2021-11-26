package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class UttrekkArbeidssøkerService(
        private val tilgangService: TilgangService,
        private val uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository,
) {

    fun forrigeMåned(): () -> YearMonth = { YearMonth.now().minusMonths(1) }

    @Transactional
    fun opprettUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()) {
        hentArbeidssøkere(årMåned).forEach {
            uttrekkArbeidssøkerRepository.insert(UttrekkArbeidssøkere(fagsakId = it.fagsakId,
                                                                      vedtakId = it.behandlingIdForVedtak,
                                                                      årMåned = årMåned))
        }
    }

    fun settKontrollert(id: UUID, kontrollert: Boolean) {
        val uttrekkArbeidssøkere = uttrekkArbeidssøkerRepository.findByIdOrThrow(id)
        tilgangService.validerTilgangTilFagsak(uttrekkArbeidssøkere.fagsakId)
        uttrekkArbeidssøkerRepository.update(uttrekkArbeidssøkere.medKontrollert(kontrollert = kontrollert))
    }

    fun hentUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke(),
                                 side: Int = 1,
                                 visKontrollerte: Boolean = true): UttrekkArbeidssøkereDto {
        val antallKontrollert = uttrekkArbeidssøkerRepository.countByÅrMånedAndKontrollertIsTrue(årMåned)
        val arbeidssøkere = hentPaginerteArbeidssøkere(årMåned, side, visKontrollerte)
        return UttrekkArbeidssøkereDto(årMåned = årMåned,
                                       antallTotalt = arbeidssøkere.totalElements.toInt(),
                                       antallKontrollert = antallKontrollert,
                                       arbeidssøkere = arbeidssøkere.toList().map(UttrekkArbeidssøkere::tilDto))
    }

    private fun hentPaginerteArbeidssøkere(årMåned: YearMonth,
                                           side: Int,
                                           visKontrollerte: Boolean): Page<UttrekkArbeidssøkere> {
        feilHvis(side < 1) {
            "Side må være større enn 0, men var side=$side"
        }
        val pageable = PageRequest.of(side - 1, 20, Sort.by("id"))
        return if (visKontrollerte) {
            uttrekkArbeidssøkerRepository.findAllByÅrMåned(årMåned, pageable)
        } else {
            uttrekkArbeidssøkerRepository.findAllByÅrMånedAndKontrollert(årMåned, !visKontrollerte, pageable)
        }
    }

    fun hentArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()): List<VedtaksperioderForUttrekk> {
        val startdato = årMåned.atDay(1)
        val sluttdato = årMåned.atEndOfMonth()
        val arbeidssøkere = uttrekkArbeidssøkerRepository.hentVedtaksperioderForSisteFerdigstilteBehandlinger(startdato, sluttdato)
        return arbeidssøkere.filter { harPeriodeSomArbeidssøker(it, startdato, sluttdato) }
    }

    private fun harPeriodeSomArbeidssøker(it: VedtaksperioderForUttrekk,
                                          startdato: LocalDate,
                                          sluttdato: LocalDate) =
            it.perioder.perioder.any {
                it.datoFra <= startdato
                && it.datoTil >= sluttdato
                && erArbeidssøker(it)
            }

    private fun erArbeidssøker(it: Vedtaksperiode) =
            (it.aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER
             || it.aktivitet == AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)

}
