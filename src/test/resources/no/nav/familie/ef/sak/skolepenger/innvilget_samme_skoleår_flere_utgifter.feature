# language: no
# encoding: UTF-8

Egenskap: Skolepenger samme skoleår, flere utgifter

  Scenario: D - To utgifter på ett skoleår, over grensen på første utgiften - får ikke noe utbetalt for den andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 70_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 10.2021         | 06.2022         | 100              | 70_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 68_000 | 1               |

  Scenario: E - To utgifter på ett skoleår, over grensen på andre utgiften, får ikke fullt beløp for andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 60_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10.2021      | 60_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 60_000 | 1               |
      | 10.2021         | 10.2021         | 8_000  | 1               |

  Scenario: E2 - To utgifter på ett skoleår, over grensen på andre men som er i en egen periode

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 60_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 01.2022         | 06.2022         | 100              | 01.2022      | 60_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 60_000 | 1               |
      | 01.2022         | 01.2022         | 8_000  | 1               |