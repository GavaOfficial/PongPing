// PongPing web - startup: preloads the game's resources (what the desktop JAR contains),
// then runs the translated Java entry point (src-web/Main.java) with a browser game loop.
'use strict';

(async function () {
    const status = document.getElementById('loading-status');
    const setStatus = (t) => { if (status) status.textContent = t; };

    // ------------------------------------------------------------ resources
    const manifest = await (await fetch('assets.json', { cache: 'no-cache' })).json();
    const res = J.$resources;
    let done = 0;
    const total = manifest.files.length;
    await Promise.all(manifest.files.map(async (path) => {
        const response = await fetch(encodeURI(path));
        if (!response.ok) throw new Error('Cannot load ' + path);
        const lower = path.toLowerCase();
        if (/\.(png|jpe?g|gif|bmp)$/.test(lower)) {
            const blob = await response.blob();
            const img = new Image();
            img.src = URL.createObjectURL(blob);
            await img.decode();
            res.images.set(path, new J.BufferedImage(img));
            res.binary.set(path, new ArrayBuffer(0));
        } else if (/\.(ttf|otf)$/.test(lower)) {
            const buf = await response.arrayBuffer();
            const ttf = J.$parseTTF(buf);
            const name = path.replace(/^.*\//, '').replace(/\.[^.]+$/, '');
            res.fonts.set(path, { name, ttf });
            res.binary.set(path, buf);
        } else if (/\.(wav|mp3|ogg)$/.test(lower)) {
            res.binary.set(path, await response.arrayBuffer());
        } else {
            res.text.set(path, await response.text());
        }
        done++;
        setStatus(Math.round(done / total * 100) + '%');
    }));

    // ------------------------------------------------------------ game
    for (const cls of PongPing.$classes) J.$userClasses[cls.name] = cls;

    // The desktop game runs its logic on a thread with a fixed 60 Hz timestep and repaints
    // continuously. In the browser the same loop runs on requestAnimationFrame.
    const PongGame = PongPing.PongGame;
    PongGame.prototype.startGameLoop = function () {
        this.gameRunning = true;
        const stepMs = 1000 / 60;
        let last = performance.now();
        let accumulator = 0;
        const frame = (now) => {
            if (!this.gameRunning) return;
            accumulator += now - last;
            last = now;
            // after a pause (hidden tab) do not replay minutes of game time at once
            if (accumulator > 250) accumulator = 250;
            while (accumulator >= stepMs) {
                try { this.updateGameLogic(); } catch (e) { console.error(e); }
                accumulator -= stepMs;
            }
            this.$paintFrame();
            requestAnimationFrame(frame);
        };
        requestAnimationFrame(frame);
    };
    const originalStop = PongGame.prototype.stopGameLoop;
    PongGame.prototype.stopGameLoop = function () {
        this.saveSettingsToFile();
        this.gameRunning = false;
    };
    void originalStop;

    PongPing.$clinit();
    document.getElementById('loading').remove();
    PongPing.Main.main([]);
})().catch((e) => {
    console.error(e);
    const s = document.getElementById('loading-status');
    if (s) s.textContent = 'Errore: ' + e.message;
});
