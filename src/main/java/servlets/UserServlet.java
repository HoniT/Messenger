package servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.users.SessionRequest;
import contracts.users.UserRequest;
import contracts.users.UserResponse;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import persistence.UserDbService;
import persistence.entities.User;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserDbService _db;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        _db = UserDbService.getInstance();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        switch (pathInfo) {
            case "/register" -> handleRegister(req, resp);
            case "/login" -> handleLogin(req, resp);
            case "/logout" -> handleLogout(req, resp);
            case null, default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        objectMapper.writeValue(resp.getWriter(), _db.getAllUsers());
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserRequest userRequest = objectMapper.readValue(req.getReader(), UserRequest.class);
        // Validating request
        if(userRequest.getUsername() == null || userRequest.getUsername().isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Username is needed");
            return;
        }
        if(userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Password is needed");
            return;
        }

        // Password strength validation
        if(!isValidPassword(userRequest.getPassword())) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Password must be minimum of 8 characters; contain upper and lower letters, number and special characters.");
            return;
        }

        boolean userExists = _db.checkUser(userRequest.getUsername());
        if(userExists) {
            // This is vulnerable to user enumeration, but it's alright for now :)
            resp.sendError(HttpServletResponse.SC_CONFLICT, "User already exists");
            return;
        }

        User user = _db.addUser(userRequest.getUsername(), userRequest.getPassword(), UUID.randomUUID());
        objectMapper.writeValue(resp.getWriter(), new UserResponse(user.getCurrSession()));

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserRequest userRequest = objectMapper.readValue(req.getReader(), UserRequest.class);
        // Validating request
        if(userRequest.getUsername() == null || userRequest.getUsername().isBlank()) {
            resp. sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Username is needed");
            return;
        }
        if(userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Password is needed");
            return;
        }

        // Checking credentials
        User user = _db.getUser(userRequest.getUsername(), userRequest.getPassword());
        if(user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
        }

        // Generating and setting new session UUID
        UUID newUuid = UUID.randomUUID();
        boolean uuidSuccess = _db.setSessionId(user.getId(), newUuid);
        if(!uuidSuccess) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        objectMapper.writeValue(resp.getWriter(), new UserResponse(newUuid));

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionRequest sessionRequest = objectMapper.readValue(req.getReader(), SessionRequest.class);
        // Validating request
        if(sessionRequest.getSessionId() == null) {
            resp. sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Session Id is needed");
            return;
        }

        boolean removeSuccess = _db.removeSessionId(sessionRequest.getSessionId());
        if(!removeSuccess) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()-+=]).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValidPassword(String password) {
        if (password == null) return false;

        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}
