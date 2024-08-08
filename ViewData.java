package com.example.biogassimulation;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViewData extends AppCompatActivity {

    private static final String TAG = "ViewData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        TextView textView = findViewById(R.id.textView);
        Button exit = findViewById(R.id.Exit);
        Button clearData = findViewById(R.id.Delete);
        exit.setOnClickListener(v -> {
            startActivity(new Intent(ViewData.this, MainActivity.class));
            finish();
        });

        clearData.setOnClickListener(v -> {
            try {
                // Disable the button to prevent multiple clicks during the process
                clearData.setEnabled(false);

                // Rename the current file as a backup
                File sourceFile = new File(getExternalFilesDir(null), "formula.xls");
                if (sourceFile.exists()) {
                    String backupFileName = "formula_backup_" + System.currentTimeMillis() + ".xls";
                    File backupFile = new File(getExternalFilesDir(null), backupFileName);
                    if (sourceFile.renameTo(backupFile)) {
                        Log.d(TAG, "File renamed successfully: " + backupFileName);
                    } else {
                        Log.e(TAG, "Failed to rename file");
                        return;
                    }
                } else {
                    Log.e(TAG, "Source file does not exist");
                    return;
                }

                // Copy the new spreadsheet file
                File newFile = new File(getExternalFilesDir(null), "originalformula.xls");
                if (newFile.exists()) {
                    File destinationFile = new File(getExternalFilesDir(null), "formula.xls");
                    FileInputStream fis = new FileInputStream(newFile);
                    FileOutputStream fos = new FileOutputStream(destinationFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fis.close();
                    fos.close();

                    Log.d(TAG, "New file copied successfully: formula.xls");

                    // Show a toast after 2 seconds
                    new Handler().postDelayed(() -> {
                        Toast.makeText(this, "Data cleared successfully", Toast.LENGTH_SHORT).show();
                        // Re-enable the button after the process completes
                        clearData.setEnabled(true);
                        // Refresh the activity
                        recreate();
                    }, 2000);
                } else {
                    Log.e(TAG, "New file does not exist");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to clear data", Toast.LENGTH_SHORT).show();
                // Re-enable the button in case of an exception
                clearData.setEnabled(true);
            }
        });



        try {
            // Merge the latest rows from temporary files into the original file
            mergeLatestRowsFromTemporaryFiles("formula.xls");

            // Read the merged XLS file
            FileInputStream fileInputStream = new FileInputStream(new File(getExternalFilesDir(null), "formula.xls"));
            HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);

            // Create buttons dynamically for each sheet
            LinearLayout buttonsLayout = findViewById(R.id.button1);
            for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
                Button button = new Button(this);
                button.setText("Sheet " + (i + 1));
                int finalI = i;
                button.setOnClickListener(v -> displaySheet(workbook.getSheetAt(finalI), textView));
                buttonsLayout.addView(button);
            }

            // Display the first sheet by default
            displaySheet(workbook.getSheetAt(1), textView);

        } catch (IOException e) {
            e.printStackTrace();
        }
        Button share = findViewById(R.id.Share);
        share.setOnClickListener(v -> {
            shareFileViaBluetooth();
            finish();
        });
    }

    private void mergeLatestRowsFromTemporaryFiles(String originalFileName) {
        try {
            File originalFile = new File(getExternalFilesDir(null), originalFileName);
            if (!originalFile.exists()) {
                Log.e(TAG, "Original file does not exist: " + originalFileName);
                return;
            }

            FileInputStream originalInputStream = new FileInputStream(originalFile);
            Workbook originalWorkbook = new HSSFWorkbook(originalInputStream);
            Sheet originalSheet = originalWorkbook.getSheetAt(1);

            File[] tempFiles = getExternalFilesDir(null).listFiles((dir, name) -> name.startsWith("temp_") && name.endsWith(".xls"));

            if (tempFiles == null || tempFiles.length == 0) {
                originalInputStream.close();
                return;
            }

            int lastRowNum = getLastDataRowIndex(originalSheet) + 1;

            for (File tempFile : tempFiles) {
                FileInputStream tempInputStream = new FileInputStream(tempFile);
                Workbook tempWorkbook = new HSSFWorkbook(tempInputStream);
                Sheet tempSheet = tempWorkbook.getSheetAt(1);

                int tempLastRowNum = getLastDataRowIndex(tempSheet);
                if (tempLastRowNum >= 0) {
                    Row tempRow = tempSheet.getRow(tempLastRowNum);
                    Row newRow = originalSheet.createRow(lastRowNum++);

                    for (Cell tempCell : tempRow) {
                        Cell newCell = newRow.createCell(tempCell.getColumnIndex(), tempCell.getCellType());
                        switch (tempCell.getCellType()) {
                            case STRING:
                                newCell.setCellValue(tempCell.getStringCellValue());
                                break;
                            case NUMERIC:
                                newCell.setCellValue(tempCell.getNumericCellValue());
                                break;
                            default:
                                break;
                        }
                    }
                }

                tempInputStream.close();
                tempFile.delete();
            }

            originalInputStream.close();

            FileOutputStream outputStream = new FileOutputStream(originalFile);
            originalWorkbook.write(outputStream);
            outputStream.close();
            originalWorkbook.close();

        } catch (IOException e) {
            Log.e(TAG, "Error merging files: ", e);
        }
    }

    private int getLastDataRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int j = 1; j <= 4; j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null && !cell.toString().isEmpty()) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    // Method to display the contents of a specific sheet
    private void displaySheet(HSSFSheet sheet, TextView textView) {
        int startColumn = 0;
        int endColumn = 8;
        int columnWidth = 10;

        textView.setTypeface(Typeface.MONOSPACE);

        StringBuilder data = new StringBuilder();
        for (Row row : sheet) {
            for (int i = startColumn; i <= endColumn; i++) {
                Cell cell = row.getCell(i);
                String cellValue = cell != null ? cell.toString() : "";
                if (cellValue.length() > columnWidth) {
                    cellValue = cellValue.substring(0, columnWidth);
                } else if (cellValue.length() < columnWidth) {
                    StringBuilder paddedValue = new StringBuilder(cellValue);
                    while (paddedValue.length() < columnWidth) {
                        paddedValue.append(" ");
                    }
                    cellValue = paddedValue.toString();
                }
                data.append(cellValue).append("\t");
            }
            data.append("\n");
        }

        textView.setText(data.toString());
    }
    private void shareFileViaBluetooth() {
        File file = new File(getExternalFilesDir(null), "formula.xls");

        if (!file.exists()) {
            // File not found, show a toast or handle the error accordingly
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");

        // Get the content URI using FileProvider
        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);

        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.setPackage("com.android.bluetooth"); // Restrict to Bluetooth

        // Grant permission to access the content URI
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Bluetooth sharing not supported or no app found, handle the error
            Toast.makeText(this, "Bluetooth not supported or no app found", Toast.LENGTH_SHORT).show();
        }
    }

}
