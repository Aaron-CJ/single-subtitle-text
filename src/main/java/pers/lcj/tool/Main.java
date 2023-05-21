package pers.lcj.tool;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Aaron on 2023/4/28 0:29
 */
@Log4j2
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, "GBK");

        System.out.println("请输入导入文件路径：");
        Path inputPath = getPath(scanner, "导入文件");
        System.out.println("请输入导出目录路径：");
        Path outputPath = getPath(scanner, "导出目录");

        Map<String, String> result = readFile(inputPath.toString());

        writeOutputFiles(outputPath, result);
    }

    public static Path getPath(Scanner sc, String pathType) {
        Path path;
        do {
            String inputPath = sc.nextLine().replaceAll("^\"+|\"+$", "");
            path = Paths.get(inputPath);
            if (pathType.equals("导入文件") && !Files.isRegularFile(path)) {
                System.out.println("输入路径不是一个有效文件路径，请重新输入：");
                path = null;
            } else if (pathType.equals("导出目录") && !Files.isDirectory(path)) {
                System.out.println("输入路径不是一个有效文件路径，请重新输入：");
                path = null;
            }
        } while (path == null);
        return path;
    }

    private static final int BUFFER_SIZE = 8192;
    private static final Pattern REGEX_PATTERN = Pattern.compile("(?<=-\\s)([^.]+)(?=\\.jpeg)");

    private static Map<String, String> readFile(String inputFilePath) {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath), BUFFER_SIZE)) {
            StringBuilder currentValueBuilder = new StringBuilder(1024); // 初始化StringBuilder大小，避免过多扩容
            Matcher matcher;
            String key = null;
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                matcher = REGEX_PATTERN.matcher(currentLine);
                if (matcher.find()) {
                    if (key != null) {
                        String value = currentValueBuilder.toString().trim();
                        result.put(key, value.isEmpty() ? "null" : value); // 添加新的结果到Map中，并清空StringBuilder
                        currentValueBuilder.setLength(0);
                    }
                    key = matcher.group(1);
                } else if (key != null) { // 对currentValue进行构建
                    currentValueBuilder.append(currentLine.trim()).append(" "); // 添加空格，避免拼接相邻字符串时出现语义混淆
                }
            }
            if (key != null) { // 处理最后一个结果
                result.put(key, currentValueBuilder.toString().trim().replace("  ------------ prower by PearOCR.com ------------", ""));
            }
        } catch (IOException e) {
            log.error("Error reading file：", e);
        }
        return result;
    }

    private static void writeOutputFiles(Path outputFolderPath, Map<String, String> result) {
        System.out.println("正在处理，请稍等......");
        result.entrySet().parallelStream().forEach(entry -> {
            String fileName = entry.getKey();
            Path txtFilePath = outputFolderPath.resolve(fileName + ".txt");
            try (BufferedWriter bw = Files.newBufferedWriter(txtFilePath, StandardCharsets.UTF_8)) {
                bw.write(entry.getValue());
            } catch (IOException e) {
                log.error("Error write file：", e);
            }
        });
        System.out.println("文件总计（个）：" + result.size());
    }

}