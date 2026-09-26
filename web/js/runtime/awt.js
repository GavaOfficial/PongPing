// PongPing web runtime - java.awt emulation on top of the HTML canvas.
// Rendering follows Java2D's rules where they change what is on screen: stroke normalization
// (shapes snapped to pixel centres when antialiasing is on), Java's shape geometry (ellipse,
// round-rect and arc curves), integer-pixel clip rectangles, gradient interpolation in
// non-premultiplied colour, and text laid out with the font's hinted advances.
'use strict';
(function () {

// ---------------------------------------------------------------- Color

const f32 = Math.fround;
class Color {
    constructor(r, g, b, a) {
        if (g === undefined) {                 // Color(int rgb)
            this.value = (0xff000000 | r) >>> 0;
        } else if (typeof g === 'boolean') {   // Color(int rgba, boolean hasAlpha)
            this.value = (g ? r : (0xff000000 | r)) >>> 0;
        } else {
            if (a === undefined) a = 255;
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 255) {
                const bad = [];
                if (a < 0 || a > 255) bad.push('Alpha');
                if (r < 0 || r > 255) bad.push('Red');
                if (g < 0 || g > 255) bad.push('Green');
                if (b < 0 || b > 255) bad.push('Blue');
                throw new J.IllegalArgumentException('Color parameter outside of expected range: ' + bad.join(' '));
            }
            this.value = (((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)) >>> 0;
        }
        this.$css = null;
    }
    static $fromFloats(r, g, b, a) {
        if (a === undefined) a = 1;
        for (const v of [r, g, b, a]) if (v < 0 || v > 1) throw new J.IllegalArgumentException('Color parameter outside of expected range');
        return new Color(J.$i(r * 255 + 0.5), J.$i(g * 255 + 0.5), J.$i(b * 255 + 0.5), J.$i(a * 255 + 0.5));
    }
    getRed() { return (this.value >>> 16) & 0xff; }
    getGreen() { return (this.value >>> 8) & 0xff; }
    getBlue() { return this.value & 0xff; }
    getAlpha() { return (this.value >>> 24) & 0xff; }
    getRGB() { return this.value | 0; }
    getTransparency() { const a = this.getAlpha(); return a === 255 ? 1 : a === 0 ? 2 : 3; }
    brighter() {
        let r = this.getRed(), g = this.getGreen(), b = this.getBlue();
        const alpha = this.getAlpha();
        const i = J.$i(1.0 / (1.0 - 0.7));
        if (r === 0 && g === 0 && b === 0) return new Color(i, i, i, alpha);
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;
        return new Color(Math.min(J.$i(r / 0.7), 255), Math.min(J.$i(g / 0.7), 255), Math.min(J.$i(b / 0.7), 255), alpha);
    }
    darker() {
        return new Color(Math.max(J.$i(this.getRed() * 0.7), 0), Math.max(J.$i(this.getGreen() * 0.7), 0),
            Math.max(J.$i(this.getBlue() * 0.7), 0), this.getAlpha());
    }
    equals(o) { return o instanceof Color && o.value === this.value; }
    hashCode() { return this.value | 0; }
    $mapKey() { return 'Color:' + this.value; }
    toString() { return 'java.awt.Color[r=' + this.getRed() + ',g=' + this.getGreen() + ',b=' + this.getBlue() + ']'; }
    $cssColor() {
        if (this.$css === null) this.$css = 'rgba(' + this.getRed() + ',' + this.getGreen() + ',' + this.getBlue() + ',' + (this.getAlpha() / 255) + ')';
        return this.$css;
    }
    static HSBtoRGB(hue, saturation, brightness) {
        hue = f32(hue); saturation = f32(saturation); brightness = f32(brightness);
        let r = 0, g = 0, b = 0;
        const c255 = (v) => J.$i(f32(f32(v * 255) + 0.5));
        if (saturation === 0) {
            r = g = b = c255(brightness);
        } else {
            const h = f32(f32(hue - f32(Math.floor(hue))) * 6);
            const f = f32(h - f32(Math.floor(h)));
            const p = f32(brightness * f32(1 - saturation));
            const q = f32(brightness * f32(1 - f32(saturation * f)));
            const t = f32(brightness * f32(1 - f32(saturation * f32(1 - f))));
            switch (J.$i(h)) {
                case 0: r = c255(brightness); g = c255(t); b = c255(p); break;
                case 1: r = c255(q); g = c255(brightness); b = c255(p); break;
                case 2: r = c255(p); g = c255(brightness); b = c255(t); break;
                case 3: r = c255(p); g = c255(q); b = c255(brightness); break;
                case 4: r = c255(t); g = c255(p); b = c255(brightness); break;
                case 5: r = c255(brightness); g = c255(p); b = c255(q); break;
            }
        }
        return (0xff000000 | (r << 16) | (g << 8) | b) | 0;
    }
    static RGBtoHSB(r, g, b, hsbvals) {
        let hue, saturation, brightness;
        if (!hsbvals) hsbvals = [0, 0, 0];
        let cmax = Math.max(r, g, b), cmin = Math.min(r, g, b);
        brightness = f32(cmax / 255);
        saturation = cmax !== 0 ? f32((cmax - cmin) / cmax) : 0;
        if (saturation === 0) hue = 0;
        else {
            const redc = f32((cmax - r) / (cmax - cmin));
            const greenc = f32((cmax - g) / (cmax - cmin));
            const bluec = f32((cmax - b) / (cmax - cmin));
            if (r === cmax) hue = f32(bluec - greenc);
            else if (g === cmax) hue = f32(f32(2 + redc) - bluec);
            else hue = f32(f32(4 + greenc) - redc);
            hue = f32(hue / 6);
            if (hue < 0) hue = f32(hue + 1);
        }
        hsbvals[0] = hue; hsbvals[1] = saturation; hsbvals[2] = brightness;
        return hsbvals;
    }
    static getHSBColor(h, s, b) { return new Color(Color.HSBtoRGB(h, s, b)); }
    static decode(s) { return new Color(parseInt(s.replace(/^#|^0x/i, ''), 16)); }
}
const named = {
    white: [255, 255, 255], lightGray: [192, 192, 192], gray: [128, 128, 128], darkGray: [64, 64, 64],
    black: [0, 0, 0], red: [255, 0, 0], pink: [255, 175, 175], orange: [255, 200, 0], yellow: [255, 255, 0],
    green: [0, 255, 0], magenta: [255, 0, 255], cyan: [0, 255, 255], blue: [0, 0, 255],
};
for (const [k, v] of Object.entries(named)) {
    const c = new Color(v[0], v[1], v[2]);
    Color[k] = c;
    Color[k.replace(/([A-Z])/g, '_$1').toUpperCase()] = c;
}
J.Color = Color;

// ---------------------------------------------------------------- geometry

class Point {
    constructor(x, y) { this.x = x || 0; this.y = y || 0; }
    getX() { return this.x; }
    getY() { return this.y; }
    getLocation() { return new Point(this.x, this.y); }
    setLocation(x, y) { if (x instanceof Point) { this.x = x.x; this.y = x.y; } else { this.x = J.$i(Math.floor(x + 0.5)); this.y = J.$i(Math.floor(y + 0.5)); } }
    distance(x, y) { if (x instanceof Object) { y = x.getY(); x = x.getX(); } return Math.hypot(this.x - x, this.y - y); }
    equals(o) { return o && o.x === this.x && o.y === this.y; }
    toString() { return 'java.awt.Point[x=' + this.x + ',y=' + this.y + ']'; }
}
J.Point = Point;
class Point2D {
    constructor(x, y) { this.x = x || 0; this.y = y || 0; }
    getX() { return this.x; }
    getY() { return this.y; }
    setLocation(x, y) { this.x = x; this.y = y; }
    distance(x, y) { if (x instanceof Object) { y = x.getY(); x = x.getX(); } return Math.hypot(this.x - x, this.y - y); }
}
Point2D.Double = class extends Point2D {};
Point2D.Float = class extends Point2D {
    constructor(x, y) { super(f32(x || 0), f32(y || 0)); }
};
J.Point2D = Point2D;

class Dimension {
    constructor(w, h) {
        if (w instanceof Dimension) { h = w.height; w = w.width; }
        this.width = w || 0; this.height = h || 0;
    }
    getWidth() { return this.width; }
    getHeight() { return this.height; }
    setSize(w, h) { this.width = J.$i(Math.ceil(w)); this.height = J.$i(Math.ceil(h)); }
    equals(o) { return o instanceof Dimension && o.width === this.width && o.height === this.height; }
    toString() { return 'java.awt.Dimension[width=' + this.width + ',height=' + this.height + ']'; }
}
J.Dimension = Dimension;

/** Base for shapes: $path() returns {cmds, evenOdd} in user space. */
class Shape {
    getBounds() { const b = this.getBounds2D(); return new Rectangle(Math.floor(b.x), Math.floor(b.y), Math.ceil(b.x + b.width) - Math.floor(b.x), Math.ceil(b.y + b.height) - Math.floor(b.y)); }
}

class Rectangle2D extends Shape {
    constructor(x, y, w, h) { super(); this.x = x || 0; this.y = y || 0; this.width = w || 0; this.height = h || 0; }
    getX() { return this.x; }
    getY() { return this.y; }
    getWidth() { return this.width; }
    getHeight() { return this.height; }
    getMinX() { return this.x; }
    getMinY() { return this.y; }
    getMaxX() { return this.x + this.width; }
    getMaxY() { return this.y + this.height; }
    getCenterX() { return this.x + this.width / 2; }
    getCenterY() { return this.y + this.height / 2; }
    isEmpty() { return this.width <= 0 || this.height <= 0; }
    contains(x, y) {
        if (x instanceof Object) { y = x.getY(); x = x.getX(); }
        return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
    }
    intersects(x, y, w, h) {
        if (x instanceof Object) { const r = x; x = r.getX(); y = r.getY(); w = r.getWidth(); h = r.getHeight(); }
        if (this.isEmpty() || w <= 0 || h <= 0) return false;
        return x + w > this.x && y + h > this.y && x < this.x + this.width && y < this.y + this.height;
    }
    getBounds2D() { return new Rectangle2D.Double(this.x, this.y, this.width, this.height); }
    $path() {
        const x = this.x, y = this.y, w = this.width, h = this.height;
        if (w < 0 || h < 0) return { cmds: [], evenOdd: false };
        return { cmds: [['M', x, y], ['L', x + w, y], ['L', x + w, y + h], ['L', x, y + h], ['Z']], evenOdd: false, rect: true };
    }
}
Rectangle2D.Double = class extends Rectangle2D {};
Rectangle2D.Float = class extends Rectangle2D {
    constructor(x, y, w, h) { super(f32(x || 0), f32(y || 0), f32(w || 0), f32(h || 0)); }
};
J.Rectangle2D = Rectangle2D;

class Rectangle extends Rectangle2D {
    constructor(x, y, w, h) {
        if (x instanceof Dimension) super(0, 0, x.width, x.height);
        else if (w === undefined && x !== undefined && y !== undefined) super(0, 0, x, y);
        else super(x || 0, y || 0, w || 0, h || 0);
    }
    getBounds() { return new Rectangle(this.x, this.y, this.width, this.height); }
    getLocation() { return new Point(this.x, this.y); }
    getSize() { return new Dimension(this.width, this.height); }
    setBounds(x, y, w, h) { this.x = x; this.y = y; this.width = w; this.height = h; }
    translate(dx, dy) { this.x += dx; this.y += dy; }
    toString() { return 'java.awt.Rectangle[x=' + this.x + ',y=' + this.y + ',width=' + this.width + ',height=' + this.height + ']'; }
}
J.Rectangle = Rectangle;

const ELLIPSE_CV = 0.5522847498307933;
const PCV = 0.5 + ELLIPSE_CV * 0.5, NCV = 0.5 - ELLIPSE_CV * 0.5;
function ellipseCmds(x, y, w, h) {
    if (w < 0 || h < 0) return [];
    const P = (a, b) => [x + a * w, y + b * h];
    return [
        ['M', ...P(1, 0.5)],
        ['C', ...P(1, PCV), ...P(PCV, 1), ...P(0.5, 1)],
        ['C', ...P(NCV, 1), ...P(0, PCV), ...P(0, 0.5)],
        ['C', ...P(0, NCV), ...P(NCV, 0), ...P(0.5, 0)],
        ['C', ...P(PCV, 0), ...P(1, NCV), ...P(1, 0.5)],
        ['Z'],
    ];
}
class Ellipse2D extends Rectangle2D {
    $path() { return { cmds: ellipseCmds(this.x, this.y, this.width, this.height), evenOdd: false }; }
    contains(px, py) {
        const w = this.width, h = this.height;
        if (w <= 0 || h <= 0) return false;
        const nx = (px - this.x) / w - 0.5, ny = (py - this.y) / h - 0.5;
        return nx * nx + ny * ny < 0.25;
    }
}
Ellipse2D.Double = class extends Ellipse2D {};
Ellipse2D.Float = class extends Ellipse2D {
    constructor(x, y, w, h) { super(f32(x), f32(y), f32(w), f32(h)); }
};
J.Ellipse2D = Ellipse2D;

// java.awt.geom.RoundRectIterator
const RR_ANGLE = Math.PI / 4;
const RR_A = 1 - Math.cos(RR_ANGLE);
const RR_B = Math.tan(RR_ANGLE);
const RR_C = Math.sqrt(1 + RR_B * RR_B) - 1 + RR_A;
const RR_CV = 4 / 3 * RR_A * RR_B / RR_C;
const RR_ACV = (1 - RR_CV) / 2;
const RR_CTRL = [
    [0, 0, 0, 0.5],
    [0, 0, 1, -0.5],
    [0, 0, 1, -RR_ACV, 0, RR_ACV, 1, 0, 0, 0.5, 1, 0],
    [1, -0.5, 1, 0],
    [1, -RR_ACV, 1, 0, 1, 0, 1, -RR_ACV, 1, 0, 1, -0.5],
    [1, 0, 0, 0.5],
    [1, 0, 0, RR_ACV, 1, -RR_ACV, 0, 0, 1, -0.5, 0, 0],
    [0, 0.5, 0, 0],
    [0, RR_ACV, 0, 0, 0, 0, 0, RR_ACV, 0, 0, 0, 0.5],
];
const RR_TYPES = ['M', 'L', 'C', 'L', 'C', 'L', 'C', 'L', 'C'];
function roundRectCmds(x, y, w, h, aw, ah) {
    if (w < 0 || h < 0) return [];
    aw = Math.min(w, Math.abs(aw));
    ah = Math.min(h, Math.abs(ah));
    const cmds = [];
    for (let i = 0; i < RR_CTRL.length; i++) {
        const c = RR_CTRL[i];
        const pts = [];
        for (let k = 0; k < c.length; k += 4) pts.push(x + c[k] * w + c[k + 1] * aw, y + c[k + 2] * h + c[k + 3] * ah);
        cmds.push([RR_TYPES[i], ...pts]);
    }
    cmds.push(['Z']);
    return cmds;
}
class RoundRectangle2D extends Rectangle2D {
    constructor(x, y, w, h, aw, ah) { super(x, y, w, h); this.arcwidth = aw || 0; this.archeight = ah || 0; }
    getArcWidth() { return this.arcwidth; }
    getArcHeight() { return this.archeight; }
    $path() { return { cmds: roundRectCmds(this.x, this.y, this.width, this.height, this.arcwidth, this.archeight), evenOdd: false }; }
}
RoundRectangle2D.Double = class extends RoundRectangle2D {};
RoundRectangle2D.Float = class extends RoundRectangle2D {
    constructor(x, y, w, h, aw, ah) { super(f32(x), f32(y), f32(w), f32(h), f32(aw), f32(ah)); }
};
J.RoundRectangle2D = RoundRectangle2D;

// java.awt.geom.ArcIterator
function arcCmds(ax, ay, aw, ah, start, extent, type) {
    const w = aw / 2, h = ah / 2, x = ax + w, y = ay + h;
    const angStRad = -start / 180 * Math.PI;
    const ext = -extent;
    let arcSegs, increment, cv;
    if (ext >= 360 || ext <= -360) {
        arcSegs = 4; increment = Math.PI / 2; cv = 0.5522847498307933;
        if (ext < 0) { increment = -increment; cv = -cv; }
    } else {
        arcSegs = Math.ceil(Math.abs(ext) / 90);
        increment = (ext / arcSegs) / 180 * Math.PI;
        const half = increment / 2;
        cv = 4 / 3 * Math.sin(half) / (1 + Math.cos(half));
        if (cv === 0) arcSegs = 0;
    }
    const lineSegs = type === 0 ? 0 : type === 1 ? 1 : 2;
    if (w < 0 || h < 0) return [];
    const cmds = [['M', x + Math.cos(angStRad) * w, y + Math.sin(angStRad) * h]];
    let angle = angStRad;
    for (let i = 1; i <= arcSegs; i++) {
        let relx = Math.cos(angle), rely = Math.sin(angle);
        const c1x = x + (relx - cv * rely) * w, c1y = y + (rely + cv * relx) * h;
        angle += increment;
        relx = Math.cos(angle); rely = Math.sin(angle);
        cmds.push(['C', c1x, c1y, x + (relx + cv * rely) * w, y + (rely - cv * relx) * h, x + relx * w, y + rely * h]);
    }
    if (lineSegs === 2) cmds.push(['L', x, y]);
    if (lineSegs >= 1) cmds.push(['Z']);
    return cmds;
}
class Arc2D extends Rectangle2D {
    constructor(x, y, w, h, start, extent, type) {
        super(x, y, w, h);
        this.start = start; this.extent = extent; this.type = type || 0;
    }
    getAngleStart() { return this.start; }
    getAngleExtent() { return this.extent; }
    $path() { return { cmds: arcCmds(this.x, this.y, this.width, this.height, this.start, this.extent, this.type), evenOdd: false }; }
}
Arc2D.OPEN = 0; Arc2D.CHORD = 1; Arc2D.PIE = 2;
Arc2D.Double = class extends Arc2D {};
Arc2D.Float = class extends Arc2D {};
J.Arc2D = Arc2D;

class Line2D extends Shape {
    constructor(x1, y1, x2, y2) { super(); this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; }
    $path() { return { cmds: [['M', this.x1, this.y1], ['L', this.x2, this.y2]], evenOdd: false }; }
}
Line2D.Double = class extends Line2D {};
Line2D.Float = class extends Line2D {};
J.Line2D = Line2D;

class Polygon extends Shape {
    constructor(xs, ys, n) {
        super();
        this.npoints = n || 0;
        this.xpoints = xs ? xs.slice(0, this.npoints) : [];
        this.ypoints = ys ? ys.slice(0, this.npoints) : [];
    }
    addPoint(x, y) { this.xpoints.push(x); this.ypoints.push(y); this.npoints++; this.$bounds = null; }
    getBoundingBox() {
        if (this.npoints === 0) return new Rectangle();
        let minx = Infinity, miny = Infinity, maxx = -Infinity, maxy = -Infinity;
        for (let i = 0; i < this.npoints; i++) {
            minx = Math.min(minx, this.xpoints[i]); maxx = Math.max(maxx, this.xpoints[i]);
            miny = Math.min(miny, this.ypoints[i]); maxy = Math.max(maxy, this.ypoints[i]);
        }
        return new Rectangle(minx, miny, maxx - minx, maxy - miny);
    }
    getBounds() { return this.getBoundingBox(); }
    getBounds2D() { return this.getBoundingBox(); }
    contains(x, y) {
        if (x instanceof Object) { y = x.getY(); x = x.getX(); }
        const n = this.npoints, xp = this.xpoints, yp = this.ypoints;
        if (n <= 2 || !this.getBoundingBox().contains(x, y)) return false;
        let hits = 0;
        let lastx = xp[n - 1], lasty = yp[n - 1], curx, cury;
        for (let i = 0; i < n; lastx = curx, lasty = cury, i++) {
            curx = xp[i]; cury = yp[i];
            if (cury === lasty) continue;
            let leftx;
            if (curx < lastx) { if (x >= lastx) continue; leftx = curx; } else { if (x >= curx) continue; leftx = lastx; }
            let test1, test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) continue;
                if (x < leftx) { hits++; continue; }
                test1 = x - curx; test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) continue;
                if (x < leftx) { hits++; continue; }
                test1 = x - lastx; test2 = y - lasty;
            }
            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) hits++;
        }
        return (hits & 1) !== 0;
    }
    $path() {
        const cmds = [];
        for (let i = 0; i < this.npoints; i++) cmds.push([i === 0 ? 'M' : 'L', this.xpoints[i], this.ypoints[i]]);
        if (this.npoints) cmds.push(['Z']);
        return { cmds, evenOdd: true };
    }
}
J.Polygon = Polygon;

class GeneralPath extends Shape {
    constructor() { super(); this.cmds = []; }
    moveTo(x, y) { this.cmds.push(['M', x, y]); }
    lineTo(x, y) { this.cmds.push(['L', x, y]); }
    quadTo(x1, y1, x, y) { this.cmds.push(['Q', x1, y1, x, y]); }
    curveTo(a, b, c, d, e, f) { this.cmds.push(['C', a, b, c, d, e, f]); }
    closePath() { this.cmds.push(['Z']); }
    $path() { return { cmds: this.cmds, evenOdd: false }; }
}
J.GeneralPath = GeneralPath;
J.Path2D = { Double: GeneralPath, Float: GeneralPath };

// ---------------------------------------------------------------- AffineTransform

class AffineTransform {
    constructor(m00, m10, m01, m11, m02, m12) {
        if (m00 instanceof AffineTransform) { const t = m00; m00 = t.m00; m10 = t.m10; m01 = t.m01; m11 = t.m11; m02 = t.m02; m12 = t.m12; }
        if (m00 === undefined) { m00 = 1; m10 = 0; m01 = 0; m11 = 1; m02 = 0; m12 = 0; }
        this.m00 = m00; this.m10 = m10; this.m01 = m01; this.m11 = m11; this.m02 = m02; this.m12 = m12;
    }
    static getTranslateInstance(tx, ty) { return new AffineTransform(1, 0, 0, 1, tx, ty); }
    static getRotateInstance(theta, x, y) { const t = new AffineTransform(); t.rotate(theta, x, y); return t; }
    static getScaleInstance(sx, sy) { return new AffineTransform(sx, 0, 0, sy, 0, 0); }
    clone() { return new AffineTransform(this); }
    getScaleX() { return this.m00; }
    getScaleY() { return this.m11; }
    getShearX() { return this.m01; }
    getShearY() { return this.m10; }
    getTranslateX() { return this.m02; }
    getTranslateY() { return this.m12; }
    isIdentity() { return this.m00 === 1 && this.m10 === 0 && this.m01 === 0 && this.m11 === 1 && this.m02 === 0 && this.m12 === 0; }
    setToIdentity() { this.m00 = 1; this.m10 = 0; this.m01 = 0; this.m11 = 1; this.m02 = 0; this.m12 = 0; }
    setTransform(t) { this.m00 = t.m00; this.m10 = t.m10; this.m01 = t.m01; this.m11 = t.m11; this.m02 = t.m02; this.m12 = t.m12; }
    concatenate(t) {
        const a = this.m00, b = this.m10, c = this.m01, d = this.m11;
        this.m00 = a * t.m00 + c * t.m10;
        this.m10 = b * t.m00 + d * t.m10;
        this.m01 = a * t.m01 + c * t.m11;
        this.m11 = b * t.m01 + d * t.m11;
        this.m02 = a * t.m02 + c * t.m12 + this.m02;
        this.m12 = b * t.m02 + d * t.m12 + this.m12;
    }
    translate(tx, ty) {
        this.m02 = tx * this.m00 + ty * this.m01 + this.m02;
        this.m12 = tx * this.m10 + ty * this.m11 + this.m12;
    }
    scale(sx, sy) { this.m00 *= sx; this.m10 *= sx; this.m01 *= sy; this.m11 *= sy; }
    rotate(theta, x, y) {
        if (x !== undefined) this.translate(x, y);
        let sin = Math.sin(theta), cos = Math.cos(theta);
        // Java snaps quadrant rotations to exact values
        if (sin === 1 || sin === -1) cos = 0;
        if (cos === 1 || cos === -1) sin = 0;
        const a = this.m00, b = this.m10, c = this.m01, d = this.m11;
        this.m00 = cos * a + sin * c;
        this.m01 = -sin * a + cos * c;
        this.m10 = cos * b + sin * d;
        this.m11 = -sin * b + cos * d;
        if (x !== undefined) this.translate(-x, -y);
    }
    shear(shx, shy) {
        const a = this.m00, b = this.m10, c = this.m01, d = this.m11;
        this.m00 = a + c * shy; this.m10 = b + d * shy;
        this.m01 = a * shx + c; this.m11 = b * shx + d;
    }
    transform(src, dst) {
        const x = src.getX(), y = src.getY();
        const nx = this.m00 * x + this.m01 * y + this.m02, ny = this.m10 * x + this.m11 * y + this.m12;
        if (!dst) dst = new Point2D.Double();
        dst.setLocation(nx, ny);
        return dst;
    }
    createTransformedShape(s) {
        const t = this;
        const p = s.$path();
        const cmds = p.cmds.map(c => {
            const out = [c[0]];
            for (let i = 1; i < c.length; i += 2) out.push(t.m00 * c[i] + t.m01 * c[i + 1] + t.m02, t.m10 * c[i] + t.m11 * c[i + 1] + t.m12);
            return out;
        });
        const g = new GeneralPath();
        g.cmds = cmds;
        return g;
    }
    equals(o) { return o instanceof AffineTransform && o.m00 === this.m00 && o.m10 === this.m10 && o.m01 === this.m01 && o.m11 === this.m11 && o.m02 === this.m02 && o.m12 === this.m12; }
}
J.AffineTransform = AffineTransform;

// ---------------------------------------------------------------- paints, strokes, composites, hints

function lerpColor(c1, c2, t) {
    const L = (a, b) => a + (b - a) * t;
    return 'rgba(' + Math.round(L(c1.getRed(), c2.getRed())) + ',' + Math.round(L(c1.getGreen(), c2.getGreen())) + ','
        + Math.round(L(c1.getBlue(), c2.getBlue())) + ',' + (L(c1.getAlpha(), c2.getAlpha()) / 255) + ')';
}
/** Adds gradient stops, interpolating in non-premultiplied colour like Java when alpha changes. */
function addStops(grad, fractions, colors) {
    for (let i = 0; i < fractions.length; i++) {
        if (i > 0 && colors[i - 1].getAlpha() !== colors[i].getAlpha() && (colors[i - 1].value & 0xffffff) !== (colors[i].value & 0xffffff)) {
            const f0 = fractions[i - 1], f1 = fractions[i];
            for (let k = 1; k < 8; k++) grad.addColorStop(f0 + (f1 - f0) * k / 8, lerpColor(colors[i - 1], colors[i], k / 8));
        }
        grad.addColorStop(fractions[i], colors[i].$cssColor());
    }
}

class GradientPaint {
    constructor(x1, y1, c1, x2, y2, c2, cyclic) {
        if (x1 instanceof Object && !(typeof x1 === 'number')) {   // (Point2D, Color, Point2D, Color[, cyclic])
            const p1 = x1, col1 = y1, p2 = c1, col2 = x2; cyclic = y2;
            x1 = p1.getX(); y1 = p1.getY(); c1 = col1; x2 = p2.getX(); y2 = p2.getY(); c2 = col2;
        }
        this.x1 = f32(x1); this.y1 = f32(y1); this.x2 = f32(x2); this.y2 = f32(y2);
        this.c1 = c1; this.c2 = c2; this.cyclic = !!cyclic;
    }
    getColor1() { return this.c1; }
    getColor2() { return this.c2; }
    getPoint1() { return new Point2D.Float(this.x1, this.y1); }
    getPoint2() { return new Point2D.Float(this.x2, this.y2); }
    isCyclic() { return this.cyclic; }
    $canvasPaint(ctx, dev) {
        const [ax, ay] = dev.pt(this.x1, this.y1);
        const [bx, by] = dev.pt(this.x2, this.y2);
        if (ax === bx && ay === by) return this.c1.$cssColor();
        if (!this.cyclic) {
            const g = ctx.createLinearGradient(ax, ay, bx, by);
            addStops(g, [0, 1], [this.c1, this.c2]);
            return g;
        }
        // cyclic: c1 -> c2 -> c1 repeating; build a long gradient covering the canvas
        const dx = bx - ax, dy = by - ay;
        const span = Math.ceil((ctx.canvas.width + ctx.canvas.height) / Math.hypot(dx, dy)) + 2;
        const g = ctx.createLinearGradient(ax - dx * span * 2, ay - dy * span * 2, ax + dx * span * 2, ay + dy * span * 2);
        const total = span * 4;
        for (let k = 0; k <= total; k++) g.addColorStop(k / total, (k % 2 === 0 ? this.c1 : this.c2).$cssColor());
        return g;
    }
}
J.GradientPaint = GradientPaint;

class RadialGradientPaint {
    constructor(cx, cy, radius, fractions, colors) {
        if (cx instanceof Object) {   // (Point2D center, float radius, float[] fractions, Color[] colors)
            colors = fractions; fractions = radius; radius = cy; cy = cx.getY(); cx = cx.getX();
        }
        if (!(radius > 0)) throw new J.IllegalArgumentException('Radius must be greater than zero');
        if (fractions.length !== colors.length) throw new J.IllegalArgumentException('Colors and fractions must have equal size');
        for (let i = 0; i < fractions.length; i++) {
            if (fractions[i] < 0 || fractions[i] > 1) throw new J.IllegalArgumentException('Fraction values must be in the range 0 to 1: ' + fractions[i]);
            if (i > 0 && fractions[i] <= fractions[i - 1]) throw new J.IllegalArgumentException('Keyframe fractions must be increasing: ' + fractions[i]);
        }
        this.cx = f32(cx); this.cy = f32(cy); this.radius = f32(radius);
        this.fractions = fractions.slice(); this.colors = colors.slice();
    }
    $canvasPaint(ctx, dev) {
        const [x, y] = dev.pt(this.cx, this.cy);
        const r = this.radius * dev.scale;
        const g = ctx.createRadialGradient(x, y, 0, x, y, r);
        let fr = this.fractions, cols = this.colors;
        // Java pads with the end colours when the fractions do not start at 0 / end at 1
        if (fr[0] !== 0) { fr = [0, ...fr]; cols = [cols[0], ...cols]; }
        if (fr[fr.length - 1] !== 1) { fr = [...fr, 1]; cols = [...cols, cols[cols.length - 1]]; }
        addStops(g, fr, cols);
        return g;
    }
}
J.RadialGradientPaint = RadialGradientPaint;
J.MultipleGradientPaint = { CycleMethod: { NO_CYCLE: 0, REFLECT: 1, REPEAT: 2 } };

class LinearGradientPaint {
    constructor(x1, y1, x2, y2, fractions, colors) {
        if (x1 instanceof Object) { colors = x2; fractions = y2; const p1 = x1, p2 = y1; x1 = p1.getX(); y1 = p1.getY(); x2 = p2.getX(); y2 = p2.getY(); }
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.fractions = fractions; this.colors = colors;
    }
    $canvasPaint(ctx, dev) {
        const [ax, ay] = dev.pt(this.x1, this.y1), [bx, by] = dev.pt(this.x2, this.y2);
        const g = ctx.createLinearGradient(ax, ay, bx, by);
        addStops(g, this.fractions, this.colors);
        return g;
    }
}
J.LinearGradientPaint = LinearGradientPaint;

class BasicStroke {
    constructor(width, cap, join, miterlimit, dash, dashPhase) {
        this.width = width === undefined ? 1 : f32(width);
        this.cap = cap === undefined ? BasicStroke.CAP_SQUARE : cap;
        this.join = join === undefined ? BasicStroke.JOIN_MITER : join;
        this.miterlimit = miterlimit === undefined ? 10 : miterlimit;
        this.dash = dash || null;
        this.dashPhase = dashPhase || 0;
        if (this.width < 0) throw new J.IllegalArgumentException('negative width');
    }
    getLineWidth() { return this.width; }
    getEndCap() { return this.cap; }
    getLineJoin() { return this.join; }
    getMiterLimit() { return this.miterlimit; }
    getDashArray() { return this.dash ? this.dash.slice() : null; }
    getDashPhase() { return this.dashPhase; }
}
BasicStroke.CAP_BUTT = 0; BasicStroke.CAP_ROUND = 1; BasicStroke.CAP_SQUARE = 2;
BasicStroke.JOIN_MITER = 0; BasicStroke.JOIN_ROUND = 1; BasicStroke.JOIN_BEVEL = 2;
J.BasicStroke = BasicStroke;
const CAPS = ['butt', 'round', 'square'];
const JOINS = ['miter', 'round', 'bevel'];

class AlphaComposite {
    constructor(rule, alpha) { this.rule = rule; this.alpha = alpha === undefined ? 1 : f32(alpha); }
    static getInstance(rule, alpha) {
        if (alpha !== undefined && (alpha < 0 || alpha > 1)) throw new J.IllegalArgumentException('alpha value out of range');
        return new AlphaComposite(rule, alpha);
    }
    getAlpha() { return this.alpha; }
    getRule() { return this.rule; }
    derive(alpha) { return new AlphaComposite(this.rule, alpha); }
}
Object.assign(AlphaComposite, { CLEAR: 1, SRC: 2, SRC_OVER: 3, DST_OVER: 4, SRC_IN: 5, DST_IN: 6, SRC_OUT: 7, DST_OUT: 8, DST: 9, SRC_ATOP: 10, DST_ATOP: 11, XOR: 12 });
AlphaComposite.SrcOver = new AlphaComposite(3, 1);
AlphaComposite.Src = new AlphaComposite(2, 1);
AlphaComposite.Clear = new AlphaComposite(1, 1);
J.AlphaComposite = AlphaComposite;
const COMPOSITE_OPS = { 1: 'destination-out', 2: 'copy', 3: 'source-over', 4: 'destination-over', 5: 'source-in', 6: 'destination-in', 7: 'source-out', 8: 'destination-out', 9: 'source-over', 10: 'source-atop', 11: 'destination-atop', 12: 'xor' };

class HintKey { constructor(name) { this.name = name; } toString() { return this.name; } }
class HintValue { constructor(name) { this.name = name; } toString() { return this.name; } }
const RH = {};
for (const k of ['KEY_ANTIALIASING', 'KEY_TEXT_ANTIALIASING', 'KEY_RENDERING', 'KEY_INTERPOLATION', 'KEY_STROKE_CONTROL',
    'KEY_FRACTIONALMETRICS', 'KEY_ALPHA_INTERPOLATION', 'KEY_COLOR_RENDERING', 'KEY_DITHERING', 'KEY_TEXT_LCD_CONTRAST']) RH[k] = new HintKey(k);
for (const v of ['VALUE_ANTIALIAS_ON', 'VALUE_ANTIALIAS_OFF', 'VALUE_ANTIALIAS_DEFAULT', 'VALUE_TEXT_ANTIALIAS_ON',
    'VALUE_TEXT_ANTIALIAS_OFF', 'VALUE_TEXT_ANTIALIAS_DEFAULT', 'VALUE_TEXT_ANTIALIAS_GASP', 'VALUE_TEXT_ANTIALIAS_LCD_HRGB',
    'VALUE_RENDER_QUALITY', 'VALUE_RENDER_SPEED', 'VALUE_RENDER_DEFAULT', 'VALUE_INTERPOLATION_NEAREST_NEIGHBOR',
    'VALUE_INTERPOLATION_BILINEAR', 'VALUE_INTERPOLATION_BICUBIC', 'VALUE_STROKE_PURE', 'VALUE_STROKE_NORMALIZE',
    'VALUE_STROKE_DEFAULT', 'VALUE_FRACTIONALMETRICS_ON', 'VALUE_FRACTIONALMETRICS_OFF', 'VALUE_FRACTIONALMETRICS_DEFAULT',
    'VALUE_ALPHA_INTERPOLATION_QUALITY', 'VALUE_ALPHA_INTERPOLATION_SPEED', 'VALUE_ALPHA_INTERPOLATION_DEFAULT',
    'VALUE_COLOR_RENDER_QUALITY', 'VALUE_COLOR_RENDER_SPEED', 'VALUE_COLOR_RENDER_DEFAULT',
    'VALUE_DITHER_ENABLE', 'VALUE_DITHER_DISABLE', 'VALUE_DITHER_DEFAULT']) RH[v] = new HintValue(v);
const HINT_DEFAULTS = new Map([
    [RH.KEY_ANTIALIASING, RH.VALUE_ANTIALIAS_OFF],
    [RH.KEY_TEXT_ANTIALIASING, RH.VALUE_TEXT_ANTIALIAS_DEFAULT],
    [RH.KEY_RENDERING, RH.VALUE_RENDER_DEFAULT],
    [RH.KEY_STROKE_CONTROL, RH.VALUE_STROKE_DEFAULT],
    [RH.KEY_FRACTIONALMETRICS, RH.VALUE_FRACTIONALMETRICS_OFF],
    [RH.KEY_ALPHA_INTERPOLATION, RH.VALUE_ALPHA_INTERPOLATION_DEFAULT],
    [RH.KEY_COLOR_RENDERING, RH.VALUE_COLOR_RENDER_DEFAULT],
    [RH.KEY_DITHERING, RH.VALUE_DITHER_DEFAULT],
]);
J.RenderingHints = RH;

// ---------------------------------------------------------------- images

class BufferedImage {
    constructor(w, h, type) {
        if (w instanceof Object && !(typeof w === 'number')) {
            // wrap a decoded HTML image (resources)
            this.$src = w;
            this.width = w.naturalWidth || w.width;
            this.height = w.naturalHeight || w.height;
        } else {
            this.width = w; this.height = h; this.type = type;
            this.$src = document.createElement('canvas');
            this.$src.width = w; this.$src.height = h;
        }
        this.$pixels = null;
    }
    getWidth() { return this.width; }
    getHeight() { return this.height; }
    getType() { return this.type || BufferedImage.TYPE_INT_ARGB; }
    $data() {
        if (!this.$pixels) {
            const c = document.createElement('canvas');
            c.width = this.width; c.height = this.height;
            const ctx = c.getContext('2d');
            ctx.drawImage(this.$src, 0, 0);
            this.$pixels = ctx.getImageData(0, 0, this.width, this.height).data;
        }
        return this.$pixels;
    }
    getRGB(x, y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) throw new J.IndexOutOfBoundsException('Coordinate out of bounds!');
        const d = this.$data(), i = (y * this.width + x) * 4;
        return ((d[i + 3] << 24) | (d[i] << 16) | (d[i + 1] << 8) | d[i + 2]) | 0;
    }
    createGraphics() {
        this.$pixels = null;
        const ctx = this.$src.getContext('2d');
        return new Graphics2D(ctx, 1, this.width, this.height, null);
    }
    getGraphics() { return this.createGraphics(); }
    getScaledInstance(w, h) {
        const img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        const ctx = img.$src.getContext('2d');
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'high';
        ctx.drawImage(this.$src, 0, 0, w, h);
        return img;
    }
    getSubimage(x, y, w, h) {
        const img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.$src.getContext('2d').drawImage(this.$src, x, y, w, h, 0, 0, w, h);
        return img;
    }
    flush() {}
}
BufferedImage.TYPE_INT_RGB = 1; BufferedImage.TYPE_INT_ARGB = 2; BufferedImage.TYPE_INT_ARGB_PRE = 3;
BufferedImage.TYPE_4BYTE_ABGR = 6;
J.BufferedImage = BufferedImage;
J.Image = BufferedImage;
J.Image.SCALE_SMOOTH = 4; J.Image.SCALE_DEFAULT = 1; J.Image.SCALE_FAST = 2;

// ---------------------------------------------------------------- fonts

const FONT_PLAIN = 0, FONT_BOLD = 1, FONT_ITALIC = 2;
const strikeCache = new Map();
class Font {
    constructor(name, style, size) {
        if (name instanceof Font) { const f = name; name = f.name; style = f.style; size = f.pointSize; this.$ttf = f.$ttf; this.$file = f.$file; }
        this.name = name === null || name === undefined ? 'Default' : name;
        this.style = (style & ~3) === 0 ? style : 0;
        this.pointSize = size;
        this.size = J.$i(size + 0.5);
        if (this.$ttf === undefined) { this.$ttf = null; this.$file = null; }
        this.$metrics = null;
    }
    static createFont(format, stream) {
        const path = stream && stream.path ? stream.path : (stream instanceof J.File ? stream.orig.replace(/^\//, '') : null);
        const res = path !== null ? J.$resources.fonts.get(path) : null;
        if (!res) throw new J.FontFormatException('Font file not available: ' + path);
        const f = new Font(res.name, FONT_PLAIN, 1);
        f.$ttf = res.ttf;
        f.$file = path;
        return f;
    }
    deriveFont(a, b) {
        // deriveFont(float size) | deriveFont(int style, float size)
        const f = b === undefined ? new Font(this.name, this.style, a) : new Font(this.name, a, b);
        f.$ttf = this.$ttf; f.$file = this.$file;
        return f;
    }
    deriveFontStyle(style) {
        const f = new Font(this.name, style, this.pointSize);
        f.$ttf = this.$ttf; f.$file = this.$file;
        return f;
    }
    getSize() { return this.size; }
    getSize2D() { return this.pointSize; }
    getStyle() { return this.style; }
    isBold() { return (this.style & FONT_BOLD) !== 0; }
    isItalic() { return (this.style & FONT_ITALIC) !== 0; }
    isPlain() { return this.style === 0; }
    getName() { return this.name; }
    getFontName() { return this.name; }
    getFamily() { return this.name; }
    canDisplay(c) { return this.$ttf ? this.$ttf.glyphMap.has(typeof c === 'string' ? c.codePointAt(0) : c) : true; }
    equals(o) { return o instanceof Font && o.name === this.name && o.style === this.style && o.pointSize === this.pointSize && o.$file === this.$file; }
    hashCode() { return J.$stringHash(this.name) ^ this.style ^ this.size; }
    toString() { return 'java.awt.Font[family=' + this.name + ',name=' + this.name + ',style=' + ['plain', 'bold', 'italic', 'bolditalic'][this.style] + ',size=' + this.size + ']'; }
    /** Metrics source for this font: a TrueType strike or a CSS font for logical fonts. */
    $strike() {
        if (this.$metrics) return this.$metrics;
        const key = (this.$file || this.name) + '|' + this.style + '|' + this.pointSize;
        let s = strikeCache.get(key);
        if (!s) {
            if (this.$ttf) {
                s = J.$ttfStrike(this.$ttf, this.pointSize, this.isBold() && !this.$ttf.isBold);
                s.ttf = this.$ttf;
                s.italic = this.isItalic() && !this.$ttf.isItalic;
            } else {
                s = cssStrike(this);
            }
            strikeCache.set(key, s);
        }
        this.$metrics = s;
        return s;
    }
}
Font.PLAIN = FONT_PLAIN; Font.BOLD = FONT_BOLD; Font.ITALIC = FONT_ITALIC;
Font.TRUETYPE_FONT = 0; Font.TYPE1_FONT = 1;
Font.SERIF = 'Serif'; Font.SANS_SERIF = 'SansSerif'; Font.MONOSPACED = 'Monospaced'; Font.DIALOG = 'Dialog'; Font.DIALOG_INPUT = 'DialogInput';
J.Font = Font;

/** Logical fonts (Monospaced, SansSerif, ...) use the browser's fonts, measured per glyph like Java. */
const CSS_FAMILIES = {
    monospaced: '"DejaVu Sans Mono", Menlo, Consolas, "Liberation Mono", "Courier New", monospace',
    dialoginput: '"DejaVu Sans Mono", Menlo, Consolas, "Courier New", monospace',
    serif: '"DejaVu Serif", "Times New Roman", Times, serif',
};
let measureCtx = null;
function cssStrike(font) {
    if (!measureCtx) measureCtx = document.createElement('canvas').getContext('2d');
    const fam = CSS_FAMILIES[font.name.toLowerCase()] || '"DejaVu Sans", Arial, Helvetica, sans-serif';
    const css = (font.isItalic() ? 'italic ' : '') + (font.isBold() ? 'bold ' : '') + font.pointSize + 'px ' + fam;
    measureCtx.font = css;
    const m = measureCtx.measureText('Mg');
    const ascent = m.fontBoundingBoxAscent !== undefined ? m.fontBoundingBoxAscent : font.pointSize * 0.8;
    const descent = m.fontBoundingBoxDescent !== undefined ? m.fontBoundingBoxDescent : font.pointSize * 0.2;
    const cache = new Map();
    return {
        css, ascent, descent, leading: 0, boldExtra: 0, ttf: null,
        advance(cp) {
            let a = cache.get(cp);
            if (a === undefined) {
                measureCtx.font = css;
                a = Math.floor(measureCtx.measureText(String.fromCodePoint(cp)).width + 0.5);
                cache.set(cp, a);
            }
            return a;
        },
        isEmpty() { return false; },
    };
}

class FontMetrics {
    constructor(font) { this.font = font; this.s = font.$strike(); }
    getFont() { return this.font; }
    getAscent() { return J.$i(0.95 + this.s.ascent); }
    getDescent() { return J.$i(0.95 + this.s.descent); }
    getLeading() { return J.$i(0.95 + this.s.descent + this.s.leading) - J.$i(0.95 + this.s.descent); }
    getHeight() { return this.getAscent() + J.$i(0.95 + this.s.descent + this.s.leading); }
    getMaxAscent() { return this.getAscent(); }
    getMaxDescent() { return this.getDescent(); }
    $advance(str) {
        let w = 0;
        for (const ch of str) w += this.s.advance(ch.codePointAt(0));
        return w;
    }
    stringWidth(str) { return J.$i(0.5 + this.$advance(str)); }
    charWidth(c) { return J.$i(0.5 + this.s.advance(typeof c === 'string' ? c.codePointAt(0) : c)); }
    charsWidth(chars, off, len) { return this.stringWidth(chars.slice(off, off + len).join('')); }
    getStringBounds(str) {
        return new Rectangle2D.Float(0, -this.s.ascent, this.$advance(str), this.s.ascent + this.s.descent + this.s.leading);
    }
    getLineMetrics() { const s = this.s; return { getAscent: () => s.ascent, getDescent: () => s.descent, getLeading: () => s.leading, getHeight: () => s.ascent + s.descent + s.leading }; }
}
J.FontMetrics = FontMetrics;

// ---------------------------------------------------------------- Graphics2D

/** Device transform helper passed to paints. */
class Dev {
    constructor(a, b, c, d, e, f) {
        this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f;
        this.scale = Math.sqrt(Math.abs(a * d - b * c));
        this.similarity = Math.abs(a - d) < 1e-9 && Math.abs(b + c) < 1e-9;
        this.axisAligned = b === 0 && c === 0;
    }
    pt(x, y) { return [this.a * x + this.c * y + this.e, this.b * x + this.d * y + this.f]; }
}

const DEFAULT_FONT = new Font('Dialog', 0, 12);

class Graphics2D {
    /**
     * @param ctx     canvas 2D context
     * @param dpr     device pixels per CSS pixel (the base transform, like a HiDPI screen in Java)
     * @param width   user-space width of the drawing surface
     * @param height  user-space height
     * @param owner   component (for background colour / font), or null
     */
    constructor(ctx, dpr, width, height, owner) {
        this.$ctx = ctx;
        this.$dpr = dpr;
        this.$w = width; this.$h = height;
        this.$owner = owner;
        this.$paint = owner && owner.$foreground ? owner.$foreground : Color.BLACK;
        this.$background = owner && owner.$background ? owner.$background : Color.WHITE;
        this.$font = owner && owner.$font ? owner.$font : DEFAULT_FONT;
        this.$stroke = new BasicStroke(1);
        this.$composite = AlphaComposite.SrcOver;
        this.$tx = new AffineTransform();
        this.$hints = new Map();
        this.$clip = null;           // {paths: [Path2D device], rect: [x0,y0,x1,y1] | null, userShape}
        this.$ctxClipVersion = -1;
        this.$clipVersion = 0;
        this.$devCache = null;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.save();
        this.$saved = true;
    }

    // ----- state

    $dev() {
        if (!this.$devCache) {
            const t = this.$tx, s = this.$dpr;
            this.$devCache = new Dev(s * t.m00, s * t.m10, s * t.m01, s * t.m11, s * t.m02, s * t.m12);
        }
        return this.$devCache;
    }
    $aa() { return this.$hints.get(RH.KEY_ANTIALIASING) === RH.VALUE_ANTIALIAS_ON; }
    $normalize() { return this.$hints.get(RH.KEY_STROKE_CONTROL) !== RH.VALUE_STROKE_PURE; }

    setColor(c) { if (c !== null && c !== undefined) this.$paint = c; }
    getColor() { return this.$paint instanceof Color ? this.$paint : Color.BLACK; }
    setPaint(p) { if (p !== null && p !== undefined) this.$paint = p; }
    getPaint() { return this.$paint; }
    setBackground(c) { this.$background = c; }
    getBackground() { return this.$background; }
    setFont(f) { if (f !== null && f !== undefined) this.$font = f; }
    getFont() { return this.$font; }
    getFontMetrics(f) { return new FontMetrics(f || this.$font); }
    getFontRenderContext() { return {}; }
    setStroke(s) { if (s) this.$stroke = s; }
    getStroke() { return this.$stroke; }
    setComposite(c) { if (c) this.$composite = c; }
    getComposite() { return this.$composite; }
    setRenderingHint(k, v) { this.$hints.set(k, v); }
    getRenderingHint(k) { return this.$hints.has(k) ? this.$hints.get(k) : (HINT_DEFAULTS.get(k) || null); }
    setRenderingHints(m) { this.$hints.clear(); if (m) for (const e of m.entrySet()) this.$hints.set(e.getKey(), e.getValue()); }
    addRenderingHints(m) { for (const e of m.entrySet()) this.$hints.set(e.getKey(), e.getValue()); }
    getRenderingHints() { const m = new J.HashMap(); for (const [k, v] of this.$hints) m.put(k, v); return m; }
    getTransform() { return new AffineTransform(this.$tx); }
    setTransform(t) { this.$tx = new AffineTransform(t); this.$devCache = null; }
    transform(t) { this.$tx.concatenate(t); this.$devCache = null; }
    translate(x, y) { this.$tx.translate(x, y); this.$devCache = null; }
    rotate(theta, x, y) { this.$tx.rotate(theta, x, y); this.$devCache = null; }
    scale(sx, sy) { this.$tx.scale(sx, sy); this.$devCache = null; }
    shear(a, b) { this.$tx.shear(a, b); this.$devCache = null; }
    create() {
        const g = Object.create(Graphics2D.prototype);
        Object.assign(g, this);
        g.$tx = new AffineTransform(this.$tx);
        g.$hints = new Map(this.$hints);
        g.$devCache = null;
        g.$ctxClipVersion = -1;
        return g;
    }
    dispose() {}

    // ----- clip (kept in device space; rectangles are integer regions like Java's Region)

    $setClipShape(shape, intersect) {
        if (shape === null || shape === undefined) {
            if (!intersect) { this.$clip = null; this.$clipVersion++; }
            return;
        }
        const dev = this.$dev();
        const p = shape.$path ? shape.$path() : null;
        let entry;
        if (p && p.rect && dev.axisAligned) {
            const [ax, ay] = dev.pt(shape.x, shape.y), [bx, by] = dev.pt(shape.x + shape.width, shape.y + shape.height);
            const cr = (v) => Math.ceil(v - 0.5);
            entry = { rect: [cr(Math.min(ax, bx)), cr(Math.min(ay, by)), cr(Math.max(ax, bx)), cr(Math.max(ay, by))] };
        } else if (p) {
            entry = { path: this.$devicePath(p.cmds, null), evenOdd: p.evenOdd };
        } else if (shape.$clipList) {
            entry = null;
        }
        let list;
        if (shape.$clipList && !intersect) list = shape.$clipList.slice();
        else if (shape.$clipList) list = (this.$clip ? this.$clip.list : []).concat(shape.$clipList);
        else list = intersect && this.$clip ? this.$clip.list.concat([entry]) : [entry];
        // merge rectangles
        const rects = list.filter(e => e.rect), others = list.filter(e => !e.rect);
        if (rects.length > 1) {
            const r = rects.reduce((a, e) => [Math.max(a[0], e.rect[0]), Math.max(a[1], e.rect[1]), Math.min(a[2], e.rect[2]), Math.min(a[3], e.rect[3])], [-Infinity, -Infinity, Infinity, Infinity]);
            list = [{ rect: r }].concat(others);
        }
        this.$clip = { list };
        this.$clipVersion++;
    }
    setClip(x, y, w, h) {
        if (typeof x === 'number') this.$setClipShape(new Rectangle(x, y, w, h), false);
        else this.$setClipShape(x, false);
    }
    clipRect(x, y, w, h) { this.$setClipShape(new Rectangle(x, y, w, h), true); }
    clip(s) { this.$setClipShape(s, true); }
    getClip() {
        if (!this.$clip) return null;
        const c = new ClipShape(this.$clip.list, this);
        return c;
    }
    getClipBounds() { const c = this.getClip(); return c ? c.getBounds() : null; }

    $applyClip() {
        if (this.$ctxClipVersion === this.$clipVersion && this.$ctx.$owner === this) return;
        const ctx = this.$ctx;
        ctx.restore();
        ctx.save();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        if (this.$clip) {
            for (const e of this.$clip.list) {
                if (e.rect) {
                    const p = new Path2D();
                    p.rect(e.rect[0], e.rect[1], Math.max(0, e.rect[2] - e.rect[0]), Math.max(0, e.rect[3] - e.rect[1]));
                    ctx.clip(p);
                } else {
                    ctx.clip(e.path, e.evenOdd ? 'evenodd' : 'nonzero');
                }
            }
        }
        this.$ctxClipVersion = this.$clipVersion;
        ctx.$owner = this;
    }

    // ----- path helpers

    /** Transforms user-space path commands to a device-space Path2D, applying Java's normalization. */
    $devicePath(cmds, norm) {
        const dev = this.$dev();
        const path = new Path2D();
        let curAx = 0, curAy = 0, movAx = 0, movAy = 0;
        const nc = norm === 'center' ? (v) => Math.floor(v) + 0.5 : norm === 'quarter' ? (v) => Math.floor(v + 0.25) + 0.25 : null;
        for (const c of cmds) {
            const t = c[0];
            if (t === 'Z') { path.closePath(); curAx = movAx; curAy = movAy; continue; }
            const pts = [];
            for (let i = 1; i < c.length; i += 2) pts.push(...dev.pt(c[i], c[i + 1]));
            if (nc) {
                const li = pts.length - 2;
                const ox = pts[li], oy = pts[li + 1];
                const nx = nc(ox), ny = nc(oy);
                const ax = nx - ox, ay = ny - oy;
                pts[li] = nx; pts[li + 1] = ny;
                if (t === 'M') { movAx = ax; movAy = ay; }
                else if (t === 'Q') { pts[0] += (curAx + ax) / 2; pts[1] += (curAy + ay) / 2; }
                else if (t === 'C') { pts[0] += curAx; pts[1] += curAy; pts[2] += ax; pts[3] += ay; }
                curAx = ax; curAy = ay;
            }
            if (t === 'M') path.moveTo(pts[0], pts[1]);
            else if (t === 'L') path.lineTo(pts[0], pts[1]);
            else if (t === 'Q') path.quadraticCurveTo(pts[0], pts[1], pts[2], pts[3]);
            else if (t === 'C') path.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
        }
        return path;
    }

    $prepare() {
        this.$applyClip();
        const ctx = this.$ctx;
        const comp = this.$composite;
        ctx.globalAlpha = comp.alpha;
        ctx.globalCompositeOperation = COMPOSITE_OPS[comp.rule] || 'source-over';
        return ctx;
    }
    $style() {
        const p = this.$paint;
        if (p instanceof Color) return p.$cssColor();
        if (p && p.$canvasPaint) return p.$canvasPaint(this.$ctx, this.$dev());
        return '#000';
    }

    $fillCmds(cmds, evenOdd) {
        const ctx = this.$prepare();
        const norm = this.$normalize() ? (this.$aa() ? 'center' : 'quarter') : null;
        const path = this.$devicePath(cmds, norm);
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.fillStyle = this.$style();
        ctx.fill(path, evenOdd ? 'evenodd' : 'nonzero');
    }
    $strokeCmds(cmds, thinLine) {
        const ctx = this.$prepare();
        const dev = this.$dev();
        const st = this.$stroke;
        ctx.fillStyle = ctx.strokeStyle = this.$style();
        ctx.lineCap = CAPS[st.cap];
        ctx.lineJoin = JOINS[st.join];
        ctx.miterLimit = st.miterlimit;
        if (dev.similarity) {
            const norm = this.$normalize() ? (this.$aa() || thinLine ? 'center' : 'quarter') : null;
            const path = this.$devicePath(cmds, norm);
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            const w = st.width * dev.scale;
            ctx.lineWidth = w > 0 ? w : 1;
            ctx.setLineDash(st.dash ? st.dash.map(d => d * dev.scale) : []);
            ctx.lineDashOffset = st.dashPhase * dev.scale;
            ctx.stroke(path);
        } else {
            // Non-uniform transform: stroke in user space.
            const path = new Path2D();
            for (const c of cmds) {
                if (c[0] === 'M') path.moveTo(c[1], c[2]);
                else if (c[0] === 'L') path.lineTo(c[1], c[2]);
                else if (c[0] === 'Q') path.quadraticCurveTo(c[1], c[2], c[3], c[4]);
                else if (c[0] === 'C') path.bezierCurveTo(c[1], c[2], c[3], c[4], c[5], c[6]);
                else path.closePath();
            }
            ctx.setTransform(dev.a, dev.b, dev.c, dev.d, dev.e, dev.f);
            ctx.lineWidth = st.width > 0 ? st.width : 1 / dev.scale;
            ctx.setLineDash(st.dash || []);
            ctx.lineDashOffset = st.dashPhase;
            ctx.stroke(path);
        }
    }

    // ----- primitives

    fillRect(x, y, w, h) {
        if (w <= 0 || h <= 0) return;
        const dev = this.$dev();
        if (dev.axisAligned) {
            const ctx = this.$prepare();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.fillStyle = this.$style();
            const [ax, ay] = dev.pt(x, y), [bx, by] = dev.pt(x + w, y + h);
            ctx.fillRect(Math.min(ax, bx), Math.min(ay, by), Math.abs(bx - ax), Math.abs(by - ay));
            return;
        }
        // rotated rectangles are exact parallelograms in Java (not normalized)
        const ctx = this.$prepare();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.fillStyle = this.$style();
        ctx.fill(this.$devicePath([['M', x, y], ['L', x + w, y], ['L', x + w, y + h], ['L', x, y + h], ['Z']], null));
    }
    clearRect(x, y, w, h) {
        const p = this.$paint, c = this.$composite;
        this.$paint = this.$background;
        this.$composite = AlphaComposite.Src;
        this.fillRect(x, y, w, h);
        this.$paint = p;
        this.$composite = c;
    }
    drawRect(x, y, w, h) {
        if (w < 0 || h < 0) return;
        this.$strokeCmds([['M', x, y], ['L', x + w, y], ['L', x + w, y + h], ['L', x, y + h], ['Z']], true);
    }
    drawLine(x1, y1, x2, y2) {
        this.$strokeCmds([['M', x1, y1], ['L', x2, y2]], true);
    }
    fillOval(x, y, w, h) { this.$fillCmds(ellipseCmds(x, y, w, h), false); }
    drawOval(x, y, w, h) { this.$strokeCmds(ellipseCmds(x, y, w, h), false); }
    fillRoundRect(x, y, w, h, aw, ah) { this.$fillCmds(roundRectCmds(x, y, w, h, aw, ah), false); }
    drawRoundRect(x, y, w, h, aw, ah) { this.$strokeCmds(roundRectCmds(x, y, w, h, aw, ah), false); }
    fillArc(x, y, w, h, s, e) { this.$fillCmds(arcCmds(x, y, w, h, s, e, Arc2D.PIE), false); }
    drawArc(x, y, w, h, s, e) { this.$strokeCmds(arcCmds(x, y, w, h, s, e, Arc2D.OPEN), false); }
    fillPolygon(xs, ys, n) {
        const p = xs instanceof Polygon ? xs : new Polygon(xs, ys, n);
        this.$fillCmds(p.$path().cmds, true);
    }
    drawPolygon(xs, ys, n) {
        const p = xs instanceof Polygon ? xs : new Polygon(xs, ys, n);
        this.$strokeCmds(p.$path().cmds, false);
    }
    drawPolyline(xs, ys, n) {
        const cmds = [];
        for (let i = 0; i < n; i++) cmds.push([i === 0 ? 'M' : 'L', xs[i], ys[i]]);
        this.$strokeCmds(cmds, false);
    }
    fill(shape) {
        if (shape instanceof Rectangle2D && shape.$path().rect && !(shape instanceof RoundRectangle2D) && !(shape instanceof Arc2D) && !(shape instanceof Ellipse2D)) {
            this.fillRect(shape.x, shape.y, shape.width, shape.height);
            return;
        }
        const p = shape.$path();
        this.$fillCmds(p.cmds, p.evenOdd);
    }
    draw(shape) { this.$strokeCmds(shape.$path().cmds, false); }

    drawImage(img, a, b, c, d, e, f, g, h, i) {
        if (!img) return true;
        const src = img.$src || img;
        const ctx = this.$prepare();
        const dev = this.$dev();
        const interp = this.$hints.get(RH.KEY_INTERPOLATION);
        ctx.imageSmoothingEnabled = interp === RH.VALUE_INTERPOLATION_BILINEAR || interp === RH.VALUE_INTERPOLATION_BICUBIC;
        ctx.setTransform(dev.a, dev.b, dev.c, dev.d, dev.e, dev.f);
        if (typeof c !== 'number') {                 // (img, x, y, observer)
            ctx.drawImage(src, a, b);
        } else if (typeof e !== 'number') {          // (img, x, y, w, h, observer) or (img, x, y, bgcolor, observer)
            if (c > 0 && d > 0) ctx.drawImage(src, a, b, c, d);
        } else {                                     // (img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer)
            const dx1 = a, dy1 = b, dx2 = c, dy2 = d, sx1 = e, sy1 = f, sx2 = g, sy2 = h;
            if (dx1 === dx2 || dy1 === dy2 || sx1 === sx2 || sy1 === sy2) return true;
            ctx.save();
            ctx.translate(dx1, dy1);
            ctx.scale((dx2 - dx1) / (sx2 - sx1), (dy2 - dy1) / (sy2 - sy1));
            ctx.translate(-sx1, -sy1);
            const x0 = Math.min(sx1, sx2), y0 = Math.min(sy1, sy2);
            ctx.beginPath();
            ctx.rect(x0, y0, Math.abs(sx2 - sx1), Math.abs(sy2 - sy1));
            ctx.clip();
            ctx.drawImage(src, 0, 0);
            ctx.restore();
            ctx.$owner = null;
        }
        return true;
    }

    // ----- text

    drawString(str, x, y) {
        if (str === null || str === undefined) throw new TypeError('String is null');
        str = typeof str === 'string' ? str : J.$str(str);
        if (str.length === 0) return;
        const ctx = this.$prepare();
        const dev = this.$dev();
        const font = this.$font;
        const s = font.$strike();
        ctx.fillStyle = this.$style();
        const snap = dev.axisAligned;
        let penX = x;
        if (s.ttf) {
            const upem = s.ttf.unitsPerEm;
            const k = font.pointSize / upem;
            const shear = s.italic ? -0.2 : 0;
            const extra = s.boldExtra;
            for (const ch of str) {
                const cp = ch.codePointAt(0);
                const adv = s.advance(cp);
                if (!s.isEmpty(cp)) {
                    let [ox, oy] = dev.pt(penX, y);
                    if (snap) { ox = Math.floor(ox + 0.5); oy = Math.floor(oy + 0.5); }
                    const path = s.ttf.glyphPath(s.glyph(cp));
                    // font units (y up) -> user space -> device space
                    const a = dev.a * k, b = dev.b * k, c = -dev.c * k, d = -dev.d * k;
                    const cc = c + a * shear * -1, dd = d + b * shear * -1;
                    if (extra > 0) {
                        // Java's synthetic bold (FT_Outline_EmboldenXY(extra, 0)) keeps the left edges
                        // and moves the right edges by `extra`: the union of the glyph at x and x + extra.
                        const steps = Math.max(1, Math.ceil(extra * dev.scale));
                        for (let k = 0; k <= steps; k++) {
                            const off = extra * k / steps;
                            ctx.setTransform(a, b, cc, dd, ox + dev.a * off, oy + dev.b * off);
                            ctx.fill(path);
                        }
                    } else {
                        ctx.setTransform(a, b, cc, dd, ox, oy);
                        ctx.fill(path);
                    }
                }
                penX += adv;
            }
        } else {
            ctx.font = s.css;
            ctx.textBaseline = 'alphabetic';
            ctx.textAlign = 'left';
            for (const ch of str) {
                const cp = ch.codePointAt(0);
                let [ox, oy] = dev.pt(penX, y);
                if (snap) { ox = Math.floor(ox + 0.5); oy = Math.floor(oy + 0.5); }
                ctx.setTransform(dev.a, dev.b, dev.c, dev.d, ox, oy);
                ctx.fillText(ch, 0, 0);
                penX += s.advance(cp);
            }
        }
    }
    drawChars(chars, off, len, x, y) { this.drawString(chars.slice(off, off + len).join(''), x, y); }
}
J.Graphics2D = Graphics2D;
J.Graphics = Graphics2D;

/** Opaque clip returned by getClip(); setClip() restores it exactly (device space). */
class ClipShape extends Shape {
    constructor(list, g) { super(); this.$clipList = list; this.$g = g; }
    getBounds() {
        let x0 = -Infinity, y0 = -Infinity, x1 = Infinity, y1 = Infinity;
        for (const e of this.$clipList) if (e.rect) { x0 = Math.max(x0, e.rect[0]); y0 = Math.max(y0, e.rect[1]); x1 = Math.min(x1, e.rect[2]); y1 = Math.min(y1, e.rect[3]); }
        if (x0 === -Infinity) { x0 = 0; y0 = 0; x1 = this.$g.$w * this.$g.$dpr; y1 = this.$g.$h * this.$g.$dpr; }
        // back to user space (translation/scale only)
        const t = this.$g.$tx, s = this.$g.$dpr;
        const ux0 = (x0 / s - t.m02) / t.m00, uy0 = (y0 / s - t.m12) / t.m11, ux1 = (x1 / s - t.m02) / t.m00, uy1 = (y1 / s - t.m12) / t.m11;
        return new Rectangle(Math.floor(Math.min(ux0, ux1)), Math.floor(Math.min(uy0, uy1)), Math.ceil(Math.abs(ux1 - ux0)), Math.ceil(Math.abs(uy1 - uy0)));
    }
    getBounds2D() { return this.getBounds(); }
    contains(x, y) { return this.getBounds().contains(x, y); }
}
})();
