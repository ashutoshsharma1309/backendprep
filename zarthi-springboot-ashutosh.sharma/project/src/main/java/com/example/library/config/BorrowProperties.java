package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "library.borrow")
public class BorrowProperties {

    private int maxBooksPerPatron;
    private int loanPeriodDays;

    public int getMaxBooksPerPatron() {
        return maxBooksPerPatron;
    }

    public void setMaxBooksPerPatron(int maxBooksPerPatron) {
        this.maxBooksPerPatron = maxBooksPerPatron;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }

    public void setLoanPeriodDays(int loanPeriodDays) {
        this.loanPeriodDays = loanPeriodDays;
    }
}
