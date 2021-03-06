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

package com.jitlogic.zorka.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks information on all symbol strings used by tracer. These are mainly trace names,
 * class names, method names and method signature strings. Maintains name-to-ID maps, so
 * tracer can use just integer IDs internally but present human-readable names when necessary.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SymbolRegistry {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(SymbolRegistry.class);

    /** ID of last symbol added to registry. */
    private AtomicInteger lastSymbolId = new AtomicInteger(0);

    /** Symbol name to ID map */
    private ConcurrentHashMap<String,Integer> symbolIds = new ConcurrentHashMap<String, Integer>();

    /** Symbol ID to name map */
    private ConcurrentHashMap<Integer,String> symbolNames = new ConcurrentHashMap<Integer,String>();

    /**
     * Returns ID of named symbol. If symbol hasn't been registered yet,
     * it will be and new ID will be assigned for it.
     *
     * @param symbol symbol name
     *
     * @return symbol ID (integer)
     */
    public int symbolId(String symbol) {

        if (symbol == null) {
            return 0;
        }

        Integer id = symbolIds.get(symbol);

        if (id == null) {
            int newid = lastSymbolId.incrementAndGet();

            log.debug(ZorkaLogger.ZTR_SYMBOL_REGISTRY, "Adding symbol '%s', newid=%s", symbol, newid);

            id = symbolIds.putIfAbsent(symbol, newid);
            if (id == null) {
                symbolNames.put(newid, symbol);
                id = newid;
            }
        }

        return id;
    }


    /**
     * Returns symbol name based on ID or null if no such symbol has been registered.
     *
     * @param symbolId symbol ID
     *
     * @return symbol name
     */
    public String symbolName(int symbolId) {
        if (symbolId == 0) {
            return "<null>";
        }
        return symbolNames.get(symbolId);
    }


    /**
     * Adds new symbol to registry (with predefined ID).
     *
     * @param symbolId symbol ID
     *
     * @param symbol symbol name
     */
    public void put(int symbolId, String symbol) {

        log.debug(ZorkaLogger.ZTR_SYMBOL_REGISTRY, "Putting symbol '%s', newid=%s", symbol, symbolId);

        symbolIds.put(symbol, symbolId);
        symbolNames.put(symbolId, symbol);

        // TODO not thread safe !
        if (symbolId > lastSymbolId.get()) {
            lastSymbolId.set(symbolId);
        }
    }

    public int size() {
        return symbolIds.size();
    }

}
