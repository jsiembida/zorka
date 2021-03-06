/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.perfmon.MetricsRegistry;
import com.jitlogic.zorka.core.perfmon.SimplePerfDataFormat;
import com.jitlogic.zorka.core.perfmon.Submittable;
import com.jitlogic.zorka.core.util.*;

import java.io.*;

/**
 * This class implements worker thread that receives traces submitted
 * by (instrumented) application threads, adds symbols and writes
 * to local files. Files are rotated, maximum number of files and
 * maximum file size are configurable.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceFileWriter extends ZorkaAsyncThread<Submittable> {

    /** Logger object. */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Symbol registry containing symbols for incoming traces. */
    private SymbolRegistry symbols;

    /** Symbol enricher responsible for adding missing symbols to generated files. */
    private SymbolEnricher enricher;

    /** Trace encoder. Simple uncompressed binary format is used at the moment. */
    private SimplePerfDataFormat encoder;

    /** Working byte buffer. */
    private ByteBuffer buffer;

    /** Maximum number of archived files. */
    private int maxFiles;

    /** Maximum file size. */
    private long maxFileSize;

    /** Path to trace file. */
    private File traceFile;

    /** Currently opened trace file. */
    private OutputStream stream;

    /** Current trace file size. */
    private long curSize;


    /**
     * Creates trace file writer.
     *
     * @param path path to trace file
     *
     * @param symbols symbol registry containing symbols from incoming traces
     */
//    public TraceFileWriter(String path, SymbolRegistry symbols, MetricsRegistry metricsRegistry) {
//        this(path, symbols, metricsRegistry, 8, 8 * 1024 * 1024);
//    }


    /**
     * Creates trace file writer.
     *
     * @param path path to trace file
     *
     * @param symbols symbol registry containing symbols from incoming traces
     *
     * @param maxFiles max number of archived trace files
     *
     * @param maxFileSize maximum trace file size
     */
    public TraceFileWriter(String path, SymbolRegistry symbols, MetricsRegistry metricsRegistry, int maxFiles, long maxFileSize) {
        super("trace-writer");
        this.symbols = symbols;
        this.buffer = new ByteBuffer(4096);
        this.encoder = new SimplePerfDataFormat(buffer);
        this.enricher = new SymbolEnricher(this.symbols, metricsRegistry, encoder);
        this.traceFile = new File(path);
        this.maxFiles = maxFiles;
        this.maxFileSize = maxFileSize;

        log.info(ZorkaLogger.ZTR_CONFIG, "Initialized trace file writer: path=" + path);
    }


    @Override
    protected void process(Submittable obj) {

        // TODO make sure this buffer won't grow too big ...
        // TODO reengineer to make it testable

        if (curSize > maxFileSize) {
            roll();
        }

        buffer.reset();
        obj.traverse(enricher);
        byte[] buf = buffer.getContent();

        curSize += buf.length;

        try {
            if (stream != null) {
                stream.write(buf);
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot write to trace file " + traceFile, e);
            roll();
        }
    }


    @Override
    protected void open() {
        roll();
    }


    /**
     * Rotates and reopens trace file.
     */
    private void roll() {

        if (stream != null) {
            close();
        }

        File f = new File(traceFile.getPath() + "." + maxFiles);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxFiles-1; i >= 0; i--) {
            f = new File(traceFile.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(traceFile.getPath() + "." + (i+1));
                f.renameTo(nf);
            }
        }

        traceFile.renameTo(new File(traceFile.getPath() + ".0"));

        try {
            stream = new BufferedOutputStream(new FileOutputStream(traceFile));
        } catch (FileNotFoundException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot open trace file " + traceFile, e);
        }

        enricher.reset();
        curSize = 0; // TODO unit test exposing lack of this line
    }


    @Override
    protected void close() {
        try {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot close trace file " + traceFile, e);
        }
    }


    @Override
    protected void flush() {
        try {
            stream.flush();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot flush trace file " + traceFile, e);
        }
    }
}
