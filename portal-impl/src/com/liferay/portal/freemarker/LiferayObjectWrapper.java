/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.freemarker;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.templateparser.TemplateNode;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.ClassLoaderUtil;
import com.liferay.portal.util.PropsValues;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mika Koivisto
 * @author Tibor Lipusz
 */
public class LiferayObjectWrapper extends DefaultObjectWrapper {

	public LiferayObjectWrapper() {
		super();

		String[] allowedClassNames = GetterUtil.getStringValues(
			PropsValues.FREEMARKER_ENGINE_ALLOWED_CLASSES);

		_allowedClassNames = new ArrayList<String>(allowedClassNames.length);

		for (String allowedClassName : allowedClassNames) {
			allowedClassName = StringUtil.trim(allowedClassName);

			if (Validator.isBlank(allowedClassName)) {
				continue;
			}

			_allowedClassNames.add(allowedClassName);
		}

		_allowAllClasses = _allowedClassNames.contains(StringPool.STAR);

		String[] restrictedClassNames = GetterUtil.getStringValues(
			PropsValues.FREEMARKER_ENGINE_RESTRICTED_CLASSES);

		String[] restrictedPackageNames = GetterUtil.getStringValues(
			PropsValues.FREEMARKER_ENGINE_RESTRICTED_PACKAGES);

		_restrictedClasses = new ArrayList<Class<?>>(
			restrictedClassNames.length);
		_restrictedPackageNames = new ArrayList<String>(
			restrictedPackageNames.length);

		for (String restrictedClassName : restrictedClassNames) {
			restrictedClassName = StringUtil.trim(restrictedClassName);

			if (Validator.isBlank(restrictedClassName)) {
				continue;
			}

			try {
				_restrictedClasses.add(
					Class.forName(
						restrictedClassName, true,
						ClassLoaderUtil.getContextClassLoader()));
			}
			catch (ClassNotFoundException cnfe) {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Unable to find restricted class " +
						restrictedClassName + ". Registering as a package.",
						cnfe);
				}

				_restrictedPackageNames.add(restrictedClassName);
			}
		}

		for (String restrictedPackageName : restrictedPackageNames) {
			restrictedPackageName = StringUtil.trim(restrictedPackageName);

			if (Validator.isBlank(restrictedPackageName)) {
				continue;
			}

			_restrictedPackageNames.add(restrictedPackageName);
		}
	}

	@Override
	public TemplateModel wrap(Object object) throws TemplateModelException {
		if (object == null) {
			return null;
		}

		if (object instanceof TemplateNode) {
			return new LiferayTemplateModel((TemplateNode)object, this);
		}

		if (!_allowAllClasses) {
			_checkClassIsRestricted(object.getClass());
		}

		return super.wrap(object);
	}

	private void _checkClassIsRestricted(Class<?> clazz)
		throws TemplateModelException {

		ClassRestrictionInformation classRestrictionInformation =
			_getClassRestrictionInformation(clazz);

		_classRestrictionInformations.put(
			clazz.getName(), classRestrictionInformation);

		if (classRestrictionInformation.isRestricted()) {
			throw new TemplateModelException(
				classRestrictionInformation.getDescription());
		}
	}

	private ClassRestrictionInformation _getClassRestrictionInformation(
		Class<?> clazz) {

		String className = clazz.getName();

		if (_classRestrictionInformations.containsKey(className)) {
			return _classRestrictionInformations.get(className);
		}

		if (_allowedClassNames.contains(className)) {
			return _nullInstance;
		}

		for (Class<?> restrictedClass : _restrictedClasses) {
			if (!restrictedClass.isAssignableFrom(clazz)) {
				continue;
			}

			return new ClassRestrictionInformation(
				"Denied resolving class " + className + " by " +
				restrictedClass.getName());
		}

		int index = className.lastIndexOf(StringPool.PERIOD);

		if (index == -1) {
			return _nullInstance;
		}

		String packageName = className.substring(0, index);

		packageName = packageName.concat(StringPool.PERIOD);

		for (String restrictedPackageName : _restrictedPackageNames) {
			if (!packageName.startsWith(restrictedPackageName)) {
				continue;
			}

			return new ClassRestrictionInformation(
					"Denied resolving class " + className + " by " +
					restrictedPackageName);
		}

		return _nullInstance;
	}

	private static Log _log = LogFactoryUtil.getLog(LiferayObjectWrapper.class);

	private static final ClassRestrictionInformation _nullInstance =
		new ClassRestrictionInformation(null);

	private final boolean _allowAllClasses;
	private final List<String> _allowedClassNames;
	private final Map<String, ClassRestrictionInformation>
		_classRestrictionInformations =
			new ConcurrentHashMap<String, ClassRestrictionInformation>();
	private final List<Class<?>> _restrictedClasses;
	private final List<String> _restrictedPackageNames;

	private static class ClassRestrictionInformation {

		public String getDescription() {
			return _description;
		}

		public boolean isRestricted() {
			if (_description == null) {
				return false;
			}

			return true;
		}

		private ClassRestrictionInformation(String description) {
			_description = description;
		}

		private final String _description;

	}

}