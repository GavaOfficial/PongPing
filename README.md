# PongPing

Un gioco Pong moderno scritto in Java con modalità innovative e sistema di achievement.

## 🎮 Gioca Online

**[▶️ Gioca Ora nel Browser](https://gavaofficial.github.io/PongPing)** - Nessuna installazione richiesta!

La versione web include tutte le modalità di gioco:
- Circle Mode con sistema combo e power-ups
- Classic Mode single/multiplayer
- Sistema achievement e progressione completo
- Supporto multilingua

> **Nota**: La versione web è identica alla versione desktop: stessi temi, sfondi, effetti visivi, animazioni e comandi. È scritta in JavaScript/HTML5 (nessun plugin o Java richiesto) e si ricava direttamente dal codice Java, quindi le due versioni restano sempre uguali. Impostazioni, progressi e cronologia vengono salvati nel browser.

## Caratteristiche

### Modalità di Gioco
- **Circle Mode**: Difendi il cerchio centrale da palle che arrivano da tutte le direzioni
  - Sistema combo e spiral frenzy
  - Power-ups (slow-mo, shield, paddle enlarge, health)
  - Difficoltà progressiva basata sul tempo
  - 3 vite e progressione continua
- **Classic Mode**: Pong tradizionale
  - Single Player con AI (5 livelli di difficoltà)
  - Two Players locale
  - Sistema combo e fire ball

### Sistema Progression
- **77+ Achievement** divisi in 5 categorie (First Time, Circle Mode, Classic Mode, Mastery, Special)
- **Sistema XP e Livelli** con tier Bronze/Silver/Gold/Platinum
- **Notifiche retro-arcade** per achievement e level up
- **Statistiche dettagliate** per entrambe le modalità

### Personalizzazione
- Temi personalizzati e sfondi
- Audio e effetti sonori con controllo volume
- Controlli configurabili
- Supporto multilingua (Italiano, English, Español)

## Come giocare

### Controlli Classic Mode
- **Player 1**: W (su), S (giù)
- **Player 2**: Frecce su/giù
- **ESC**: Menu/Esci
- **Spazio**: Pausa

### Controlli Circle Mode
- **Mouse**: Muovi la paddle circolare
- **Click e tieni**: Start game (dal menu Circle)
- **ESC**: Pausa/Menu
- **Ctrl+Shift+Alt+N**: Test notifiche (debug)

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

### Versione web (HTML5 + JavaScript)
La versione per browser è generata dallo stesso codice Java:
- `tools/java2js/` traduce `src/` in JavaScript (`web/js/pongping.js`), mantenendo la semantica di Java che influisce sul risultato a schermo (divisioni intere, cast, formattazione dei numeri, ecc.);
- `web/js/runtime/` riproduce nel browser le parti di Java usate dal gioco (Java2D su canvas, font letti dai file TTF, eventi di tastiera e mouse, timer, audio, salvataggi);
- `src-web/Main.java` è il punto di ingresso della versione web.

```bash
# Genera il sito in dist-web/ (richiede Java 17+ e Maven)
tools/build-web.sh

# Provalo in locale
python3 -m http.server -d dist-web 8000   # poi apri http://localhost:8000
```

Dopo ogni modifica al codice Java basta rilanciare `tools/build-web.sh`: la pubblicazione su GitHub Pages avviene automaticamente a ogni push su `main`.

### Download installer
Scarica l'installer per il tuo sistema dalla sezione [Releases](../../releases/latest):
- Windows: `.exe`
- macOS: `.dmg` 
- Linux: `.deb`

## Struttura progetto

```
PongPing/
├── src/                     # Codice sorgente modularizzato (desktop e web)
│   ├── Main.java           # Punto di ingresso applicazione
│   ├── game/               # Logica di gioco
│   │   ├── PongGame.java   # Classe principale del gioco (15000+ linee)
│   │   ├── GameState.java  # Stati del gioco
│   │   └── Particle.java   # Sistema particelle
│   ├── advancement/        # Sistema achievement e progressione
│   │   ├── Achievement.java        # Classe achievement
│   │   ├── AchievementRegistry.java # 77+ achievements
│   │   └── PlayerProgress.java     # XP, livelli, statistiche
│   ├── context/            # Gestione contesti
│   │   ├── GameContext.java      # Stato del gioco
│   │   ├── FontContext.java      # Gestione font
│   │   ├── SettingsContext.java  # Configurazioni
│   │   ├── LanguageContext.java  # Localizzazione
│   │   ├── AIContext.java        # IA
│   │   ├── AnimationContext.java # Animazioni e transizioni
│   │   └── ContextLoader.java    # Caricamento risorse
│   └── settings/           # Sistema impostazioni
│       ├── GeneralSettings.java  # Impostazioni generali
│       ├── MusicSettings.java    # Audio
│       ├── LanguageSettings.java # Lingue
│       └── HistorySettings.java  # Cronologia partite
├── src-web/Main.java       # Punto di ingresso della versione web
├── web/                    # Versione web: pagina, runtime JavaScript e gioco tradotto
├── tools/                  # build-web.sh e traduttore Java -> JavaScript (java2js)
├── font/                   # Font personalizzati
├── temi/                   # Temi e sfondi
│   ├── GameBack/          # Sfondi di gioco
│   └── Padle/             # Temi paddle
├── lingue/                # File di localizzazione (IT/EN/ES)
├── music/                 # File audio (opzionale)
└── icon.*                 # Icone applicazione
```

## Requisiti

- Java 21 o superiore (per sviluppo)
- Versione web: un browser moderno (Chrome, Edge, Firefox, Safari)
- Gli installer includono tutto il necessario

## Licenza

MIT License - vedi [LICENSE](LICENSE)