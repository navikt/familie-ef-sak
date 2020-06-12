package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.mapper.sjekkAtAlleVerdierErSatt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.søknad
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class OvergangsstønadMapperTest {

    @Test
    internal fun `sjekker at mappet aktivitet har fått satt alle verdier`() {
        val dto = OvergangsstønadMapper.tilAktivitetDto(søknad(), "Ja")
        sjekkAtAlleVerdierErSatt(dto)
    }

    @Test
    internal fun `sjekker at mappet sagtOppEllerRedusertStillingDto har fått satt alle verdier`() {
        val dto = OvergangsstønadMapper.tilSagtOppEllerRedusertStilling(søknad().situasjon.verdi)
        sjekkAtAlleVerdierErSatt(dto!!)
    }

    private fun søknad(): Søknad {
        return søknad(aktivitet = aktivitet(), situasjon = situasjon())
    }

    private fun situasjon(): Søknadsfelt<Situasjon> =
            søknadsfelt(Situasjon(arbeidskontrakt = dokumentliste(),
                                  barnMedSærligeBehov = dokumentliste(),
                                  barnsSykdom = dokumentliste(),
                                  gjelderDetteDeg = søknadsfelt(emptyList()),
                                  manglendeBarnepass = dokumentliste(),
                                  oppsigelseReduksjonDokumentasjon = dokumentliste(),
                                  oppsigelseReduksjonTidspunkt = søknadsfelt(LocalDate.now()),
                                  oppsigelseReduksjonÅrsak = søknadsfelt("Årsak"),
                                  oppstartNyJobb = null,
                                  oppstartUtdanning = null,
                                  sagtOppEllerRedusertStilling = søknadsfelt("Ja"),
                                  sykdom = dokumentliste(),
                                  utdanningstilbud = dokumentliste()))

    private fun dokumentliste(): Søknadsfelt<List<Dokument>> = søknadsfelt(listOf(Dokument(byteArrayOf(), "tittel")))

    private fun aktivitet(): Søknadsfelt<Aktivitet> =
            søknadsfelt(Aktivitet(arbeidsforhold = arbeidsforhold(),
                                  arbeidssøker = arbeidssøker(),
                                  hvordanErArbeidssituasjonen = søknadsfelt(listOf("Fin")),
                                  selvstendig = selvstendig(),
                                  underUtdanning = underUtdanning(),
                                  aksjeselskap = aksjeselskap(),
                                  virksomhet = virksomhet()))

    private fun aksjeselskap(): Søknadsfelt<List<Aksjeselskap>>? =
            søknadsfelt(listOf(Aksjeselskap(navn = søknadsfelt("navn"), arbeidsmengde = søknadsfelt(30))))

    private fun virksomhet() = søknadsfelt(Virksomhet(virksomhetsbeskrivelse = søknadsfelt("virksomhet")))

    private fun underUtdanning(): Søknadsfelt<UnderUtdanning> =
            søknadsfelt(UnderUtdanning(heltidEllerDeltid = søknadsfelt("Heltid"),
                                       hvaErMåletMedUtdanningen = søknadsfelt("Kose seg"),
                                       hvorMyeSkalDuStudere = søknadsfelt(50),
                                       offentligEllerPrivat = søknadsfelt("privat"),
                                       skoleUtdanningssted = søknadsfelt("Oslo"),
                                       tidligereUtdanninger = søknadsfelt(listOf(utdanning())),
                                       utdanning = søknadsfelt(utdanning()),
                                       utdanningEtterGrunnskolen = søknadsfelt(true)))

    private fun utdanning(): Utdanning =
            Utdanning(linjeKursGrad = søknadsfelt("linje"),
                      nårVarSkalDuVæreElevStudent = søknadsfelt(periode()))

    private fun periode() = Periode(Month.JANUARY, 2020, Month.JANUARY, 2021)

    private fun selvstendig(): Søknadsfelt<Selvstendig> =
            søknadsfelt(Selvstendig(arbeidsmengde = søknadsfelt(50),
                                    etableringsdato = søknadsfelt(LocalDate.now()),
                                    firmanavn = søknadsfelt("SelvstendigFirmanavn"),
                                    hvordanSerArbeidsukenUt = søknadsfelt("fin"),
                                    organisasjonsnummer = søknadsfelt("987654321")))

    private fun arbeidsforhold(): Søknadsfelt<List<Arbeidsgiver>> =
            søknadsfelt(listOf(Arbeidsgiver(arbeidsgivernavn = søknadsfelt("Arbeidsgivernavn"),
                                            fastEllerMidlertidig = søknadsfelt("Fast"),
                                            harSluttdato = null,
                                            sluttdato = søknadsfelt(LocalDate.now()),
                                            arbeidsmengde = søknadsfelt(50))))

    private fun arbeidssøker(): Søknadsfelt<Arbeidssøker> =
            søknadsfelt(Arbeidssøker(hvorØnskerDuArbeid = søknadsfelt("På dagen"),
                                     kanDuBegynneInnenEnUke = søknadsfelt(true),
                                     kanDuSkaffeBarnepassInnenEnUke = søknadsfelt(true),
                                     registrertSomArbeidssøkerNav = søknadsfelt(true),
                                     villigTilÅTaImotTilbudOmArbeid = søknadsfelt(true),
                                     ønskerDuMinst50ProsentStilling = søknadsfelt(true)))
}
