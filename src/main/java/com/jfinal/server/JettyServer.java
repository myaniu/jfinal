/**
 * Copyright (c) 2011-2017, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jfinal.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import com.jfinal.core.Const;
import com.jfinal.kit.FileKit;
import com.jfinal.kit.LogKit;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.StrKit;

/**
 * JettyServer is used to config and start jetty web server.
 * Jetty version 8.1.8
 */
class JettyServer implements IServer {
	
	private String webAppDir;
	private int port;
	private String host="127.0.0.1";
	private String context;
	private int scanIntervalSeconds;
	private boolean running = false;
	private Server server;
	private WebAppContext webApp;
	
	private String keyStorePath = null;
	private String keyStorePassword = null;
	private String keyManagerPassword = null;
	
	JettyServer(String webAppDir,String host, int port, String context, int scanIntervalSeconds) {
		if (webAppDir == null)
			throw new IllegalStateException("Invalid webAppDir of web server: " + webAppDir);
		if (port < 0 || port > 65536)
			throw new IllegalArgumentException("Invalid port of web server: " + port);
		if (StrKit.isBlank(context))
			throw new IllegalStateException("Invalid context of web server: " + context);
		
		this.webAppDir = webAppDir;
		this.port = port;
		this.host = host;
		this.context = context;
		this.scanIntervalSeconds = scanIntervalSeconds;
	}
	
	JettyServer(String webAppDir, String host,int port, String context, String keyStorePath,String keyStorePassword,String keyManagerPassword) {
		this(webAppDir,host,port,context,0);
		this.keyManagerPassword = keyManagerPassword;
		this.keyStorePassword = keyStorePassword;
		this.host = host;
		this.keyStorePath = keyStorePath;
	}
	
	public void start() {
		if (!running) {
			try {doStart();} catch (Exception e) {LogKit.error(e.getMessage(), e);}
			running = true;
		}
	}
	
	public void stop() {
		if (running) {
			try {webApp.stop();server.stop();} catch (Exception e) {e.printStackTrace();}
			running = false;
		}
	}
	
	private void doStart() {
		if (!available(port))
			throw new IllegalStateException("port: " + port + " already in use!");
		
		deleteSessionData();
		
		System.out.println("Starting JFinal " + Const.JFINAL_VERSION);
		server = new Server();
		//httl配置。
		if(null == this.keyStorePath){
			HttpConfiguration http_config = new HttpConfiguration();	        // HTTP connector
	        ServerConnector connector = new ServerConnector(server,new HttpConnectionFactory(http_config));
			connector.setReuseAddress(true);
			connector.setIdleTimeout(30000);
			connector.setPort(port);
			connector.setHost(host);
			server.addConnector(connector);
		}else{
			//https 配置
			HttpConfiguration https_config = new HttpConfiguration();
			https_config.setSecureScheme("https");
			https_config.setSecurePort(port);
			https_config.setOutputBufferSize(32768);
			https_config.addCustomizer(new SecureRequestCustomizer());
			SslContextFactory sslContextFactory = new SslContextFactory();
	        sslContextFactory.setKeyStorePath(this.keyStorePath);
	        sslContextFactory.setKeyStorePassword(this.keyStorePassword);
	        sslContextFactory.setKeyManagerPassword(this.keyManagerPassword);
	        ServerConnector httpsConnector = new ServerConnector(server,
	                new SslConnectionFactory(sslContextFactory,"http/1.1"),
	                new HttpConnectionFactory(https_config));
	        httpsConnector.setPort(port);
	        httpsConnector.setHost(host);
	        httpsConnector.setIdleTimeout(500000);
	        server.addConnector(httpsConnector);
		}
		
		webApp = new WebAppContext();		
		
		webApp.setContextPath(context);
		webApp.setResourceBase(webAppDir);
		webApp.setMaxFormContentSize(81920000);
		webApp.getInitParams().put("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		webApp.getInitParams().put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "true");
		webApp.getInitParams().put("org.eclipse.jetty.server.Request.maxFormContentSize", "-1");
		
		persistSession(webApp);
		
		server.setHandler(webApp);
		changeClassLoader(webApp);
		
		// configureScanner
		if (scanIntervalSeconds > 0) {
			Scanner scanner = new Scanner(PathKit.getRootClassPath(), scanIntervalSeconds) {
				public void onChange() {
					try {
						System.err.println("\nLoading changes ......");
						webApp.stop();
						JFinalClassLoader loader = new JFinalClassLoader(webApp, getClassPath());
						webApp.setClassLoader(loader);
						webApp.start();
						System.err.println("Loading complete.");
					} catch (Exception e) {
						System.err.println("Error reconfiguring/restarting webapp after change in watched files");
						LogKit.error(e.getMessage(), e);
					}
				}
			};
			System.out.println("Starting scanner at interval of " + scanIntervalSeconds + " seconds.");
			scanner.start();
		}
		
		try {
			System.out.println("Starting web server on port: " + port);
			server.start();
			System.out.println("Starting Complete. Welcome To The JFinal World (仨多ta爹特别版) :)");
			server.join();
		} catch (Exception e) {
			LogKit.error(e.getMessage(), e);
			System.exit(100);
		}
		return;
	}

	private void changeClassLoader(WebAppContext webApp) {
		try {
			String classPath = getClassPath();
			JFinalClassLoader wacl = new JFinalClassLoader(webApp, classPath);
			webApp.setClassLoader(wacl);
		} catch (IOException e) {
			LogKit.error(e.getMessage(), e);
		}
	}
	private String getClassPath() {
		return System.getProperty("java.class.path");
	}
	
	private void deleteSessionData() {
		try {
			FileKit.delete(new File(getStoreDir()));
		}
		catch (Exception e) {
			LogKit.logNothing(e);
		}
	}
	
	private String getStoreDir() {
		String storeDir = PathKit.getWebRootPath() + "/../../session_data" + context;
		if ("\\".equals(File.separator))
			storeDir = storeDir.replaceAll("/", "\\\\");
		return storeDir;
	}
	
	private void persistSession(WebAppContext webApp) {
		String storeDir = getStoreDir();
		
		SessionManager sm = webApp.getSessionHandler().getSessionManager();
		if (sm instanceof HashSessionManager) {
			try {
				((HashSessionManager)sm).setStoreDirectory(new File(storeDir));
			} catch (IOException e) {
				LogKit.logNothing(e);
			}
			return ;
		}
		
		HashSessionManager hsm = new HashSessionManager();
		try {
			hsm.setStoreDirectory(new File(storeDir));
		} catch (IOException e) {
			LogKit.logNothing(e);
		}
		SessionHandler sh = new SessionHandler();
		sh.setSessionManager(hsm);
		webApp.setSessionHandler(sh);
	}
	
	private static boolean available(int port) {
		if (port <= 0) {
			throw new IllegalArgumentException("Invalid start port: " + port);
		}
		
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
			LogKit.logNothing(e);
		} finally {
			if (ds != null) {
				ds.close();
			}
			
			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					// should not be thrown, just detect port available.
					LogKit.logNothing(e);
				}
			}
		}
		return false;
	}
}






