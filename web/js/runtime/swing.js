// PongPing web runtime - Swing/AWT components, input events, timers and sound.
// The browser page plays the role of the (maximized, undecorated) JFrame of the web build.
'use strict';
(function () {

// ---------------------------------------------------------------- events

const KE = {
    VK_ENTER: 10, VK_BACK_SPACE: 8, VK_TAB: 9, VK_CANCEL: 3, VK_CLEAR: 12, VK_SHIFT: 16, VK_CONTROL: 17, VK_ALT: 18,
    VK_PAUSE: 19, VK_CAPS_LOCK: 20, VK_ESCAPE: 27, VK_SPACE: 32, VK_PAGE_UP: 33, VK_PAGE_DOWN: 34, VK_END: 35,
    VK_HOME: 36, VK_LEFT: 37, VK_UP: 38, VK_RIGHT: 39, VK_DOWN: 40, VK_COMMA: 44, VK_MINUS: 45, VK_PERIOD: 46,
    VK_SLASH: 47, VK_SEMICOLON: 59, VK_EQUALS: 61, VK_OPEN_BRACKET: 91, VK_BACK_SLASH: 92, VK_CLOSE_BRACKET: 93,
    VK_NUMPAD0: 96, VK_MULTIPLY: 106, VK_ADD: 107, VK_SEPARATOR: 108, VK_SUBTRACT: 109, VK_DECIMAL: 110, VK_DIVIDE: 111,
    VK_DELETE: 127, VK_NUM_LOCK: 144, VK_SCROLL_LOCK: 145, VK_F1: 112, VK_PRINTSCREEN: 154, VK_INSERT: 155,
    VK_META: 157, VK_BACK_QUOTE: 192, VK_QUOTE: 222, VK_PLUS: 521, VK_WINDOWS: 524, VK_CONTEXT_MENU: 525,
    VK_UNDEFINED: 0, CHAR_UNDEFINED: '￿',
    KEY_PRESSED: 401, KEY_RELEASED: 402, KEY_TYPED: 400,
};
for (let i = 0; i < 26; i++) KE['VK_' + String.fromCharCode(65 + i)] = 65 + i;
for (let i = 0; i < 10; i++) { KE['VK_' + i] = 48 + i; KE['VK_NUMPAD' + i] = 96 + i; }
for (let i = 1; i <= 24; i++) KE['VK_F' + i] = i <= 12 ? 111 + i : 61440 + i - 13;

const CODE_TO_VK = {
    Enter: 10, NumpadEnter: 10, Backspace: 8, Tab: 9, ShiftLeft: 16, ShiftRight: 16, ControlLeft: 17, ControlRight: 17,
    AltLeft: 18, AltRight: 18, Pause: 19, CapsLock: 20, Escape: 27, Space: 32, PageUp: 33, PageDown: 34, End: 35,
    Home: 36, ArrowLeft: 37, ArrowUp: 38, ArrowRight: 39, ArrowDown: 40, Comma: 44, Minus: 45, Period: 46, Slash: 47,
    Semicolon: 59, Equal: 61, BracketLeft: 91, Backslash: 92, BracketRight: 93, NumpadMultiply: 106, NumpadAdd: 107,
    NumpadSubtract: 109, NumpadDecimal: 110, NumpadDivide: 111, Delete: 127, NumLock: 144, ScrollLock: 145,
    PrintScreen: 154, Insert: 155, MetaLeft: 157, MetaRight: 157, Backquote: 192, Quote: 222, ContextMenu: 525,
    IntlBackslash: 153,
};
function vkFromDom(e) {
    if (/^Key[A-Z]$/.test(e.code)) {
        // letters follow the keyboard layout, like Java's virtual key codes
        if (e.key && e.key.length === 1 && /[a-z]/i.test(e.key)) return e.key.toUpperCase().charCodeAt(0);
        return e.code.charCodeAt(3);
    }
    if (/^Digit\d$/.test(e.code)) return 48 + Number(e.code[5]);
    if (/^Numpad\d$/.test(e.code)) return 96 + Number(e.code[6]);
    const f = /^F(\d+)$/.exec(e.code);
    if (f) { const n = Number(f[1]); return n <= 12 ? 111 + n : 61440 + n - 13; }
    if (e.code in CODE_TO_VK) return CODE_TO_VK[e.code];
    if (e.key === '+') return KE.VK_PLUS;
    return 0;
}
const KEY_TEXT = {
    10: 'Enter', 8: 'Backspace', 9: 'Tab', 3: 'Cancel', 12: 'Clear', 16: 'Shift', 17: 'Ctrl', 18: 'Alt', 19: 'Pause',
    20: 'Caps Lock', 27: 'Escape', 32: 'Space', 33: 'Page Up', 34: 'Page Down', 35: 'End', 36: 'Home', 37: 'Left',
    38: 'Up', 39: 'Right', 40: 'Down', 44: 'Comma', 45: 'Minus', 46: 'Period', 47: 'Slash', 59: 'Semicolon',
    61: 'Equals', 91: 'Open Bracket', 92: 'Back Slash', 93: 'Close Bracket', 106: 'NumPad *', 107: 'NumPad +',
    108: 'NumPad ,', 109: 'NumPad -', 110: 'NumPad .', 111: 'NumPad /', 127: 'Delete', 144: 'Num Lock',
    145: 'Scroll Lock', 154: 'Print Screen', 155: 'Insert', 157: 'Meta', 192: 'Back Quote', 222: 'Quote',
    521: 'Plus', 524: 'Windows', 525: 'Context Menu', 153: 'Less',
};
class InputEvent {
    constructor(source, dom) { this.source = source; this.$dom = dom; this.consumed = false; this.when = Date.now(); }
    getSource() { return this.source; }
    getComponent() { return this.source; }
    isShiftDown() { return !!(this.$dom && this.$dom.shiftKey); }
    isControlDown() { return !!(this.$dom && this.$dom.ctrlKey); }
    isAltDown() { return !!(this.$dom && this.$dom.altKey); }
    isMetaDown() { return !!(this.$dom && this.$dom.metaKey); }
    getModifiers() { return (this.isShiftDown() ? 1 : 0) | (this.isControlDown() ? 2 : 0) | (this.isMetaDown() ? 4 : 0) | (this.isAltDown() ? 8 : 0); }
    getModifiersEx() { return (this.isShiftDown() ? 64 : 0) | (this.isControlDown() ? 128 : 0) | (this.isMetaDown() ? 256 : 0) | (this.isAltDown() ? 512 : 0) | (this.$buttonsEx || 0); }
    getWhen() { return this.when; }
    consume() { this.consumed = true; }
    isConsumed() { return this.consumed; }
}
InputEvent.SHIFT_DOWN_MASK = 64; InputEvent.CTRL_DOWN_MASK = 128; InputEvent.META_DOWN_MASK = 256; InputEvent.ALT_DOWN_MASK = 512;
InputEvent.BUTTON1_DOWN_MASK = 1024; InputEvent.BUTTON2_DOWN_MASK = 2048; InputEvent.BUTTON3_DOWN_MASK = 4096;
InputEvent.SHIFT_MASK = 1; InputEvent.CTRL_MASK = 2; InputEvent.META_MASK = 4; InputEvent.ALT_MASK = 8; InputEvent.BUTTON1_MASK = 16;
J.InputEvent = InputEvent;

class KeyEvent extends InputEvent {
    constructor(source, dom, id, code, ch) { super(source, dom); this.id = id; this.keyCode = code; this.keyChar = ch; }
    getKeyCode() { return this.keyCode; }
    getKeyChar() { return this.keyChar; }
    getID() { return this.id; }
    getExtendedKeyCode() { return this.keyCode; }
    getKeyLocation() { return 1; }
    setKeyCode(c) { this.keyCode = c; }
    static getKeyText(code) {
        if ((code >= 48 && code <= 57) || (code >= 65 && code <= 90)) return String.fromCharCode(code);
        if (code >= 96 && code <= 105) return 'NumPad-' + (code - 96);
        if (code >= 112 && code <= 123) return 'F' + (code - 111);
        if (code in KEY_TEXT) return KEY_TEXT[code];
        return 'Unknown keyCode: 0x' + (code >>> 0).toString(16);
    }
}
Object.assign(KeyEvent, KE);
J.KeyEvent = KeyEvent;

class MouseEvent extends InputEvent {
    constructor(source, dom, id, x, y, button, clickCount) {
        super(source, dom);
        this.id = id; this.x = x; this.y = y; this.button = button; this.clickCount = clickCount;
    }
    getX() { return this.x; }
    getY() { return this.y; }
    getPoint() { return new J.Point(this.x, this.y); }
    getXOnScreen() { return this.x + (this.source ? this.source.$screenX() : 0); }
    getYOnScreen() { return this.y + (this.source ? this.source.$screenY() : 0); }
    getLocationOnScreen() { return new J.Point(this.getXOnScreen(), this.getYOnScreen()); }
    getButton() { return this.button; }
    getClickCount() { return this.clickCount; }
    getID() { return this.id; }
    isPopupTrigger() { return this.button === 3 && this.id === MouseEvent.MOUSE_PRESSED; }
}
Object.assign(MouseEvent, { NOBUTTON: 0, BUTTON1: 1, BUTTON2: 2, BUTTON3: 3, MOUSE_CLICKED: 500, MOUSE_PRESSED: 501, MOUSE_RELEASED: 502, MOUSE_MOVED: 503, MOUSE_ENTERED: 504, MOUSE_EXITED: 505, MOUSE_DRAGGED: 506, MOUSE_WHEEL: 507 });
J.MouseEvent = MouseEvent;

class MouseWheelEvent extends MouseEvent {
    constructor(source, dom, x, y, rotation, precise) { super(source, dom, 507, x, y, 0, 0); this.rotation = rotation; this.precise = precise; }
    getWheelRotation() { return this.rotation; }
    getPreciseWheelRotation() { return this.precise; }
    getScrollAmount() { return 3; }
    getScrollType() { return 0; }
    getUnitsToScroll() { return this.rotation * 3; }
}
J.MouseWheelEvent = MouseWheelEvent;

class ActionEvent { constructor(source) { this.source = source; } getSource() { return this.source; } getWhen() { return Date.now(); } }
J.ActionEvent = ActionEvent;
class ComponentEvent { constructor(source) { this.source = source; } getComponent() { return this.source; } getSource() { return this.source; } }
J.ComponentEvent = ComponentEvent;
class WindowEvent extends ComponentEvent {
    constructor(source, oldState, newState) { super(source); this.oldState = oldState || 0; this.newState = newState || 0; }
    getWindow() { return this.source; }
    getOldState() { return this.oldState; }
    getNewState() { return this.newState; }
}
J.WindowEvent = WindowEvent;

// Listener adapters (anonymous subclasses are plain objects created by J.$impl).
J.ComponentAdapter = function ComponentAdapter() {};
J.WindowAdapter = function WindowAdapter() {};
J.KeyAdapter = function KeyAdapter() {};
J.MouseAdapter = function MouseAdapter() {};
J.ActionListener = function ActionListener() {};
J.WindowStateListener = function WindowStateListener() {};
J.WindowListener = function WindowListener() {};
J.Runnable = function Runnable() {};
J.Comparator = function Comparator() {};

function call(listener, method, ev) {
    if (!listener) return;
    if (typeof listener === 'function') listener(ev);
    else if (typeof listener[method] === 'function') listener[method](ev);
}

// ---------------------------------------------------------------- timers

class Timer {
    constructor(delay, listener) {
        this.delay = delay;
        this.initialDelay = delay;
        this.listeners = listener ? [listener] : [];
        this.repeats = true;
        this.handle = null;
        this.coalesce = true;
    }
    addActionListener(l) { this.listeners.push(l); }
    removeActionListener(l) { this.listeners = this.listeners.filter(x => x !== l); }
    setRepeats(r) { this.repeats = r; }
    isRepeats() { return this.repeats; }
    setDelay(d) { this.delay = d; }
    getDelay() { return this.delay; }
    setInitialDelay(d) { this.initialDelay = d; }
    setCoalesce(c) { this.coalesce = c; }
    isRunning() { return this.handle !== null; }
    start() {
        if (this.handle !== null) return;
        this.$schedule(this.initialDelay);
    }
    $schedule(delay) {
        this.handle = setTimeout(() => {
            if (this.handle === null) return;
            if (this.repeats) this.$schedule(this.delay); else this.handle = null;
            const ev = new ActionEvent(this);
            for (const l of this.listeners.slice()) {
                try { call(l, 'actionPerformed', ev); } catch (e) { console.error(e); }
            }
        }, Math.max(0, delay));
    }
    stop() { if (this.handle !== null) clearTimeout(this.handle); this.handle = null; }
    restart() { this.stop(); this.start(); }
}
J.Timer = Timer;

// ---------------------------------------------------------------- components

class Component {
    constructor() {
        this.$listeners = { key: [], mouse: [], motion: [], wheel: [], component: [], focus: [] };
        this.$background = null;
        this.$foreground = J.Color.BLACK;
        this.$font = new J.Font('Dialog', 0, 12);
        this.$width = 0; this.$height = 0;
        this.$parent = null;
        this.$visible = true;
        this.$preferred = null;
        this.$cursor = null;
    }
    addKeyListener(l) { this.$listeners.key.push(l); }
    addMouseListener(l) { this.$listeners.mouse.push(l); }
    addMouseMotionListener(l) { this.$listeners.motion.push(l); }
    addMouseWheelListener(l) { this.$listeners.wheel.push(l); }
    addComponentListener(l) { this.$listeners.component.push(l); }
    addFocusListener(l) { this.$listeners.focus.push(l); }
    removeKeyListener(l) { this.$listeners.key = this.$listeners.key.filter(x => x !== l); }
    removeMouseListener(l) { this.$listeners.mouse = this.$listeners.mouse.filter(x => x !== l); }
    removeMouseMotionListener(l) { this.$listeners.motion = this.$listeners.motion.filter(x => x !== l); }
    removeMouseWheelListener(l) { this.$listeners.wheel = this.$listeners.wheel.filter(x => x !== l); }
    removeComponentListener(l) { this.$listeners.component = this.$listeners.component.filter(x => x !== l); }
    getKeyListeners() { return this.$listeners.key.slice(); }
    getMouseListeners() { return this.$listeners.mouse.slice(); }
    getMouseMotionListeners() { return this.$listeners.motion.slice(); }
    getComponentListeners() { return this.$listeners.component.slice(); }
    setBackground(c) { this.$background = c; }
    getBackground() { return this.$background; }
    setForeground(c) { this.$foreground = c; }
    getForeground() { return this.$foreground; }
    setFont(f) { this.$font = f; }
    getFont() { return this.$font; }
    getFontMetrics(f) { return new J.FontMetrics(f); }
    getWidth() { return this.$width; }
    getHeight() { return this.$height; }
    getSize() { return new J.Dimension(this.$width, this.$height); }
    getBounds() { return new J.Rectangle(0, 0, this.$width, this.$height); }
    getX() { return 0; }
    getY() { return 0; }
    getLocation() { return new J.Point(0, 0); }
    getLocationOnScreen() { return new J.Point(this.$screenX(), this.$screenY()); }
    $screenX() { return this.$canvas ? Math.round(this.$canvas.getBoundingClientRect().left + (window.screenX || 0)) : 0; }
    $screenY() { return this.$canvas ? Math.round(this.$canvas.getBoundingClientRect().top + (window.screenY || 0)) : 0; }
    getMousePosition() { return J.$mouse.inside && J.$mouse.target === this ? new J.Point(J.$mouse.x, J.$mouse.y) : null; }
    setPreferredSize(d) { this.$preferred = d; }
    getPreferredSize() { return this.$preferred || new J.Dimension(this.$width, this.$height); }
    setMinimumSize() {}
    setMaximumSize() {}
    setSize(w, h) { if (w instanceof J.Dimension) { h = w.height; w = w.width; } this.$setSize(w, h); }
    setBounds(x, y, w, h) { this.$setSize(w, h); }
    $setSize(w, h) {
        if (w === this.$width && h === this.$height) return;
        this.$width = w; this.$height = h;
        const ev = new ComponentEvent(this);
        for (const l of this.$listeners.component.slice()) call(l, 'componentResized', ev);
    }
    setFocusable() {}
    isFocusable() { return true; }
    setFocusTraversalKeysEnabled() {}
    requestFocus() { if (this.$canvas) this.$canvas.focus({ preventScroll: true }); }
    requestFocusInWindow() { this.requestFocus(); return true; }
    hasFocus() { return !!this.$canvas && document.activeElement === this.$canvas; }
    isFocusOwner() { return this.hasFocus(); }
    setCursor(c) {
        this.$cursor = c;
        if (this.$canvas) this.$canvas.style.cursor = c ? c.$css : 'default';
    }
    getCursor() { return this.$cursor || J.Cursor.getDefaultCursor(); }
    setVisible(v) { this.$visible = v; }
    isVisible() { return this.$visible; }
    isShowing() { return this.$visible; }
    setDoubleBuffered() {}
    setOpaque() {}
    setLayout() {}
    revalidate() {}
    validate() {}
    invalidate() {}
    getParent() { return this.$parent; }
    getTopLevelAncestor() { let c = this; while (c.$parent) c = c.$parent; return c; }
    repaint() { this.$dirty = true; }
    paintImmediately() { this.$dirty = true; }
    getGraphics() {
        const c = document.createElement('canvas');
        c.width = 1; c.height = 1;
        return new J.Graphics2D(c.getContext('2d'), 1, 1, 1, this);
    }
    dispatchEvent() {}
}
J.Component = Component;
J.Container = Component;

/** The game panel: a canvas that fills the page, double buffered like Swing. */
class JPanel extends Component {
    constructor() {
        super();
        this.$background = new J.Color(238, 238, 238);
        this.$canvas = document.createElement('canvas');
        this.$canvas.tabIndex = 0;
        this.$canvas.className = 'java-panel';
        this.$buffer = document.createElement('canvas');
        this.$dirty = true;
    }
    add() {}
    paint(g) { this.paintComponent(g); }
    paintComponent(g) {
        if (this.$background) {
            g.setColor(this.$background);
            g.fillRect(0, 0, this.$width, this.$height);
        }
    }
    /** Renders one frame into the back buffer and shows it (only if painting completed). */
    $paintFrame() {
        const dpr = (window.devicePixelRatio || 1) * (this.$cssScale || 1);
        const w = this.$width, h = this.$height;
        const pw = Math.max(1, Math.round(w * dpr)), ph = Math.max(1, Math.round(h * dpr));
        const buf = this.$buffer, vis = this.$canvas;
        if (buf.width !== pw || buf.height !== ph) { buf.width = pw; buf.height = ph; }
        if (vis.width !== pw || vis.height !== ph) { vis.width = pw; vis.height = ph; }
        const ctx = buf.getContext('2d');
        if (ctx.reset) ctx.reset(); else { buf.width = pw; }
        ctx.$owner = null;
        const g = new J.Graphics2D(ctx, pw / Math.max(1, w), w, h, this);
        g.$clip = { list: [{ rect: [0, 0, pw, ph] }] };
        g.$clipVersion++;
        g.$font = this.$font;
        g.$paint = this.$foreground;
        try {
            this.paint(g);
        } catch (e) {
            // Swing reports exceptions thrown while painting and keeps the previous frame.
            console.error('Exception while painting:', e);
            return;
        }
        const vctx = vis.getContext('2d');
        vctx.setTransform(1, 0, 0, 1, 0, 0);
        vctx.globalAlpha = 1;
        vctx.globalCompositeOperation = 'copy';
        vctx.drawImage(buf, 0, 0);
        vctx.globalCompositeOperation = 'source-over';
        this.$dirty = false;
    }
}
J.JPanel = JPanel;
J.JComponent = JPanel;
J.Canvas = JPanel;

class JFrame extends Component {
    constructor(title) {
        super();
        this.title = title || '';
        this.$content = null;
        this.$state = 0;
        this.$windowStateListeners = [];
        this.$windowListeners = [];
        if (title) document.title = title;
    }
    add(c) {
        this.$content = c;
        c.$parent = this;
        J.$mountPanel(c, this);
        return c;
    }
    getContentPane() { return this; }
    setTitle(t) { this.title = t; document.title = t; }
    getTitle() { return this.title; }
    setDefaultCloseOperation() {}
    setUndecorated() {}
    isUndecorated() { return true; }
    setResizable() {}
    setExtendedState(s) {
        const old = this.$state;
        this.$state = s;
        const ev = new WindowEvent(this, old, s);
        for (const l of this.$windowStateListeners.slice()) call(l, 'windowStateChanged', ev);
    }
    getExtendedState() { return this.$state; }
    addWindowStateListener(l) { this.$windowStateListeners.push(l); }
    addWindowListener(l) { this.$windowListeners.push(l); }
    removeWindowListener(l) { this.$windowListeners = this.$windowListeners.filter(x => x !== l); }
    getWindowListeners() { return this.$windowListeners.slice(); }
    addWindowFocusListener(l) { this.$windowListeners.push(l); }
    setMinimumSize(d) { this.$minSize = d; J.$layoutPanel(); }
    getMinimumSize() { return this.$minSize || new J.Dimension(0, 0); }
    setIconImage() {}
    setIconImages() {}
    setLocationRelativeTo() {}
    setLocation() {}
    pack() {}
    dispose() {}
    toFront() {}
    setAlwaysOnTop() {}
    isActive() { return document.hasFocus(); }
    isFocused() { return document.hasFocus(); }
    setVisible(v) {
        this.$visible = v;
        if (v && this.$content) J.$layoutPanel();
    }
    // The page is the window: its size is the viewport.
    getSize() { return new J.Dimension(window.innerWidth, window.innerHeight); }
    getWidth() { return window.innerWidth; }
    getHeight() { return window.innerHeight; }
    setSize() {}
    getLocationOnScreen() { return new J.Point(window.screenX || 0, window.screenY || 0); }
}
JFrame.EXIT_ON_CLOSE = 3; JFrame.DISPOSE_ON_CLOSE = 2; JFrame.DO_NOTHING_ON_CLOSE = 0; JFrame.HIDE_ON_CLOSE = 1;
JFrame.NORMAL = 0; JFrame.ICONIFIED = 1; JFrame.MAXIMIZED_HORIZ = 2; JFrame.MAXIMIZED_VERT = 4; JFrame.MAXIMIZED_BOTH = 6;
J.JFrame = JFrame;
J.Frame = JFrame;
J.Window = JFrame;

J.SwingUtilities = {
    invokeLater(r) { setTimeout(() => call(r, 'run'), 0); },
    invokeAndWait(r) { call(r, 'run'); },
    isEventDispatchThread: () => true,
    getWindowAncestor(c) { while (c && !(c instanceof JFrame)) c = c.$parent; return c || null; },
    getRoot(c) { return J.SwingUtilities.getWindowAncestor(c); },
    isLeftMouseButton: (e) => e.button === 1 || (e.$buttonsEx & 1024) !== 0,
    isRightMouseButton: (e) => e.button === 3 || (e.$buttonsEx & 4096) !== 0,
    isMiddleMouseButton: (e) => e.button === 2 || (e.$buttonsEx & 2048) !== 0,
    convertPointFromScreen(p, c) { p.x -= c.$screenX(); p.y -= c.$screenY(); },
    convertPointToScreen(p, c) { p.x += c.$screenX(); p.y += c.$screenY(); },
};

class Cursor {
    constructor(type, css) { this.type = type || 0; this.$css = css || CURSOR_CSS[this.type] || 'default'; }
    getType() { return this.type; }
    static getDefaultCursor() { return Cursor.$default; }
    static getPredefinedCursor(t) { return new Cursor(t); }
}
const CURSOR_CSS = { 0: 'default', 1: 'crosshair', 2: 'text', 3: 'wait', 4: 'sw-resize', 5: 'se-resize', 6: 'nw-resize', 7: 'ne-resize', 8: 'n-resize', 9: 's-resize', 10: 'w-resize', 11: 'e-resize', 12: 'pointer', 13: 'move' };
Object.assign(Cursor, { DEFAULT_CURSOR: 0, CROSSHAIR_CURSOR: 1, TEXT_CURSOR: 2, WAIT_CURSOR: 3, HAND_CURSOR: 12, MOVE_CURSOR: 13, CUSTOM_CURSOR: -1 });
Cursor.$default = new Cursor(0);
J.Cursor = Cursor;

J.Toolkit = {
    getDefaultToolkit() { return J.Toolkit.$instance; },
    $instance: {
        getScreenSize: () => new J.Dimension(window.innerWidth, window.innerHeight),
        getScreenResolution: () => 96,
        createCustomCursor(img) {
            // The game uses a fully transparent cursor image to hide the pointer.
            let transparent = true;
            try {
                const d = img.$data();
                for (let i = 3; i < d.length; i += 4) if (d[i] !== 0) { transparent = false; break; }
            } catch (e) { /* ignore */ }
            return new Cursor(-1, transparent ? 'none' : 'default');
        },
        getImage: () => null,
        sync() {},
        beep() {},
    },
};

J.GraphicsEnvironment = {
    getLocalGraphicsEnvironment() { return J.GraphicsEnvironment.$instance; },
    $instance: {
        registerFont: () => true,
        getDefaultScreenDevice: () => J.GraphicsEnvironment.$device,
        getScreenDevices: () => [J.GraphicsEnvironment.$device],
        getAvailableFontFamilyNames: () => ['Dialog', 'Monospaced', 'SansSerif', 'Serif'],
        isHeadless: () => false,
    },
    $device: {
        isFullScreenSupported: () => false,
        setFullScreenWindow() {},
        getFullScreenWindow: () => null,
        getDisplayMode: () => ({ getWidth: () => window.screen.width, getHeight: () => window.screen.height, getRefreshRate: () => 60 }),
        getDefaultConfiguration: () => ({ getBounds: () => new J.Rectangle(0, 0, window.innerWidth, window.innerHeight) }),
    },
};

J.$mouse = { x: 0, y: 0, screenX: 0, screenY: 0, inside: false, target: null };
J.MouseInfo = {
    getPointerInfo() {
        const m = J.$mouse;
        return { getLocation: () => new J.Point(m.screenX, m.screenY) };
    },
};

// ---------------------------------------------------------------- mounting and input

let mounted = null;
J.$mountPanel = function (panel, frame) {
    mounted = panel;
    const canvas = panel.$canvas;
    document.body.appendChild(canvas);
    canvas.style.position = 'fixed';
    canvas.style.left = '0';
    canvas.style.top = '0';
    canvas.style.outline = 'none';
    canvas.style.display = 'block';
    installInput(panel);
    window.addEventListener('resize', () => J.$layoutPanel());
    window.addEventListener('pagehide', () => {
        const ev = new WindowEvent(frame);
        for (const l of frame.$windowListeners.slice()) call(l, 'windowClosing', ev);
    });
};
J.$layoutPanel = function () {
    if (!mounted) return;
    const vw = window.innerWidth, vh = window.innerHeight;
    // Like the desktop window, the game never gets smaller than its minimum size:
    // on smaller screens it is scaled down to fit instead.
    const min = mounted.$parent && mounted.$parent.$minSize;
    const w = min ? Math.max(vw, min.width) : vw;
    const h = min ? Math.max(vh, min.height) : vh;
    const scale = Math.min(vw / w, vh / h);
    const style = mounted.$canvas.style;
    style.width = (w * scale) + 'px';
    style.height = (h * scale) + 'px';
    style.left = ((vw - w * scale) / 2) + 'px';
    style.top = ((vh - h * scale) / 2) + 'px';
    mounted.$cssScale = scale;
    mounted.$setSize(w, h);
    mounted.repaint();
};

function installInput(panel) {
    const canvas = panel.$canvas;
    const pos = (e) => {
        const r = canvas.getBoundingClientRect();
        const s = panel.$cssScale || 1;
        return [Math.floor((e.clientX - r.left) / s), Math.floor((e.clientY - r.top) / s)];
    };
    const typedKeys = new Set();
    const fireKey = (dom, id, method) => {
        const code = vkFromDom(dom);
        const ch = dom.key && [...dom.key].length === 1 ? dom.key : (code === 10 ? '\n' : code === 8 ? '\b' : code === 9 ? '\t' : code === 27 ? '\u001b' : KE.CHAR_UNDEFINED);
        const ev = new KeyEvent(panel, dom, id, id === KE.KEY_TYPED ? 0 : code, ch);
        for (const l of panel.$listeners.key.slice()) {
            try { call(l, method, ev); } catch (err) { console.error(err); }
        }
    };
    const onKeyDown = (e) => {
        J.$audio.unlock();
        // Keep the browser from scrolling / moving focus; the game handles these keys.
        if (!(e.ctrlKey || e.metaKey) || ['KeyS', 'KeyP', 'KeyN', 'KeyQ'].includes(e.code)) e.preventDefault();
        fireKey(e, KE.KEY_PRESSED, 'keyPressed');
        if (e.key && ([...e.key].length === 1 || e.key === 'Enter' || e.key === 'Backspace' || e.key === 'Escape' || e.key === 'Tab') && !e.ctrlKey && !e.metaKey) {
            fireKey(e, KE.KEY_TYPED, 'keyTyped');
        }
        typedKeys.add(e.code);
    };
    const onKeyUp = (e) => {
        e.preventDefault();
        typedKeys.delete(e.code);
        fireKey(e, KE.KEY_RELEASED, 'keyReleased');
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    window.addEventListener('blur', () => {
        // release keys that are still held when the page loses focus
        for (const code of typedKeys) fireKey({ code, key: '' }, KE.KEY_RELEASED, 'keyReleased');
        typedKeys.clear();
    });

    let pressX = 0, pressY = 0, pressButton = 0, moved = false, buttons = 0, lastClickTime = 0, clickCount = 0, lastClickX = 0, lastClickY = 0;
    const javaButton = (b) => (b === 0 ? 1 : b === 1 ? 2 : b === 2 ? 3 : 0);
    const buttonMask = (b) => (b === 1 ? 1024 : b === 2 ? 2048 : b === 3 ? 4096 : 0);
    const mouseEvent = (dom, id, x, y, button, count) => {
        const ev = new MouseEvent(panel, dom, id, x, y, button, count);
        ev.$buttonsEx = buttons;
        return ev;
    };
    const fire = (list, method, ev) => {
        for (const l of list.slice()) {
            try { call(l, method, ev); } catch (err) { console.error(err); }
        }
    };
    const track = (e) => {
        const [x, y] = pos(e);
        J.$mouse.x = x; J.$mouse.y = y;
        J.$mouse.screenX = x + panel.$screenX(); J.$mouse.screenY = y + panel.$screenY();
        return [x, y];
    };
    canvas.addEventListener('mousedown', (e) => {
        J.$audio.unlock();
        canvas.focus({ preventScroll: true });
        e.preventDefault();
        const [x, y] = track(e);
        const b = javaButton(e.button);
        buttons |= buttonMask(b);
        const now = Date.now();
        clickCount = (now - lastClickTime < 500 && Math.abs(x - lastClickX) < 3 && Math.abs(y - lastClickY) < 3) ? clickCount + 1 : 1;
        lastClickTime = now; lastClickX = x; lastClickY = y;
        pressX = x; pressY = y; pressButton = b; moved = false;
        try { canvas.setPointerCapture && e.pointerId !== undefined && canvas.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
        fire(panel.$listeners.mouse, 'mousePressed', mouseEvent(e, 501, x, y, b, clickCount));
    });
    window.addEventListener('mouseup', (e) => {
        if (!(buttons & buttonMask(javaButton(e.button)))) return;
        const [x, y] = track(e);
        const b = javaButton(e.button);
        const ev = mouseEvent(e, 502, x, y, b, clickCount);
        buttons &= ~buttonMask(b);
        fire(panel.$listeners.mouse, 'mouseReleased', ev);
        if (!moved && b === pressButton && x === pressX && y === pressY) {
            fire(panel.$listeners.mouse, 'mouseClicked', mouseEvent(e, 500, x, y, b, clickCount));
        }
    });
    window.addEventListener('mousemove', (e) => {
        const [x, y] = track(e);
        const inside = e.target === canvas;
        if (buttons) {
            if (x !== pressX || y !== pressY) moved = true;
            fire(panel.$listeners.motion, 'mouseDragged', mouseEvent(e, 506, x, y, 0, 0));
        } else if (inside) {
            fire(panel.$listeners.motion, 'mouseMoved', mouseEvent(e, 503, x, y, 0, 0));
        }
    });
    canvas.addEventListener('mouseenter', (e) => {
        J.$mouse.inside = true; J.$mouse.target = panel;
        const [x, y] = track(e);
        fire(panel.$listeners.mouse, 'mouseEntered', mouseEvent(e, 504, x, y, 0, 0));
    });
    canvas.addEventListener('mouseleave', (e) => {
        J.$mouse.inside = false;
        const [x, y] = track(e);
        fire(panel.$listeners.mouse, 'mouseExited', mouseEvent(e, 505, x, y, 0, 0));
    });
    canvas.addEventListener('contextmenu', (e) => e.preventDefault());
    let wheelAcc = 0;
    canvas.addEventListener('wheel', (e) => {
        e.preventDefault();
        const [x, y] = track(e);
        // Java reports whole notches (+1 down, -1 up); a mouse wheel notch is ~100px or 3 lines.
        let notches;
        if (e.deltaMode === 1) notches = e.deltaY / 3;
        else if (e.deltaMode === 2) notches = e.deltaY;
        else notches = e.deltaY / 100;
        wheelAcc += notches;
        let rotation = wheelAcc > 0 ? Math.floor(wheelAcc) : Math.ceil(wheelAcc);
        if (rotation === 0 && Math.abs(notches) >= 0.99) rotation = Math.sign(notches);
        if (rotation === 0) return;
        wheelAcc -= rotation;
        if (Math.abs(wheelAcc) < 0.05) wheelAcc = 0;
        fire(panel.$listeners.wheel, 'mouseWheelMoved', new MouseWheelEvent(panel, e, x, y, rotation, rotation));
    }, { passive: false });
    // Touch: map a single finger to the left mouse button.
    const touchToMouse = (type) => (e) => {
        if (e.touches.length > 1) return;
        const t = e.changedTouches[0];
        e.preventDefault();
        const fake = { clientX: t.clientX, clientY: t.clientY, button: 0, target: canvas, preventDefault() {} };
        if (type === 'down') canvas.dispatchEvent(new window.MouseEvent('mousedown', { clientX: t.clientX, clientY: t.clientY, button: 0, bubbles: true }));
        else if (type === 'up') window.dispatchEvent(new window.MouseEvent('mouseup', { clientX: t.clientX, clientY: t.clientY, button: 0 }));
        else window.dispatchEvent(new window.MouseEvent('mousemove', { clientX: fake.clientX, clientY: fake.clientY }));
    };
    canvas.addEventListener('touchstart', touchToMouse('down'), { passive: false });
    canvas.addEventListener('touchmove', touchToMouse('move'), { passive: false });
    canvas.addEventListener('touchend', touchToMouse('up'), { passive: false });
}

// ---------------------------------------------------------------- sound (javax.sound.sampled)

J.$audio = {
    ctx: null,
    get() {
        if (!this.ctx) {
            const AC = window.AudioContext || window.webkitAudioContext;
            if (AC) this.ctx = new AC();
        }
        return this.ctx;
    },
    unlock() {
        const c = this.get();
        if (c && c.state === 'suspended') c.resume();
    },
};

class AudioFormat {
    constructor(sampleRate, bits, channels, signed, bigEndian) {
        this.sampleRate = sampleRate; this.bits = bits; this.channels = channels; this.signed = signed; this.bigEndian = bigEndian;
    }
    getSampleRate() { return this.sampleRate; }
    getChannels() { return this.channels; }
    getSampleSizeInBits() { return this.bits; }
}
J.AudioFormat = AudioFormat;

/** SourceDataLine: collects written samples and plays them when drained, in order. */
let lineEndTime = 0;
class SourceDataLine {
    constructor(format) { this.format = format || null; this.chunks = []; this.open_ = false; }
    open(format) { if (format) this.format = format; this.open_ = true; }
    start() {}
    stop() {}
    write(buf, off, len) {
        this.chunks.push(buf.slice(off, off + len));
        return len;
    }
    drain() {
        const ctx = J.$audio.get();
        if (!ctx || !this.format || ctx.state !== 'running') { this.chunks = []; return; }
        const f = this.format;
        const bytesPerSample = f.bits / 8;
        const total = this.chunks.reduce((n, c) => n + c.length, 0);
        const frames = Math.floor(total / bytesPerSample / f.channels);
        if (frames === 0) return;
        const buffer = ctx.createBuffer(f.channels, frames, f.sampleRate);
        const all = new Uint8Array(total);
        let p = 0;
        for (const c of this.chunks) { for (let i = 0; i < c.length; i++) all[p++] = c[i] & 0xff; }
        for (let ch = 0; ch < f.channels; ch++) {
            const data = buffer.getChannelData(ch);
            for (let i = 0; i < frames; i++) {
                const idx = (i * f.channels + ch) * bytesPerSample;
                let v;
                if (bytesPerSample === 1) v = f.signed ? ((all[idx] << 24) >> 24) / 128 : (all[idx] - 128) / 128;
                else {
                    const lo = f.bigEndian ? all[idx + 1] : all[idx], hi = f.bigEndian ? all[idx] : all[idx + 1];
                    v = (((hi << 8) | lo) << 16 >> 16) / 32768;
                }
                data[i] = v;
            }
        }
        const src = ctx.createBufferSource();
        src.buffer = buffer;
        src.connect(ctx.destination);
        src.start();
        this.chunks = [];
    }
    flush() { this.chunks = []; }
    close() { this.open_ = false; }
    isOpen() { return this.open_; }
    getControl() { return new FloatControl(); }
    isControlSupported() { return false; }
}
J.SourceDataLine = SourceDataLine;

class FloatControl {
    constructor() { this.value = 0; }
    setValue(v) { this.value = v; }
    getValue() { return this.value; }
    getMinimum() { return -80; }
    getMaximum() { return 6.0206; }
}
FloatControl.Type = { MASTER_GAIN: 'Master Gain', VOLUME: 'Volume' };
J.FloatControl = FloatControl;

class Clip {
    constructor() { this.buffer = null; this.src = null; this.gain = null; this.open_ = false; this.control = new FloatControl(); }
    open(stream) { this.stream = stream; this.open_ = true; }
    isOpen() { return this.open_; }
    loop() {}
    start() {}
    stop() {}
    close() { this.open_ = false; }
    isRunning() { return false; }
    setFramePosition() {}
    getControl() { return this.control; }
    isControlSupported() { return true; }
}
Clip.LOOP_CONTINUOUSLY = -1;
J.Clip = Clip;

J.AudioSystem = {
    getSourceDataLine: (format) => new SourceDataLine(format),
    getClip: () => new Clip(),
    getAudioInputStream(src) {
        if (!src) throw new J.UnsupportedAudioFileException('No audio');
        return { $src: src, close() {} };
    },
    getLine: () => new SourceDataLine(null),
};
J.DataLine = { Info: class { constructor() {} } };
})();
