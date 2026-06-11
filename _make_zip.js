// Rebuild meditation-portal-site.zip from docs/meditation-portal/
const fs   = require('fs');
const path = require('path');
const zlib = require('zlib');

const SRC  = path.join(__dirname, 'docs', 'meditation-portal');
const OUT  = path.join(__dirname, 'meditation-portal-site.zip');

// ── Minimal ZIP builder using raw deflate ──────────────────────────────────
const localHeaders = [];
let   body         = Buffer.alloc(0);

function addFile(relPath) {
    const abs  = path.join(SRC, relPath);
    const raw  = fs.readFileSync(abs);
    const comp = zlib.deflateRawSync(raw, { level: 9 });
    const crc  = crc32(raw);
    const name = Buffer.from(relPath.replace(/\\/g, '/'));

    const offset = body.length;
    // Local file header
    const lh = Buffer.alloc(30 + name.length);
    lh.writeUInt32LE(0x04034b50, 0);   // sig
    lh.writeUInt16LE(20, 4);           // version needed
    lh.writeUInt16LE(0, 6);            // flags
    lh.writeUInt16LE(8, 8);            // deflate
    lh.writeUInt16LE(0, 10); lh.writeUInt16LE(0, 12); // mod time/date
    lh.writeUInt32LE(crc, 14);
    lh.writeUInt32LE(comp.length, 18);
    lh.writeUInt32LE(raw.length, 22);
    lh.writeUInt16LE(name.length, 26);
    lh.writeUInt16LE(0, 28);
    name.copy(lh, 30);

    body = Buffer.concat([body, lh, comp]);
    localHeaders.push({ name, crc, comp: comp.length, raw: raw.length, offset });
}

function crc32(buf) {
    let c = ~0;
    for (let i = 0; i < buf.length; i++) {
        c ^= buf[i];
        for (let k = 0; k < 8; k++) c = c & 1 ? (c >>> 1) ^ 0xedb88320 : c >>> 1;
    }
    return (~c) >>> 0;
}

// Walk source directory
function walk(dir, base) {
    for (const entry of fs.readdirSync(dir)) {
        const full = path.join(dir, entry);
        const rel  = base ? base + '/' + entry : entry;
        if (fs.statSync(full).isDirectory()) walk(full, rel);
        else addFile(rel);
    }
}
walk(SRC, '');

// Central directory
const cdStart = body.length;
let   cd      = Buffer.alloc(0);
for (const h of localHeaders) {
    const rec = Buffer.alloc(46 + h.name.length);
    rec.writeUInt32LE(0x02014b50, 0);
    rec.writeUInt16LE(20, 4); rec.writeUInt16LE(20, 6);
    rec.writeUInt16LE(0, 8); rec.writeUInt16LE(8, 10);
    rec.writeUInt16LE(0, 12); rec.writeUInt16LE(0, 14);
    rec.writeUInt32LE(h.crc, 16);
    rec.writeUInt32LE(h.comp, 20);
    rec.writeUInt32LE(h.raw, 24);
    rec.writeUInt16LE(h.name.length, 28);
    rec.writeUInt16LE(0, 30); rec.writeUInt16LE(0, 32);
    rec.writeUInt16LE(0, 34); rec.writeUInt16LE(0, 36);
    rec.writeUInt32LE(0, 38);
    rec.writeUInt32LE(h.offset, 42);
    h.name.copy(rec, 46);
    cd = Buffer.concat([cd, rec]);
}

// End of central directory
const eocd = Buffer.alloc(22);
eocd.writeUInt32LE(0x06054b50, 0);
eocd.writeUInt16LE(0, 4); eocd.writeUInt16LE(0, 6);
eocd.writeUInt16LE(localHeaders.length, 8);
eocd.writeUInt16LE(localHeaders.length, 10);
eocd.writeUInt32LE(cd.length, 12);
eocd.writeUInt32LE(cdStart, 16);
eocd.writeUInt16LE(0, 20);

fs.writeFileSync(OUT, Buffer.concat([body, cd, eocd]));
const kb = Math.round(fs.statSync(OUT).size / 1024);
console.log(`Created ${OUT} (${kb} KB, ${localHeaders.length} files)`);
