package com.example.environment.setup;

import com.example.environment.setup.v1_0_0.InitialEnvironmentSetup;
import com.example.environment.setup.v1_1_0.UserGroupEnvironmentSetup;
import com.example.environment.setup.v1_2_0.UserGroupRoleEnvironmentSetup;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.RoleService;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserGroupRoleService;
import com.liferay.portal.kernel.service.UserGroupService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * EnvironmentSetupUpgradeStepRegistrator: This is the upgrade step registration class. This is the only really OSGi
 * component, so any dependencies that the upgrade steps need should be @Reference injected here.
 *
 * @author dnebinger
 */
@Component(
		immediate = true,
		service = UpgradeStepRegistrator.class
)
public class EnvironmentSetupUpgradeStepRegistrator implements UpgradeStepRegistrator {

	private String bundleSymbolicName;

	@Activate
	private void activate(BundleContext bundleContext) {
		// rather than hard coding, we'll take the BSN we're given.
		bundleSymbolicName = bundleContext.getBundle().getSymbolicName();
	}

	/**
	 * register: This is the method that we must implement and is used to register every upgrade step into the given registry.
	 * @param registry The registry to add upgrade steps to.
	 */
	@Override
	public void register(Registry registry) {

		// use the default company for loading.
		long companyId = getDefaultCompanyId();

		// we should derive a user id from the company.
		long userId = getCompanyAdminUserId(companyId);

		// for version 1.0.0, we need an InitialEnvironmentSetup:
		InitialEnvironmentSetup initialEnvironmentSetup = new InitialEnvironmentSetup(companyId, userId, getPermissionCheckerFactory(), getUserLocalService(), getGroupLocalService(), getRoleService());

		// NOTE: You don't have to wrap everything into one super InitialEnvironmentSetup class; this register() call accepts multiple trailing upgrade process steps as the last argument.
		// so you can really organize your code into separate specific classes that do certain things in simple yet straight-forward manners.
		registry.register(bundleSymbolicName, "0.0.0", "1.0.0", initialEnvironmentSetup);

		// for version 1.1.0, we need a UserGroupEnvironmentSetup:
		UserGroupEnvironmentSetup userGroupEnvironmentSetup = new UserGroupEnvironmentSetup(companyId, userId, getPermissionCheckerFactory(), getUserLocalService(), getGroupLocalService(), getUserGroupService());
		registry.register(bundleSymbolicName, "1.0.0", "1.1.0", userGroupEnvironmentSetup);

		// for version 1.2.0, we need a UserGroupRoleEnvironmentSetup:
		UserGroupRoleEnvironmentSetup userGroupRoleEnvironmentSetup = new UserGroupRoleEnvironmentSetup(companyId, userId, getPermissionCheckerFactory(), getUserLocalService(), getGroupLocalService(), getRoleService(), getUserGroupService(), getUserGroupRoleService());
		registry.register(bundleSymbolicName, "1.1.0", "1.2.0", userGroupRoleEnvironmentSetup);
	}

	/**
	 * getDefaultCompanyId: Return the default company id.
	 * @return long The default company id.
	 */
	protected long getDefaultCompanyId() {
		long companyId = _portal.getDefaultCompanyId();

		return companyId;
	}

	/**
	 * getDefaultCompanyAdminUserId: Return an admin user id for the default company id.
	 * @return long The admin user id.
	 */
	protected long getDefaultCompanyAdminUserId() {
		return getCompanyAdminUserId(getDefaultCompanyId());
	}

	/**
	 * getCompanyAdminUserId: Return an admin user id for the given company id.
	 * @param companyId
	 * @return long The admin user id.
	 */
	protected long getCompanyAdminUserId(final long companyId) {
		Company company = _companyLocalService.fetchCompany(companyId);

		return getCompanyAdminUserId(company);
	}

	/**
	 * getCompanyAdminUserId: Finds a user id who is an admin for the given company.
	 * @param company
	 * @return long The admin user id.
	 * @throws PortalException
	 */
	protected long getCompanyAdminUserId(Company company) {
		if (Validator.isNull(company)) {
			// no company, nothing to find.
			return -1;
		}

		Role role = null;
		try {
			role = _roleLocalService.getRole(company.getCompanyId(), RoleConstants.ADMINISTRATOR);
		} catch (PortalException e) {
			_log.error("Error fetching admin role for company: " + e.getMessage(), e);

			return -1;
		}

		long[] userIds = _userLocalService.getRoleUserIds(role.getRoleId());

		long activeUserId = getActiveUserIdFromArray(userIds);

		if (activeUserId != -1) {
			return activeUserId;
		}

		List<Group> groups = _groupLocalService.getRoleGroups(role.getRoleId());

		for (Group group : groups) {
			if (group.isOrganization()) {
				userIds = _organizationLocalService.getUserPrimaryKeys(group.getClassPK());

				activeUserId = getActiveUserIdFromArray(userIds);

				if (activeUserId != -1) {
					return activeUserId;
				}
			} else if (group.isRegularSite()) {
				userIds = _groupLocalService.getUserPrimaryKeys(group.getGroupId());

				activeUserId = getActiveUserIdFromArray(userIds);

				if (activeUserId != -1) {
					return activeUserId;
				}
			} else if (group.isUserGroup()) {
				userIds = _userGroupLocalService.getUserPrimaryKeys(group.getClassPK());

				activeUserId = getActiveUserIdFromArray(userIds);

				if (activeUserId != -1) {
					return activeUserId;
				}
			}
		}

		_log.error("Unable to find an administrator user in company " + company.getCompanyId());

		return -1;
	}

