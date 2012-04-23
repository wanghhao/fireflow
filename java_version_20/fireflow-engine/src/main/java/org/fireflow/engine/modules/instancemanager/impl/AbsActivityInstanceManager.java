/**
 * Copyright 2007-2010 非也
 * All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation。
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 */
package org.fireflow.engine.modules.instancemanager.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fireflow.engine.WorkflowSession;
import org.fireflow.engine.context.AbsEngineModule;
import org.fireflow.engine.context.RuntimeContext;
import org.fireflow.engine.context.RuntimeContextAware;
import org.fireflow.engine.entity.EntityProperty;
import org.fireflow.engine.entity.runtime.ActivityInstance;
import org.fireflow.engine.entity.runtime.ActivityInstanceState;
import org.fireflow.engine.entity.runtime.ProcessInstanceProperty;
import org.fireflow.engine.entity.runtime.impl.ActivityInstanceImpl;
import org.fireflow.engine.impl.InternalSessionAttributeKeys;
import org.fireflow.engine.impl.WorkflowSessionLocalImpl;
import org.fireflow.engine.invocation.ServiceInvoker;
import org.fireflow.engine.modules.beanfactory.BeanFactory;
import org.fireflow.engine.modules.calendar.CalendarService;
import org.fireflow.engine.modules.event.EventBroadcaster;
import org.fireflow.engine.modules.event.EventBroadcasterManager;
import org.fireflow.engine.modules.instancemanager.ActivityInstanceManager;
import org.fireflow.engine.modules.instancemanager.event.ActivityInstanceEvent;
import org.fireflow.engine.modules.instancemanager.event.ActivityInstanceEventTrigger;
import org.fireflow.engine.modules.persistence.ActivityInstancePersister;
import org.fireflow.engine.modules.persistence.PersistenceService;
import org.fireflow.engine.modules.workitem.WorkItemManager;
import org.fireflow.model.servicedef.ServiceDef;
import org.fireflow.pvm.kernel.BookMark;
import org.fireflow.pvm.kernel.ExecutionEntrance;
import org.fireflow.pvm.kernel.KernelManager;
import org.fireflow.pvm.kernel.Token;

/**
 * @author 非也
 * @version 2.0
 */
