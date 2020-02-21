package com.kritsin.simplezip;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            SimpleZip.archive("d:\\arc.zip", "d:\\222.txt", "d:\\111.txt","d:\\333.txt", "d:\\com_intervale_ibt_issue_crash_5D.txt", "d:\\CODING_INTERVIEW.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
