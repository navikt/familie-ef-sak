package no.nav.familie.ef.sak.repository.domain.søknad

data class SøknadOvergangsstønad(val personalia: Personalia,
                                 val innsendingsdetaljer: Innsendingsdetaljer,
                                 val sivilstandsdetaljer: Sivilstandsdetaljer,
                                 val medlemskapsdetaljer: Medlemskapsdetaljer,
                                 val bosituasjon: Bosituasjon,
                                 val sivilstandsplaner: Sivilstandsplaner? = null,
                                 val barn: List<Barn>,
                                 val aktivitet: Aktivitet,
                                 val situasjon: Situasjon,
                                 val stønadsstart: Stønadsstart)
