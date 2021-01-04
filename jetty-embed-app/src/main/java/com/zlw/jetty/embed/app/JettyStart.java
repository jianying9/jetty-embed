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
