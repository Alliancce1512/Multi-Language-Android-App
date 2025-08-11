package com.shellytask.app.legacy;

public final class LegacyCalculator {

    public static int add(int left, int right) {
        return left + right;
    }

    public static int subtract(int left, int right) {
        return left - right;
    }

    public static int multiply(int left, int right) {
        return left * right;
    }

    public static double divide(double numerator, double denominator) {
        if (denominator == 0.0d) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return numerator / denominator;
    }
}