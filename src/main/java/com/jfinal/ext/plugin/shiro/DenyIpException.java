package com.jfinal.ext.plugin.shiro;

import org.apache.shiro.authz.UnauthenticatedException;

/**
 * ip禁止访问。
 * 
 * @ClassName: DenyIpException  
 * @author 李飞  
 * @date 2016年5月19日 上午11:50:52
 * @since V1.0.0
 */
public class DenyIpException extends UnauthenticatedException{
	private static final long serialVersionUID = 4293695890007276300L;

	public DenyIpException() {
		super();
	}

	public DenyIpException(String message, Throwable cause) {
		super(message, cause);
	}

	public DenyIpException(String message) {
		super(message);
	}

	public DenyIpException(Throwable cause) {
		super(cause);
	}
	
}
