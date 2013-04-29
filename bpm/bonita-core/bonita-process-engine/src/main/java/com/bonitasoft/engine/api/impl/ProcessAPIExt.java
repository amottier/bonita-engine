/*******************************************************************************
 * Copyright (C) 2009, 2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.actor.ActorMappingExportException;
import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.api.impl.PageIndexCheckingUtil;
import org.bonitasoft.engine.api.impl.ProcessAPIImpl;
import org.bonitasoft.engine.api.impl.transaction.AddSDependency;
import org.bonitasoft.engine.api.impl.transaction.CreateManualUserTask;
import org.bonitasoft.engine.api.impl.transaction.DeleteProcess;
import org.bonitasoft.engine.api.impl.transaction.GetActivityInstance;
import org.bonitasoft.engine.api.impl.transaction.GetArchivedProcessInstanceList;
import org.bonitasoft.engine.api.impl.transaction.GetLastArchivedProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.GetProcessDefinition;
import org.bonitasoft.engine.api.impl.transaction.identity.GetSUser;
import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.model.ActivityInstance;
import org.bonitasoft.engine.bpm.model.ConnectorEvent;
import org.bonitasoft.engine.bpm.model.ConnectorInstance;
import org.bonitasoft.engine.bpm.model.ConnectorState;
import org.bonitasoft.engine.bpm.model.ConnectorStateReset;
import org.bonitasoft.engine.bpm.model.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.model.Index;
import org.bonitasoft.engine.bpm.model.ManualTaskInstance;
import org.bonitasoft.engine.bpm.model.ProcessDefinition;
import org.bonitasoft.engine.bpm.model.ProcessInstance;
import org.bonitasoft.engine.bpm.model.TaskPriority;
import org.bonitasoft.engine.bpm.model.archive.ArchivedProcessInstance;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.connector.ConnectorInstanceCriterion;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.ConnectorResult;
import org.bonitasoft.engine.core.connector.ConnectorService;
import org.bonitasoft.engine.core.connector.exception.SConnectorException;
import org.bonitasoft.engine.core.connector.exception.SConnectorInstanceModificationException;
import org.bonitasoft.engine.core.connector.exception.SConnectorInstanceReadException;
import org.bonitasoft.engine.core.connector.exception.SInvalidConnectorImplementationException;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.operation.Operation;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.exception.SDeletingEnabledProcessException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.model.SParameterDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityReadException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceReadException;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.SConnectorInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementsContainerType;
import org.bonitasoft.engine.core.process.instance.model.SHumanTaskInstance;
import org.bonitasoft.engine.core.process.instance.model.SManualTaskInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.STaskPriority;
import org.bonitasoft.engine.core.process.instance.model.archive.SAActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.builder.SAProcessInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.SConnectorInstanceBuilder;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.model.builder.DependencyBuilderAccessor;
import org.bonitasoft.engine.exception.ActivityCreationException;
import org.bonitasoft.engine.exception.ActivityExecutionErrorException;
import org.bonitasoft.engine.exception.ActivityExecutionFailedException;
import org.bonitasoft.engine.exception.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.exception.ActivityInterruptedException;
import org.bonitasoft.engine.exception.ActivityNotFoundException;
import org.bonitasoft.engine.exception.ArchivedActivityInstanceNotFoundException;
import org.bonitasoft.engine.exception.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.ClassLoaderException;
import org.bonitasoft.engine.exception.ConnectorException;
import org.bonitasoft.engine.exception.IllegalProcessStateException;
import org.bonitasoft.engine.exception.InvalidConnectorImplementationException;
import org.bonitasoft.engine.exception.InvalidEvaluationConnectorConditionException;
import org.bonitasoft.engine.exception.InvalidSessionException;
import org.bonitasoft.engine.exception.NotSerializableException;
import org.bonitasoft.engine.exception.ObjectDeletionException;
import org.bonitasoft.engine.exception.ObjectModificationException;
import org.bonitasoft.engine.exception.ObjectNotFoundException;
import org.bonitasoft.engine.exception.ObjectReadException;
import org.bonitasoft.engine.exception.PageOutOfRangeException;
import org.bonitasoft.engine.exception.ProcessDefinitionAlreadyExistsException;
import org.bonitasoft.engine.exception.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.ProcessDeletionException;
import org.bonitasoft.engine.exception.ProcessDeployException;
import org.bonitasoft.engine.exception.ProcessInstanceModificationException;
import org.bonitasoft.engine.exception.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.exception.ProcessInstanceReadException;
import org.bonitasoft.engine.execution.ContainerRegistry;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.execution.transaction.AddActivityInstanceTokenCount;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.parameter.OrderBy;
import org.bonitasoft.engine.parameter.ParameterService;
import org.bonitasoft.engine.parameter.SOutOfBoundException;
import org.bonitasoft.engine.parameter.SParameter;
import org.bonitasoft.engine.parameter.SParameterProcessNotFoundException;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.PlatformServiceAccessor;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.transaction.STransactionException;
import org.bonitasoft.engine.util.IOUtil;

import com.bonitasoft.engine.api.ParameterSorting;
import com.bonitasoft.engine.api.ProcessAPI;
import com.bonitasoft.engine.api.impl.transaction.UpdateProcessInstance;
import com.bonitasoft.engine.api.impl.transaction.connector.SetConnectorInstancesState;
import com.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import com.bonitasoft.engine.bpm.model.ParameterInstance;
import com.bonitasoft.engine.bpm.model.ProcessInstanceUpdateDescriptor;
import com.bonitasoft.engine.bpm.model.impl.ParameterImpl;
import com.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import com.bonitasoft.engine.core.process.instance.model.builder.SProcessInstanceUpdateBuilder;
import com.bonitasoft.engine.exception.InvalidParameterValueException;
import com.bonitasoft.engine.exception.ParameterNotFoundException;
import com.bonitasoft.engine.service.TenantServiceAccessor;
import com.bonitasoft.engine.service.impl.LicenseChecker;
import com.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import com.bonitasoft.engine.service.impl.TenantServiceSingleton;
import com.bonitasoft.manager.Features;

/**
 * @author Matthieu Chaffotte
 */
