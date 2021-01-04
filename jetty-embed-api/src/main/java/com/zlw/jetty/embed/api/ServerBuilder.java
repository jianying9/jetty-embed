package com.zlw.jetty.embed.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.Servlet;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * jetty server实例化
 *
 * @author jianying9
 */
public class ServerBuilder
{

    private final String appName;

    private final String appPath;

    private final int port;

    private final Logger logger;

    private final List<EventListener> eventListenerList = new ArrayList();

    private final Map<String, Class<? extends Servlet>> servletMap = new HashMap();

    /**
     * 实例化
     *
     * @param appName 应用名称
     * @param port 监听端口
     */
    public ServerBuilder(String appName, int port)
    {
        this.appName = appName;
        this.port = port;
        //初始化当前应用目录
        String basePath = new File("").getAbsolutePath();
        //如果是maven运行环境则根目录定位到target
        String targetPath = basePath + "/target";
        File targetDir = new File(targetPath);
        if (targetDir.exists()) {
            String buildName = basePath.substring(basePath.lastIndexOf("/") + 1);
            basePath = targetPath + "/" + buildName;
        }
        this.appPath = basePath;
        //设置环境变量,用于jetty的log4j2日志输出
        System.setProperty("AppPath", this.appPath);
        this.logger = Log.getLogger(ServerBuilder.class);
        logger.info("app目录:{}", this.appPath);
    }

    private void initDir()
    {
        //lib   第三方库
        String libPath = this.appPath + "/lib";
        this.checkDir(libPath);
        logger.info("lib目录:{}", libPath);
        //webapps   web服务目录
        String webappPath = this.appPath + "/webapps";
        this.checkDir(webappPath);
        logger.info("web应用目录:{}", webappPath);
        //webapps/logs  日志web应用目录
        String logsWebappPath = webappPath + "/logs";
        this.checkDir(logsWebappPath);
        logger.info("日志web应用目录:{}", logsWebappPath);
        //webapps/appName
        String defaultWebappPath = webappPath + "/" + this.appName;
        this.checkDir(defaultWebappPath);
        logger.info("默认web应用目录:{}", defaultWebappPath);
    }

    private void checkDir(String path)
    {
        File dir = new File(path);
        if (dir.exists() == false) {
            dir.mkdir();
        }
    }

    /**
     * 添加监听
     *
     * @param listener
     * @return
     */
    public ServerBuilder addEventListener(EventListener listener)
    {
        this.eventListenerList.add(listener);
        return this;
    }

    /**
     * 添加servlet
     *
     * @param servlet 类
     * @param pathSpec path
     * @return
     */
    public ServerBuilder addServlet(Class<? extends Servlet> servlet, String pathSpec)
    {
        this.servletMap.put(pathSpec, servlet);
        return this;
    }

    private void checkAndKillPort()
    {
        //获取系统版本
        String osName = System.getProperty("os.name").toLowerCase();
        logger.info("当前系统:{}", osName);
        if (osName.startsWith("linux")) {
            //linux
            checkAndKillPortInLinux();
        } else if (osName.startsWith("mac")) {
            //mac
            checkAndKillPortInMac();
        }
    }

    private void checkAndKillPortInMac()
    {
        try {
            //检查端口
            Process lsofProcess = Runtime.getRuntime().exec("lsof -i:" + Integer.toString(port));
            lsofProcess.waitFor();
            InputStream in = lsofProcess.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String temp = read.readLine();
            String last = "";
            while (temp != null) {
                last = temp;
                temp = read.readLine();
            }
            String[] fieldArray = last.split(" ");
            if (fieldArray.length > 5) {
                logger.warn("端口已被占用:{}", last);
                //第5个为pid
                String pid = fieldArray[4];
                //杀死该进程
                Process killProcess = Runtime.getRuntime().exec("kill -9 " + pid);
                killProcess.waitFor();
                logger.warn("杀死进程:{}", pid);
            }
        } catch (IOException | InterruptedException ex) {
            this.logger.warn("mac端口检测异常", ex);
        }
    }

    private void checkAndKillPortInLinux()
    {
        try {
            //检查端口
            Process netstatProcess = Runtime.getRuntime().exec("netstat -ntlp | grep " + Integer.toString(port));
            netstatProcess.waitFor();
            InputStream in = netstatProcess.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String temp = read.readLine();
            String last = "";
            while (temp != null) {
                last = temp;
                temp = read.readLine();
            }
            String[] fieldArray = last.split(" ");
            logger.warn("debug:netstat length {}", fieldArray.length);
            if (fieldArray.length > 5) {
                logger.warn("端口已被占用:{}", last);
                //第5个为pid
                String pid = fieldArray[4];
                //杀死该进程
//                Process killProcess = Runtime.getRuntime().exec("kill -9 " + pid);
//                killProcess.waitFor();
//                logger.warn("杀死进程:{}", pid);
            }
        } catch (IOException | InterruptedException ex) {
            this.logger.warn("linux端口检测异常", ex);
        }
    }

    public void build()
    {
        //初始化目录
        this.initDir();
        //创建server
        final Server server = new Server();
        //监听端口
        HttpConfiguration config = new HttpConfiguration();
        //http不返回版本信息
        config.setSendServerVersion(false);
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(config);
        ServerConnector connector = new ServerConnector(server, null, null, null, -1, -1, httpConnectionFactory);
        connector.setPort(this.port);
        server.setConnectors(new Connector[]{connector});
        //初始化默认webapp服务
        WebAppContext defaultAppContext = new WebAppContext(this.appPath + "/webapps/" + this.appName, "/" + appName);
        //禁止http目录遍历
        defaultAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        //添加监听
        for (EventListener eventListener : eventListenerList) {
            defaultAppContext.addEventListener(eventListener);
        }
        //添加servlet
        for (Entry<String, Class<? extends Servlet>> entry : this.servletMap.entrySet()) {
            defaultAppContext.addServlet(entry.getValue(), entry.getKey());
        }
        server.setHandler(defaultAppContext);
        //初始化日志文件web服务
        ResourceHandler logResourceHandler = new ResourceHandler();
        logResourceHandler.setEtags(true);
        //允许遍历子目录
        logResourceHandler.setDirectoriesListed(true);
        //设置文件头
        MimeTypes mimeTypes = new MimeTypes();
        logResourceHandler.setMimeTypes(mimeTypes);
        mimeTypes.addMimeMapping("log", "text/plain;charset=utf-8");
        //设置路径
        ContextHandler logContext = new ContextHandler();
        logContext.setContextPath("/logs");
        logContext.setBaseResource(Resource.newResource(new File(this.appPath + "/webapps/logs")));
        logContext.setHandler(logResourceHandler);
        //设置安全验证
        ClassLoader classLoader = ServerBuilder.class.getClassLoader();
        URL realmProps = classLoader.getResource("realm.properties");
        LoginService loginService = new HashLoginService("logRealm",
                realmProps.toExternalForm());
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"admin"});
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);
        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        security.setHandler(logContext);
        //加载web服务
        HandlerList handlers = new HandlerList();
        handlers.addHandler(defaultAppContext);
        handlers.addHandler(security);
        server.setHandler(handlers);
        //准备启动服务
        this.logger.info("端口:{},应用名称:{},准备启动服务...", port, appName);
        //检测端口是否被占用,如果已经被使用则杀死已有服务
        this.checkAndKillPort();
        try {
            //启动服务
            server.start();
        } catch (Exception ex) {
            this.logger.warn("server启动异常", ex);
        }
        //修改http响应头Server信息
//        HttpGenerator.setJettyVersion("Zlw(3.3.3)");
    }

}
