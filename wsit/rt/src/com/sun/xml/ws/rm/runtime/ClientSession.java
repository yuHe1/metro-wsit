/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.xml.ws.rm.runtime;

import com.sun.xml.ws.rm.runtime.sequence.SequenceManagerFactory;
import com.sun.xml.ws.rm.runtime.sequence.UnknownSequenceException;
import com.sun.xml.ws.rm.runtime.sequence.SequenceManager;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.rm.RmException;
import com.sun.xml.ws.rm.RmRuntimeException;
import com.sun.xml.ws.rm.localization.LocalizationMessages;
import com.sun.xml.ws.rm.localization.RmLogger;
import com.sun.xml.ws.rm.policy.Configuration;
import com.sun.xml.ws.rm.runtime.sequence.Sequence;
import com.sun.xml.ws.security.secext10.SecurityTokenReferenceType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * <p>
 * RM client session represents a contract between single WS proxy and it's corresponding service. Multiple tubelines (of the same
 * WS proxy) may share a single RM session, each WS proxy however creates it's own client session.
 * </p>
 * <p>
 * RM client session performs all tasks related to RM message processing, while being focused on a single reliable connection.
 * </p>
 * 
 * @author Marek Potociar (marek.potociar at sun.com)
 */
abstract class ClientSession {

    private static final RmLogger LOGGER = RmLogger.getLogger(ClientSession.class);
    private static final int MAX_INITIATE_SESSION_ATTEMPTS = 3;
    //
    String inboundSequenceId = null;
    String outboundSequenceId = null;
    final Configuration configuration;
    final SequenceManager sequenceManager;
    final ProtocolCommunicator communicator;
    //
    private final Lock initLock;
    private final ScheduledTaskManager scheduledTaskManager;
    private final AtomicLong lastAckRequestedTime = new AtomicLong(0);
    private final PeriodicFiberResumeTask resendTask;

    static ClientSession create(Configuration configuration, ProtocolCommunicator communicator) {
        switch (configuration.getRmVersion()) {
            case WSRM10:
                return new Rm10ClientSession(configuration, communicator);
            case WSRM11:
                return new Rm11ClientSession(configuration, communicator);
            default:
                throw new IllegalStateException(LocalizationMessages.WSRM_1104_RM_VERSION_NOT_SUPPORTED(configuration.getRmVersion().namespaceUri));
        }
    }

    ClientSession(Configuration configuration, ProtocolCommunicator communicator) {
        this.initLock = new ReentrantLock();
        this.configuration = configuration;
        this.sequenceManager = SequenceManagerFactory.INSTANCE.getClientSequenceManager();
        this.communicator = communicator;
        this.scheduledTaskManager = new ScheduledTaskManager();
        this.resendTask = new PeriodicFiberResumeTask(configuration.getMessageRetransmissionInterval());
    }

    abstract void openRmSession(String offerInboundSequenceId, SecurityTokenReferenceType strType) throws RmRuntimeException;

    abstract void closeOutboundSequence() throws RmException;

    abstract void terminateOutboundSequence() throws RmException;

    final void processInboundMessageHeaders(PacketAdapter responseAdapter, boolean expectSequenceHeader) throws RmRuntimeException {
        if (expectSequenceHeader) {
            String sequenceId = responseAdapter.getSequenceId();
            if (sequenceId != null) {
                Utilities.assertSequenceId(inboundSequenceId, sequenceId);
                sequenceManager.getSequence(sequenceId).acknowledgeMessageId(responseAdapter.getMessageNumber());
            } else {
                throw new RmRuntimeException(LocalizationMessages.WSRM_1118_MANDATORY_HEADER_NOT_PRESENT("wsrm:Sequence"));
            }
        }

        String ackRequestedSequenceId = responseAdapter.getAckRequestedHeaderSequenceId();
        if (ackRequestedSequenceId != null) {
            Utilities.assertSequenceId(inboundSequenceId, ackRequestedSequenceId);
            sequenceManager.getSequence(ackRequestedSequenceId).setAckRequestedFlag();
        }

        responseAdapter.processAcknowledgements(sequenceManager, outboundSequenceId);
    }

