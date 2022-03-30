# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Sanksjon

  Scenario: Sanksjon mellom to innvilgelser skal være med i vedtakshistorikken

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |
      | 2            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 04.2022         | 05.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            |              |                       | 0       | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |
      | 2            |              |                       | 0       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |
      | 3            |              |                       | 0       | FORSØRGER_I_ARBEID   | 04.2022         | 05.2022         |

  Scenario: Flere sanksjoner mellom flere innvilgelser skal være med i vedtakshistorikken

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 10.2021         | 10.2021         |
      | 2            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 11.2021         | 11.2021         |
      | 3            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 12.2021         | 12.2021         |
      | 4            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 01.2022         | 01.2022         |
      | 5            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 02.2022         | 02.2022         |
      | 6            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 03.2022         | 04.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            |              |                       | 0       | BARN_UNDER_ETT_ÅR    | 10.2021         | 10.2021         |
      | 2            |              |                       | 0       | BARN_UNDER_ETT_ÅR    | 11.2021         | 11.2021         |
      | 3            |              |                       | 0       | IKKE_AKTIVITETSPLIKT | 12.2021         | 12.2021         |
      | 4            |              |                       | 0       | FORSØRGER_I_ARBEID   | 01.2022         | 01.2022         |
      | 5            |              |                       | 0       | IKKE_AKTIVITETSPLIKT | 02.2022         | 02.2022         |
      | 6            |              |                       | 0       | FORSØRGER_I_ARBEID   | 03.2022         | 04.2022         |

  Scenario: Sanksjon som erstatter en innvilgelse skal være med i vedtakshistorikken

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 03.2022         |
      | 2            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 04.2022         | 05.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | SPLITTET     | 2                     | 0       | BARN_UNDER_ETT_ÅR    | 01.2022         | 02.2022         |
      | 1            | FJERNET      | 2                     | 0       | BARN_UNDER_ETT_ÅR    | 03.2022         | 03.2022         |
      | 2            |              |                       | 0       | IKKE_AKTIVITETSPLIKT | 03.2022         | 03.2022         |
      | 3            |              |                       | 0       | FORSØRGER_I_ARBEID   | 04.2022         | 05.2022         |

  Scenario: Sanksjon blir erstattet i historikken

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Vedtaksperiode | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR    | 01.2022         | 01.2022         |
      | 2            | SANKSJONERE     | SANKSJON       | IKKE_AKTIVITETSPLIKT | 02.2022         | 02.2022         |
      | 3            | INNVILGE        | HOVEDPERIODE   | FORSØRGER_I_ARBEID   | 02.2022         | 03.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Aktivitet            | Fra og med dato | Til og med dato |
      | 1            |              |                       | 0       | BARN_UNDER_ETT_ÅR    | 01.2022         | 01.2022         |
      | 2            | ERSTATTET    | 3                     | 0       | IKKE_AKTIVITETSPLIKT | 02.2022         | 02.2022         |
      | 3            |              |                       | 0       | FORSØRGER_I_ARBEID   | 02.2022         | 03.2022         |

