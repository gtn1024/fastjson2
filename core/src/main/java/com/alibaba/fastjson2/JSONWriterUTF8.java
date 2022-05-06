package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.RyuDouble;
import com.alibaba.fastjson2.util.RyuFloat;

import static com.alibaba.fastjson2.util.IOUtils.DigitOnes;
import static com.alibaba.fastjson2.util.IOUtils.DigitTens;
import static com.alibaba.fastjson2.util.IOUtils.digits;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.alibaba.fastjson2.JSONFactory.*;

class JSONWriterUTF8 extends JSONWriter {
    static final byte[] REF_PREF = "{\"$ref\":".getBytes(StandardCharsets.US_ASCII);

    final AtomicReferenceFieldUpdater<JSONFactory.Cache, byte[]> byteUpdater;
    protected byte[] bytes;

    JSONWriterUTF8(Context ctx) {
        super(ctx, StandardCharsets.UTF_8);

        int identityHashCode = System.identityHashCode(Thread.currentThread());
        switch (identityHashCode & 3) {
            case 0:
                byteUpdater = JSONFactory.BYTES0_UPDATER;
                break;
            case 1:
                byteUpdater = JSONFactory.BYTES1_UPDATER;
                break;
            case 2:
                byteUpdater = JSONFactory.BYTES2_UPDATER;
                break;
            default:
                byteUpdater = JSONFactory.BYTES3_UPDATER;
                break;
        }
        bytes = byteUpdater.getAndSet(JSONFactory.CACHE, null);

        if (bytes == null) {
            bytes = new byte[1024];
        }
    }

