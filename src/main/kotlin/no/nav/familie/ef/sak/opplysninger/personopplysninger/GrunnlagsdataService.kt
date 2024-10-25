package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.Timer.loggTid
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Personopplysninger
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
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
) {
    fun opprettGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdataDomene = hentFraRegisterMedSøknadsdata(behandlingId)

        val grunnlagsdata =
            Grunnlagsdata(
                behandlingId = behandlingId,
                data = grunnlagsdataDomene,
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

    fun hentGrunnlagsdataForBehandlinger(behandlingIder: Set<UUID>): Map<UUID, GrunnlagsdataMedMetadata> {
        val grunnlagsdataForBehandlinger = hentLagretGrunnlagsdataForBehandlinger(behandlingIder)
        return grunnlagsdataForBehandlinger.associate { it.behandlingId to GrunnlagsdataMedMetadata(it.data, it.sporbar.opprettetTid) }
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

    fun hentLagretGrunnlagsdata(behandlingId: UUID): Grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandlingId)

    fun hentLagretGrunnlagsdataForBehandlinger(behandlingIder: Set<UUID>): List<Grunnlagsdata> = grunnlagsdataRepository.findAllByIdOrThrow(behandlingIder) { it.behandlingId }

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
    ): GrunnlagsdataDomene =
        loggTid {
            grunnlagsdataRegisterService.hentGrunnlagsdataFraRegister(
                personIdent,
                barneforeldreFraSøknad,
            )
        }

    fun hentPersonopplysninger(personIdent: String): Personopplysninger =
        loggTid {
            grunnlagsdataRegisterService.hentPersonopplysninger(personIdent)
        }

    fun oppdaterEndringer(grunnlagsdata: Grunnlagsdata): Grunnlagsdata = grunnlagsdataRepository.update(grunnlagsdata)
}