    final void requestAcknowledgement() throws RmException {
        PacketAdapter responseAdapter = null;
        try {
            PacketAdapter requestAdapter = PacketAdapter.getInstance(configuration, communicator.createEmptyRequestPacket());
            requestAdapter.setEmptyRequestMessage(configuration.getRmVersion().ackRequestedAction).appendAckRequestedHeader(outboundSequenceId);

            responseAdapter = PacketAdapter.getInstance(configuration, communicator.send(requestAdapter.getPacket()));
            if (!responseAdapter.containsMessage()) {
                throw new RmException(LocalizationMessages.WSRM_1108_NULL_RESPONSE_FOR_ACK_REQUEST());
            }

            processInboundMessageHeaders(responseAdapter, false);

            if (responseAdapter.isFault()) {
                // FIXME: refactor the exception creation - we should not pass the SOAP fault directly into the exception
                throw new RmException(LocalizationMessages.WSRM_1109_SOAP_FAULT_RESPONSE_FOR_ACK_REQUEST(), responseAdapter.message);
            }
        } finally {
            if (responseAdapter != null) {
                responseAdapter.consume();
            }
        }
    }

    final Packet processOutgoingPacket(Packet requestPacket) {
        PacketAdapter requestAdapter = PacketAdapter.getInstance(configuration, requestPacket);
        initializeIfNecessary(requestAdapter);

        requestAdapter.appendSequenceHeader(
                outboundSequenceId,
                sequenceManager.getSequence(outboundSequenceId).generateNextMessageId());
        if (isPendingAckRequest()) {
            requestAdapter.appendAckRequestedHeader(outboundSequenceId);
            lastAckRequestedTime.set(System.currentTimeMillis());
        }
        if (inboundSequenceId != null) {
            Sequence inboundSequence = sequenceManager.getSequence(inboundSequenceId);

            if (inboundSequence.getLastMessageId() > 0 /*sequence has been used already*/) { // FIXME: create an API method to test this
                requestAdapter.appendSequenceAcknowledgementHeader(sequenceManager.getSequence(inboundSequenceId));
            }
        }

        return requestAdapter.getPacket();
    }

    final Packet processIncommingPacket(Packet responsePacket, boolean responseToOneWayRequest) throws RmRuntimeException {
        PacketAdapter responseAdapter = PacketAdapter.getInstance(configuration, responsePacket);
        if (responseAdapter.containsMessage()) {
            processInboundMessageHeaders(responseAdapter, !responseToOneWayRequest && !responseAdapter.isProtocolMessage());
        }

        return responseAdapter.getPacket();
    }

    /**
     * Registers given fiber for a resend (resume)
     * 
     * @param fiber a fiber to be resumed after resend interval has passed
     * @return {@code true} if the fiber was successfully registered; {@code false} otherwise.
     */
    final boolean registerForResend(Fiber fiber, Packet packet) {
        return resendTask.registerForResume(fiber, packet);
    }

    /**
     * Closes and terminates associated sequences and releases other resources associated with this RM session
     */
    final void close() {
        try {
            if (outboundSequenceId != null && sequenceManager.isValid(outboundSequenceId)) {
                try {
                    closeOutboundSequence();
                } catch (RmException ex) {
                    LOGGER.logException(ex, Level.WARNING);
                } finally {
                    try {
                        sequenceManager.closeSequence(outboundSequenceId);
                    } catch (UnknownSequenceException ex) {
                        LOGGER.logException(ex, Level.WARNING);
                    }
                }

                try {
                    waitUntilAllRequestsAckedOrTimeout();
                    terminateOutboundSequence();
                } catch (RmException ex) {
                    LOGGER.logException(ex, Level.WARNING);
                } finally {
                    try {
                        sequenceManager.terminateSequence(outboundSequenceId);
                    } catch (UnknownSequenceException ex) {
                        LOGGER.logException(ex, Level.WARNING);
                    }
                }
            }

            if (inboundSequenceId != null && sequenceManager.isValid(inboundSequenceId)) {
                try {
                    if (!sequenceManager.getSequence(inboundSequenceId).isClosed()) {
                        sequenceManager.closeSequence(inboundSequenceId);
                    }
                    // TODO wait for an external event?
                    sequenceManager.terminateSequence(inboundSequenceId);
                } catch (UnknownSequenceException ex) {
                    LOGGER.logException(ex, Level.WARNING);
                }
            }
        } finally {
            scheduledTaskManager.stopAll();
        }
    }

