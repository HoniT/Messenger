package util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ErrorUtil {
    public static void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(message);
    }
}
