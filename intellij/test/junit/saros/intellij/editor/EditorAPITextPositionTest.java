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

  /** The mocked editor instance provided for testing. */
  private Editor editor;

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
    editor = EasyMock.createNiceMock(Editor.class);

    //noinspection ConstantConditions
    EasyMock.expect(editor.logicalPositionToOffset(EasyMock.anyObject(LogicalPosition.class)))
        .andAnswer(logicalPositionToOffsetAnswer)
        .anyTimes();

    EasyMock.expect(editor.offsetToLogicalPosition(EasyMock.anyInt()))
        .andAnswer(offsetToLogicalPositionAnswer)
        .anyTimes();

    EasyMock.replay(editor);
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
    int startLine = 0;
    int endLine = 1;
    TextSelection textSelection = selection(startLine, 0, endLine, 1);

    lineOffsetLookupAnswers.put(startLine, 0);
    lineOffsetLookupAnswers.put(endLine, 10);

    Pair<Integer, Integer> expectedOffsets = offsets(0, 11);

    Pair<Integer, Integer> calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsMultiLineSelection() {
    int startLine = 5;
    int endLine = 9;
    TextSelection textSelection = selection(startLine, 7, endLine, 3);

    lineOffsetLookupAnswers.put(startLine, 250);
    lineOffsetLookupAnswers.put(endLine, 315);

    Pair<Integer, Integer> expectedOffsets = offsets(257, 318);

    Pair<Integer, Integer> calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsOneLineSelection() {
    int startLine = 3;
    int endLine = 3;
    TextSelection textSelection = selection(startLine, 7, endLine, 9);

    lineOffsetLookupAnswers.put(startLine, 11);

    Pair<Integer, Integer> expectedOffsets = offsets(18, 20);

    Pair<Integer, Integer> calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsEmptySelection() {
    int startLine = 7;
    int endLine = 7;
    TextSelection textSelection = selection(startLine, 5, endLine, 5);

    lineOffsetLookupAnswers.put(startLine, 23);

    Pair<Integer, Integer> expectedOffsets = offsets(28, 28);

    Pair<Integer, Integer> calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateSelectionStartSelection() {
    int offset = 0;
    int endOffset = 15;
    int startLine = 0;
    int endLine = 1;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 0);
    lineOffsetLookupAnswers.put(endLine, 8);

    TextSelection expectedSelection = selection(startLine, 0, endLine, 7);

    TextSelection calculatedSelection =
        EditorAPI.calculateSelectionPosition(editor, offset, endOffset);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionMultipleLineSelection() {
    int offset = 75;
    int endOffset = 351;
    int startLine = 9;
    int endLine = 28;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 68);
    lineOffsetLookupAnswers.put(endLine, 290);

    TextSelection expectedSelection = selection(startLine, 7, endLine, 61);

    TextSelection calculatedSelection =
        EditorAPI.calculateSelectionPosition(editor, offset, endOffset);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionOneLineSelection() {
    int offset = 85;
    int endOffset = 110;
    int startLine = 10;
    int endLine = 10;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 79);

    TextSelection expectedSelection = selection(startLine, 6, endLine, 31);

    TextSelection calculatedSelection =
        EditorAPI.calculateSelectionPosition(editor, offset, endOffset);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionEmptySelection() {
    int offset = 243;
    int endOffset = 243;
    int startLine = 98;
    int endLine = 98;

    lineNumberLookupAnswers.put(offset, startLine);
    lineOffsetLookupAnswers.put(startLine, 100);

    TextSelection expectedSelection = selection(startLine, 143, endLine, 143);

    TextSelection calculatedSelection =
        EditorAPI.calculateSelectionPosition(editor, offset, endOffset);

    assertEquals(expectedSelection, calculatedSelection);
  }
}
