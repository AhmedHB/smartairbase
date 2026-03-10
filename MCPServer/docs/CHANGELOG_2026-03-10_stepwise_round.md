# Ändringslogg 2026-03-10

## Syfte

Införa ett stegvis rundflöde för SmartAirBase så att MCP-klienten kan styra spelets sekvens i mindre, reglerade steg i stället för att servern kör hela rundan automatiskt.

## Viktiga funktionsändringar

- `execute_round` har ersatts av separata rundsteg:
  - `start_round`
  - `resolve_missions`
  - `record_dice_roll`
  - `list_available_landing_bases`
  - `land_aircraft`
  - `send_aircraft_to_holding`
  - `complete_round`
- Uppdrag löses nu först till `AWAITING_DICE_ROLL` och därefter till `AWAITING_LANDING`.
- Tärningsvärdet kommer nu från klienten/spelaren i stället för att kastas automatiskt på servern.
- Landning valideras mot tillgängliga parkeringsplatser och baskapacitet för underhåll.
- Holding kan nu användas explicit när ingen bas kan ta emot planet.
- Ny runda kan bara startas om ingen runda redan är öppen och spelet fortfarande är aktivt.

## Domänmodell

- Ny enum: `RoundPhase`
  - `PLANNING`
  - `DICE_ROLL`
  - `LANDING`
  - `ROUND_COMPLETE`
- `GameRound` har fått:
  - `phase`
  - hjälpfunktion för om rundan är öppen
- `AircraftState` har fått:
  - `lastDiceValue`
- `BaseState` har fått hjälpfunktioner för att öka/minska parkering- och maintenance-platser.

## MCP/DTO

- `GameSummaryDto` visar nu:
  - aktuell rundfas
  - om rundan är öppen
  - om klienten får starta eller avsluta runda
- `AircraftStateDto` visar nu:
  - `lastDiceValue`
  - `allowedActions`
- `MissionStateDto` visar blockerande orsak när mission inte är valbar.
- Ny DTO:
  - `LandingOptionDto`
  - `LandingOptionsDto`
- `RoundExecutionResultDto` visar nu fas, pending aircraft och meddelanden per rundsteg.

## Service-lager

- `RoundService` är omskriven från monolitisk helrunda till fasstyrd tillståndsmaskin.
- `GameQueryService` beräknar nu rundstatus och rekommenderade tillåtna handlingar.
- `AircraftService` returnerar utökad flygplansstatus med tillåtna handlingar.

## Repository-lager

- Nya query-metoder för:
  - aktiv öppen runda
  - slaget tärning per runda och flygplan
  - full-service-regel
  - baskapabilitet per servicetyp

## Liquibase

- `001-create-schema.yml` är uppdaterad så att den faktiska grundstrukturen nu innehåller:
  - `aircraft_state.last_dice_value`
  - `game_round.phase`
- `003-create-game-schema.yml` har neutraliserats eftersom samma game-tabeller redan skapas i `001-create-schema.yml`.

## Verifiering

- Lokal verifiering körd med:
  - `./mvnw test -Dspring.liquibase.clear-checksums=true -Dspring.liquibase.drop-first=true`
- Resultat:
  - build success
  - Spring context startade
  - Liquibase skapade schema och laddade seed-data utan fel
