// PongPing web runtime - minimal TrueType parser.
// Java's FontMetrics come from the font tables (hhea, hmtx, cmap); reading them here lets the
// browser version measure and place text exactly like the desktop game.
'use strict';
(function () {

J.$parseTTF = function (buffer) {
    const dv = new DataView(buffer);
    const u16 = (o) => dv.getUint16(o);
    const i16 = (o) => dv.getInt16(o);
    const u32 = (o) => dv.getUint32(o);
    const tables = {};
    const numTables = u16(4);
    for (let i = 0; i < numTables; i++) {
        const rec = 12 + i * 16;
        const tag = String.fromCharCode(dv.getUint8(rec), dv.getUint8(rec + 1), dv.getUint8(rec + 2), dv.getUint8(rec + 3));
        tables[tag] = { offset: u32(rec + 8), length: u32(rec + 12) };
    }
    const head = tables.head.offset;
    const unitsPerEm = u16(head + 18);
    const headFlags = u16(head + 16);
    const indexToLocFormat = i16(head + 50);
    const hhea = tables.hhea.offset;
    let ascender = i16(hhea + 4);
    let descender = i16(hhea + 6);
    const lineGap = i16(hhea + 8);
    const numberOfHMetrics = u16(hhea + 34);
    if (ascender === 0 && descender === 0 && tables['OS/2']) {
        const os2 = tables['OS/2'].offset;
        ascender = i16(os2 + 68);
        descender = i16(os2 + 70);
    }
    const numGlyphs = u16(tables.maxp.offset + 4);
    const hmtx = tables.hmtx.offset;
    const advances = new Uint16Array(numGlyphs);
    let last = 0;
    for (let g = 0; g < numGlyphs; g++) {
        if (g < numberOfHMetrics) last = u16(hmtx + g * 4);
        advances[g] = last;
    }
    // Glyphs without contours (e.g. space) are not emboldened by Java's synthetic bold.
    const emptyGlyph = new Uint8Array(numGlyphs);
    if (tables.loca && tables.glyf) {
        const loca = tables.loca.offset, glyf = tables.glyf.offset;
        for (let g = 0; g < numGlyphs; g++) {
            const a = indexToLocFormat === 0 ? u16(loca + g * 2) * 2 : u32(loca + g * 4);
            const b = indexToLocFormat === 0 ? u16(loca + g * 2 + 2) * 2 : u32(loca + g * 4 + 4);
            emptyGlyph[g] = (b <= a || i16(glyf + a) === 0) ? 1 : 0;
        }
    }
    let weightClass = 400, fsSelection = 0, macStyle = u16(head + 44);
    if (tables['OS/2']) {
        weightClass = u16(tables['OS/2'].offset + 4);
        fsSelection = u16(tables['OS/2'].offset + 62);
    }

    // cmap: prefer format 12 (full Unicode), then format 4 (BMP)
    const cmap = tables.cmap.offset;
    const nSub = u16(cmap + 2);
    let best = null, bestFmt = 0;
    for (let i = 0; i < nSub; i++) {
        const platform = u16(cmap + 4 + i * 8);
        const encoding = u16(cmap + 6 + i * 8);
        const off = cmap + u32(cmap + 8 + i * 8);
        const fmt = u16(off);
        const unicode = platform === 0 || (platform === 3 && (encoding === 1 || encoding === 10));
        if (!unicode) continue;
        if (fmt === 12 && bestFmt !== 12) { best = off; bestFmt = 12; }
        else if (fmt === 4 && !best) { best = off; bestFmt = 4; }
    }
    const glyphMap = new Map();
    if (bestFmt === 4) {
        const segX2 = u16(best + 6);
        const endCodes = best + 14, startCodes = endCodes + segX2 + 2, idDeltas = startCodes + segX2, idRangeOffsets = idDeltas + segX2;
        for (let s = 0; s < segX2 / 2; s++) {
            const end = u16(endCodes + s * 2), start = u16(startCodes + s * 2);
            const delta = i16(idDeltas + s * 2), ro = u16(idRangeOffsets + s * 2);
            for (let c = start; c <= end && c !== 0xFFFF; c++) {
                let g;
                if (ro === 0) g = (c + delta) & 0xFFFF;
                else {
                    const addr = idRangeOffsets + s * 2 + ro + (c - start) * 2;
                    g = u16(addr);
                    if (g !== 0) g = (g + delta) & 0xFFFF;
                }
                if (g !== 0) glyphMap.set(c, g);
            }
        }
    } else if (bestFmt === 12) {
        const nGroups = u32(best + 12);
        for (let i = 0; i < nGroups; i++) {
            const start = u32(best + 16 + i * 12), end = u32(best + 20 + i * 12), sg = u32(best + 24 + i * 12);
            for (let c = start; c <= end; c++) glyphMap.set(c, sg + (c - start));
        }
    }
    // Glyph outlines (quadratic TrueType contours) as Path2D in font units, built lazily.
    const glyf = tables.glyf ? tables.glyf.offset : 0, loca = tables.loca ? tables.loca.offset : 0;
    const glyphOffset = (g) => {
        const a = indexToLocFormat === 0 ? u16(loca + g * 2) * 2 : u32(loca + g * 4);
        const b = indexToLocFormat === 0 ? u16(loca + g * 2 + 2) * 2 : u32(loca + g * 4 + 4);
        return b > a ? glyf + a : -1;
    };
    function contours(g, depth) {
        const out = [];
        if (!glyf || g >= numGlyphs || depth > 8) return out;
        const off = glyphOffset(g);
        if (off < 0) return out;
        const nContours = i16(off);
        if (nContours >= 0) {
            const ends = [];
            for (let i = 0; i < nContours; i++) ends.push(u16(off + 10 + i * 2));
            const nPts = nContours ? ends[nContours - 1] + 1 : 0;
            let p = off + 10 + nContours * 2;
            p += 2 + u16(p);
            const flags = new Uint8Array(nPts);
            for (let i = 0; i < nPts;) {
                const f = dv.getUint8(p++);
                flags[i++] = f;
                if (f & 8) { let r = dv.getUint8(p++); while (r-- > 0 && i < nPts) flags[i++] = f; }
            }
            const xs = new Float64Array(nPts), ys = new Float64Array(nPts);
            let v = 0;
            for (let i = 0; i < nPts; i++) {
                const f = flags[i];
                if (f & 2) { const d = dv.getUint8(p++); v += (f & 16) ? d : -d; }
                else if (!(f & 16)) { v += i16(p); p += 2; }
                xs[i] = v;
            }
            v = 0;
            for (let i = 0; i < nPts; i++) {
                const f = flags[i];
                if (f & 4) { const d = dv.getUint8(p++); v += (f & 32) ? d : -d; }
                else if (!(f & 32)) { v += i16(p); p += 2; }
                ys[i] = v;
            }
            let start = 0;
            for (const end of ends) {
                const pts = [];
                for (let i = start; i <= end; i++) pts.push([xs[i], ys[i], (flags[i] & 1) === 1]);
                out.push(pts);
                start = end + 1;
            }
        } else {
            let p = off + 10, more = true;
            while (more) {
                const f = u16(p), gi = u16(p + 2);
                p += 4;
                let dx, dy;
                if (f & 1) { dx = i16(p); dy = i16(p + 2); p += 4; } else { dx = dv.getInt8(p); dy = dv.getInt8(p + 1); p += 2; }
                let a = 1, b = 0, c = 0, d = 1;
                const f2 = (o) => i16(o) / 16384;
                if (f & 8) { a = d = f2(p); p += 2; }
                else if (f & 0x40) { a = f2(p); d = f2(p + 2); p += 4; }
                else if (f & 0x80) { a = f2(p); b = f2(p + 2); c = f2(p + 4); d = f2(p + 6); p += 8; }
                for (const ct of contours(gi, depth + 1)) {
                    out.push(ct.map(([x, y, on]) => [a * x + c * y + ((f & 2) ? dx : 0), b * x + d * y + ((f & 2) ? dy : 0), on]));
                }
                more = (f & 0x20) !== 0;
            }
        }
        return out;
    }
    const pathCache = new Map();
    function glyphPath(g) {
        let path = pathCache.get(g);
        if (path) return path;
        path = new Path2D();
        for (const pts of contours(g, 0)) {
            const n = pts.length;
            if (!n) continue;
            // find a starting on-curve point (or the midpoint of two off-curve points)
            let startIdx = pts.findIndex(q => q[2]);
            let sx, sy;
            if (startIdx < 0) { sx = (pts[0][0] + pts[1 % n][0]) / 2; sy = (pts[0][1] + pts[1 % n][1]) / 2; startIdx = 0; }
            else { sx = pts[startIdx][0]; sy = pts[startIdx][1]; }
            path.moveTo(sx, sy);
            let ctrl = null;
            for (let k = 1; k <= n; k++) {
                const q = pts[(startIdx + k) % n];
                if (q[2]) {
                    if (ctrl) { path.quadraticCurveTo(ctrl[0], ctrl[1], q[0], q[1]); ctrl = null; }
                    else path.lineTo(q[0], q[1]);
                } else {
                    if (ctrl) {
                        const mx = (ctrl[0] + q[0]) / 2, my = (ctrl[1] + q[1]) / 2;
                        path.quadraticCurveTo(ctrl[0], ctrl[1], mx, my);
                    }
                    ctrl = q;
                }
            }
            if (ctrl) path.quadraticCurveTo(ctrl[0], ctrl[1], sx, sy);
            path.closePath();
        }
        pathCache.set(g, path);
        return path;
    }

    const isBold = weightClass >= 600 || (fsSelection & 0x20) !== 0 || (macStyle & 1) !== 0;
    const isItalic = (fsSelection & 1) !== 0 || (macStyle & 2) !== 0;
    return { glyphPath, unitsPerEm, headFlags, emptyGlyph, ascender, descender, lineGap, advances, glyphMap, numGlyphs, isBold, isItalic };
};

function yScaleFor(size, ttf) {
    const size26 = Math.floor(size * 64);
    return Math.floor((size26 * 65536 + Math.floor(ttf.unitsPerEm / 2)) / ttf.unitsPerEm);
}

/** Rounds to 26.6 fixed point like FreeType (floor to 1/64). */
const to64floor = (x) => Math.floor(x * 64) / 64;

/**
 * Per-size strike metrics, following the JDK FreeType scaler: hinted advances are rounded to whole
 * pixels; synthetic bold adds size/32 to each advance; ascent/descent/leading are the scaled hhea values.
 */
J.$ttfStrike = function (ttf, size, syntheticBold) {
    const scale = size / ttf.unitsPerEm;
    // Hinted advances: fonts with head.flags bit 3 are scaled with an integer ppem.
    // Computed with FreeType's fixed-point arithmetic (FT_DivFix / FT_MulFix, 26.6 rounding).
    const size26 = Math.floor(size * 64);
    const scaled = (ttf.headFlags & 8) ? ((size26 + 32) & ~63) : size26;
    const xScale = Math.floor((scaled * 65536 + Math.floor(ttf.unitsPerEm / 2)) / ttf.unitsPerEm);
    const hintedAdvance = (units) => {
        const adv26 = Math.floor((units * xScale + 0x8000) / 65536);
        return Math.floor((adv26 + 32) / 64);
    };
    const boldExtra = syntheticBold ? Math.floor(Math.floor((ttf.unitsPerEm * yScaleFor(size, ttf) + 0x8000) / 65536) / 32) / 64 : 0;
    const cache = new Map();
    const yScale = Math.floor((size26 * 65536 + Math.floor(ttf.unitsPerEm / 2)) / ttf.unitsPerEm);
    const vmetric = (units) => units * yScale / 65536 / 64;
    return {
        ascent: vmetric(ttf.ascender),
        descent: vmetric(-ttf.descender),
        leading: vmetric(ttf.lineGap),
        boldExtra,
        isEmpty(cp) { return ttf.emptyGlyph[ttf.glyphMap.get(cp) || 0] === 1; },
        glyph(cp) {
            return ttf.glyphMap.get(cp) || 0;
        },
        advance(cp) {
            let a = cache.get(cp);
            if (a === undefined) {
                const g = ttf.glyphMap.get(cp) || 0;
                a = hintedAdvance(ttf.advances[g]) + (ttf.emptyGlyph[g] ? 0 : boldExtra);
                cache.set(cp, a);
            }
            return a;
        },
    };
};
})();