    /**
     * Performs late initialization of sequences and timer task, provided those have not yet been initialized.
     * The actual initialization thus happens only once in the lifetime of each client RM session object.
     */
    private void initializeIfNecessary(PacketAdapter request) throws RmRuntimeException {
        initLock.lock();
        try {
            if (!isInitialized()) {
                communicator.registerMusterRequestPacket(request.copyPacket(false));

                int numberOfInitiateSessionAttempts = 0;
                while (true) {
                    try {
                        openRmSession(
                                (configuration.requestResponseOperationsDetected()) ? sequenceManager.generateSequenceUID() : null,
                                communicator.tryStartSecureConversation());
                        break;
                    } catch (Exception ex) {
                        LOGGER.warning(LocalizationMessages.WSRM_1106_RM_SESSION_INIT_ATTEMPT_FAILED(), ex);
                    } finally {
                        if (++numberOfInitiateSessionAttempts > MAX_INITIATE_SESSION_ATTEMPTS) {
                            throw LOGGER.logSevereException(new RmRuntimeException(LocalizationMessages.WSRM_1107_MAX_RM_SESSION_INIT_ATTEMPTS_REACHED()));
                        }
                    }
                }

                scheduledTaskManager.startTasks(resendTask, createAckRequesterTask());
            }
        } finally {
            initLock.unlock();
        }
    }

    private boolean isInitialized() {
        return outboundSequenceId != null;
    }

    /**
     * Send Message with empty body and a AckRequestedElement (with Last child) down the pipe. Process the response,
     * which may contain a SequenceAcknowledgementElement.
     *
     * @param seq Outbound sequence to which SequenceHeaderElement will belong.
     *
     */
    private Runnable createAckRequesterTask() {
        return new Runnable() {

            public void run() {
                try {
                    if (isPendingAckRequest()) {
                        requestAcknowledgement();
                        lastAckRequestedTime.set(System.currentTimeMillis());
                    }
                } catch (RmException ex) {
                    LOGGER.warning(LocalizationMessages.WSRM_1110_ACK_REQUEST_FAILED(), ex);
                }
            }
        };
    }

    private boolean isPendingAckRequest() throws RmRuntimeException {
        return lastAckRequestedTime.get() - System.currentTimeMillis() > configuration.getAcknowledgementRequestInterval() &&
                sequenceManager.getSequence(outboundSequenceId).hasPendingAcknowledgements();
    }

    private void waitUntilAllRequestsAckedOrTimeout() {
        final CountDownLatch doneSignal = new CountDownLatch(1);
        ScheduledFuture<?> taskHandle = scheduledTaskManager.startTask(new Runnable() {

            public void run() {
                try {
                    if (!sequenceManager.getSequence(outboundSequenceId).hasPendingAcknowledgements()) {
                        doneSignal.countDown();
                    }
                } catch (UnknownSequenceException ex) {
                    LOGGER.severe(LocalizationMessages.WSRM_1111_UNEXPECTED_EXCEPTION_WHILE_WAITING_FOR_SEQ_ACKS(), ex);
                    doneSignal.countDown();
                }
            }
        });
        try {
            if (configuration.getCloseSequenceOperationTimeout() > 0) {
                boolean waitResult = doneSignal.await(configuration.getCloseSequenceOperationTimeout(), TimeUnit.MILLISECONDS);
                if (!waitResult) {
                    LOGGER.info(LocalizationMessages.WSRM_1112_CLOSE_OUTBOUND_SEQUENCE_TIMED_OUT(outboundSequenceId));
                }
            } else {
                doneSignal.await();
            }
        } catch (InterruptedException ex) {
            LOGGER.fine(LocalizationMessages.WSRM_1113_CLOSE_OUTBOUND_SEQUENCE_INTERRUPTED(outboundSequenceId), ex);
        } finally {
            taskHandle.cancel(true);
        }
    }
}