package saros.editor.internal;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.junit.Test;
import saros.editor.text.TextPosition;
import saros.editor.text.TextSelection;

/** Tests the position calculation logic of the editor api. */
public class EditorAPITextPositionTest {

  private class IDocumentBuilder {
    private IDocument document;

    private IDocumentBuilder() {
      document = EasyMock.createNiceMock(IDocument.class);
    }

    private IDocumentBuilder withLineOffsetAnswer(int lineNumberInput, int lineOffsetAnswer) {
      try {
        EasyMock.expect(document.getLineOffset(lineNumberInput)).andReturn(lineOffsetAnswer);
      } catch (BadLocationException e) {
      }
      return this;
    }

    private IDocumentBuilder withLineOfOffsetAnswer(int offsetInput, int lineNumberAnswer) {
      try {
        EasyMock.expect(document.getLineOfOffset(offsetInput)).andReturn(lineNumberAnswer);
      } catch (BadLocationException e) {
      }
      return this;
    }

    private IDocument build() {
      EasyMock.replay(document);
      return document;
    }
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
  public void testCalculateOffsetsBySelectionStartSelection() {
    int startLine = 0;
    int endLine = 1;
    TextSelection textSelection = selection(startLine, 0, endLine, 1);
    IDocument document =
        new IDocumentBuilder()
            .withLineOffsetAnswer(startLine, 0)
            .withLineOffsetAnswer(endLine, 10)
            .build();

    ITextSelection expectedOffsets = offsets(0, 11);

    ITextSelection calculatedOffsets =
        EditorAPI.calculateOffsetsBySelection(document, textSelection);
    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsBySelectionMultiLineSelection() {
    int startLine = 5;
    int endLine = 9;
    TextSelection textSelection = selection(startLine, 7, endLine, 3);
    IDocument document =
        new IDocumentBuilder()
            .withLineOffsetAnswer(startLine, 250)
            .withLineOffsetAnswer(endLine, 315)
            .build();

    ITextSelection expectedOffsets = offsets(257, 318);

    ITextSelection calculatedOffsets =
        EditorAPI.calculateOffsetsBySelection(document, textSelection);
    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsBySelectionOneLineSelection() {
    int startLine = 3;
    int endLine = 3;
    TextSelection textSelection = selection(startLine, 7, endLine, 9);
    IDocument document = new IDocumentBuilder().withLineOffsetAnswer(startLine, 11).build();

    ITextSelection expectedOffsets = offsets(18, 20);

    ITextSelection calculatedOffsets =
        EditorAPI.calculateOffsetsBySelection(document, textSelection);
    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateOffsetsBySelectionEmptySelection() {
    int startLine = 7;
    int endLine = 7;
    TextSelection textSelection = selection(startLine, 5, endLine, 5);
    IDocument document = new IDocumentBuilder().withLineOffsetAnswer(startLine, 23).build();

    ITextSelection expectedOffsets = offsets(28, 28);

    ITextSelection calculatedOffsets =
        EditorAPI.calculateOffsetsBySelection(document, textSelection);
    assertEquals(expectedOffsets, calculatedOffsets);
  }

  @Test
  public void testCalculateSelectionStartSelection() {
    int offset = 0;
    int endOffset = 15;
    int length = endOffset - offset;
    IDocument document =
        new IDocumentBuilder()
            .withLineOfOffsetAnswer(offset, 0)
            .withLineOfOffsetAnswer(endOffset, 1)
            .withLineOffsetAnswer(0, 0)
            .withLineOffsetAnswer(1, 8)
            .build();

    TextSelection expectedSelection = selection(0, 0, 1, 7);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);
    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionMultipleLineSelection() {
    int offset = 75;
    int endOffset = 351;
    int length = endOffset - offset;
    IDocument document =
        new IDocumentBuilder()
            .withLineOfOffsetAnswer(offset, 9)
            .withLineOfOffsetAnswer(endOffset, 28)
            .withLineOffsetAnswer(9, 68)
            .withLineOffsetAnswer(28, 290)
            .build();

    TextSelection expectedSelection = selection(9, 7, 28, 61);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);
    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionOneLineSelection() {
    int offset = 85;
    int endOffset = 110;
    int length = endOffset - offset;
    IDocument document =
        new IDocumentBuilder()
            .withLineOfOffsetAnswer(offset, 10)
            .withLineOfOffsetAnswer(endOffset, 10)
            .withLineOffsetAnswer(10, 79)
            .build();

    TextSelection expectedSelection = selection(10, 6, 10, 31);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);
    assertEquals(expectedSelection, calculatedSelection);
  }

  @Test
  public void testCalculateSelectionEmptySelection() {
    int offset = 243;
    int endOffset = 243;
    int length = endOffset - offset;
    IDocument document =
        new IDocumentBuilder()
            .withLineOfOffsetAnswer(offset, 98)
            .withLineOffsetAnswer(98, 100)
            .build();

    TextSelection expectedSelection = selection(98, 143, 98, 143);

    TextSelection calculatedSelection = EditorAPI.calculateSelection(document, offset, length);
    assertEquals(expectedSelection, calculatedSelection);
  }
}
