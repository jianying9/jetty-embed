package com.zlw.jetty.embed.app;

import com.zlw.jetty.embed.api.ServerBuilder;

/**
 * 内置jetty启动
 *
 * @author jianying9
 */
public class JettyStart
{

    public static void main(String[] args)
    {
        //nohup java -jar -server -XX:MetaspaceSize=32m -XX:MaxMetaspaceSize=32m -Xms128m -Xmx128m -XX:+UseG1GC -XX:ParallelGCThreads=2 jetty-embed-app.jar >/dev/null 2>&1 &
        int port = 8080;
        String appName = "base";
        ServerBuilder serverBuilder = new ServerBuilder(appName, port);
        serverBuilder.addServlet(InfoHttpServlet.class)
                .addServlet(WarnHttpServlet.class)
                .addServlet(ErrorHttpServlet.class)
                .addServlet(DebugHttpServlet.class)
                .addEventListener(new TestListener())
                .build();
    }

}
