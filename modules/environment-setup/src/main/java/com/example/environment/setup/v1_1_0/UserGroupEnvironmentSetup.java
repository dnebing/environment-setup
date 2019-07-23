package com.example.environment.setup.v1_1_0;

import com.example.environment.setup.RemoteServiceDependentBaseUpgradeProcess;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupService;
import com.liferay.portal.kernel.service.UserLocalService;

/**
 * class UserGroupEnvironmentSetup: So we found out that we wanted to have some user groups, things we forgot to add in the initial environment setup class.
 *
 * So this is the upgrade process for version 1.1.0 and is used to create a new user group.
 *
 * @author dnebinger
 */
public class UserGroupEnvironmentSetup extends RemoteServiceDependentBaseUpgradeProcess {
	/**
	 * UserGroupEnvironmentSetup: Constructor which takes values we need to properly set up the environment.
	 *
	 * @param companyId
	 * @param adminUserId
	 * @param permissionCheckerFactory
	 * @param userLocalService
	 * @param groupLocalService
	 */
	public UserGroupEnvironmentSetup(long companyId, long adminUserId, PermissionCheckerFactory permissionCheckerFactory, UserLocalService userLocalService, GroupLocalService groupLocalService, UserGroupService userGroupService) {
		super(companyId, adminUserId, permissionCheckerFactory, userLocalService, groupLocalService);

		this.userGroupService = userGroupService;
	}

	@Override
	protected void setupEnvironment(ServiceContext serviceContext) throws Exception {
		UserGroup userGroup = userGroupService.addUserGroup("UI Testers", "User group that contains all UI Tester users.", serviceContext);

		// now that we have the group, we might want to create new users in this account or if users already exist we might add them to the user.
	}

	private final UserGroupService userGroupService;
}