    @Override
    public void writeReference(String path) {
        this.lastReference = path;

        writeRaw(REF_PREF);
        writeString(path);
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) '}';
    }

    @Override
    public void close() {
        if (bytes.length > CACHE_THREAD) {
            return;
        }
        byteUpdater.set(JSONFactory.CACHE, bytes);
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, off);
    }

    @Override
    public int flushTo(OutputStream to) throws IOException {
        int len = off;
        to.write(bytes, 0, off);
        off = 0;
        return len;
    }

    @Override
    protected void write0(char c) {
        ensureCapacity(off + 1);
        bytes[off++] = (byte) c;
    }

    @Override
    public void startObject() {
        level++;
        startObject = true;
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) '{';
    }

    @Override
    public void endObject() {
        level--;
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) '}';
        startObject = false;
    }

    @Override
    public void writeComma() {
        startObject = false;
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) ',';
    }

    @Override
    public void startArray() {
        level++;
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) '[';
    }

    @Override
    public void endArray() {
        level--;
        if (off == bytes.length) {
            int minCapacity = off + 1;
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
        bytes[off++] = (byte) ']';
    }

    @Override
    public void writeString(String str) {
        if (str == null) {
            writeNull();
            return;
        }

        char[] chars = JDKUtils.getCharArray(str);

        // ensureCapacity
        int minCapacity = off
                + chars.length * 3 // utf8 3 bytes
                + 2;

        if (minCapacity - this.bytes.length > 0) {
            int oldCapacity = this.bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            this.bytes = Arrays.copyOf(this.bytes, newCapacity);
        }

        bytes[off++] = (byte) '"';

        // vector optimize
        int i = 0;
        while (i + 4 <= chars.length) {
            char c0 = chars[i];
            char c1 = chars[i + 1];
            char c2 = chars[i + 2];
            char c3 = chars[i + 3];
            if (c0 == '"' || c1 == '"' || c2 == '"' || c3 == '"') {
                break;
            }
            if (c0 == '\\' || c1 == '\\' || c2 == '\\' || c3 == '\\') {
                break;
            }
            if (c0 < ' ' || c1 < ' ' || c2 < ' ' || c3 < ' ') {
                break;
            }
            if (c0 > 0x007F || c1 > 0x007F || c2 > 0x007F || c3 > 0x007F) {
                break;
            }
            bytes[off] = (byte) c0;
            bytes[off + 1] = (byte) c1;
            bytes[off + 2] = (byte) c2;
            bytes[off + 3] = (byte) c3;
            off += 4;
            i += 4;
        }
        if (i + 2 <= chars.length) {
            char c0 = chars[i];
            char c1 = chars[i + 1];
            if (c0 == '"' || c1 == '"'
                    || c0 == '\\' || c1 == '\\'
                    || c0 < ' ' || c1 < ' '
                    || c0 > 0x007F || c1 > 0x007F
            ) {
            } else {
                bytes[off] = (byte) c0;
                bytes[off + 1] = (byte) c1;
                off += 2;
                i += 2;
            }
        }
        if (i + 1 == chars.length) {
            char c0 = chars[i];
            if (c0 != '"'
                    && c0 != '\\'
                    && c0 >= ' '
                    && c0 <= 0x007F
            ) {
                bytes[off++] = (byte) c0;
                bytes[off++] = (byte) '"';
                return;
            }
        }
        for (; i < chars.length; ++i) { // ascii none special fast write
            char ch = chars[i];
            if ((ch >= 0x0001) && (ch <= 0x007F)) {
                if (ch == '"') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = '"';
                } else if (ch == '\\') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) '\\';
                } else if (ch == '\n') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) 'n';
                } else if (ch == '\r') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) 'r';
                } else if (ch == '\f') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) 'f';
                } else if (ch == '\b') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) 'b';
                } else if (ch == '\t') {
                    bytes[off++] = (byte) '\\';
                    bytes[off++] = (byte) 't';
                } else {
                    bytes[off++] = (byte) ch;
                }
            } else if (ch >= '\uD800' && ch < ('\uDFFF' + 1)) { //  //Character.isSurrogate(c)
                final int uc;
                if (ch >= '\uD800' && ch < ('\uDBFF' + 1)) { // Character.isHighSurrogate(c)
                    if (chars.length - i < 2) {
                        uc = -1;
                    } else {
                        char d = chars[i + 1];
                        // d >= '\uDC00' && d < ('\uDFFF' + 1)
                        if (d >= '\uDC00' && d < ('\uDFFF' + 1)) { // Character.isLowSurrogate(d)
                            uc = ((ch << 10) + d) + (0x010000 - ('\uD800' << 10) - '\uDC00'); // Character.toCodePoint(c, d)
                        } else {
//                            throw new JSONException("encodeUTF8 error", new MalformedInputException(1));
                            bytes[off++] = (byte) '?';
                            continue;
                        }
                    }
                } else {
                    //
                    if (ch >= '\uDC00' && ch < ('\uDFFF' + 1)) { // Character.isLowSurrogate(c)
                        bytes[off++] = (byte) '?';
                        continue;
//                        throw new JSONException("encodeUTF8 error", new MalformedInputException(1));
                    } else {
                        uc = ch;
                    }
                }

                if (uc < 0) {
                    bytes[off++] = (byte) '?';
                } else {
                    bytes[off++] = (byte) (0xf0 | ((uc >> 18)));
                    bytes[off++] = (byte) (0x80 | ((uc >> 12) & 0x3f));
                    bytes[off++] = (byte) (0x80 | ((uc >> 6) & 0x3f));
                    bytes[off++] = (byte) (0x80 | (uc & 0x3f));
                    i++; // 2 chars
                }
            } else if (ch > 0x07FF) {
                bytes[off++] = (byte) (0xE0 | ((ch >> 12) & 0x0F));
                bytes[off++] = (byte) (0x80 | ((ch >> 6) & 0x3F));
                bytes[off++] = (byte) (0x80 | ((ch >> 0) & 0x3F));
            } else {
                bytes[off++] = (byte) (0xC0 | ((ch >> 6) & 0x1F));
                bytes[off++] = (byte) (0x80 | ((ch >> 0) & 0x3F));
            }
        }

        bytes[off++] = (byte) '"';
    }

    @Override
    public void writeUUID(UUID value) {
        if (value == null) {
            writeNull();
            return;
        }

        long msb = value.getMostSignificantBits();
        long lsb = value.getLeastSignificantBits();

        ensureCapacity(off + 38);
        bytes[off++] = '"';
        formatUnsignedLong0(lsb, bytes, off + 24, 12);
        formatUnsignedLong0(lsb >>> 48, bytes, off + 19, 4);
        formatUnsignedLong0(msb, bytes, off + 14, 4);
        formatUnsignedLong0(msb >>> 16, bytes, off + 9, 4);
        formatUnsignedLong0(msb >>> 32, bytes, off + 0, 8);

        bytes[off + 23] = '-';
        bytes[off + 18] = '-';
        bytes[off + 13] = '-';
        bytes[off + 8] = '-';
        off += 36;
        bytes[off++] = '"';
    }

    @Override
    public void writeRaw(String str) {
        char[] chars = JDKUtils.getCharArray(str);
        {
            int minCapacity = off
                    + chars.length * 3; // utf8 3 bytes

            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
        for (char c : chars) {
            if (c == '"') {
                bytes[off++] = (byte) '\\';
            }

            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytes[off++] = (byte) c;
            } else if (c > 0x07FF) {
                bytes[off++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytes[off++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[off++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            } else {
                bytes[off++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytes[off++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            }
        }
    }

    @Override
    public void writeRaw(byte[] bytes) {
        {
            // inline ensureCapacity
            int minCapacity = off + bytes.length;
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
        System.arraycopy(bytes, 0, this.bytes, this.off, bytes.length);
        off += bytes.length;
    }

    @Override
    public void writeNameRaw(byte[] bytes) {
        {
            // inline ensureCapacity
            int minCapacity = off + bytes.length + (startObject ? 0 : 1);
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
        if (startObject) {
            startObject = false;
        } else {
            this.bytes[off++] = ',';
        }
        System.arraycopy(bytes, 0, this.bytes, this.off, bytes.length);
        off += bytes.length;
    }

    @Override
    public void writeNameRaw(byte[] bytes, int off, int len) {
        {
            // inline ensureCapacity
            int minCapacity = this.off + len + (startObject ? 0 : 1);
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }

        if (startObject) {
            startObject = false;
        } else {
            this.bytes[this.off++] = ',';
        }
        System.arraycopy(bytes, off, this.bytes, this.off, len);
        this.off += len;
    }

    void ensureCapacity(int minCapacity) {
        if (minCapacity - bytes.length > 0) {
            int oldCapacity = bytes.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                throw new OutOfMemoryError();
            }

            // minCapacity is usually close to size, so this is a win:
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
    }

    @Override
    public void writeInt32(int i) {
        if ((context.features & Feature.WriteNonStringValueAsString.mask) != 0) {
            writeString(Integer.toString(i));
            return;
        }

        if (i == Integer.MIN_VALUE) {
            writeRaw("-2147483648");
            return;
        }

        int size;
        {
            int x = i < 0 ? -i : i;
            if (x <= 9) {
                size = 1;
            } else if (x <= 99) {
                size = 2;
            } else if (x <= 999) {
                size = 3;
            } else if (x <= 9999) {
                size = 4;
            } else if (x <= 99999) {
                size = 5;
            } else if (x <= 999999) {
                size = 6;
            } else if (x <= 9999999) {
                size = 7;
            } else if (x <= 99999999) {
                size = 8;
            } else if (x <= 999999999) {
                size = 9;
            } else {
                size = 10;
            }
            if (i < 0) {
                size++;
            }
        }

//        int size = (i < 0) ? IOUtils.stringSize(-i) + 1 : IOUtils.stringSize(i);
        {
            // inline ensureCapacity
            int minCapacity = off + size;
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
//        getChars(i, off + size, bytes);
        {
            int index = off + size;
            int q, r, p = index;
            byte sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            while (i >= 65536) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = i - ((q << 6) + (q << 5) + (q << 2));
                i = q;
                bytes[--p] = DigitOnes[r];
                bytes[--p] = DigitTens[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i <= 65536, i);
            for (; ; ) {
                q = (i * 52429) >>> (16 + 3);
                r = i - ((q << 3) + (q << 1)); // r = i-(q*10) ...
                bytes[--p] = digits[r];
                i = q;
                if (i == 0) {
                    break;
                }
            }
            if (sign != 0) {
                bytes[--p] = sign;
            }
        }
        off += size;
    }

    @Override
    public void writeInt64(long i) {
        if ((context.features & Feature.WriteNonStringValueAsString.mask) != 0
                || ((context.features & Feature.BrowserCompatible.mask) != 0
                && (i > 9007199254740991L || i < -9007199254740991L))) {
            String str = Long.toString(i);
            writeString(str);
            return;
        }

        if (i == Long.MIN_VALUE) {
            writeRaw("-9223372036854775808");
            return;
        }

        int size;
        {
            long x = i < 0 ? -i : i;
            if (x <= 9) {
                size = 1;
            } else if (x <= 99L) {
                size = 2;
            } else if (x <= 999L) {
                size = 3;
            } else if (x <= 9999L) {
                size = 4;
            } else if (x <= 99999L) {
                size = 5;
            } else if (x <= 999999L) {
                size = 6;
            } else if (x <= 9999999L) {
                size = 7;
            } else if (x <= 99999999L) {
                size = 8;
            } else if (x <= 999999999L) {
                size = 9;
            } else if (x <= 9999999999L) {
                size = 10;
            } else if (x <= 99999999999L) {
                size = 11;
            } else if (x <= 999999999999L) {
                size = 12;
            } else if (x <= 9999999999999L) {
                size = 13;
            } else if (x <= 99999999999999L) {
                size = 14;
            } else if (x <= 999999999999999L) {
                size = 15;
            } else if (x <= 9999999999999999L) {
                size = 16;
            } else if (x <= 99999999999999999L) {
                size = 17;
            } else if (x <= 999999999999999999L) {
                size = 18;
            } else {
                size = 19;
            }
            if (i < 0) {
                size++;
            }
        }

        {
            // inline ensureCapacity
            int minCapacity = off + size;
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
//        IOUtils.getChars(i, off + size, bytes);
        {
            int index = off + size;
            long q;
            int r;
            int charPos = index;
            byte sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i > Integer.MAX_VALUE) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
                i = q;
                bytes[--charPos] = DigitOnes[r];
                bytes[--charPos] = DigitTens[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 >= 65536) {
                q2 = i2 / 100;
                // really: r = i2 - (q * 100);
                r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
                i2 = q2;
                bytes[--charPos] = DigitOnes[r];
                bytes[--charPos] = DigitTens[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (; ; ) {
                q2 = (i2 * 52429) >>> (16 + 3);
                r = i2 - ((q2 << 3) + (q2 << 1)); // r = i2-(q2*10) ...
                bytes[--charPos] = digits[r];
                i2 = q2;
                if (i2 == 0) {
                    break;
                }
            }
            if (sign != 0) {
                bytes[--charPos] = sign;
            }
        }
        off += size;
    }

    @Override
    public void writeFloat(float value) {
        ensureCapacity(off + 15);
        int len = RyuFloat.toString(value, bytes, off);
        off += len;
    }

    @Override
    public void writeDouble(double value) {
        ensureCapacity(off + 24);
        int len = RyuDouble.toString(value, bytes, off);
        off += len;
    }

    @Override
    public void writeDateTime19(
            int year,
            int month,
            int dayOfMonth,
            int hour,
            int minute,
            int second) {

        ensureCapacity(off + 21);

        bytes[off++] = '"';

        bytes[off++] = (byte) (year / 1000 + '0');
        bytes[off++] = (byte) ((year / 100) % 10 + '0');
        bytes[off++] = (byte) ((year / 10) % 10 + '0');
        bytes[off++] = (byte) (year % 10 + '0');
        bytes[off++] = '-';
        bytes[off++] = (byte) (month / 10 + '0');
        bytes[off++] = (byte) (month % 10 + '0');
        bytes[off++] = '-';
        bytes[off++] = (byte) (dayOfMonth / 10 + '0');
        bytes[off++] = (byte) (dayOfMonth % 10 + '0');
        bytes[off++] = ' ';
        bytes[off++] = (byte) (hour / 10 + '0');
        bytes[off++] = (byte) (hour % 10 + '0');
        bytes[off++] = ':';
        bytes[off++] = (byte) (minute / 10 + '0');
        bytes[off++] = (byte) (minute % 10 + '0');
        bytes[off++] = ':';
        bytes[off++] = (byte) (second / 10 + '0');
        bytes[off++] = (byte) (second % 10 + '0');

        bytes[off++] = '"';
    }

    @Override
    public void writeLocalDateTime(LocalDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int dayOfMonth = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        int nano = dateTime.getNano();

        int yearSize = IOUtils.stringSize(year);
        int len = 17 + yearSize;
        int small;
        if (nano % 1000_000_000 == 0) {
            small = 0;
        } else if (nano % 1000_000_00 == 0) {
            len += 2;
            small = nano / 1000_000_00;
        } else if (nano % 1000_000_0 == 0) {
            len += 3;
            small = nano / 1000_000_0;
        } else if (nano % 1000_000 == 0) {
            len += 4;
            small = nano / 1000_000;
        } else if (nano % 1000_00 == 0) {
            len += 5;
            small = nano / 1000_00;
        } else if (nano % 1000_0 == 0) {
            len += 6;
            small = nano / 1000_0;
        } else if (nano % 1000 == 0) {
            len += 7;
            small = nano / 1000;
        } else if (nano % 100 == 0) {
            len += 8;
            small = nano / 100;
        } else if (nano % 10 == 0) {
            len += 9;
            small = nano / 10;
        } else {
            len += 10;
            small = nano;
        }

        byte[] chars = new byte[len];
        chars[0] = '"';
        Arrays.fill(chars, 1, len - 1, (byte) '0');
        IOUtils.getChars(year, yearSize + 1, chars);
        chars[yearSize + 1] = '-';
        IOUtils.getChars(month, yearSize + 4, chars);
        chars[yearSize + 4] = '-';
        IOUtils.getChars(dayOfMonth, yearSize + 7, chars);
        chars[yearSize + 7] = 'T';
        IOUtils.getChars(hour, yearSize + 10, chars);
        chars[yearSize + 10] = ':';
        IOUtils.getChars(minute, yearSize + 13, chars);
        chars[yearSize + 13] = ':';
        IOUtils.getChars(second, yearSize + 16, chars);
        if (small != 0) {
            chars[yearSize + 16] = '.';
            IOUtils.getChars(small, len - 1, chars);
        }
        chars[len - 1] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
        byte[] chars = new byte[12];

        chars[0] = '"';
        chars[1] = (byte) (year / 1000 + '0');
        chars[2] = (byte) ((year / 100) % 10 + '0');
        chars[3] = (byte) ((year / 10) % 10 + '0');
        chars[4] = (byte) (year % 10 + '0');
        chars[5] = '-';
        chars[6] = (byte) (month / 10 + '0');
        chars[7] = (byte) (month % 10 + '0');
        chars[8] = '-';
        chars[9] = (byte) (dayOfMonth / 10 + '0');
        chars[10] = (byte) (dayOfMonth % 10 + '0');
        chars[11] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeTimeHHMMSS8(int hour, int minute, int second) {
        byte[] chars = new byte[10];

        chars[0] = '"';
        chars[1] = (byte) (hour / 10 + '0');
        chars[2] = (byte) (hour % 10 + '0');
        chars[3] = ':';
        chars[4] = (byte) (minute / 10 + '0');
        chars[5] = (byte) (minute % 10 + '0');
        chars[6] = ':';
        chars[7] = (byte) (second / 10 + '0');
        chars[8] = (byte) (second % 10 + '0');
        chars[9] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeLocalTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();
        int nano = time.getNano();

        int len = 10;
        int small;
        if (nano % 1000_000_000 == 0) {
            small = 0;
        } else if (nano % 1000_000_00 == 0) {
            len += 2;
            small = nano / 1000_000_00;
        } else if (nano % 1000_000_0 == 0) {
            len += 3;
            small = nano / 1000_000_0;
        } else if (nano % 1000_000 == 0) {
            len += 4;
            small = nano / 1000_000;
        } else if (nano % 1000_00 == 0) {
            len += 5;
            small = nano / 1000_00;
        } else if (nano % 1000_0 == 0) {
            len += 6;
            small = nano / 1000_0;
        } else if (nano % 1000 == 0) {
            len += 7;
            small = nano / 1000;
        } else if (nano % 100 == 0) {
            len += 8;
            small = nano / 100;
        } else if (nano % 10 == 0) {
            len += 9;
            small = nano / 10;
        } else {
            len += 10;
            small = nano;
        }

        byte[] chars = new byte[len];
        chars[0] = '"';
        Arrays.fill(chars, 1, chars.length - 1, (byte) '0');
        IOUtils.getChars(hour, 3, chars);
        chars[3] = ':';
        IOUtils.getChars(minute, 6, chars);
        chars[6] = ':';
        IOUtils.getChars(second, 9, chars);
        if (small != 0) {
            chars[9] = '.';
            IOUtils.getChars(small, len - 1, chars);
        }
        chars[len - 1] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeZonedDateTime(ZonedDateTime dateTime) {
        if (dateTime == null) {
            writeNull();
            return;
        }

        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int dayOfMonth = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        int nano = dateTime.getNano();
        String zoneId = dateTime.getZone().getId();

        int len = 17;

        int zoneSize;
        if (zoneId.equals("UTC")) {
            zoneId = "Z";
            zoneSize = 1;
        } else {
            zoneSize = 2 + zoneId.length();
        }
        len += zoneSize;

        int yearSize = IOUtils.stringSize(year);
        len += yearSize;
        int small;
        if (nano % 1000_000_000 == 0) {
            small = 0;
        } else if (nano % 1000_000_00 == 0) {
            len += 2;
            small = nano / 1000_000_00;
        } else if (nano % 1000_000_0 == 0) {
            len += 3;
            small = nano / 1000_000_0;
        } else if (nano % 1000_000 == 0) {
            len += 4;
            small = nano / 1000_000;
        } else if (nano % 1000_00 == 0) {
            len += 5;
            small = nano / 1000_00;
        } else if (nano % 1000_0 == 0) {
            len += 6;
            small = nano / 1000_0;
        } else if (nano % 1000 == 0) {
            len += 7;
            small = nano / 1000;
        } else if (nano % 100 == 0) {
            len += 8;
            small = nano / 100;
        } else if (nano % 10 == 0) {
            len += 9;
            small = nano / 10;
        } else {
            len += 10;
            small = nano;
        }

        byte[] chars = new byte[len];
        chars[0] = '"';
        Arrays.fill(chars, 1, chars.length - 1, (byte) '0');
        IOUtils.getChars(year, yearSize + 1, chars);
        chars[yearSize + 1] = '-';
        IOUtils.getChars(month, yearSize + 4, chars);
        chars[yearSize + 4] = '-';
        IOUtils.getChars(dayOfMonth, yearSize + 7, chars);
        chars[yearSize + 7] = 'T';
        IOUtils.getChars(hour, yearSize + 10, chars);
        chars[yearSize + 10] = ':';
        IOUtils.getChars(minute, yearSize + 13, chars);
        chars[yearSize + 13] = ':';
        IOUtils.getChars(second, yearSize + 16, chars);
        if (small != 0) {
            chars[yearSize + 16] = '.';
            IOUtils.getChars(small, len - 1 - zoneSize, chars);
        }
        if (zoneSize == 1) {
            chars[len - 2] = 'Z';
        } else {
            chars[len - zoneSize - 1] = '[';
            zoneId.getBytes(0, zoneId.length(), chars, len - zoneSize);
            chars[len - 2] = ']';
        }
        chars[len - 1] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeBigInt(BigInteger value, long features) {
        if (value == null) {
            writeNull();
            return;
        }

        String str = value.toString(10);

        if (((context.features | features) & Feature.BrowserCompatible.mask) != 0
                && (value.compareTo(LOW_BIGINT) < 0 || value.compareTo(HIGH_BIGINT) > 0)) {
            writeString(str);
            return;
        }

        int strlen = str.length();
        {
            // inline ensureCapacity
            int minCapacity = off + strlen;
            if (minCapacity - this.bytes.length > 0) {
                int oldCapacity = this.bytes.length;
                int newCapacity = oldCapacity + (oldCapacity >> 1);
                if (newCapacity - minCapacity < 0) {
                    newCapacity = minCapacity;
                }
                if (newCapacity - MAX_ARRAY_SIZE > 0) {
                    throw new OutOfMemoryError();
                }

                // minCapacity is usually close to size, so this is a win:
                this.bytes = Arrays.copyOf(this.bytes, newCapacity);
            }
        }
        str.getBytes(0, strlen, this.bytes, off);
        off += strlen;
    }

    @Override
    public void writeDateTimeISO8601(
            int year,
            int month,
            int dayOfMonth,
            int hour,
            int minute,
            int second,
            int millis,
            int offsetSeconds
    ) {
        int millislen = millis == 0 ? 0 : IOUtils.stringSize(millis) + 1;
        if (millis == 0) {
            millislen = 0;
        } else if (millis < 10) {
            millislen = 4;
        } else {
            if (millis % 100 == 0) {
                millislen = 2;
            } else if (millis % 10 == 0) {
                millislen = 3;
            } else {
                millislen = 4;
            }
        }
        int zonelen = offsetSeconds == 0 ? 1 : 6;
        int offset = offsetSeconds / 3600;
        int len = 21 + millislen + zonelen;
        byte[] chars = new byte[len];

        chars[0] = '"';
        chars[1] = (byte) (year / 1000 + '0');
        chars[2] = (byte) ((year / 100) % 10 + '0');
        chars[3] = (byte) ((year / 10) % 10 + '0');
        chars[4] = (byte) (year % 10 + '0');
        chars[5] = '-';
        chars[6] = (byte) (month / 10 + '0');
        chars[7] = (byte) (month % 10 + '0');
        chars[8] = '-';
        chars[9] = (byte) (dayOfMonth / 10 + '0');
        chars[10] = (byte) (dayOfMonth % 10 + '0');
        chars[11] = 'T';
        chars[12] = (byte) (hour / 10 + '0');
        chars[13] = (byte) (hour % 10 + '0');
        chars[14] = ':';
        chars[15] = (byte) (minute / 10 + '0');
        chars[16] = (byte) (minute % 10 + '0');
        chars[17] = ':';
        chars[18] = (byte) (second / 10 + '0');
        chars[19] = (byte) (second % 10 + '0');
        if (millislen > 0) {
            chars[20] = '.';
            Arrays.fill(chars, 21, 20 + millislen, (byte) '0');
            if (millis < 10) {
                IOUtils.getChars(millis, 20 + millislen, chars);
            } else {
                if (millis % 100 == 0) {
                    IOUtils.getChars(millis / 100, 20 + millislen, chars);
                } else if (millis % 10 == 0) {
                    IOUtils.getChars(millis / 10, 20 + millislen, chars);
                } else {
                    IOUtils.getChars(millis, 20 + millislen, chars);
                }
            }
        }
        if (offsetSeconds == 0) {
            chars[20 + millislen] = 'Z';
        } else {
            int offsetAbs = Math.abs(offset);
            int offsetlen = IOUtils.stringSize(offsetAbs);

            if (offset >= 0) {
                chars[20 + millislen] = '+';
            } else {
                chars[20 + millislen] = '-';
            }
            chars[20 + millislen + 1] = '0';
            IOUtils.getChars(offsetAbs, 20 + millislen + offsetlen + 2, chars);
            chars[20 + millislen + 3] = ':';
            chars[20 + millislen + 4] = '0';
            int offsetMinutes = (offsetSeconds - offset * 3600) / 60;
            IOUtils.getChars(offsetMinutes, 20 + millislen + zonelen, chars);
        }
        chars[chars.length - 1] = '"';

        writeRaw(chars);
    }

    @Override
    public void writeDecimal(BigDecimal value) {
        if (value == null) {
            writeNull();
            return;
        }

        String str = value.toString();

        if ((context.features & Feature.BrowserCompatible.mask) != 0
                && (value.compareTo(LOW) < 0 || value.compareTo(HIGH) > 0)) {
            final int strlen = str.length();
            ensureCapacity(off + strlen + 2);
            bytes[off++] = '"';
            str.getBytes(0, strlen, bytes, off);
            off += strlen;
            bytes[off++] = '"';
        } else {
            final int strlen = str.length();
            ensureCapacity(off + strlen);
            str.getBytes(0, strlen, bytes, off);
            off += strlen;
        }
    }

    @Override
    public String toString() {
        return new String(bytes, 0, off, StandardCharsets.UTF_8);
    }

    static void formatUnsignedLong0(long val, byte[] buf, int offset, int len) { // for uuid
        int charPos = offset + len;
        int radix = 16;
        int mask = radix - 1;
        do {
            buf[--charPos] = (byte) DIGITS[((int) val) & mask];
            val >>>= 4;
        } while (charPos > offset);
    }
}
