# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Sanksjon

  Scenario: Revurderer en periode som inneholder sanksjon

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato | Sanksjonsårsak    |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |                   |
      | 2            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         | SAGT_OPP_STILLING |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 01.2022         | 02.2022         |                   |
      | 3            | INNVILGE        | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         | SAGT_OPP_STILLING |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 3
      | Beløp | Fra og med dato | Til og med dato | Kildebehandling |
      | 19950 | 01.2022         | 02.2022         | 3               |

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | ERSTATTET    | 3                     | 0       | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |
      | 3            |              |                       | 0       | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 01.2022         | 02.2022         |
      | 2            |              |                       | 0       | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |

  Scenario: Revurderer en periode som inneholder sanksjon, med innvilget periode etterpå

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato | Sanksjonsårsak    |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |                   |
      | 2            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         | SAGT_OPP_STILLING |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 01.2022         | 02.2022         |                   |
      | 3            | INNVILGE        | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         | SAGT_OPP_STILLING |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 04.2022         | 04.2022         |                   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 3
      | Beløp | Fra og med dato | Til og med dato | Kildebehandling |
      | 19950 | 01.2022         | 02.2022         | 3               |
      | 19950 | 04.2022         | 04.2022         | 3               |

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | ERSTATTET    | 3                     | 0       | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |
      | 3            |              |                       | 0       | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 01.2022         | 02.2022         |
      | 2            |              |                       | 0       | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |
      | 3            |              |                       | 0       | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 04.2022         | 04.2022         |
