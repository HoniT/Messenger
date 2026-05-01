package persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.mindrot.jbcrypt.BCrypt;
import persistence.entities.User;

import java.util.List;
import java.util.UUID;

public class UserDbService extends DbService {
    private static UserDbService INSTANCE;

    protected UserDbService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public User getUser(String username, String rawPassword) {
        return runInTransaction(em -> {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<User> query = builder.createQuery(User.class);
            Root<User> root = query.from(User.class);

            Predicate usernamePredicate = builder.equal(root.get("username"), username);
            query.select(root).where(usernamePredicate);

            User user = em.createQuery(query)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (user != null && BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
                return user; // Credentials are correct
            }

            return null; // Invalid username OR password
        });
    }

    public User getUserBySession(UUID oldSessionId) {
        return runInTransaction(em -> em.createQuery("SELECT u FROM User u WHERE u.currSession = :sessionId", User.class)
                    .setParameter("sessionId", oldSessionId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null));
    }

    public boolean checkUser(String username) {
        return runInTransaction(em -> {
            Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return count > 0;
        });
    }

    public User addUser(String username, String rawPassword, UUID sessionId) {
        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        return runInTransaction(em -> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPasswordHash(passwordHash);
            newUser.setCurrSession(sessionId);

            em.persist(newUser);
            return newUser;
        });
    }

    public boolean setSessionId(Long userId, UUID newSessionId) {
        return runInTransaction(em -> {
            User user = em.find(User.class, userId);
            if (user == null) return false;

            user.setCurrSession(newSessionId);

            return true;
        });
    }

    public boolean removeSessionId(UUID oldSessionId) {
        return runInTransaction(em -> {
            User user = em.createQuery("SELECT u FROM User u WHERE u.currSession = :sessionId", User.class)
                    .setParameter("sessionId", oldSessionId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (user == null) return false;

            // Clear the session by setting it to null
            user.setCurrSession(null);

            return true;
        });
    }

    /// Gets all users. Only for testing
    public List<User> getAllUsers() {
        return runInTransaction(em ->
            em.createQuery("SELECT u FROM User u", User.class)
                    .getResultList()
        );
    }

    public static synchronized UserDbService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserDbService(Persistence.createEntityManagerFactory("messenger"));
        }
        return INSTANCE;
    }
}
