package com.example.biogassimulation;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

public class LoadDataFromExcelTask extends AsyncTask<Void, Void, Double> {
    private Context context;
    private EditText biogasInput;

    public LoadDataFromExcelTask(Context context, EditText biogasInput) {
        this.context = context;
        this.biogasInput = biogasInput;
    }

    @Override
    protected Double doInBackground(Void... voids) {
        try {
            // Load biogas production data from Excel
            return readBiogasProductionFromExcel(context, "formula.xls");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Double biogasproductionvalue) {
        super.onPostExecute(biogasproductionvalue);
        if (biogasproductionvalue != null) {
            // Update biogasInput field with loaded value
            biogasInput.setText(String.valueOf(biogasproductionvalue));
        } else {
            // Inform the user if data loading fails
            Toast.makeText(context, "Failed to load data from Excel", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to read biogas production data from Excel
    private Double readBiogasProductionFromExcel(Context context, String fileName) throws IOException {
        File file = new File(context.getExternalFilesDir(null), fileName);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            // Create instance having reference to .xls file
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(3);

            // Iterate through rows to find biogas production value
            for (int i = sheet.getLastRowNum(); i >= 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        if (cell.getCellType() == CellType.NUMERIC) {
                            // Assuming biogas production is in column 7 (0-indexed) of sheet index 3
                            if (sheet.getWorkbook().getSheetIndex(sheet) == 3 && cell.getColumnIndex() == 8) {
                                return cell.getNumericCellValue();
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
