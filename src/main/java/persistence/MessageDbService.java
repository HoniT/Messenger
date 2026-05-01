package persistence;

import contracts.messages.MessageDto;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import persistence.entities.Message;
import persistence.entities.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageDbService extends DbService {
    private static MessageDbService INSTANCE;

    protected MessageDbService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public Set<String> getMessagedUsers(long userId) {
        return runInTransaction(em -> {
            String jpql = "SELECT DISTINCT CASE " +
                    "WHEN m.sender.id = :userId THEN m.recipient.username " +
                    "ELSE m.sender.username END " +
                    "FROM Message m " +
                    "WHERE m.sender.id = :userId OR m.recipient.id = :userId";

            return new HashSet<>(
                    em.createQuery(jpql, String.class)
                            .setParameter("userId", userId)
                            .getResultList()
            );
        });
    }

    public List<MessageDto> getMessages(String username1, String username2) {
        return runInTransaction(em -> {
            String jpql = "SELECT m FROM Message m " +
                    "JOIN FETCH m.sender " +
                    "WHERE (m.sender.username = :user1 AND m.recipient.username = :user2) " +
                    "   OR (m.sender.username = :user2 AND m.recipient.username = :user1) " +
                    "ORDER BY m.sentAt ASC";

            List<Message> messages = em.createQuery(jpql, Message.class)
                    .setParameter("user1", username1)
                    .setParameter("user2", username2)
                    .getResultList();

            return messages.stream().map(m -> {
                MessageDto dto = new MessageDto();
                dto.mapFromMessage(m);
                return dto;
            }).toList();
        });
    }

    public Message addMessage(long senderUserId, String targetUsername, String payload) {
        return runInTransaction(em -> {
            User sender = em.getReference(User.class, senderUserId);
            User recipient = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", targetUsername)
                    .getSingleResult();

            Message newMessage = new Message()
                    .setSender(sender)
                    .setRecipient(recipient)
                    .setPayload(payload);

            em.persist(newMessage);

            return newMessage;
        });
    }

    public static synchronized MessageDbService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessageDbService(Persistence.createEntityManagerFactory("messenger"));
        }
        return INSTANCE;
    }
}
