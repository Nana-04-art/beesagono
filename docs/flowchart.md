# Game Logic Flowchart

> Updated to reflect: the Candidate Pangrams puzzle-generation strategy, explicit loading/error/retry states, invalid-word tracking, and career/streak statistics recording. See `requirements.md` and `data-models.md` for full detail.

```mermaid
graph TD
    A[Start Game / Get Current Date YYYY-MM-DD] --> A2[Seed PRNG from Date String]
    A2 --> B[Generate Candidate 7-Letter Set]
    B --> C[Extract Target Words Set & Mielegrammi Set per Candidate Center Letter]
    C --> QG{Quality Gate:<br/>Target Words >= 15<br/>AND Mielegrammi >= 1?}
    QG -->|No, retry next seed| B
    QG -->|Yes| D{Saved State for Today in LocalStorage?}
    D -->|Yes| DR{LocalStorage Read OK?}
    DR -->|Yes| E[Restore Found Words & Recompute Score/Mielegrammi]
    DR -->|No, fallback| F
    D -->|No| F[Initialize Fresh Daily Game State]
    
    E --> G[Render 7-Hexagon Honeycomb Grid]
    F --> G
    
    G --> H[Wait for User Input]

    %% Input handlers channelled to a unified input handler
    H -->|Physical Keyboard Letter| INP[GameService.handleInput]
    H -->|Mouse Click / Touch Tap Tile| INP
    INP --> I[Append Letter to currentInput]
    I --> H

    H -->|Click Shuffle| J[Reorder 6 Outer SVG Hexagons]
    J --> H

    H -->|Press Enter / Click Submit| K{Length >= 4?}

    K -->|No| ERR1[Toast: Too Short]
    K -->|Yes| L{Contains Center Letter?}

    L -->|No| ERR2[Toast: Missing Center Letter]
    L -->|Yes| M{Uses Only Allowed 7 Letters?}

    M -->|No| ERR3[Toast: Invalid Letters]
    M -->|Yes| N{Already Found?}

    N -->|Yes| ERR4[Toast: Already Found]
    N -->|No| O{In Today's Target Words Set?}

    O -->|No| ERR5[Toast: Not in Word List]
    O -->|Yes| P[Calculate Points & Check Mielegramma +7 Bonus]

    ERR1 & ERR2 & ERR3 & ERR5 --> TRACK[Add word to invalidWords] --> Q[Trigger Input Shake & Show Error Toast]
    ERR4 --> Q
    Q --> H

    P --> R[Add to Found Words Set & Check if Mielegramma]
    R --> S[Update Score & LocalStorage,<br/>StatsService.recordProgress]
    S --> T[Clear Input Field]
    
    T --> U{All Target Words Found?}
    U -->|No| H
    U -->|Yes| V[Display End Game Summary Modal /<br/>Share Score: clipboard -> Web Share -> execCommand]

    %% Midnight rollover, independent of gameplay loop
    H -.->|window focus / visibilitychange, tab visible| MC{Current Local Date<br/>!= Loaded Puzzle Date?}
    MC0 -.-> MC
    MC -->|No| H
    MC -->|Yes| MT[Toast: New Day Started] --> LG
```