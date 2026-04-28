import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import servlets.MessageServlet;
import servlets.UserServlet;

import java.io.File;


public class Main {
    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("server");
        tomcat.setPort(8080);

        // Set context path and document base
        String contextPath = "";
        String docBase = new File("./src/main/webapp").getAbsolutePath();

        // Create context
        Context context = tomcat.addContext(contextPath, new File(docBase).getAbsolutePath());

        // Add DefaultServlet to handle static files (including index.html)
        Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
        context.addServletMappingDecoded("/", "default");

        Tomcat.addServlet(context, "messageServlet", new MessageServlet());
        context.addServletMappingDecoded("/api/messages", "messageServlet");
        context.addServletMappingDecoded("/api/messages/*", "messageServlet");

        Tomcat.addServlet(context, "userServlet", new UserServlet());
        context.addServletMappingDecoded("/api/users", "userServlet");
        context.addServletMappingDecoded("/api/users/*", "userServlet");

        tomcat.start();
        tomcat.getConnector();
        tomcat.getServer().await();
    }
}
