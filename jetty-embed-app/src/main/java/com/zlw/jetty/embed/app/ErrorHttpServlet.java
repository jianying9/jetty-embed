package com.zlw.jetty.embed.app;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 *
 * @author jianying9
 */
@WebServlet(urlPatterns = "/log/error/*")
public final class ErrorHttpServlet extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    public static String state = "no";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // 编码
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");

        // 跨域
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            response.addHeader("Access-Control-Allow-Origin", origin);
        }
        //
        Logger logger = LogManager.getLogger(ErrorHttpServlet.class);
        logger.error("level:{}", "error");
        // 写入客户端
        JSONObject result = new JSONObject();
        result.put("sate", state);
        response.setContentType("text/html;charset=utf-8");
        PrintWriter pw = response.getWriter();
        pw.write(result.toJSONString());
        pw.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        this.doPost(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // 处理跨域
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            response.addHeader("Access-Control-Allow-Origin", origin);
        }
        // 处理header
        String headers = request.getHeader("Access-Control-Request-Headers");
        if (headers != null && !headers.isEmpty()) {
            response.addHeader("Access-Control-Allow-Headers", headers);
        }
    }
}
