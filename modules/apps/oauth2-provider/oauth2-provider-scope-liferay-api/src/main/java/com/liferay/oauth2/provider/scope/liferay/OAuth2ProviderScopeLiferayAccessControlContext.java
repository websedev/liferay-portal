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

package com.liferay.oauth2.provider.scope.liferay;

import com.liferay.portal.kernel.security.access.control.AccessControlUtil;
import com.liferay.portal.kernel.security.auth.AccessControlContext;
import com.liferay.portal.kernel.security.auth.verifier.AuthVerifierResult;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Carlos Sierra Andrés
 */
public class OAuth2ProviderScopeLiferayAccessControlContext {

	public static boolean isOAuth2AuthVerified() {
		AccessControlContext accessControlContext =
			AccessControlUtil.getAccessControlContext();

		if (accessControlContext == null) {
			return false;
		}

		AuthVerifierResult authVerifierResult =
			accessControlContext.getAuthVerifierResult();

		if (authVerifierResult == null) {
			return false;
		}

		if (AuthVerifierResult.State.SUCCESS.equals(
				authVerifierResult.getState())) {

			String authType = MapUtil.getString(
				authVerifierResult.getSettings(), "auth.type");

			if (Validator.isNotNull(authType) &&
				authType.equals(
					OAuth2ProviderScopeLiferayConstants.
						AUTH_VERIFIER_OAUTH2_TYPE)) {

				return true;
			}
		}

		return false;
	}

}