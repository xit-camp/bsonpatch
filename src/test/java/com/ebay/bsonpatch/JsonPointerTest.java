package com.ebay.bsonpatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bson.BsonArray;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.junit.Test;


public class JsonPointerTest {

    // Parsing tests --

    @Test
    public void parsesEmptyStringAsRoot() {
        JsonPointer parsed = JsonPointer.parse("");
        assertTrue(parsed.isRoot());
    }

    @Test
    public void parsesSingleSlashAsFieldReference() {
        JsonPointer parsed = JsonPointer.parse("/");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
        assertEquals("", parsed.get(0).getField());
    }

    @Test
    public void parsesArrayIndirection() {
        JsonPointer parsed = JsonPointer.parse("/0");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertTrue(parsed.get(0).isArrayIndex());
        assertEquals(0, parsed.get(0).getIndex());
    }

    @Test
    public void parsesArrayTailIndirection() {
        JsonPointer parsed = JsonPointer.parse("/-");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertTrue(parsed.get(0).isArrayIndex());
        assertEquals(JsonPointer.LAST_INDEX, parsed.get(0).getIndex());
    }

    @Test
    public void parsesMultipleArrayIndirection() {
        JsonPointer parsed = JsonPointer.parse("/0/1");
        assertFalse(parsed.isRoot());
        assertEquals(2, parsed.size());
        assertTrue(parsed.get(0).isArrayIndex());
        assertEquals(0, parsed.get(0).getIndex());
        assertTrue(parsed.get(1).isArrayIndex());
        assertEquals(1, parsed.get(1).getIndex());
    }

    @Test
    public void parsesArrayIndirectionsWithLeadingZeroAsObjectIndirections() {
        JsonPointer parsed = JsonPointer.parse("/0123");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
    }

    @Test
    public void parsesArrayIndirectionsWithNegativeOffsetAsObjectIndirections() {
        JsonPointer parsed = JsonPointer.parse("/-3");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
    }

    @Test
    public void parsesObjectIndirections() {
        JsonPointer parsed = JsonPointer.parse("/a");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
        assertEquals("a", parsed.get(0).getField());
    }

    @Test
    public void parsesMultipleObjectIndirections() {
        JsonPointer parsed = JsonPointer.parse("/a/b");
        assertFalse(parsed.isRoot());
        assertEquals(2, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
        assertEquals("a", parsed.get(0).getField());
        assertFalse(parsed.get(1).isArrayIndex());
        assertEquals("b", parsed.get(1).getField());
    }

    @Test
    public void parsesMixedIndirections() {
        JsonPointer parsed = JsonPointer.parse("/0/a/1/b");
        assertFalse(parsed.isRoot());
        assertEquals(4, parsed.size());
        assertTrue(parsed.get(0).isArrayIndex());
        assertEquals(0, parsed.get(0).getIndex());
        assertFalse(parsed.get(1).isArrayIndex());
        assertEquals("a", parsed.get(1).getField());
        assertTrue(parsed.get(2).isArrayIndex());
        assertEquals(1, parsed.get(2).getIndex());
        assertFalse(parsed.get(3).isArrayIndex());
        assertEquals("b", parsed.get(3).getField());
    }

    @Test
    public void parsesEscapedTilde() {
        JsonPointer parsed = JsonPointer.parse("/~0");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
        assertEquals("~", parsed.get(0).getField());
    }

    @Test
    public void parsesEscapedForwardSlash() {
        JsonPointer parsed = JsonPointer.parse("/~1");
        assertFalse(parsed.isRoot());
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0).isArrayIndex());
        assertEquals("/", parsed.get(0).getField());
    }

    // Parsing error conditions --

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingLeadingSlash() {
        JsonPointer.parse("illegal/reftoken");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnInvalidEscapedSequence1() {
        JsonPointer.parse("/~2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnInvalidEscapedSequence2() {
        JsonPointer.parse("/~a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void trailingTildeCorrectException() {
        JsonPointer.parse("/a/b/c~");
    }

    // Evaluation tests --

    @Test
    public void evaluatesAccordingToRFC6901() throws IOException, JsonPointerEvaluationException {
        // Tests resolution according to https://tools.ietf.org/html/rfc6901#section-5

        BsonValue data = TestUtils.loadResourceAsBsonValue("/rfc6901/data.json");
        BsonValue testData = data.asDocument().get("testData");

        assertEquals(BsonArray.parse("[\"bar\", \"baz\"]"), JsonPointer.parse("/foo").evaluate(testData));
        assertEquals(new BsonString("bar"), JsonPointer.parse("/foo/0").evaluate(testData));
        assertEquals(new BsonInt32(0), JsonPointer.parse("/").evaluate(testData));
        assertEquals(new BsonInt32(1), JsonPointer.parse("/a~1b").evaluate(testData));
        assertEquals(new BsonInt32(2), JsonPointer.parse("/c%d").evaluate(testData));
        assertEquals(new BsonInt32(3), JsonPointer.parse("/e^f").evaluate(testData));
        assertEquals(new BsonInt32(4), JsonPointer.parse("/g|h").evaluate(testData));
        assertEquals(new BsonInt32(5), JsonPointer.parse("/i\\j").evaluate(testData));
        assertEquals(new BsonInt32(6), JsonPointer.parse("/k\"l").evaluate(testData));
        assertEquals(new BsonInt32(7), JsonPointer.parse("/ ").evaluate(testData));
        assertEquals(new BsonInt32(8), JsonPointer.parse("/m~0n").evaluate(testData));
    }

    // Utility methods --

    @Test
    public void rendersRootToEmptyString() {
        assertEquals("", JsonPointer.ROOT.toString());
    }

    @Test
    public void symmetricalInParsingAndRendering() {
        assertEquals("/foo", JsonPointer.parse("/foo").toString());
        assertEquals("/foo/0", JsonPointer.parse("/foo/0").toString());
        assertEquals("/", JsonPointer.parse("/").toString());
        assertEquals("/a~1b", JsonPointer.parse("/a~1b").toString());
        assertEquals("/c%d", JsonPointer.parse("/c%d").toString());
        assertEquals("/e^f", JsonPointer.parse("/e^f").toString());
        assertEquals("/g|h", JsonPointer.parse("/g|h").toString());
        assertEquals("/i\\j", JsonPointer.parse("/i\\j").toString());
        assertEquals("/k\"l", JsonPointer.parse("/k\"l").toString());
        assertEquals("/ ", JsonPointer.parse("/ ").toString());
        assertEquals("/m~0n", JsonPointer.parse("/m~0n").toString());
    }
}
