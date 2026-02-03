package dk.balancetracker.web;

import dk.balancetracker.domain.PaymentPeriod;
import dk.balancetracker.service.PaymentPeriodService;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.money.MonetaryAmount;
import java.util.List;

/**
 * Home controller for the dashboard.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
public class HomeController {

    private final PaymentPeriodService paymentPeriodService;

    public HomeController(PaymentPeriodService paymentPeriodService) {
        this.paymentPeriodService = paymentPeriodService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Get all payment periods with payment items eagerly loaded
        List<PaymentPeriod> allPeriods = paymentPeriodService.findAllWithPaymentItems();

        // Calculate dashboard metrics
        int totalPeriods = allPeriods.size();

        // Get the most recent period for current balance
        MonetaryAmount currentBalance = Money.of(0, "USD");
        MonetaryAmount totalPaymentsCurrentPeriod = Money.of(0, "USD");

        if (!allPeriods.isEmpty()) {
            PaymentPeriod mostRecent = allPeriods.get(0); // Already sorted by date desc
            currentBalance = mostRecent.getEndingBalance() != null
                ? mostRecent.getEndingBalance()
                : mostRecent.getStartingBalance();
            totalPaymentsCurrentPeriod = mostRecent.getTotalPayments();
        }

        // Get recent periods for the list (last 5)
        List<PaymentPeriod> recentPeriods = allPeriods.stream()
            .limit(5)
            .toList();

        // Add to model
        model.addAttribute("totalPeriods", totalPeriods);
        model.addAttribute("currentBalance", currentBalance);
        model.addAttribute("totalPaymentsCurrentPeriod", totalPaymentsCurrentPeriod);
        model.addAttribute("recentPeriods", recentPeriods);

        return "home";
    }
}
