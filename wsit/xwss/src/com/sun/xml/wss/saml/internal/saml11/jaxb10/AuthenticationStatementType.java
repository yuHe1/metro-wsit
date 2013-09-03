//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-b16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2005.09.05 at 03:09:41 PM IST 
//


package com.sun.xml.wss.saml.internal.saml11.jaxb10;


/**
 * Java content class for AuthenticationStatementType complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/space/combination/jwsdp1.6_tc/jaxb/bin/oasis-sstc-saml-schema-assertion-1.1.xsd line 123)
 * <p>
 * <pre>
 * &lt;complexType name="AuthenticationStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:1.0:assertion}SubjectStatementAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}SubjectLocality" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}AuthorityBinding" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="AuthenticationInstant" use="required" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="AuthenticationMethod" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface AuthenticationStatementType
    extends com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectStatementAbstractType
{


    /**
     * Gets the value of the AuthorityBinding property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the AuthorityBinding property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthorityBinding().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.AuthorityBinding}
     * {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.AuthorityBindingType}
     * 
     */
    java.util.List getAuthorityBinding();

    /**
     * Gets the value of the authenticationInstant property.
     * 
     * @return
     *     possible object is
     *     {@link java.util.Calendar}
     */
    java.util.Calendar getAuthenticationInstant();

    /**
     * Sets the value of the authenticationInstant property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.util.Calendar}
     */
    void setAuthenticationInstant(java.util.Calendar value);

    /**
     * Gets the value of the subjectLocality property.
     * 
     * @return
     *     possible object is
     *     {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocalityType}
     *     {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocality}
     */
    com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocalityType getSubjectLocality();

    /**
     * Sets the value of the subjectLocality property.
     * 
     * @param value
     *     allowed object is
     *     {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocalityType}
     *     {@link com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocality}
     */
    void setSubjectLocality(com.sun.xml.wss.saml.internal.saml11.jaxb10.SubjectLocalityType value);

    /**
     * Gets the value of the authenticationMethod property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getAuthenticationMethod();

    /**
     * Sets the value of the authenticationMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setAuthenticationMethod(java.lang.String value);

}