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
package com.foxit.uiextensions.security.certificate;

import android.content.Context;
import android.text.TextUtils;

import com.foxit.uiextensions.security.ICertificateSupport;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;


public class CertificateSupport implements ICertificateSupport{

    private Context mContext;

	public CertificateSupport(Context context) {
        mContext = context;
	}

	private static boolean matchUsage(boolean[] keyUsage, int usage) {
		if (usage == 0 || keyUsage == null)
			return true;
		for (int i = 0; i < Math.min(keyUsage.length, 32); i++) {
			if ((usage & (1 << i)) != 0 && !keyUsage[i])
				return false;
		}
		return true;
	}

	private void generateCertificateInfo(String keyStorePath, String keyStorePassword, CertificateInfo info) throws Exception {

			KeyStore keyStore = null;
			if (keyStorePath.toLowerCase().endsWith(".pfx")|| keyStorePath.toLowerCase().endsWith(".p12"))
					keyStore = KeyStore.getInstance("PKCS12");
			else
				keyStore = KeyStore.getInstance("JKS");
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(keyStorePath);
				keyStore.load(fis, keyStorePassword.toCharArray());
			} finally {
				if (fis != null)
					fis.close();
			}
			Enumeration aliases = keyStore.aliases();
			String keyAlias = null;
			if (aliases != null) {
				while (aliases.hasMoreElements()) {
					keyAlias = (String) aliases.nextElement();
					Certificate[] certs = keyStore.getCertificateChain(keyAlias);
					if (certs == null || certs.length == 0)
						continue;
					X509Certificate cert = (X509Certificate)certs[0];

					Date beforeDate = cert.getNotBefore();
					info.startDate = AppDmUtil.getLocalDateString(beforeDate);
					Date afterDate = cert.getNotAfter();
					info.expiringDate = AppDmUtil.getLocalDateString(afterDate);
					info.issuerUniqueID 	= "";
					boolean[] uniqueid 	= cert.getIssuerUniqueID();
					if (uniqueid != null) {
						for (int i = 0; i < uniqueid.length; i++) {
							info.issuerUniqueID += uniqueid[i];
						}
					}
					info.identity = "XXXX";
					String algName = cert.getSigAlgName();
                    BigInteger bigInteger = cert.getSerialNumber();
                    if (bigInteger.compareTo(BigInteger.ZERO) < 0) {
                        bigInteger = new BigInteger(1, bigInteger.toByteArray());
                    }
                    info.serialNumber  = bigInteger.toString(16).toUpperCase();
					info.issuer = AppUtil.getEntryName(cert.getIssuerDN().getName(), "CN=");
					info.emailAddress = AppUtil.getEntryName(cert.getIssuerDN().getName(),"E=");
					info.publisher = AppUtil.getEntryName(cert.getIssuerDN().getName(), "CN=");

					StringBuilder sb = new StringBuilder();
					sb.append(AppUtil.getEntryName(cert.getSubjectDN().getName(), "CN="));
					String temp = AppUtil.getEntryName(cert.getSubjectDN().getName(), "E=");
					if (!TextUtils.isEmpty(temp)) {
					    sb.append(" <");
					    sb.append(temp);
					    sb.append("> ");
                    }
					info.name = sb.toString();
					info.subject = AppUtil.getEntryName(cert.getSubjectDN().getName(), "CN=");
                    info.keyUsage = cert.getKeyUsage();
                    int usageCode = 0;
                    if (info.keyUsage != null) {
                        for (int i = 0; i < info.keyUsage.length; i++) {
							usageCode = (info.keyUsage[i] ? (usageCode | 1 << i) : usageCode);
                        }
                    }
					info.usageCode = usageCode;

					if (matchUsage(cert.getKeyUsage(), 1)) {
						try {
							cert.checkValidity();
						}  catch (CertificateExpiredException e) {
							info.expired = true;
						} catch (CertificateNotYetValidException e) {
							info.expired = false;
						}
						break;
					}
				}
			}
	}

	public CertificateInfo verifyPassword(String filePath, String password) {
		CertificateInfo info = null;
		boolean ret = false;
		InputStream stream = null;
		try {
			KeyStore var2 = KeyStore.getInstance("PKCS12", "BC");
			stream = new FileInputStream(filePath);
			var2.load(stream, password.toCharArray());
			Enumeration var3 = var2.aliases();
			ret = true;
			info = new CertificateInfo();

			generateCertificateInfo(filePath, password, info);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stream != null){
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return info;
	}

	public CertificateInfo getCertificateInfo(String filePath) {
		CertificateInfo info = null;
		try {
			InputStream is = new FileInputStream(filePath);
			try {
				CertificateFactory factory = CertificateFactory.getInstance("X.509");
				X509Certificate certificate = (X509Certificate) factory.generateCertificate(is);
				info = new CertificateInfo();
			} finally {
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return info;
	}
}
