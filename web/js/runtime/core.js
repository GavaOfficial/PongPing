// PongPing web runtime - core Java semantics (numbers, strings, collections, system).
// The game code in pongping.js is translated from Java and calls into this namespace (J).
'use strict';

const J = {};
(function () {

// ---------------------------------------------------------------- numbers

/** (int) cast from a double: truncate, NaN -> 0, saturate. */
J.$i = function (x) {
    if (x !== x) return 0;
    if (x >= 2147483647) return 2147483647;
    if (x <= -2147483648) return -2147483648;
    return x < 0 ? Math.ceil(x) : Math.floor(x);
};
/** (long) cast from a double. */
J.$l = function (x) {
    if (x !== x) return 0;
    if (x >= 9223372036854775807) return 9223372036854775807;
    if (x <= -9223372036854775808) return -9223372036854775808;
    return Math.trunc(x);
};
J.$i2b = (x) => (x << 24) >> 24;
J.$i2s = (x) => (x << 16) >> 16;
/** char (a 1-length string) to its code. */
J.$c = (ch) => (typeof ch === 'string' ? ch.charCodeAt(0) : ch);

class ArithmeticException extends Error {
    constructor(msg) { super(msg); this.name = 'ArithmeticException'; }
}
J.$idiv = function (a, b) {
    if (b === 0) throw new ArithmeticException('/ by zero');
    return Math.trunc(a / b);
};
J.$imod = function (a, b) {
    if (b === 0) throw new ArithmeticException('/ by zero');
    return a % b;
};

/** Java Double.toString */
J.$d2s = function (d) {
    if (d !== d) return 'NaN';
    if (d === Infinity) return 'Infinity';
    if (d === -Infinity) return '-Infinity';
    if (d === 0) return (1 / d < 0) ? '-0.0' : '0.0';
    const a = Math.abs(d);
    if (a >= 1e-3 && a < 1e7) {
        const s = String(d);
        return s.indexOf('.') >= 0 ? s : s + '.0';
    }
    return javaSci(d.toExponential());
};
function javaSci(s) {
    // "1.234e-5" -> "1.234E-5", "1e+21" -> "1.0E21"
    let [m, e] = s.split('e');
    if (m.indexOf('.') < 0) m += '.0';
    if (e[0] === '+') e = e.substring(1);
    return m + 'E' + e;
}
/** Java Float.toString */
J.$f2s = function (f) {
    if (f !== f) return 'NaN';
    if (f === Infinity) return 'Infinity';
    if (f === -Infinity) return '-Infinity';
    if (f === 0) return (1 / f < 0) ? '-0.0' : '0.0';
    let digits = null;
    for (let p = 1; p <= 9; p++) {
        const s = f.toPrecision(p);
        if (Math.fround(parseFloat(s)) === f) { digits = parseFloat(s); break; }
    }
    if (digits === null) digits = f;
    const a = Math.abs(digits);
    if (a >= 1e-3 && a < 1e7) {
        const s = String(digits);
        return s.indexOf('.') >= 0 ? s : s + '.0';
    }
    return javaSci(digits.toExponential());
};

/** Marks a number as a Java double/float when passed to String.format / string conversion. */
class JDouble {
    constructor(v) { this.v = v; }
    toString() { return J.$d2s(this.v); }
    valueOf() { return this.v; }
}
J.$dbl = (v) => new JDouble(v);

/** String.valueOf(Object) */
J.$str = function (x) {
    if (x === null || x === undefined) return 'null';
    if (typeof x === 'string') return x;
    if (typeof x === 'number') return Number.isInteger(x) ? String(x) : J.$d2s(x);
    if (typeof x === 'boolean') return String(x);
    if (typeof x.toString === 'function') return x.toString();
    return String(x);
};

J.$compare = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

// ---------------------------------------------------------------- strings

J.$equalsIgnoreCase = (a, b) => b !== null && b !== undefined && a.length === b.length && a.toUpperCase() === b.toUpperCase();
J.$trim = function (s) {
    let st = 0, len = s.length;
    while (st < len && s.charCodeAt(st) <= 32) st++;
    while (st < len && s.charCodeAt(len - 1) <= 32) len--;
    return (st > 0 || len < s.length) ? s.substring(st, len) : s;
};
J.$compareStrings = function (a, b) {
    const n = Math.min(a.length, b.length);
    for (let i = 0; i < n; i++) {
        const d = a.charCodeAt(i) - b.charCodeAt(i);
        if (d !== 0) return d;
    }
    return a.length - b.length;
};
J.$stringHash = function (s) {
    let h = 0;
    for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    return h;
};
J.$charArrayToString = (arr) => arr.join('');

const regexCache = new Map();
function javaRegex(re, flags) {
    const key = re + '/' + flags;
    let r = regexCache.get(key);
    if (!r) {
        // Java and JS regex syntax agree for the patterns used by the game.
        r = new RegExp(re, flags);
        regexCache.set(key, r);
    }
    r.lastIndex = 0;
    return r;
}
function javaReplacement(rep) {
    // Java: $n group refs, backslash escapes. JS: $n, $$ for a literal dollar.
    let out = '';
    for (let i = 0; i < rep.length; i++) {
        const c = rep[i];
        if (c === '\\' && i + 1 < rep.length) { const n = rep[++i]; out += n === '$' ? '$$' : n; }
        else out += c;
    }
    return out;
}
J.$replaceAll = (s, re, rep) => s.replace(javaRegex(re, 'g'), javaReplacement(rep));
J.$replaceFirst = (s, re, rep) => s.replace(javaRegex(re, ''), javaReplacement(rep));
J.$matches = (s, re) => javaRegex('^(?:' + re + ')$', '').test(s);
/** Java String.split semantics (trailing empty strings removed when limit == 0). */
J.$split = function (s, re, limit) {
    limit = limit || 0;
    const list = [];
    const r = javaRegex(re, 'g');
    let index = 0, m;
    const limited = limit > 0;
    while ((m = r.exec(s)) !== null) {
        const start = m.index, end = m.index + m[0].length;
        if (end === start) r.lastIndex++;
        if (end === start && start >= s.length) break;
        if (!limited || list.length < limit - 1) {
            if (index === 0 && start === 0 && start === end) continue;
            list.push(s.substring(index, start));
            index = end;
        } else {
            break;
        }
    }
    if (index === 0) return [s];
    list.push(s.substring(index));
    if (limit === 0) {
        let n = list.length;
        while (n > 0 && list[n - 1].length === 0) n--;
        list.length = n;
    }
    return list;
};

/** Java String.format (subset used by the game: %d %s %f %x %c %b %% %n with flags and width). */
J.$format = function (fmt, ...args) {
    let argIndex = 0;
    return fmt.replace(/%(\d+\$)?([-#+ 0,(]*)(\d+)?(\.\d+)?([a-zA-Z%])/g, (all, pos, flags, width, prec, conv) => {
        if (conv === '%') return pad('%', flags, width);
        if (conv === 'n') return '\n';
        let arg = pos ? args[parseInt(pos) - 1] : args[argIndex++];
        const precision = prec ? parseInt(prec.substring(1)) : -1;
        let s;
        switch (conv) {
            case 'd': {
                const v = arg instanceof JDouble ? arg.v : arg;
                let digits = String(Math.abs(v));
                if (flags.indexOf(',') >= 0) digits = group(digits);
                s = signed(v < 0, digits, flags);
                return pad(s, flags, width);
            }
            case 'f': case 'e': case 'E': {
                const v = arg instanceof JDouble ? arg.v : Number(arg);
                const p = precision < 0 ? 6 : precision;
                if (conv === 'f') {
                    let digits = formatFixed(Math.abs(v), p).replace('.', J.$decimalSeparator);
                    if (flags.indexOf(',') >= 0) {
                        const dot = digits.indexOf(J.$decimalSeparator);
                        digits = group(dot < 0 ? digits : digits.substring(0, dot)) + (dot < 0 ? '' : digits.substring(dot));
                    }
                    s = signed(v < 0 || (v === 0 && 1 / v < 0), digits, flags);
                } else {
                    s = signed(v < 0, Math.abs(v).toExponential(p).replace(/e([+-])(\d)$/, 'e$10$2'), flags);
                    if (conv === 'E') s = s.toUpperCase();
                }
                return pad(s, flags, width);
            }
            case 's': case 'S': {
                s = J.$str(arg);
                if (precision >= 0) s = s.substring(0, precision);
                if (conv === 'S') s = s.toUpperCase();
                return pad(s, flags, width);
            }
            case 'x': case 'X': {
                let v = arg instanceof JDouble ? arg.v : arg;
                s = (v >>> 0).toString(16);
                if (conv === 'X') s = s.toUpperCase();
                return pad(s, flags, width);
            }
            case 'c': return pad(typeof arg === 'number' ? String.fromCharCode(arg) : String(arg), flags, width);
            case 'b': case 'B': return pad(String(arg !== null && arg !== undefined && arg !== false), flags, width);
        }
        return all;
    });
    function group(digits) {
        return digits.replace(/\B(?=(\d{3})+(?!\d))/g, J.$groupingSeparator);
    }
    function signed(neg, digits, flags) {
        if (neg) return flags.indexOf('(') >= 0 ? '(' + digits + ')' : '-' + digits;
        if (flags.indexOf('+') >= 0) return '+' + digits;
        if (flags.indexOf(' ') >= 0) return ' ' + digits;
        return digits;
    }
    function pad(s, flags, width) {
        if (!width) return s;
        const w = parseInt(width);
        if (s.length >= w) return s;
        if (flags.indexOf('-') >= 0) return s + ' '.repeat(w - s.length);
        if (flags.indexOf('0') >= 0) {
            const sign = (s[0] === '-' || s[0] === '+') ? s[0] : '';
            return sign + '0'.repeat(w - s.length) + s.substring(sign.length);
        }
        return ' '.repeat(w - s.length) + s;
    }
};
/**
 * Java's String.format uses the default locale of the machine (e.g. "2,5" on an Italian system).
 * The browser's locale plays the same role here.
 */
J.$decimalSeparator = '.';
J.$groupingSeparator = ',';
try {
    const parts = new Intl.NumberFormat(navigator.language).formatToParts(12345.6);
    for (const p of parts) {
        if (p.type === 'decimal') J.$decimalSeparator = p.value;
        if (p.type === 'group') J.$groupingSeparator = p.value;
    }
    if (J.$groupingSeparator === J.$decimalSeparator) J.$groupingSeparator = J.$decimalSeparator === ',' ? '.' : ',';
    if (/^(it|es|de|pt|nl)/.test(navigator.language) && J.$decimalSeparator === ',') J.$groupingSeparator = '.';
} catch (e) { /* keep defaults */ }

/**
 * Java's Formatter rounds the shortest decimal representation of the double with HALF_UP
 * (so String.format("%.2f", 1.005) is "1.01", unlike toFixed).
 */
function formatFixed(v, p) {
    if (!isFinite(v)) return v !== v ? 'NaN' : 'Infinity';
    let s = String(v);
    let exp = 0;
    const ei = s.indexOf('e');
    if (ei >= 0) { exp = parseInt(s.substring(ei + 1)); s = s.substring(0, ei); }
    let [ip, fp] = s.split('.');
    fp = fp || '';
    // shift by exponent
    let digits = ip + fp;
    let pointPos = ip.length + exp;
    if (pointPos < 0) { digits = '0'.repeat(-pointPos) + digits; pointPos = 0; }
    if (pointPos > digits.length) digits = digits + '0'.repeat(pointPos - digits.length);
    let intPart = digits.substring(0, pointPos) || '0';
    let fracPart = digits.substring(pointPos);
    if (fracPart.length <= p) return intPart.replace(/^0+(?=\d)/, '') + (p > 0 ? '.' + fracPart + '0'.repeat(p - fracPart.length) : '');
    const roundUp = fracPart.charCodeAt(p) >= 53; // '5'
    let kept = (intPart + fracPart.substring(0, p)).split('').map(Number);
    if (roundUp) {
        let i = kept.length - 1;
        while (i >= 0) {
            if (kept[i] === 9) { kept[i] = 0; i--; } else { kept[i]++; break; }
        }
        if (i < 0) kept.unshift(1);
    }
    const str = kept.join('');
    const ilen = str.length - p;
    const res = str.substring(0, ilen).replace(/^0+(?=\d)/, '') + (p > 0 ? '.' + str.substring(ilen) : '');
    return res;
}
J.$formatFixed = formatFixed;

// ---------------------------------------------------------------- misc language support

J.$newArray = function (dims, def) {
    function make(level) {
        const n = dims[level];
        const a = new Array(n);
        if (level === dims.length - 1) a.fill(def);
        else for (let i = 0; i < n; i++) a[i] = make(level + 1);
        return a;
    }
    return make(0);
};

class $Enum {
    constructor(name, ordinal) { this.$name = name; this.$ordinal = ordinal; }
    name() { return this.$name; }
    ordinal() { return this.$ordinal; }
    toString() { return this.$name; }
    equals(o) { return this === o; }
    hashCode() { return this.$ordinal * 31 + 7; }
    compareTo(o) { return this.$ordinal - o.$ordinal; }
}
J.$Enum = $Enum;
J.$enumValueOf = function (cls, name) {
    for (const v of cls.$VALUES) if (v.$name === name) return v;
    throw new IllegalArgumentException('No enum constant ' + cls.name + '.' + name);
};

/** Anonymous class instance: an object with the methods; `type` is kept for instanceof-like checks. */
J.$impl = function (type, obj) {
    obj.$type = type;
    return obj;
};

// Exceptions ---------------------------------------------------------------

class JavaException extends Error {
    constructor(msg) {
        super(msg === undefined || msg === null ? '' : String(msg));
        this.$message = (msg === undefined) ? null : msg;
        this.name = this.constructor.name;
    }
    getMessage() { return this.$message; }
    printStackTrace() { console.error(this); }
    toString() { return this.name + (this.$message !== null ? ': ' + this.$message : ''); }
}
class RuntimeException extends JavaException {}
class IllegalArgumentException extends RuntimeException {}
class NumberFormatException extends IllegalArgumentException {}
class IllegalStateException extends RuntimeException {}
class IndexOutOfBoundsException extends RuntimeException {}
class UnsupportedOperationException extends RuntimeException {}
class IOException extends JavaException {}
class FileNotFoundException extends IOException {}
class ClassNotFoundException extends JavaException {}
class InterruptedException extends JavaException {}
class UnsupportedAudioFileException extends JavaException {}
class LineUnavailableException extends JavaException {}
class FontFormatException extends JavaException {}
Object.assign(J, {
    RuntimeException, IllegalArgumentException, NumberFormatException, IllegalStateException,
    IndexOutOfBoundsException, UnsupportedOperationException, IOException, FileNotFoundException,
    ClassNotFoundException, InterruptedException, UnsupportedAudioFileException, LineUnavailableException,
    FontFormatException, ArithmeticException, Exception: JavaException,
});
// Errors raised by the JS engine behave like Java runtime exceptions (e.g. NullPointerException).
Error.prototype.getMessage = function () { return this.message; };
Error.prototype.printStackTrace = function () { console.error(this); };

J.$isException = function (e, type) {
    switch (type) {
        case 'Exception': case 'Throwable': case 'RuntimeException': case 'Error': return true;
        case 'NullPointerException': return e instanceof TypeError;
        case 'ArrayIndexOutOfBoundsException': case 'IndexOutOfBoundsException': return e instanceof IndexOutOfBoundsException;
        case 'ClassCastException': return e instanceof TypeError;
    }
    const cls = J[type];
    return typeof cls === 'function' && e instanceof cls;
};
J.$wrapException = (e) => e;

// Class objects ----------------------------------------------------------------

class JClass {
    constructor(cls) { this.cls = cls; }
    getClassLoader() { return J.ClassLoader.$instance; }
    getResource(path) { return J.ClassLoader.$instance.getResource(path.startsWith('/') ? path.substring(1) : path); }
    getResourceAsStream(path) { return J.ClassLoader.$instance.getResourceAsStream(path.startsWith('/') ? path.substring(1) : path); }
    getProtectionDomain() {
        return { getCodeSource: () => ({ getLocation: () => new J.URL('file:/web/PongGame.jar') }) };
    }
    getName() { return this.cls.name; }
    getSimpleName() { return this.cls.name; }
}
J.$class = (cls) => new JClass(cls);
J.$getClass = (obj) => new JClass(obj.constructor);

// ---------------------------------------------------------------- java.lang

J.JMath = Object.assign(Object.create(null), {
    abs: Math.abs, min: Math.min, max: Math.max, sqrt: Math.sqrt, pow: Math.pow, sin: Math.sin, cos: Math.cos,
    tan: Math.tan, atan: Math.atan, atan2: Math.atan2, asin: Math.asin, acos: Math.acos, exp: Math.exp,
    log: Math.log, log10: Math.log10, floor: Math.floor, ceil: Math.ceil, hypot: Math.hypot, cbrt: Math.cbrt,
    random: Math.random, PI: Math.PI, E: Math.E,
    round: (x) => (x !== x ? 0 : Math.floor(x + 0.5)),
    signum: (x) => (x > 0 ? 1 : x < 0 ? -1 : x),
    toRadians: (d) => d / 180 * Math.PI,
    toDegrees: (r) => r * 180 / Math.PI,
    floorDiv: (a, b) => Math.floor(a / b),
    floorMod: (a, b) => ((a % b) + b) % b,
    clamp: (v, lo, hi) => Math.min(hi, Math.max(lo, v)),
});
J.Math = J.JMath;
J.StrictMath = J.JMath;

J.Integer = {
    MAX_VALUE: 2147483647,
    MIN_VALUE: -2147483648,
    parseInt(s, radix) {
        radix = radix || 10;
        if (s === null || s === undefined) throw new NumberFormatException('Cannot parse null string: null');
        const re = radix === 10 ? /^[+-]?\d+$/ : /^[+-]?[0-9a-zA-Z]+$/;
        if (!re.test(s)) throw new NumberFormatException('For input string: "' + s + '"');
        const v = parseInt(s, radix);
        if (isNaN(v) || v > 2147483647 || v < -2147483648) throw new NumberFormatException('For input string: "' + s + '"');
        return v;
    },
    valueOf(x) { return typeof x === 'string' ? J.Integer.parseInt(x) : x; },
    toString(x) { return String(x); },
    compare: (a, b) => (a < b ? -1 : a > b ? 1 : 0),
    toHexString: (x) => (x >>> 0).toString(16),
    max: Math.max, min: Math.min, sum: (a, b) => (a + b) | 0,
};
J.Long = {
    MAX_VALUE: 9223372036854775807, MIN_VALUE: -9223372036854775808,
    parseLong(s) {
        if (!/^[+-]?\d+$/.test(s)) throw new NumberFormatException('For input string: "' + s + '"');
        return parseInt(s, 10);
    },
    valueOf(x) { return typeof x === 'string' ? J.Long.parseLong(x) : x; },
    toString: (x) => String(x),
    compare: (a, b) => (a < b ? -1 : a > b ? 1 : 0),
};
J.Double = {
    MAX_VALUE: Number.MAX_VALUE, MIN_VALUE: Number.MIN_VALUE,
    POSITIVE_INFINITY: Infinity, NEGATIVE_INFINITY: -Infinity, NaN: NaN,
    parseDouble(s) {
        const t = J.$trim(s);
        if (!/^[+-]?(NaN|Infinity|((\d+\.?\d*|\.\d+)([eE][+-]?\d+)?)[fFdD]?)$/.test(t)) throw new NumberFormatException('For input string: "' + s + '"');
        return parseFloat(t.replace(/[fFdD]$/, ''));
    },
    valueOf(x) { return typeof x === 'string' ? J.Double.parseDouble(x) : x; },
    toString: (x) => J.$d2s(x),
    compare: (a, b) => (a < b ? -1 : a > b ? 1 : 0),
    isNaN: (x) => x !== x,
    isInfinite: (x) => x === Infinity || x === -Infinity,
};
J.Float = {
    MAX_VALUE: 3.4028234663852886e38,
    parseFloat: (s) => Math.fround(J.Double.parseDouble(s)),
    valueOf: (x) => (typeof x === 'string' ? Math.fround(J.Double.parseDouble(x)) : x),
    toString: (x) => J.$f2s(x),
    compare: (a, b) => (a < b ? -1 : a > b ? 1 : 0),
};
J.Boolean = {
    TRUE: true, FALSE: false,
    parseBoolean: (s) => s !== null && s !== undefined && s.toLowerCase() === 'true',
    valueOf: (x) => (typeof x === 'string' ? J.Boolean.parseBoolean(x) : x),
    toString: (b) => String(b),
};
J.Character = {
    toUpperCase: (c) => (typeof c === 'number' ? String.fromCharCode(c).toUpperCase().charCodeAt(0) : c.toUpperCase()),
    toLowerCase: (c) => (typeof c === 'number' ? String.fromCharCode(c).toLowerCase().charCodeAt(0) : c.toLowerCase()),
    isDigit: (c) => /\p{Nd}/u.test(typeof c === 'number' ? String.fromCharCode(c) : c),
    isLetter: (c) => /\p{L}/u.test(typeof c === 'number' ? String.fromCharCode(c) : c),
    isLetterOrDigit: (c) => /[\p{L}\p{Nd}]/u.test(typeof c === 'number' ? String.fromCharCode(c) : c),
    isWhitespace: (c) => /\s/.test(typeof c === 'number' ? String.fromCharCode(c) : c),
    isUpperCase: (c) => { const s = typeof c === 'number' ? String.fromCharCode(c) : c; return s !== s.toLowerCase(); },
    toString: (c) => (typeof c === 'number' ? String.fromCharCode(c) : c),
};
J.String = {
    valueOf: (x) => J.$str(x),
    format: (...a) => J.$format(...a),
    join(delim, ...items) {
        if (items.length === 1 && items[0] !== null && typeof items[0] === 'object' && !(typeof items[0] === 'string')) {
            const it = items[0];
            return Array.from(it[Symbol.iterator] ? it : [], J.$str).join(delim);
        }
        return items.map(J.$str).join(delim);
    },
};

class StringBuilder {
    constructor(init) { this.s = (typeof init === 'string') ? init : ''; }
    append(x) { this.s += (typeof x === 'string') ? x : J.$str(x); return this; }
    toString() { return this.s; }
    length() { return this.s.length; }
    charAt(i) { return this.s.charAt(i); }
    insert(i, x) { this.s = this.s.substring(0, i) + J.$str(x) + this.s.substring(i); return this; }
    reverse() { this.s = Array.from(this.s).reverse().join(''); return this; }
    setLength(n) { this.s = n <= this.s.length ? this.s.substring(0, n) : this.s + '\0'.repeat(n - this.s.length); }
    deleteCharAt(i) { this.s = this.s.substring(0, i) + this.s.substring(i + 1); return this; }
}
J.StringBuilder = StringBuilder;
J.StringBuffer = StringBuilder;

// ---------------------------------------------------------------- System / Thread / time

const perfStart = performance.now();
J.$currentTimeMillis = () => Date.now();
J.$nanoTime = () => Math.floor((performance.now() - perfStart) * 1e6);

const debugLogging = /[?&]debug\b/.test(location.search);
class PrintStream {
    constructor(err) { this.err = err; }
    println(x) { if (debugLogging || this.err) (this.err ? console.error : console.log)(arguments.length ? J.$str(x) : ''); }
    print(x) { if (debugLogging) console.log(J.$str(x)); }
    printf(fmt, ...a) { if (debugLogging) console.log(J.$format(fmt, ...a)); return this; }
    flush() {}
}
const systemProps = {
    'os.name': 'Linux', 'user.home': '/files', 'user.dir': '/', 'file.separator': '/', 'line.separator': '\n',
    'java.version': '17', 'user.name': 'web',
};
J.System = {
    out: new PrintStream(false),
    err: new PrintStream(true),
    getProperty: (k, def) => (k in systemProps ? systemProps[k] : (def === undefined ? null : def)),
    setProperty: (k, v) => { const old = systemProps[k]; systemProps[k] = v; return old === undefined ? null : old; },
    getenv: () => null,
    currentTimeMillis: () => Date.now(),
    nanoTime: () => J.$nanoTime(),
    exit: (code) => J.$exit(code),
    arraycopy: (src, sp, dst, dp, n) => { const tmp = src.slice(sp, sp + n); for (let i = 0; i < n; i++) dst[dp + i] = tmp[i]; },
    gc: () => {},
};
J.$exit = function () {
    // A web page cannot quit: restart the game from the intro, like reopening the app.
    try { window.close(); } catch (e) { /* ignore */ }
    location.reload();
};

class Thread {
    constructor(runnable) { this.runnable = runnable; this.name = 'Thread'; }
    start() {
        const r = this.runnable;
        setTimeout(() => { if (typeof r === 'function') r(); else if (r) r.run(); else this.run(); }, 0);
    }
    run() {}
    setName(n) { this.name = n; }
    setDaemon() {}
    interrupt() {}
    join() {}
    isAlive() { return false; }
    static currentThread() { return Thread.$current; }
    static sleep() {}
}
Thread.$current = new Thread(null);
J.Thread = Thread;

class JDate {
    constructor(ms) { this.ms = (ms === undefined) ? Date.now() : ms; }
    getTime() { return this.ms; }
    toString() { return new Date(this.ms).toString(); }
}
J.Date = JDate;
class Calendar {
    constructor() { this.d = new Date(); }
    static getInstance() { return new Calendar(); }
    setTime(date) { this.d = new Date(date.getTime()); }
    getTime() { return new JDate(this.d.getTime()); }
    getTimeInMillis() { return this.d.getTime(); }
    setTimeInMillis(ms) { this.d = new Date(ms); }
    get(field) {
        switch (field) {
            case Calendar.YEAR: return this.d.getFullYear();
            case Calendar.MONTH: return this.d.getMonth();
            case Calendar.DAY_OF_MONTH: return this.d.getDate();
            case Calendar.DAY_OF_YEAR: {
                const start = new Date(this.d.getFullYear(), 0, 1);
                return Math.round((new Date(this.d.getFullYear(), this.d.getMonth(), this.d.getDate()) - start) / 86400000) + 1;
            }
            case Calendar.HOUR_OF_DAY: return this.d.getHours();
            case Calendar.MINUTE: return this.d.getMinutes();
            case Calendar.SECOND: return this.d.getSeconds();
            case Calendar.DAY_OF_WEEK: return this.d.getDay() + 1;
        }
        return 0;
    }
    set(field, v) {
        switch (field) {
            case Calendar.HOUR_OF_DAY: this.d.setHours(v); break;
            case Calendar.MINUTE: this.d.setMinutes(v); break;
            case Calendar.SECOND: this.d.setSeconds(v); break;
            case Calendar.MILLISECOND: this.d.setMilliseconds(v); break;
        }
    }
    add(field, amount) {
        switch (field) {
            case Calendar.DAY_OF_YEAR: case Calendar.DAY_OF_MONTH: this.d.setDate(this.d.getDate() + amount); break;
            case Calendar.MONTH: this.d.setMonth(this.d.getMonth() + amount); break;
            case Calendar.YEAR: this.d.setFullYear(this.d.getFullYear() + amount); break;
            case Calendar.HOUR_OF_DAY: this.d.setHours(this.d.getHours() + amount); break;
            case Calendar.MINUTE: this.d.setMinutes(this.d.getMinutes() + amount); break;
        }
    }
}
Object.assign(Calendar, { YEAR: 1, MONTH: 2, DAY_OF_MONTH: 5, DAY_OF_YEAR: 6, DAY_OF_WEEK: 7, HOUR_OF_DAY: 11, MINUTE: 12, SECOND: 13, MILLISECOND: 14 });
J.Calendar = Calendar;

class SimpleDateFormat {
    constructor(pattern) { this.pattern = pattern; }
    format(date) {
        const d = new Date(date.getTime());
        const p2 = (n) => String(n).padStart(2, '0');
        return this.pattern.replace(/yyyy|yy|MM|dd|HH|mm|ss/g, (t) => {
            switch (t) {
                case 'yyyy': return String(d.getFullYear());
                case 'yy': return p2(d.getFullYear() % 100);
                case 'MM': return p2(d.getMonth() + 1);
                case 'dd': return p2(d.getDate());
                case 'HH': return p2(d.getHours());
                case 'mm': return p2(d.getMinutes());
                case 'ss': return p2(d.getSeconds());
            }
            return t;
        });
    }
}
J.SimpleDateFormat = SimpleDateFormat;
J.DateFormat = SimpleDateFormat;

// ---------------------------------------------------------------- java.util.Random (exact LCG)

const TWO24 = 0x1000000;
const MUL_HI = 0x5DE, MUL_LO = 0xECE66D;
let seedUniquifier = 8682522807148012;
class Random {
    constructor(seed) {
        if (seed === undefined) seed = (seedUniquifier = (seedUniquifier * 1181783497276652981) % 9007199254740991) ^ Math.floor(Math.random() * 281474976710656);
        this.setSeed(seed);
        this.haveNextNextGaussian = false;
    }
    setSeed(seed) {
        // (seed ^ 0x5DEECE66D) & ((1 << 48) - 1), seed given as a JS number (low 48 bits used)
        const s = BigInt.asUintN(48, BigInt(Math.trunc(seed)) ^ 0x5DEECE66Dn);
        this.hi = Number(s >> 24n);
        this.lo = Number(s & 0xFFFFFFn);
        this.haveNextNextGaussian = false;
    }
    next(bits) {
        const p = this.lo * MUL_LO + 0xB;
        const carry = Math.floor(p / TWO24);
        const lo = p - carry * TWO24;
        const hi = (this.hi * MUL_LO + this.lo * MUL_HI + carry) % TWO24;
        this.hi = hi;
        this.lo = lo;
        const v = Math.floor((hi * TWO24 + lo) / Math.pow(2, 48 - bits));
        return v | 0;
    }
    nextInt(bound) {
        if (bound === undefined) return this.next(32);
        if (bound <= 0) throw new IllegalArgumentException('bound must be positive');
        let r = this.next(31);
        const m = bound - 1;
        if ((bound & m) === 0) return Number((BigInt(bound) * BigInt(r)) >> 31n);
        for (let u = r; u - (r = u % bound) + m < 0 || u - r + m > 2147483647; u = this.next(31)) { /* retry */ }
        return r;
    }
    nextLong() { return this.next(32) * 4294967296 + this.next(32); }
    nextBoolean() { return this.next(1) !== 0; }
    nextFloat() { return this.next(24) / (1 << 24); }
    nextDouble() { return (this.next(26) * 134217728 + this.next(27)) * 1.1102230246251565e-16; }
    nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        }
        let v1, v2, s;
        do {
            v1 = 2 * this.nextDouble() - 1;
            v2 = 2 * this.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s === 0);
        const multiplier = Math.sqrt(-2 * Math.log(s) / s);
        this.nextNextGaussian = v2 * multiplier;
        this.haveNextNextGaussian = true;
        return v1 * multiplier;
    }
}
J.Random = Random;

// ---------------------------------------------------------------- collections

function jequals(a, b) {
    if (a === b) return true;
    if (a === null || b === null || a === undefined || b === undefined) return false;
    if (typeof a === 'object' && typeof a.equals === 'function') return a.equals(b);
    return false;
}
J.$equals = jequals;

class JIterator {
    constructor(list, snapshot) { this.list = list; this.arr = snapshot || list.a; this.i = 0; this.last = -1; }
    hasNext() { return this.i < this.arr.length; }
    next() {
        if (this.i >= this.arr.length) throw new J.NoSuchElementException();
        this.last = this.i;
        return this.arr[this.i++];
    }
    remove() {
        if (this.last < 0) throw new IllegalStateException();
        if (this.arr !== this.list.a) throw new UnsupportedOperationException();
        this.list.a.splice(this.last, 1);
        this.i = this.last;
        this.last = -1;
    }
}
J.NoSuchElementException = class NoSuchElementException extends RuntimeException {};

class Collection {
    stream() { return new Stream(Array.from(this)); }
    isEmpty() { return this.size() === 0; }
    forEach(f) { for (const x of this) (typeof f === 'function' ? f(x) : f.accept(x)); }
    toArray() { return Array.from(this); }
    containsAll(c) { for (const x of c) if (!this.contains(x)) return false; return true; }
    toString() { return '[' + Array.from(this, J.$str).join(', ') + ']'; }
}

class ArrayList extends Collection {
    constructor(init) {
        super();
        this.a = (init !== undefined && init !== null && typeof init === 'object') ? Array.from(init) : [];
    }
    [Symbol.iterator]() { return this.a[Symbol.iterator](); }
    size() { return this.a.length; }
    get(i) { this.$check(i); return this.a[i]; }
    set(i, v) { this.$check(i); const old = this.a[i]; this.a[i] = v; return old; }
    $check(i) {
        if (i < 0 || i >= this.a.length) throw new IndexOutOfBoundsException('Index ' + i + ' out of bounds for length ' + this.a.length);
    }
    add(i, v) {
        if (arguments.length === 1) { this.a.push(i); return true; }
        if (i < 0 || i > this.a.length) throw new IndexOutOfBoundsException('Index: ' + i + ', Size: ' + this.a.length);
        this.a.splice(i, 0, v);
    }
    addAll(i, c) {
        if (arguments.length === 1) { const arr = Array.from(i); for (const x of arr) this.a.push(x); return arr.length > 0; }
        const arr = Array.from(c);
        this.a.splice(i, 0, ...arr);
        return arr.length > 0;
    }
    removeAt(i) { this.$check(i); return this.a.splice(i, 1)[0]; }
    remove(o) {
        const i = this.indexOf(o);
        if (i < 0) return false;
        this.a.splice(i, 1);
        return true;
    }
    removeAll(c) { const s = Array.from(c); const n = this.a.length; this.a = this.a.filter(x => !s.some(y => jequals(x, y))); return this.a.length !== n; }
    removeIf(pred) {
        const f = typeof pred === 'function' ? pred : (x) => pred.test(x);
        const n = this.a.length;
        this.a = this.a.filter(x => !f(x));
        return this.a.length !== n;
    }
    clear() { this.a.length = 0; }
    contains(o) { return this.indexOf(o) >= 0; }
    indexOf(o) { for (let i = 0; i < this.a.length; i++) if (jequals(this.a[i], o)) return i; return -1; }
    lastIndexOf(o) { for (let i = this.a.length - 1; i >= 0; i--) if (jequals(this.a[i], o)) return i; return -1; }
    iterator() { return new JIterator(this); }
    listIterator() { return new JIterator(this); }
    sort(cmp) { J.$sort(this.a, cmp); }
    subList(from, to) { return new ArrayList(this.a.slice(from, to)); }
    toArray() { return this.a.slice(); }
    getFirst() { return this.get(0); }
    getLast() { return this.get(this.a.length - 1); }
    equals(o) { return o instanceof ArrayList && o.a.length === this.a.length && this.a.every((x, i) => jequals(x, o.a[i])); }
}
J.ArrayList = ArrayList;
J.LinkedList = ArrayList;
J.List = { of: (...a) => new ArrayList(a), copyOf: (c) => new ArrayList(c) };

/** Iteration works on a snapshot, like java.util.concurrent.CopyOnWriteArrayList. */
class CopyOnWriteArrayList extends ArrayList {
    [Symbol.iterator]() { return this.a.slice()[Symbol.iterator](); }
    iterator() { return new JIterator(this, this.a.slice()); }
    add(i, v) {
        if (arguments.length === 1) { this.a = this.a.concat([i]); return true; }
        const c = this.a.slice(); c.splice(i, 0, v); this.a = c;
    }
    removeAt(i) { this.$check(i); const c = this.a.slice(); const r = c.splice(i, 1)[0]; this.a = c; return r; }
    remove(o) { const i = this.indexOf(o); if (i < 0) return false; this.removeAt(i); return true; }
    clear() { this.a = []; }
}
J.CopyOnWriteArrayList = CopyOnWriteArrayList;

/** Stable merge sort, like Java's TimSort for objects (Array.prototype.sort is stable too). */
J.$sort = function (arr, cmp) {
    const c = cmp ? (typeof cmp === 'function' ? cmp : (a, b) => cmp.compare(a, b))
        : (a, b) => (typeof a === 'string' ? J.$compareStrings(a, b) : (a.compareTo ? a.compareTo(b) : J.$compare(a, b)));
    arr.sort(c);
};
J.Arrays = {
    sort(arr, a, b, cmp) {
        if (typeof a === 'number') { const part = arr.slice(a, b); J.$sort(part, cmp); for (let i = 0; i < part.length; i++) arr[a + i] = part[i]; }
        else J.$sort(arr, a);
    },
    asList: (...a) => new ArrayList(a.length === 1 && Array.isArray(a[0]) ? a[0] : a),
    fill(arr, v) { arr.fill(v); },
    copyOf(arr, n) { const r = arr.slice(0, n); while (r.length < n) r.push(typeof arr[0] === 'number' ? 0 : null); return r; },
    toString: (arr) => arr === null ? 'null' : '[' + arr.map(J.$str).join(', ') + ']',
    stream: (arr) => new Stream(arr.slice()),
};
J.Collections = {
    sort(list, cmp) { list.sort(cmp); },
    reverse(list) { list.a.reverse(); },
    shuffle(list, rnd) {
        const r = rnd || new Random();
        for (let i = list.a.length; i > 1; i--) { const j = r.nextInt(i); const t = list.a[i - 1]; list.a[i - 1] = list.a[j]; list.a[j] = t; }
    },
    unmodifiableList: (l) => l,
    emptyList: () => new ArrayList(),
    max(c, cmp) { let best; let first = true; for (const x of c) { if (first || (cmp ? cmp(x, best) : J.$compare(x, best)) > 0) best = x; first = false; } return best; },
};

/** Java's HashMap hash spreading and bucket order, so iteration order matches Java. */
function javaHash(k) {
    let h;
    if (k === null || k === undefined) return 0;
    if (typeof k === 'string') h = J.$stringHash(k);
    else if (typeof k === 'number') h = Number.isInteger(k) ? k | 0 : doubleHash(k);
    else if (typeof k === 'boolean') h = k ? 1231 : 1237;
    else if (typeof k.hashCode === 'function') h = k.hashCode() | 0;
    else h = identityHash(k);
    return h ^ (h >>> 16);
}
const dv = new DataView(new ArrayBuffer(8));
function doubleHash(d) {
    dv.setFloat64(0, d);
    return (dv.getInt32(0) ^ dv.getInt32(4)) | 0;
}
const identityHashes = new WeakMap();
let nextIdentityHash = 0x1234567;
function identityHash(o) {
    let h = identityHashes.get(o);
    if (h === undefined) { nextIdentityHash = (Math.imul(nextIdentityHash, 1103515245) + 12345) | 0; h = nextIdentityHash; identityHashes.set(o, h); }
    return h;
}
function mapKey(k) {
    // Keys are compared with equals(): strings/numbers/booleans by value, objects with equals() by a string key.
    if (k !== null && typeof k === 'object' && typeof k.$mapKey === 'function') return k.$mapKey();
    return k;
}

class HashMap {
    constructor(init) {
        this.m = new Map();      // mapKey -> {k, v, seq}
        this.seq = 0;
        this.capacity = 16;
        if (init instanceof HashMap) for (const e of init.entrySet()) this.put(e.getKey(), e.getValue());
    }
    $grow() {
        while (this.m.size > this.capacity * 0.75) this.capacity *= 2;
    }
    size() { return this.m.size; }
    isEmpty() { return this.m.size === 0; }
    get(k) { const e = this.m.get(mapKey(k)); return e ? e.v : null; }
    getOrDefault(k, d) { const e = this.m.get(mapKey(k)); return e ? e.v : d; }
    containsKey(k) { return this.m.has(mapKey(k)); }
    containsValue(v) { for (const e of this.m.values()) if (jequals(e.v, v)) return true; return false; }
    put(k, v) {
        const mk = mapKey(k);
        const e = this.m.get(mk);
        if (e) { const old = e.v; e.v = v; return old; }
        this.m.set(mk, { k, v, seq: this.seq++, h: javaHash(k) });
        this.$grow();
        this.$order = null;
        return null;
    }
    putIfAbsent(k, v) { const e = this.m.get(mapKey(k)); if (e && e.v !== null) return e.v; this.put(k, v); return null; }
    putAll(o) { for (const e of o.entrySet()) this.put(e.getKey(), e.getValue()); }
    remove(k) {
        const mk = mapKey(k);
        const e = this.m.get(mk);
        if (!e) return null;
        this.m.delete(mk);
        this.$order = null;
        return e.v;
    }
    clear() { this.m.clear(); this.$order = null; }
    merge(k, v, f) {
        const old = this.get(k);
        const nv = old === null ? v : (typeof f === 'function' ? f(old, v) : f.apply(old, v));
        if (nv === null) this.remove(k); else this.put(k, nv);
        return nv;
    }
    computeIfAbsent(k, f) {
        let v = this.get(k);
        if (v === null) { v = typeof f === 'function' ? f(k) : f.apply(k); if (v !== null) this.put(k, v); }
        return v;
    }
    /** Entries in Java HashMap iteration order: by bucket, then insertion order within a bucket. */
    $entries() {
        if (!this.$order || this.$orderCap !== this.capacity) {
            const mask = this.capacity - 1;
            this.$order = Array.from(this.m.values()).sort((a, b) => ((a.h & mask) - (b.h & mask)) || (a.seq - b.seq));
            this.$orderCap = this.capacity;
        }
        return this.$order;
    }
    entrySet() { return new EntrySet(this); }
    keySet() { return new KeyView(this); }
    values() { return new ValueView(this); }
    forEach(f) { for (const e of this.$entries()) (typeof f === 'function' ? f(e.k, e.v) : f.accept(e.k, e.v)); }
    toString() { return '{' + this.$entries().map(e => J.$str(e.k) + '=' + J.$str(e.v)).join(', ') + '}'; }
}
class MapEntry {
    constructor(e, map) { this.e = e; this.map = map; }
    getKey() { return this.e.k; }
    getValue() { return this.e.v; }
    setValue(v) { const o = this.e.v; this.e.v = v; return o; }
    toString() { return J.$str(this.e.k) + '=' + J.$str(this.e.v); }
}
class EntrySet extends Collection {
    constructor(map) { super(); this.map = map; }
    *[Symbol.iterator]() { for (const e of this.map.$entries().slice()) yield new MapEntry(e, this.map); }
    size() { return this.map.size(); }
    iterator() { const l = new ArrayList(this); return new JIterator(l, l.a); }
}
class KeyView extends Collection {
    constructor(map) { super(); this.map = map; }
    *[Symbol.iterator]() { for (const e of this.map.$entries().slice()) yield e.k; }
    size() { return this.map.size(); }
    contains(k) { return this.map.containsKey(k); }
    iterator() {
        const keys = Array.from(this);
        const map = this.map;
        let i = 0;
        return { hasNext: () => i < keys.length, next: () => keys[i++], remove: () => map.remove(keys[i - 1]) };
    }
}
class ValueView extends Collection {
    constructor(map) { super(); this.map = map; }
    *[Symbol.iterator]() { for (const e of this.map.$entries().slice()) yield e.v; }
    size() { return this.map.size(); }
    contains(v) { return this.map.containsValue(v); }
    iterator() { const l = new ArrayList(this); return new JIterator(l, l.a); }
}
J.HashMap = HashMap;
J.Hashtable = HashMap;
J.ConcurrentHashMap = HashMap;
/** LinkedHashMap iterates in insertion order. */
class LinkedHashMap extends HashMap {
    $entries() { return Array.from(this.m.values()); }
}
J.LinkedHashMap = LinkedHashMap;
J.Map = {
    Entry: {
        comparingByValue: (cmp) => (a, b) => (cmp ? (typeof cmp === 'function' ? cmp(a.getValue(), b.getValue()) : cmp.compare(a.getValue(), b.getValue()))
            : (typeof a.getValue() === 'string' ? J.$compareStrings(a.getValue(), b.getValue()) : J.$compare(a.getValue(), b.getValue()))),
        comparingByKey: () => (a, b) => (typeof a.getKey() === 'string' ? J.$compareStrings(a.getKey(), b.getKey()) : J.$compare(a.getKey(), b.getKey())),
    },
    of: (...kv) => { const m = new HashMap(); for (let i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]); return m; },
};

