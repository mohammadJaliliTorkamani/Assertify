package org.example;

/***
 * a class for configuring the type of repositories expected to be cloned
 */
public class SearchURL {
    /**
     * maximum number of files displayed in each page of the URL (grep.app/search)
     */
    public final static int PAGE_LIMIT = 10;
    private final static String DEFAULT_SEARCH_BASE_URL = "https://grep.app/search";
    private final static int DEFAULT_PAGE_NUMBER = 1;
    private final String base_url;
    private final boolean caseSensitive;
    private final boolean regularExpression;
    private final boolean wholeWords;
    private final String[] languages;
    private int pageNumber;
    private String codePath;
    private String keywordQuery;

    public SearchURL(boolean caseSensitive, boolean regularExpression, boolean wholeWords, String[] languages,
                     String codePath, String keywordQuery) {
        this.base_url = DEFAULT_SEARCH_BASE_URL;
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.wholeWords = wholeWords;
        this.languages = languages;
        this.codePath = codePath;
        this.keywordQuery = keywordQuery;
    }

    public void setSubDirToCodePath(String subDirName) {
        if (subDirName.equals("src/main/java/")) {
            this.codePath += (subDirName + "/");
        } else {
            String[] dirs = this.codePath.split("/");
            this.codePath = dirs[0] + "/" + dirs[1] + "/" + dirs[2] + "/" + subDirName + "/";
        }
    }//

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * resets the page number to the default value
     *
     * @return the reset value
     */
    public int resetPageNumber() {
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        return pageNumber;
    }

    /**
     * check whether the codePath is empty, and if not, creates a query part to be embedded inside the URL
     *
     * @return query in format:   &filter[path][0]=codePath
     */
    private String getFormattedCodePath() {
        return codePath != null ? "&filter[path][0]=" + codePath : "";
    }

    @Override
    public String toString() {
        return String.format("%s?current=%s&q=%s&regexp=%s&case=%s&words=%s" + getFormattedCodePath() + getFormattedLanguages(),
                base_url,
                pageNumber,
                keywordQuery,
                regularExpression,
                Boolean.toString(caseSensitive).toLowerCase(),
                wholeWords);
    }

    /**
     * check whether the languages are empty, and if not, creates a query part to be embedded inside the URL
     *
     * @return query in format:   &filter[lang][INDEX]=LANGUAGE
     */
    private String getFormattedLanguages() {
        if (languages == null || languages.length == 0)
            return "";

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < languages.length; i++)
            str.append(String.format("&filter[lang][%d]=%s", i, languages[i]));

        return str.toString();
    }

    /**
     * increase the pageNumber by one
     */
    public void increasePageNumber() {
        setPageNumber(getPageNumber() + 1);
    }

    public void setKeywordQuery(String keyword) {
        this.keywordQuery = keyword;
    }
}
