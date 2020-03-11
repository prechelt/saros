package saros.editor.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import saros.editor.text.TextPosition;
import saros.editor.text.TextSelection;

/** Tests the position calculation logic of the editor api. */
public class EditorAPITextPositionTest {
  private IDocument testDocument;
  private ITextEditor editorPart;

  /** Map to back line start offset look-ups given a line number. */
  private Map<Integer, Integer> lineOffsetLookupAnswers = new HashMap<>();
  /** Map to back line number look-ups given an offset. */
  private Map<Integer, Integer> lineNumberLookupAnswers = new HashMap<>();

  /**
   * Logic mocking {@link IDocument#getLineOffset(int)} calls by using the {@link
   * #lineOffsetLookupAnswers} map.
   */
  private IAnswer<Integer> getLineOffsetAnswer =
      () -> {
        int argument = (int) EasyMock.getCurrentArguments()[0];

        if (!lineOffsetLookupAnswers.containsKey(argument)) {
          throw new IllegalStateException(
              "Line offset to return was not set up for line " + argument);
        }

        return lineOffsetLookupAnswers.get(argument);
      };

  /**
   * Logic mocking {@link IDocument#getLineOfOffset(int)} calls by using the {@link
   * #lineNumberLookupAnswers} map.
   */
  private IAnswer<Integer> getLineOfOffsetAnswer =
      () -> {
        int argument = (int) EasyMock.getCurrentArguments()[0];

        if (!lineNumberLookupAnswers.containsKey(argument)) {
          throw new IllegalStateException(
              "Line number to return was not set up for offset " + argument);
        }

        return lineNumberLookupAnswers.get(argument);
      };

  @Before
  public void setUp() throws BadLocationException {
    testDocument = EasyMock.createNiceMock(IDocument.class);

    EasyMock.expect(testDocument.getLineOffset(EasyMock.anyInt()))
        .andAnswer(getLineOffsetAnswer)
        .anyTimes();

    EasyMock.expect(testDocument.getLineOfOffset(EasyMock.anyInt()))
        .andAnswer(getLineOfOffsetAnswer)
        .anyTimes();

    EasyMock.replay(testDocument);

    IEditorInput editorInput = EasyMock.createNiceMock(IEditorInput.class);
    EasyMock.replay(editorInput);

    IDocumentProvider documentProvider = EasyMock.createNiceMock(IDocumentProvider.class);
    EasyMock.expect(documentProvider.getDocument(editorInput)).andReturn(testDocument).anyTimes();
    EasyMock.replay(documentProvider);

    editorPart = EasyMock.createNiceMock(ITextEditor.class);
    EasyMock.expect(editorPart.getDocumentProvider()).andReturn(documentProvider).anyTimes();
    EasyMock.expect(editorPart.getEditorInput()).andReturn(editorInput).anyTimes();
    EasyMock.replay(editorPart);
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
   * Ease of use method to instantiate an <code>ITextSelection</code> object holding the given start
   * offset and the length calculated using the given endOffset.
   *
   * @param startOffset the start offset
   * @param endOffset the end offset
   * @return an <code>ITextSelection</code> object holding the given start offset and the length
   *     calculated using the given endOffset
   */
  private ITextSelection offsets(int startOffset, int endOffset) {
    int length = endOffset - startOffset;

    return new org.eclipse.jface.text.TextSelection(startOffset, length);
  }

  @Test
  public void testCalculateOffsetsStartSelection() {
    TextSelection textSelection;
    ITextSelection expectedOffsets;
    ITextSelection calculatedOffsets;

    textSelection = selection(0, 0, 1, 1);
    lineOffsetLookupAnswers.put(0, 0);
    lineOffsetLookupAnswers.put(1, 10);
    expectedOffsets = offsets(0, 11);

    calculatedOffsets = EditorAPI.calculateOffsets(editorPart, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsMultiLineSelection() {
    TextSelection textSelection;
    ITextSelection expectedOffsets;
    ITextSelection calculatedOffsets;

    textSelection = selection(5, 7, 9, 3);
    lineOffsetLookupAnswers.put(5, 250);
    lineOffsetLookupAnswers.put(9, 315);
    expectedOffsets = offsets(257, 318);

    calculatedOffsets = EditorAPI.calculateOffsets(editorPart, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsOneLineSelection() {
    TextSelection textSelection;
    ITextSelection expectedOffsets;
    ITextSelection calculatedOffsets;

    textSelection = selection(3, 7, 3, 9);
    lineOffsetLookupAnswers.put(3, 11);
    expectedOffsets = offsets(18, 20);

    calculatedOffsets = EditorAPI.calculateOffsets(editorPart, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsEmptySelection() {
    TextSelection textSelection;
    ITextSelection expectedOffsets;
    ITextSelection calculatedOffsets;

    textSelection = selection(7, 5, 7, 5);
    lineOffsetLookupAnswers.put(7, 23);
    expectedOffsets = offsets(28, 28);

    calculatedOffsets = EditorAPI.calculateOffsets(editorPart, textSelection);
    assertEquals("", expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateSelectionStartSelection() {
    int offset;
    int endOffset;
    int length;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 0;
    endOffset = 15;
    length = endOffset - offset;

    lineNumberLookupAnswers.put(0, 0);
    lineNumberLookupAnswers.put(15, 1);
    lineOffsetLookupAnswers.put(0, 0);
    lineOffsetLookupAnswers.put(1, 8);

    expectedSelection = selection(0, 0, 1, 7);

    calculatedSelection = EditorAPI.calculateSelection(testDocument, offset, length);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionMultipleLineSelection() {
    int offset;
    int endOffset;
    int length;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 75;
    endOffset = 351;
    length = endOffset - offset;

    lineNumberLookupAnswers.put(75, 9);
    lineNumberLookupAnswers.put(351, 28);
    lineOffsetLookupAnswers.put(9, 68);
    lineOffsetLookupAnswers.put(28, 290);

    expectedSelection = selection(9, 7, 28, 61);

    calculatedSelection = EditorAPI.calculateSelection(testDocument, offset, length);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionOneLineSelection() {
    int offset;
    int endOffset;
    int length;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 85;
    endOffset = 110;
    length = endOffset - offset;

    lineNumberLookupAnswers.put(85, 10);
    lineNumberLookupAnswers.put(110, 10);
    lineOffsetLookupAnswers.put(10, 79);

    expectedSelection = selection(10, 6, 10, 31);

    calculatedSelection = EditorAPI.calculateSelection(testDocument, offset, length);
    assertEquals("", expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionEmptySelection() {
    int offset;
    int endOffset;
    int length;
    TextSelection expectedSelection;
    TextSelection calculatedSelection;

    offset = 243;
    endOffset = 243;
    length = endOffset - offset;

    lineNumberLookupAnswers.put(243, 98);
    lineOffsetLookupAnswers.put(98, 100);

    expectedSelection = selection(98, 143, 98, 143);

    calculatedSelection = EditorAPI.calculateSelection(testDocument, offset, length);
    assertEquals("", expectedSelection, calculatedSelection);
  }
}
