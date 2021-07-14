
package BTree;

import java.io.IOException;
import java.io.RandomAccessFile;

// TODO: clean the several usages of RandomAccessFile by passing by parameter.

public class BTree {

    /* Cabeçalho
    *  Posição  Valor
    *  0        MINIMUN_DEGREE
    *  4        pageQuantity
    *  8        rootAddress
    * */


    private int MINIMUM_DEGREE; // Always at position 0 of the file
    private int PAGE_SIZE;
    private final String FILE_NAME;
    private int pageQuantity;
    private Page rootPage;

    // Usado quando o arquivo já existe
    public BTree(String fileName) {
        this.FILE_NAME = fileName;
        try (RandomAccessFile bTreeFile = new RandomAccessFile(fileName, "r")) {
            this.MINIMUM_DEGREE = bTreeFile.readInt();
            this.pageQuantity = bTreeFile.readInt();
            int rootAddress = bTreeFile.readInt();

            this.PAGE_SIZE = calculatePageSize();
            this.rootPage = readPageFromDisk(rootAddress, bTreeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Usado quando o arquivo ainda não existe
    public BTree(String fileName, int minimumDegree) {
        this.FILE_NAME = fileName;
        this.MINIMUM_DEGREE = minimumDegree;
        this.PAGE_SIZE = calculatePageSize();

        try (RandomAccessFile bTreeFile = new RandomAccessFile(this.FILE_NAME, "rw")) {
            bTreeFile.writeInt(MINIMUM_DEGREE); // Grava o grau mínimo na primeira posição (0)
            bTreeFile.writeInt(this.pageQuantity); // Grava a quantidade de páginas na segunda posição (4)

            this.rootPage = this.createPage(true, bTreeFile);
            this.updateRootAddress(this.rootPage.getAddressInDisk(), bTreeFile); // Grava o primeiro endereço de raiz na terceira posição (8)
            this.writePageToDisk(this.rootPage, bTreeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // TODO: write code to handle when the file already exists
    }

    private int calculatePageSize() {
        /* Tamanho da página segundo esta implementação:
         *  3 inteiros fixos: 12 bytes +
         *  (2 * t - 1) inteiros para as chaves: (2 * t - 1) * 4 bytes +
         *  (2 * t) inteiros para os ponteiros: (2 * t) * 4 bytes
         *  = 8 * (2 * t - 1)
         * */
        return 8 * (2 * this.MINIMUM_DEGREE + 1);
    }

    private Page createPage(boolean isLeaf, RandomAccessFile bTreeFile) throws IOException {
        // Chamando o getAvailableAddress antes pois o incremento no pageQuantity vai influenciar o seu valor
        int availableAddress = this.getAvailableAddress();
        this.updatePageQuantity(1, bTreeFile);
        return new Page(availableAddress, isLeaf, this.MINIMUM_DEGREE);
    }

    private void updatePageQuantity(int shift, RandomAccessFile bTreeFile) throws IOException {
        int pageQuantityPosition = 4;
        bTreeFile.seek(pageQuantityPosition);
        this.pageQuantity += shift;
        bTreeFile.writeInt(this.pageQuantity);
    }

    private void updateRootAddress(int address, RandomAccessFile btreeFile) throws IOException {
        int rootAddressPosition = 8;
        btreeFile.seek(rootAddressPosition);
        btreeFile.writeInt(address);
    }

    public void insert(int key) {
        try (RandomAccessFile bTreeFile = new RandomAccessFile(this.FILE_NAME, "rw")) {
            Page actualPage = this.rootPage;

            if (actualPage.isFull(this.MINIMUM_DEGREE)) { // Se cheio, bora pro split

                this.rootPage = createPage(false, bTreeFile);
                this.rootPage.insertPointerAtPosition(0, actualPage.getAddressInDisk()); // Endereço do primeiro filho
                this.updateRootAddress(this.rootPage.getAddressInDisk(), bTreeFile);

                this.split(this.rootPage, actualPage, 0, bTreeFile); //chamar metodo split
                this.insertNonFull(this.rootPage, key, bTreeFile); //chamar insert_nonfull
            } else this.insertNonFull(actualPage, key, bTreeFile);
            // TODO: apagar isso
            //            System.out.printf("Page quantity: %d\n", this.pageQuantity);
            //            System.out.printf("This page address: %d\n", actualPage.getAddressInDisk());
            //            System.out.printf("This page keys quantity: %d\n", actualPage.getKeysQuantity());
            //            System.out.printf("This page pointers quantity: %d\n", actualPage.getPointersQuantity());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void split(Page parentPage, Page leftChild, int indexOfLeftChild, RandomAccessFile bTreeFile) throws IOException {

        Page rightChild = this.createPage(leftChild.isLeaf(), bTreeFile);

        // Move as chaves do filho à esquerda pro filho à direita
        for (int i = 0; i < this.MINIMUM_DEGREE - 1; i++) {
            int newKey = leftChild.getKeyAtPosition(i + this.MINIMUM_DEGREE);
            rightChild.insertKeyAtPosition(i, newKey);
            leftChild.deleteKeyAtPosition(i + this.MINIMUM_DEGREE);
        }

        // Move as ponteiros do filho à esquerda pro filho à direita
        if (!leftChild.isLeaf()) { //se o filho da esquerda for não for folha
            for (int i = 0; i < this.MINIMUM_DEGREE; i++) {
                int newPointer = leftChild.getPointerAtPosition(i + this.MINIMUM_DEGREE);
                rightChild.insertPointerAtPosition(i, newPointer);
                leftChild.deletePointerAtPosition(i + this.MINIMUM_DEGREE);
            }
        }

        // Move os ponteiros do pai 1 posição à direita para abrir espaço para receber o ponteiro do filho à direita
        parentPage.shiftPointersRight(indexOfLeftChild+1);

        // Recebe o ponteiro do filho à direita
        parentPage.insertPointerAtPosition(indexOfLeftChild + 1, rightChild.getAddressInDisk());

        // Abrir espaço para receber a chave mediana
        parentPage.shiftKeysRight(indexOfLeftChild);

        // Recebe a chave mediana
        int medianKey = leftChild.getKeyAtPosition(this.MINIMUM_DEGREE - 1); // Chave mediana
        leftChild.deleteKeyAtPosition(this.MINIMUM_DEGREE - 1);
        parentPage.insertKeyAtPosition(indexOfLeftChild, medianKey);


        this.writePageToDisk(parentPage, bTreeFile);
        this.writePageToDisk(leftChild, bTreeFile);
        this.writePageToDisk(rightChild, bTreeFile);
    }

    private int getAvailableAddress() {
        int firstAvailableAddress = 12;
        return firstAvailableAddress + (this.pageQuantity * this.PAGE_SIZE);
    }


    private void insertNonFull(Page actualPage, int key, RandomAccessFile bTreeFile) throws IOException {
        // A inserção só ocorre em uma folha e, obviamente, em páginas que não estão cheias
        // Esse método irá iterar do maior pro maior para já poder ir abrindo o espaço para a chave a inserida

        int i = actualPage.getKeysQuantity() - 1;

        if (!actualPage.isLeaf()) { // Se a página não é uma folha, temos que encontrar seu filho apropriado para fazermos a chamada recursiva

            while (i >= 0 && key < actualPage.getKeyAtPosition(i)) i--;
            i++;

            Page childPage = this.readPageFromDisk(actualPage.getPointerAtPosition(i), bTreeFile);

            if (childPage.isFull(this.MINIMUM_DEGREE)) { // Se esse filho estiver cheio, usamos o split
                this.split(actualPage, childPage, i, bTreeFile);

                // Escolhe se irá descer pelo filho esquerdo ou direito após o split
                childPage = key > actualPage.getKeyAtPosition(i)
                        ? this.readPageFromDisk(actualPage.getPointerAtPosition(i+1), bTreeFile) // Escolhe o filho direito
                        : childPage; // Continua no filho esquerdo
            }

            this.insertNonFull(childPage, key, bTreeFile);

        } else { // Hora da inserção finalmente

            // Move as chaves para a direita para receber a nova chave na posição correta
            for ( ; i >= 0 && key < actualPage.getKeyAtPosition(i); i--) actualPage.copyKeyToRight(i);

            actualPage.insertKeyAtPosition(i+1, key);

            this.writePageToDisk(actualPage, bTreeFile);
        }
    }


    // This is what the book calls "BTree-Search"
    public SearchedPage search(int searchedKey) {
        return this.search(this.rootPage, searchedKey);
    }

    private SearchedPage search(Page page, int searchedKey) {
        SearchedPage searchedPage = null;
        try (RandomAccessFile bTreeFile = new RandomAccessFile(this.FILE_NAME, "rw")) {
            int i = page.binarySearch(searchedKey);
            int keyAtPositionI = page.getKeyAtPosition(i);

            // Se a chave estiver na página, retorna um SearchedPage
            // Se não, e se não for folha, faz chamada recursiva
            // no filho a esquerda ou direita a em relação à chave mais adequada
            // Se for folha, o método acaba e retorna null no bloco finally
            if (searchedKey == keyAtPositionI) searchedPage = new SearchedPage(page, i);
            else if (!page.isLeaf()) {
                if (searchedKey > keyAtPositionI) i++;
                searchedPage = this.search(this.readPageFromDisk(page.getPointerAtPosition(i), bTreeFile), searchedKey);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return searchedPage;
        }
    }

    private Page readPageFromDisk(int pointer, RandomAccessFile btreeFile) throws IOException {
        btreeFile.seek(pointer);

        int address = btreeFile.readInt();
        int keysQuantity = btreeFile.readInt();
        int isLeaf = btreeFile.readInt();

        int[] keys = new int[this.MINIMUM_DEGREE*2-1];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = btreeFile.readInt();
        }

        int[] pointers = new int[this.MINIMUM_DEGREE*2];
        for (int i = 0; i < pointers.length; i++) {
            pointers[i] = btreeFile.readInt();
        }

        return new Page(address, keysQuantity, isLeaf, keys, pointers);

    }

    private void writePageToDisk(Page pageToBeWritten, RandomAccessFile btreeFile) throws IOException {
        btreeFile.seek(pageToBeWritten.getAddressInDisk());

        btreeFile.writeInt(pageToBeWritten.getAddressInDisk()); // Page.addressInDisk
        btreeFile.writeInt(pageToBeWritten.getKeysQuantity()); // Page.keysQuantity
        btreeFile.writeInt(pageToBeWritten.isLeaf() ? 1 : 0); // Page.isLeaf

        for (int key : pageToBeWritten.getKeys()) {
            btreeFile.writeInt(key);
        }

        for (int pointer : pageToBeWritten.getPointers()) {
            btreeFile.writeInt(pointer);
        }
    }

    public void print() {
        try (RandomAccessFile btreeFile = new RandomAccessFile(this.FILE_NAME, "r")) {
            System.out.println("------------------------");
            System.out.println("Cabeçalho");
            int minimumDegree = btreeFile.readInt();
            System.out.println("Grau mínimo: " + minimumDegree);
            int pageQuantity = btreeFile.readInt();
            System.out.println("Page's quantity: " + pageQuantity);
            int rootAddress = btreeFile.readInt();
            System.out.println("Endereço da raiz: " + rootAddress);
            System.out.println("------------------------");

            System.out.println("Páginas:");
            System.out.println();

            for (int i = 0; btreeFile.getFilePointer() < btreeFile.length(); i++) {
                System.out.println(btreeFile.getFilePointer());
                int address = btreeFile.readInt();
                System.out.println("Endereço da Página: " + address);

                int quantity = btreeFile.readInt();
                System.out.println("Quantidade de chaves: " + quantity);

                int leaf = btreeFile.readInt();
                System.out.println("É folha? " + (leaf == 1 ? "Sim" : "Não"));

                for (int j = 0; j < this.MINIMUM_DEGREE*2-1; j++) {
                    System.out.println("Chave [" + j + "]: " + btreeFile.readInt());
                }

                for (int j = 0; j < this.MINIMUM_DEGREE*2; j++) {
                    System.out.println("Ponteiro [" + j + "]: " + btreeFile.readInt());
                }

                System.out.println();
                System.out.println();
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prinTalagada() {
        try (RandomAccessFile btreeFile = new RandomAccessFile(this.FILE_NAME, "r")) {
            for (long i = 0; i < btreeFile.length(); i+=4) {
                System.out.printf("Address: %d\tValue: %d\n", i, btreeFile.readInt());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(int key) {
        try (RandomAccessFile bTreeFile = new RandomAccessFile(this.FILE_NAME, "rw")) {
            this.delete(null, this.rootPage, key, bTreeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void delete(Page parentPage, Page actualPage, int keyToDelete, RandomAccessFile bTreeFile) throws IOException {
        if (actualPage.isLeaf()) {
            // Devido a natureza preemptiva desta ÁrvoreB, o código só executará nesse ponto caso
            // a página tenha filhos suficientes. Se não esse caso, será em uma raiz
            this.deleteInLeaf(actualPage, keyToDelete, bTreeFile); // Se a chave não estiver na página, este método não faz nada
            return;
        }

        // A partir daqui, não estamos em uma folha

        // Determinamos então a posição da chave e a posição do ponteiro da raiz da subarvore que deveria conter a chave
        int keyPosition = actualPage.binarySearch(keyToDelete);
        int pointerPosition = keyPosition;

        int desiredKey = actualPage.getKeyAtPosition(keyPosition);

        // Se a chave estiver na página, realiza a remoção em nó interno
        if (desiredKey == keyToDelete) {
            this.deleteInInternPage(parentPage, actualPage, keyToDelete, keyPosition, bTreeFile);
            return;
        }

        // Se chegar até aqui, esta página não contém a chave que buscamos
        // Precisamos determinar para qual subárvore temos que fazer a chamada recursiva
        // garantindo que a página da subárvore que descermos possui pelo menos t chaves fazendo as manobras necessárias

        // Ajusta o ponteiro da raiz da subárvore
        if (keyToDelete > desiredKey) pointerPosition++;

        Page subTreeRoot = this.readPageFromDisk(actualPage.getPointerAtPosition(pointerPosition), bTreeFile);

        // Se a raiz da subarvore que queremos remover a chave não possui chaves o suficiente, precisamos
        // ou doar chaves para ela ou fazer um merge desta raiz com um de seus irmãos
        if (subTreeRoot.getKeysQuantity() == this.MINIMUM_DEGREE - 1) {

            // Primeiro precisamos ver quem será o filho que vai doar a chave. O esquerdo tem preferência
            Page leftSibling = this.getChildPage(actualPage, pointerPosition-1, bTreeFile);
            Page rightSibling = this.getChildPage(actualPage, pointerPosition+1, bTreeFile);

            boolean swapOccurred = true;

            if (!this.swapLeft(actualPage, leftSibling, subTreeRoot, keyPosition, bTreeFile))
                swapOccurred = this.swapRight(actualPage, subTreeRoot, rightSibling, keyPosition, bTreeFile);


            if (!swapOccurred) {


                /*
                * Se ambos os irmãos não possuem chaves o suficiente, o que implica no swapOccurred ser falso
                * faz-se um merge da página da raiz da sub-árvore com um desses seus irmãos. O irmão esquerdo é
                * a preferência. Pode ser que o irmão esquerdo seja nulo, então usa-se o da direita.
                * */
                if (leftSibling != null) {
                    this.merge2(actualPage, leftSibling, subTreeRoot, keyPosition, bTreeFile);
                    if (actualPage.getKeysQuantity() == 0) { // Se a página pai, ao perder sua chave, fica vazia, ela é apagada
                        this.deletePage(parentPage, actualPage, leftSibling, bTreeFile);
                    }

                    /*
                    * Como, nesse if, a antiga subTreeRoot foi esvaziada e seu conteúdo foi movido para o leftSibling,
                    * precisamos atualizar a referência da variável subTreeRoot para a nova "raiz da subárvore".
                    * */
                    subTreeRoot = leftSibling;
                } else {
                    this.merge2(actualPage, subTreeRoot, rightSibling, keyPosition, bTreeFile);
                    if (actualPage.getKeysQuantity() == 0) { // Se a página pai, ao perder sua chave, fica vazia, ela é apagada
                        this.deletePage(parentPage, actualPage, subTreeRoot, bTreeFile);
                    }
                }
            }
        }

        this.delete(actualPage, subTreeRoot, keyToDelete, bTreeFile);

    }

    private boolean swapLeft(Page parentPage, Page leftSibling, Page childPage, int keyPosition, RandomAccessFile bTreeFile) throws IOException {
        if (leftSibling == null || leftSibling.getKeysQuantity() == this.MINIMUM_DEGREE - 1) return false;

        // Movendo os ponteiros
        // Estou movendo os ponteiros primeiro pois o metodo deleteLastPointer usa o atributo keysQuantity,
        // que por sua vez é alterado ao usar o deleteLastKey
        childPage.shiftPointersRight(0);
        childPage.insertPointerAtPosition(0, leftSibling.getLastPointer());
        leftSibling.deleteLastPointer();

        // Movendo as chaves
        childPage.shiftKeysRight(0);
        childPage.insertKeyAtPosition(0, parentPage.getKeyAtPosition(keyPosition));

        parentPage.substituteKeyAtPosition(keyPosition, leftSibling.getLastKey());
        leftSibling.deleteLastKey();

        this.writePageToDisk(parentPage, bTreeFile);
        this.writePageToDisk(leftSibling, bTreeFile);
        this.writePageToDisk(childPage, bTreeFile);

        return true;
    }

    private boolean swapRight(Page parentPage, Page childPage, Page rightSibling, int keyPosition, RandomAccessFile bTreeFile) throws IOException {
        if (rightSibling == null || rightSibling.getKeysQuantity() == this.MINIMUM_DEGREE - 1) return false;

        // Movendo os ponteiros
        // Estou movendo os ponteiros primeiro pois o metodo deleteLastPointer usa o atributo keysQuantity,
        // que por sua vez é alterado ao usar o deleteLastKey
        childPage.insertPointerAtPosition(childPage.getKeysQuantity()+1, rightSibling.getPointerAtPosition(0));
        rightSibling.shiftPointersLeft(0);

        // Movendo as chaves
        childPage.insertKeyAtPosition(childPage.getKeysQuantity(), parentPage.getKeyAtPosition(keyPosition));
        parentPage.substituteKeyAtPosition(keyPosition, rightSibling.getKeyAtPosition(0));
        rightSibling.shiftKeysLeft(0);

        this.writePageToDisk(parentPage, bTreeFile);
        this.writePageToDisk(rightSibling, bTreeFile);
        this.writePageToDisk(childPage, bTreeFile);

        return true;
    }

    private Page getChildPage(Page parentPage, int i, RandomAccessFile bTreeFile) throws IOException {
        Page childPage = null;

        if (i < 0 || i >= parentPage.getPointersQuantity()) return childPage;

        childPage = this.readPageFromDisk(parentPage.getPointerAtPosition(i), bTreeFile);

        return childPage;
    }

    private void deleteInInternPage(Page parentPage, Page actualPage, int keyToDelete, int keyPosition, RandomAccessFile bTreeFile) throws IOException {
        Page leftChild = this.readPageFromDisk(actualPage.getPointerAtPosition(keyPosition), bTreeFile);
        Page rightChild = this.readPageFromDisk(actualPage.getPointerAtPosition(keyPosition+1), bTreeFile);

        // Esses ifs checarão se os filhos podem ter uma chave removida. A preferência é o da esquerda.
        if (leftChild.getKeysQuantity() >= this.MINIMUM_DEGREE) {
            int substituteKey = this.takeAndDeleteGreaterKey(leftChild, bTreeFile);
            actualPage.substituteKeyAtPosition(keyPosition, substituteKey);
            this.writePageToDisk(actualPage, bTreeFile);
            return;
        }

        if (rightChild.getKeysQuantity() >= this.MINIMUM_DEGREE) {
            int substituteKey = this.takeAndDeleteLowestKey(rightChild, bTreeFile);
            actualPage.substituteKeyAtPosition(keyPosition, substituteKey);
            this.writePageToDisk(actualPage, bTreeFile);
            return;
        }

        // Se nenhum dos filhos puder, faz o merge deles
        this.merge2(actualPage, leftChild, rightChild, keyPosition, bTreeFile);

        if (actualPage.getKeysQuantity() == 0) { // Se a página pai, ao perder sua chave, fica vazia, ela é apagada
            this.deletePage(parentPage, actualPage, leftChild, bTreeFile);
            actualPage = null;
        }

        this.delete(actualPage, leftChild, keyToDelete, bTreeFile);
    }

    private void deletePage(Page parentPage, Page actualPage, Page childPage, RandomAccessFile bTreeFile) throws IOException {
        actualPage.clear();
        this.updatePageQuantity(-1, bTreeFile); //
        this.writePageToDisk(actualPage, bTreeFile);

        // Se a página era uma raiz, o filho vira a raiz
        if (parentPage == null) {
            this.rootPage = childPage;
            this.updateRootAddress(childPage.getAddressInDisk(), bTreeFile);
        } else { // Se não o filho que sobrou assume sua posição
            int pointerIndex = parentPage.getPointerIndexOf(actualPage);
            parentPage.setPointer(pointerIndex, childPage.getAddressInDisk());
            this.writePageToDisk(parentPage, bTreeFile);
        }
    }

    private boolean isRoot(Page actualPage) {
        return actualPage.getAddressInDisk() == this.rootPage.getAddressInDisk();
    }

    private void merge(Page leftChild, Page rightChild, RandomAccessFile bTreeFile) throws IOException {

        // Move chaves
        for (int i = 0; i < this.MINIMUM_DEGREE - 1; i++) {
            int rightChildKey = rightChild.getKeyAtPosition(i);
            rightChild.deleteKeyAtPosition(i);
            leftChild.insertKeyAtPosition(i + this.MINIMUM_DEGREE - 1, rightChildKey);
        }

        // Move ponteiros
        for (int i = 0; i < this.MINIMUM_DEGREE; i++) {
            int rightChildPointer = rightChild.getPointerAtPosition(i);
            rightChild.deletePointerAtPosition(i);
            leftChild.insertPointerAtPosition(i + this.MINIMUM_DEGREE, rightChildPointer);
        }

        rightChild.clear();
        this.updatePageQuantity(-1, bTreeFile); // Diminui o contador em referência ao filho que se perdeu no merge

        this.writePageToDisk(leftChild, bTreeFile);
        this.writePageToDisk(rightChild, bTreeFile);
    }

    private void merge2(Page parentPage, Page leftChild, Page rightChild, int keyPosition, RandomAccessFile bTreeFile) throws IOException {

        leftChild.insertKeyAtPosition(this.MINIMUM_DEGREE - 1, parentPage.getKeyAtPosition(keyPosition));

        /*
        * Os ponteiros precisam ser alterados antes pois o keysQuantity altera o seu comportamento,
        * e por sua vez, o shiftKeysLeft altera o valor de keysQuantity
        * */
        parentPage.shiftPointersLeft(keyPosition +1);
        parentPage.shiftKeysLeft(keyPosition);

        // Move chaves
        for (int i = 0; i < this.MINIMUM_DEGREE - 1; i++) {
            int rightChildKey = rightChild.getKeyAtPosition(i);
            rightChild.deleteKeyAtPosition(i);
            leftChild.insertKeyAtPosition(i + this.MINIMUM_DEGREE, rightChildKey);
        }

        // Move ponteiros
        for (int i = 0; i < this.MINIMUM_DEGREE; i++) {
            int rightChildPointer = rightChild.getPointerAtPosition(i);
            rightChild.deletePointerAtPosition(i);
            leftChild.insertPointerAtPosition(i + this.MINIMUM_DEGREE, rightChildPointer);
        }

        rightChild.clear();
        this.updatePageQuantity(-1, bTreeFile); // Diminui o contador em referência ao filho que se perdeu no merge

        this.writePageToDisk(leftChild, bTreeFile);
        this.writePageToDisk(rightChild, bTreeFile);
        this.writePageToDisk(parentPage, bTreeFile);
    }

    private int takeAndDeleteLowestKey(Page actualPage, RandomAccessFile bTreeFile) throws IOException {
        int substituteKey;
        if (actualPage.isLeaf()) {
            substituteKey = actualPage.getKeyAtPosition(0);
            actualPage.shiftKeysLeft(0);
            this.writePageToDisk(actualPage, bTreeFile);
            return substituteKey;
        }

        int leftestChildPointer = actualPage.getPointerAtPosition(0);
        Page leftestChild = this.readPageFromDisk(leftestChildPointer, bTreeFile);
        return this.takeAndDeleteGreaterKey(leftestChild, bTreeFile);
    }

    private int takeAndDeleteGreaterKey(Page actualPage, RandomAccessFile bTreeFile) throws IOException {
        int substituteKey;
        if (actualPage.isLeaf()) {
            substituteKey = actualPage.getLastKey();
            actualPage.deleteLastKey();
            this.writePageToDisk(actualPage, bTreeFile);
            return substituteKey;
        }

        int rightestChildPointer = actualPage.getLastPointer();
        Page rightestChild = this.readPageFromDisk(rightestChildPointer, bTreeFile);
        return this.takeAndDeleteGreaterKey(rightestChild, bTreeFile);

    }

    private void deleteInLeaf(Page leafPage, int key, RandomAccessFile bTreeFile) throws IOException {
        int i = leafPage.binarySearch(key);

        if (leafPage.getKeyAtPosition(i) != key) return; // If the key isn't in the page

        leafPage.shiftKeysLeft(i);

        this.writePageToDisk(leafPage, bTreeFile);
    }
}

