/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.workflowdefinitions.action;

import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.util.PropsValues;

/**
 * @author Inácio Nery
 */
public class WorkflowDefinitionPermissionChecker {

	public static boolean canPublishWorkflowDefinition(
		PermissionChecker permissionChecker) {

		if (permissionChecker == null) {
			return true;
		}

		if (PropsValues.WORKFLOW_COMPANY_ADMINISTRATOR_CAN_PUBLISH &&
			permissionChecker.isCompanyAdmin()) {

			return true;
		}

		if (permissionChecker.isOmniadmin()) {
			return true;
		}

		return false;
	}

}