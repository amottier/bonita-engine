/*
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 */
package org.bonitasoft.engine.page;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SExecutionException;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SHumanTaskInstance;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

/**
 * author Emmanuel Duchastenier, Anthony Birembaut
 */
public class IsTaskAvailableForUserRule implements AuthorizationRule {

	ActivityInstanceService activityInstanceService;
	
	SessionService sessionService;
	
	SessionAccessor sessionAccessor;
	
    public IsTaskAvailableForUserRule(
			ActivityInstanceService activityInstanceService,
			SessionService sessionService, SessionAccessor sessionAccessor) {
		super();
		this.activityInstanceService = activityInstanceService;
		this.sessionService = sessionService;
		this.sessionAccessor = sessionAccessor;
	}

	@Override
    public boolean isAllowed(String key, Map<String, Serializable> context) throws SExecutionException {
        @SuppressWarnings("unchecked")
        final Map<String, String[]> queryParameters = (Map<String, String[]>) context.get(URLAdapterConstants.QUERY_PARAMETERS);
        String[] idParamValue = new String[0];
        String[] userParamValue = new String[0];
        if(queryParameters != null){
            idParamValue = queryParameters.get(URLAdapterConstants.ID_QUERY_PARAM);
            userParamValue = queryParameters.get(URLAdapterConstants.USER_QUERY_PARAM);
        }
        long taskInstanceId;
        if (idParamValue == null || idParamValue.length == 0) {
            throw new IllegalArgumentException("The parameter \"id\" is missing from the original URL");
        } else {
        	taskInstanceId = Long.parseLong(idParamValue[0]);
        	try {
	        	long userId;
	        	if (userParamValue == null || userParamValue.length == 0) {
	        		userId = sessionService.getSession(sessionAccessor.getSessionId()).getUserId();
	        	} else {
	        		userId = Long.parseLong(userParamValue[0]);
	        	}
	            final SHumanTaskInstance humanTaskInstance = activityInstanceService.getHumanTaskInstance(taskInstanceId);
	            long assigneeId = humanTaskInstance.getAssigneeId();
	            if (assigneeId > 0) {
	                return userId == assigneeId;
	            } else {
	                return activityInstanceService.isTaskPendingForUser(taskInstanceId, userId);
	            }
            } catch (final SBonitaException e) {
                throw new SExecutionException("Unable to figure out if the task " + taskInstanceId + " is available for the user.", e);
            }
        }
    }

    @Override
    public String getId() {
        return AuthorizationRuleConstants.IS_TASK_AVAILABLE_FOR_USER;
    }
}
