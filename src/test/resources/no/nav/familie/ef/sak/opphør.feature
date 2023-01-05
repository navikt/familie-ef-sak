# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Opphør

  Scenario: Periode fjernes som følge av opphør i sin helhet

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 01.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 01.2021     |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 01.2021         | FJERNET      | 2                     |           |
      | 2            | 01.2021         | 01.2021         |              |                       | Ja        |


  Scenario: Opphør midt i en periode

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 02.2021     |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |           |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |           |
      | 2            | 02.2021         | 02.2021         |              |                       | Ja        |

  Scenario: Opphør et tidligere opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 03.2021     |
      | 3            |                 |                 | OPPHØRT         | 02.2021     |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 3                     |           |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 3                     |           |
      | 3            | 02.2021         | 02.2021         |              |                       | Ja        |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 2                     |           |
      | 2            | 03.2021         | 03.2021         | FJERNET      | 3                     | Ja        |

  Scenario: Innvilger etter opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 02.2021     |
      | 3            | 03.2021         | 03.2021         |                 |             |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |           |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |           |
      | 2            | 02.2021         | 02.2021         |              |                       | Ja        |
      | 3            | 03.2021         | 03.2021         |              |                       |           |

    # TODO sanksjon

  Scenario: Flere innvilget/opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 03.2021     |
      | 3            | 03.2021         | 04.2021         |                 |             |
      | 4            |                 |                 | OPPHØRT         | 04.2021     |
      | 5            | 05.2021         | 05.2021         |                 |             |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 02.2021         | SPLITTET     | 2                     |           |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 2                     |           |
      | 2            | 03.2021         | 03.2021         | ERSTATTET    | 3                     | Ja        |
      | 3            | 03.2021         | 03.2021         | SPLITTET     | 4                     |           |
      | 3            | 04.2021         | 04.2021         | FJERNET      | 4                     |           |
      | 4            | 04.2021         | 04.2021         |              |                       | Ja        |
      | 5            | 05.2021         | 05.2021         |              |                       |           |

  Scenario: Innvilger etter opphør, men før opphørsdato

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |             |
      | 2            |                 |                 | OPPHØRT         | 03.2021     |
      | 3            | 02.2021         | 03.2021         |                 |             |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 3                     |           |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 3                     |           |
      | 3            | 02.2021         | 03.2021         |              |                       |           |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 2                     |           |
      | 2            | 03.2021         | 03.2021         | FJERNET      | 3                     | Ja        |


  Scenario: En periode splittes og en periode fjernes som følge av opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 03.2021         |
      | 2            | 02.2021         |                 |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |

