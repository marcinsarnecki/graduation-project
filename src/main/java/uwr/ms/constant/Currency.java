package uwr.ms.constant;

import lombok.Getter;

public enum Currency {
    USD("United States Dollar"),
    EUR("Euro"),
    GBP("British Pound Sterling"),
    CHF("Swiss Franc"),
    JPY("Japanese Yen"),
    CNY("Chinese Yuan"),
    CAD("Canadian Dollar"),
    AUD("Australian Dollar"),
    INR("Indian Rupee"),
    RUB("Russian Ruble"),
    BRL("Brazilian Real"),
    ZAR("South African Rand"),
    SEK("Swedish Krona"),
    NOK("Norwegian Krone"),
    DKK("Danish Krone"),
    PLN("Polish Zloty"),
    CZK("Czech Koruna"),
    HUF("Hungarian Forint"),
    TRY("Turkish Lira"),
    MXN("Mexican Peso"),
    SGD("Singapore Dollar");

    @Getter
    private final String description;

    Currency(String description) {
        this.description = description;
    }
}
