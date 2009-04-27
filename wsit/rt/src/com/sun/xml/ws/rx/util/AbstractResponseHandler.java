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

package com.sun.xml.ws.rx.util;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.commons.Logger;
import com.sun.xml.ws.rx.RxRuntimeException;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class AbstractResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(AbstractResponseHandler.class);
    private final TimestampedCollection<String, Fiber> suspendedFiberStorage;
    private String correlationId;

    public AbstractResponseHandler(TimestampedCollection<String, Fiber> suspendedFiberStorage, String correlationId) {
        this.suspendedFiberStorage = suspendedFiberStorage;
        this.correlationId = correlationId;
    }

    protected final Fiber getParentFiber() {
        return suspendedFiberStorage.remove(correlationId);
    }

    protected final void resumeParentFiber(Packet response) throws RxRuntimeException {
        Fiber parent = getParentFiber();
        if (parent == null) {
            // TODO L10N
            throw LOGGER.logSevereException(new RxRuntimeException(String.format("No parent fiber found for correlationId [ %s ]. " + "Unable to resume parent fiber with a response packet.", correlationId)));
        }
        parent.resume(response);
    }

    protected final void resumeParentFiber(Throwable error) throws RxRuntimeException {
        Fiber parent = getParentFiber();
        if (parent == null) {
            // TODO L10N
            throw LOGGER.logSevereException(new RxRuntimeException(String.format("No parent fiber found for correlationId [ %s ]. " + "Unable to resume parent fiber with a fiber-processing error. " + "(Original fiber-processing error attached as a nested exception)", correlationId), error));
        }
        parent.resume(error);
    }

    protected final String getCorrelationId() {
        return correlationId;
    }

    protected final void setCorrelationId(String newCorrelationId) {
        this.correlationId = newCorrelationId;
    }

}