package org.fireflow.engine.impl;

// Generated Feb 23, 2008 12:04:21 AM by Hibernate Tools 3.2.0.b9

import java.util.HashSet;
import java.util.Set;

import org.fireflow.engine.IProcessInstance;

/**
 * JoinPoint generated by hbm2java
 */
public class JoinPoint implements org.fireflow.kenel.IJoinPoint, java.io.Serializable {

	private String id;
	private String synchronizerId;
	private Integer value;
	private Boolean alive = Boolean.FALSE;
	private Set<String> appointedTransitionNames = new HashSet<String>(0);
	private IProcessInstance processInstance;

	public JoinPoint() {
	}

	public JoinPoint(ProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

	public JoinPoint(String synchronizerID, Integer value, Boolean alive,
			Set<String> appointedTransitionNames, ProcessInstance processInstance) {
		this.synchronizerId = synchronizerID;
		this.value = value;
		this.alive = alive;
		this.appointedTransitionNames = appointedTransitionNames;
		this.processInstance = processInstance;
	}
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
	public String getSynchronizerId() {
		return this.synchronizerId;
	}

	public void setSynchronizerId(String synchronizerId) {
		this.synchronizerId = synchronizerId;
	}

	public Integer getValue() {
		return this.value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}
	
	public void addValue(Integer v){
		if (this.value==null){
			this.value = v;
		}else {
			this.value = new Integer(this.value.intValue()+v.intValue());
		}
	}

	public Boolean getAlive() {
		return this.alive;
	}

	public void setAlive(Boolean alive) {
		this.alive = alive;
	}

	public Set<String> getAppointedTransitionNames() {
		return this.appointedTransitionNames;
	}

	public void setAppointedTransitionNames(Set<String> appointedTransitionNames) {
		this.appointedTransitionNames = appointedTransitionNames;
	}

	public IProcessInstance getProcessInstance() {
		return this.processInstance;
	}

	public void setProcessInstance(IProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

	public void addAppointedTransitionNames(Set<String> arg){
		appointedTransitionNames.addAll(arg);
	}
}