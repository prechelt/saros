package saros.intellij.editor;

import static org.junit.Assert.assertEquals;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Pair;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import saros.editor.text.TextPosition;
import saros.editor.text.TextSelection;

/** Tests the position calculation logic of the editor api. */
public class EditorAPITextPositionTest {
  private Editor testEditor;

  /** Map to back line start offset look-ups given a line number. */
  private Map<Integer, Integer> lineOffsetLookupAnswers = new HashMap<>();
  /** Map to back line number look-ups given an offset. */
  private Map<Integer, Integer> lineNumberLookupAnswers = new HashMap<>();

  /**
   * Logic mocking {@link Editor#logicalPositionToOffset(LogicalPosition)} calls by using the {@link
   * #lineOffsetLookupAnswers} map.
   */
  private IAnswer<Integer> logicalPositionToOffsetAnswer =
      () -> {
        LogicalPosition argument = (LogicalPosition) EasyMock.getCurrentArguments()[0];

        int line = argument.line;

        if (!lineOffsetLookupAnswers.containsKey(line)) {
          throw new IllegalStateException(
              "Line offset to return was not set up for line " + argument);
        }

        return lineOffsetLookupAnswers.get(line);
      };

  /**
   * Logic mocking {@link Editor#offsetToLogicalPosition(int)} calls by using the {@link
   * #lineNumberLookupAnswers} map.
   */
  private IAnswer<LogicalPosition> offsetToLogicalPositionAnswer =
      () -> {
        int argument = (int) EasyMock.getCurrentArguments()[0];

        if (!lineNumberLookupAnswers.containsKey(argument)) {
          throw new IllegalStateException(
              "Line number to return was not set up for line " + argument);
        }

        return new LogicalPosition(lineNumberLookupAnswers.get(argument), 0);
      };

  @Before
  public void setUp() {
    testEditor = EasyMock.createNiceMock(Editor.class);

    //noinspection ConstantConditions
    EasyMock.expect(testEditor.logicalPositionToOffset(EasyMock.anyObject(LogicalPosition.class)))
        .andAnswer(logicalPositionToOffsetAnswer)
        .anyTimes();

    EasyMock.expect(testEditor.offsetToLogicalPosition(EasyMock.anyInt()))
        .andAnswer(offsetToLogicalPositionAnswer)
        .anyTimes();

    EasyMock.replay(testEditor);
  }

  @After
  public void tearDown() {
    lineNumberLookupAnswers.clear();
    lineOffsetLookupAnswers.clear();
  }

  /**
   * Ease of use method to instantiate a text selection with the given parameters.
   *
   * @param startLine the start line
   * @param startInLineOffset the start in-line offset
   * @param endLine the end line
   * @param endInLineOffset the end in-line offset
   * @return a text selection with the given parameters
   * @see TextPosition#TextPosition(int, int)
   * @see TextSelection#TextSelection(TextPosition, TextPosition)
   */
  private TextSelection selection(
      int startLine, int startInLineOffset, int endLine, int endInLineOffset) {

    return new TextSelection(
        new TextPosition(startLine, startInLineOffset), new TextPosition(endLine, endInLineOffset));
  }

  /**
   * Ease of use method to instantiate a <code>Pair</code> object holding the given start and end
   * offset.
   *
   * @param startOffset the start offset
   * @param endOffset the end offset
   * @return a <code>Pair</code> object holding the given start and end offset
   */
  private Pair<Integer, Integer> offsets(int startOffset, int endOffset) {
    return new Pair<>(startOffset, endOffset);
  }

  @Test
  public void testCalculateOffsetsStartSelection() {
    TextSelection textSelection;
    Pair<Integer, Integer> expectedOffsets;
    Pair<Integer, Integer> calculatedOffsets;

    textSelection = selection(0, 0, 1, 1);
    lineOffsetLookupAnswers.put(0, 0);
    lineOffsetLookupAnswers.put(1, 10);
    expectedOffsets = offsets(0, 11);

    calculatedOffsets = EditorAPI.calculateOffsets(testEditor, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsMultiLineSelection() {
    TextSelection textSelection;
    Pair<Integer, Integer> expectedOffsets;
    Pair<Integer, Integer> calculatedOffsets;

    textSelection = selection(5, 7, 9, 3);
    lineOffsetLookupAnswers.put(5, 250);
    lineOffsetLookupAnswers.put(9, 315);
    expectedOffsets = offsets(257, 318);

    calculatedOffsets = EditorAPI.calculateOffsets(testEditor, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsOneLineSelection() {
    TextSelection textSelection;
    Pair<Integer, Integer> expectedOffsets;
    Pair<Integer, Integer> calculatedOffsets;

    textSelection = selection(3, 7, 3, 9);
    lineOffsetLookupAnswers.put(3, 11);
    expectedOffsets = offsets(18, 20);

    calculatedOffsets = EditorAPI.calculateOffsets(testEditor, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsEmptySelection() {
    TextSelection textSelection;
    Pair<Integer, Integer> expectedOffsets;
    Pair<Integer, Integer> calculatedOffsets;

    textSelection = selection(7, 5, 7, 5);
    lineOffsetLookupAnswers.put(7, 23);
    expectedOffsets = offsets(28, 28);

    calculatedOffsets = EditorAPI.calculateOffsets(testEditor, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateSelectionStartSelection() {
    int offset;
    int endOffset;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 0;
    endOffset = 15;

    lineNumberLookupAnswers.put(0, 0);
    lineNumberLookupAnswers.put(15, 1);
    lineOffsetLookupAnswers.put(0, 0);
    lineOffsetLookupAnswers.put(1, 8);

    expectedSelection = selection(0, 0, 1, 7);

    calculatedSelection = EditorAPI.calculateSelectionPosition(testEditor, offset, endOffset);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionMultipleLineSelection() {
    int offset;
    int endOffset;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 75;
    endOffset = 351;

    lineNumberLookupAnswers.put(75, 9);
    lineNumberLookupAnswers.put(351, 28);
    lineOffsetLookupAnswers.put(9, 68);
    lineOffsetLookupAnswers.put(28, 290);

    expectedSelection = selection(9, 7, 28, 61);

    calculatedSelection = EditorAPI.calculateSelectionPosition(testEditor, offset, endOffset);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionOneLineSelection() {
    int offset;
    int endOffset;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 85;
    endOffset = 110;

    lineNumberLookupAnswers.put(85, 10);
    lineNumberLookupAnswers.put(110, 10);
    lineOffsetLookupAnswers.put(10, 79);

    expectedSelection = selection(10, 6, 10, 31);

    calculatedSelection = EditorAPI.calculateSelectionPosition(testEditor, offset, endOffset);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionEmptySelection() {
    int offset;
    int endOffset;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 243;
    endOffset = 243;

    lineNumberLookupAnswers.put(243, 98);
    lineOffsetLookupAnswers.put(98, 100);

    expectedSelection = selection(98, 143, 98, 143);

    calculatedSelection = EditorAPI.calculateSelectionPosition(testEditor, offset, endOffset);
    assertEquals("", expectedSelection, calculatedSelection);
  }
}
