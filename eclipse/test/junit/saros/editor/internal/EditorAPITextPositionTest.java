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

  /** The mocked document instance provided for testing. */
  private IDocument document;

  /** The mocked text editor instance provided for testing. */
  private ITextEditor editor;

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
    document = EasyMock.createNiceMock(IDocument.class);

    EasyMock.expect(document.getLineOffset(EasyMock.anyInt()))
        .andAnswer(getLineOffsetAnswer)
        .anyTimes();

    EasyMock.expect(document.getLineOfOffset(EasyMock.anyInt()))
        .andAnswer(getLineOfOffsetAnswer)
        .anyTimes();

    EasyMock.replay(document);

    IEditorInput editorInput = EasyMock.createNiceMock(IEditorInput.class);
    EasyMock.replay(editorInput);

    IDocumentProvider documentProvider = EasyMock.createNiceMock(IDocumentProvider.class);
    EasyMock.expect(documentProvider.getDocument(editorInput)).andReturn(document).anyTimes();
    EasyMock.replay(documentProvider);

    editor = EasyMock.createNiceMock(ITextEditor.class);
    EasyMock.expect(editor.getDocumentProvider()).andReturn(documentProvider).anyTimes();
    EasyMock.expect(editor.getEditorInput()).andReturn(editorInput).anyTimes();
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
    int startLine = 0;
    int endLine = 1;
    TextSelection textSelection = selection(startLine, 0, endLine, 1);

    lineOffsetLookupAnswers.put(startLine, 0);
    lineOffsetLookupAnswers.put(endLine, 10);

    ITextSelection expectedOffsets = offsets(0, 11);

    ITextSelection calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsMultiLineSelection() {
    int startLine = 5;
    int endLine = 9;
    TextSelection textSelection = selection(startLine, 7, endLine, 3);

    lineOffsetLookupAnswers.put(startLine, 250);
    lineOffsetLookupAnswers.put(endLine, 315);

    ITextSelection expectedOffsets = offsets(257, 318);

    ITextSelection calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsOneLineSelection() {
    int startLine = 3;
    int endLine = 3;
    TextSelection textSelection = selection(startLine, 7, endLine, 9);

    lineOffsetLookupAnswers.put(startLine, 11);

    ITextSelection expectedOffsets = offsets(18, 20);

    ITextSelection calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsEmptySelection() {
    int startLine = 7;
    int endLine = 7;
    TextSelection textSelection = selection(startLine, 5, endLine, 5);

    lineOffsetLookupAnswers.put(startLine, 23);

    ITextSelection expectedOffsets = offsets(28, 28);

    ITextSelection calculatedOffsets = EditorAPI.calculateOffsets(editor, textSelection);

    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateSelectionStartSelection() {
    int offset = 0;
    int endOffset = 15;
    int length = endOffset - offset;
    int startLine = 0;
    int endLine = 1;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 0);
    lineOffsetLookupAnswers.put(endLine, 8);

    TextSelection expectedSelection = selection(startLine, 0, endLine, 7);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionMultipleLineSelection() {
    int offset = 75;
    int endOffset = 351;
    int length = endOffset - offset;
    int startLine = 9;
    int endLine = 28;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 68);
    lineOffsetLookupAnswers.put(endLine, 290);

    TextSelection expectedSelection = selection(startLine, 7, endLine, 61);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionOneLineSelection() {
    int offset = 85;
    int endOffset = 110;
    int length = endOffset - offset;
    int startLine = 10;
    int endLine = 10;

    lineNumberLookupAnswers.put(offset, startLine);
    lineNumberLookupAnswers.put(endOffset, endLine);
    lineOffsetLookupAnswers.put(startLine, 79);

    TextSelection expectedSelection = selection(startLine, 6, endLine, 31);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);

    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionEmptySelection() {
    int offset = 243;
    int endOffset = 243;
    int length = endOffset - offset;
    int startLine = 98;
    int endLine = 98;

    lineNumberLookupAnswers.put(offset, startLine);
    lineOffsetLookupAnswers.put(startLine, 100);

    TextSelection expectedSelection = selection(startLine, 143, endLine, 143);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);

    assertEquals(expectedSelection, calculatedSelection);
  }
}
