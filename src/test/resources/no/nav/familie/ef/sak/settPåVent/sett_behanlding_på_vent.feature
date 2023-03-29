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
  Oppgave flyttet fra saksbehandler Ola til Kari
  Oppgave endret fra prioritet NORM til HOY
  Oppgave endret frist fra 2023-03-18 til 2023-03-24
  Oppgave flyttet fra mappe søknad til venter på dokumentasjon

  Tekst fra saksbehandler

  Gammel beskrivelse
    """

    Så forventer vi at oppgaven er oppdatert med
      | tilordnetRessurs     | Kari       |
      | fristFerdigstillelse | 24.03.2023 |
      | enhetsmappe          | 222        |
      | prioritet            | hoy        |

  Scenario: Oppdaterer beskrivelse på oppgave

    Gitt eksisterende oppgave
      | saksbehandler | Ola                |
      | frist         | 18.03.2023         |
      | prioritet     | NORM               |
      | beskrivelse   | Gammel beskrivelse |

    Og mapper
      | Mappeid | Mappenavn               |
      | 111     | søknad                  |
      | 222     | venter på dokumentasjon |

    Og sett på vent request
      | saksbehandler | Ola                     |
      | frist         | 18.03.2023              |
      | prioritet     | NORM                    |
      | beskrivelse   | Tekst fra saksbehandler |

    Når vi setter behandling på vent

    Så forventer vi følgende beskrivelse på oppgaven
    """
  Tekst fra saksbehandler
    """

    Så forventer vi at oppgaven er oppdatert med
      | tilordnetRessurs     | Ola        |
      | fristFerdigstillelse | 18.03.2023 |
      | prioritet            | NORM       |

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
  Oppgave flyttet fra saksbehandler Ola til Rita

  Sendt til Rita
    """

    Så forventer vi at oppgaven er oppdatert med
      | tilordnetRessurs     | Rita       |
      | fristFerdigstillelse | 18.03.2023 |
      | prioritet            | NORM       |

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
Oppgave endret frist fra 2023-03-18 til 2023-03-19

Venter på bruker
    """

    Så forventer vi at oppgaven er oppdatert med
      | tilordnetRessurs     | Ola        |
      | fristFerdigstillelse | 19.03.2023 |
      | prioritet            | NORM       |

