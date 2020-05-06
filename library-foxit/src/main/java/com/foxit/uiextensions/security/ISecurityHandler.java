/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
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

public interface ISecurityHandler {
	public String getName();
	public int			getSupportedTypes();
	
	public boolean		isOwner(int permissions);
	
	public boolean		canPrintHighQuality(int permissions);
	public boolean		canPrint(int permissions);
	public boolean		canCopy(int permissions);
	public boolean		canCopyForAssess(int permissions);
	public boolean		canAssemble(int permissions);
	public boolean		canFillForm(int permissions);
	public boolean		canAddAnnot(int permissions);
	public boolean		canModifyContents(int permissions);
	public boolean		canSigning(int permissions);
}
