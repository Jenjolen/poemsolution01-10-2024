package dk.cphbusiness.persistence.daos;

import dk.cphbusiness.exceptions.EntityNotFoundException;
import dk.cphbusiness.persistence.entities.IJPAEntity;

import java.util.Set;

/**
 * Purpose: This is an interface for making a DAO (Data Access Object) that can be used to perform CRUD operations on any entity.
 * Author: Thomas Hartmann
 * @param <T>
 */
public interface IDAO<T extends IJPAEntity> {
    T findById(Long id) throws EntityNotFoundException;
    Set<T> getAll();
    T create(T t);
    T update(T t);
    void delete(T t);
}