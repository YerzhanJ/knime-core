/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node.config;

/**
 * Config entry for double values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigDoubleEntry extends AbstractConfigEntry {
    
    /** The double value. */
    private final double m_double;
    
    /**
     * Creates a new Config entry for type double.
     * @param key This entry's key.
     * @param d The double value.
     */
    ConfigDoubleEntry(final String key, final double d) {
        super(key, ConfigEntries.xdouble);
        m_double = d;
    }

    /**
     * Creates a new Config entry for type double.
     * @param key This entry's key.
     * @param d The double value as String.
     */
    ConfigDoubleEntry(final String key, final String d) {
        super(key, ConfigEntries.xdouble);
        m_double = Double.parseDouble(d);
    }
    
    /**
     * @return The double value.
     */
    public double getDouble() {
        return m_double;
    }

    /**
     * @return A String representation of this double value.
     * @see de.unikn.knime.core.node.config.ConfigurableEntry#toStringValue()
     */
    public String toStringValue() {
        return Double.toString(m_double);
    }

    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigDoubleEntry) ace).m_double == m_double;
    }

}
