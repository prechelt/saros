/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package saros.activities;

import java.util.Objects;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import saros.concurrent.jupiter.Operation;
import saros.concurrent.jupiter.internal.text.DeleteOperation;
import saros.concurrent.jupiter.internal.text.InsertOperation;
import saros.concurrent.jupiter.internal.text.NoOperation;
import saros.concurrent.jupiter.internal.text.SplitOperation;
import saros.editor.text.TextPosition;
import saros.editor.text.TextPositionUtils;
import saros.session.User;

/** An immutable TextEditActivity. */
public class TextEditActivity extends AbstractResourceActivity {

  private static final Logger log = Logger.getLogger(TextEditActivity.class);

  protected final String newText;
  protected final String replacedText;

  private final TextPosition startPosition;

  private final int newTextLineDelta;
  private final int newTextOffsetDelta;

  private final int replacedTextLineDelta;
  private final int replacedTextOffsetDelta;

  /**
   * Instantiates a new text edit activity with the given parameters.
   *
   * <p>Uses the given line separator to calculate the line delta and offset delta of the given new
   * and replaced text.
   *
   * @param source the user that caused the activity
   * @param startPosition the position at which the text edit activity applies
   * @param newText the new text added with this activity
   * @param replacedText the replaced text removed with this activity
   * @param path the resource the activity belongs to
   * @param lineSeparator the line separator used in the given new and replaced text
   * @see TextPositionUtils#calculateDeltas(String, String)
   */
  // TODO unify once content is normalized to Unix line separators
  public TextEditActivity(
      User source,
      TextPosition startPosition,
      String newText,
      String replacedText,
      SPath path,
      String lineSeparator) {

    this(
        source,
        startPosition,
        TextPositionUtils.calculateDeltas(newText, lineSeparator),
        newText,
        TextPositionUtils.calculateDeltas(replacedText, lineSeparator),
        replacedText,
        path);
  }

  /**
   * Instantiates a new text edit activity with the given parameters.
   *
   * <p>Tries to guess the used line separator by checking for Windows (<code>\r\n</code>) or Unix
   * line separators (<code>\n</code>) in the text.
   *
   * @param source the user that caused the activity
   * @param startPosition the position at which the text edit activity applies
   * @param newText the new text added with this activity
   * @param replacedText the replaced text removed with this activity
   * @param path the resource the activity belongs to
   * @see TextPositionUtils#calculateDeltas(String)
   * @see TextPositionUtils#guessLineSeparator(String)
   */
  // TODO unify once content is normalized to Unix line separators
  public TextEditActivity(
      User source, TextPosition startPosition, String newText, String replacedText, SPath path) {

    this(
        source,
        startPosition,
        TextPositionUtils.calculateDeltas(newText),
        newText,
        TextPositionUtils.calculateDeltas(replacedText),
        replacedText,
        path);
  }

  /**
   * Instantiates a new text edit activity with the given parameters.
   *
   * @param source the user that caused the activity
   * @param startPosition the position at which the text edit activity applies
   * @param newTextLineDelta the number of lines added with the new text
   * @param newTextOffsetDelta the offset delta in the last line of the new text
   * @param newText the new text added with this activity
   * @param replacedTextLineDelta the number of lines removed with the replaced text
   * @param replacedTextOffsetDelta the offset delta in the last line of the replaced text
   * @param replacedText the replaced text removed with this activity
   * @param path the resource the activity belongs to
   */
  public TextEditActivity(
      User source,
      TextPosition startPosition,
      int newTextLineDelta,
      int newTextOffsetDelta,
      String newText,
      int replacedTextLineDelta,
      int replacedTextOffsetDelta,
      String replacedText,
      SPath path) {

    this(
        source,
        startPosition,
        new ImmutablePair<>(newTextLineDelta, newTextOffsetDelta),
        newText,
        new ImmutablePair<>(replacedTextLineDelta, replacedTextOffsetDelta),
        replacedText,
        path);
  }

