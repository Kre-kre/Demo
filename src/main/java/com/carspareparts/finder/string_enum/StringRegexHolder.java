package com.carspareparts.finder.string_enum;

public enum StringRegexHolder {
    REPLACE_TEXT("[а-яА-Яії: ?a-zA-Z;&]+"),
    REPLACE_HYPHEN_PRICE("(\\-\\d+)"),
    ;
    private final String path;

    StringRegexHolder(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}