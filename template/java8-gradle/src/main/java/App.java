import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.openfaas.FunctionService;

public class App {

	static Map<String,FunctionService> functionClasses;
    public static void main(String[] args) throws Exception {
        int port = 8082;
        functionClasses = loadFunctionClasses();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new FunctionHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class FunctionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String requestBody = "";
            String method = t.getRequestMethod();
            if (method.equalsIgnoreCase("POST")) {
		InputStream inputStream = t.getRequestBody();
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
		    result.write(buffer, 0, length);
		}
		// StandardCharsets.UTF_8.name() > JDK 7
		requestBody = result.toString("UTF-8");
	    }
            FunctionService sut = functionClasses.get("1");
            String response = sut.handle(requestBody);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    public static Map<String,FunctionService> loadFunctionClasses() throws Exception
    {
    	Map<String,FunctionService> functionClasses = new HashMap<>();
    	String pathToJar = "libs/com.openfaas-all.jar";
    	JarFile jarFile = new JarFile(pathToJar);
    	Enumeration<JarEntry> e = jarFile.entries();

    	URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
    	URLClassLoader cl = URLClassLoader.newInstance(urls);

    	while (e.hasMoreElements()) {
    	    JarEntry je = e.nextElement();
    	    if(je.isDirectory() || !je.getName().endsWith(".class")){
    	        continue;
    	    }
    	    String className = je.getName().substring(0,je.getName().length()-6);
    	    className = className.replace('/', '.');
    	    if(className.equalsIgnoreCase(FunctionService.class.getName()))
    	    	continue;
    	    Class c = cl.loadClass(className);
    	    if(FunctionService.class.isAssignableFrom(c))
    	    	functionClasses.put("1", (FunctionService)c.newInstance());

    	}
    	jarFile.close();
    	return functionClasses;
    }
}
