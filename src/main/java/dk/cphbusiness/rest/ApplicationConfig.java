package dk.cphbusiness.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.bugelhartmann.UserDTO;
import dk.cphbusiness.exceptions.ApiException;
import dk.cphbusiness.exceptions.EntityNotFoundException;
import dk.cphbusiness.security.SecurityController;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.path;

import dk.cphbusiness.security.SecurityRoutes.Role;
//import io.javalin.plugin.bundled.

/**
 * Purpose: To configure the Javalin server
 * Author: Thomas Hartmann
 */
public class ApplicationConfig {
    private ObjectMapper objectMapper = new ObjectMapper();
    private static ApplicationConfig appConfig;
    private static JavalinConfig javalinConfig;
    private static Javalin app;

    private ApplicationConfig() {
    }

    public static ApplicationConfig getInstance() {
        if (appConfig == null) {
            appConfig = new ApplicationConfig();
        }
        return appConfig;
    }

    public ApplicationConfig initiateServer() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        String separator = System.getProperty("file.separator");
        app = Javalin.create(config -> {
            javalinConfig = config;
            config.bundledPlugins.enableDevLogging(); // enables extensive development logging in terminal
            config.staticFiles.add("/public"); // enables serving of static files from the public folder in the classpath. PROs: easy to use, CONs: you have to restart the server every time you change a file
            config.http.defaultContentType = "application/json"; // default content type for requests
            config.router.contextPath = "/api"; // base path for all routes
            config.bundledPlugins.enableRouteOverview("/routes", Role.ANYONE); // html overview of all registered routes at /routes for api documentation: https://javalin.io/news/2019/08/11/javalin-3.4.1-released.html
        });
        return appConfig;
    }

    public ApplicationConfig setRoutes(EndpointGroup routes) {
        javalinConfig.router.apiBuilder(() -> {
            path("/", routes);
        });

//        appConfig.setRoutes(() -> {
//            path("/", routes); // e.g. /person
//        });
        return appConfig;
    }


    public ApplicationConfig setCORS() {
        app.before(ctx -> {
            setCorsHeaders(ctx);
        });
        app.options("/*", ctx -> { // Burde nok ikke være nødvendig?
            setCorsHeaders(ctx);
        });
        return appConfig;
    }

    private static void setCorsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true");
    }

    // Adding below methods to ApplicationConfig, means that EVERY ROUTE will be checked for security roles. So open routes must have a role of ANYONE
    public ApplicationConfig checkSecurityRoles() {
        app.beforeMatched(ctx -> { // Before matched is different from before, in that it is not called for 404 etc.
            if (ctx.routeRoles().isEmpty())
                return;
            // 1. Get permitted roles
            Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
            if (allowedRoles.contains("ANYONE")) {
                return;
            }
            // 2. Get user roles
            UserDTO user = ctx.attribute("user");

            // 3. Compare
            if (user == null)
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(objectMapper.createObjectNode()
                                .put("msg", "Not authorized. No username were added from the token"));

            if (!SecurityController.getInstance().authorize(user, allowedRoles)) {
                System.out.println("USER: " + user + " is not authorized. Needed roles are: " + allowedRoles);
                // throw new UnAuthorizedResponse(); // version 6 migration guide
                throw new ApiException(HttpStatus.FORBIDDEN.getCode(), "Unauthorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
            }
        });


        // Check roles on the user (ctx.attribute("username") and compare with permittedRoles using securityController.authorize()
//        app.updateConfig(config -> {

//            config.accessManager((handler, ctx, permittedRoles) -> {
        // permitted roles are defined in the last arg to routes: get("/", ctx -> ctx.result("Hello World"), Role.ANYONE);

//                Set<String> allowedRoles = permittedRoles.stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
//                if (allowedRoles.contains("ANYONE") || ctx.method().toString().equals("OPTIONS")) {
//                    // Allow requests from anyone and OPTIONS requests (preflight in CORS)
//                    handler.handle(ctx);
//                    return;
//                }
//
//                UserDTO user = ctx.attribute("user");
//                System.out.println("USER IN CHECK_SEC_ROLES: " + user);
//                if (user == null)
//                    ctx.status(HttpStatus.FORBIDDEN)
//                            .json(jsonMapper.createObjectNode()
//                                    .put("msg", "Not authorized. No username were added from the token"));
//
//                if (SecurityController.getInstance().authorize(user, allowedRoles))
//                    handler.handle(ctx);
//                else
//                    throw new ApiException(HttpStatus.FORBIDDEN.getCode(), "Unauthorized with roles: " + allowedRoles);
//            });
//        });
        return appConfig;
    }


    public ApplicationConfig startServer(int port) {
        app.start(port);

        return appConfig;
    }

    public ApplicationConfig stopServer() {
        app.stop();
        return appConfig;
    }
//    public static int getPort() {
//        return Integer.parseInt(Utils.getPomProp("javalin.port"));
//    }

    public ApplicationConfig setErrorHandling() {
        // To use this one, just set ctx.status(404) in the controller and add a ctx.attribute("msg", "Your message") to the ctx
        // Look at the PersonController: delete() method for an example
        // Might be better to just use the setApiExceptionHandling method below
        app.error(404, ctx -> {
            String message = ctx.attribute("msg");
            message = "{\"msg\": \"" + message + "\"}";
            ctx.json(message);
        });
        return appConfig;
    }

    public ApplicationConfig setApi404ExceptionHandling() {
        app.exception(EntityNotFoundException.class, (e, ctx) -> {
            ctx.status(404);
            ObjectNode errorMsg = objectMapper.createObjectNode();
            ctx.json(errorMsg.put("msg", e.getMessage()));

        });
        return appConfig;

    }



//    public ApplicationConfig setApiExceptionHandling() {
//        // Might be overruled by the setErrorHandling method
//        app.exception(ApiException.class, (e, ctx) -> {
//            int statusCode = e.getStatusCode();
//            System.out.println("Status code: " + statusCode + ", Message: " + e.getMessage());
//            var on = jsonMapper
//                    .createObjectNode()
//                    .put("status", statusCode)
//                    .put("msg", e.getMessage());
//            ctx.json(on);
//            ctx.status(statusCode);
//        });
//        return appConfig;
//    }

    public ApplicationConfig setGeneralExceptionHandling() {
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.result(e.getMessage());
        });
        return appConfig;
    }

    //    public ApplicationConfig setStaticFiles(String dirPath){
//        app.updateConfig(config -> {
//            config.staticFiles.add("/public", Location.EXTERNAL);
//        });
//        return appConfig;
//    }
    public ApplicationConfig beforeFilter() {
        app.before(ctx -> {
            String pathInfo = ctx.req().getPathInfo();
            ctx.req().getHeaderNames().asIterator().forEachRemaining(el -> System.out.println(el));
        });
        return appConfig;
    }
}
