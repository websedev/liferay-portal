<%--
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
--%>

<%@ include file="/init.jsp" %>

<%
String language = ParamUtil.getString(renderRequest, "language", "groovy");

if (SessionMessages.contains(renderRequest, "language")) {
	language = (String)SessionMessages.get(renderRequest, "language");
}

String output = ParamUtil.getString(renderRequest, "output", "text");

if (SessionMessages.contains(renderRequest, "output")) {
	output = (String)SessionMessages.get(renderRequest, "output");
}

String script = "// ### Groovy Sample ###\n\nnumber = com.liferay.portal.kernel.service.UserLocalServiceUtil.getUsersCount();\n\nout.println(number);";

if (SessionMessages.contains(renderRequest, "script")) {
	script = (String)SessionMessages.get(renderRequest, "script");
}

String scriptOutput = (String)SessionMessages.get(renderRequest, "scriptOutput");
%>

<liferay-ui:error exception="<%= ScriptingException.class %>">

	<%
	ScriptingException se = (ScriptingException)errorException;
	%>

	<pre><%= HtmlUtil.escape(se.getMessage()) %></pre>
</liferay-ui:error>

<aui:fieldset-group markupView="lexicon">
	<aui:fieldset>
		<aui:select name="language">

			<%
			for (String supportedLanguage : ScriptingUtil.getSupportedLanguages()) {
			%>

				<aui:option label="<%= TextFormatter.format(supportedLanguage, TextFormatter.J) %>" selected="<%= supportedLanguage.equals(language) %>" value="<%= supportedLanguage %>" />

			<%
			}
			%>

		</aui:select>

		<aui:select name="output">
			<aui:option label="text" selected='<%= output.equals("text") %>' value="text" />
			<aui:option label="html" selected='<%= output.equals("html") %>' value="html" />
		</aui:select>

		<aui:input cssClass="lfr-textarea-container" name="script" resizable="<%= true %>" type="textarea" value="<%= script %>" />
	</aui:fieldset>
</aui:fieldset-group>

<c:if test="<%= Validator.isNotNull(scriptOutput) %>">
	<b><liferay-ui:message key="output" /></b>

	<c:choose>
		<c:when test='<%= output.equals("html") %>'>
			<div><%= scriptOutput %></div>
		</c:when>
		<c:otherwise>
			<pre><%= HtmlUtil.escape(scriptOutput) %></pre>
		</c:otherwise>
	</c:choose>
</c:if>

<aui:button-row>
	<aui:button cssClass="save-server-button" data-cmd="runScript" value="execute" />
</aui:button-row>

<aui:script>
	var <portlet:namespace />selectLanguage = document.getElementById(
		'<portlet:namespace />language'
	);
	var <portlet:namespace />textArea = document.getElementById(
		'<portlet:namespace />script'
	);

	if (<portlet:namespace />selectLanguage && <portlet:namespace />textArea) {
		<portlet:namespace />selectLanguage.addEventListener('change', function() {
			<portlet:namespace />textArea.value = '';
		});
	}
</aui:script>