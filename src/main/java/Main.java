import BTree.BTree;
import BTree.SearchedPage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class Main {

    // Valores de t escolhido para a inserção
    private static final int[] t = {2, 100, 500, 1000};

    // Quantidade de chaves testadas
    private static final int[] q = {1000, 10000, 100000};

    private static int randomInteger() {
        long range = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE + 1;
        return (int) (Math.random() * range + Integer.MIN_VALUE);
    }
    private static void deleteIfExists(String fileName) {
        try {
            Path p = Paths.get(fileName);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertion(String fileName, int t, int quantity) {
        BTree bt = new BTree(fileName, t);
        for (int i = 1; i <= quantity; i++) {
            bt.insert(i);
            System.out.format("Insertion: t=%d \t i=%d\n", t, i);
        }
        System.out.println();
    }

    private static void search(String fileName, int quantity) throws IOException {
        BTree bt = new BTree(fileName);
        for (int i = 1; i <= quantity; i++) {
            SearchedPage sp = bt.search(i);
            System.out.format("Search: %d == %d ?\n", i, sp.getSearchedKey());
            if (i != sp.getSearchedKey()) throw new IOException();
        }
        System.out.println();
    }

    private static void delete(String fileName, int quantity) {
        BTree bt = new BTree(fileName);
        for (int i = 1; i <= quantity; i++) {
           bt.delete(i);
           System.out.format("Delete: %d\n", i);
        }
        System.out.println();
    }

    public static void searchReport(int[] minimumDegrees, int[] quantities, String STORAGE_METHOD) throws IOException {
        LinkedList<String> searchLines = new LinkedList<String>();

        searchLines.add("grau_minimo,quantidade,tempo\n");


        // Testes
        for (int minimumDegree : minimumDegrees) {
            for (int quantity : quantities) {
                String fileName = String.format("t%d_q%d.bin", minimumDegree, quantity);
                deleteIfExists(fileName);
                long startTime = System.nanoTime();
                search(fileName, quantity);
                searchLines.add(String.format("%d,%d,%d\n", minimumDegree, quantity, System.nanoTime() - startTime));
            }
        }

        writeReport(searchLines, "BUSCA", STORAGE_METHOD);
    }

    public static void insertionReport(int[] minimumDegrees, int[] quantities, String STORAGE_METHOD) {
        LinkedList<String> insertionLines = new LinkedList<String>();

        insertionLines.add("grau_minimo,quantidade,tempo\n");


        // Testes
        for (int minimumDegree : minimumDegrees) {
            for (int quantity : quantities) {
                String fileName = String.format("t%d_q%d.bin", minimumDegree, quantity);
                deleteIfExists(fileName);
                long startTime = System.nanoTime();
                insertion(fileName, minimumDegree, quantity);
                insertionLines.add(String.format("%d,%d,%d\n", minimumDegree, quantity, System.nanoTime() - startTime));
            }
        }

        writeReport(insertionLines, "INSERCAO", STORAGE_METHOD);
    }

    public static void deleteReport() {
        LinkedList<String> phrases = new LinkedList<String>();

        phrases.add("grau_minimo,quantidade,tempo\n");


        // Testes
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                long startTime = System.nanoTime();
                delete(fileName, q[j]);
                phrases.add(String.format("%d,%d,%d\n", t[i], q[j], System.nanoTime() - startTime));
            }
        }

        // Escrevendo relatório
        String fileName = "relatorio-delete.csv";
        deleteIfExists(fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String phrase : phrases) bw.write(phrase);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertionAndSearchReportDynamic(int[] t, int[] q, String ARMAZENAMENTO) throws IOException {
        LinkedList<String> phrases = new LinkedList<String>();

        phrases.add("t,q,tempo_nanossegundos\n");


        // Insertion
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                insertion(fileName, t[i], q[j]);
            }
        }

        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                long startTime = System.nanoTime();
                search(fileName, q[j]);
                phrases.add(String.format("%d,%d,%d\n", t[i], q[j], System.nanoTime() - startTime));
            }
        }

        // Escrevendo relatório
        String fileName = String.format("relatorio-busca-%s.csv", ARMAZENAMENTO);
        deleteIfExists(fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String phrase : phrases) bw.write(phrase);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void fullDynamicReport(String ARMAZENAMENTO) throws IOException {
        LinkedList<String> insertionLines = new LinkedList<String>();
        LinkedList<String> searchLines = new LinkedList<String>();
        LinkedList<String> deleteLines = new LinkedList<String>();

        insertionLines.add("t,q,tempo_nanossegundos\n");
        searchLines.add("t,q,tempo_nanossegundos\n");
        deleteLines.add("t,q,tempo_nanossegundos\n");


        // Insertion
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                long startTime = System.nanoTime();
                insertion(fileName, t[i], q[j]);
                insertionLines.add(String.format("%d,%d,%d\n", t[i], q[j], System.nanoTime() - startTime));
            }
        }
        writeReport(insertionLines, "INSERCAO", ARMAZENAMENTO);

        // Busca
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                long startTime = System.nanoTime();
                search(fileName, q[j]);
                searchLines.add(String.format("%d,%d,%d\n", t[i], q[j], System.nanoTime() - startTime));
            }
        }
        writeReport(searchLines, "BUSCA", ARMAZENAMENTO);

        // Remoção
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < q.length; j++) {
                String fileName = String.format("t%d_q%d.bin", t[i], q[j]);
                long startTime = System.nanoTime();
                delete(fileName, q[j]);
                deleteLines.add(String.format("%d,%d,%d\n", t[i], q[j], System.nanoTime() - startTime));
            }
        }
        writeReport(deleteLines, "REMOCAO", ARMAZENAMENTO);
    }

    private static void writeReport(LinkedList<String> lines, String PROC, String STORAGE_METHOD) {
        String fileName = String.format("relatorio-%s-%s.csv", PROC, STORAGE_METHOD);
        deleteIfExists(fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : lines) bw.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
