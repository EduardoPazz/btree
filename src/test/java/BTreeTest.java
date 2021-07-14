import BTree.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BTreeTest {

    @Test
    public void deveDeletarCorretamente() throws IOException {
        String fileName = "deleteTest.bin";
        Path p = Paths.get(fileName);
        Files.deleteIfExists(p);
        BTree bt1 = new BTree(fileName, 2);

        bt1.insert(90);
        bt1.insert(80);
        bt1.insert(70);
        bt1.insert(60);

        bt1.delete(90);
        bt1.delete(80);
        bt1.delete(70);
        bt1.delete(60);

        bt1.insert(90);
        bt1.insert(80);
        bt1.insert(70);
        bt1.insert(60);



        bt1.print();

//        BTree bt2 = new BTree("test.txt");
//        bt2.print();
    }

}

