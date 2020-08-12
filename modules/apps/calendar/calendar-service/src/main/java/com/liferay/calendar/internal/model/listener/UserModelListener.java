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

package com.liferay.calendar.internal.model.listener;

import com.liferay.calendar.model.CalendarResource;
import com.liferay.calendar.service.CalendarResourceLocalService;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Portal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Antonio Junior
 */
@Component(immediate = true, service = ModelListener.class)
public class UserModelListener extends BaseModelListener<User> {

	@Override
	public void onAfterUpdate(User user) throws ModelListenerException {
		try {
			long classNameId = _portal.getClassNameId(User.class);

			CalendarResource calendarResource =
				_calendarResourceLocalService.fetchCalendarResource(
					classNameId, user.getUserId());

			if (calendarResource == null) {
				return;
			}

			String name = calendarResource.getName(LocaleUtil.getSiteDefault());

			if (Objects.equals(name, user.getFullName())) {
				return;
			}

			if (user.isDefaultUser() && name.equals(GroupConstants.GUEST)) {
				return;
			}

			Map<Locale, String> nameMap = new HashMap<>();

			nameMap.put(LocaleUtil.getSiteDefault(), user.getFullName());

			calendarResource.setNameMap(
				LocalizationUtil.populateLocalizationMap(
					nameMap,
					LocaleUtil.toLanguageId(LocaleUtil.getSiteDefault()),
					user.getGroupId()));

			_calendarResourceLocalService.updateCalendarResource(
				calendarResource);
		}
		catch (Exception e) {
			throw new ModelListenerException(e);
		}
	}

	@Reference
	private CalendarResourceLocalService _calendarResourceLocalService;

	@Reference
	private Portal _portal;

}