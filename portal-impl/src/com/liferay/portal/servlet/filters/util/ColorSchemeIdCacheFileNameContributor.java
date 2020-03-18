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

package com.liferay.portal.servlet.filters.util;

import com.liferay.portal.model.ColorScheme;
import com.liferay.portal.service.ThemeLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * @author José Ángel Jiménez Campoy
 */
public class ColorSchemeIdCacheFileNameContributor
	implements CacheFileNameContributor {

	@Override
	public String getParameterName() {
		return "colorSchemeId";
	}

	@Override
	public String getParameterValue(HttpServletRequest request) {
		String themeId = request.getParameter("themeId");

		if (themeId != null) {
			ColorScheme colorScheme =
				ThemeLocalServiceUtil.fetchColorScheme(
					PortalUtil.getCompanyId(request), themeId,
					request.getParameter(getParameterName()));

			if (colorScheme != null) {
				return colorScheme.getColorSchemeId();
			}
		}

		return null;
	}

}