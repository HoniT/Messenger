package persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MessageDbService extends DbService {
    private static MessageDbService INSTANCE;

    protected MessageDbService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public static synchronized MessageDbService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessageDbService(Persistence.createEntityManagerFactory("messenger"));
        }
        return INSTANCE;
    }
}
