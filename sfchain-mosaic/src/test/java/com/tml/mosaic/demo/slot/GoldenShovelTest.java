package com.tml.mosaic.demo.slot;

import com.tml.mosaic.tool.GoldenShovel;

public class GoldenShovelTest {

    public static void main(String[] args) {
        GoldenShovel.dig()
                .slotName("cs2.hack")
                .executeInfo("autoAim", "AI自瞄", "DeepSeek", "头部优先")
                .execute();
    }
}
