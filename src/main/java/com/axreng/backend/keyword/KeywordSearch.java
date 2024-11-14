package com.axreng.backend.keyword;

public class KeywordSearch {
    private String keyword;

    public KeywordSearch() {}
    public KeywordSearch(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
