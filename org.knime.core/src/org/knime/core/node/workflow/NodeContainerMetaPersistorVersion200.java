/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 25, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeMessage.Type;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class NodeContainerMetaPersistorVersion200 extends
        NodeContainerMetaPersistorVersion1xx {
    
    private static final String CFG_STATE = "state";
    private static final String CFG_IS_DELETABLE = "isDeletable";
    private static final String CFG_JOB_MANAGER_CONFIG = "job.manager";
    private static final String CFG_JOB_CONFIG = "execution.job";
    
    /** @param baseDir The node container directory (only important while load)
     */
    NodeContainerMetaPersistorVersion200(final ReferencedFile baseDir) {
        super(baseDir);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeExecutionJobManager loadNodeExecutionJobManager(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(CFG_JOB_MANAGER_CONFIG)) {
            return null;
        }
        return NodeExecutionJobManagerPool.load(
                settings.getNodeSettings(CFG_JOB_MANAGER_CONFIG));
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadNodeExecutionJobSettings(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(CFG_JOB_CONFIG)) {
            return null;
        }
        return settings.getNodeSettings(CFG_JOB_CONFIG);
    }
    
    /** {@inheritDoc} */
    @Override
    protected State loadState(final NodeSettingsRO settings, final NodeSettingsRO parentSettings)
            throws InvalidSettingsException {
        String stateString = settings.getString(CFG_STATE);
        if (stateString == null) {
            throw new InvalidSettingsException("State information is null");
        }
        try {
            return State.valueOf(stateString);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Unable to parse state \""
                    + stateString + "\"");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean loadIsDeletable(final NodeSettingsRO settings) {
        return settings.getBoolean(CFG_IS_DELETABLE, true);
    }

    /** {@inheritDoc} */
    @Override
    protected String loadCustomName(final NodeSettingsRO settings,
            final NodeSettingsRO parentSettings) 
    throws InvalidSettingsException {
        if (!settings.containsKey(KEY_CUSTOM_NAME)) {
            return null;
        }
        return settings.getString(KEY_CUSTOM_NAME);
    }

    /** {@inheritDoc} */
    @Override
    protected String loadCustomDescription(final NodeSettingsRO settings,
            final NodeSettingsRO parentSettings) 
    throws InvalidSettingsException {
        if (!settings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
            return null;
        }
        return settings.getString(KEY_CUSTOM_DESCRIPTION);
    }

    /** {@inheritDoc} */
    @Override
    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey("node_message")) {
            NodeSettingsRO sub = settings.getNodeSettings("node_message");
            String typeS = sub.getString("type");
            if (typeS == null) {
                throw new InvalidSettingsException(
                        "Message type must not be null");
            }
            Type type;
            try {
                type = Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid message type: "
                        + typeS, iae);
            }
            String message = sub.getString("message");
            return new NodeMessage(type, message);
        }
        return null;
    }
    
    public void save(final NodeContainer nc, final NodeSettingsWO settings)
        throws IOException {
        saveCustomName(settings, nc);
        saveCustomDescription(settings, nc);
        saveNodeExecutionJobManager(settings, nc);
        boolean mustAlsoSaveExecutorSettings = saveState(settings, nc);
        if (mustAlsoSaveExecutorSettings) {
            saveNodeExecutionJob(settings, nc);
        }
        saveNodeMessage(settings, nc);
        saveIsDeletable(settings, nc);
    }

    protected void saveNodeExecutionJobManager(final NodeSettingsWO settings,
            final NodeContainer nc) {
        NodeExecutionJobManager jobManager = nc.getJobManager();
        if (jobManager != null) {
            NodeSettingsWO s = settings.addNodeSettings(CFG_JOB_MANAGER_CONFIG);
            NodeExecutionJobManagerPool.saveJobManager(jobManager, s);
        }
    }
    
    protected void saveNodeExecutionJob(
            final NodeSettingsWO settings, final NodeContainer nc) {
        assert nc.getState().equals(State.EXECUTING)
            : "Can't save node execution job, node is not EXECUTING but "
                + nc.getState();
        NodeExecutionJobManager jobManager = nc.getJobManager();
        NodeExecutionJob job = nc.getExecutionJob();
        assert nc.findJobManager().canDisconnect(nc.getExecutionJob())
        : "Execution job can be saved/disconnected";
        NodeSettingsWO sub = settings.addNodeSettings(CFG_JOB_CONFIG);
        jobManager.saveReconnectSettings(job, sub);
    }

    protected boolean saveState(final NodeSettingsWO settings,
            final NodeContainer nc) {
        String state;
        boolean mustAlsoSaveExecutorSettings = false;
        switch (nc.getState()) {
        case IDLE:
        case UNCONFIGURED_MARKEDFOREXEC:
            state = State.IDLE.toString();
            break;
        case EXECUTED:
            state = State.EXECUTED.toString();
            break;
        case EXECUTING:
            if (nc.findJobManager().canDisconnect(nc.getExecutionJob())) {
                state = State.EXECUTING.toString();
                mustAlsoSaveExecutorSettings = true;
                break;
            }
        default:
            state = State.CONFIGURED.toString();
        }
        settings.addString(CFG_STATE, state);
        return mustAlsoSaveExecutorSettings;
    }
    
    protected void saveCustomName(final NodeSettingsWO settings,
            final NodeContainer nc) {
        settings.addString(KEY_CUSTOM_NAME, nc.getCustomName());
    }

    protected void saveCustomDescription(final NodeSettingsWO settings,
            final NodeContainer nc) {
        settings.addString(KEY_CUSTOM_DESCRIPTION, nc.getCustomDescription());
    }
    
    protected void saveIsDeletable(final NodeSettingsWO settings, 
            final NodeContainer nc) {
        if (!nc.isDeletable()) {
            settings.addBoolean(CFG_IS_DELETABLE, false);
        }
    }

    protected void saveNodeMessage(final NodeSettingsWO settings,
            final NodeContainer nc) {
        NodeMessage message = nc.getNodeMessage();
        if (message != null && !message.getMessageType().equals(Type.RESET)) {
            NodeSettingsWO sub = settings.addNodeSettings("node_message");
            sub.addString("type", message.getMessageType().name());
            sub.addString("message", message.getMessage());
        }
    }

}
