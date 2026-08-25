# Game Logic Flowchart

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
    
    %% Input handlers (Keyboard, Click, Touch) channelled to a unified input handler
    H -->|Physical Keyboard Input| INP[Unified Input Handler: handleInput]
    H -->|Mouse Click Tile| INP
    H -->|Touch Screen Tap| INP
    
    INP --> I[Append Letter to Active Input]
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
    
    ERR1 & ERR2 & ERR3 & ERR4 & ERR5 --> Q[Trigger Input Shake & Show Error Toast]
    Q --> H
    
    P --> R[Add to Found Words Set & Check if Mielegramma]
    R --> S[Update Score & LocalStorage]
    S --> T[Clear Input Field]
    
    T --> U{All Target Words Found?}
    U -->|No| H
    U -->|Yes| V[Display End Game Summary Screen / Modal]

    %% Midnight rollover, independent of gameplay loop
    H -.->|App Regains Foreground: visibilitychange| MC{Current Local Date<br/>!= Loaded Puzzle Date?}
    MC -->|No| H
    MC -->|Yes| MT[Toast: New Day Started] --> A