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

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.core.perfmon.Metric;
import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.util.ObjectInspector;

import java.util.Map;
import java.util.Set;

public class WindowedRateMetric extends Metric {

    private long lastNom, lastDiv;

    public WindowedRateMetric(int id, String name, Map<String, Object> attrs) {
        super(id, name, attrs);
    }

    public WindowedRateMetric(MetricTemplate template, Set<Map.Entry<String, Object>> attrSet) {
        super(template, attrSet);
    }

    @Override
    public Number getValue(long clock, Object value) {
        Number rawNom = (Number) ObjectInspector.get(value, getTemplate().getNomField());
        Number rawDiv = (Number) ObjectInspector.get(value, getTemplate().getDivField());

        if (rawNom == null || rawDiv == null) {
            return 0.0;
        }

        long curNom = rawNom.longValue();
        long curDiv = rawDiv.longValue();

        Double rslt = 0.0;

        if (curDiv - lastDiv > 0) {
            rslt = ((double)(curNom-lastNom)) / ((double)(curDiv-lastDiv));
        }

        lastNom = curNom;
        lastDiv = curDiv;

        Double multiplier = getTemplate().getMultiplier();
        return multiplier != 1.0 ? multiplier * rslt : rslt;
    }
}
