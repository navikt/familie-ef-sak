package no.nav.familie.ef.sak.repository.domain.søknad

data class SøknadSkolepenger(val personalia: Personalia,
                             val barn: List<Barn>,
                             val innsendingsdetaljer: Innsendingsdetaljer,
                             val sivilstandsdetaljer: Sivilstandsdetaljer,
                             val medlemskapsdetaljer: Medlemskapsdetaljer,
                             val bosituasjon: Bosituasjon,
                             val sivilstandsplaner: Sivilstandsplaner? = null,
                             val utdanning: UnderUtdanning,
                             val dokumentasjon: SkolepengerDokumentasjon)

data class SkolepengerDokumentasjon(val utdanningsutgifter: Dokumentasjon? = null)
