package com.example.environment.setup.v1_2_0;

import com.example.environment.setup.RemoteServiceDependentBaseUpgradeProcess;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupRoleService;
import com.liferay.portal.kernel.service.UserGroupService;
import com.liferay.portal.kernel.service.UserLocalService;

/**
 * class UserGroupRoleEnvironmentSetup: In 1.0.0 we added the role and in v 1.1.0 we added the user group, but we forgot to assign the role to the group.
 *
 * So this is the upgrade process for version 1.2.0 and is used to assign the role to the user group.
 *
 * @author dnebinger
 */
public class UserGroupRoleEnvironmentSetup extends RemoteServiceDependentBaseUpgradeProcess {
	/**
	 * UserGroupRoleEnvironmentSetup: Constructor which takes values we need to properly set up the environment.
	 *
	 * @param companyId
	 * @param adminUserId
	 * @param permissionCheckerFactory
	 * @param userLocalService
	 * @param groupLocalService
	 */
	public UserGroupRoleEnvironmentSetup(long companyId, long adminUserId, PermissionCheckerFactory permissionCheckerFactory, UserLocalService userLocalService, GroupLocalService groupLocalService, RoleService roleService, UserGroupService userGroupService, UserGroupRoleService userGroupRoleService) {
		super(companyId, adminUserId, permissionCheckerFactory, userLocalService, groupLocalService);

		this.userGroupService = userGroupService;
		this.roleService = roleService;
		this.userGroupRoleService = userGroupRoleService;
	}

	@Override
	protected void setupEnvironment(ServiceContext serviceContext) throws Exception {
		// fetch the role
		Role role = roleService.getRole(serviceContext.getCompanyId(), "UI Tester");

		// we could be defensive and make sure the role exists, but this upgrade process only runs if the 1.0.0 ran successfully.

		// fetch the user group
		UserGroup userGroup = userGroupService.getUserGroup("UI Testers");

		// we now have both of the elements, let's assign the role to the user group
		userGroupRoleService.addUserGroupRoles(serviceContext.getUserId(), userGroup.getGroupId(), new long[] {role.getRoleId()});
	}

	private final UserGroupService userGroupService;
	private final RoleService roleService;
	private final UserGroupRoleService userGroupRoleService;
}
