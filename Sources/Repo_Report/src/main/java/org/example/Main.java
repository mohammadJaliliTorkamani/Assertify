package org.example;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.util.LinkedList;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.DefaultApiClient.Operation.GET_STARS;


/**
 * This project extracts the number of stars + number or production assertions from a methods corpus (default: those having at least 500 stars)
 */
public class Main {


    public static void main(String[] args) {
        String METHODS_CORPUS_FILE_NAME = "ADDRESS GOES HERE";

        int counter  = 0;

        try {
            FileReader fileReader = new FileReader(METHODS_CORPUS_FILE_NAME);
            BufferedReader reader = new BufferedReader(fileReader);
            Gson gson = new Gson();

            Repository[] repositories = gson.fromJson(reader, Repository[].class);
            System.out.println(repositories.length);
            String[][] data = new String[repositories.length][3];

            for (Repository repository : repositories) {
                String url = repository.getUrl();
                String creator = url.split("/")[url.split("/").length - 2];
                String repoName = url.split("/")[url.split("/").length - 1];
                int numStars = extractNumberOfStars(url,creator,repoName);
                int numProductionAssertions = extractNumberOfProductionAssertions(repository);
                System.out.println(url);
                data[counter][0]=url;
                data[counter][1]=numStars+"";
                data[counter][2]=numProductionAssertions+"";
                System.out.println((counter+1)+" / "+repositories.length+" =>  "+url+" : "+numStars+" "+numProductionAssertions);
                counter++;
            }
            createExcel("repo_statistics.xlsx",new String[]{"Repository","Number of Stars","Number of Production Assertions"},data);
        }catch (Exception e){
        }

    }

    private static int extractNumberOfProductionAssertions(Repository repository) {
        int counter = 0;
        for (RepoFile repoFile : repository.getFiles()) {
            for (Method method : repoFile.getMethods()) {
                if (hasAssertions(method)) {
                    counter++;
                }
            }
        }
        return counter;
    }

    private static int extractNumberOfStars(String url, String creator, String repoName) {
        ApiClient client = new DefaultApiClient();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "TOKEN GOES HERE");
        try {
            Map<String, String> map = new HashMap<>();
            JSONObject jsonObject = client.get(GET_STARS, creator, repoName, headers, map).getJsonArray().getJSONObject(0);
            RepositoriesInfo.getInstance().putNumberOfStars(url, jsonObject.getInt("watchers_count"));
            return jsonObject.getInt("watchers_count");
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void createExcel(String fileName, String[] headers, String[][] data) throws Exception{
        // Create a workbook
        Workbook workbook = new XSSFWorkbook();

        // Create a sheet
        Sheet sheet = workbook.createSheet("(500_Plus) Repository Statistics");

        // Create header row
        Row headerRow = sheet.createRow(0);

        // Create header cells
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Populate rows with data
        int rowNum = 1;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                if (rowData[i] instanceof String) {
                    cell.setCellValue((String) rowData[i]);
                } else if (rowData[i] instanceof Integer) {
                    cell.setCellValue((Integer) rowData[i]);
                } else if (rowData[i] instanceof Double) {
                    cell.setCellValue((Double) rowData[i]);
                }
            }
        }

        // Resize columns to fit the content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the workbook to a file
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
            workbook.close();
            System.out.println("Excel file '" + fileName + "' created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean hasAssertions(Method method) {
        return method.getAssertions().stream().anyMatch(assertion -> assertion.getType().equals("java"));
    }
}