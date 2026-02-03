package dk.balancetracker.web;

import dk.balancetracker.domain.Payee;
import dk.balancetracker.domain.PaymentPeriod;
import dk.balancetracker.domain.PaymentItem;
import dk.balancetracker.service.PayeeService;
import dk.balancetracker.service.PaymentPeriodService;
import dk.balancetracker.service.DefaultPaymentPeriodService;
import jakarta.servlet.http.HttpServletResponse;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing payment periods and payment items.
 * <p>
 * Includes traditional MVC endpoints and HTMX-powered endpoints for
 * dynamic interactions without full page reloads.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
@RequestMapping("/payment-periods")
public class PaymentPeriodController {

    private static final Logger log = LoggerFactory.getLogger(PaymentPeriodController.class);

    private final PaymentPeriodService paymentPeriodService;
    private final PayeeService payeeService;

    public PaymentPeriodController(
        PaymentPeriodService paymentPeriodService,
        PayeeService payeeService
    ) {
        this.paymentPeriodService = paymentPeriodService;
        this.payeeService = payeeService;
    }

    // ==================== Traditional MVC Endpoints ====================

    /**
     * Lists all payment periods.
     * GET /payment-periods
     */
    @GetMapping
    public String list(Model model) {
        List<PaymentPeriod> paymentPeriods = paymentPeriodService.findAllWithPaymentItems();
        model.addAttribute("paymentPeriods", paymentPeriods);
        return "payment-periods/list";
    }

