package dk.cphbusiness.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.cphbusiness.persistence.daos.IDAO;
import dk.cphbusiness.persistence.daos.PoemDAO;
import dk.cphbusiness.dtos.PoemDTO;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.exceptions.ApiException;
import dk.cphbusiness.persistence.entities.Poem;
import dk.cphbusiness.rest.Populator;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Purpose: To demonstrate rest api with Javalin and a database.
 * Author: Thomas Hartmann
 */
public class PoemController implements IController {

    private static PoemController instance;
    private static IDAO<Poem> poemDAO;

    private PoemController() { }

    public static PoemController getInstance(EntityManagerFactory _emf) { // Singleton because we don't want multiple instances of the same class
        if (instance == null) {
            instance = new PoemController();
        }
        // Everytime we request an instance, we get a new EMF, so we can get the proper EMF for test or prod
        poemDAO = new PoemDAO(_emf);
        return instance;
    }


    @Override
    public Handler getAll() {
        return ctx -> {
            Set<PoemDTO> poems = poemDAO
                    .getAll()
                    .stream()
                    .map(PoemDTO::new)
                    .collect(Collectors.toSet());
            if(poems.size()==0)
                throw new ApiException(404, "No poems found");
            ctx.status(HttpStatus.OK).json(poems);
        };
    }

    @Override
    public Handler getById() {
        return ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            Poem p = poemDAO.findById(id);
//            if (p == null){
//                ctx.attribute("msg", "No person with that id");
//                throw new ApiException(404, "No person with that id");
//            }
            ctx.status(HttpStatus.OK).json(new PoemDTO(p));
        };
    }

    @Override
    public Handler create() {
        return ctx -> {
//            BodyValidator<PoemDTO> validator = ctx.bodyValidator(PoemDTO.class);
//            validator.check(person -> person.getAge() > 0 && person.getAge() < 120, "Age must be greater than 0 and less than 120");
            PoemDTO poem = ctx.bodyAsClass(PoemDTO.class);
            Poem created = poemDAO.create(poem.toEntity());
            ctx.json(new PoemDTO(created)).status(HttpStatus.CREATED);
        };
    }

    @Override
    public Handler update() {
        return ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            PoemDTO poem = ctx.bodyAsClass(PoemDTO.class);
            poem.setId(id);
            poemDAO.update(poem.toEntity());
            ctx.json(poem);
        };
    }

    @Override
    public Handler delete() {
        return ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            Poem found = poemDAO.findById(id);
            if(found == null)
                throw new ApiException(404, "No person with that id");
            PoemDTO person = new PoemDTO(found);
            poemDAO.delete(person.toEntity());
            ctx.json(person);
        };
    }

    public Handler resetData(){
        return ctx -> {
            new Populator().createUsersAndRoles(HibernateConfig.getEntityManagerFactory());
            new Populator().createPoemEntities(HibernateConfig.getEntityManagerFactory());
            ctx.json(new ObjectMapper().createObjectNode().put("message", "Data resat"));
        };
    }
}
