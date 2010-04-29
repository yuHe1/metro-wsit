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

package com.sun.xml.ws.encoding.policy;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicyMapExtender;
import com.sun.xml.ws.policy.PolicyMapKey;
import com.sun.xml.ws.policy.PolicySubject;
import com.sun.xml.ws.policy.jaxws.spi.PolicyMapUpdateProvider;
import com.sun.xml.ws.policy.privateutil.PolicyLogger;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import java.util.ArrayList;
import javax.xml.ws.soap.MTOMFeature;

import static com.sun.xml.ws.encoding.policy.EncodingConstants.OPTIMIZED_MIME_SERIALIZATION_ASSERTION;
import java.util.logging.Level;
import javax.xml.namespace.QName;

/**
 * Generate an MTOM policy if MTOM was enabled.
 *
 * @author Jakub Podlesak (japod at sun.com)
 */
public class MtomMapUpdateProvider implements PolicyMapUpdateProvider{
    
    private static final PolicyLogger logger = PolicyLogger.getLogger(MtomMapUpdateProvider.class);
    
    static class MtomAssertion extends PolicyAssertion {
        
        private static final AssertionData mtomData;
        static {
            mtomData= AssertionData.createAssertionData(OPTIMIZED_MIME_SERIALIZATION_ASSERTION);
            //JAX-WS MTOMFeature does n't currently capture if MTOM is required/optional.
            //JAX-WS accepts both normal messages and XOP encoded messages. Using wsp:Optional=true represents that behavior.
            //Moreover, this allows interoperability with non-MTOM aware clients.
            //See https://wsit.dev.java.net/issues/show_bug.cgi?id=1062
            mtomData.setOptionalAttribute(true);
        }
        
        MtomAssertion() {
            super(mtomData, null, null);
        }
    }
    
    /**
     * Generates an MTOM policy if MTOM is enabled.
     *
     * <ol>
     * <li>If MTOM is enabled
     * <ol>
     * <li>If MTOM policy does not already exist, generate
     * <li>Otherwise do nothing
     * </ol>
     * <li>Otherwise, do nothing (that implies that we do not remove any MTOM policies if MTOM is disabled)
     * </ol>
     *
     */
    public void update(PolicyMapExtender policyMapMutator, PolicyMap policyMap, SEIModel model, WSBinding wsBinding) throws PolicyException {
        logger.entering(policyMapMutator, policyMap, model, wsBinding);
        
        if (policyMap != null) {
            final MTOMFeature mtomFeature = wsBinding.getFeature(MTOMFeature.class);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("mtomFeature = " + mtomFeature);
            }
            if ((mtomFeature != null) && mtomFeature.isEnabled()) {
                final PolicyMapKey endpointKey = PolicyMap.createWsdlEndpointScopeKey(model.getServiceQName(), model.getPortName());
                final Policy existingPolicy = policyMap.getEndpointEffectivePolicy(endpointKey);
                if ((existingPolicy == null) || ! existingPolicy.contains(OPTIMIZED_MIME_SERIALIZATION_ASSERTION)) {
                    final QName bindingName = model.getBoundPortTypeName();
                    final Policy mtomPolicy = createMtomPolicy(bindingName);
                    PolicySubject mtomPolicySubject = new PolicySubject(bindingName, mtomPolicy);
                    PolicyMapKey aKey = PolicyMap.createWsdlEndpointScopeKey(model.getServiceQName(), model.getPortName());
                    policyMapMutator.putEndpointSubject(aKey, mtomPolicySubject);
                    logger.fine("Added MTOM policy with ID \"" + mtomPolicy.getIdOrName() + "\" to binding element \"" + bindingName + "\"");
                } else {
                    logger.fine("MTOM policy exists already, doing nothing");
                }
            }
        } // endif policy map not null
        
        logger.exiting();
    }
    
    
    /**
     * Create a policy with an MTOM assertion.
     *
     * @param model The binding element name. Used to generate a (locally) unique ID for the policy.
     * @return The policy.
     */
    private Policy createMtomPolicy(final QName bindingName) {
        ArrayList<AssertionSet> assertionSets = new ArrayList<AssertionSet>(1);
        ArrayList<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>(1);
        assertions.add(new MtomAssertion());
        assertionSets.add(AssertionSet.createAssertionSet(assertions));
        return Policy.createPolicy(null, bindingName.getLocalPart() + "_MTOM_Policy", assertionSets);
    }
    
}