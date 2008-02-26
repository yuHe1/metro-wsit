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

package com.sun.xml.ws.transport.tcp.wsit;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.ClientPipeAssemblerContext;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.xml.ws.assembler.WsitClientTubeAssemblyContext;
import com.sun.xml.ws.transport.tcp.client.*;
import com.sun.xml.ws.transport.tcp.util.TCPConstants;
import com.sun.xml.ws.transport.tcp.servicechannel.stubs.ServiceChannelWSImplService;
import javax.xml.namespace.QName;

/**
 * @author Alexey Stashok
 */
public class TCPTransportPipeFactory extends com.sun.xml.ws.transport.tcp.client.TCPTransportPipeFactory {
    private static final QName serviceChannelServiceName = new ServiceChannelWSImplService().getServiceName();
    
    @Override
    public Pipe doCreate(@NotNull final ClientPipeAssemblerContext context) {
        return doCreate(context, true);
    }
    
    public static Pipe doCreate(@NotNull final ClientPipeAssemblerContext context, final boolean checkSchema) {
        if (checkSchema && !TCPConstants.PROTOCOL_SCHEMA.equalsIgnoreCase(context.getAddress().getURI().getScheme())) {
            return null;
        }
        
        setClientSettingsIfRequired(context.getWsdlModel());
        if (context.getService().getServiceName().equals(serviceChannelServiceName)) {
            return new ServiceChannelTransportPipe(context);
        }
        
        return new TCPTransportPipe(context);
    }
    
    public static Tube doCreate(@NotNull final WsitClientTubeAssemblyContext context, final boolean checkSchema) {
        if (checkSchema && !TCPConstants.PROTOCOL_SCHEMA.equalsIgnoreCase(context.getAddress().getURI().getScheme())) {
            return null;
        }
        
        setClientSettingsIfRequired(context.getWsdlPort());
        if (context.getService().getServiceName().equals(serviceChannelServiceName)) {
            return PipeAdapter.adapt(new ServiceChannelTransportPipe(context));
        }
        
        return PipeAdapter.adapt(new TCPTransportPipe(context));
    }
    
    /**
     * Sets the client ConnectionManagement settings, which are passed via cliend
     * side policies for ServiceChannelWS
     */
    private static void setClientSettingsIfRequired(WSDLPort port) {
        PolicyConnectionManagementSettingsHolder instance = 
                PolicyConnectionManagementSettingsHolder.getInstance();
        
        if (instance.clientSettings == null) {
            synchronized(instance) {
                if (instance.clientSettings == null) {
                    instance.clientSettings = 
                            PolicyConnectionManagementSettingsHolder.createSettingsInstance(port);
                }
            }
        }
    }
}