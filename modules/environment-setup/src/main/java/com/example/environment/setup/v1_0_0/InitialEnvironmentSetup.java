package com.example.environment.setup.v1_0_0;

import com.example.environment.setup.RemoteServiceDependentBaseUpgradeProcess;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * class InitialEnvironmentSetup: This is stuff we want to do initially, things we know about up front.
 *
 * @author dnebinger
 */
public class InitialEnvironmentSetup extends RemoteServiceDependentBaseUpgradeProcess {
	/**
	 * InitialEnvironmentSetup: Constructor which takes values we need to properly set up the environment.
	 *
	 * @param companyId
	 * @param adminUserId
	 * @param permissionCheckerFactory
	 * @param userLocalService
	 * @param groupLocalService
	 */
	public InitialEnvironmentSetup(long companyId, long adminUserId, PermissionCheckerFactory permissionCheckerFactory, UserLocalService userLocalService, GroupLocalService groupLocalService, RoleService roleService) {
		super(companyId, adminUserId, permissionCheckerFactory, userLocalService, groupLocalService);

		this.roleService = roleService;
	}

	@Override
	protected void setupEnvironment(ServiceContext serviceContext) throws Exception {

		// we're good to go, let's create a new role...
		Map<Locale, String> titleMap = new HashMap<>();
		Map<Locale, String> descMap = new HashMap<>();

		Role role = roleService.addRole(null, 0,"UI Tester", titleMap, descMap, RoleConstants.TYPE_REGULAR, null, serviceContext);
	}

	private final RoleService roleService;
}
