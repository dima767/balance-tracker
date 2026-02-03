package dk.balancetracker.repository;

import dk.balancetracker.domain.Payee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Payee} entity.
 * <p>
 * Provides standard CRUD operations plus custom query methods for
 * finding payees by name and other criteria.
 * <p>
 * Auto-configured by Spring Boot's JPA auto-configuration.
 *
 * @author Dmitriy Kopylenko
 */
public interface PayeeRepository extends JpaRepository<Payee, Long> {

    /**
     * Finds a payee by its exact name (case-sensitive).
     *
     * @param name the payee name
     * @return optional containing the payee if found
     */
    Optional<Payee> findByName(String name);

    /**
     * Finds a payee by its name ignoring case.
     *
     * @param name the payee name
     * @return optional containing the payee if found
     */
    Optional<Payee> findByNameIgnoreCase(String name);

    /**
     * Checks if a payee with the given name exists (case-insensitive).
     *
     * @param name the payee name
     * @return true if a payee with this name exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Finds all payees ordered by name ascending.
     *
     * @return list of all payees sorted alphabetically
     */
    List<Payee> findAllByOrderByNameAsc();

    /**
     * Finds payees whose names contain the search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of matching payees ordered by name
     */
    List<Payee> findByNameContainingIgnoreCaseOrderByNameAsc(String searchTerm);
}
