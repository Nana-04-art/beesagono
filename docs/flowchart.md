# Game Logic Flowchart

```mermaid
graph TD
    A[Start Game / Load Daily Puzzle] --> B{Saved State in LocalStorage?}
    B -->|Yes| C[Restore Score & Found Words]
    B -->|No| D[Initialize Empty Game State]
    
    C --> E[Render 7-Hexagon Honeycomb Grid]
    D --> E
    
    E --> F[Wait for User Input]
    
    F -->|Physical Keyboard / Hexagon Click| G[Append Letter to Active Input]
    G --> F
    
    F -->|Click Shuffle| H[Reorder 6 Outer SVG Hexagons]
    H --> F
    
    F -->|Press Enter / Click Submit| I{Length >= 4?}
    
    I -->|No| ERR1[Toast: Too Short]
    I -->|Yes| J{Contains Center Letter?}
    
    J -->|No| ERR2[Toast: Missing Center Letter]
    J -->|Yes| K{Uses Only Allowed 7 Letters?}
    
    K -->|No| ERR3[Toast: Invalid Letters]
    K -->|Yes| L{Already Found?}
    
    L -->|Yes| ERR4[Toast: Already Found]
    L -->|No| M{In Dictionary?}
    
    M -->|No| ERR5[Toast: Not in Dictionary]
    M -->|Yes| N[Calculate Score & Check Mielegramma]
    
    ERR1 & ERR2 & ERR3 & ERR4 & ERR5 --> O[Trigger Input Shake & Show Error Toast]
    O --> F
    
    N --> P[Add to Found Words List]
    P --> Q[Update Score & LocalStorage]
    Q --> R[Clear Input Field]
    R --> F