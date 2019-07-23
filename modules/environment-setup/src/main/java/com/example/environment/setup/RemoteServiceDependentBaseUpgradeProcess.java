package com.example.environment.setup;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeStep;

import java.util.Date;

/**
 * class RemoteServiceDependentBaseUpgradeProcess: When using remote services (instead of the local services), the remote services will use
 * the permission checker to see if the user has access.
 *
 * This base class will set up to use an administrator of the system and will also set up a PermissionChecker for the admin user.
 *
 * This will allow subclasses to invoke either local or remote services as necessary.
 *
 * @author dnebinger
 */
public abstract class RemoteServiceDependentBaseUpgradeProcess extends UpgradeProcess implements UpgradeStep {

	/**
	 * setupEnvironment: At this point everything is ready to begin setting up the environment... Do the actual work in this overriding method.
	 * @param serviceContext The constructed service context you can use to pass to service calls.
	 * @throws Exception
	 */
	protected abstract void setupEnvironment(ServiceContext serviceContext) throws Exception;

	/**
	 * RemoteServiceDependentBaseUpgradeProcess: Constructor which takes values we need to properly set up the environment.
	 * @param companyId
	 * @param adminUserId
	 * @param permissionCheckerFactory
	 * @param userLocalService
	 * @param groupLocalService
	 */
	public RemoteServiceDependentBaseUpgradeProcess(long companyId, long adminUserId, PermissionCheckerFactory permissionCheckerFactory, UserLocalService userLocalService, GroupLocalService groupLocalService) {
		super();

		this.companyId = companyId;
		this.adminUserId = adminUserId;
		this.permissionCheckerFactory = permissionCheckerFactory;
		this.userLocalService = userLocalService;
		this.groupLocalService = groupLocalService;
	}

	/**
	 * doUpgrade: This is the method the Liferay interface wants us to create. We'll just prep for the upgrade and defer to the setupEnvironment() method to do the work.
	 * @throws Exception
	 */
	@Override
	protected void doUpgrade() throws Exception {
		// before we start the upgrade, we should pretend to be an administrator...
		// update the principal thread local with the admin user id.
		long currentId = PrincipalThreadLocal.getUserId();
		boolean changed = false;
		PermissionChecker currentChecker = null;
		PermissionChecker adminChecker = null;

		if ((currentId == 0) && (getAdminUserId() >= 0)) {
			PrincipalThreadLocal.setName(getAdminUserId());
			changed = true;

			// if we have changed, we should also update the permission checker...
			User admin = userLocalService.fetchUser(getAdminUserId());

			adminChecker = permissionCheckerFactory.create(admin);
			currentChecker = PermissionThreadLocal.getPermissionChecker();

			// update the permission checker
			PermissionThreadLocal.setPermissionChecker(adminChecker);
		}

		try {
			// create a service context...
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(getCompanyId());
			Date current = new Date();
			serviceContext.setCreateDate(current);
			serviceContext.setModifiedDate(current);
			serviceContext.setUserId(getAdminUserId());

			// will not be setting the service context scope group id, that can be left to the subclasses to handle if necessary.

			// invoke the abstract method now that everything is set.
		} finally {
			if (changed) {
				// restore the permission checker
				PermissionThreadLocal.setPermissionChecker(currentChecker);

				// this is here in case we wanted to support changing it back, this is often not necessary.
				// PrincipalThreadLocal.setName(currentId);
			}
		}

	}

	protected long getSiteId(final String siteName) {
		long siteId = -1;

		Group group = groupLocalService.fetchGroup(getCompanyId(), siteName);

		if (group == null) {
			// well this is a weird error, use the company group then
			group = groupLocalService.fetchCompanyGroup(getCompanyId());
		}

		if (! group.isSite()) {
			// this too is a weird error, the group is not a site
			return -1;
		}

		return group.getGroupId();
	}

	public long getAdminUserId() {
		return adminUserId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public PermissionCheckerFactory getPermissionCheckerFactory() {
		return permissionCheckerFactory;
	}

	public UserLocalService getUserLocalService() {
		return userLocalService;
	}

	public GroupLocalService getGroupLocalService() {
		return groupLocalService;
	}

	private final long companyId;
	private final long adminUserId;
	private final PermissionCheckerFactory permissionCheckerFactory;
	private final UserLocalService userLocalService;
	private final GroupLocalService groupLocalService;
}
