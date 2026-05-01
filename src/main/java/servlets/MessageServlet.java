package servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.messages.MessageDto;
import contracts.messages.SendMessageRequest;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import persistence.MessageDbService;
import persistence.UserDbService;
import persistence.entities.Message;
import persistence.entities.User;
import util.ErrorUtil;
import util.MessageValidator;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MessageServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserDbService _userDb;
    private MessageDbService _messageDb;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        _userDb = UserDbService.getInstance();
        _messageDb = MessageDbService.getInstance();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String usernameParam = req.getParameter("username");

        if (usernameParam != null && !usernameParam.trim().isEmpty()) {
            getMessagesByUser(req, resp, usernameParam);
        } else {
            getMessagedUsers(req, resp);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        sendMessage(req, resp);
    }

    private void getMessagesByUser(HttpServletRequest req, HttpServletResponse resp, String username) throws IOException {
        User user = getAuthenticatedUser(req, resp);
        if (user == null) return;

        List<MessageDto> messages = _messageDb.getMessages(user.getUsername(), username);
        objectMapper.writeValue(resp.getWriter(), messages);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void getMessagedUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req, resp);
        if (user == null) return;

        Set<String> usernames = _messageDb.getMessagedUsers(user.getId());
        objectMapper.writeValue(resp.getWriter(), usernames);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void sendMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SendMessageRequest request = objectMapper.readValue(req.getReader(), SendMessageRequest.class);
        // Validating request
        if(request.getTargetUsername() == null || request.getTargetUsername().isBlank()) {
            ErrorUtil.sendError(resp, HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Target username is required");
            return;
        }

        boolean validMessage = MessageValidator.getInstance().isValid(request.getMessage());
        if(!validMessage) {
            ErrorUtil.sendError(resp, HttpServletResponse.SC_UNPROCESSABLE_CONTENT, MessageValidator.errorMessage);
            return;
        }

        // Get the sender from the session
        User sender = getAuthenticatedUser(req, resp);
        if(sender == null) return;

        try {
            Message message = _messageDb.addMessage(sender.getId(), request.getTargetUsername(), request.getMessage());
            if(message == null) {
                ErrorUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't send message");
                return;
            }
        } catch (Exception e) {
            ErrorUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User '" + request.getTargetUsername() + "' does not exist.");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private User getAuthenticatedUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UUID sessionId = getSessionFromCookie(req);

        if (sessionId == null) {
            ErrorUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid session cookie");
            return null;
        }

        User user = _userDb.getUserBySession(sessionId);
        if (user == null) {
            ErrorUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
            return null;
        }

        return user;
    }

    private UUID getSessionFromCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;

        for (Cookie cookie : req.getCookies()) {
            if ("chat_session".equals(cookie.getName())) {
                try {
                    return UUID.fromString(cookie.getValue());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}