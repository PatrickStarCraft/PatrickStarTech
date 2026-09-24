package com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

import static com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.NBTPredicates.*;
import static org.junit.jupiter.api.Assertions.*;

class PortNBTPredicateCheck {
    public static void main(String[] args) {
        var test = new PortNBTPredicateCheck();
        test.comparesEveryNumericTypeWithoutRequiringMatchingTagTypes();
        test.missingAndNonNumericValuesDoNotMatchNumericComparisons();
        test.nestedCompoundAndListPathsResolveAndRejectInvalidIndices();
        test.predicatesRetainTheirBehaviorAcrossJsonRoundTrips();
        test.retainsIeeeComparisonBehavior();
        test.stringEqualityInequalityAndTypeMismatches();
        test.comparisonOperatorsRespectInclusiveAndExclusiveBounds();
        test.compositePredicatesPreserveAnyAllAndNotSemantics();
        System.out.println("NBT predicates: 8 checks passed");
    }

    @Test
    void comparesEveryNumericTypeWithoutRequiringMatchingTagTypes() {
        NumericTag[] values = {ByteTag.valueOf((byte) 7), ShortTag.valueOf((short) 7), IntTag.valueOf(7),
                LongTag.valueOf(7), FloatTag.valueOf(7), DoubleTag.valueOf(7)};
        for (NumericTag value : values) {
            var data = new CompoundTag();
            data.put("amount", value);
            assertTrue(new EqualsNBTPredicate("amount", DoubleTag.valueOf(7)).test(data));
            assertFalse(new EqualsNBTPredicate("amount", IntTag.valueOf(7), true).test(data));
            assertTrue(new ComparisonNBTPredicate("amount", 6).test(data));
            assertTrue(new ComparisonNBTPredicate("amount", 8, true, false).test(data));
            assertFalse(new ComparisonNBTPredicate("amount", 7).test(data));
            assertTrue(new ComparisonNBTPredicate("amount", 7, false, true).test(data));
            assertTrue(new ComparisonNBTPredicate("amount", 7, true, true).test(data));
        }
    }

    @Test
    void missingAndNonNumericValuesDoNotMatchNumericComparisons() {
        var data = new CompoundTag();
        assertFalse(eq("missing", "x").test(data));
        assertFalse(eq("missing", 1).test(data));
        assertFalse(eq("missing", new CompoundTag()).test(data));
        assertFalse(neq("missing", "x").test(data));
        assertFalse(neq("missing", 1).test(data));
        assertFalse(neq("missing", new CompoundTag()).test(data));
        assertFalse(new ComparisonNBTPredicate("missing", 1).test(data));
        assertFalse(new EqualsNBTPredicate("missing", IntTag.valueOf(1), true).test(data));
        assertFalse(gt("num", 5).test(data));
        assertFalse(gte("num", 5).test(data));
        assertFalse(lt("num", 15).test(data));
        assertFalse(lte("num", 9).test(data));
        data.putString("amount", "7");
        assertFalse(new ComparisonNBTPredicate("amount", 6).test(data));
        assertFalse(new EqualsNBTPredicate("amount", IntTag.valueOf(7)).test(data));
        assertTrue(new EqualsNBTPredicate("amount", StringTag.valueOf("7")).test(data));
    }

    @Test
    void nestedCompoundAndListPathsResolveAndRejectInvalidIndices() {
        var entry = new CompoundTag();
        entry.putDouble("amount", 3.5);
        var list = new ListTag();
        list.add(entry);
        var data = new CompoundTag();
        data.put("tanks", list);
        assertTrue(new ComparisonNBTPredicate("tanks[0].amount", 3).test(data));
        for (String path : new String[]{"tanks[-1].amount", "tanks[1].amount", "tanks[x].amount", "tanks[0].missing"}) {
            assertNull(NBTPredicateUtils.getNestedTag(data, path));
        }
    }

    @Test
    void predicatesRetainTheirBehaviorAcrossJsonRoundTrips() {
        var data = new CompoundTag();
        data.putDouble("amount", 3.5);
        var comparison = new ComparisonNBTPredicate("amount", 3.5, true, true);
        assertTrue(ComparisonNBTPredicate.fromJson(comparison.toJson()).test(data));
        var equality = new EqualsNBTPredicate("amount", DoubleTag.valueOf(3.5), true);
        var restored = EqualsNBTPredicate.fromJson(equality.toJson());
        assertFalse(restored.test(data));
        data.putDouble("amount", 4.5);
        assertTrue(restored.test(data));
    }

    @Test
    void retainsIeeeComparisonBehavior() {
        var data = new CompoundTag();
        data.putDouble("amount", Double.NaN);
        assertFalse(new ComparisonNBTPredicate("amount", 0, false, true).test(data));
        assertFalse(new EqualsNBTPredicate("amount", DoubleTag.valueOf(Double.NaN)).test(data));
        data.putDouble("amount", Double.POSITIVE_INFINITY);
        assertTrue(new ComparisonNBTPredicate("amount", Double.MAX_VALUE).test(data));
    }

    /** Migrated from the pure-data cases in the legacy NBTPredicateTest GameTest. */
    @Test
    void stringEqualityInequalityAndTypeMismatches() {
        var data = new CompoundTag();
        data.putString("foo", "bar");

        assertTrue(eq("foo", "bar").test(data));
        assertFalse(eq("foo", "baz").test(data));
        assertFalse(eq("foo", 1).test(data));
        assertFalse(eq("foo", new CompoundTag()).test(data));
        assertFalse(neq("foo", "bar").test(data));
        assertTrue(neq("foo", "baz").test(data));
        assertTrue(neq("foo", 1).test(data));
        assertTrue(neq("foo", new CompoundTag()).test(data));
    }

    /** Migrated from the pure-data cases in the legacy NBTPredicateTest GameTest. */
    @Test
    void comparisonOperatorsRespectInclusiveAndExclusiveBounds() {
        var data = new CompoundTag();
        data.putDouble("num", 10);

        assertTrue(gt("num", 5).test(data));
        assertFalse(gt("num", 10).test(data));
        assertFalse(gt("num", 11).test(data));
        assertTrue(gte("num", 10).test(data));
        assertTrue(lte("num", 10).test(data));
        assertTrue(lt("num", 15).test(data));
        assertFalse(lt("num", 10).test(data));
        assertFalse(lte("num", 9).test(data));
    }

    /** Migrated from the pure-data cases in the legacy NBTPredicateTest GameTest. */
    @Test
    void compositePredicatesPreserveAnyAllAndNotSemantics() {
        var data = new CompoundTag();
        data.putInt("a", 5);
        data.putInt("b", 10);

        var anyMatches = any(
                new EqualsNBTPredicate("a", IntTag.valueOf(7)),
                new EqualsNBTPredicate("b", IntTag.valueOf(10)),
                eq("missing", 99));
        var allMatches = all(
                lt("a", 6),
                gt("b", 9));
        var failingAll = all(
                lt("a", 4),
                gt("b", 9));
        var failingAny = any(
                new EqualsNBTPredicate("a", IntTag.valueOf(7)),
                new EqualsNBTPredicate("b", IntTag.valueOf(11)));

        assertTrue(anyMatches.test(data));
        assertTrue(allMatches.test(data));
        assertFalse(failingAll.test(data));
        assertFalse(failingAny.test(data));
        assertFalse(not(new EqualsNBTPredicate("b", IntTag.valueOf(10))).test(data));
        assertTrue(not(new EqualsNBTPredicate("a", IntTag.valueOf(7))).test(data));
        assertTrue(NBTPredicates.fromJson(allMatches.toJson()).test(data));
    }
}
