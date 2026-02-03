package dk.balancetracker.web;

import dk.balancetracker.domain.PaymentPeriod;
import dk.balancetracker.service.PaymentPeriodService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tools.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for reports and analytics.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
@RequestMapping("/reports")
public class ReportsController {

    private final PaymentPeriodService paymentPeriodService;
    private final ObjectMapper objectMapper;

    public ReportsController(PaymentPeriodService paymentPeriodService, ObjectMapper objectMapper) {
        this.paymentPeriodService = paymentPeriodService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String reports(Model model) {
        // Get all payment periods with items for analytics
        List<PaymentPeriod> allPeriods = paymentPeriodService.findAllWithPaymentItems();

        model.addAttribute("paymentPeriods", allPeriods);

        // Create JavaScript-friendly data
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");
        List<PeriodData> periodData = allPeriods.stream()
            .map(p -> new PeriodData(
                p.getPeriodDate().format(dateFormatter),
                p.getStartingBalance().getNumber().doubleValue(),
                p.getTotalPayments().getNumber().doubleValue(),
                p.getEndingBalance().getNumber().doubleValue()
            ))
            .toList();

        // Serialize to JSON for JavaScript consumption
        try {
            String periodDataJson = objectMapper.writeValueAsString(periodData);
            model.addAttribute("periodDataJson", periodDataJson);
        } catch (Exception e) {
            // If JSON serialization fails, provide empty array
            model.addAttribute("periodDataJson", "[]");
        }

        return "reports";
    }

    /**
     * Simple DTO for JavaScript consumption.
     */
    record PeriodData(
        String periodDate,
        double startingBalance,
        double totalPayments,
        double endingBalance
    ) {}
}
