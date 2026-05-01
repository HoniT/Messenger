package servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.SessionRequest;
import contracts.messages.MessageDto;
import contracts.messages.SendMessageRequest;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import persistence.MessageDbService;
import persistence.UserDbService;
import persistence.entities.Message;
import persistence.entities.User;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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
        SessionRequest request = objectMapper.readValue(req.getReader(), SessionRequest.class);
        // Validating request
        if(request.getSessionId() == null) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Session id is required");
            return;
        }

        User user = _userDb.getUserBySession(request.getSessionId());
        if(user == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        List<MessageDto> messages = _messageDb.getMessages(user.getUsername(), username);
        objectMapper.writeValue(resp.getWriter(), messages);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void getMessagedUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionRequest request = objectMapper.readValue(req.getReader(), SessionRequest.class);
        // Validating request
        if(request.getSessionId() == null) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Session id is required");
            return;
        }

        User user = _userDb.getUserBySession(request.getSessionId());
        if(user == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        Set<String> usernames = _messageDb.getMessagedUsers(user.getId());
        objectMapper.writeValue(resp.getWriter(), usernames);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
    }

    private void sendMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SendMessageRequest request = objectMapper.readValue(req.getReader(), SendMessageRequest.class);
        // Validating request
        if(request.getSenderSessionId() == null) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Sender session id is required");
            return;
        }
        if(request.getTargetUsername() == null || request.getTargetUsername().isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Target username is required");
            return;
        }
        if(request.getMessage() == null) {
            resp.sendError(HttpServletResponse.SC_UNPROCESSABLE_CONTENT, "Message is required");
            return;
        }

        // Get the sender from the session
        User sender = _userDb.getUserBySession(request.getSenderSessionId());
        if(sender == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Sender not found");
            return;
        }

        Message message = _messageDb.addMessage(sender.getId(), request.getTargetUsername(), request.getMessage());
        if(message == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't send message");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
