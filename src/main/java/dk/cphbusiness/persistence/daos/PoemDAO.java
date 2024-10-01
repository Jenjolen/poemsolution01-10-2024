package dk.cphbusiness.persistence.daos;


import dk.cphbusiness.exceptions.EntityNotFoundException;
import dk.cphbusiness.persistence.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Purpose: This class is a specific DAO (Data Access Object) that can be used to perform CRUD operations on the Person entity plus some extra queries.
 * @param <T> The entity class that the DAO should be used for.
 * Author: Thomas Hartmann
 */
public class PoemDAO implements IDAO<Poem> {
    private EntityManagerFactory emf;

    public PoemDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Poem findById(Long id) throws EntityNotFoundException {
        try (EntityManager entityManager = emf.createEntityManager()) {
            Poem poem = entityManager.find(Poem.class, id);
            if(poem == null){
                throw new EntityNotFoundException("Poem with id: " + id + " not found");
            }
            return poem;
        }
    }

    @Override
    public Set<Poem> getAll() {
        try (EntityManager entityManager = emf.createEntityManager()) {
            TypedQuery<Poem> query = entityManager.createQuery( "SELECT p FROM Poem p",Poem.class);
            Set<Poem> poems = query.getResultStream().collect(Collectors.toSet());
            poems.forEach(p -> System.out.println(p));
            return poems;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Poem create(Poem poem) {
        try (EntityManager entityManager = emf.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.persist(poem);
            entityManager.getTransaction().commit();
        }
        return poem;
    }

    @Override
    public Poem update(Poem poem) {
        try (EntityManager entityManager = emf.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.merge(poem);
            entityManager.getTransaction().commit();
        }
        return poem;
    }

    @Override
    public void delete(Poem poem) {
        try (EntityManager entityManager = emf.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.remove(poem);
            entityManager.getTransaction().commit();
        }
    }
}