class HashSet extends Collection {
    constructor(init) {
        super();
        this.map = new HashMap();
        if (init !== undefined && init !== null && typeof init === 'object') for (const x of init) this.add(x);
    }
    [Symbol.iterator]() { return this.map.keySet()[Symbol.iterator](); }
    add(x) { if (this.map.containsKey(x)) return false; this.map.put(x, true); return true; }
    addAll(c) { let ch = false; for (const x of c) ch = this.add(x) || ch; return ch; }
    remove(x) { return this.map.remove(x) !== null; }
    contains(x) { return this.map.containsKey(x); }
    size() { return this.map.size(); }
    clear() { this.map.clear(); }
    iterator() { return this.map.keySet().iterator(); }
    removeIf(pred) {
        const f = typeof pred === 'function' ? pred : (x) => pred.test(x);
        let ch = false;
        for (const x of Array.from(this)) if (f(x)) { this.remove(x); ch = true; }
        return ch;
    }
    equals(o) { return o instanceof HashSet && o.size() === this.size() && this.containsAll(o); }
}
J.HashSet = HashSet;
J.LinkedHashSet = HashSet;
J.Set = { of: (...a) => new HashSet(a) };

class ArrayDeque extends Collection {
    constructor() { super(); this.a = []; }
    [Symbol.iterator]() { return this.a[Symbol.iterator](); }
    size() { return this.a.length; }
    offer(x) { this.a.push(x); return true; }
    add(x) { this.a.push(x); return true; }
    addLast(x) { this.a.push(x); }
    addFirst(x) { this.a.unshift(x); }
    push(x) { this.a.unshift(x); }
    poll() { return this.a.length ? this.a.shift() : null; }
    pollFirst() { return this.poll(); }
    pollLast() { return this.a.length ? this.a.pop() : null; }
    pop() { if (!this.a.length) throw new J.NoSuchElementException(); return this.a.shift(); }
    peek() { return this.a.length ? this.a[0] : null; }
    peekFirst() { return this.peek(); }
    peekLast() { return this.a.length ? this.a[this.a.length - 1] : null; }
    clear() { this.a.length = 0; }
    contains(o) { return this.a.some(x => jequals(x, o)); }
    iterator() { return new JIterator({ a: this.a }, this.a); }
}
J.ArrayDeque = ArrayDeque;
J.Queue = ArrayDeque;
J.Deque = ArrayDeque;

