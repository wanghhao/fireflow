package org.fireflow.engine.impl;

// Generated Feb 23, 2008 12:04:21 AM by Hibernate Tools 3.2.0.b9
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.fireflow.engine.EngineException;
import org.fireflow.engine.IProcessInstance;
import org.fireflow.engine.IRuntimeContextAware;
import org.fireflow.engine.ITaskInstance;
import org.fireflow.engine.IWorkflowSession;
import org.fireflow.engine.IWorkflowSessionAware;
import org.fireflow.kenel.IJoinPoint;
import org.fireflow.kenel.INetInstance;
import org.fireflow.kenel.ISynchronizerInstance;
import org.fireflow.kenel.KenelException;
import org.fireflow.engine.RuntimeContext;
import org.fireflow.engine.definition.WorkflowDefinition;
import org.fireflow.engine.event.IProcessInstanceEventListener;
import org.fireflow.engine.event.ProcessInstanceEvent;
import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.kenel.IToken;
import org.fireflow.model.EventListener;
import org.fireflow.model.WorkflowProcess;

/**
 * ProcessInstance generated by hbm2java
 */
public class ProcessInstance implements IProcessInstance, IRuntimeContextAware,IWorkflowSessionAware,java.io.Serializable {

    private String id = null;
    private String processId = null;
    private Integer version = null;
    private String name = null;
    private String displayName = null;
    private Integer state = null;
    private Date createdTime = null;
    private Date startedTime = null;
    private Date endTime = null;
    private Date expiredTime = null;
    private String parentProcessInstanceId = null;
    private String parentTaskInstanceId = null;
    private Map<String, Object> processInstanceVariables = new HashMap<String, Object>();

    protected RuntimeContext rtCtx = null;
    protected IWorkflowSession workflowSession = null;
    
    public void setRuntimeContext(RuntimeContext ctx){
        this.rtCtx = ctx;
    }    
    public RuntimeContext getRuntimeContext(){
        return this.rtCtx;
    }     
    public ProcessInstance() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessId() {
        return this.processId;
    }

