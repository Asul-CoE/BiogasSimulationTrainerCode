package com.example.biogassimulation;

public class FeedstockCalculator {

    public double calculateFeedstockAmount(String selectedItem, double livestockNum) {
        double resultL;

        // Calculate based on selected item
        switch (selectedItem) {
            case "Buffalo Waste":
                resultL = calculateFeedstockAmountBuffalo(livestockNum);
                break;
            case "Cow Waste":
                resultL = calculateFeedstockAmountCow(livestockNum);
                break;
            case "Calf Waste":
                resultL = calculateFeedstockAmountCalf(livestockNum);
                break;
            case "Sheep/Goat Waste":
                resultL = calculateFeedstockAmountSheepGoat(livestockNum);
                break;
            case "Pig Waste":
                resultL = calculateFeedstockAmountPig(livestockNum);
                break;
            case "Chicken Waste":
                resultL = calculateFeedstockAmountHens(livestockNum);
                break;
            case "Horse Waste":
                resultL = calculateFeedstockAmountHorse(livestockNum);
                break;
            case "Human Waste":
                resultL = calculateFeedstockAmountHuman(livestockNum);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItem);
        }

        return resultL;
    }

    // Methods to calculate feedstock amount for each livestock type
    private double calculateFeedstockAmountBuffalo(double livestockNum) {
        // Formula for Buffalo
        double wBuff = 14;
        return livestockNum * wBuff;
    }

    private double calculateFeedstockAmountCow(double livestockNum) {
        // Formula for Cow
        double wCow = 10;
        return livestockNum * wCow;
    }

    private double calculateFeedstockAmountCalf(double livestockNum) {
        // Formula for Calf
        double wCalf = 5; // Assuming a value of 3 for wCalf
        return livestockNum * wCalf;
    }

    private double calculateFeedstockAmountSheepGoat(double livestockNum) {
        // Formula for Sheep/Goat
        double wShp = 2;
        return livestockNum * wShp;
    }

    private double calculateFeedstockAmountPig(double livestockNum) {
        // Formula for Pig
        double wPg = 5; // Assuming a value of 5 for wPg
        return livestockNum * wPg;
    }

    private double calculateFeedstockAmountHens(double livestockNum) {
        // Formula for Chicken, base 100
        double wChk = 7.5;
        return (livestockNum / 100) * wChk;
    }

    private double calculateFeedstockAmountHorse(double livestockNum) {
        // Formula for Horse
        double wHors = 10;
        return livestockNum * wHors;
    }

    private double calculateFeedstockAmountHuman(double livestockNum) {
        // Formula for Human waste
        double wHum = 0.2;
        return livestockNum * wHum;
    }
}
