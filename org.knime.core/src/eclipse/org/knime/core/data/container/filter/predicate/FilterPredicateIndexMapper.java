/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.data.container.filter.predicate;

import org.knime.core.data.container.RearrangeColumnsTable;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.And;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.Or;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.CustomPredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.EqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.MissingValuePredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.NotEqualTo;
import org.knime.core.data.container.filter.predicate.FilterPredicate.Visitor;

/**
 * Helper class that maps the indices of all {@link ColumnPredicate ColumnPredicates} held by a {@link FilterPredicate}
 * to new values. This is helpful if columns are rearranged and the indices held by the predicate have to be rearranged
 * as well, as for instance in the case of {@link RearrangeColumnsTable RearrangeColumnsTables}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterPredicateIndexMapper implements Visitor<Void> {

    private final int[] m_map;

    /**
     * Constructs a new index mapper visitor. Initialized with an array that maps the old index values onto their new
     * positions. For instance, an index map of [0, 2, 1] entails that the column at the current index 1 is mapped onto
     * the new index 2 and vice versa.
     *
     * @param map an array that maps the old indices onto their new positions
     */
    public FilterPredicateIndexMapper(final int[] map) {
        m_map = map;
    }

    private <T> Void visitColumnPredicate(final ColumnPredicate<T> pred) {
        Column<T> col = pred.getColumn();
        if (col instanceof IndexedColumn) {
            IndexedColumn<T> indexedCol = (IndexedColumn<T>)col;
            indexedCol.setIndex(m_map[indexedCol.getIndex()]);
        }
        return null;
    }

    @Override
    public <T> Void visit(final MissingValuePredicate<T> mvp) {
        return visitColumnPredicate(mvp);
    }

    @Override
    public <T> Void visit(final CustomPredicate<T> udf) {
        return visitColumnPredicate(udf);
    }

    @Override
    public <T> Void visit(final EqualTo<T> eq) {
        return visitColumnPredicate(eq);
    }

    @Override
    public <T> Void visit(final NotEqualTo<T> neq) {
        return visitColumnPredicate(neq);
    }

    @Override
    public <T extends Comparable<T>> Void visit(final LesserThan<T> lt) {
        return visitColumnPredicate(lt);
    }

    @Override
    public <T extends Comparable<T>> Void visit(final LesserThanOrEqualTo<T> leq) {
        return visitColumnPredicate(leq);
    }

    @Override
    public <T extends Comparable<T>> Void visit(final GreaterThan<T> gt) {
        return visitColumnPredicate(gt);
    }

    @Override
    public <T extends Comparable<T>> Void visit(final GreaterThanOrEqualTo<T> geq) {
        return visitColumnPredicate(geq);
    }

    @Override
    public Void visit(final And and) {
        and.getLeft().accept(this);
        and.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(final Or or) {
        or.getLeft().accept(this);
        or.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(final Not not) {
        not.getPredicate().accept(this);
        return null;
    }

}
