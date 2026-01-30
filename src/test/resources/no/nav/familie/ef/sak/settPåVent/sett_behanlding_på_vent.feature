# language: no
# encoding: UTF-8

Egenskap: Sett behandling på vent og oppdater oppgave

  Scenario: Oppdaterer saksbehandler, frist, prioritet, mappe og beskrivelse på oppgave

    Gitt eksisterende oppgave
      | saksbehandler | Ola                |
      | frist         | 18.03.2023         |
      | mappe         | 111                |
      | prioritet     | NORM               |
      | beskrivelse   | Gammel beskrivelse |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Kari                    |
      | frist         | 24.03.2023              |
      | mappe         | 222                     |
      | prioritet     | HOY                     |
      | beskrivelse   | Tekst fra saksbehandler |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
 --- 25.10.2020 13:34 System (VL) ---
  Tekst fra saksbehandler
  Oppgave flyttet fra saksbehandler Ola til Kari
  Oppgave endret fra prioritet NORM til HOY
  Oppgave endret frist fra 2023-03-18 til 2023-03-24
  Oppgave flyttet fra mappe søknad til venter på dokumentasjon

  Gammel beskrivelse
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Kari       |
      | frist         | 24.03.2023 |
      | mappe         | 222        |
      | prioritet     | HOY        |

  Scenario: Oppdaterer kun saksbehandler

    Gitt eksisterende oppgave
      | saksbehandler | Ola                |
      | frist         | 18.03.2023         |
      | mappe         | 111                |
      | prioritet     | NORM               |
      | beskrivelse   | Gammel beskrivelse |

    Gitt mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Kari       |
      | frist         | 18.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    --- 25.10.2020 13:34 System (VL) ---
  Oppgave flyttet fra saksbehandler Ola til Kari

  Gammel beskrivelse
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Kari       |
      | frist         | 18.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |

  Scenario: Oppdaterer frist på oppgave

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Gitt mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 30.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    --- 25.10.2020 13:34 System (VL) ---
  Oppgave endret frist fra 2023-03-18 til 2023-03-30
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Ola        |
      | frist         | 30.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |


  Scenario: Oppdaterer mappe på oppgave som ikke lå på mappe fra før

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | mappe         |            |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Gitt mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    --- 25.10.2020 13:34 System (VL) ---
  Oppgave flyttet fra mappe <ingen> til søknad
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | mappe         | 111        |
      | prioritet     | NORM       |


  Scenario: Fjern saksbehandler og sett inn beskrivelse

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | mappe         |            |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Gitt mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler |                                                                |
      | frist         | 18.03.2023                                                     |
      | mappe         |                                                                |
      | prioritet     | NORM                                                           |
      | beskrivelse   | Har lagt inn en ny beskrivelse.\n\nDen kan også være formatert |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    --- 25.10.2020 13:34 System (VL) ---
  Har lagt inn en ny beskrivelse.

  Den kan også være formatert
  Oppgave flyttet fra saksbehandler Ola til <ingen>
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler |            |
      | frist         | 18.03.2023 |
      | mappe         |            |
      | prioritet     | NORM       |


  Scenario: Oppdaterer kun saksbehandler og beskrivelse

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Rita           |
      | frist         | 18.03.2023     |
      | prioritet     | NORM           |
      | beskrivelse   | Sendt til Rita |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    --- 25.10.2020 13:34 System (VL) ---
  Sendt til Rita
  Oppgave flyttet fra saksbehandler Ola til Rita
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Rita       |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |

  Scenario: Oppdaterer kun frist og beskrivelse

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola              |
      | frist         | 19.03.2023       |
      | prioritet     | NORM             |
      | beskrivelse   | Venter på bruker |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
--- 25.10.2020 13:34 System (VL) ---
Venter på bruker
Oppgave endret frist fra 2023-03-18 til 2023-03-19
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Ola        |
      | frist         | 19.03.2023 |
      | prioritet     | NORM       |

  Scenario: Sett på vent uten oppdateringer

    Gitt eksisterende oppgave
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |


  Scenario: Sett på vent uten oppdateringer - med eksisterende beskrivelse

    Gitt eksisterende oppgave
      | saksbehandler | Ola                     |
      | frist         | 18.03.2023              |
      | prioritet     | NORM                    |
      | beskrivelse   | eksistrende beskrivelse |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
    eksistrende beskrivelse
    """

    Så forventer vi at oppgaven er oppdatert med
      | saksbehandler | Ola                     |
      | frist         | 18.03.2023              |
      | prioritet     | NORM                    |
      | beskrivelse   | eksistrende beskrivelse |

  Scenario: Sett på vent med en oppfølgingsoppgave som automatisk skal opprettes - informer om søkt overgangsstønad

    Gitt eksisterende oppgave
      | saksbehandler | Ola                     |
      | frist         | 18.03.2023              |
      | prioritet     | NORM                    |
      | beskrivelse   | eksistrende beskrivelse |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Og behandling
      | BehandlingId | a7947fc8-e39d-4cbc-960c-73ef39a9b59d |

    Og valgte oppfølgingsoppgaver
      | oppfølgingsopppgave               |
      | INFORMERE_OM_SØKT_OVERGANGSSTØNAD |

    Når vi setter behandling på vent

    Så forventer vi at følgende task lagres
      | Tasktype           | opprettOppgave                                       |
      | BehandlingId       | a7947fc8-e39d-4cbc-960c-73ef39a9b59d                 |
      | Oppgavetype        | VurderHenvendelse                                    |
      | Oppgavebeskrivelse | Bruker har søkt om overgangsstønad. Vurder oppfølging og eventuelt refusjonskrav. |

  Scenario: Sett på vent med en oppfølgingsoppgave som automatisk skal opprettes - be om innstilling om utdanning

    Gitt eksisterende oppgave
      | saksbehandler | Ola                     |
      | frist         | 18.03.2023              |
      | prioritet     | NORM                    |
      | beskrivelse   | eksistrende beskrivelse |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola        |
      | frist         | 18.03.2023 |
      | prioritet     | NORM       |
      | beskrivelse   |            |

    Og behandling
      | BehandlingId | a7947fc8-e39d-4cbc-960c-73ef39a9b59d |

    Og valgte oppfølgingsoppgaver
      | oppfølgingsopppgave              |
      | INNSTILLING_VEDRØRENDE_UTDANNING |

    Når vi setter behandling på vent

    Så forventer vi at følgende task lagres
      | Tasktype           | opprettOppgave                                                       |
      | BehandlingId       | a7947fc8-e39d-4cbc-960c-73ef39a9b59d                                 |
      | Oppgavetype        | VurderHenvendelse                                                    |
      | Oppgavebeskrivelse | Vi trenger en vurdering fra dere fordi bruker tar/skal ta utdanning. |
