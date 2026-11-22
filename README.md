# CreativeGates – TheCivia fork

Fork pluginu **CreativeGate** (Tamasoft): portály mezi branami postavenými hráči. Rám z bloků, uvnitř voda nebo portál, brány se stejným jménem sítě (pojmenovaný item z kovadliny) jsou propojené.

## Větve
- `master`: původní kód (Kotlin, Gradle) beze změn, jen tento README.
- `feature/badges-hooks-and-fixes`: naše verze **0.1.2-thecivia**:
  - **Maven build proti Paper 26.3** místo Gradle,
  - **příkazy za konzoli** při vytvoření brány (`createCommands`) a při průchodu (`travelCommands`, `travelCommandsOwnGates`) s placeholdery `{player} {uuid} {network} {gate} {gates} {creator} {creator_uuid}`; hodnoty od hráče se čistí na `[\p{L}\p{N}_-]` (max. 64 znaků), takže nejde připsat vlastní argumenty (napojení na Badges),
  - **bezpečné ukládání `gates.json`**: atomický zápis (`*.tmp` + `ATOMIC_MOVE`), ukládání na pozadí, synchronně při vypnutí.

- `fix/review-0.1.3` (navazuje na předchozí, dnes **verze 0.1.6-thecivia**):
  - 0.1.3: opravy z code review: únik paměti ve `Frames`, bezpečnější ukládání (jen při změně, poškozený `gates.json` se přejmenuje), částice místo skutečné exploze při rozbití rámu, vlastnictví sítě `networkOwnership` (`creativegates.network.bypass`), správná ruka a výdej itemu, index bran podle tvůrce, limit jen při vytvoření, `teleportAsync` do středu bloku s kontrolou průchodnosti, příkazy po průchodu až po příchodu,
  - 0.1.4: `networkOwnership` je výchozí `true` (vlastník sítě je tvůrce nejstarší brány se známým tvůrcem, síť bez známého tvůrce je uzavřená), režim brány (blaze rod) přepne jen tvůrce, hráč s `creativegates.mode.bypass` nebo hráč, který smí na místě stavět, vstup do brány je zakázaný při combat tagu PvPManageru (volitelná závislost) a `combatBlockSeconds` (10) sekund po zranění (`creativegates.combat.bypass`), před vytvořením brány se ověří ochrana pozemků (`checkBuildPermission`, BlockPlaceEvent pro Lands a WorldGuard) a rám nesmí patřit jiné bráně, výstup se kontroluje až po načtení chunku (průchozí, bez lávy, ohně a jiných zraňujících bloků),
  - 0.1.5: hodiny beze jména už nekončí výjimkou v MiniMessage zprávě,
  - 0.1.6: výstup v pevném bloku (cestička, pole, půlblok) se zkusí o blok výš,
  - volitelná událost pro analytiku po průchodu bránou (plugin `CiviaAnalytics`, když neběží, nic se neděje).

## Build
Na větvi `fix/review-0.1.3` (0.1.6-thecivia, nebo `feature/badges-hooks-and-fixes` pro 0.1.2):
```
mvn clean package
```
