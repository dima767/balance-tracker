package dk.balancetracker.web;

import dk.balancetracker.domain.Payee;
import dk.balancetracker.service.PayeeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing payee reference data.
 * <p>
 * Provides both traditional MVC endpoints for settings page management
 * and API endpoints for HTMX/AJAX interactions.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
public class PayeeController {

    private final PayeeService payeeService;

    public PayeeController(PayeeService payeeService) {
        this.payeeService = payeeService;
    }

    // ==================== Settings Page Endpoints ====================

    /**
     * Shows the settings page with payee management.
     * GET /settings
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        List<Payee> payees = payeeService.findAll();
        model.addAttribute("payees", payees);
        return "settings/index";
    }

    /**
     * Creates a new payee.
     * POST /settings/payees
     */
    @PostMapping("/settings/payees")
    public String createPayee(
        @RequestParam("name") String name,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        Model model,
        HttpServletResponse response
    ) {
        try {
            payeeService.createPayee(name);

            if ("true".equals(hxRequest)) {
                // Return updated payees list for HTMX
                List<Payee> payees = payeeService.findAll();
                model.addAttribute("payees", payees);
                response.setHeader("HX-Trigger", "payeeCreated");
                return "settings/fragments/payees-table :: payees-table";
            }

            return "redirect:/settings";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<Payee> payees = payeeService.findAll();
            model.addAttribute("payees", payees);
            return "settings/index";
        }
    }

    /**
     * Updates an existing payee.
     * PUT /settings/payees/{id}
     */
    @PutMapping("/settings/payees/{id}")
    public String updatePayee(
        @PathVariable Long id,
        @RequestParam("name") String name,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        Model model,
        HttpServletResponse response
    ) {
        try {
            payeeService.updatePayee(id, name);

            if ("true".equals(hxRequest)) {
                // Return updated payees list for HTMX
                List<Payee> payees = payeeService.findAll();
                model.addAttribute("payees", payees);
                response.setHeader("HX-Trigger", "payeeUpdated");
                return "settings/fragments/payees-table :: payees-table";
            }

            return "redirect:/settings";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<Payee> payees = payeeService.findAll();
            model.addAttribute("payees", payees);
            return "settings/index";
        }
    }

    /**
     * Deletes a payee.
     * DELETE /settings/payees/{id}
     */
    @DeleteMapping("/settings/payees/{id}")
    public String deletePayee(
        @PathVariable Long id,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        Model model,
        HttpServletResponse response
    ) {
        try {
            payeeService.deletePayee(id);

            if ("true".equals(hxRequest)) {
                // Return updated payees list for HTMX
                List<Payee> payees = payeeService.findAll();
                model.addAttribute("payees", payees);
                response.setHeader("HX-Trigger", "payeeDeleted");
                return "settings/fragments/payees-table :: payees-table";
            }

            return "redirect:/settings";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Cannot delete payee: it is referenced by existing payment items");
            List<Payee> payees = payeeService.findAll();
            model.addAttribute("payees", payees);

            if ("true".equals(hxRequest)) {
                return "settings/fragments/payees-table :: payees-table";
            }
            return "settings/index";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<Payee> payees = payeeService.findAll();
            model.addAttribute("payees", payees);

            if ("true".equals(hxRequest)) {
                return "settings/fragments/payees-table :: payees-table";
            }
            return "settings/index";
        }
    }

    // ==================== API Endpoints for Forms ====================

    /**
     * Returns all payees as JSON for dropdown population.
     * GET /api/payees
     */
    @GetMapping("/api/payees")
    @ResponseBody
    public List<Map<String, Object>> getAllPayees() {
        return payeeService.findAll().stream()
            .map(payee -> Map.of(
                "id", (Object) payee.getId(),
                "name", payee.getName()
            ))
            .toList();
    }

    /**
     * Searches payees by name (case-insensitive).
     * GET /api/payees/search?q={query}
     */
    @GetMapping("/api/payees/search")
    @ResponseBody
    public List<Map<String, Object>> searchPayees(@RequestParam("q") String query) {
        return payeeService.searchByName(query).stream()
            .map(payee -> Map.of(
                "id", (Object) payee.getId(),
                "name", payee.getName()
            ))
            .toList();
    }
}
