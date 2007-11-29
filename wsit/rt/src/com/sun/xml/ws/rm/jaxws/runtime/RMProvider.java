/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.xml.ws.rm.jaxws.runtime;

import com.sun.xml.ws.rm.jaxws.util.ProcessingFilter;

/**
 * RMProvider is a base class for <code>RMSource</code> and 
 * <code>RMDestination</code> that provides storage for Lists of
 * <code>InboundSequences</code> and <code>OutboundSequences</code> and
 * handles the processing of messages coming from the network.
 *
 */
public abstract class RMProvider {

    /*
     * Instance of a Helper class to handle inbound messages based 
     * on their WS-RM protocol headers
     */
    private InboundMessageProcessor messageProcessor;
    private ProcessingFilter filter;

    /**
     * Constructor
     */
    public RMProvider() {
        this.messageProcessor = new InboundMessageProcessor(this);
    }

    /**
     * Sets and instance of ProcessingFilter to be used for diagnostic/debugging
     * purposes.  
     * 
     * @param filter The ProcessingFilter to be used.
     */
    public void setProcessingFilter(ProcessingFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns the ProcessingFilter instance that has been designated for use for
     * diagnostic/debugging purposes.
     *
     * @return The filter that has been set by calling <code>setProcessingFilter</code>.
     * Returns null if no ProcessingFilter has been set.
     */
    public ProcessingFilter getProcessingFilter() {
        return filter;
    }

    /**
     * Look up <code>OutboundSequence</code> with given id.
     *
     * @param The sequence id
     */
    public abstract OutboundSequence getOutboundSequence(String id);

    /**
     * Look up <code>OutboundSequence</code> with given id.
     *
     * @param The sequence id
     */
    public abstract InboundSequence getInboundSequence(String id);

    public final InboundMessageProcessor getInboundMessageProcessor() {
        return messageProcessor;
    }
}
