package BTree;

import java.util.Arrays;

public class Page {

    /* Page's size:
    *  3 default integers: 12 bytes +
    *  (2 * t - 1) key integers: (2 * t - 1) * 4 bytes +
    *  (2 * t) pointer integers: (2 * t) * 4 bytes
    *  = 8 * (2 * t - 1)
    * */

    private int addressInDisk;
    private int keysQuantity;
    private int isLeaf; // 0 or 1
    private int[] keys;
    private int[] pointers;

    Page(int addressInDisk, int keysQuantity, int isLeaf, int[] keys, int[] pointers) {
        this.addressInDisk = addressInDisk;
        this.keysQuantity = keysQuantity;
        this.isLeaf = isLeaf;
        this.keys = keys;
        this.pointers = pointers;
    }

    Page(int address, boolean isLeaf, int minimumDegree) {
        this.addressInDisk = address;
        this.isLeaf = isLeaf ? 1 : 0;
        this.keys = new int[minimumDegree * 2 - 1];
        this.pointers = new int[minimumDegree * 2];
    }

    int[] getPointers() {
        return this.pointers;
    }

    int getPointerAtPosition(int position) {
        return this.pointers[position];
    }

    int[] getKeys() {
        return keys;
    }

    int getKeyAtPosition(int position) {
        return this.keys[position];
    }

    void insertKeyAtPosition(int position, int key) {
        this.keys[position] = key;
        this.keysQuantity++;
    }

    void increaseKeysQuantityByOne() {
        this.keysQuantity++;
    }

    int getKeysQuantity() {
        return this.keysQuantity;
    }

    int getPointersQuantity() {
        return this.keysQuantity + 1;
    }

    int getAddressInDisk() {
        return this.addressInDisk;
    }

    boolean isLeaf() {
        return this.isLeaf == 1;
    }

    boolean isFull(int minimumDegree) {
        return this.keysQuantity == minimumDegree * 2 - 1;
    }

    void insertPointerAtPosition(int position, int pointer) {
       this.pointers[position] = pointer;
    }

    int binarySearch(int key) {
        int min = 0;
        int max = this.keysQuantity - 1;
        int middle;

        while (min <= max) {
            middle = (max + min) / 2;

            if (key == this.keys[middle]) return middle;

            if (key > this.keys[middle]) min = middle + 1;
            else max = middle - 1;
        }

        // Essa adaptação faz com que o retorno indique ao menos a posição do filho
        // que deveria conter a chave procurada.
        return (max + min) / 2;
    }

    void decreaseKeysQuantityByOne() {
        this.keysQuantity--;
    }

    int getLastKeyIndex() {
        return this.keysQuantity-1;
    }

    int getLastKey() {
        return this.keys[this.keysQuantity-1];
    }

    int getLastPointer() {
        return this.pointers[this.keysQuantity];
    }

    void deleteLastKey() {
        this.deleteKeyAtPosition(this.getKeysQuantity() - 1);
    }

    void deleteKeyAtPosition(int position) {
        this.keys[position] = 0;
        this.keysQuantity--;
    }

    void deletePointerAtPosition(int position) {
        this.pointers[position] = 0;
    }

    void copyPointerToRight(int position) {
        this.pointers[position+1] = this.pointers[position];
    }

    void copyKeyToRight(int position) {
        this.keys[position+1] = this.keys[position];
    }

    void moveRightPointerToPosition(int position) {
        this.pointers[position] = this.pointers[position+1];
        this.pointers[position+1] = 0;
    }

    void moveRightKeyToPosition(int position) {
        this.keys[position] = this.keys[position+1];
        this.keys[position+1] = 0;
        this.keysQuantity--;
    }

    void shiftKeysLeft(int position) {
        this.shiftLeft(this.keys, this.keysQuantity, position);
        this.keysQuantity--;
    }

    void shiftPointersLeft(int position) {
        this.shiftLeft(this.pointers, this.keysQuantity+1, position);
    }

    private void shiftLeft(int[] arr, int quantity, int position) {
        int quantityOfValuesToBeShifted = quantity - position - 1;

        if (quantityOfValuesToBeShifted == 0) arr[position] = 0;
        else for (int i = 0; i < quantityOfValuesToBeShifted; i++, position++) {
            arr[position] = arr[position+1];
            arr[position+1] = 0;
        }
    }

    void shiftKeysRight(int position) {
        this.shiftRight(this.keys, this.keysQuantity, position);
    }

    void shiftPointersRight(int position) {
        this.shiftRight(this.pointers, this.keysQuantity+1, position);
    }

    private void shiftRight(int[] arr, int quantity, int position) {
        if (arr[position] == 0) return;
        // Esse método nunca será usando em arrays cheios
        for (int i = quantity - 1; i >= position; i--) {
            arr[i+1] = arr[i];
            arr[i] = 0;
        }
    }

    void deleteLastPointer() {
        this.pointers[this.keysQuantity] = 0;
    }

    void clear() {
        this.keysQuantity = 0;
        this.isLeaf = 0;
        Arrays.fill(this.keys, 0);
        Arrays.fill(this.pointers, 0);
    }

    void substituteKeyAtPosition(int position, int substituteKey) {
        this.keys[position] = substituteKey;
    }

    int getPointerIndexOf(Page child) {
        int index = -1;
        for (int i = 0; i < this.keysQuantity + 1; i++) {
            if (child.getAddressInDisk() == this.pointers[i]) index = i; break;
        }
        return index;
    }

    void setPointer(int pointerIndex, int addressInDisk) {
        this.pointers[pointerIndex] = addressInDisk;
    }

}
