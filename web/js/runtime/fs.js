// PongPing web runtime - resources (the files packed in the desktop JAR), a small persistent
// file system for settings/progress/history (localStorage), and java.io streams.
'use strict';
(function () {

// ---------------------------------------------------------------- resources

/**
 * Resources are preloaded before the game starts (see main.js), so the synchronous Java code
 * (getResourceAsStream, ImageIO.read, Font.createFont) can use them directly.
 */
J.$resources = {
    text: new Map(),    // path -> string
    binary: new Map(),  // path -> ArrayBuffer
    images: new Map(),  // path -> BufferedImage
    fonts: new Map(),   // path -> {family, metrics}
};

class ResourceInputStream {
    constructor(path) { this.path = path; this.pos = 0; }
    $text() {
        const r = J.$resources;
        if (r.text.has(this.path)) return r.text.get(this.path);
        if (r.binary.has(this.path)) return new TextDecoder('utf-8').decode(r.binary.get(this.path));
        return '';
    }
    close() {}
    read() { return -1; }
    available() { return 0; }
}

class URL {
    constructor(spec) { this.spec = spec; }
    getPath() { return this.spec.replace(/^[a-z]+:/, ''); }
    getProtocol() { const m = /^([a-z]+):/.exec(this.spec); return m ? m[1] : ''; }
    toURI() { return this; }
    toString() { return this.spec; }
    openStream() { return new ResourceInputStream(this.getPath().replace(/^\/res\//, '')); }
}
J.URL = URL;
J.URI = URL;

class ClassLoader {
    static $has(path) {
        const r = J.$resources;
        return r.text.has(path) || r.binary.has(path) || r.images.has(path) || r.fonts.has(path);
    }
    getResourceAsStream(path) {
        path = path.replace(/^\//, '');
        return ClassLoader.$has(path) ? new ResourceInputStream(path) : null;
    }
    getResource(path) {
        path = path.replace(/^\//, '');
        return ClassLoader.$has(path) ? new URL('res:/res/' + path) : null;
    }
}
ClassLoader.$instance = new ClassLoader();
J.ClassLoader = ClassLoader;

// ---------------------------------------------------------------- persistent files (localStorage)

const FS_PREFIX = 'pongping:file:';
const DIR_PREFIX = 'pongping:dir:';
const memoryStore = new Map(); // fallback when localStorage is not available
const store = {
    get(k) { try { const v = localStorage.getItem(k); return v; } catch (e) { return memoryStore.has(k) ? memoryStore.get(k) : null; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) { memoryStore.set(k, v); } },
    remove(k) { try { localStorage.removeItem(k); } catch (e) { memoryStore.delete(k); } },
    keys() {
        try { return Object.keys(localStorage); } catch (e) { return Array.from(memoryStore.keys()); }
    },
};

function normPath(p) {
    p = String(p).replace(/\\/g, '/');
    if (!p.startsWith('/')) p = '/' + p;
    const out = [];
    for (const part of p.split('/')) {
        if (part === '' || part === '.') continue;
        if (part === '..') out.pop(); else out.push(part);
    }
    return '/' + out.join('/');
}

class File {
    constructor(a, b) {
        if (b !== undefined) {
            const parent = (a instanceof File) ? a.path : a;
            this.path = normPath(parent + '/' + b);
        } else {
            this.path = normPath(a);
        }
        this.orig = (b === undefined && typeof a === 'string') ? a : this.path;
    }
    $isResourcePath() {
        const rel = this.orig.replace(/^\//, '');
        return ClassLoader.$has(rel);
    }
    exists() {
        return store.get(FS_PREFIX + this.path) !== null || this.isDirectory();
    }
    isFile() { return store.get(FS_PREFIX + this.path) !== null; }
    isDirectory() {
        if (store.get(DIR_PREFIX + this.path) !== null) return true;
        return false;
    }
    mkdirs() {
        let p = '';
        for (const part of this.path.split('/').filter(Boolean)) {
            p += '/' + part;
            store.set(DIR_PREFIX + p, '1');
        }
        return true;
    }
    mkdir() { return this.mkdirs(); }
    getAbsolutePath() { return this.path; }
    getPath() { return this.orig; }
    getCanonicalPath() { return this.path; }
    getName() { const i = this.path.lastIndexOf('/'); return this.path.substring(i + 1); }
    getParent() { const i = this.path.lastIndexOf('/'); return i > 0 ? this.path.substring(0, i) : '/'; }
    getParentFile() { return new File(this.getParent()); }
    delete() { const had = this.exists(); store.remove(FS_PREFIX + this.path); store.remove(DIR_PREFIX + this.path); return had; }
    length() { const v = store.get(FS_PREFIX + this.path); return v === null ? 0 : new TextEncoder().encode(v).length; }
    listFiles(filter) {
        if (!this.isDirectory()) return null;
        const prefix = FS_PREFIX + this.path + '/';
        const res = [];
        for (const k of store.keys()) {
            if (k.startsWith(prefix) && k.indexOf('/', prefix.length) < 0) {
                const f = new File(k.substring(FS_PREFIX.length));
                if (!filter) res.push(f);
                else if (typeof filter === 'function' ? (filter.length === 2 ? filter(this, f.getName()) : filter(f)) : (filter.accept.length === 2 ? filter.accept(this, f.getName()) : filter.accept(f))) res.push(f);
            }
        }
        return res;
    }
    toString() { return this.orig; }
    $read() { return store.get(FS_PREFIX + this.path); }
    $write(text) { this.getParentFile().mkdirs(); store.set(FS_PREFIX + this.path, text); }
}
File.separator = '/';
File.separatorChar = '/';
File.pathSeparator = ':';
J.File = File;

function toFile(f) { return f instanceof File ? f : new File(f); }

// ---------------------------------------------------------------- streams (text based)

class FileInputStream {
    constructor(f) {
        this.file = toFile(f);
        const t = this.file.$read();
        if (t === null) throw new J.FileNotFoundException(this.file.getPath() + ' (No such file or directory)');
        this.text = t;
    }
    $text() { return this.text; }
    close() {}
}
J.FileInputStream = FileInputStream;

class FileOutputStream {
    constructor(f, append) {
        this.file = toFile(f);
        this.buf = append ? (this.file.$read() || '') : '';
        this.file.$write(this.buf);
    }
    $append(s) { this.buf += s; this.file.$write(this.buf); }
    write() { /* raw bytes are not used by the game */ }
    flush() {}
    close() {}
}
J.FileOutputStream = FileOutputStream;

class InputStreamReader {
    constructor(stream) { this.stream = stream; }
    $text() { return this.stream.$text(); }
    close() {}
}
J.InputStreamReader = InputStreamReader;

class FileReader {
    constructor(f) { this.inner = new FileInputStream(f); }
    $text() { return this.inner.$text(); }
    close() {}
}
J.FileReader = FileReader;

class BufferedReader {
    constructor(reader) {
        const t = reader.$text();
        this.lines = t.split(/\r\n|\n|\r/);
        if (this.lines.length && this.lines[this.lines.length - 1] === '') this.lines.pop();
        this.i = 0;
    }
    readLine() { return this.i < this.lines.length ? this.lines[this.i++] : null; }
    lines() { return new J.Stream(this.lines.slice(this.i)); }
    ready() { return this.i < this.lines.length; }
    close() {}
}
J.BufferedReader = BufferedReader;

class FileWriter {
    constructor(f, append) { this.out = new FileOutputStream(f, !!append); }
    $append(s) { this.out.$append(s); }
    write(s) { this.$append(typeof s === 'string' ? s : String.fromCharCode(s)); }
    flush() {}
    close() {}
}
J.FileWriter = FileWriter;

class PrintWriter {
    constructor(target) { this.target = (target instanceof File || typeof target === 'string') ? new FileWriter(target, false) : target; }
    println(x) { this.target.$append((arguments.length ? J.$str(x) : '') + '\n'); }
    print(x) { this.target.$append(J.$str(x)); }
    printf(fmt, ...a) { this.target.$append(J.$format(fmt, ...a)); return this; }
    write(s) { this.target.$append(s); }
    flush() {}
    close() {}
}
J.PrintWriter = PrintWriter;
J.BufferedWriter = PrintWriter;

// ---------------------------------------------------------------- java.util.Properties

function escapeProp(s, isKey) {
    let out = '';
    for (let i = 0; i < s.length; i++) {
        const c = s[i];
        switch (c) {
            case ' ': out += (i === 0 || isKey) ? '\\ ' : ' '; break;
            case '\t': out += '\\t'; break;
            case '\n': out += '\\n'; break;
            case '\r': out += '\\r'; break;
            case '\f': out += '\\f'; break;
            case '=': case ':': case '#': case '!': case '\\': out += '\\' + c; break;
            default: {
                const code = c.charCodeAt(0);
                out += (code < 0x20 || code > 0x7e) ? '\\u' + code.toString(16).toUpperCase().padStart(4, '0') : c;
            }
        }
    }
    return out;
}
function unescapeProp(s) {
    return s.replace(/\\(u[0-9a-fA-F]{4}|.)/g, (m, g) => {
        if (g[0] === 'u' && g.length === 5) return String.fromCharCode(parseInt(g.substring(1), 16));
        switch (g) { case 't': return '\t'; case 'n': return '\n'; case 'r': return '\r'; case 'f': return '\f'; }
        return g;
    });
}
class Properties extends J.HashMap {
    getProperty(k, def) { const v = this.get(k); return v === null ? (def === undefined ? null : def) : v; }
    setProperty(k, v) { return this.put(k, v); }
    stringPropertyNames() { return new J.HashSet(this.keySet()); }
    load(stream) {
        const text = stream.$text();
        const raw = text.split(/\r\n|\n|\r/);
        for (let i = 0; i < raw.length; i++) {
            let line = raw[i].replace(/^[ \t\f]+/, '');
            if (line === '' || line[0] === '#' || line[0] === '!') continue;
            while (/(^|[^\\])(\\\\)*\\$/.test(line) && i + 1 < raw.length) line = line.slice(0, -1) + raw[++i].replace(/^[ \t\f]+/, '');
            const m = /^((?:\\.|[^=: \t\f\\])*)[ \t\f]*[=:]?[ \t\f]*(.*)$/.exec(line);
            if (m) this.put(unescapeProp(m[1]), unescapeProp(m[2]));
        }
    }
    store(out, comments) {
        let s = '';
        if (comments !== null && comments !== undefined) s += '#' + comments + '\n';
        s += '#' + new Date().toString() + '\n';
        for (const e of this.entrySet()) s += escapeProp(e.getKey(), true) + '=' + escapeProp(e.getValue(), false) + '\n';
        if (out instanceof FileOutputStream || out instanceof FileWriter) out.$append(s);
        else if (out && out.$append) out.$append(s);
    }
}
J.Properties = Properties;

// ---------------------------------------------------------------- object serialization (JSON)

function serialize(v) {
    if (v === null || v === undefined) return null;
    if (typeof v !== 'object') return v;
    if (Array.isArray(v)) return { $array: v.map(serialize) };
    if (v instanceof J.HashSet) return { $set: Array.from(v, serialize) };
    if (v instanceof J.ArrayList) return { $list: v.a.map(serialize) };
    if (v instanceof J.HashMap) return { $map: Array.from(v.entrySet(), e => [serialize(e.getKey()), serialize(e.getValue())]) };
    if (v instanceof J.$Enum) return { $enum: v.constructor.name, name: v.name() };
    const o = { $class: v.constructor.name };
    for (const k of Object.keys(v)) o[k] = serialize(v[k]);
    return o;
}
function deserialize(v) {
    if (v === null || typeof v !== 'object') return v;
    if (v.$array) return v.$array.map(deserialize);
    if (v.$set) return new J.HashSet(v.$set.map(deserialize));
    if (v.$list) return new J.ArrayList(v.$list.map(deserialize));
    if (v.$map) { const m = new J.HashMap(); for (const [k, x] of v.$map) m.put(deserialize(k), deserialize(x)); return m; }
    if (v.$enum) { const cls = J.$userClasses[v.$enum]; return J.$enumValueOf(cls, v.name); }
    const cls = J.$userClasses[v.$class];
    if (!cls) throw new J.ClassNotFoundException(v.$class);
    const o = Object.create(cls.prototype);
    for (const k of Object.keys(v)) if (k !== '$class') o[k] = deserialize(v[k]);
    return o;
}
J.$userClasses = {};

class ObjectOutputStream {
    constructor(out) { this.out = out; }
    writeObject(o) { this.out.$append(JSON.stringify(serialize(o))); }
    flush() {}
    close() {}
}
class ObjectInputStream {
    constructor(input) { this.text = input.$text(); }
    readObject() {
        let data;
        try { data = JSON.parse(this.text); } catch (e) { throw new J.IOException('invalid stream header'); }
        return deserialize(data);
    }
    close() {}
}
J.ObjectOutputStream = ObjectOutputStream;
J.ObjectInputStream = ObjectInputStream;

// ---------------------------------------------------------------- javax.imageio / jar

J.ImageIO = {
    read(src) {
        let path = null;
        if (src instanceof ResourceInputStream) path = src.path;
        else if (src instanceof URL) path = src.getPath().replace(/^\/res\//, '');
        else if (src instanceof File) path = src.orig.replace(/^\//, '');
        if (path !== null && J.$resources.images.has(path)) return J.$resources.images.get(path);
        if (src instanceof File) throw new J.IOException("Can't read input file!");
        return null;
    },
};

class JarFile {
    constructor() { throw new J.IOException('JAR files are not available in the browser'); }
}
J.JarFile = JarFile;
})();