	/**
	 * getActiveUserIdFromArray: Find an active admin user id from the given array.
	 * @param userIds
	 * @return long An active user id or <code>-1</code> if there isn't one.
	 */
	private long getActiveUserIdFromArray(long[] userIds) {
		if (!ArrayUtil.isEmpty(userIds)) {
			for (long id : userIds) {
				if (isActive(id)) {
					return id;
				}
			}
		}

		return -1;
	}

	/**
	 * isActive: Utility method to determine if the found user would be active
	 * and not the default user.
	 * @param userId
	 * @return boolean <code>true</code> if the user is active and not default.
	 */
	protected boolean isActive(final long userId) {
		User user = _userLocalService.fetchUser(userId);

		if (Validator.isNull(user)) {
			return false;
		}

		if (!user.isActive()) return false;

		if (Validator.isNull(user.getFirstName()) || Validator.isNull(user.getLastName())) {
			return false;
		}

		// we also want to skip the default user
		return ! user.isDefaultUser();
	}

	@Reference(unbind = "-")
	protected void setPortal(final Portal portal) {
		_portal = portal;
	}

	@Reference(unbind = "-")
	protected void setCompanyLocalService(final CompanyLocalService companyLocalService) {
		_companyLocalService = companyLocalService;
	}

	@Reference(unbind = "-")
	protected void setUserLocalService(final UserLocalService userLocalService) {
		_userLocalService = userLocalService;
	}

	@Reference(unbind = "-")
	protected void setGroupLocalService(final GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	@Reference(unbind = "-")
	protected void setRoleLocalService(final RoleLocalService roleLocalService) {
		_roleLocalService = roleLocalService;
	}

	@Reference(unbind = "-")
	protected void setUserGroupLocalService(final UserGroupLocalService userGroupLocalService) {
		_userGroupLocalService = userGroupLocalService;
	}

	@Reference(unbind = "-")
	protected void setOrganizationLocalService(final OrganizationLocalService organizationLocalService) {
		_organizationLocalService = organizationLocalService;
	}

	@Reference(unbind = "-")
	protected void setPermissionCheckerFactory(PermissionCheckerFactory permissionCheckerFactory) {
		this._permissionCheckerFactory = permissionCheckerFactory;
	}

	@Reference(unbind = "-")
	protected void setRoleService(RoleService roleService) {
		this._roleService = roleService;
	}

	@Reference(unbind = "-")
	protected void setUserGroupService(UserGroupService userGroupService) {
		this._userGroupService = userGroupService;
	}

	@Reference(unbind = "-")
	protected void setUserGroupRoleService(UserGroupRoleService userGroupRoleService) {
		this._userGroupRoleService = userGroupRoleService;
	}

	protected Portal getPortal() {
		return _portal;
	}

	protected CompanyLocalService getCompanyLocalService() {
		return _companyLocalService;
	}

	protected UserLocalService getUserLocalService() {
		return _userLocalService;
	}

	protected GroupLocalService getGroupLocalService() {
		return _groupLocalService;
	}

	protected RoleLocalService getRoleLocalService() {
		return _roleLocalService;
	}

	protected OrganizationLocalService getOrganizationLocalService() {
		return _organizationLocalService;
	}

	protected UserGroupLocalService getUserGroupLocalService() {
		return _userGroupLocalService;
	}

	protected PermissionCheckerFactory getPermissionCheckerFactory() {
		return _permissionCheckerFactory;
	}
	protected RoleService getRoleService() {
		return _roleService;
	}
	protected UserGroupService getUserGroupService() {
		return _userGroupService;
	}
	protected UserGroupRoleService getUserGroupRoleService() {
		return _userGroupRoleService;
	}

	private Portal _portal;
	private CompanyLocalService _companyLocalService;
	private UserLocalService _userLocalService;
	private GroupLocalService _groupLocalService;
	private RoleLocalService _roleLocalService;
	private OrganizationLocalService _organizationLocalService;
	private UserGroupLocalService _userGroupLocalService;
	private PermissionCheckerFactory _permissionCheckerFactory;
	private RoleService _roleService;
	private UserGroupService _userGroupService;
	private UserGroupRoleService _userGroupRoleService;

	private static final Log _log = LogFactoryUtil.getLog(EnvironmentSetupUpgradeStepRegistrator.class);
}
