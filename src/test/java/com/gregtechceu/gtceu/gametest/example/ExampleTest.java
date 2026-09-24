package com.gregtechceu.gtceu.gametest.example;

import net.minecraft.gametest.framework.GameTestHelper;

public class ExampleTest {

    public static void myTest(GameTestHelper helper) {
        helper.assertTrue(true, "true is false");
        helper.succeed();
    }
}
