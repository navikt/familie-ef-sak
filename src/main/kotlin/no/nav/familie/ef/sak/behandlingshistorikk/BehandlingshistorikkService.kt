package no.nav.familie.ef.sak.behandlingshistorikk

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.behandlingshistorikk.domain.tilHendelseshistorikkDto
import no.nav.familie.ef.sak.behandlingshistorikk.dto.Hendelse
import no.nav.familie.ef.sak.behandlingshistorikk.dto.HendelseshistorikkDto
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingshistorikkService(
    private val behandlingshistorikkRepository: BehandlingshistorikkRepository,
) {
    fun finnHendelseshistorikk(saksbehandling: Saksbehandling): List<HendelseshistorikkDto> {
        val (hendelserOpprettet, andreHendelser) =
            behandlingshistorikkRepository
                .findByBehandlingIdOrderByEndretTidDesc(
                    saksbehandling.id,
                ).map {
                    it.tilHendelseshistorikkDto(saksbehandling)
                }.filter {
                    it.hendelse != Hendelse.UKJENT
                }.partition { it.hendelse == Hendelse.OPPRETTET }
        val sisteOpprettetHendelse = hendelserOpprettet.lastOrNull()
        return if (sisteOpprettetHendelse != null) {
            andreHendelser + sisteOpprettetHendelse
        } else {
            andreHendelser
        }
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID): Behandlingshistorikk = behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId)

    fun finnSisteBehandlingshistorikk(
        behandlingId: UUID,
        type: StegType,
    ): Behandlingshistorikk? = behandlingshistorikkRepository.findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId, type)

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }

    /**
     * @param metadata json object that will be serialized
     */
    fun opprettHistorikkInnslag(
        behandlingId: UUID,
        stegtype: StegType,
        utfall: StegUtfall?,
        metadata: Any?,
    ) {
        opprettHistorikkInnslag(
            Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = stegtype,
                utfall = utfall,
                metadata =
                    metadata?.let {
                        JsonWrapper(objectMapper.writeValueAsString(it))
                    },
            ),
        )
    }
}
