package BTree;

public class SearchedPage {
    private Page page;
    private int i;


    public Page getPage() {
        return this.page;
    }

    public int getIndex() {
        return this.i;
    }

    public int getSearchedKey() {
        return this.page.getKeyAtPosition(this.i);
    }

    public SearchedPage(Page page, int i) {
        this.page = page;
        this.i = i;
    }
}
