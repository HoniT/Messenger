package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.function.Function;

public abstract class DbService {
    private final EntityManagerFactory entityManagerFactory;

    protected DbService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    protected <T> T runInTransaction(Function<EntityManager, T> action) {
        var entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            var result = action.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            entityManager.getTransaction().rollback();
            throw new RuntimeException("Transaction failed", e);
        } finally {
            entityManager.close();
        }
    }
}
