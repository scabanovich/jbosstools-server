/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.as.internal.management.eap61plus;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	public static String ModuleStateEvaluationFailed;
	public static String OperationOnAddressFailed;
	public static String FailedToBuildOperation;
	public static String OperationOutcomeToString;
	public static String OperationOnUnitNotExecuted;
	public static String OperationOnUnitFailed;
	public static String OperationOnUnitRolledBack;
	public static String OperationNotExecConfigRequiresRestart;
	static {
		NLS.initializeMessages("org.jboss.ide.eclipse.as.internal.management.wildfly9.Messages", //$NON-NLS-1$ 
				Messages.class);
	}

}