    /**
     * Shows details of a specific payment period with all payment items.
     * GET /payment-periods/{id}
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);
        return "payment-periods/view";
    }

    /**
     * Shows the form to create a new payment period.
     * GET /payment-periods/new
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        // For new form, we don't need to pass a PaymentPeriod object
        model.addAttribute("isNew", true);

        // Create simple DTOs for payees (avoid LazyInitializationException)
        List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
            .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
            .toList();
        model.addAttribute("payees", payeeDtos);

        return "payment-periods/form";
    }

    /**
     * Creates a new payment period with optional payment items.
     * POST /payment-periods
     */
    @PostMapping
    public String create(
        @RequestParam("periodDate") LocalDate periodDate,
        @RequestParam("startingBalance") BigDecimal startingBalance,
        @RequestParam(value = "currency", defaultValue = "USD") String currency,
        @RequestParam(value = "payeeId", required = false) Long[] payeeIds,
        @RequestParam(value = "payeeName", required = false) String[] payeeNames,
        @RequestParam(value = "amount", required = false) BigDecimal[] amounts,
        @RequestParam(value = "notes", required = false) String[] notesArray,
        Model model
    ) {
        log.info("Creating payment period for date: {}", periodDate);
        log.debug("Create payment period - Starting Balance: {}, Currency: {}", startingBalance, currency);
        log.debug("Create payment period - Payee IDs: {}", payeeIds != null ? java.util.Arrays.toString(payeeIds) : "null");
        log.debug("Create payment period - Payee Names: {}", payeeNames != null ? java.util.Arrays.toString(payeeNames) : "null");
        log.debug("Create payment period - Amounts: {}", amounts != null ? java.util.Arrays.toString(amounts) : "null");
        log.debug("Create payment period - Notes: {}", notesArray != null ? java.util.Arrays.toString(notesArray) : "null");

        MonetaryAmount monetaryAmount = Money.of(startingBalance, currency);

        // Build payment items list from arrays
        List<DefaultPaymentPeriodService.PaymentItemData> paymentItems = new ArrayList<>();
        if (amounts != null) {
            for (int i = 0; i < amounts.length; i++) {
                if (amounts[i] != null) {
                    MonetaryAmount itemAmount = Money.of(amounts[i], currency);
                    Long payeeId = (payeeIds != null && i < payeeIds.length) ? payeeIds[i] : null;
                    String payeeName = (payeeNames != null && i < payeeNames.length) ? payeeNames[i] : null;
                    String notes = (notesArray != null && i < notesArray.length) ? notesArray[i] : null;

                    // Skip if no payee information provided
                    if (payeeId != null || (payeeName != null && !payeeName.isBlank())) {
                        paymentItems.add(new DefaultPaymentPeriodService.PaymentItemData(
                            itemAmount,
                            payeeId,
                            payeeName,
                            notes
                        ));
                    }
                }
            }
        }

        try {
            PaymentPeriod paymentPeriod;
            if (paymentItems.isEmpty()) {
                // No payment items - use existing method
                paymentPeriod = paymentPeriodService.createPaymentPeriod(periodDate, monetaryAmount);
            } else {
                // With payment items - use new method
                paymentPeriod = paymentPeriodService.createPaymentPeriodWithPaymentItems(
                    periodDate,
                    monetaryAmount,
                    paymentItems
                );
            }

            return "redirect:/payment-periods/" + paymentPeriod.getId();
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., duplicate date)
            log.warn("Validation error creating payment period: {}", e.getMessage());

            model.addAttribute("error", e.getMessage());
            model.addAttribute("isNew", true);

            // Preserve user's input by creating a transient PaymentPeriod object
            PaymentPeriod tempPeriod = new PaymentPeriod(periodDate, monetaryAmount);

            // Rebuild payment items from submitted data
            for (DefaultPaymentPeriodService.PaymentItemData itemData : paymentItems) {
                Payee payee;
                if (itemData.payeeId() != null) {
                    payee = payeeService.findById(itemData.payeeId()).orElse(new Payee(itemData.payeeName()));
                } else {
                    payee = new Payee(itemData.payeeName());
                }
                PaymentItem item = new PaymentItem(itemData.amount(), payee, itemData.notes());
                tempPeriod.addPaymentItem(item);
            }

            // Calculate ending balance for display
            tempPeriod.calculateEndingBalance();

            model.addAttribute("paymentPeriod", tempPeriod);

            // Reload payees for the form
            List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
                .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
                .toList();
            model.addAttribute("payees", payeeDtos);

            // Return to form with error and preserved data
            return "payment-periods/form";
        }
    }

    /**
     * Shows the form to edit an existing payment period.
     * GET /payment-periods/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);

        // Create simple DTOs for payees (avoid LazyInitializationException)
        List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
            .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
            .toList();
        model.addAttribute("payees", payeeDtos);

        return "payment-periods/form";
    }

    /**
     * Updates an existing payment period atomically.
     * All changes (period info and payment items) succeed or fail together.
     * POST /payment-periods/{id}
     */
    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam("periodDate") LocalDate periodDate,
        @RequestParam("startingBalance") BigDecimal startingBalance,
        @RequestParam(value = "currency", defaultValue = "USD") String currency,
        @RequestParam(value = "originalIndex", required = false) Integer[] originalIndices,
        @RequestParam(value = "payeeId", required = false) Long[] payeeIds,
        @RequestParam(value = "payeeName", required = false) String[] payeeNames,
        @RequestParam(value = "amount", required = false) BigDecimal[] amounts,
        @RequestParam(value = "notes", required = false) String[] notesArray,
        Model model
    ) {
        log.info("Updating payment period ID: {} for date: {}", id, periodDate);
        log.debug("Update payment period - Starting Balance: {}, Currency: {}", startingBalance, currency);
        log.debug("Update payment period - Original Indices: {}", originalIndices != null ? java.util.Arrays.toString(originalIndices) : "null");
        log.debug("Update payment period - Payee IDs: {}", payeeIds != null ? java.util.Arrays.toString(payeeIds) : "null");
        log.debug("Update payment period - Payee Names: {}", payeeNames != null ? java.util.Arrays.toString(payeeNames) : "null");
        log.debug("Update payment period - Amounts: {}", amounts != null ? java.util.Arrays.toString(amounts) : "null");
        log.debug("Update payment period - Notes: {}", notesArray != null ? java.util.Arrays.toString(notesArray) : "null");

        MonetaryAmount monetaryAmount = Money.of(startingBalance, currency);

        // Build payment items with tracking for existing vs new
        record ItemWithIndex(DefaultPaymentPeriodService.PaymentItemData data, Integer originalIndex) {}
        List<ItemWithIndex> allItems = new ArrayList<>();

        if (amounts != null) {
            for (int i = 0; i < amounts.length; i++) {
                if (amounts[i] != null) {
                    MonetaryAmount itemAmount = Money.of(amounts[i], currency);
                    Long payeeId = (payeeIds != null && i < payeeIds.length) ? payeeIds[i] : null;
                    String payeeName = (payeeNames != null && i < payeeNames.length) ? payeeNames[i] : null;
                    String notes = (notesArray != null && i < notesArray.length) ? notesArray[i] : null;
                    Integer originalIndex = (originalIndices != null && i < originalIndices.length) ? originalIndices[i] : null;

                    // Only add if we have a payee (either ID or name)
                    if (payeeId != null || (payeeName != null && !payeeName.isBlank())) {
                        DefaultPaymentPeriodService.PaymentItemData data =
                            new DefaultPaymentPeriodService.PaymentItemData(
                                itemAmount,
                                payeeId,
                                payeeName,
                                notes
                            );
                        allItems.add(new ItemWithIndex(data, originalIndex));
                    }
                }
            }
        }

        // Separate existing items (originalIndex >= 0) from new items (originalIndex = -1 or null)
        List<ItemWithIndex> existingItems = allItems.stream()
            .filter(item -> item.originalIndex != null && item.originalIndex >= 0)
            .sorted((a, b) -> Integer.compare(a.originalIndex, b.originalIndex))
            .toList();

        List<ItemWithIndex> newItems = allItems.stream()
            .filter(item -> item.originalIndex == null || item.originalIndex < 0)
            .toList();

        // Combine: existing items first (in original order), then new items
        List<DefaultPaymentPeriodService.PaymentItemData> paymentItems = new ArrayList<>();
        existingItems.forEach(item -> paymentItems.add(item.data));
        newItems.forEach(item -> paymentItems.add(item.data));

        log.debug("Reordered payment items: {} existing + {} new = {} total",
            existingItems.size(), newItems.size(), paymentItems.size());

        try {
            // Update atomically - all changes succeed or all fail
            paymentPeriodService.updatePaymentPeriodWithPaymentItems(
                id,
                periodDate,
                monetaryAmount,
                paymentItems
            );

            return "redirect:/payment-periods/" + id;
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., duplicate date)
            log.warn("Validation error updating payment period {}: {}", id, e.getMessage());

            model.addAttribute("error", e.getMessage());
            model.addAttribute("periodId", id); // Pass ID separately for form action

            // Preserve user's input by creating a transient PaymentPeriod object with their changes
            PaymentPeriod tempPeriod = new PaymentPeriod(periodDate, monetaryAmount);

            // Rebuild payment items from submitted data
            for (DefaultPaymentPeriodService.PaymentItemData itemData : paymentItems) {
                Payee payee;
                if (itemData.payeeId() != null) {
                    payee = payeeService.findById(itemData.payeeId()).orElse(new Payee(itemData.payeeName()));
                } else {
                    payee = new Payee(itemData.payeeName());
                }
                PaymentItem item = new PaymentItem(itemData.amount(), payee, itemData.notes());
                tempPeriod.addPaymentItem(item);
            }

            // Calculate ending balance for display
            tempPeriod.calculateEndingBalance();

            model.addAttribute("paymentPeriod", tempPeriod);

            // Reload payees for the form
            List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
                .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
                .toList();
            model.addAttribute("payees", payeeDtos);

            // Return to form with error and preserved data
            return "payment-periods/form";
        }
    }

    /**
     * Deletes a payment period.
     * POST /payment-periods/{id}/delete (using POST to support HTML forms)
     * <p>
     * For HTMX requests, returns HX-Redirect header to navigate to the list page.
     * For regular requests, returns a standard redirect.
     */
    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        paymentPeriodService.deletePaymentPeriod(id);

        // If this is an HTMX request, use HX-Redirect header
        if ("true".equals(hxRequest)) {
            response.setHeader("HX-Redirect", "/balancetracker/payment-periods");
            return null;
        }

        return "redirect:/payment-periods";
    }

    // ==================== HTMX-Powered Endpoints ====================

    /**
     * Adds a new payment item row to the form (returns HTML fragment).
     * This endpoint is called by HTMX to dynamically add payment item rows.
     * <p>
     * POST /payment-periods/{id}/payment-items/add-row
     * Returns: HTML fragment for a new empty payment item row
     */
    @PostMapping("/{id}/payment-items/add-row")
    public String addPaymentItemRow(
        @PathVariable Long id,
        @RequestParam("index") int index,
        Model model
    ) {
        model.addAttribute("index", index);
        model.addAttribute("paymentPeriodId", id);

        // Create simple DTOs for payees (avoid LazyInitializationException)
        List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
            .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
            .toList();
        model.addAttribute("payees", payeeDtos);

        return "payment-periods/fragments/payment-item-row :: payment-item-row";
    }

    /**
     * Adds a new payment item row to the form for NEW payment period (returns HTML fragment).
     * This endpoint is used during payment period creation before an ID exists.
     * <p>
     * POST /payment-periods/new/payment-items/add-row
     * Returns: HTML fragment for a new empty payment item row
     */
    @PostMapping("/new/payment-items/add-row")
    public String addPaymentItemRowForNew(
        @RequestParam("index") int index,
        Model model
    ) {
        model.addAttribute("index", index);
        model.addAttribute("paymentPeriodId", "new");

        // Create simple DTOs for payees (avoid LazyInitializationException)
        List<Map<String, Object>> payeeDtos = payeeService.findAll().stream()
            .map(p -> Map.of("id", (Object) p.getId(), "name", (Object) p.getName()))
            .toList();
        model.addAttribute("payees", payeeDtos);

        return "payment-periods/fragments/payment-item-row :: payment-item-row";
    }

    /**
     * Adds a payment item to a payment period and returns the updated payment items list.
     * <p>
     * POST /payment-periods/{id}/payment-items
     * Returns: HTML fragment with updated payment items table
     */
    @PostMapping("/{id}/payment-items")
    public String addPaymentItem(
        @PathVariable Long id,
        @RequestParam("amount") BigDecimal amount,
        @RequestParam(value = "payeeId", required = false) Long payeeId,
        @RequestParam(value = "payeeName", required = false) String payeeName,
        @RequestParam(value = "notes", required = false) String notes,
        @RequestParam(value = "currency", defaultValue = "USD") String currency,
        Model model,
        HttpServletResponse response
    ) {
        MonetaryAmount monetaryAmount = Money.of(amount, currency);
        paymentPeriodService.addPaymentItem(id, monetaryAmount, payeeId, payeeName, notes);

        // Return updated payment period with all payment items
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);

        // Trigger balance update via htmx event
        response.setHeader("HX-Trigger", "balanceUpdated");

        return "payment-periods/fragments/payment-items-table :: payment-items-table";
    }

    /**
     * Removes a payment item from a payment period.
     * Returns updated payment items list and balance.
     * <p>
     * DELETE /payment-periods/{id}/payment-items/{itemId}
     * Returns: HTML fragment with updated payment items table and balance
     */
    @DeleteMapping("/{id}/payment-items/{itemId}")
    public String removePaymentItem(
        @PathVariable Long id,
        @PathVariable Long itemId,
        @RequestHeader(value = "X-View-Page", required = false) String viewPage,
        Model model,
        HttpServletResponse response
    ) {
        paymentPeriodService.removePaymentItem(id, itemId);

        // If request is from view page, redirect back to view page
        if ("true".equals(viewPage)) {
            response.setHeader("HX-Redirect", "/balancetracker/payment-periods/" + id);
            return null;
        }

        // Return updated payment period with remaining payment items (for edit page)
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);

        // Trigger balance update via htmx event
        response.setHeader("HX-Trigger", "balanceUpdated");

        return "payment-periods/fragments/payment-items-table :: payment-items-table";
    }

    /**
     * Calculates and returns the real-time ending balance.
     * Used by HTMX to update balance as payment items are added/removed.
     * <p>
     * GET /payment-periods/{id}/balance
     * Returns: HTML fragment with calculated balance
     */
    @GetMapping("/{id}/balance")
    public String calculateBalance(@PathVariable Long id, Model model) {
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);
        return "payment-periods/fragments/balance :: balance";
    }

    /**
     * Searches/filters payment periods based on query.
     * Returns filtered list of payment periods.
     * <p>
     * GET /payment-periods/search?q={query}
     * Returns: HTML fragment with filtered payment periods
     */
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String query, Model model) {
        List<PaymentPeriod> paymentPeriods;

        if (query == null || query.isBlank()) {
            paymentPeriods = paymentPeriodService.findAllWithPaymentItems();
        } else {
            // Simple search - in a real app, you'd have more sophisticated filtering
            paymentPeriods = paymentPeriodService.findAllWithPaymentItems().stream()
                .filter(pp -> pp.getPeriodDate().toString().contains(query))
                .toList();
        }

        model.addAttribute("paymentPeriods", paymentPeriods);
        return "payment-periods/fragments/payment-periods-list :: payment-periods-list";
    }

    /**
     * Updates a payment item and returns the updated table.
     * <p>
     * PUT /payment-periods/{id}/payment-items/{itemId}
     * Returns: HTML fragment with updated payment items table
     */
    @PutMapping("/{id}/payment-items/{itemId}")
    public String updatePaymentItem(
        @PathVariable Long id,
        @PathVariable Long itemId,
        @RequestParam("amount") BigDecimal amount,
        @RequestParam(value = "payeeId", required = false) Long payeeId,
        @RequestParam(value = "payeeName", required = false) String payeeName,
        @RequestParam(value = "notes", required = false) String notes,
        @RequestParam(value = "currency", defaultValue = "USD") String currency,
        Model model
    ) {
        MonetaryAmount monetaryAmount = Money.of(amount, currency);
        paymentPeriodService.updatePaymentItem(id, itemId, monetaryAmount, payeeId, payeeName, notes);

        // Return updated payment period with all payment items
        PaymentPeriod paymentPeriod = paymentPeriodService.findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        model.addAttribute("paymentPeriod", paymentPeriod);
        return "payment-periods/fragments/payment-items-table :: payment-items-table";
    }
}
