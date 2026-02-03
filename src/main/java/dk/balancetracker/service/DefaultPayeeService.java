package dk.balancetracker.service;

import dk.balancetracker.domain.Payee;
import dk.balancetracker.repository.PayeeRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PayeeService}.
 * <p>
 * Manages payee reference data with validation and uniqueness checks.
 * <p>
 * Configured as a bean in {@link dk.balancetracker.config.AppAutoConfiguration.ServiceConfig}.
 *
 * @author Dmitriy Kopylenko
 */
@Transactional
public class DefaultPayeeService implements PayeeService {

    private final PayeeRepository payeeRepository;

    public DefaultPayeeService(PayeeRepository payeeRepository) {
        this.payeeRepository = payeeRepository;
    }

    @Override
    public Payee createPayee(String name) {
        validatePayeeName(name);

        if (existsByName(name)) {
            throw new IllegalArgumentException("Payee already exists with name: " + name);
        }

        Payee payee = new Payee(name.trim());
        return payeeRepository.save(payee);
    }

    @Override
    public Payee findOrCreatePayee(String name) {
        validatePayeeName(name);

        String trimmedName = name.trim();

        // Try to find existing payee (case-insensitive)
        Optional<Payee> existing = payeeRepository.findByNameIgnoreCase(trimmedName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new payee
        Payee payee = new Payee(trimmedName);
        return payeeRepository.save(payee);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payee> findById(Long id) {
        return payeeRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payee> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return payeeRepository.findByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payee> findAll() {
        return payeeRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payee> searchByName(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findAll();
        }
        return payeeRepository.findByNameContainingIgnoreCaseOrderByNameAsc(searchTerm.trim());
    }

    @Override
    public Payee updatePayee(Long id, String newName) {
        validatePayeeName(newName);

        Payee payee = payeeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + id));

        String trimmedName = newName.trim();

        // Check if new name is different (case-insensitive)
        if (payee.getName().equalsIgnoreCase(trimmedName)) {
            // Name hasn't changed (ignoring case), just update the exact case
            payee.setName(trimmedName);
            return payeeRepository.save(payee);
        }

        // Check if another payee already has this name
        if (existsByName(trimmedName)) {
            throw new IllegalArgumentException("Payee already exists with name: " + trimmedName);
        }

        payee.setName(trimmedName);
        return payeeRepository.save(payee);
    }

    @Override
    public void deletePayee(Long id) {
        if (!payeeRepository.existsById(id)) {
            throw new IllegalArgumentException("Payee not found: " + id);
        }

        // Let the database constraint handle the foreign key check
        // If payee is referenced by payment items, a DataIntegrityViolationException will be thrown
        payeeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return payeeRepository.existsByNameIgnoreCase(name.trim());
    }

    private void validatePayeeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Payee name cannot be blank");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Payee name must not exceed 200 characters");
        }
    }
}
