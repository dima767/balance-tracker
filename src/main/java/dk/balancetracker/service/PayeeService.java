package dk.balancetracker.service;

import dk.balancetracker.domain.Payee;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing payee reference data.
 *
 * @author Dmitriy Kopylenko
 */
public interface PayeeService {

    /**
     * Creates a new payee.
     *
     * @param name the payee name
     * @return the created payee
     * @throws IllegalArgumentException if name is invalid or already exists
     */
    Payee createPayee(String name);

    /**
     * Finds or creates a payee by name (case-insensitive).
     * If a payee with the given name exists, returns it. Otherwise creates a new one.
     *
     * @param name the payee name
     * @return the existing or newly created payee
     * @throws IllegalArgumentException if name is invalid
     */
    Payee findOrCreatePayee(String name);

    /**
     * Finds a payee by ID.
     *
     * @param id the payee ID
     * @return optional containing the payee if found
     */
    Optional<Payee> findById(Long id);

    /**
     * Finds a payee by name (case-insensitive).
     *
     * @param name the payee name
     * @return optional containing the payee if found
     */
    Optional<Payee> findByName(String name);

    /**
     * Finds all payees ordered by name.
     *
     * @return list of all payees sorted alphabetically
     */
    List<Payee> findAll();

    /**
     * Searches for payees by name (case-insensitive partial match).
     *
     * @param searchTerm the search term
     * @return list of matching payees
     */
    List<Payee> searchByName(String searchTerm);

    /**
     * Updates a payee's name.
     *
     * @param id the payee ID
     * @param newName the new name
     * @return the updated payee
     * @throws IllegalArgumentException if payee not found or new name already exists
     */
    Payee updatePayee(Long id, String newName);

    /**
     * Deletes a payee.
     * Note: This will fail if the payee is referenced by any payment items.
     *
     * @param id the payee ID
     * @throws IllegalArgumentException if payee not found
     */
    void deletePayee(Long id);

    /**
     * Checks if a payee with the given name exists (case-insensitive).
     *
     * @param name the payee name
     * @return true if a payee with this name exists
     */
    boolean existsByName(String name);
}
