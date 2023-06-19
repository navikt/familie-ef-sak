package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FritekstBrevKategori
import org.springframework.data.annotation.Id
import java.util.UUID

@Deprecated("Skal slettes")
data class MellomlagretFritekstbrev(@Id val behandlingId: UUID, val brev: Fritekstbrev, val brevType: FritekstBrevKategori)
