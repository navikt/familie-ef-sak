# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Revurdere sanksjon for barnetilsyn

  Scenario: Sanksjonerer innvilget periode, innvilger ny periode som sanksjoneres

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Barn | Utgifter |
      | 1            | 01.2021         | 02.2021         |                 |                | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND |      |          |
      | 3            | 03.2021         | 04.2021         |                 |                | A    | 100      |
      | 4            | 04.2021         | 04.2021         | SANKSJONERE     | SANKSJON_1_MND |      |          |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |                |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 2                     |                |
      | 2            | 02.2021         | 02.2021         |              |                       | SANKSJON_1_MND |
      | 3            | 03.2021         | 03.2021         | SPLITTET     | 4                     |                |
      | 3            | 04.2021         | 04.2021         | FJERNET      | 4                     |                |
      | 4            | 04.2021         | 04.2021         |              |                       | SANKSJON_1_MND |

  Scenario: Revurder før sanksjon, beholder sanksjon

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 3            | 02.2021         | 02.2021         | INNVILGE        | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 3                     |                | 01.01.2021  |
      | 3            | 01.2021         | 01.2021         |              |                       |                | 01.03.2021  |
      | 2            | 02.2021         | 02.2021         |              |                       | SANKSJON_1_MND | 01.02.2021  |

  Scenario: Revurder før sanksjon og fjerner sanksjonen

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 3                     |                | 01.01.2021  |
      | 3            | 01.2021         | 01.2021         |              |                       |                | 01.03.2021  |
      | 2            | 02.2021         | 02.2021         | FJERNET      | 3                     | SANKSJON_1_MND | 01.02.2021  |

  Scenario: Revurder fra sanksjon og fjerner sanksjonen med ny periode i stedet for sanksjonen

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 02.2021         | 02.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         |              |                       |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         | ERSTATTET    | 3                     | SANKSJON_1_MND | 01.02.2021  |
      | 3            | 02.2021         | 02.2021         |              |                       |                | 01.03.2021  |

  Scenario: Revurder før sanksjon og fjerner sanksjonen med ny periode i stedet for sanksjonen

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 3            | 02.2021         | 02.2021         | INNVILGE        |                |                   | A    | 200      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 3                     |                | 01.01.2021  |
      | 3            | 01.2021         | 01.2021         |              |                       |                | 01.03.2021  |
      | 2            | 02.2021         | 02.2021         | ERSTATTET    | 3                     | SANKSJON_1_MND | 01.02.2021  |
      | 3            | 02.2021         | 02.2021         |              |                       |                | 01.03.2021  |

  Scenario: Revurder før sanksjon med periode før og etter, fjerner sanksjon

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 03.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 01.2021         | 03.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | ERSTATTET    | 3                     |                | 01.01.2021  |
      | 3            | 01.2021         | 03.2021         |              |                       |                | 01.03.2021  |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         | FJERNET      | 3                     | SANKSJON_1_MND | 01.02.2021  |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 3                     |                | 01.01.2021  |

  Scenario: Revurder før sanksjon med periode før og etter, beholde sanksjon

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 03.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 3            | 02.2021         | 02.2021         | INNVILGE        | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 03.2021         | 03.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 3                     |                | 01.01.2021  |
      | 3            | 01.2021         | 01.2021         |              |                       |                | 01.03.2021  |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         |              |                       | SANKSJON_1_MND | 01.02.2021  |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 3                     |                | 01.01.2021  |
      | 3            | 03.2021         | 03.2021         |              |                       |                | 01.03.2021  |

  Scenario: 2 sanksjoner revurderer før begge og begge fjernes

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |
      | 4            | REVURDERING           | 01.04.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 03.2021         | 03.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 4            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 4                     |                | 01.01.2021  |
      | 4            | 01.2021         | 01.2021         |              |                       |                | 01.04.2021  |
      | 2            | 02.2021         | 02.2021         | FJERNET      | 4                     | SANKSJON_1_MND | 01.02.2021  |
      | 3            | 03.2021         | 03.2021         | FJERNET      | 4                     | SANKSJON_1_MND | 01.03.2021  |

  Scenario: 2 sanksjoner der siste blir revurder og fjernet

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |
      | 4            | REVURDERING           | 01.04.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 03.2021         | 03.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 4            | 03.2021         | 03.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         |              |                       |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         |              |                       | SANKSJON_1_MND | 01.02.2021  |
      | 3            | 03.2021         | 03.2021         | ERSTATTET    | 4                     | SANKSJON_1_MND | 01.03.2021  |
      | 4            | 03.2021         | 03.2021         |              |                       |                | 01.04.2021  |

  Scenario: 2 sanksjoner, revurderer fra datot på første sanksjonen og begge fjernes

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |
      | 4            | REVURDERING           | 01.04.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 03.2021         | 03.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 4            | 02.2021         | 02.2021         | INNVILGE        |                |                   | A    | 100      |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         |              |                       |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         | ERSTATTET    | 4                     | SANKSJON_1_MND | 01.02.2021  |
      | 4            | 02.2021         | 02.2021         |              |                       |                | 01.04.2021  |
      | 3            | 03.2021         | 03.2021         | FJERNET      | 4                     | SANKSJON_1_MND | 01.03.2021  |

  Scenario: 2 sanksjoner med revurdering fra første der andre beholdes

    Gitt følgende behandlinger for Barnetilsyn
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 01.02.2021  |
      | 3            | REVURDERING           | 01.03.2021  |
      | 4            | REVURDERING           | 01.04.2021  |

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Sanksjonsårsak    | Barn | Utgifter |
      | 1            | 01.2021         | 01.2021         | INNVILGE        |                |                   | A    | 100      |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 3            | 03.2021         | 03.2021         | SANKSJONERE     | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |
      | 4            | 02.2021         | 02.2021         | INNVILGE        |                |                   | A    | 100      |
      | 4            | 03.2021         | 03.2021         | INNVILGE        | SANKSJON_1_MND | SAGT_OPP_STILLING |      |          |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Vedtaksdato |
      | 1            | 01.2021         | 01.2021         |              |                       |                | 01.01.2021  |
      | 2            | 02.2021         | 02.2021         | ERSTATTET    | 4                     | SANKSJON_1_MND | 01.02.2021  |
      | 4            | 02.2021         | 02.2021         |              |                       |                | 01.04.2021  |
      | 3            | 03.2021         | 03.2021         |              |                       | SANKSJON_1_MND | 01.03.2021  |
