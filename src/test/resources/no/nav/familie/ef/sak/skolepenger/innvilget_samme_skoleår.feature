# language: no
# encoding: UTF-8

Egenskap: Skolepenger samme skåleår

  Scenario: En utgift på ett skoleår

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |

    # TODO håndterer ikke historikk ennå
    #Så forvent følgende historikk
    #  | BehandlingId | Fra og med dato | Til og med dato | Utgifter | Beløp |
    #  | 1            | 01.2021         | 03.2021         | 200      | 100   |


  Scenario: En utgift på ett skoleår over grensen skal gi maksbeløp for hva man kan få i stønad

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 100_000  |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |

  Scenario: To utgifter på ett skoleår, over grensen på første utgiften - får ikke noe utbetalt for den andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 100_000  |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 10.2021         | 06.2022         | 100              | 10_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |

  Scenario: To utgifter på ett skoleår, over grensen på andre utgiften, får ikke fullt utbetalt for den andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 10.2021         | 06.2022         | 100              | 100_000  |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |