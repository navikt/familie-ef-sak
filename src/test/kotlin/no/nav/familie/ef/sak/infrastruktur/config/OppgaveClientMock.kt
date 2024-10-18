package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Configuration
class OppgaveClientMock {
    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient: OppgaveClient = mockk()

        val oppgaver: MutableMap<Long, Oppgave> =
            listOf(
                oppgave1,
                oppgave2,
                oppgave3,
                oppgave4,
                oppgave5,
                oppgave6,
                oppgave7,
                oppgave8,
                oppgave9,
                oppgave10,
                tilbakekreving1,
                oppgavePapirsøknad,
                oppgaveEttersending,
                oppgaveEttersendingUtenBehandlesAvApplikasjon,
            ).associateBy { it.id!! }.toMutableMap()
        var maxId: Long = oppgaver.values.maxOf { it.id!! }
        every {
            oppgaveClient.hentOppgaver(any())
        } answers {
            FinnOppgaveResponseDto(oppgaver.size.toLong(), oppgaver.values.toList())
        }

        every { oppgaveClient.finnOppgaveMedId(any()) } answers {
            val oppgaveId = firstArg<Long>()
            oppgaver[oppgaveId] ?: oppgave1.copy(id = oppgaveId, tilordnetRessurs = SikkerhetContext.hentSaksbehandler())
        }

        every { oppgaveClient.finnMapper(any(), any()) } answers {
            val enhetsnr = firstArg<String>()
            if (enhetsnr == "4489") {
                FinnMappeResponseDto(
                    antallTreffTotalt = 2,
                    mapper =
                        listOf(
                            MappeDto(
                                id = 101,
                                navn = "EF Sak - 01 Uplassert lokal",
                                enhetsnr = "4489",
                            ),
                            MappeDto(
                                id = 104,
                                navn = "EF Sak - 62 Hendelser",
                                enhetsnr = "4489",
                            ),
                            MappeDto(
                                id = 105,
                                navn = "62 Hendelser",
                                enhetsnr = "4489",
                            ),
                            MappeDto(
                                id = 106,
                                navn = "64 Utdanning",
                                enhetsnr = "4489",
                            ),
                            MappeDto(
                                id = 102,
                                navn = "70 Godkjennevedtak",
                                enhetsnr = "4489",
                            ),
                            MappeDto(
                                id = 103,
                                navn = "EF Sak - 99 testmappe lokal 99",
                                enhetsnr = "4489",
                            ),
                        ),
                )
            } else if (enhetsnr == "4483") {
                FinnMappeResponseDto(
                    antallTreffTotalt = 2,
                    mapper =
                        listOf(
                            MappeDto(
                                id = 202,
                                navn = "70 Godkjennevedtak",
                                enhetsnr = "4483",
                            ),
                            MappeDto(
                                id = 203,
                                navn = "EF Sak - 99 testmappe lokal 99",
                                enhetsnr = "4483",
                            ),
                        ),
                )
            } else {
                FinnMappeResponseDto(0, emptyList())
            }
        }

        every { oppgaveClient.opprettOppgave(any()) } answers {
            val arg = firstArg<OpprettOppgaveRequest>()
            val nyOppgaveId = ++maxId
            val oppgave =
                Oppgave(
                    id = nyOppgaveId,
                    identer = arg.ident?.let { listOf(it) },
                    saksreferanse = arg.saksId,
                    tema = arg.tema,
                    oppgavetype = arg.oppgavetype.value,
                    fristFerdigstillelse = arg.fristFerdigstillelse.format(DateTimeFormatter.ISO_DATE),
                    beskrivelse = arg.beskrivelse,
                    tildeltEnhetsnr = arg.enhetsnummer,
                    behandlingstema = arg.behandlingstema,
                    journalpostId = arg.journalpostId,
                    tilordnetRessurs = arg.tilordnetRessurs,
                    status = StatusEnum.OPPRETTET,
                    opprettetTidspunkt = LocalDate.now().toString(),
                    prioritet = OppgavePrioritet.NORM,
                )
            oppgaver[nyOppgaveId] = oppgave
            nyOppgaveId
        }

        every { oppgaveClient.fordelOppgave(any(), any()) } answers {
            val oppgaveId = firstArg<Long>()
            val saksbehandler = secondArg<String?>()
            val oppgave = oppgaver[oppgaveId] ?: error("Finner ikke oppgave med $oppgaveId")
            oppgaver[oppgaveId] =
                oppgave.copy(
                    tilordnetRessurs = saksbehandler,
                    status =
                        saksbehandler?.let { StatusEnum.UNDER_BEHANDLING }
                            ?: StatusEnum.OPPRETTET,
                )
            oppgaveId
        }

        every { oppgaveClient.ferdigstillOppgave(any()) } answers {
            oppgaver.remove(firstArg())
        }