// Minimal java.util.stream
class Optional {
    constructor(v) { this.v = v; }
    isPresent() { return this.v !== null && this.v !== undefined; }
    get() { if (!this.isPresent()) throw new J.NoSuchElementException('No value present'); return this.v; }
    orElse(d) { return this.isPresent() ? this.v : d; }
    map(f) { return this.isPresent() ? new Optional(typeof f === 'function' ? f(this.v) : f.apply(this.v)) : this; }
    ifPresent(f) { if (this.isPresent()) (typeof f === 'function' ? f(this.v) : f.accept(this.v)); }
    getAsDouble() { return this.get(); }
    getAsInt() { return this.get(); }
}
J.Optional = Optional;
const fn = (f) => (typeof f === 'function' ? f : (x) => (f.apply || f.applyAsInt || f.applyAsDouble || f.test).call(f, x));
class Stream {
    constructor(a) { this.a = a; }
    map(f) { return new Stream(this.a.map(fn(f))); }
    mapToInt(f) { return new Stream(this.a.map(fn(f))); }
    mapToDouble(f) { return new Stream(this.a.map(fn(f))); }
    mapToObj(f) { return new Stream(this.a.map(fn(f))); }
    filter(f) { return new Stream(this.a.filter(fn(f))); }
    sorted(cmp) { const b = this.a.slice(); J.$sort(b, cmp); return new Stream(b); }
    limit(n) { return new Stream(this.a.slice(0, n)); }
    count() { return this.a.length; }
    sum() { return this.a.reduce((s, x) => s + x, 0); }
    average() { return new Optional(this.a.length ? this.a.reduce((s, x) => s + x, 0) / this.a.length : null); }
    max(cmp) {
        if (!this.a.length) return new Optional(null);
        const c = cmp ? (typeof cmp === 'function' ? cmp : (x, y) => cmp.compare(x, y)) : J.$compare;
        let best = this.a[0];
        for (let i = 1; i < this.a.length; i++) if (c(best, this.a[i]) < 0) best = this.a[i];
        return new Optional(best);
    }
    min(cmp) {
        if (!this.a.length) return new Optional(null);
        const c = cmp ? (typeof cmp === 'function' ? cmp : (x, y) => cmp.compare(x, y)) : J.$compare;
        let best = this.a[0];
        for (let i = 1; i < this.a.length; i++) if (c(best, this.a[i]) > 0) best = this.a[i];
        return new Optional(best);
    }
    anyMatch(f) { return this.a.some(fn(f)); }
    allMatch(f) { return this.a.every(fn(f)); }
    forEach(f) { this.a.forEach(fn(f)); }
    findFirst() { return new Optional(this.a.length ? this.a[0] : null); }
    toArray() { return this.a.slice(); }
    collect() { return new ArrayList(this.a); }
    toList() { return new ArrayList(this.a); }
}
J.Stream = Stream;
})();
