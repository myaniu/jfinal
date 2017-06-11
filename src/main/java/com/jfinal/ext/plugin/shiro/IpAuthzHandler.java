
package com.jfinal.ext.plugin.shiro;

import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletRequest;

/**  
 * ip检查
 * @ClassName: IpAuthzHandler  
 * @author 李飞  
 * @date 2016年5月19日 上午11:36:44
 * @since V1.0.0  
 */
public class IpAuthzHandler extends AbstractAuthzHandler {
	
	private final Annotation annotation;

	public IpAuthzHandler(Annotation annotation) {
		this.annotation = annotation;
	}

	@Override
	public void assertAuthorized(HttpServletRequest req) throws DenyIpException {
		String remoteIp = getRemoteLoginUserIp(req);
		RequiresIps riAnnotation = (RequiresIps) annotation;
        String[] ips = riAnnotation.value();
        for(String ip: ips){
        	//1条匹配即可
        	if(remoteIp.equals(ip)){
        		return;
        	}
        }
        //都不匹配
        throw new DenyIpException("Your ip is :" + remoteIp +", not in whitelist!");
	}
	private  String getRemoteLoginUserIp(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

}
