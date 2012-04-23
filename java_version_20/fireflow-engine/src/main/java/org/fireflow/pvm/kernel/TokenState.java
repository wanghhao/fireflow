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
package org.fireflow.pvm.kernel;

import java.util.Locale;
import java.util.ResourceBundle;

import org.fireflow.engine.entity.runtime.ProcessInstanceState;

/**
 * @author 非也
 * @version 2.0
 */
public enum TokenState {
	/**
	 * 活的状态
	 */
	ALIVE(-1),
	
	/**
	 * 初始化状态
	 */
	INITIALIZED(0),

	/**
	 * 运行状态
	 */
	RUNNING(1),
	
	
	/**
	 * 错误处理中
	 */
	FAULTING(5),
	
	/**
	 * 取消处理中
	 */
	ABORTING(6),
	
	/**
	 * 补偿操作中
	 */
	COMPENSATING(7),
	
	/**
	 * 活动属性和非活动属性的分界线
	 */
	DELIMITER(10),
	
	/**
	 * 已经结束
	 */
	COMPLETED(11),
	
	/**
	 * 因错误而终止
	 */
	FAULTED(15),
	
	/**
	 * 被取消
	 */
	ABORTED(16),
	/**
	 * 被补偿
	 */
	COMPENSATED(17);
	
	
	
	
	private int value = 0;
	private TokenState(int value){
		this.value = value;
	}
	
	public String getDisplayName(Locale locale){
		ResourceBundle resb = ResourceBundle.getBundle("EngineMessages", locale);
		return resb.getString(this.name());
	}
	
	public String getDisplayName(){
		return this.getDisplayName(Locale.getDefault());
	}
	
	public int getValue(){
		return this.value;
	}
	
	public static TokenState valueOf(Integer v){
		TokenState[] states =  TokenState.values();
		for (TokenState state : states){
			if (state.getValue()== v){
				return state;
			}
		}
		return null;
	}	
}
