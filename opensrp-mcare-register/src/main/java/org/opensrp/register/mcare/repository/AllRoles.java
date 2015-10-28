package org.opensrp.register.mcare.repository;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.GenerateView;
import org.motechproject.dao.MotechBaseRepository;
import org.opensrp.common.AllConstants;
import org.opensrp.register.mcare.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class AllRoles  extends MotechBaseRepository<Role>{
	@Autowired
	public AllRoles(
			@Qualifier(AllConstants.OPENSRP_DATABASE_CONNECTOR) CouchDbConnector db) {
		super(Role.class, db);
	}
	@GenerateView
	public Role findByUserName(String userName) {
		List<Role> roles = queryView("by_userName", userName);
		if (roles == null || roles.isEmpty()) {
			return null;
		}
		return roles.get(0);
	}
}
