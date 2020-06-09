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

package com.liferay.portal.template.freemarker.internal;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalImpl;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mika Koivisto
 */
public class RestrictedLiferayObjectWrapper extends LiferayObjectWrapper {

	public RestrictedLiferayObjectWrapper(
		String[] allowedClassNames, String[] restrictedClassNames,
		String[] restrictedMethodNames) {

		if (allowedClassNames == null) {
			_allowedClassNames = Collections.emptyList();
		}
		else {
			_allowedClassNames = new ArrayList<>(allowedClassNames.length);

			for (String allowedClassName : allowedClassNames) {
				allowedClassName = StringUtil.trim(allowedClassName);

				if (Validator.isBlank(allowedClassName)) {
					continue;
				}

				_allowedClassNames.add(allowedClassName);
			}
		}

		if (restrictedMethodNames == null) {
			_restrictedMethodNames = Collections.emptyMap();
		}
		else {
			_restrictedMethodNames = new HashMap<>();

			for (String restrictedMethodName : restrictedMethodNames) {
				int index = restrictedMethodName.indexOf(CharPool.POUND);

				if (index < 0) {
					_log.error(
						StringBundler.concat(
							"\"", restrictedMethodName,
							"\" does not match format ",
							"\"className#methodName\""));

					continue;
				}

				String className = StringUtil.trim(
					restrictedMethodName.substring(0, index));
				String methodName = StringUtil.trim(
					restrictedMethodName.substring(index + 1));

				Set<String> methodNames =
					_restrictedMethodNames.computeIfAbsent(
						className, key -> new HashSet<>());

				methodNames.add(StringUtil.toLowerCase(methodName));
			}
		}

		_allowAllClasses = _allowedClassNames.contains(StringPool.STAR);

		if (restrictedClassNames == null) {
			_restrictedClasses = Collections.emptyList();
			_restrictedPackageNames = Collections.emptyList();
		}
		else {
			_restrictedClasses = new ArrayList<>(restrictedClassNames.length);
			_restrictedPackageNames = new ArrayList<>();

			AggregateClassLoader aggregateClassLoader =
				new AggregateClassLoader(
					LiferayObjectWrapper.class.getClassLoader());

			aggregateClassLoader.addClassLoader(
				PortalImpl.class.getClassLoader());

			Thread thread = Thread.currentThread();

			if (thread.getContextClassLoader() != null) {
				aggregateClassLoader.addClassLoader(
					thread.getContextClassLoader());
			}

			for (String restrictedClassName : restrictedClassNames) {
				restrictedClassName = StringUtil.trim(restrictedClassName);

				if (Validator.isBlank(restrictedClassName)) {
					continue;
				}

				try {
					_restrictedClasses.add(
						aggregateClassLoader.loadClass(restrictedClassName));
				}
				catch (ClassNotFoundException cnfe) {
					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								"Unable to find restricted class ",
								restrictedClassName,
								". Registering as a package."),
							cnfe);
					}

					if (restrictedClassName.endsWith(StringPool.STAR)) {
						restrictedClassName = restrictedClassName.substring(
							0, restrictedClassName.length() - 1);
					}

					_restrictedPackageNames.add(restrictedClassName);
				}
			}
		}
	}

	@Override
	public TemplateModel wrap(Object object) throws TemplateModelException {
		if (object == null) {
			return null;
		}

		if (object instanceof TemplateModel) {
			return (TemplateModel)object;
		}

		Class<?> clazz = object.getClass();

		String className = clazz.getName();

		if (!_allowAllClasses && _isRestricted(clazz)) {
			return _LIFERAY_FREEMARKER_STRING_MODEL_FACTORY.create(
				object, this);
		}

		if (_restrictedMethodNames.containsKey(className)) {
			LiferayFreeMarkerStringModel liferayFreeMarkerStringModel =
				(LiferayFreeMarkerStringModel)
					_LIFERAY_FREEMARKER_STRING_MODEL_FACTORY.create(
						object, this);

			liferayFreeMarkerStringModel.setRestrictedMethodNames(
				_restrictedMethodNames.get(className));

			return liferayFreeMarkerStringModel;
		}

		return super.wrap(object);
	}

	private boolean _isRestricted(Class<?> clazz) {
		return _restrictedClassMap.computeIfAbsent(
			clazz.getName(),
			className -> {
				if (_allowedClassNames.contains(className)) {
					return false;
				}

				for (Class<?> restrictedClass : _restrictedClasses) {
					if (!restrictedClass.isAssignableFrom(clazz)) {
						continue;
					}

					return true;
				}

				int index = className.lastIndexOf(StringPool.PERIOD);

				if (index == -1) {
					return false;
				}

				String packageName = className.substring(0, index);

				packageName = packageName.concat(StringPool.PERIOD);

				for (String restrictedPackageName : _restrictedPackageNames) {
					if (!packageName.startsWith(restrictedPackageName)) {
						continue;
					}

					return true;
				}

				return false;
			});
	}

	private static final ModelFactory _LIFERAY_FREEMARKER_STRING_MODEL_FACTORY =
		new ModelFactory() {

			@Override
			public TemplateModel create(
				Object object, ObjectWrapper objectWrapper) {

				return new LiferayFreeMarkerStringModel(
					object, (BeansWrapper)objectWrapper);
			}

		};

	private static final Log _log = LogFactoryUtil.getLog(
		RestrictedLiferayObjectWrapper.class);

	private final boolean _allowAllClasses;
	private final List<String> _allowedClassNames;
	private final List<Class<?>> _restrictedClasses;
	private final Map<String, Boolean> _restrictedClassMap =
		new ConcurrentHashMap<>();
	private final Map<String, Set<String>> _restrictedMethodNames;
	private final List<String> _restrictedPackageNames;

}