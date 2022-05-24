# language: no
# encoding: UTF-8

Egenskap: Skolepenger

  Scenario: Innvilget skolepenger skal gi beløp fordelt på måneder

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 01.2021         | 03.2021         | 100              | 200      |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 100   | 1               |

    # TODO håndterer ikke historikk ennå
    #Så forvent følgende historikk
    #  | BehandlingId | Fra og med dato | Til og med dato | Utgifter | Beløp |
    #  | 1            | 01.2021         | 03.2021         | 200      | 100   |

