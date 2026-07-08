Word Duel Documentation Package

Purpose
- This folder defines the product, rules, UX, balance, multiplayer behavior, QA coverage, and future expansion for Word Duel.
- The documents are written to remove ambiguity before implementation.
- Scope is MVP-first, with future-facing notes where useful.

Game Summary
- Word Duel is a 2-player real-time word race.
- Each player chooses one letter before the round starts.
- At the start of every round, the system randomly assigns direction for the pair.
- Option 1: Player 1 letter becomes the required first letter and Player 2 letter becomes the required last letter.
- Option 2: Player 2 letter becomes the required first letter and Player 1 letter becomes the required last letter.
- Players do not know the direction before the round begins.
- Once the round starts, both players see the final required word condition.
- Both players then race to submit a valid English word within 60 seconds.
- The first valid submitted word wins the round.

Core Product Goals
- Fast match start and fast resolution.
- High clarity on what is valid or invalid.
- Strong competitive fairness despite network latency.
- Minimalist presentation with sharp feedback.
- High replay value through short sessions and rematch flow.

MVP Boundaries
- Platform target: Android.
- Main mode: live 1v1.
- Input type: typed text only.
- Dictionary requirement: trusted English dictionary with Oxford-style acceptance; fallback dictionary providers are allowed if they preserve rule consistency.
- No power-ups or ranked mode in MVP.

Non-Goals For MVP
- Long-form social features.
- Large cosmetic economies.
- Multi-round tournaments.
- Team play.

Document Order
- 01_core_game defines the match concept and outcome rules.
- 02_rules defines exact validation behavior.
- 03_screens defines required screen-level product behavior.
- 04_player_experience defines messaging, feel, and accessibility.
- 05_assets defines art and audio planning.
- 06_balance defines tuning guidance.
- 07_multiplayer defines authoritative live-match behavior.
- 08_progression defines long-term retention systems outside core gameplay.
- 09_edge_cases defines abnormal cases and required handling.
- 10_qa defines test expectations.
- 11_future defines post-MVP opportunities.

Implementation Guidance
- Developers should treat 07_multiplayer and 02_rules as authoritative when conflicts appear.
- If a live service constraint forces a rule adjustment, the documentation should be updated before the product behavior changes.
