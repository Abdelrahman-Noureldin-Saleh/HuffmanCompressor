/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package huffman;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 *
 * @author Abd El Rahman
 */
public class Main {

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte US = 0x1f;
    private static final byte GS = 0x1d;

    private static final int COMPRESSION = 0;
    private static final int DECOMPRESSION = 1;

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws IOException {

        Scanner keyboard = new Scanner(System.in);
        while (true) {
            System.out.print("Enter 0 for compression, 1 for decompression: ");
            int choice = keyboard.nextInt();
            switch (choice) {
                case COMPRESSION: { // compression 
                    System.out.print("please Enter the file Name, including the extension (e.g myFile.txt): ");
                    String FileName = keyboard.next();
                    Main.compress(FileName);
                    break;
                }
                case DECOMPRESSION: {
                    System.out.print("please Enter the compressed file Name, including the extension (e.g myFile.txt): ");
                    String FileName = keyboard.next();
                    Main.decompress(FileName);
                    break;
                }
                default:
                    System.out.print("Please Enter 0 or 1 ");
                    break;
            }
        }

    }

    private static void compress(String fileName) throws IOException {

        long start = System.currentTimeMillis();
        File file = new File(fileName);
        if (file.exists()) {
            if (file.isDirectory()) {
                compressFolder(fileName);
            } else {
                compressFile(fileName);
            }
        } else {
            System.out.println("File Does Not Exist");
            return;
        }
        long end = System.currentTimeMillis();
        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.println("Execution time is " + formatter.format((end - start) / 1000d) + " seconds");

    }

    private static void compressFile(String fileName) throws IOException {

        ArrayList<TableEntry> encodingTable = new ArrayList<>();
        File file = new File(fileName);
        long totalSpace = file.getTotalSpace();
        Map<Byte, Node> C = readFile(fileName);
        Node huffmanTree = Huffman(C);

        generateCode(huffmanTree, encodingTable, "");
        Collections.sort(encodingTable);
        generateCanonicalCode(encodingTable);

        byte[] out = writeToFile(fileName, encodingTable);
        Path path = Paths.get(fileName + ".compressed");
        Files.write(path, out);
        File o = new File(fileName + ".compressed");
        long totalSpace1 = o.getTotalSpace();
        //System.out.println("Compression ratio: " + (double) totalSpace1 / totalSpace);

        int i = 0;
        for (TableEntry entry : encodingTable) {
            System.out.printf("%03d] symbol: %-5s  freq: %-5d  ascii: %-8s    ascii: %02X    code: %-12s   canonical: %s \n", i++, escape(entry.symbol), entry.frequency, rightShift8(Integer.toString(entry.symbol & 0xFF, 2)), entry.symbol, entry.code, entry.canonicalCode);
        }
    }

