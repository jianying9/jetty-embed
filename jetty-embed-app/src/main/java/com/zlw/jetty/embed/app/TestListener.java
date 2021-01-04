package com.zlw.jetty.embed.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet容器监听器，应用程序的执行入口
 *
 * @author Sean
 */
public final class TestListener implements ServletContextListener
{

    @Override
    public void contextInitialized(ServletContextEvent ctxe)
    {
        TestHttpServlet.state = "ok";
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctxe)
    {
    }
}
