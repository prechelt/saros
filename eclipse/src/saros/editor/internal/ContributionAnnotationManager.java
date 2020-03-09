package saros.editor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import saros.editor.annotations.ContributionAnnotation;
import saros.preferences.EclipsePreferenceConstants;
import saros.session.ISarosSession;
import saros.session.ISessionListener;
import saros.session.User;
import saros.ui.util.SWTUtils;

/**
 * Throughout a session, the user should be made aware of the textual changes made by the other
 * session participants. Additions and changes are represented by {@link ContributionAnnotation}s
 * and distinguished by authors (deletions are not highlighted). The Annotations are added in
 * real-time along with the application of the textual changes and are removed when the characters
 * they belong to are deleted, the session ends, or their respective author leaves the session. To
 * avoid cluttering the editors, only the last {@value #MAX_HISTORY_LENGTH} changes are annotated.
 *
 * <p>This class takes care of managing the annotations for session participants which involves
 * adding, removing, and splitting of Annotations.
 */
// <p>TODO Move responsibilities from EditorManager to here
public class ContributionAnnotationManager {

  private static final Logger log = Logger.getLogger(ContributionAnnotationManager.class);

  private final Map<User, History> sourceToHistory = new HashMap<>();

  private final ISarosSession sarosSession;

  private final IPreferenceStore preferenceStore;

  private boolean contribtionAnnotationsEnabled;

  private final AnnotationModelHelper annotationModelHelper = new AnnotationModelHelper();

  private final ISessionListener sessionListener =
      new ISessionListener() {
        @Override
        public void userLeft(User user) {
          removeAnnotationsForUser(user);
        }
      };

  private final IPropertyChangeListener propertyChangeListener =
      new IPropertyChangeListener() {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {

          if (!EclipsePreferenceConstants.SHOW_CONTRIBUTION_ANNOTATIONS.equals(event.getProperty()))
            return;

          SWTUtils.runSafeSWTAsync(
              log,
              new Runnable() {

                @Override
                public void run() {
                  contribtionAnnotationsEnabled = Boolean.valueOf(event.getNewValue().toString());

                  if (!contribtionAnnotationsEnabled) removeAllAnnotations();
                }
              });
        }
      };

  public ContributionAnnotationManager(
      ISarosSession sarosSession, IPreferenceStore preferenceStore) {

    this.sarosSession = sarosSession;
    this.preferenceStore = preferenceStore;
    this.preferenceStore.addPropertyChangeListener(propertyChangeListener);
    this.sarosSession.addListener(sessionListener);

    contribtionAnnotationsEnabled =
        preferenceStore.getBoolean(EclipsePreferenceConstants.SHOW_CONTRIBUTION_ANNOTATIONS);
  }

  /**
   * Inserts contribution annotations to given model if there is not already a contribution
   * annotation at given position. This method should be called after the text has changed.
   *
   * @param model to add the annotation to.
   * @param offset start of the annotation to add.
   * @param length length of the annotation.
   * @param source of the annotation.
   */
  public void insertAnnotation(IAnnotationModel model, int offset, int length, User source) {

    if (!contribtionAnnotationsEnabled || length < 0) return;

    final History history = getHistory(source);
    List<ContributionAnnotation> annotationsToRemove = history.removeHistoryEntries();
    Map<ContributionAnnotation, Position> annotationsToAdd =
        createAnnotationsForContributionRange(model, offset, length, source);

    if (!annotationsToAdd.isEmpty()) {
      history.addNewEntry(new ArrayList<ContributionAnnotation>(annotationsToAdd.keySet()));
    }
    annotationModelHelper.replaceAnnotationsInModel(model, annotationsToRemove, annotationsToAdd);
  }

