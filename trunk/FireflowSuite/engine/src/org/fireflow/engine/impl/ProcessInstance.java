package org.fireflow.engine.impl;

// Generated Feb 23, 2008 12:04:21 AM by Hibernate Tools 3.2.0.b9
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fireflow.engine.EngineException;
import org.fireflow.engine.IProcessInstance;
import org.fireflow.engine.ITaskInstance;
import org.fireflow.kenel.IJoinPoint;
import org.fireflow.kenel.INetInstance;
import org.fireflow.kenel.ISynchronizerInstance;
import org.fireflow.kenel.KenelException;
import org.fireflow.engine.RuntimeContext;
import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.kenel.IToken;
import org.fireflow.model.WorkflowProcess;

/**
 * ProcessInstance generated by hbm2java
 */
public class ProcessInstance implements IProcessInstance, java.io.Serializable {

    private String id = null;
    private String processId = null;
    private String name = null;
    private String displayName = null;
    private Integer state = null;
    private String parentProcessInstanceId = null;
    private String parentTaskInstanceId = null;
    private Map<String, Object> processInstanceVariables = new HashMap<String, Object>();
// private ProcessInstance parentProcessInstance;
// private Set taskInstances = new HashSet(0);
// private Set joinPoints = new HashSet(0);
// private Set tokens = new HashSet(0);
    public ProcessInstance() {
    }

// public ProcessInstance(String processId, String name, String displayName,
// Integer state, ProcessInstance parentProcessInstance,
// Set taskInstances, Set joinPoints, Set tokens) {
// this.processId = processId;
// this.name = name;
// this.label = displayName;
// this.state = state;
// this.parentProcessInstance = parentProcessInstance;
// this.taskInstances = taskInstances;
// this.joinPoints = joinPoints;
// this.tokens = tokens;
// }
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

// public ProcessInstance getParentProcessInstance() {
// return this.parentProcessInstance;
// }
//
// public void setParentProcessInstance(ProcessInstance parentProcessInstance) {
// this.parentProcessInstance = parentProcessInstance;
// }
//
// public Set getTaskInstances() {
// return this.taskInstances;
// }
//
// public void setTaskInstances(Set taskInstances) {
// this.taskInstances = taskInstances;
// }
//
// public Set getJoinPoints() {
// return this.joinPoints;
// }
    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    // public void setJoinPoints(Set joinPoints) {
// this.joinPoints = joinPoints;
// }
//
// public Set getTokens() {
// return this.tokens;
// }
//
// public void setTokens(Set tokens) {
// this.tokens = tokens;
// }
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
            RuntimeContext ctx = RuntimeContext.getInstance();

            IPersistenceService persistenceService = ctx.getPersistenceService();
            IJoinPoint joinPoint = persistenceService.findJoinPoint(this, synchInst.getSynchronizer().getId());
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
            persistenceService.saveJoinPoint(resultJoinPoint);
            return resultJoinPoint;
        }
    }

    public void run() throws EngineException, KenelException {
        if (this.getState().intValue() != IProcessInstance.INITIALIZED) {
            throw new EngineException("流程实例的状态为" + this.getState() + ",不可以执行run操作");
        }

        RuntimeContext ctx = RuntimeContext.getInstance();
        INetInstance netInstance = (INetInstance) ctx.getKenelManager().getWFElementInstance(this.getProcessId());
        if (netInstance == null) {
            throw new EngineException("系统中没有找到workflowProcessId=" + this.getProcessId() + "的NetInstance,不可以执行run操作");
        }
        this.setState(IProcessInstance.STARTED);
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

    public WorkflowProcess getWorkflowProcess() {
        RuntimeContext ctx = RuntimeContext.getInstance();
        return ctx.getDefinitionService().getWorkflowProcessById(this.getProcessId());
    }

    public String getParentTaskInstanceId() {
        return parentTaskInstanceId;
    }

    public void setParentTaskInstanceId(String taskInstId) {
        parentTaskInstanceId = taskInstId;
    }

    /**
     * 正常结束工作流
     * 1、首先检查有无活动的token,如果有则直接返回，如果没有则结束当前流程
     * 2、执行结束流程的操作，将state的值设置为结束状态
     * 3、然后检查parentTaskInstanceId是否为null，如果不为null则，调用父taskinstance的complete操作。
     */
    public void complete() throws EngineException, KenelException {
        RuntimeContext ctx = RuntimeContext.getInstance();
        List<IToken> tokens = ctx.getPersistenceService().findTokens(this);
        boolean canBeCompleted = true;
        for (int i = 0; tokens != null && i < tokens.size(); i++) {
            IToken token = tokens.get(i);
            if (token.isAlive()) {
                canBeCompleted = false;
                break;
            }
        }
        if (!canBeCompleted) {
            return;
        }

        this.setState(IProcessInstance.COMPLETED);
        //TODO 应该记录结束时间吧？
        ctx.getPersistenceService().saveProcessInstance(this);

        if (this.getParentTaskInstanceId() != null && !this.getParentTaskInstanceId().trim().equals("")) {
            ITaskInstance taskInstance = ctx.getPersistenceService().findTaskInstanceById(this.getParentTaskInstanceId());
            taskInstance.complete();
        }
    }
}