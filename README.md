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
javac PongGame.java
java PongGame
```

### Download installer
Scarica l'installer per il tuo sistema dalla sezione [Releases](../../releases/latest):
- Windows: `.exe`
- macOS: `.dmg` 
- Linux: `.deb`

## Struttura progetto

```
PongPing/
├── PongGame.java    # Codice principale
├── font/           # Font personalizzati
├── temi/           # Temi e sfondi
├── music/          # File audio
└── icon.*          # Icone applicazione
```

## Requisiti

- Java 21 o superiore (per sviluppo)
- Gli installer includono tutto il necessario

## Licenza

MIT License - vedi [LICENSE](LICENSE)