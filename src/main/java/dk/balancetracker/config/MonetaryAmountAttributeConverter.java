package dk.balancetracker.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;

/**
 * JPA AttributeConverter for persisting MonetaryAmount as a string in format "amount|currency".
 * <p>
 * Example: 100.50 USD → "100.50|USD"
 * <p>
 * This converter allows MonetaryAmount to be stored as a single VARCHAR column in the database.
 *
 * @author Dmitriy Kopylenko
 */
@Converter(autoApply = true)
public class MonetaryAmountAttributeConverter implements AttributeConverter<MonetaryAmount, String> {

    private static final String SEPARATOR = "|";
    private static final String DEFAULT_CURRENCY_CODE = "USD";

    @Override
    public String convertToDatabaseColumn(MonetaryAmount monetaryAmount) {
        if (monetaryAmount == null) {
            return null;
        }

        return monetaryAmount.getNumber().toString() +
               SEPARATOR +
               monetaryAmount.getCurrency().getCurrencyCode();
    }

    @Override
    public MonetaryAmount convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        String[] parts = dbData.split("\\" + SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid monetary amount format: " + dbData +
                ". Expected format: amount|currency"
            );
        }

        try {
            BigDecimal amount = new BigDecimal(parts[0]);
            CurrencyUnit currency = Monetary.getCurrency(parts[1]);
            return Money.of(amount, currency);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to parse monetary amount: " + dbData, e
            );
        }
    }
}
