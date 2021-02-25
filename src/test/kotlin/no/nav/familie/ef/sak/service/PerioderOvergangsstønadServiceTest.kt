package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeOvergangsstønad
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.LocalDate.parse

internal class PerioderOvergangsstønadServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)
    private val perioderOvergangsstønadService =
            PerioderOvergangsstønadService(infotrygdReplikaClient, mockk(relaxed = true), pdlClient)

    private val ident = "01234567890"

    @Test
    internal fun `hentPerioder henter perioder fra infotrygd med alle identer fra pdl`() {
        val historiskIdent = "01234567890"
        val fomDato = LocalDate.MIN
        val tomDato = LocalDate.MAX
        val request = PerioderOvergangsstønadRequest(ident, fomDato, tomDato)

        mockPdl(historiskIdent)
        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                infotrygdResponse(InfotrygdPeriodeOvergangsstønad(ident, LocalDate.now(), LocalDate.now(), 10f))

        val hentPerioder = perioderOvergangsstønadService.hentReplikaPerioder(request)

        assertThat(hentPerioder.perioder).hasSize(1)

        verify(exactly = 1) { pdlClient.hentPersonidenter(ident, true) }
        verify(exactly = 1) {
            val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(setOf(ident, historiskIdent), fomDato, tomDato)
            infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        }
    }

    @Test
    internal fun `skal kalle infotrygd hvis pdl ikke finner personIdent med personIdent i requesten`() {
        every { pdlClient.hentPersonidenter(any(), true) } throws PdlNotFoundException()

        perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))
        verify(exactly = 1) {
            infotrygdReplikaClient.hentPerioderOvergangsstønad(InfotrygdPerioderOvergangsstønadRequest(setOf(ident)))
        }
    }

    @Test
    internal fun `skal slå sammen perioder til arena`() {
        mockPdl()
        val infotrygPerioder = listOf(periode(parse("2017-08-01"), parse("2018-04-30"), 1f, parse("2018-07-31")),
                                      periode(parse("2018-05-01"), parse("2020-07-31"), 1f, parse("2018-07-31")),
                                      periode(parse("2018-09-01"), parse("2018-12-31"), 1f),
                                      periode(parse("2019-01-01"), parse("2019-04-30"), 1f),
                                      periode(parse("2019-05-01"), parse("2020-04-30"), 1f),
                                      periode(parse("2020-05-01"), parse("2020-08-31"), 1f))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2017-08-01") to parse("2018-07-31"),
                                                  parse("2018-09-01") to parse("2020-08-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena 2`() {
        mockPdl()
        val infotrygPerioder = listOf(periode(parse("2018-12-01"), parse("2019-04-30"), 1f),
                                      periode(parse("2019-05-01"), parse("2019-12-31"), 1f),
                                      periode(parse("2019-12-01"), parse("2020-02-29"), 1f),
                                      periode(parse("2020-02-01"), parse("2020-04-30"), 1f),
                                      periode(parse("2020-05-01"), parse("2020-10-31"), 1f),
                                      periode(parse("2020-09-01"), parse("2020-12-31"), 1f),
                                      periode(parse("2021-01-01"), parse("2022-01-31"), 1f))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2018-12-01") to parse("2022-01-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena 3`() {
        mockPdl()
        val infotrygPerioder = listOf(periode(parse("2017-06-01"), parse("2017-10-31"), 1f, parse("2018-07-31")),
                                      periode(parse("2017-08-01"), parse("2018-04-30"), 1f, parse("2018-07-31")),
                                      periode(parse("2018-05-01"), parse("2020-07-31"), 1f, parse("2018-07-31")),
                                      periode(parse("2018-09-01"), parse("2018-12-31"), 1f),
                                      periode(parse("2019-01-01"), parse("2019-04-30"), 1f),
                                      periode(parse("2019-05-01"), parse("2020-04-30"), 1f),
                                      periode(parse("2020-05-01"), parse("2020-08-31"), 1f))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2017-06-01") to parse("2018-07-31"),
                                                  parse("2018-09-01") to parse("2020-08-31"))
        )
    }

    @Test
    internal fun `skal slå sammen perioder til arena 4`() {
        mockPdl()
        val infotrygPerioder =
                listOf(periode(parse("2018-05-01"), parse("2018-05-31"), 1555f),
                       periode(parse("2018-04-01"), parse("2018-05-31"), 2887f),
                       periode(parse("2018-03-01"), parse("2018-05-31"), 0f),
                       periode(parse("2018-02-01"), parse("2018-05-31"), 2024f),
                       periode(parse("2017-08-01"), parse("2018-05-31"), 3269f),
                       periode(parse("2016-09-01"), parse("2018-05-31"), 869f),
                       periode(parse("2016-04-01"), parse("2018-05-31"), 0f),
                       periode(parse("2016-03-01"), parse("2018-05-31"), 0f),
                       periode(parse("2018-01-01"), parse("2018-01-31"), 3299f),
                       periode(parse("2017-12-01"), parse("2017-12-31"), 0f),
                       periode(parse("2017-11-01"), parse("2017-12-31"), 1199f),
                       periode(parse("2017-10-01"), parse("2017-12-31"), 1612f),
                       periode(parse("2017-09-01"), parse("2017-12-31"), 0f),
                       periode(parse("2017-08-01"), parse("2017-12-31"), 1424f),
                       periode(parse("2017-02-01"), parse("2017-12-31"), 306f),
                       periode(parse("2017-05-01"), parse("2017-11-30"), 3269f),
                       periode(parse("2017-04-01"), parse("2017-04-30"), 2406f),
                       periode(parse("2017-03-01"), parse("2017-04-30"), 0f),
                       periode(parse("2017-02-01"), parse("2017-04-30"), 3006f),
                       periode(parse("2017-01-01"), parse("2017-02-28"), 2969f),
                       periode(parse("2016-12-01"), parse("2017-02-28"), 0f),
                       periode(parse("2016-11-01"), parse("2017-02-28"), 2256f),
                       periode(parse("2016-10-01"), parse("2017-02-28"), 2069f),
                       periode(parse("2016-09-01"), parse("2017-02-28"), 269f),
                       periode(parse("2016-08-01"), parse("2016-08-31"), 2481f),
                       periode(parse("2016-07-01"), parse("2016-07-31"), 3194f),
                       periode(parse("2016-06-01"), parse("2016-06-30"), 0f),
                       periode(parse("2016-05-01"), parse("2016-05-31"), 269f))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(
                listOf(parse("2016-05-01") to parse("2016-05-31"),
                       parse("2016-07-01") to parse("2018-05-31"))
        )
    }

    @Test
    internal fun `skal slå sammen perioder til arena 5 - perioder som har samme startdato og sluttdato`() {
        mockPdl()
        val infotrygPerioder =
                listOf(periode(parse("2017-01-01"), parse("2017-06-30"), 17358f, parse("2017-01-01")),
                       periode(parse("2017-01-01"), parse("2017-01-31"), 17358f, parse("2017-01-01")),
                       periode(parse("2017-01-01"), parse("2017-05-31"), 17358f, parse("2017-03-31")))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2017-01-01") to parse("2017-03-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena - sammenhengende måneder`() {
        mockPdl()
        val infotrygPerioder =
                listOf(periode(parse("2011-08-01"), parse("2011-08-01"), 1f),
                       periode(parse("2011-09-01"), parse("2017-02-28"), 1f))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2011-08-01") to parse("2017-02-28")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena - opphør før FOM`() {
        mockPdl()
        val infotrygPerioder =
                listOf(periode(parse("2013-04-01"), parse("2013-06-30"), 2013f),
                       periode(parse("2009-02-01"), parse("2011-04-30"), 11534f, parse("2009-01-31")),
                       periode(parse("2009-02-01"), parse("2011-04-30"), 11534f, parse("2009-02-28")),
                       periode(parse("2008-12-01"), parse("2009-01-31"), 11534f, parse("2009-01-31")))

        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                InfotrygdPerioderOvergangsstønadResponse(infotrygPerioder)

        val perioder = perioderOvergangsstønadService.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))

        val fomTomDatoer = perioder.perioder.map { it.fomDato to it.tomDato }
        assertThat(fomTomDatoer).isEqualTo(listOf(parse("2008-12-01") to parse("2009-02-28"),
                                                  parse("2013-04-01") to parse("2013-06-30")))
    }

    private fun periode(fomDato: LocalDate, tomDato: LocalDate, beløp: Float, opphørsdato: LocalDate? = null) =
            InfotrygdPeriodeOvergangsstønad(ident, fomDato, tomDato, beløp, opphørsdato)

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(pdlIdenter)
    }

    private fun infotrygdResponse(vararg infotrygdPeriodeOvergangsstønad: InfotrygdPeriodeOvergangsstønad) =
            InfotrygdPerioderOvergangsstønadResponse(infotrygdPeriodeOvergangsstønad.toList())
}