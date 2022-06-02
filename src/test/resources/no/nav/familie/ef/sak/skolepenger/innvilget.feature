# language: no
# encoding: UTF-8

Egenskap: Skolepenger

  Scenario: Innvilget skolepenger

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 01.2021         | 03.2021         | 100              | 01.2021      | 200      |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 200   | 1               |

    # TODO håndterer ikke historikk ennå
    #Så forvent følgende historikk
    #  | BehandlingId | Fra og med dato | Til og med dato | Utgifter | Beløp |
    #  | 1            | 01.2021         | 03.2021         | 200      | 100   |

