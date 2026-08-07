package com.minbao.multiverse.manager;

public interface BailianManager {
    String generateText(String prompt, String model);
    String generateImage(String prompt);
    String detectCompliance(String imageUrl, String prompt);
    String callR1(String prompt);
    String generateVideo(String prompt);
    String tts(String text);
}
