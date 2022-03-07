# language: no
# encoding: UTF-8
  #                      P1      P2
  #  BehandlingId 1: |------|------|
  #  BehandlingId 2: |------|
  #  Behandling 2 fører til at P1 blir stående, mens P2 blir slettet hvor historikk peker på behandling 2

Egenskap: Andelhistorikk: Nytt vedtak sletter en andel

  Scenario: Eksisterende behandling med to perioder blir overskrevet hvor bare en periode skal beholdes

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Aktivitet         | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.2021         | 02.2021         |
      | 2            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.2021         | 01.2021         |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         |                       |
      | 1            | 02.2021         | 02.2021         |                       |
      | 2            | 01.2021         | 01.2021         | 1                     |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato |
      | 1            |              |                       | 01.2021         | 01.2021         |
      | 1            | FJERNET      | 2                     | 02.2021         | 02.2021         |

