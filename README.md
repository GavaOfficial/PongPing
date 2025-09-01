# PongPing

Un gioco Pong moderno scritto in Java.

## Caratteristiche

- Gioco per uno o due giocatori
- AI con 5 livelli di difficoltà
- Temi personalizzati e sfondi
- Audio e effetti sonori
- Controlli configurabili

## Come giocare

### Controlli
- **Player 1**: W (su), S (giù)
- **Player 2**: Frecce su/giù
- **ESC**: Menu
- **Spazio**: Pausa

### Modalità
- **Single Player**: Gioca contro AI
- **Two Players**: Gioco locale

## Installazione

### Da codice sorgente
```bash
# Compila tutti i file Java della struttura modulare
find src -name "*.java" -exec javac -d . {} +

# Esegui l'applicazione
java Main
```

### Creazione JAR
```bash
# Compila il codice
find src -name "*.java" -exec javac -d . {} +

# Crea JAR eseguibile
mkdir -p dist
jar cfe dist/PongGame.jar Main $(find . -name "*.class" -type f) font temi icon.png lingue

# Esegui JAR
java -jar dist/PongGame.jar
```

### Download installer
Scarica l'installer per il tuo sistema dalla sezione [Releases](../../releases/latest):
- Windows: `.exe`
- macOS: `.dmg` 
- Linux: `.deb`

## Struttura progetto

```
PongPing/
├── src/                     # Codice sorgente modularizzato
│   ├── Main.java           # Punto di ingresso applicazione
│   ├── game/               # Logica di gioco
│   │   ├── PongGame.java   # Classe principale del gioco
│   │   ├── GameState.java  # Stati del gioco
│   │   └── Particle.java   # Sistema particelle
│   ├── context/            # Gestione contesti
│   │   ├── GameContext.java      # Stato del gioco
│   │   ├── FontContext.java      # Gestione font
│   │   ├── SettingsContext.java  # Configurazioni
│   │   ├── LanguageContext.java  # Localizzazione
│   │   ├── AIContext.java        # IA
│   │   └── ContextLoader.java    # Caricamento risorse
│   └── settings/           # Sistema impostazioni
│       ├── GeneralSettings.java  # Impostazioni generali
│       ├── MusicSettings.java    # Audio
│       └── LanguageSettings.java # Lingue
├── font/                   # Font personalizzati
├── temi/                   # Temi e sfondi
│   ├── GameBack/          # Sfondi di gioco
│   └── Padle/             # Temi paddle
├── lingue/                # File di localizzazione
├── music/                 # File audio (opzionale)
└── icon.*                 # Icone applicazione
```

## Requisiti

- Java 21 o superiore (per sviluppo)
- Gli installer includono tutto il necessario

## Licenza

MIT License - vedi [LICENSE](LICENSE)