    private static void compressFolder(String fileName) throws IOException {
        File dir = new File(fileName);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isDirectory()) {
                    compressFolder(child.getAbsolutePath());
                } else {
                    compressFile(child.getAbsolutePath());
                }
            }
        }
    }

    private static void decompress(String fileName) throws IOException {
        long TimeStart = System.currentTimeMillis();
        
        Path path = Paths.get(fileName + ".compressed");
        byte[] bytes = Files.readAllBytes(path);

        ArrayList<TableEntry> table = new ArrayList<>();
        int i = 0;
        while (i < bytes.length) {
            if (bytes[i++] == GS) {
                break;
            }
        }
        int l = i;
        int x = 0;
        for (int j = 0; j < l - 1; j++) {
            for (int k = 0; k < (bytes[j] & 0xFF); k++) {
                x++;
                TableEntry e = new TableEntry();
                e.symbol = bytes[i++];
                e.codeLength = j;
                table.add(e);
            }
        }
        if (bytes[i++] != STX) {
            System.out.println("Problem... compressed File is corrupted: " + x);
            System.exit(0);
        }

        generateCanonicalCode(table);

        int t = 0;
        for (TableEntry entry : table) {
            System.out.printf("%03d] symbol: %-5s  ascii: %-8s    ascii: %02X   canonical: %s \n", t++, escape(entry.symbol), rightShift8(Integer.toString(entry.symbol & 0xFF, 2)), entry.symbol, entry.canonicalCode);
        }

        Map<String, Byte> decodingTable = new HashMap<>();
        table.forEach((entry) -> {
            decodingTable.put(entry.canonicalCode, entry.symbol);
        });

        FileOutputStream out = new FileOutputStream("decompressed - " + fileName);

        String buffer = rightShift8(Integer.toString(bytes[i++] & 0xFF, 2));
        int end = 1;
        while (true) {
            if (end >= buffer.length()) {
                if (i == bytes.length) {
                    break;
                }
                buffer += rightShift8(Integer.toString(bytes[i++] & 0xFF, 2));
            }
            String sequence = buffer.substring(0, end);
            if (decodingTable.containsKey(sequence)) {
                byte bytee = decodingTable.get(sequence);
                if (bytee == ETX && i > bytes.length - 1) {
                    break;
                }
                out.write(bytee);
                buffer = buffer.substring(end);
                end = 1;
            }
            end++;
        }
        System.out.println();

        out.close();
        
        long TimeEnd = System.currentTimeMillis();
        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.println("Execution time is " + formatter.format((TimeEnd - TimeStart) / 1000d) + " seconds");

    }

    private static Map<Byte, Node> readFile(String fileName) throws IOException {

        Path path = Paths.get(fileName);
        byte[] bytes = Files.readAllBytes(path);
        Map<Byte, Node> list = new HashMap<>();

        for (byte b : bytes) {
            if (list.containsKey(b)) {
                Node node = list.get(b);
                node.frequency = node.frequency + 1;
                list.put(b, node);
            } else {
                Node x = new Node();
                x.letter = b;
                x.frequency = 1;
                list.put(b, x);
            }
        }
        Node etx = new Node();
        etx.letter = ETX;
        etx.frequency = 1;
        list.put(etx.letter, etx);
        return list;
    }

    private static byte[] writeToFile(String fileName, ArrayList<TableEntry> table) throws FileNotFoundException, IOException {

        ArrayList<Byte> out = new ArrayList<>();
        int max = table.get(table.size() - 1).codeLength;
        int[] count = new int[max + 1];
        table.forEach((entry) -> {
            count[entry.codeLength]++;
        });
        Path path = Paths.get(fileName);
        byte[] bytes = Files.readAllBytes(path);

        Map<Byte, String> encodingTable = new HashMap<>();
        table.forEach((entry) -> {
            encodingTable.put(entry.symbol, entry.canonicalCode);
        });

        //FileOutputStream out = new FileOutputStream(fileName + ".compressed");
        for (int num : count) {
            out.add((byte) num);
            //System.out.printf("%02X ", num);
        }
        out.add(GS);
        //System.out.printf("%02X ", GS);
        for (TableEntry entry : table) {
            out.add(entry.symbol);
            //System.out.printf("%02X ", entry.symbol);
        }
        out.add(STX);
        //System.out.printf("%02X ", STX);

        String buffer = "";
        for (byte b : bytes) {
            buffer += encodingTable.get(b);
            while (buffer.length() >= 8) {
                short output = (short) Integer.parseInt(buffer.substring(0, 8), 2);
                out.add((byte) output);
                buffer = buffer.substring(8);
            }
        }
        buffer += encodingTable.get(ETX);
        if (buffer.length() > 8) {
            short output = (short) Integer.parseInt(buffer.substring(0, 8), 2);
            out.add((byte) output);
            buffer = buffer.substring(8);
        }

        short output = (short) Integer.parseInt(leftShift8(buffer), 2);
        out.add((byte) output);

        System.out.println();

        Byte[] array = out.toArray(new Byte[out.size()]);
        byte[] result = new byte[array.length];
        for (int j = 0; j < array.length; j++) {
            result[j] = array[j];
        }

        return result;

    }

    // HUFFMAN ALGORITHM
    private static Node Huffman(Map<Byte, Node> C) {
        int n = C.size();
        PriorityQueue<Node> Q = new PriorityQueue(C.values());
        for (int i = 0; i < n - 1; i++) {
            Node z = new Node();
            Node x = z.left = Q.poll();
            Node y = z.right = Q.poll();
            z.frequency = x.frequency + y.frequency;
            Q.add(z);
        }
        return Q.poll();
    }

    private static void generateCode(Node node, ArrayList<TableEntry> table, String code) {

        boolean leaf = node.left == null && node.right == null;
        if (leaf) {
            TableEntry entry = new TableEntry();
            entry.symbol = node.letter;
            entry.code = code;
            entry.codeLength = code.length();
            entry.frequency = node.frequency;
            table.add(entry);
            return;
        }
        generateCode(node.left, table, code + "0");
        generateCode(node.right, table, code + "1");
    }

    private static void generateCanonicalCode(ArrayList<TableEntry> encodingTable) {

        String code = "0";
        for (TableEntry entry : encodingTable) {
            int amount = entry.codeLength - code.length();
            entry.canonicalCode = code = leftShift(code, amount);
            code = increment(code);
        }
    }

    // UTILITY FUNCTIONS
    private static String leftShift(String code, int amount) {
        for (int i = 0; i < amount; i++) {
            code = code + "0";
        }
        return code;
    }

    private static String rightShift(String code, int amount) {
        for (int i = 0; i < amount; i++) {
            code = "0" + code;
        }
        return code;
    }

    private static String rightShift8(String code) {
        return rightShift(code, 8 - code.length());
    }

    private static String leftShift8(String code) {
        return leftShift(code, 8 - code.length());
    }

    private static String increment(String code) {
        int length = code.length();
        BigInteger num = new BigInteger(code, 2);
        num = num.add(BigInteger.ONE);
        String result = num.toString(2);
        while (result.length() < length) {
            result = "0" + result;
        }
        return result;
    }

    private static String escape(byte ascii) {
        if (ascii < 32) {
            return "'-'";
        }
        return "'" + (char) ascii + "'";
    }

}
