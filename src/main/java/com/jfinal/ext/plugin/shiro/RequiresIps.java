package com.jfinal.ext.plugin.shiro;

/**  
 * 限制ip地址的权限，需要指定ip地址才可以访问
 * @ClassName: RequireIps  
 * @author 李飞  
 * @date 2016年5月19日 上午11:29:56
 * @since V1.0.0  
 */
public @interface RequiresIps{
	public java.lang.String[] value();
}
