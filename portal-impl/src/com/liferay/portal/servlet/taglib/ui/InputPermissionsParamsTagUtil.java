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

package com.liferay.portal.servlet.taglib.ui;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.URLCodec;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.List;

import javax.portlet.RenderResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

/**
 * @author     Brian Chan
 * @author     Jorge Ferrer
 * @deprecated As of 6.2.0, replaced by {@link
 *             com.liferay.taglib.ui.InputPermissionsParamsTag}
 */
@Deprecated
public class InputPermissionsParamsTagUtil {

	public static void doEndTag(String modelName, PageContext pageContext)
		throws JspException {

		try {
			HttpServletRequest request =
				(HttpServletRequest)pageContext.getRequest();

			RenderResponse renderResponse =
				(RenderResponse)request.getAttribute(
					JavaConstants.JAVAX_PORTLET_RESPONSE);

			ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
				WebKeys.THEME_DISPLAY);

			Layout layout = themeDisplay.getLayout();

			Group layoutGroup = layout.getGroup();

			Group group = themeDisplay.getScopeGroup();

			List<String> supportedActions =
				ResourceActionsUtil.getModelResourceActions(modelName);
			List<String> groupDefaultActions =
				ResourceActionsUtil.getModelResourceGroupDefaultActions(
					modelName);
			List<String> guestDefaultActions =
				ResourceActionsUtil.getModelResourceGuestDefaultActions(
					modelName);
			List<String> guestUnsupportedActions =
				ResourceActionsUtil.getModelResourceGuestUnsupportedActions(
					modelName);

			StringBundler sb = new StringBundler();

			for (String action : supportedActions) {
				boolean groupChecked = groupDefaultActions.contains(action);

				boolean guestChecked = false;

				if (layoutGroup.isControlPanel()) {
					if (!group.hasPrivateLayouts() &&
						guestDefaultActions.contains(action)) {

						guestChecked = true;
					}
				}
				else if (layout.isPublicLayout() &&
						 guestDefaultActions.contains(action)) {

					guestChecked = true;
				}

				boolean guestDisabled = guestUnsupportedActions.contains(
					action);

				if (guestDisabled) {
					guestChecked = false;
				}

				if (group.isOrganization() || group.isRegularSite()) {
					if (groupChecked) {
						sb.append(StringPool.AMPERSAND);
						sb.append(renderResponse.getNamespace());
						sb.append("groupPermissions=");
						sb.append(URLCodec.encodeURL(action));
					}
				}

				if (guestChecked) {
					sb.append(StringPool.AMPERSAND);
					sb.append(renderResponse.getNamespace());
					sb.append("guestPermissions=");
					sb.append(URLCodec.encodeURL(action));
				}
			}

			String inputPermissionsViewRole = getDefaultViewRole(
				modelName, themeDisplay);

			sb.append(StringPool.AMPERSAND);
			sb.append(renderResponse.getNamespace());
			sb.append("inputPermissionsViewRole=");
			sb.append(URLCodec.encodeURL(inputPermissionsViewRole));

			JspWriter jspWriter = pageContext.getOut();

			jspWriter.print(sb.toString());
		}
		catch (Exception e) {
			throw new JspException(e);
		}
	}

	public static String getDefaultViewRole(
			String modelName, ThemeDisplay themeDisplay)
		throws PortalException {

		Layout layout = themeDisplay.getLayout();

		Group layoutGroup = layout.getGroup();

		List<String> guestDefaultActions =
			ResourceActionsUtil.getModelResourceGuestDefaultActions(modelName);

		if (layoutGroup.isControlPanel()) {
			Group group = themeDisplay.getScopeGroup();

			if (!group.hasPrivateLayouts() &&
				guestDefaultActions.contains(ActionKeys.VIEW)) {

				return RoleConstants.GUEST;
			}
		}
		else if (layout.isPublicLayout() &&
				 guestDefaultActions.contains(ActionKeys.VIEW)) {

			return RoleConstants.GUEST;
		}

		List<String> groupDefaultActions =
			ResourceActionsUtil.getModelResourceGroupDefaultActions(modelName);

		if (groupDefaultActions.contains(ActionKeys.VIEW)) {
			Group siteGroup = GroupLocalServiceUtil.getGroup(
				themeDisplay.getSiteGroupId());

			Role defaultGroupRole = RoleLocalServiceUtil.getDefaultGroupRole(
				siteGroup.getGroupId());

			return defaultGroupRole.getName();
		}

		return RoleConstants.OWNER;
	}

}