package saros.editor.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import saros.net.xmpp.JID;
import saros.preferences.EclipsePreferenceConstants;
import saros.session.ISarosSession;
import saros.session.ISessionListener;
import saros.session.User;
import saros.ui.util.SWTUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SWTUtils.class)
public class ContributionAnnotationManagerTest {

  private ContributionAnnotationManager manager;
  private ISarosSession sessionMock;
  private IPreferenceStore store;
  private IAnnotationModel model;

  private Capture<ISessionListener> sessionListenerCapture;
  private static final int MAX_HISTORY_LENGTH =
      ContributionAnnotationManager.History.MAX_HISTORY_LENGTH;

  @Before
  public void setUp() {
    store = new PreferenceStore();
    store.setValue(EclipsePreferenceConstants.SHOW_CONTRIBUTION_ANNOTATIONS, true);
    createListenerMocks();

    manager = new ContributionAnnotationManager(sessionMock, store);
    model = new AnnotationModel();
  }

  private void createListenerMocks() {
    sessionListenerCapture = EasyMock.newCapture();

    sessionMock = EasyMock.createNiceMock(ISarosSession.class);

    sessionMock.addListener(EasyMock.capture(sessionListenerCapture));

    EasyMock.expectLastCall().once();

    PowerMock.mockStatic(SWTUtils.class);

    final Capture<Runnable> capture = EasyMock.newCapture();

    SWTUtils.runSafeSWTAsync(EasyMock.anyObject(), EasyMock.capture(capture));

    EasyMock.expectLastCall()
        .andAnswer(
            () -> {
              capture.getValue().run();
              return null;
            })
        .anyTimes();

    PowerMock.replayAll(sessionMock);
  }

  @Test
  public void testHistoryRemoval() {

    User alice = createAliceTestUser();

    for (int i = 0; i <= MAX_HISTORY_LENGTH; i++) manager.insertAnnotation(model, i, 1, alice);

    assertEquals(MAX_HISTORY_LENGTH, getAnnotationCount(model));

    manager.insertAnnotation(model, MAX_HISTORY_LENGTH + 1, 1, alice);

    assertEquals(MAX_HISTORY_LENGTH, getAnnotationCount(model));

    assertFalse(
        "oldest annotation was not removed",
        getAnnotationPositions(model).contains(new Position(0, 1)));
  }

  @Test
  public void testHistoryRemovalAfterRefresh() {
    User alice = createAliceTestUser();

    for (int i = 0; i <= MAX_HISTORY_LENGTH; i++) manager.insertAnnotation(model, i, 1, alice);

    manager.refreshAnnotations(model);

    manager.insertAnnotation(model, MAX_HISTORY_LENGTH + 1, 1, alice);

    assertFalse(
        "oldest annotation was not removed after refresh",
        getAnnotationPositions(model).contains(new Position(0, 1)));
  }

  @Test
  public void testRemoveAllAnnotationsBySwitchingProperty() {

    final List<User> users =
        Arrays.asList(
            createAliceTestUser(), createBobTestUser(), createCarlTestUser(), createDaveTestUser());

    int idx = 0;

    for (final User user : users)
      for (int i = 0; i < MAX_HISTORY_LENGTH; i++, idx++)
        manager.insertAnnotation(model, idx, 1, user);

    assertEquals(MAX_HISTORY_LENGTH * users.size(), getAnnotationCount(model));

    store.setValue(EclipsePreferenceConstants.SHOW_CONTRIBUTION_ANNOTATIONS, false);

    assertEquals(0, getAnnotationCount(model));
  }

  @Test
  public void testRemoveAnnotationsWhenUserLeaves() {

    final List<User> users = new ArrayList<>();

    users.add(createAliceTestUser());
    users.add(createBobTestUser());

    int idx = 0;

    for (final User user : users)
      for (int i = 0; i < MAX_HISTORY_LENGTH; i++, idx++)
        manager.insertAnnotation(model, idx, 1, user);

    assertEquals(MAX_HISTORY_LENGTH * users.size(), getAnnotationCount(model));

    final ISessionListener sessionListener = sessionListenerCapture.getValue();

    assertNotNull(sessionListener);

    while (!users.isEmpty()) {
      sessionListener.userLeft(users.remove(0));

      assertEquals(MAX_HISTORY_LENGTH * users.size(), getAnnotationCount(model));
    }
  }

  @Test
  public void testInsertAnnotationWithLengthGreaterOne() {
    int annotationLength = 3;

    manager.insertAnnotation(model, 5, annotationLength, createAliceTestUser());

    List<Position> annotationPositions = getAnnotationPositions(model);

    assertEquals(
        "Annotation was not split into multiple annotation of length 1",
        annotationLength,
        annotationPositions.size());
    assertTrue(annotationPositions.contains(new Position(5, 1)));
    assertTrue(annotationPositions.contains(new Position(6, 1)));
    assertTrue(annotationPositions.contains(new Position(7, 1)));
  }

  @Test
  public void testInsertAnnotationWithLengthZero() {
    manager.insertAnnotation(model, 3, 0, createAliceTestUser());
    assertEquals("Annotation with length 0 was inserted", 0, getAnnotationCount(model));
  }

  @Test
  public void testInsertAnnotationWithRedundantAnnotationIsIgnored() {
    int length = 1;
    int offset = 3;

    manager.insertAnnotation(model, offset, length, createAliceTestUser());
    manager.insertAnnotation(model, offset, length, createAliceTestUser());

    assertEquals("Inserted same annotation twice", 1, getAnnotationCount(model));
  }

  public void testInsertWhileNotEnable() {
    store.setValue(EclipsePreferenceConstants.SHOW_CONTRIBUTION_ANNOTATIONS, false);

    final User alice = new User(new JID("alice@test"), false, false, null);

    manager.insertAnnotation(model, 5, 7, alice);

    assertEquals(0, getAnnotationCount(model));
  }

  @Test
  public void testDispose() {

    final List<User> users = new ArrayList<>();

    users.add(createAliceTestUser());
    users.add(createBobTestUser());

    int idx = 0;

    for (final User user : users)
      for (int i = 0; i < MAX_HISTORY_LENGTH; i++, idx++)
        manager.insertAnnotation(model, idx, 1, user);

    assertEquals(MAX_HISTORY_LENGTH * users.size(), getAnnotationCount(model));

    manager.dispose();

    assertEquals(0, getAnnotationCount(model));
  }

  private int getAnnotationCount(IAnnotationModel model) {
    int count = 0;

    Iterator<Annotation> it = model.getAnnotationIterator();

    while (it.hasNext()) {
      count++;
      it.next();
    }

    return count;
  }

  private List<Position> getAnnotationPositions(IAnnotationModel model) {

    List<Position> positions = new ArrayList<Position>();

    Iterator<Annotation> it = model.getAnnotationIterator();

    while (it.hasNext()) {
      Annotation annotation = it.next();
      positions.add(model.getPosition(annotation));
    }

    return positions;
  }

  private User createAliceTestUser() {
    return createTestUser("alice@test");
  }

  private User createBobTestUser() {
    return createTestUser("bob@test");
  }

  private User createCarlTestUser() {
    return createTestUser("carl@test");
  }

  private User createDaveTestUser() {
    return createTestUser("dave@test");
  }

  private User createTestUser(String jid) {
    return new User(new JID(jid), false, false, null);
  }
}