  /**
   * Creates one contribution annotations with length 1 for each char contained in the range defined
   * by {@code offset} and {@code length}. The annotation is inserted to given model if there is not
   * already a contribution annotation at given position.
   *
   * @param model to add the annotation to.
   * @param offset start of the annotation to add.
   * @param length length of the annotation.
   * @param source of the annotation.
   * @returns a map containing the annotations and their positions
   */
  private Map<ContributionAnnotation, Position> createAnnotationsForContributionRange(
      IAnnotationModel model, int offset, int length, User source) {

    Map<ContributionAnnotation, Position> annotationsToAdd = new HashMap<>();

    for (int i = 0; i < length; i++) {
      Pair<ContributionAnnotation, Position> positionedAnnotation =
          createPositionedAnnotation(model, offset + i, source);
      if (positionedAnnotation == null) continue;

      annotationsToAdd.put(positionedAnnotation.getKey(), positionedAnnotation.getValue());
    }

    return annotationsToAdd;
  }

  /**
   * Creates a contribution annotations with length 1 at position {@code offset} The annotation is
   * inserted to given model if there is not already a contribution annotation at given position.
   *
   * @param model to add the annotation to.
   * @param offset start of the annotation to add.
   * @param source of the annotation.
   * @returns a pair containing the annotation and its position.
   */
  private Pair<ContributionAnnotation, Position> createPositionedAnnotation(
      IAnnotationModel model, int offset, User source) {
    final int ANNOTATION_SIZE = 1;
    /* Return early if there already is an annotation at that offset */

    for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext(); ) {
      Annotation annotation = it.next();

      if (annotation instanceof ContributionAnnotation
          && model.getPosition(annotation).includes(offset)
          && ((ContributionAnnotation) annotation).getSource().equals(source)) {
        return null;
      }
    }