        every { oppgaveClient.oppdaterOppgave(any()) } answers {
            firstArg<Oppgave>().id ?: 0
        }

        every { oppgaveClient.hentSaksbehandlerInfo(any()) } answers {
            val navIdent = utledNavIdent(firstArg<String>())
            val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn().split(" ")
            val fornavn = saksbehandlerNavn.first()
            val etternavn = if (saksbehandlerNavn.size > 1) saksbehandlerNavn.elementAt(1) else "Saksbehandlersen"

            Saksbehandler(
                azureId = UUID.randomUUID(),
                navIdent = navIdent,
                fornavn = fornavn,
                etternavn = etternavn,
                enhet = "4405",
            )
        }

        return oppgaveClient
    }

    private val oppgave1 =
        lagOppgave(1L, Oppgavetype.Journalføring, "Z999999", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave2 = lagOppgave(2L, Oppgavetype.BehandleSak, "Z999999", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave3 =
        lagOppgave(3L, Oppgavetype.Journalføring, beskivelse = "", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave4 =
        lagOppgave(24681L, Oppgavetype.Journalføring, "SAKSBEHANDLER", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave5 =
        lagOppgave(24682L, Oppgavetype.Journalføring, "BESLUTTER", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave6 =
        lagOppgave(24683L, Oppgavetype.BehandleSak, tilordnetRessurs = null, behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave7 =
        lagOppgave(24684L, Oppgavetype.BehandleSak, tilordnetRessurs = "julenissen", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave8 =
        lagOppgave(24685L, Oppgavetype.BehandleSak, tilordnetRessurs = "BESLUTTER_2", behandlesAvApplikasjon = "familie-ef-sak")
    private val oppgave9 =
        lagOppgave(24686L, Oppgavetype.BehandleSak, tilordnetRessurs = "julenissen", behandlesAvApplikasjon = "familie-ef-sak", ytelsestema = Tema.BAR)
    private val oppgave10 =
        lagOppgave(24687L, Oppgavetype.BehandleSak, tilordnetRessurs = "julenissen", behandlesAvApplikasjon = "familie-ef-sak", status = StatusEnum.FEILREGISTRERT)
    private val oppgavePapirsøknad =
        lagOppgave(
            5L,
            Oppgavetype.Journalføring,
            beskivelse = "Papirsøknad",
            behandlesAvApplikasjon = "",
            journalpostId = "23456",
        )
    private val oppgaveEttersending =
        lagOppgave(
            6L,
            Oppgavetype.Journalføring,
            beskivelse = "Ettersending",
            behandlesAvApplikasjon = "familie-ef-sak",
            journalpostId = "23457",
        )
    private val oppgaveEttersendingUtenBehandlesAvApplikasjon =
        lagOppgave(
            7L,
            Oppgavetype.Journalføring,
            beskivelse = "Ettersending uten behandlesAvApplikasjon",
            behandlesAvApplikasjon = "",
            journalpostId = "23458",
        )
    private val tilbakekreving1 =
        lagOppgave(
            4L,
            Oppgavetype.BehandleSak,
            beskivelse = "",
            behandlingstype = "ae0161",
            behandlesAvApplikasjon = "familie-tilbake",
        )

    private fun lagOppgave(
        oppgaveId: Long,
        oppgavetype: Oppgavetype,
        tilordnetRessurs: String? = null,
        beskivelse: String? =
            "Beskrivelse av oppgaven. \n\n\n" +
                "Denne teksten kan jo være lang, kort eller ikke inneholde noenting. ",
        journalpostId: String? = "1234",
        behandlingstype: String? = null,
        behandlesAvApplikasjon: String,
        behandlingstema: Behandlingstema = Behandlingstema.Overgangsstønad,
        ytelsestema: Tema = Tema.ENF,
        status: StatusEnum = StatusEnum.OPPRETTET,
    ): Oppgave =
        Oppgave(
            id = oppgaveId,
            aktoerId = "1234",
            identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
            journalpostId = journalpostId,
            tildeltEnhetsnr = "4489",
            tilordnetRessurs = tilordnetRessurs,
            behandlingstype = behandlingstype,
            mappeId = 123,
            behandlesAvApplikasjon = behandlesAvApplikasjon,
            beskrivelse = beskivelse,
            tema = ytelsestema,
            behandlingstema = behandlingstema.value,
            oppgavetype = oppgavetype.value,
            opprettetTidspunkt = LocalDate.of(2020, 1, 1).toString(),
            fristFerdigstillelse = LocalDate.of(2020, 2, 1).toString(),
            prioritet = OppgavePrioritet.NORM,
            status = status,
            versjon = 2,
        )

    private fun utledNavIdent(navIdent: String) =
        when (navIdent) {
            "BESLUTTER" -> "ANNEN_SAKSBEHANDLER"
            "julenissen" -> "julenissen"
            else -> SikkerhetContext.hentSaksbehandler()
        }
}