public abstract class AbsActivityInstanceManager  extends AbsEngineModule implements
		ActivityInstanceManager,RuntimeContextAware {
	Log log = LogFactory.getLog(AbsActivityInstanceManager.class);
	
	protected RuntimeContext runtimeContext = null;
	protected Map<String,ServiceInvoker> serviceInvokerRegistry = new HashMap<String,ServiceInvoker>();

	
	public RuntimeContext getRuntimeContext() {
		return runtimeContext;
	}


	public void setRuntimeContext(RuntimeContext ctx) {
		runtimeContext = ctx;		
	}
	
	/* (non-Javadoc)
	 * @see org.fireflow.engine.instancemanager.ActivityInstanceManager#completeActivityInstance(org.fireflow.engine.WorkflowSession, org.fireflow.engine.entity.runtime.ActivityInstance)
	 */
	public void onServiceCompleted(WorkflowSession session,
			ActivityInstance activityInstance) {
		WorkflowSessionLocalImpl sessionLocalImpl = (WorkflowSessionLocalImpl)session;
		RuntimeContext runtimeContext = sessionLocalImpl.getRuntimeContext();

		
		KernelManager kernelManager = runtimeContext.getDefaultEngineModule(KernelManager.class);
		
		Token token = kernelManager.getToken(activityInstance.getTokenId(), activityInstance.getProcessType());
		BookMark bookMark = new BookMark();
		bookMark.setToken(token);
		bookMark.setExtraArg(BookMark.SOURCE_TOKEN, token);
		bookMark.setExecutionEntrance(ExecutionEntrance.FORWARD_TOKEN);
		kernelManager.addBookMark(bookMark);
		
		kernelManager.execute(sessionLocalImpl);
		
	}	
	
//	public ActivityInstance abortActivityInstance(WorkflowSession session , ActivityInstance activityInstance){
//		RuntimeContext context = ((WorkflowSessionLocalImpl)session).getRuntimeContext();
//		KernelManager kernelManager = context.getDefaultEngineModule(KernelManager.class);		
//		Token token = kernelManager.getToken(activityInstance.getTokenId(), activityInstance.getProcessType());
//
//		kernelManager.fireTerminationEvent(session, token, null);
//
//		return activityInstance;
//	}
	
	public ActivityInstance suspendActivityInstance(WorkflowSession session , ActivityInstance activityInstance){
		RuntimeContext context = ((WorkflowSessionLocalImpl)session).getRuntimeContext();
		PersistenceService persistenceService = context.getEngineModule(PersistenceService.class, activityInstance.getProcessType());
		//从session取相关字段的值
		Map<EntityProperty,Object> fieldsValues = (Map<EntityProperty,Object>)session.getAttribute(InternalSessionAttributeKeys.FIELDS_VALUES);
		if (fieldsValues!=null){
			String note = (String)fieldsValues.get(ProcessInstanceProperty.NOTE);
			if (!StringUtils.isEmpty(note)){
				((ActivityInstanceImpl)activityInstance).setNote(note);
			}
		}
		ActivityInstancePersister persister = persistenceService.getActivityInstancePersister();
		((ActivityInstanceImpl)activityInstance).setSuspended(true);
		persister.saveOrUpdate(activityInstance);
		
		return activityInstance;
	}
	
	public ActivityInstance restoreActivityInstance(WorkflowSession session , ActivityInstance activityInstance){
		RuntimeContext context = ((WorkflowSessionLocalImpl)session).getRuntimeContext();
		PersistenceService persistenceService = context.getEngineModule(PersistenceService.class, activityInstance.getProcessType());
		
		//2、然后restore processInstance
		//从session取相关字段的值
		Map<EntityProperty,Object> fieldsValues = (Map<EntityProperty,Object>)session.getAttribute(InternalSessionAttributeKeys.FIELDS_VALUES);
		if (fieldsValues!=null){
			String note = (String)fieldsValues.get(ProcessInstanceProperty.NOTE);
			if (!StringUtils.isEmpty(note)){
				((ActivityInstanceImpl)activityInstance).setNote(note);
			}
		}
		ActivityInstancePersister persister = persistenceService.getActivityInstancePersister();
		((ActivityInstanceImpl)activityInstance).setSuspended(false);
		persister.saveOrUpdate(activityInstance);
		return activityInstance;
	}
	
	protected ServiceInvoker getServiceInvoker(RuntimeContext runtimeContext,ServiceDef service,String processType){
		ServiceInvoker serviceInvoker = null;
		String invokerBeanName = service.getInvokerBeanName();
		String invokerClassName = service.getInvokerClassName();
		
		BeanFactory beanFactory = runtimeContext.getEngineModule(BeanFactory.class,processType);
		if (!StringUtils.isEmpty(invokerBeanName)){
			
			serviceInvoker = (ServiceInvoker)beanFactory.getBean(invokerBeanName);
		}
		
		if (serviceInvoker==null && !StringUtils.isEmpty(invokerClassName)){
			//首先从缓存中获得serviceInvoker，避免重复创建。
			serviceInvoker = this.serviceInvokerRegistry.get(invokerClassName);
			if (serviceInvoker==null){
				serviceInvoker = (ServiceInvoker)beanFactory.createBean(invokerClassName);
				if (serviceInvoker!=null){
					serviceInvokerRegistry.put(invokerClassName, serviceInvoker);
				}
			}
		}
		return serviceInvoker;
	}
	
	public void changeActivityInstanceState(WorkflowSession session,ActivityInstance activityInstance,ActivityInstanceState newState,Object workflowElement){
		CalendarService calendarService = runtimeContext.getEngineModule(CalendarService.class,activityInstance.getProcessType());		
		PersistenceService persistenceStrategy = runtimeContext.getEngineModule(PersistenceService.class, activityInstance.getProcessType());
		ActivityInstancePersister actInstPersistenceService = persistenceStrategy.getActivityInstancePersister();

		//执行abort操作
		if (newState.getValue()==ActivityInstanceState.ABORTED.getValue() ){
			Map<EntityProperty,Object> fieldsValues = (Map<EntityProperty,Object>)session.getAttribute(InternalSessionAttributeKeys.FIELDS_VALUES);
			if (fieldsValues!=null){
				String note = (String)fieldsValues.get(ProcessInstanceProperty.NOTE);
				if (!StringUtils.isEmpty(note)){
					((ActivityInstanceImpl)activityInstance).setNote(note);
				}
			}
			if (!StringUtils.isEmpty(activityInstance.getServiceId())){
				// 将Workitem设置为Aborted状态，
				WorkItemManager wiMgr =  runtimeContext.getEngineModule(WorkItemManager.class, activityInstance.getProcessType());
				wiMgr.abortWorkfItemOfTheSameActInst(session,activityInstance);
			}
		}
		
		if (newState.getValue()>ActivityInstanceState.DELIMITER.getValue()){
			((ActivityInstanceImpl)activityInstance).setEndTime(calendarService.getSysDate());
			

		}else if (newState.equals(ActivityInstanceState.RUNNING)){
			((ActivityInstanceImpl)activityInstance).setStartedTime(calendarService.getSysDate());
		}
		
		((ActivityInstanceImpl)activityInstance).setState(newState);
		actInstPersistenceService.saveOrUpdate(activityInstance);

		if (newState.getValue()>ActivityInstanceState.DELIMITER.getValue()){
			//发布AFTER_ACTIVITY_INSTANCE_END
			
			this.fireActivityInstanceEvent(session, activityInstance, workflowElement, ActivityInstanceEventTrigger.AFTER_ACTIVITY_INSTANCE_END);
		}
	}

	public void fireActivityInstanceEvent(WorkflowSession session,ActivityInstance actInst,Object activity,ActivityInstanceEventTrigger eventType){

		WorkflowSessionLocalImpl sessionLocalImpl = (WorkflowSessionLocalImpl)session;
		RuntimeContext rtCtx = sessionLocalImpl.getRuntimeContext();
		EventBroadcasterManager evetBroadcasterMgr = rtCtx.getDefaultEngineModule(EventBroadcasterManager.class);
		
		EventBroadcaster broadcaster = evetBroadcasterMgr.getEventBroadcaster(ActivityInstanceEvent.class.getName());
		if (broadcaster!=null){
			ActivityInstanceEvent event = new ActivityInstanceEvent();
			event.setSource(actInst);
			event.setEventTrigger(eventType);
			event.setWorkflowElement(activity);
			
			broadcaster.fireEvent(sessionLocalImpl, event);
		}
	}
}
