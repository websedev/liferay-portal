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

package com.liferay.portal.json.jabsorb.serializer;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.util.PropsUtil;

import java.io.Closeable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tomas Polesovsky
 */
public class LiferayJSONDeserializationWhitelist {

	public LiferayJSONDeserializationWhitelist() {
		for (String className :
				PropsUtil.getArray(
					PropsKeys.JSON_DESERIALIZATION_WHITELIST_CLASS_NAMES)) {

			_registeredClassNames.put(className, new AtomicInteger(1));
		}
	}

	public boolean isWhitelisted(String className) {
		if (_registeredClassNames.containsKey(className)) {
			return true;
		}

		if (_log.isWarnEnabled()) {
			StringBundler sb = new StringBundler(3);

			sb.append("Unable to deserialize ");
			sb.append(className);
			sb.append(" due to security restrictions");

			_log.warn(sb.toString());
		}

		return false;
	}

	public Closeable register(final String... classNames) {
		for (String className : classNames) {
			AtomicInteger counter = _registeredClassNames.get(className);

			if (counter == null) {
				synchronized (_registeredClassNames) {
					counter = _registeredClassNames.get(className);

					if (counter == null) {
						counter = new AtomicInteger(0);
						_registeredClassNames.put(className, counter);
					}
				}
			}

			counter.incrementAndGet();
		};

		return new Closeable() {

			@Override
			public void close() {
				if (!_closed.compareAndSet(false, true)) {
					return;
				}

				for (String className : classNames) {
					AtomicInteger counter = _registeredClassNames.get(
						className);

					if (counter.decrementAndGet() == 0) {
						synchronized (_registeredClassNames) {
							if (counter.get() == 0) {
								_registeredClassNames.remove(className);
							}
						}
					}
				}
			}

			private final AtomicBoolean _closed = new AtomicBoolean();

		};
	}

	private static Log _log = LogFactoryUtil.getLog(
		LiferayJSONDeserializationWhitelist.class);

	private final Map<String, AtomicInteger> _registeredClassNames =
		new ConcurrentHashMap<String, AtomicInteger>();

}