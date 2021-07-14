import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainTest {

    @Test
    public void insertionTest(){
        Main.insertionReport(
                new int[] {100},
                new int[] {1000, 10000},
                "HD"
        );
    }

    @Test
    public void searchTest() throws IOException {
        Main.searchReport(
                new int[] {100},
                new int[] {1000, 10000},
                "HD"
        );
    }

    @Test
    public void deleteTest() {
        Main.deleteReport();
    }

    @Test
    public void fullReport() throws IOException {
        Main.fullDynamicReport("HD");
    }

    @Test
    public void emergenceTest() throws IOException {
        Main.insertionAndSearchReportDynamic(
                new int[] {100},
                new int[] {1000, 10000},
                "HD"
        );
    }

    @Test
    public void testFile() {
        Main.insertion("t1000_q100000.bin", 1000, 100000);
    }

    @Test
    public void testBufferedReader()  {
        String fileName = "test.txt";
        try {
            Files.deleteIfExists(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }


        String line1 = "abcde\n";
        String line2 = "fghij\n";

        try {
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter inputStream = new BufferedWriter(fw);
            inputStream.write(line1); inputStream.newLine();
            inputStream.write(line2);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}