public class ProcessAPIExt extends ProcessAPIImpl implements ProcessAPI {

    private static TenantServiceAccessor getTenantAccessor() throws InvalidSessionException {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new InvalidSessionException(e);
        }
    }

    @Override
    public void deleteProcess(final long processDefinitionId) throws InvalidSessionException, ProcessDefinitionNotFoundException, ProcessDeletionException,
            IllegalProcessStateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            deleteProcessInstancesFromProcessDefinition(processDefinitionId, tenantAccessor);
            final SProcessDefinition serverProcessDefinition = getServerProcessDefinition(transactionExecutor, processDefinitionId, processDefinitionService);
            final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
            final DeleteProcess deleteProcess = new DeleteProcess(processDefinitionService, serverProcessDefinition, processInstanceService,
                    tenantAccessor.getArchiveService(), actorMappingService);
            transactionExecutor.execute(deleteProcess);
            final String processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantAccessor.getTenantId());
            final File file = new File(processesFolder);
            if (!file.exists()) {
                file.mkdir();
            }
            if (!serverProcessDefinition.getParameters().isEmpty()) {
                tenantAccessor.getParameterService().deleteAll(serverProcessDefinition.getId());
            }
            final File processeFolder = new File(file, String.valueOf(serverProcessDefinition.getId()));
            IOUtil.deleteDir(processeFolder);
        } catch (final SProcessDefinitionNotFoundException e) {
            log(tenantAccessor, e);
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SDeletingEnabledProcessException e) {
            log(tenantAccessor, e);
            throw new IllegalProcessStateException(e);
        } catch (final SProcessDefinitionReadException e) {
            log(tenantAccessor, e);
            throw new ProcessDeletionException(e);
        } catch (final BonitaHomeNotSetException e) {
            log(tenantAccessor, e);
            throw new BonitaRuntimeException(e);
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new ProcessDeletionException(e);
        } catch (final IOException e) {
            log(tenantAccessor, e);
            throw new ProcessDeletionException(e);
        }
    }

    @Override
    public void importParameters(final long pDefinitionId, final byte[] parametersXML) throws InvalidSessionException, InvalidParameterValueException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.CREATE_PARAMETER);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        SProcessDefinition sDefinition = null;
        if (pDefinitionId > 0) {
            final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
            final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(pDefinitionId, processDefinitionService);
            try {
                transactionExecutor.execute(getProcessDefinition);
            } catch (final SBonitaException e) {
                throw new InvalidParameterValueException(e);
            }
            sDefinition = getProcessDefinition.getResult();
        }

        final ParameterService parameterService = tenantAccessor.getParameterService();
        final Set<SParameterDefinition> parameters = sDefinition.getParameters();
        final Map<String, String> defaultParamterValues = new HashMap<String, String>();

        if (parametersXML != null) {
            final Properties property = new Properties();
            try {
                property.load(new ByteArrayInputStream(parametersXML));
            } catch (final IOException e1) {
                throw new InvalidParameterValueException(e1);
            }

            for (final Entry<Object, Object> entry : property.entrySet()) {
                defaultParamterValues.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        final Map<String, String> storedParameters = new HashMap<String, String>();
        for (final SParameterDefinition sParameterDefinition : parameters) {
            final String name = sParameterDefinition.getName();
            final String value = defaultParamterValues.get(name);
            if (value != null) {
                storedParameters.put(name, value);
            }
        }

        try {
            parameterService.addAll(sDefinition.getId(), storedParameters);
        } catch (final SParameterProcessNotFoundException e) {
            throw new InvalidParameterValueException(e);
        }
    }

    @Override
    protected void unzipBar(final BusinessArchive businessArchive, final SProcessDefinition sDefinition, final long tenantId) throws BonitaHomeNotSetException,
            IOException {
        final String processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
        final File file = new File(processesFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        final File processFolder = new File(file, String.valueOf(sDefinition.getId()));
        BusinessArchiveFactory.writeBusinessArchiveToFolder(businessArchive, processFolder);
    }

    private void log(final TenantServiceAccessor tenantAccessor, final Exception e) {
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
    }

    private SProcessDefinition getServerProcessDefinition(final TransactionExecutor transactionExecutor, final long processDefinitionUUID,
            final ProcessDefinitionService processDefinitionService) throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException {
        final TransactionContentWithResult<SProcessDefinition> transactionContentWithResult = new GetProcessDefinition(processDefinitionUUID,
                processDefinitionService);
        try {
            transactionExecutor.execute(transactionContentWithResult);
            return transactionContentWithResult.getResult();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw e;
        } catch (final SProcessDefinitionReadException e) {
            throw e;
        } catch (final SBonitaException e) {
            throw new SProcessDefinitionNotFoundException(e);
        }
    }

    @Override
    public int getNumberOfParameterInstances(final long processDefinitionUUID) throws InvalidSessionException, ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final SProcessDefinition sProcessDefinition = getServerProcessDefinition(transactionExecutor, processDefinitionUUID, processDefinitionService);
            return sProcessDefinition.getParameters().size();
        } catch (final SProcessDefinitionReadException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
    }

    @Override
    public ParameterInstance getParameterInstance(final long processDefinitionId, final String parameterName) throws InvalidSessionException,
            ProcessDefinitionNotFoundException, ParameterNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ParameterService parameterService = tenantAccessor.getParameterService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final SProcessDefinition sProcessDefinition = getServerProcessDefinition(transactionExecutor, processDefinitionId, processDefinitionService);
            final SParameter parameter = parameterService.get(processDefinitionId, parameterName);
            final String name = parameter.getName();
            final String value = parameter.getValue();
            final SParameterDefinition parameterDefinition = sProcessDefinition.getParameter(name);
            final String description = parameterDefinition.getDescription();
            final String type = parameterDefinition.getType();
            return new ParameterImpl(name, description, value, type);
        } catch (final SParameterProcessNotFoundException e) {
            throw new ParameterNotFoundException(processDefinitionId, parameterName);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
    }

    @Override
    public List<ParameterInstance> getParameterInstances(final long processDefinitionId, final int pageIndex, final int numberPerPage,
            final ParameterSorting sort) throws InvalidSessionException, ProcessDefinitionNotFoundException, PageOutOfRangeException {
        final int totalNumber = getNumberOfParameterInstances(processDefinitionId);
        PageIndexCheckingUtil.checkIfPageIsOutOfRange(totalNumber, pageIndex, numberPerPage);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ParameterService parameterService = tenantAccessor.getParameterService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            OrderBy order = null;
            switch (sort) {
                case NAME_DESC:
                    order = OrderBy.NAME_DESC;
                    break;
                default:
                    order = OrderBy.NAME_ASC;
                    break;
            }

            final SProcessDefinition sProcessDefinition = getServerProcessDefinition(transactionExecutor, processDefinitionId, processDefinitionService);
            if (sProcessDefinition.getParameters().isEmpty()) {
                return Collections.emptyList();
            }
            final List<SParameter> parameters = parameterService.get(processDefinitionId, pageIndex * numberPerPage, numberPerPage, order);
            final List<ParameterInstance> paramterInstances = new ArrayList<ParameterInstance>();
            for (int i = 0; i < parameters.size(); i++) {
                final SParameter parameter = parameters.get(i);
                final String name = parameter.getName();
                final String value = parameter.getValue();
                final SParameterDefinition parameterDefinition = sProcessDefinition.getParameter(name);
                final String description = parameterDefinition.getDescription();
                final String type = parameterDefinition.getType();
                paramterInstances.add(new ParameterImpl(name, description, value, type));
            }
            return paramterInstances;
        } catch (final SParameterProcessNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SOutOfBoundException e) {
            throw new PageOutOfRangeException(e);
        }
    }

    @Override
    public void updateParameterInstanceValue(final long processDefinitionId, final String parameterName, final String parameterValue)
            throws InvalidSessionException, ProcessDefinitionNotFoundException, ParameterNotFoundException, InvalidParameterValueException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ParameterService parameterService = tenantAccessor.getParameterService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final SProcessDefinition sProcessDefinition = getServerProcessDefinition(transactionExecutor, processDefinitionId, processDefinitionService);
            final SParameterDefinition parameter = sProcessDefinition.getParameter(parameterName);
            if (parameter == null) {
                throw new ParameterNotFoundException(processDefinitionId, parameterName);
            }
            parameterService.update(processDefinitionId, parameterName, parameterValue);
            tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        } catch (final SParameterProcessNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
    }

    @Override
    public ManualTaskInstance addManualUserTask(final long humanTaskId, final String taskName, final String displayName, final long assignTo,
            final String description, final Date dueDate, final TaskPriority priority) throws InvalidSessionException, ActivityInterruptedException,
            ActivityExecutionErrorException, ActivityCreationException, ActivityNotFoundException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.CREATE_MANUAL_TASK);
        TenantServiceAccessor tenantAccessor = null;
        final TaskPriority prio = priority != null ? priority : TaskPriority.NORMAL;
        try {
            final String userName = getUserNameFromSession();
            tenantAccessor = getTenantAccessor();
            final IdentityService identityService = tenantAccessor.getIdentityService();
            final TransactionExecutor transactionExecutor = getTenantAccessor().getTransactionExecutor();
            final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
            final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
            final GetSUser getSUser = new GetSUser(identityService, userName);
            transactionExecutor.execute(getSUser);
            final long userId = getSUser.getResult().getId();

            final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, humanTaskId);
            transactionExecutor.execute(getActivityInstance);
            final SActivityInstance activityInstance = getActivityInstance.getResult();
            if (!(activityInstance instanceof SHumanTaskInstance)) {
                throw new ActivityNotFoundException("The parent activity is not a Human task", humanTaskId);
            }
            if (((SHumanTaskInstance) activityInstance).getAssigneeId() != userId) {
                throw new ActivityCreationException("Unable to create a child task from this task, it's not assigned to you!");
            }
            final TransactionContentWithResult<SManualTaskInstance> createManualUserTask = new CreateManualUserTask(activityInstanceService, taskName, -1,
                    displayName, humanTaskId, assignTo, description, dueDate, STaskPriority.valueOf(prio.name()));
            transactionExecutor.execute(createManualUserTask);
            final long id = createManualUserTask.getResult().getId();
            executeActivity(id);// put it in ready
            final AddActivityInstanceTokenCount addActivityInstanceTokenCount = new AddActivityInstanceTokenCount(activityInstanceService, humanTaskId, 1);
            transactionExecutor.execute(addActivityInstanceTokenCount);
            return ModelConvertor.toManualTask(createManualUserTask.getResult(), flowNodeStateManager);
        } catch (final SActivityInstanceNotFoundException e) {
            log(tenantAccessor, e);
            throw new ActivityNotFoundException(e.getMessage(), humanTaskId);
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new ActivityCreationException(e.getMessage());
        } catch (final InvalidSessionException e) {
            throw e;
        } catch (final ActivityInterruptedException e) {
            throw e;
        } catch (final ActivityExecutionErrorException e) {
            throw e;
        } catch (final Exception e) {
            log(tenantAccessor, e);
            throw new ActivityExecutionErrorException(e.getMessage());
        }
    }

    @Override
    public void deleteManualUserTask(final long manualTaskId) throws InvalidSessionException, ObjectDeletionException, ObjectNotFoundException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.CREATE_MANUAL_TASK);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = getTenantAccessor().getTransactionExecutor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final SActivityInstance activityInstance = activityInstanceService.getActivityInstance(manualTaskId);
                if (activityInstance instanceof SManualTaskInstance) {// should check in the definition that it does not exists
                    processInstanceService.deleteFlowNodeInstance(activityInstance, null);
                } else {
                    throw new ObjectDeletionException("Can't delete a task that is not a manual task", ManualTaskInstance.class);
                }
            } catch (final SActivityInstanceNotFoundException e) {
                throw new ObjectNotFoundException("can't find activity with id " + manualTaskId, e, ManualTaskInstance.class);
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ObjectDeletionException(e, ManualTaskInstance.class);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ObjectDeletionException(e, ManualTaskInstance.class);
        }
    }

    private String getUserNameFromSession() throws InvalidSessionException {
        SessionAccessor sessionAccessor = null;
        try {
            sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long sessionId = sessionAccessor.getSessionId();
            final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
            return platformServiceAccessor.getSessionService().getSession(sessionId).getUserName();
        } catch (final Exception e) {
            throw new InvalidSessionException(e);
        }
    }

    @Override
    public List<ConnectorInstance> getConnectorInstancesOfActivity(final long activityInstanceId, final int pageNumber, final int numberPerPage,
            final ConnectorInstanceCriterion order) throws InvalidSessionException, ObjectReadException, PageOutOfRangeException {
        return getConnectorInstancesFor(activityInstanceId, pageNumber, numberPerPage, SConnectorInstance.FLOWNODE_TYPE, order);
    }

    private List<ConnectorInstance> getConnectorInstancesFor(final long instanceId, final int pageNumber, final int numberPerPage, final String flownodeType,
            final ConnectorInstanceCriterion order) throws InvalidSessionException, PageOutOfRangeException, ObjectReadException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ConnectorInstanceService connectorInstanceService = tenantAccessor.getConnectorInstanceService();
        final SConnectorInstanceBuilder connectorInstanceBuilder = tenantAccessor.getBPMInstanceBuilders().getSConnectorInstanceBuilder();
        OrderByType orderByType;
        String fieldName;
        switch (order) {
            case ACTIVATION_EVENT_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getActivationEventKey();
                break;
            case ACTIVATION_EVENT_DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getActivationEventKey();
                break;
            case CONNECTOR_ID_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getConnectorIdKey();
                break;
            case CONNECTOR_ID__DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getConnectorIdKey();
                break;
            case CONTAINER_ID_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getContainerIdKey();
                break;
            case CONTAINER_ID__DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getContainerIdKey();
                break;
            case DEFAULT:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getNameKey();
                break;
            case NAME_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getNameKey();
                break;
            case NAME_DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getNameKey();
                break;
            case STATE_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getStateKey();
                break;
            case STATE_DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getStateKey();
                break;
            case VERSION_ASC:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getVersionKey();
                break;
            case VERSION_DESC:
                orderByType = OrderByType.DESC;
                fieldName = connectorInstanceBuilder.getVersionKey();
                break;
            default:
                orderByType = OrderByType.ASC;
                fieldName = connectorInstanceBuilder.getNameKey();
                break;
        }
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            long numberOfConnectorInstances;
            try {
                numberOfConnectorInstances = connectorInstanceService.getNumberOfConnectorInstances(instanceId, flownodeType);
                PageIndexCheckingUtil.checkIfPageIsOutOfRange(numberOfConnectorInstances, pageNumber, numberPerPage);
                final List<SConnectorInstance> connectorInstances = connectorInstanceService.getConnectorInstances(instanceId, flownodeType, pageNumber
                        * numberPerPage, numberPerPage, fieldName, orderByType);
                return ModelConvertor.toConnectorInstances(connectorInstances);
            } catch (final SConnectorInstanceReadException e) {
                throw new ObjectReadException(e, ConnectorInstance.class);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ObjectReadException(e, ConnectorInstance.class);
        }
    }

    @Override
    public List<ConnectorInstance> getConnectorInstancesOfProcess(final long processInstanceId, final int pageNumber, final int numberPerPage,
            final ConnectorInstanceCriterion order) throws InvalidSessionException, ObjectReadException, PageOutOfRangeException {
        return getConnectorInstancesFor(processInstanceId, pageNumber, numberPerPage, SConnectorInstance.PROCESS_TYPE, order);
    }

    @Override
    public void setConnectorInstanceState(final long connectorInstanceId, final ConnectorStateReset state) throws InvalidSessionException, ConnectorException {
        final Map<Long, ConnectorStateReset> connectorsToReset = new HashMap<Long, ConnectorStateReset>(1);
        connectorsToReset.put(connectorInstanceId, state);
        setConnectorInstanceState(connectorsToReset);
    }

    @Override
    public void setConnectorInstanceState(final Map<Long, ConnectorStateReset> connectorsToReset) throws InvalidSessionException, ConnectorException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.SET_CONNECTOR_STATE);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ConnectorInstanceService connectorInstanceService = tenantAccessor.getConnectorInstanceService();
        final SetConnectorInstancesState txContent = new SetConnectorInstancesState(connectorsToReset, connectorInstanceService);
        try {
            transactionExecutor.execute(txContent);
        } catch (final SBonitaException e) {
            throw new ConnectorException("Error resetting connector instance state", e);
        }
    }

    /**
     * byte[] is a zip file exported from studio
     * clear: remove the old .impl file; put the new .impl file in the connector directory
     * reload the cache, connectorId and connectorVersion are used here.
     * Warning filesystem operation are not rolledback
     * 
     * @throws InvalidConnectorImplementationException
     */
    @Override
    public void setConnectorImplementation(final long processDefinitionId, final String connectorId, final String connectorVersion,
            final byte[] connectorImplementationArchive) throws InvalidSessionException, ConnectorException, InvalidConnectorImplementationException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.POST_DEPLOY_CONFIG);

        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final DependencyService dependencyService = tenantAccessor.getDependencyService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final DependencyBuilderAccessor dependencyBuilderAccessor = tenantAccessor.getDependencyBuilderAccessor();
        final long tenantId = tenantAccessor.getTenantId();
        try {
            boolean txOpened = transactionExecutor.openTransaction();
            try {
                final SProcessDefinition sProcessDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
                connectorService.setConnectorImplementation(sProcessDefinition, tenantId, connectorId, connectorVersion, connectorImplementationArchive);
                // reload dependencies
                dependencyService.deleteDependencies(processDefinitionId, "process");

                transactionExecutor.completeTransaction(txOpened);
                // reopen a new transaction: avoid primary key unique constraint violation
                txOpened = transactionExecutor.openTransaction();

                final File processFolder = new File(new File(BonitaHomeServer.getInstance().getProcessesFolder(tenantId)), String.valueOf(processDefinitionId));
                final File file = new File(processFolder, "classpath");
                if (file.exists() && file.isDirectory()) {
                    final File[] listFiles = file.listFiles();
                    for (final File jarFile : listFiles) {
                        final String name = jarFile.getName();
                        final byte[] jarContent = FileUtils.readFileToByteArray(jarFile);
                        final AddSDependency addSDependency = new AddSDependency(dependencyService, dependencyBuilderAccessor,
                                processDefinitionId + "_" + name, jarContent, processDefinitionId, "process");
                        addSDependency.execute();
                    }
                }

            } catch (final SInvalidConnectorImplementationException e) {
                throw new InvalidConnectorImplementationException(e);
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } catch (final IOException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } catch (final BonitaHomeNotSetException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void replayActivity(final long activityInstanceId) throws InvalidSessionException, ObjectNotFoundException, ObjectReadException,
            ObjectModificationException, ActivityExecutionFailedException {
        replayActivity(activityInstanceId, null);
    }

    @Override
    public void replayActivity(final long activityInstanceId, final Map<Long, ConnectorStateReset> connectorsToReset) throws InvalidSessionException,
            ObjectNotFoundException, ObjectReadException, ActivityExecutionFailedException, ObjectModificationException {
        LicenseChecker.getInstance().checkLicenceAndFeature(Features.REPLAY_ACTIVITY);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ConnectorInstanceService connectorInstanceService = tenantAccessor.getConnectorInstanceService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final ContainerRegistry containerRegistry = tenantAccessor.getContainerRegistry();
        String containerType;
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                // Reset connectors first:
                if (connectorsToReset != null) {
                    for (final Entry<Long, ConnectorStateReset> connEntry : connectorsToReset.entrySet()) {
                        final SConnectorInstance connectorInstance = connectorInstanceService.getConnectorInstance(connEntry.getKey());
                        final ConnectorStateReset state = connEntry.getValue();
                        connectorInstanceService.setState(connectorInstance, state.name());
                    }
                }

                // Then replay activity:
                final SActivityInstance activityInstance = activityInstanceService.getActivityInstance(activityInstanceId);
                List<SConnectorInstance> connectorInstances = connectorInstanceService.getConnectorInstances(activityInstanceId,
                        SConnectorInstance.FLOWNODE_TYPE, ConnectorEvent.ON_ENTER, 0, 1, ConnectorState.FAILED.name());
                if (!connectorInstances.isEmpty()) {
                    throw new ActivityExecutionFailedException("There is at least one connector in failed on ON_ENTER of the activity: "
                            + connectorInstances.get(0).getName());
                }
                connectorInstances = connectorInstanceService.getConnectorInstances(activityInstanceId, SConnectorInstance.FLOWNODE_TYPE,
                        ConnectorEvent.ON_FINISH, 0, 1, ConnectorState.FAILED.name());
                if (!connectorInstances.isEmpty()) {
                    throw new ActivityExecutionFailedException("There is at least one connector in failed on ON_FINISH of the activity: "
                            + connectorInstances.get(0).getName());
                }
                // can change state and call execute
                activityInstanceService.setState(activityInstance, flowNodeStateManager.getState(activityInstance.getPreviousStateId()));
                activityInstanceService.setExecuting(activityInstance);
                containerType = SFlowElementsContainerType.PROCESS.name();
                if (activityInstance.getLogicalGroup(2) > 0) {
                    containerType = SFlowElementsContainerType.FLOWNODE.name();
                }
            } catch (final SConnectorInstanceReadException e) {
                throw new ObjectReadException(e, ConnectorInstance.class);
            } catch (final SActivityReadException e) {
                throw new ObjectReadException(e, ActivityInstance.class);
            } catch (final SActivityInstanceNotFoundException e) {
                throw new ObjectNotFoundException(e, ActivityInstance.class);
            } catch (final SFlowNodeModificationException e) {
                throw new ObjectModificationException(e, ActivityInstance.class);
            } catch (final SConnectorInstanceModificationException e) {
                throw new ObjectModificationException(e, ConnectorInstance.class);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ObjectReadException(e, ActivityInstance.class);
        }
        try {
            containerRegistry.executeFlowNodeInSameThread(activityInstanceId, null, null, containerType, null);
        } catch (final SActivityReadException e) {
            throw new ObjectReadException(e, ActivityInstance.class);
        } catch (final SBonitaException e) {
            throw new ActivityExecutionFailedException(e);
        }
    }

    @Override
    // TODO delete files after use/if an exception occurs
    public byte[] exportBarProcessContentUnderHome(final long processDefinitionId) throws BonitaRuntimeException, IOException, InvalidSessionException {
        String processesFolder;
        try {
            final long tenantId = getTenantAccessor().getTenantId();
            processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
        } catch (final BonitaHomeNotSetException e) {
            throw new BonitaRuntimeException(e);
        }
        final File file = new File(processesFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        final File processFolder = new File(file, String.valueOf(processDefinitionId));

        // copy current parameter to parameter file
        final File currentParasF = new File(processFolder.getPath(), "current-parameters.properties");
        if (currentParasF.exists()) {
            final File parasF = new File(processFolder.getPath(), "parameters.properties");
            if (!parasF.exists()) {
                parasF.createNewFile();
            }
            final String content = IOUtil.read(currentParasF);
            IOUtil.write(parasF, content);
        }

        // export actormapping
        final File actormappF = new File(processFolder.getPath(), "actorMapping.xml");
        if (!actormappF.exists()) {
            actormappF.createNewFile();
        }
        String xmlcontent = "";
        try {
            xmlcontent = exportActorMapping(processDefinitionId);
        } catch (final ActorMappingExportException e) {
            throw new BonitaRuntimeException(e);
        }
        IOUtil.write(actormappF, xmlcontent);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            IOUtil.zipDir(processFolder.getPath(), zos, processFolder.getPath());
            return baos.toByteArray();
        } finally {
            zos.close();
        }
    }

    @Override
    public Map<String, Serializable> executeConnectorAtProcessInstantiation(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long processInstanceId)
            throws InvalidSessionException, ArchivedProcessInstanceNotFoundException, ClassLoaderException, ConnectorException,
            InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorAtProcessInstantiationWithOtWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, processInstanceId);
    }

    @Override
    public Map<String, Serializable> executeConnectorAtProcessInstantiation(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues,
            final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId) throws InvalidSessionException,
            ArchivedProcessInstanceNotFoundException, ClassLoaderException, ConnectorException, InvalidEvaluationConnectorConditionException,
            NotSerializableException {
        return executeConnectorAtProcessInstantiationWithOtWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, processInstanceId);
    }

    private Map<String, Serializable> executeConnectorAtProcessInstantiationWithOtWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId)
            throws InvalidEvaluationConnectorConditionException, InvalidSessionException, ConnectorException, ArchivedProcessInstanceNotFoundException,
            ClassLoaderException, NotSerializableException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ArchiveService archiveService = tenantAccessor.getArchiveService();
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final SExpressionBuilders sExpressionBuilders = tenantAccessor.getSExpressionBuilders();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final SAProcessInstanceBuilder saProcessInstanceBuilder = tenantAccessor.getBPMInstanceBuilders().getSAProcessInstanceBuilder();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final GetArchivedProcessInstanceList getArchivedProcessInstanceList = new GetArchivedProcessInstanceList(processInstanceService,
                        persistenceService, tenantAccessor.getSearchEntitiesDescriptor(), processInstanceId, 0, 1, saProcessInstanceBuilder.getIdKey(),
                        OrderByType.ASC);
                getArchivedProcessInstanceList.execute();
                final ArchivedProcessInstance saprocessInstance = getArchivedProcessInstanceList.getResult().get(0);
                final long processDefinitionId = saprocessInstance.getProcessDefinitionId();
                final ClassLoader classLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);

                final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(sExpressionBuilders, connectorInputParameters);
                final SExpressionContext expcontext = new SExpressionContext();
                expcontext.setContainerId(processInstanceId);
                expcontext.setContainerType("PROCESS_INSTANCE");
                expcontext.setProcessDefinitionId(processDefinitionId);
                expcontext.setTime(saprocessInstance.getArchiveDate().getTime());
                final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                        connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
                if (operations != null) {
                    // execute operations
                    return executeOperations(connectorResult, operations, expcontext, classLoader, tenantAccessor);
                } else {
                    return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
                }
            } catch (final SConnectorException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } catch (final SProcessInstanceReadException e) {
                transactionExecutor.setTransactionRollback();
                throw new ArchivedProcessInstanceNotFoundException(e);
            } catch (final org.bonitasoft.engine.classloader.ClassLoaderException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } catch (final NotSerializableException e) {
                transactionExecutor.setTransactionRollback();
                throw e;
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public Map<String, Serializable> executeConnectorOnActivityInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long activityInstanceId)
            throws InvalidSessionException, ActivityInstanceNotFoundException, ProcessInstanceNotFoundException, ClassLoaderException, ConnectorException,
            InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnActivityInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, activityInstanceId);
    }

    @Override
    public Map<String, Serializable> executeConnectorOnActivityInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues,
            final Map<Operation, Map<String, Serializable>> operations, final long activityInstanceId) throws InvalidSessionException, ConnectorException,
            NotSerializableException, ClassLoaderException, InvalidEvaluationConnectorConditionException {
        return executeConnectorOnActivityInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, activityInstanceId);
    }

    /**
     * execute the connector and return connector output if there is no operation or operation output if there is operation
     */
    private Map<String, Serializable> executeConnectorOnActivityInstanceWithOrWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final Map<Operation, Map<String, Serializable>> operations, final long activityInstanceId)
            throws InvalidSessionException, ConnectorException, NotSerializableException, ClassLoaderException, InvalidEvaluationConnectorConditionException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SExpressionBuilders sExpressionBuilders = tenantAccessor.getSExpressionBuilders();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final SActivityInstance activityInstance = activityInstanceService.getActivityInstance(activityInstanceId);
                final SProcessInstance processInstance = processInstanceService.getProcessInstance(activityInstance.getRootContainerId());
                final long processDefinitionId = processInstance.getProcessDefinitionId();
                final ClassLoader classLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);
                final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(sExpressionBuilders, connectorInputParameters);
                final SExpressionContext expcontext = new SExpressionContext();
                expcontext.setContainerId(activityInstanceId);
                expcontext.setContainerType("ACTIVITY_INSTANCE");
                expcontext.setProcessDefinitionId(processDefinitionId);
                final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                        connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
                if (operations != null) {
                    // execute operations
                    return executeOperations(connectorResult, operations, expcontext, classLoader, tenantAccessor);
                } else {
                    return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
                }
            } catch (final SConnectorException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } catch (final NotSerializableException e) {
                transactionExecutor.setTransactionRollback();
                throw e;
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public Map<String, Serializable> executeConnectorOnCompletedActivityInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long activityInstanceId)
            throws InvalidSessionException, ArchivedActivityInstanceNotFoundException, ProcessInstanceNotFoundException, ClassLoaderException,
            ConnectorException, InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnCompletedActivityInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, activityInstanceId);
    }

    @Override
    public Map<String, Serializable> executeConnectorOnCompletedActivityInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues,
            final Map<Operation, Map<String, Serializable>> operations, final long activityInstanceId) throws InvalidSessionException,
            ArchivedActivityInstanceNotFoundException, ProcessInstanceNotFoundException, ClassLoaderException, ConnectorException,
            InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnCompletedActivityInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, activityInstanceId);
    }

    private Map<String, Serializable> executeConnectorOnCompletedActivityInstanceWithOrWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final Map<Operation, Map<String, Serializable>> operations, final long activityInstanceId)
            throws InvalidEvaluationConnectorConditionException, InvalidSessionException, ArchivedActivityInstanceNotFoundException, ClassLoaderException,
            NotSerializableException, ConnectorException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ArchiveService archiveService = tenantAccessor.getArchiveService();
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final SExpressionBuilders sExpressionBuilders = tenantAccessor.getSExpressionBuilders();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final SAActivityInstance aactivityInstance = activityInstanceService.getArchivedActivityInstance(activityInstanceId, persistenceService);

                final GetLastArchivedProcessInstance getLastArchivedProcessInstance = new GetLastArchivedProcessInstance(processInstanceService,
                        aactivityInstance.getRootContainerId(), persistenceService, tenantAccessor.getSearchEntitiesDescriptor());
                getLastArchivedProcessInstance.execute();

                final long processDefinitionId = getLastArchivedProcessInstance.getResult().getProcessDefinitionId();
                final ClassLoader classLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);

                final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(sExpressionBuilders, connectorInputParameters);
                final SExpressionContext expcontext = new SExpressionContext();
                expcontext.setContainerId(activityInstanceId);
                expcontext.setContainerType("ACTIVITY_INSTANCE");
                expcontext.setProcessDefinitionId(processDefinitionId);
                expcontext.setTime(aactivityInstance.getArchiveDate() + 500);
                final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                        connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
                if (operations != null) {
                    // execute operations
                    return executeOperations(connectorResult, operations, expcontext, classLoader, tenantAccessor);
                } else {
                    return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
                }
            } catch (final SProcessInstanceReadException e) {
                transactionExecutor.setTransactionRollback();
                throw new ArchivedActivityInstanceNotFoundException(e);
            } catch (final org.bonitasoft.engine.classloader.ClassLoaderException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } catch (final NotSerializableException e) {
                transactionExecutor.setTransactionRollback();
                throw e;
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public Map<String, Serializable> executeConnectorOnCompletedProcessInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long processInstanceId)
            throws InvalidSessionException, ArchivedProcessInstanceNotFoundException, ClassLoaderException, ConnectorException,
            InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnCompletedProcessInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, processInstanceId);
    }

    @Override
    public Map<String, Serializable> executeConnectorOnCompletedProcessInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues,
            final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId) throws InvalidSessionException,
            ArchivedProcessInstanceNotFoundException, ClassLoaderException, ConnectorException, InvalidEvaluationConnectorConditionException,
            NotSerializableException {
        return executeConnectorOnCompletedProcessInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, processInstanceId);
    }

    private Map<String, Serializable> executeConnectorOnCompletedProcessInstanceWithOrWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId)
            throws InvalidEvaluationConnectorConditionException, InvalidSessionException, ArchivedProcessInstanceNotFoundException, ClassLoaderException,
            NotSerializableException, ConnectorException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ArchiveService archiveService = tenantAccessor.getArchiveService();
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final SExpressionBuilders sExpressionBuilders = tenantAccessor.getSExpressionBuilders();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final GetLastArchivedProcessInstance getLastArchivedProcessInstance = new GetLastArchivedProcessInstance(processInstanceService,
                        processInstanceId, persistenceService, tenantAccessor.getSearchEntitiesDescriptor());
                getLastArchivedProcessInstance.execute();
                final ArchivedProcessInstance saprocessInstance = getLastArchivedProcessInstance.getResult();
                final long processDefinitionId = saprocessInstance.getProcessDefinitionId();
                final ClassLoader classLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);

                final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(sExpressionBuilders, connectorInputParameters);
                final SExpressionContext expcontext = new SExpressionContext();
                expcontext.setContainerId(processInstanceId);
                expcontext.setContainerType("PROCESS_INSTANCE");
                expcontext.setProcessDefinitionId(processDefinitionId);
                expcontext.setTime(saprocessInstance.getArchiveDate().getTime() + 500);
                final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                        connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
                if (operations != null) {
                    // execute operations
                    return executeOperations(connectorResult, operations, expcontext, classLoader, tenantAccessor);
                } else {
                    return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
                }
            } catch (final SProcessInstanceReadException e) {
                transactionExecutor.setTransactionRollback();
                throw new ArchivedProcessInstanceNotFoundException(e);
            } catch (final org.bonitasoft.engine.classloader.ClassLoaderException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } catch (final NotSerializableException e) {
                transactionExecutor.setTransactionRollback();
                throw e;
            } catch (final SConnectorException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public Map<String, Serializable> executeConnectorOnProcessInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long processInstanceId)
            throws ClassLoaderException, InvalidSessionException, ProcessInstanceNotFoundException, ConnectorException,
            InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnProcessInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, processInstanceId);
    }

    @Override
    public Map<String, Serializable> executeConnectorOnProcessInstance(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues,
            final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId) throws ClassLoaderException, InvalidSessionException,
            ProcessInstanceNotFoundException, ConnectorException, InvalidEvaluationConnectorConditionException, NotSerializableException {
        return executeConnectorOnProcessInstanceWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, processInstanceId);
    }

    /**
     * execute the connector and return connector output if there is no operation or operation output if there is operation
     */
    private Map<String, Serializable> executeConnectorOnProcessInstanceWithOrWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final Map<Operation, Map<String, Serializable>> operations, final long processInstanceId)
            throws InvalidEvaluationConnectorConditionException, InvalidSessionException, NotSerializableException, ClassLoaderException, ConnectorException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SExpressionBuilders sExpressionBuilders = tenantAccessor.getSExpressionBuilders();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final SProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
                final long processDefinitionId = processInstance.getProcessDefinitionId();
                final ClassLoader classLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);
                final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(sExpressionBuilders, connectorInputParameters);
                final SExpressionContext expcontext = new SExpressionContext();
                expcontext.setContainerId(processInstanceId);
                expcontext.setContainerType("PROCESS_INSTANCE");
                expcontext.setProcessDefinitionId(processDefinitionId);
                final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                        connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
                if (operations != null) {
                    // execute operations
                    return executeOperations(connectorResult, operations, expcontext, classLoader, tenantAccessor);
                } else {
                    return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
                }
            } catch (final SConnectorException e) {
                transactionExecutor.setTransactionRollback();
                throw new ConnectorException(e);
            } catch (final NotSerializableException e) {
                transactionExecutor.setTransactionRollback();
                throw e;
            } catch (final SBonitaException e) {
                transactionExecutor.setTransactionRollback();
                throw new ClassLoaderException(e);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public ProcessDefinition deploy(final BusinessArchive businessArchive) throws InvalidSessionException, ProcessDeployException,
            ProcessDefinitionAlreadyExistsException {
        final DesignProcessDefinition processDefinition = businessArchive.getProcessDefinition();

        if (processDefinition.getStringIndexValue(1) != null || processDefinition.getStringIndexLabel(1) != null
                || processDefinition.getStringIndexValue(2) != null || processDefinition.getStringIndexLabel(2) != null
                || processDefinition.getStringIndexValue(3) != null || processDefinition.getStringIndexLabel(3) != null
                || processDefinition.getStringIndexValue(4) != null || processDefinition.getStringIndexLabel(4) != null
                || processDefinition.getStringIndexValue(5) != null || processDefinition.getStringIndexLabel(5) != null) {
            LicenseChecker.getInstance().checkLicenceAndFeature(Features.SEARCH_INDEX);
        }

        return super.deploy(businessArchive);
    }

    @Override
    public ProcessInstance updateProcessInstanceIndex(final long processInstanceId, final Index index, final String value) throws InvalidSessionException,
            ProcessInstanceNotFoundException, ProcessInstanceModificationException, ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final BPMInstanceBuilders bpmInstanceBuilders = tenantAccessor.getBPMInstanceBuilders();
        final SProcessInstanceUpdateBuilder updateBuilder = bpmInstanceBuilders.getProcessInstanceUpdateBuilder();
        try {
            final UpdateProcessInstance updateProcessInstance = new UpdateProcessInstance(processInstanceService, updateBuilder, processInstanceId, index,
                    value);
            transactionExecutor.execute(updateProcessInstance);
            return getProcessInstance(processInstanceId);
        } catch (final SProcessInstanceNotFoundException spinfe) {
            throw new ProcessInstanceNotFoundException(spinfe);
        } catch (final SBonitaException sbe) {
            throw new ProcessInstanceModificationException(sbe);
        } catch (final ProcessInstanceReadException pire) {
            throw new ProcessInstanceModificationException(pire);
        }
    }

    @Override
    public ProcessInstance updateProcessInstance(final long processInstanceId, final ProcessInstanceUpdateDescriptor updateDescriptor)
            throws InvalidSessionException, ProcessInstanceNotFoundException, ProcessInstanceModificationException, ProcessDefinitionNotFoundException {
        if (updateDescriptor == null || updateDescriptor.getFields().isEmpty()) {
            throw new ProcessInstanceModificationException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final BPMInstanceBuilders bpmInstanceBuilders = tenantAccessor.getBPMInstanceBuilders();
        final SProcessInstanceUpdateBuilder updateBuilder = bpmInstanceBuilders.getProcessInstanceUpdateBuilder();
        try {
            final UpdateProcessInstance updateProcessInstance = new UpdateProcessInstance(processInstanceService, updateDescriptor, updateBuilder,
                    processInstanceId);
            transactionExecutor.execute(updateProcessInstance);
            return getProcessInstance(processInstanceId);
        } catch (final SProcessInstanceNotFoundException spinfe) {
            throw new ProcessInstanceNotFoundException(spinfe);
        } catch (final SBonitaException sbe) {
            throw new ProcessInstanceModificationException(sbe);
        } catch (final ProcessInstanceReadException pire) {
            throw new ProcessInstanceModificationException(pire);
        }
    }

}
