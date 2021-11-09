package no.nav.familie.ef.sak.brev.domain

import org.springframework.data.annotation.Id
import java.util.UUID


data class MellomlagretFritekstbrev(@Id val behandlingId: UUID, val brev: Fritekstbrev)
