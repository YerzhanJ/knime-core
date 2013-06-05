/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on 31.05.2013 by thor
 */
package org.knime.core.node.workflow;

import java.util.ArrayDeque;
import java.util.Deque;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
 * A {@link NodeContext} holds information about the context in which an operation on a node is executed. This is used
 * for internal purposes, node implementors should not use this class.<br />
 * The node context is local to the current thread and is set by the workflow manager or the node container in which a
 * node is contained. Each thread has a stack of {@link NodeContext} objects, only the last set context can be retrieved
 * via {@link #getContext()}. You must absolutly make sure that if you push a new context that you remove it afterwards.
 * Therefore the common usage pattern is a follows:
 *
 * <pre>
 * NodeContext.pushContext(nodeContainer);
 * try {
 *     doSomething();
 * } finally {
 *     NodeContext.removeLastContext():
 * }
 * </pre>
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 * @noreference
 */
public final class NodeContext {
    private static final NodeLogger logger = NodeLogger.getLogger(NodeContext.class);

    private static final ThreadLocal<Deque<NodeContext>> threadLocal =
        new InheritableThreadLocal<Deque<NodeContext>>() {
            @Override
            protected Deque<NodeContext> childValue(final Deque<NodeContext> parentValue) {
                // make sure that child threads get a copy of the context stack and do not operate on the same stack!
                return new ArrayDeque<NodeContext>(parentValue);
            }
        };

    private final NodeContainer m_nodeContainer;

    private static final NodeContext NO_CONTEXT = new NodeContext(null);

    @SuppressWarnings("unused")
    private StackTraceElement[] m_callStack; // only used for debugging

    private NodeContext(final NodeContainer nodeContainer) {
        m_nodeContainer = nodeContainer;
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            m_callStack = new Throwable().getStackTrace();
        }
    }

    /**
     * Returns the workflow manager which currently does an operation on a node.
     *
     * @return the workflow manager associated with the current node
     */
    public WorkflowManager getWorkflowManager() {
        // find the actual workflow and not the meta node the container may be in
        WorkflowManager manager =
            (m_nodeContainer instanceof WorkflowManager) ? (WorkflowManager)m_nodeContainer : m_nodeContainer
                .getParent();
        while (!manager.isProject()) {
            manager = manager.getParent();
        }
        return manager;
    }

    /**
     * Returns the current node context or <code>null</code> if no context exists.
     *
     * @return a node context or <code>null</code>
     */
    public static NodeContext getContext() {
        NodeContext ctx = getContextStack().peek();
        if (ctx == NO_CONTEXT) {
            return null;
        } else {
            return ctx;
        }
    }

    /**
     * Pushes a new context on the context stack for the current thread using the given node container.
     *
     * @param nodeContainer the node container for the current thread, must not be <code>null</code>
     */
    public static void pushContext(final NodeContainer nodeContainer) {
        assert (nodeContainer != null) : "Node container must not be null";
        Deque<NodeContext> stack = getContextStack();
        stack.push(new NodeContext(nodeContainer));
    }

    /**
     * Removes the top-most context from the context stack.
     *
     * @throws IllegalStateException if no context is available / if the context stack is empty
     */
    public static void removeLastContext() {
        Deque<NodeContext> stack = getContextStack();
        if (stack.isEmpty()) {
            throw new IllegalStateException("No node context registered with the current thread");
        } else {
            stack.pop();
        }
    }

    /**
     * Pushes the given context on the context stack for the current thread. The context may be <code>null</code>.
     *
     * @param context an existing context, may be <code>null</code>
     */
    public static void pushContext(final NodeContext context) {
        Deque<NodeContext> stack = getContextStack();
        if (context == null) {
            stack.push(NO_CONTEXT);
        } else {
            stack.push(context);
        }
    }

    /**
     * Returns the context stack for the current thread. This methods has package scope on purpose so that the
     * {@link NodeExecutionJob} has access to the stack and can save and restore it. See {@link NodeExecutionJob#run()}.
     *
     * @return a stack with node contexts
     */
    static Deque<NodeContext> getContextStack() {
        Deque<NodeContext> stack = threadLocal.get();
        if (stack == null) {
            stack = new ArrayDeque<NodeContext>(4);
            threadLocal.set(stack);
        } else {
            if (stack.size() > 10) {
                logger.coding("Node context stack has more than 10 elements (" + stack.size()
                    + "), looks like we are leaking contexts somewhere");
            }
        }
        return stack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (m_nodeContainer == null) ? "NO CONTEXT" : m_nodeContainer.toString();
    }
}
