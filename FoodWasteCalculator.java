package com.example.biogassimulation;

import java.text.DecimalFormat;

public class FoodWasteCalculator {

    public static double calculateTotalVolatileSolidsF(String selectedItemF, double foodNum, double foodVolatile) {
        double resultF;

        // Calculate based on selected item
        switch (selectedItemF) {
            case "Vegetable Waste":
            case "Rice Straw":
            case "Fruit Waste":
            case "Mixed Organic Waste":
            case "Cereal or Grains":
            case "Wheat Straw":
            case "Grass":
            case "Corn Stalk":
            case "Fat":
            case "Mixed Food Waste":
                resultF = foodNum * foodVolatile;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItemF);
        }

        return resultF;
    }

    public static double calculateFoodAmount(String selectedItemF, double foodNum) {
        double livestockVolatile;
        double resultF;

        // Calculate based on selected item
        // Calculate based on selected item
        switch (selectedItemF) {
            case "Vegetable Waste":
                livestockVolatile = 0.16;

                break;
            case "Rice Straw":
                livestockVolatile = 0.36;
                break;
            case "Fruit Waste":
                livestockVolatile = 0.14;
                break;
            case "Mixed Organic Waste":
                livestockVolatile = 0.26;
                break;
            case "Cereal or Grains":
                livestockVolatile = 0.81;
                break;
            case "Wheat Straw":
                livestockVolatile = 0.39;
                break;
            case "Grass":
                livestockVolatile = 0.51;
                break;
            case "Corn Stalk":
                livestockVolatile = 0.43;
                break;
            case "Fat":
                livestockVolatile = 0.83;
                break;
            case "Mixed Food Waste":
                livestockVolatile = 0.08;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItemF);

        }




        // Calculate result based on foodNum and livestockVolatile
        resultF = foodNum * livestockVolatile;
        // Round off the result to four decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        resultF = Double.parseDouble(decimalFormat.format(resultF));

        return resultF;

    }
}