    return new ImmutablePair<ContributionAnnotation, Position>(
        new ContributionAnnotation(source, model), new Position(offset, ANNOTATION_SIZE));
  }

  /**
   * Refreshes all contribution annotations in the model by removing and reinserting them.
   *
   * @param model the annotation model that should be refreshed
   */
  public void refreshAnnotations(IAnnotationModel model) {

    List<Annotation> annotationsToRemove = new ArrayList<Annotation>();
    Map<Annotation, Position> annotationsToAdd = new HashMap<Annotation, Position>();

    for (Iterator<Annotation> it = model.getAnnotationIterator(); it.hasNext(); ) {

      Annotation annotation = it.next();

      if (!(annotation instanceof ContributionAnnotation)) continue;

      Position position = model.getPosition(annotation);

      if (position == null) {
        log.warn("annotation could not be found in the current model: " + annotation);
        continue;
      }

      /*
       * we rely on the fact the a user object is unique during a running
       * session so that user.equals(user) <=> user == user otherwise just
       * reinserting the annotations would not refresh the colors as the
       * color id of the user has not changed
       */
      annotationsToRemove.add(annotation);

      ContributionAnnotation annotationToAdd =
          new ContributionAnnotation(((ContributionAnnotation) annotation).getSource(), model);

      annotationsToAdd.put(annotationToAdd, position);

      replaceInHistory((ContributionAnnotation) annotation, annotationToAdd);
    }

    if (annotationsToRemove.isEmpty()) return;

    annotationModelHelper.replaceAnnotationsInModel(model, annotationsToRemove, annotationsToAdd);
  }

  public void dispose() {
    sarosSession.removeListener(sessionListener);
    preferenceStore.removePropertyChangeListener(propertyChangeListener);
    removeAllAnnotations();
  }

  /** Get the history of the given user. If no history is available a new one is created. */
  private History getHistory(final User user) {
    return sourceToHistory.computeIfAbsent(user, u -> new History());
  }

  /**
   * Replaces an existing annotation in the current history with new annotations. This method
   * <b>DOES NOT</b> alter the annotation model!
   */
  private void replaceInHistory(
      final ContributionAnnotation oldAnnotation, final ContributionAnnotation newAnnotation) {

    final User user = oldAnnotation.getSource();

    final History history = getHistory(user);

    /*
     * update the history entry, e.g we want to modify annotation D pre: A,
     * B, C, D, E, F, G post: A, B, C, D_0, D_1, ..., D_N, E, F, G
     *
     */

    for (History.Entry entry : history.entries) {

      for (final ListIterator<ContributionAnnotation> annotationsLit =
              entry.annotations.listIterator();
          annotationsLit.hasNext(); ) {
        final ContributionAnnotation annotation = annotationsLit.next();

        if (annotation.equals(oldAnnotation)) {
          annotationsLit.remove();

          assert oldAnnotation.getSource().equals(newAnnotation.getSource());

          annotationsLit.add(newAnnotation);
          return;
        }
      }
    }

    log.warn(
        "could not find annotation "
            + oldAnnotation
            + " in the current history for user: "
            + oldAnnotation.getSource());
  }

  /**
   * Removes all annotations from all annotation models that are currently stored in the history of
   * all users.
   */
  private void removeAllAnnotations() {
    removeFromHistoryAndAnnotationModel(sourceToHistory.values());
  }

  /**
   * Removes all annotations from all annotation models that are currently stored in the history for
   * the given user. The entries of the history are removed as well.
   */
  private void removeAnnotationsForUser(final User user) {
    final History history = sourceToHistory.get(user);

    if (history != null) removeFromHistoryAndAnnotationModel(Collections.singletonList(history));
  }

  /**
   * Removes all annotations from all annotation models of the given histories. The entries of the
   * histories are removed as well.
   */
  private void removeFromHistoryAndAnnotationModel(final Collection<History> histories) {

    final Set<IAnnotationModel> annotationModels = new HashSet<>();
    final Set<User> users = new HashSet<>();

    for (final History history : histories) {
      while (!history.entries.isEmpty()) {

        final History.Entry entry = history.entries.poll();

        annotationModels.addAll(
            entry
                .annotations
                .stream()
                .map(ContributionAnnotation::getModel)
                .collect(Collectors.toList()));
        users.addAll(
            entry
                .annotations
                .stream()
                .map(ContributionAnnotation::getSource)
                .collect(Collectors.toList()));
      }
    }

    for (final IAnnotationModel annotationModel : annotationModels)
      annotationModelHelper.removeAnnotationsFromModel(
          annotationModel,
          (a) ->
              a instanceof ContributionAnnotation
                  && users.contains(((ContributionAnnotation) (a)).getSource()));
  }

  static final class History {
    static final int MAX_HISTORY_LENGTH = 20;

    private final LinkedList<History.Entry> entries = new LinkedList<>();
    private int currentInsertStamp = 0;

    /**
     * Performs a step forward in the history and obsoletes old entries which are removed from the
     * history (not the annotation model) with {@code removeHistoryEntries}.
     */
    private void tick() {
      currentInsertStamp = (currentInsertStamp + 1) % MAX_HISTORY_LENGTH;
    }

    /**
     * Removes entries from the history based on the history current insert stamp. This method
     * <b>DOES NOT</b> alter the annotation model the removed annotations in the history belong to!
     */
    private List<ContributionAnnotation> removeHistoryEntries() {

      final List<ContributionAnnotation> removedEntries = new ArrayList<>();

      final int insertStampToRemove = currentInsertStamp;

      /**
       * the logic assumes that the entry order does not change during lifetime i.e if we have a
       * history of size 4 the list must look like this regarding the insert stamps 0 0 0 1 2 3 3 3
       * 0 1 1 1 2 2 2 2 3 0 1 1 1 1 1 2 3 ... and so on
       */
      final Iterator<Entry> it = entries.iterator();

      while (it.hasNext()) {
        final Entry entry = it.next();

        if (entry.insertStamp != insertStampToRemove) break;

        removedEntries.addAll(entry.annotations);
        it.remove();
      }

      return removedEntries;
    }

    /**
     * Adds annotations as one history entry and steps forward in the history.
     *
     * @param annotations the annotations that are assigned to the entry in the history
     */
    private void addNewEntry(List<ContributionAnnotation> annotations) {
      Entry entry = new Entry(annotations, currentInsertStamp);
      tick();
      entries.add(entry);
    }

    private static final class Entry {
      private List<ContributionAnnotation> annotations;
      private int insertStamp;

      private Entry(final List<ContributionAnnotation> annotations, final int insertStamp) {
        this.annotations = annotations;
        this.insertStamp = insertStamp;
      }
    }
  }
}
