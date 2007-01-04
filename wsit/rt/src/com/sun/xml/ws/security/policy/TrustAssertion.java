/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.security.policy;

import java.util.Set;

/**
 * Represents WSTrust Assertion.
 * @author K.Venugopal@sun.com
 */
public interface TrustAssertion{
  
  
    /**
     * Properties (MUST_SUPPORT_CLIENT_CHALLENGE,MUST_SUPPORT_SERVER_CHALLENGE,MUST_SUPPORT_ISSUED_TOKENS )present in the policy.
     * @return 
     */
    public Set getRequiredProperties();
    /**
     * 
     * @return 1.0
     */
    public String getType();
}