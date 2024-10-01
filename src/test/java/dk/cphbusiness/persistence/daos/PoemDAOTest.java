package dk.cphbusiness.persistence.daos;

import dk.cphbusiness.exceptions.EntityNotFoundException;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.persistence.entities.Poem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class PoemDAOTest {
    private static EntityManagerFactory emf;
    private static PoemDAO poemDAO;
    Poem poem;
    @BeforeAll
    static void setUpAll() {
        HibernateConfig.setTestMode(true);
        emf = HibernateConfig.getEntityManagerFactory();
        poemDAO = new PoemDAO(emf);
    }

    @BeforeEach
    void setUp() {
        poem = new Poem("Test title", "Test author", "Test content");
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Poem").executeUpdate();
            em.persist(poem);
            em.getTransaction().commit();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @AfterAll
    static void tearDownAll() {
        HibernateConfig.setTestMode(false);
//        emf.close();
    }
    @Test
    @DisplayName("Test if findById throws exception")
    void findByIdThrowException() throws EntityNotFoundException {
        Poem actual = poemDAO.findById(100L);
        assertEquals(poem, actual);
    }

    @Test
    void findById() throws EntityNotFoundException {
        Poem actual = poemDAO.findById(poem.getId());
        assertEquals(poem, actual);
    }

    @Test
    void getAll() {
        int actual = poemDAO.getAll().size();
        int expected = 1;
        assertEquals(expected, actual);
    }

    @Test
    void create() {
    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }
}