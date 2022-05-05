package at.fhv.sysarch.lab2.homeautomation.products;

import java.math.BigDecimal;

public class Product {
    private String name;
    private double weight;

    private BigDecimal price;

    public Product(String name, double weight, BigDecimal price) {
        this.name = name;
        this.weight = weight;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
