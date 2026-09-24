package com.gregtechceu.gtceu.gametest;

import com.gregtechceu.gtceu.gametest.example.ExampleTest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestFunctionLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Explicit 26.2 GameTest registration for test-source-only callbacks. */
@EventBusSubscriber
public final class GTGameTestRegistration extends TestFunctionLoader {

    private static final String GT_NAMESPACE = "gtceu";
    private static final Identifier DEFAULT_BATCH = Identifier.fromNamespaceAndPath(GT_NAMESPACE, "default_batch");

    /** The test and function identifiers are explicit in the 26.2 registry. */
    private static final List<RegisteredTest> TESTS = List.of(
            new RegisteredTest(
                    Identifier.fromNamespaceAndPath(GT_NAMESPACE, "my_test"),
                    Identifier.fromNamespaceAndPath(GT_NAMESPACE, "empty"),
                    DEFAULT_BATCH,
                    100,
                    ExampleTest::myTest));

    static {
        // FML initializes event subscribers during mod construction. Vanilla
        // consumes registered loaders while bootstrapping test functions; the
        // GameTestServer smoke launch must confirm this lifecycle at runtime.
        TestFunctionLoader.registerLoader(new GTGameTestRegistration());
    }

    private GTGameTestRegistration() {}

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (RegisteredTest test : TESTS) {
            Holder<TestEnvironmentDefinition<?>> environment = environments.computeIfAbsent(
                    test.batch(), batch -> event.registerEnvironment(batch, new TestEnvironmentDefinition.AllOf(List.of())));
            TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                    environment, test.structure(), test.maxTicks(), 0, true);
            event.registerTest(test.id(), new FunctionGameTestInstance(test.functionKey(), testData));
        }
    }

    @Override
    public void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> register) {
        for (RegisteredTest test : TESTS) {
            register.accept(test.functionKey(), test.function());
        }
    }

    private record RegisteredTest(
            Identifier id,
            Identifier structure,
            Identifier batch,
            int maxTicks,
            Consumer<GameTestHelper> function) {

        private ResourceKey<Consumer<GameTestHelper>> functionKey() {
            return ResourceKey.create(Registries.TEST_FUNCTION, this.id);
        }
    }
}
