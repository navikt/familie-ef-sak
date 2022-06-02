# language: no
# encoding: UTF-8

Egenskap: Skolepenger samme skoleår flere skoleårsperioder

  Scenario: flere utgifter fordelt på flere perioder som går over maksbeløp er ikke tillatt

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 08.2021      | 20_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 04.2022         | 06.2022         | 100              | 04.2022      | 20_000   |

    Når beregner ytelse kaster feil med innehold Antall perioder for skoleår=2021 er fler enn 1
