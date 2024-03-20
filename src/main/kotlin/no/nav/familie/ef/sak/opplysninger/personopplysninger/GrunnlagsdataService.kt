package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.Timer.loggTid
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class GrunnlagsdataService(
    private val grunnlagsdataRepository: GrunnlagsdataRepository,
    private val søknadService: SøknadService,
    private val grunnlagsdataRegisterService: GrunnlagsdataRegisterService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val kontantstøtteService: KontantstøtteService,
) {
    fun opprettGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdataDomene = hentFraRegisterMedSøknadsdata(behandlingId)

        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val harKontantstøttePerioder = kontantstøtteService.finnesKontantstøtteUtbetalingerPåBruker(personIdent).finnesUtbetaling

        val grunnlagsdata =
            Grunnlagsdata(
                behandlingId = behandlingId,
                data = grunnlagsdataDomene.copy(harKontantstøttePerioder = harKontantstøttePerioder),
            )
        grunnlagsdataRepository.insert(grunnlagsdata)
        return GrunnlagsdataMedMetadata(
            grunnlagsdata.data,
            grunnlagsdata.sporbar.opprettetTid,
        )
    }

    fun hentFraRegister(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdata = hentFraRegisterMedSøknadsdata(behandlingId)
        return GrunnlagsdataMedMetadata(
            grunnlagsdata = grunnlagsdata,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    fun hentGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdata = hentLagretGrunnlagsdata(behandlingId)
        return GrunnlagsdataMedMetadata(
            grunnlagsdata.data,
            grunnlagsdata.sporbar.opprettetTid,
        )
    }

    @Transactional
    fun oppdaterOgHentNyGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            "Behandlingen har en ny eier og du kan derfor ikke laste inn nye grunnlagsdata"
        }
        slettGrunnlagsdataHvisFinnes(behandlingId)
        opprettGrunnlagsdata(behandlingId)
        return hentGrunnlagsdata(behandlingId)
    }

    private fun slettGrunnlagsdataHvisFinnes(behandlingId: UUID) {
        grunnlagsdataRepository.deleteById(behandlingId)
    }

    fun hentLagretGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        return grunnlagsdataRepository.findByIdOrThrow(behandlingId)
    }

    fun hentFraRegisterMedSøknadsdata(behandlingId: UUID): GrunnlagsdataDomene {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val søknad =
            when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId)
                StønadType.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId)
                StønadType.SKOLEPENGER -> søknadService.hentSkolepenger(behandlingId)
            }

        return if (søknad == null) {
            hentFraRegisterForPersonOgAndreForeldre(behandlingService.hentAktivIdent(behandlingId), emptyList())
        } else {
            val personIdent = søknad.fødselsnummer
            val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
            hentFraRegisterForPersonOgAndreForeldre(personIdent, barneforeldreFraSøknad)
        }
    }

    fun hentFraRegisterForPersonOgAndreForeldre(
        personIdent: String,
        barneforeldreFraSøknad: List<String>,
    ): GrunnlagsdataDomene {
        return loggTid {
            grunnlagsdataRegisterService.hentGrunnlagsdataFraRegister(
                personIdent,
                barneforeldreFraSøknad,
            )
        }
    }

    fun hentGrunnlagsdataUtenTidligereVedtakshistorikk(personIdent: String): GrunnlagsdataDomene {
        return loggTid {
            grunnlagsdataRegisterService.hentGrunnlagsdataUtenVedtakshitorikkFraRegister(personIdent)
        }
    }

    fun oppdaterEndringer(grunnlagsdata: Grunnlagsdata): Grunnlagsdata {
        return grunnlagsdataRepository.update(grunnlagsdata)
    }
}
