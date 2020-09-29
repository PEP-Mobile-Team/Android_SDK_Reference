/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.security;


/**
 * Interface of the certificate support.<br>
 */
public interface ICertificateSupport {

	/** Definition of certificate information */
	public static class CertificateInfo {
		/** The publisher information of the certificate */
		public String		publisher;
		/** The serial number of the certificate. */
		public String		serialNumber;
		/** The issuer unique ID of the certificate. */
		public String		issuerUniqueID;
		/** The start of the validity period of the certificate. */
		public String		startDate;
		/** The end of the validity period of the certificate. */
		public String		expiringDate;
		/** The email address information of the certificate. */
		public String		emailAddress;
		/** The identity information of the certificate. */
		public String		identity;
//		/** Whether the certificate is validated by the CA. */
//		public boolean 		validated;
		/** the certificate status validated by the CA. */
		public int 			statusCode;

		/** the issuer information of the certificate */
		public String		issuer;

		public String name;

		public String subject;

		public int usageCode;

		/* KeyUsageUtil ::= BIT STRING {
		 *      digitalSignature        (0),
		 *      nonRepudiation          (1),
		 *      keyEncipherment         (2),
		 *      dataEncipherment        (3),
		 *      keyAgreement            (4),
		 *      keyCertSign             (5),
		 *      cRLSign                 (6),
		 *      encipherOnly            (7),
		 *      decipherOnly            (8) }
		 */
		public boolean[] 	keyUsage;

		/** extern */
		public boolean		expired;
	}


}