  /**
   * Instantiates a new text edit activity with the given parameters.
   *
   * <p>Internal constructor added to make the re-use of the general constructor logic possible for
   * the constructors having to calculate the deltas. See {@link #TextEditActivity(User,
   * TextPosition, String, String, SPath)} and {@link #TextEditActivity(User, TextPosition, String,
   * String, SPath, String)}.
   *
   * @param source the user that caused the activity
   * @param startPosition the position at which the text edit activity applies
   * @param newTextDeltas a pair containing the number of lines added with the new text and the
   *     offset delta in the last line of the new text
   * @param newText the new text added with this activity
   * @param replacedTextDeltas a pair containing the number of lines removed with the replaced text
   *     and the offset delta in the last line of the replaced text
   * @param replacedText the replaced text removed with this activity
   * @param path the resource the activity belongs to
   */
  private TextEditActivity(
      User source,
      TextPosition startPosition,
      Pair<Integer, Integer> newTextDeltas,
      String newText,
      Pair<Integer, Integer> replacedTextDeltas,
      String replacedText,
      SPath path) {

    super(source, path);

    if (startPosition == null || !startPosition.isValid())
      throw new IllegalArgumentException("Start position must be valid");

    int newTextLineDelta = newTextDeltas.getLeft();
    int newTextOffsetDelta = newTextDeltas.getRight();

    if (newTextLineDelta < 0)
      throw new IllegalArgumentException("New text line delta must not be negative");
    if (newTextOffsetDelta < 0)
      throw new IllegalArgumentException("New text offset delta must not be negative");

    int replacedTextLineDelta = replacedTextDeltas.getLeft();
    int replacedTextOffsetDelta = replacedTextDeltas.getRight();

    if (replacedTextLineDelta < 0)
      throw new IllegalArgumentException("Replaced text line delta must not be negative");
    if (replacedTextOffsetDelta < 0)
      throw new IllegalArgumentException("Replaced text offset delta must not be negative");

    if (newText == null) throw new IllegalArgumentException("Text cannot be null");
    if (replacedText == null) throw new IllegalArgumentException("ReplacedText cannot be null");

    if (path == null) throw new IllegalArgumentException("Editor cannot be null");

    this.startPosition = startPosition;

    this.newTextLineDelta = newTextLineDelta;
    this.newTextOffsetDelta = newTextOffsetDelta;

    this.newText = newText;

    this.replacedTextLineDelta = replacedTextLineDelta;
    this.replacedTextOffsetDelta = replacedTextOffsetDelta;

    this.replacedText = replacedText;
  }

  /**
   * Returns the position at which the text edit activity applies.
   *
   * @return the position at which the text edit activity applies
   */
  public TextPosition getStartPosition() {
    return startPosition;
  }

  /**
   * Returns the position at which the new text added by this activity ends.
   *
   * @return the position at which the new text added by this activity ends
   */
  public TextPosition getNewEndPosition() {
    if (newTextLineDelta == 0) {
      int lineNumber = startPosition.getLineNumber();
      int inLineOffset = startPosition.getInLineOffset() + newTextOffsetDelta;

      return new TextPosition(lineNumber, inLineOffset);

    } else {
      int lineNumber = startPosition.getLineNumber() + newTextLineDelta;

      return new TextPosition(lineNumber, newTextOffsetDelta);
    }
  }

  /**
   * Returns the new text added by this text activity.
   *
   * @return the new text added by this text activity
   */
  public String getNewText() {
    return newText;
  }

  /**
   * Returns the replaced text removed by this text activity.
   *
   * @return the replaced text removed by this text activity
   */
  public String getReplacedText() {
    return replacedText;
  }

  @Override
  public String toString() {
    String newText = StringEscapeUtils.escapeJava(StringUtils.abbreviate(this.newText, 150));
    String oldText = StringEscapeUtils.escapeJava(StringUtils.abbreviate(replacedText, 150));
    return "TextEditActivity(start: "
        + startPosition
        + ", new text line delta: "
        + newTextLineDelta
        + ", new text offset delta: "
        + newTextOffsetDelta
        + ", new: '"
        + newText
        + ", replaced text line delta: "
        + replacedTextLineDelta
        + ", replaced text offset delta: "
        + replacedTextOffsetDelta
        + "', old: '"
        + oldText
        + "', path: "
        + getPath()
        + ", src: "
        + getSource()
        + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        startPosition,
        newTextLineDelta,
        newTextOffsetDelta,
        newText,
        replacedTextLineDelta,
        replacedTextOffsetDelta,
        replacedText,
        newText);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (!(obj instanceof TextEditActivity)) return false;

    TextEditActivity other = (TextEditActivity) obj;

    return Objects.equals(this.startPosition, other.startPosition)
        && this.newTextLineDelta == other.newTextLineDelta
        && this.newTextOffsetDelta == other.newTextOffsetDelta
        && Objects.equals(this.newText, other.newText)
        && this.replacedTextLineDelta == other.replacedTextLineDelta
        && this.replacedTextOffsetDelta == other.replacedTextOffsetDelta
        && Objects.equals(this.replacedText, other.replacedText);
  }

  /**
   * Convert this text edit activity to a matching Operation.
   *
   * @see InsertOperation
   * @see DeleteOperation
   * @see SplitOperation
   */
  public Operation toOperation() {

    // delete Activity
    if ((replacedText.length() > 0) && (newText.length() == 0)) {
      return new DeleteOperation(
          startPosition, replacedTextLineDelta, replacedTextOffsetDelta, replacedText);
    }

    // insert Activity
    if ((replacedText.length() == 0) && (newText.length() > 0)) {
      return new InsertOperation(startPosition, newTextLineDelta, newTextOffsetDelta, newText);
    }

    // replace operation has to be split into delete and insert operation
    //noinspection ConstantConditions
    if ((replacedText.length() > 0) && (newText.length() > 0)) {
      return new SplitOperation(
          new DeleteOperation(
              startPosition, replacedTextLineDelta, replacedTextOffsetDelta, replacedText),
          new InsertOperation(startPosition, newTextLineDelta, newTextOffsetDelta, newText));
    }

    log.warn("NoOp Text edit: new '" + newText + "' old '" + replacedText + "'");
    return new NoOperation();
  }

  @Override
  public void dispatch(IActivityReceiver receiver) {
    receiver.receive(this);
  }
}