    public void setProcessId(String processID) {
        this.processId = processID;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String label) {
        this.displayName = label;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public IJoinPoint createJoinPoint(ISynchronizerInstance synchInst, IToken token) throws EngineException {

        int enterTransInstanceCount = synchInst.getEnteringTransitionInstances().size();
        if (enterTransInstanceCount == 0) {
            throw new EngineException("业务流程[" + this.getName() + "]的结构不正确，synchronizer[" + synchInst.getSynchronizer() + "]没有输入Transition");
        }
        IJoinPoint resultJoinPoint = null;
        if (enterTransInstanceCount == 1) {
            // 生成一个不存储到数据库中的JoinPoint
            resultJoinPoint = new JoinPoint();
            resultJoinPoint.addValue(token.getValue());

            if (token.isAlive()) {
                resultJoinPoint.setAlive(true);
            }
            if (token.getAppointedTransitionNames() != null) {
                resultJoinPoint.addAppointedTransitionNames(token.getAppointedTransitionNames());
            }

            return resultJoinPoint;
        } else {
            // 1、首先从数据库中查询，看看是否已经存在该JoinPoint
            IPersistenceService persistenceService = rtCtx.getPersistenceService();
            List<IJoinPoint> joinPointList = persistenceService.findJoinPointsForProcessInstance(this.getId(), synchInst.getSynchronizer().getId());
            IJoinPoint joinPoint = null;
            if (joinPointList != null && joinPointList.size() > 0) {
                joinPoint = joinPointList.get(0);
            }
            if (joinPoint != null) {
                resultJoinPoint = joinPoint;
            } else {
                // 2、生成一个存储到数据库中的joinPoint
                resultJoinPoint = new JoinPoint();
                resultJoinPoint.setProcessInstance(this);
                resultJoinPoint.setSynchronizerId(synchInst.getSynchronizer().getId());
            }
            resultJoinPoint.addValue(token.getValue());

            if (token.isAlive()) {
                resultJoinPoint.setAlive(true);
            }
            if (token.getAppointedTransitionNames() != null) {
                resultJoinPoint.addAppointedTransitionNames(token.getAppointedTransitionNames());
            }
            persistenceService.saveOrUpdateJoinPoint(resultJoinPoint);
            return resultJoinPoint;
        }
    }

    public void run() throws EngineException, KenelException {
        if (this.getState().intValue() != IProcessInstance.INITIALIZED) {
            throw new EngineException("流程实例的状态为" + this.getState() + ",不可以执行run操作");
        }

        INetInstance netInstance = (INetInstance) rtCtx.getKenelManager().getNetInstance(this.getProcessId(), this.getVersion());
        if (netInstance == null) {
            throw new EngineException("系统中没有找到workflowProcessId=" + this.getProcessId() + "的NetInstance,不可以执行run操作");
        }
        //触发事件
        ProcessInstanceEvent event = new ProcessInstanceEvent();
        event.setEventType(ProcessInstanceEvent.BEFORE_PROCESS_INSTANCE_RUN);
        event.setSource(this);
        this.fireProcessInstanceEvent(event);

        this.setState(IProcessInstance.STARTED);
        this.setStartedTime(rtCtx.getCalendarService().getSysDate());
        rtCtx.getPersistenceService().saveOrUpdateProcessInstance(this);
        netInstance.run(this);
    }

    public Map getProcessInstanceVariables() {
        return processInstanceVariables;
    }

    public void setProcessInstanceVariables(Map vars) {
        processInstanceVariables.putAll(vars);
    }

    public Object getProcessInstanceVariable(String name) {
        return processInstanceVariables.get(name);
    }

    public void setProcessInstanceVariable(String name, Object var) {
        processInstanceVariables.put(name, var);
    }

    public WorkflowProcess getWorkflowProcess() throws EngineException{
        WorkflowDefinition workflowDef = rtCtx.getDefinitionService().getWorkflowDefinitionByProcessIdAndVersion(this.getProcessId(), this.getVersion());
        WorkflowProcess workflowProcess = null;
        try {
            workflowProcess = workflowDef.getWorkflowProcess();
        } catch (EngineException ex) {
            Logger.getLogger(ProcessInstance.class.getName()).log(Level.SEVERE, null, ex);
        }

        return workflowProcess;
    }

    public String getParentTaskInstanceId() {
        return parentTaskInstanceId;
    }

    public void setParentTaskInstanceId(String taskInstId) {
        parentTaskInstanceId = taskInstId;
    }

    public Date getCreatedTime() {
        return this.createdTime;
    }

    public Date getStartedTime() {
        return this.startedTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
    }

    /**
     * 正常结束工作流
     * 1、首先检查有无活动的token,如果有则直接返回，如果没有则结束当前流程
     * 2、执行结束流程的操作，将state的值设置为结束状态
     * 3、然后检查parentTaskInstanceId是否为null，如果不为null则，调用父taskinstance的complete操作。
     */
    public void complete() throws EngineException, KenelException {
        System.out.println("====Inside ProcessInstance.complete()...");
        List<IToken> tokens = rtCtx.getPersistenceService().findTokensForProcessInstance(this.getId(), null);
        boolean canBeCompleted = true;
        for (int i = 0; tokens != null && i < tokens.size(); i++) {
            IToken token = tokens.get(i);
            System.out.println("====Inside ProcessInstance.complete()::" + token.getNodeId() + " is alive?" + token.isAlive());
            if (token.isAlive()) {
                canBeCompleted = false;
                break;
            }
        }
        if (!canBeCompleted) {
            return;
        }

        this.setState(IProcessInstance.COMPLETED);
        //记录结束时间
        this.setEndTime(rtCtx.getCalendarService().getSysDate());
        rtCtx.getPersistenceService().saveOrUpdateProcessInstance(this);

        //触发事件
        ProcessInstanceEvent event = new ProcessInstanceEvent();
        event.setEventType(ProcessInstanceEvent.AFTER_PROCESS_INSTANCE_COMPLETE);
        event.setSource(this);
        this.fireProcessInstanceEvent(event);

        if (this.getParentTaskInstanceId() != null && !this.getParentTaskInstanceId().trim().equals("")) {
            ITaskInstance taskInstance = rtCtx.getPersistenceService().findTaskInstanceById(this.getParentTaskInstanceId());
            ((TaskInstance) taskInstance).complete();
        }
    }

    /**
     * 强行中止流程实例，不管是否达到终态。
     * @throws RuntimeException
     */
    public void abort() throws EngineException {
        //TODO 待补充
    }

    /**
     * 触发process instance相关的事件
     * @param e
     * @throws org.fireflow.engine.EngineException
     */
    protected void fireProcessInstanceEvent(ProcessInstanceEvent e) throws EngineException {
        WorkflowProcess workflowProcess = this.getWorkflowProcess();
        if (workflowProcess == null) {
            return;
        }

        List listeners = workflowProcess.getEventListeners();
        for (int i = 0; i < listeners.size(); i++) {
            EventListener listener = (EventListener) listeners.get(i);
            Object obj = rtCtx.getBeanByClassName(listener.getClassName());
            if (obj != null) {
                ((IProcessInstanceEventListener) obj).onProcessInstanceFired(e);
            }
        }
    }

    public Date getExpiredTime() {
        return this.expiredTime;
    }
    
    public void setExpiredTime(Date arg){
        this.expiredTime = arg;
        
    }

    public IWorkflowSession getCurrentWorkflowSession() {
        return this.workflowSession;
    }

    public void setCurrentWorkflowSession(IWorkflowSession session) {
        this.workflowSession = session;
    }
}
