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

package com.liferay.frontend.image.editor.integration.document.library.internal.display.context.logic;

import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.util.DLURLHelper;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.unsync.UnsyncStringWriter;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.Image;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.servlet.taglib.ui.JavaScriptMenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.JavaScriptToolbarItem;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsFactoryUtil;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.template.TemplateManagerUtil;
import com.liferay.portal.kernel.template.URLTemplateResource;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.util.PropsValues;

import java.util.ResourceBundle;

import javax.portlet.ActionRequest;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Sergio González
 */
public class ImageEditorDLDisplayContextHelper {

	public ImageEditorDLDisplayContextHelper(
		FileVersion fileVersion, HttpServletRequest httpServletRequest,
		DLURLHelper dlURLHelper) {

		_fileVersion = fileVersion;
		_httpServletRequest = httpServletRequest;
		_dlURLHelper = dlURLHelper;

		_themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		try {
			FileEntry fileEntry = null;

			if (fileVersion != null) {
				fileEntry = fileVersion.getFileEntry();
			}

			_fileEntry = fileEntry;
		}
		catch (PortalException pe) {
			throw new SystemException(
				"Unable to create image editor document library display " +
					"context helper for file version " + fileVersion,
				pe);
		}
	}

	public JavaScriptMenuItem getJavacriptEditWithImageEditorMenuItem(
			ResourceBundle resourceBundle)
		throws PortalException {

		JavaScriptMenuItem javaScriptMenuItem = new JavaScriptMenuItem();

		javaScriptMenuItem.setKey("#edit-with-image-editor");
		javaScriptMenuItem.setLabel(
			LanguageUtil.get(resourceBundle, "edit-with-image-editor"));
		javaScriptMenuItem.setOnClick(_getOnclickMethod());
		javaScriptMenuItem.setJavaScript(_getJavaScript());

		return javaScriptMenuItem;
	}

	public JavaScriptToolbarItem getJavacriptEditWithImageEditorToolbarItem(
			ResourceBundle resourceBundle)
		throws PortalException {

		JavaScriptToolbarItem javaScriptToolbarItem =
			new JavaScriptToolbarItem();

		javaScriptToolbarItem.setKey("#edit-with-image-editor");
		javaScriptToolbarItem.setLabel(
			LanguageUtil.get(resourceBundle, "edit-with-image-editor"));
		javaScriptToolbarItem.setOnClick(_getOnclickMethod());
		javaScriptToolbarItem.setJavaScript(_getJavaScript());

		return javaScriptToolbarItem;
	}

	public boolean isShowActions() throws PortalException {
		PortletDisplay portletDisplay = _themeDisplay.getPortletDisplay();

		String portletName = portletDisplay.getPortletName();

		if (portletName.equals(DLPortletKeys.DOCUMENT_LIBRARY_ADMIN)) {
			return true;
		}

		Settings settings = SettingsFactoryUtil.getSettings(
			new PortletInstanceSettingsLocator(
				_themeDisplay.getLayout(), portletDisplay.getId()));

		TypedSettings typedSettings = new TypedSettings(settings);

		return typedSettings.getBooleanValue("showActions");
	}

	public boolean isShowImageEditorAction() throws PortalException {
		if (_showImageEditorAction != null) {
			return _showImageEditorAction;
		}

		if (!isShowActions()) {
			_showImageEditorAction = false;
		}
		else if (!_fileEntry.containsPermission(
					_themeDisplay.getPermissionChecker(), ActionKeys.UPDATE) ||
				 (_fileEntry.isCheckedOut() && !_fileEntry.hasLock())) {

			_showImageEditorAction = false;
		}
		else if (!ArrayUtil.contains(
					PropsValues.DL_FILE_ENTRY_PREVIEW_IMAGE_MIME_TYPES,
					_fileEntry.getMimeType())) {

			_showImageEditorAction = false;
		}
		else {
			_showImageEditorAction = true;
		}

		return _showImageEditorAction;
	}

	private String _getJavaScript() throws PortalException {
		String javaScript =
			"/com/liferay/frontend/image/editor/integration/document/library" +
				"/internal/display/context/dependencies" +
					"/edit_with_image_editor_js.ftl";

		Class<?> clazz = getClass();

		URLTemplateResource urlTemplateResource = new URLTemplateResource(
			javaScript, clazz.getResource(javaScript));

		Template template = TemplateManagerUtil.getTemplate(
			TemplateConstants.LANG_TYPE_FTL, urlTemplateResource, false);

		template.put(
			"editLanguageKey", LanguageUtil.get(_httpServletRequest, "edit"));

		LiferayPortletResponse liferayPortletResponse =
			_getLiferayPortletResponse();

		template.put("namespace", liferayPortletResponse.getNamespace());

		UnsyncStringWriter unsyncStringWriter = new UnsyncStringWriter();

		template.processTemplate(unsyncStringWriter);

		return unsyncStringWriter.toString();
	}

	private LiferayPortletResponse _getLiferayPortletResponse() {
		PortletResponse portletResponse =
			(PortletResponse)_httpServletRequest.getAttribute(
				JavaConstants.JAVAX_PORTLET_RESPONSE);

		return PortalUtil.getLiferayPortletResponse(portletResponse);
	}

	private String _getOnclickMethod() {
		String imageEditorPortletId = PortletProviderUtil.getPortletId(
			Image.class.getName(), PortletProvider.Action.EDIT);

		PortletURL imageEditorURL = PortletURLFactoryUtil.create(
			_httpServletRequest, imageEditorPortletId,
			PortletRequest.RENDER_PHASE);

		imageEditorURL.setParameter(
			"mvcRenderCommandName", "/image_editor/view");

		try {
			imageEditorURL.setWindowState(LiferayWindowState.POP_UP);
		}
		catch (Exception e) {
			throw new SystemException("Unable to set window state", e);
		}

		LiferayPortletResponse liferayPortletResponse =
			_getLiferayPortletResponse();

		PortletURL editURL = liferayPortletResponse.createActionURL();

		editURL.setParameter(
			ActionRequest.ACTION_NAME,
			"/document_library/edit_file_entry_with_image_editor");

		editURL.setParameter(
			"fileEntryId", String.valueOf(_fileEntry.getFileEntryId()));

		String fileEntryPreviewURL = _dlURLHelper.getPreviewURL(
			_fileEntry, _fileVersion, _themeDisplay, StringPool.BLANK);

		StringBundler sb = new StringBundler(12);

		sb.append(liferayPortletResponse.getNamespace());
		sb.append("editWithImageEditor('");
		sb.append(HtmlUtil.escapeJS(imageEditorURL.toString()));
		sb.append("', '");
		sb.append(HtmlUtil.escapeJS(editURL.toString()));
		sb.append("', '");
		sb.append(HtmlUtil.escapeJS(_fileEntry.getFileName()));
		sb.append("', '");
		sb.append(HtmlUtil.escapeJS(fileEntryPreviewURL));
		sb.append("', '");
		sb.append(HtmlUtil.escapeJS(_fileEntry.getMimeType()));
		sb.append("');");

		return sb.toString();
	}

	private final DLURLHelper _dlURLHelper;
	private final FileEntry _fileEntry;
	private final FileVersion _fileVersion;
	private final HttpServletRequest _httpServletRequest;
	private Boolean _showImageEditorAction;
	private final ThemeDisplay _themeDisplay;

}