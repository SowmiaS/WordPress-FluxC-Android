package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.InstantiateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_CommentTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @Inject CommentStore mCommentStore;
    @Inject PostStore mPostStore;

    private List<PostModel> mPosts;
    private List<CommentModel> mComments;
    private CommentModel mNewComment;

    private enum TEST_EVENTS {
        NONE,
        POSTS_FETCHED,
        COMMENT_INSTANTIATED,
        COMMENT_CHANGED,
        COMMENT_CHANGED_ERROR,
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize mSite.
        init();
        // Fetch first posts
        fetchFirstPosts();
        mNextEvent = TEST_EVENTS.NONE;
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchComment() throws InterruptedException {
        fetchFirstComments();
        // Get first comment
        CommentModel firstComment = mComments.get(0);
        // Pull the first comments
        RemoteCommentPayload fetchCommentPayload = new RemoteCommentPayload(mSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(fetchCommentPayload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInstantiateAndCreateNewComment() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Check comment has been modified in the DB
        CommentModel comment = mCommentStore.getCommentByLocalId(mNewComment.getId());
        assertTrue(comment.getContent().contains(mNewComment.getContent()));
    }

    public void testInstantiateAndCreateNewCommentDuplicate() throws InterruptedException {
        // New Comment
        InstantiateCommentPayload payload1 = new InstantiateCommentPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload1));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Edit comment instance
        mNewComment.setContent("Trying with: " + (new Random()).nextFloat() * 10 + " gigawatts");

        // Create new Comment
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        RemoteCreateCommentPayload payload2 = new RemoteCreateCommentPayload(mSite, mPosts.get(0), mNewComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Same comment again
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(payload2));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditValidComment() throws InterruptedException {
        fetchFirstComments();

        // Get first comment
        CommentModel firstComment = mComments.get(0);

        // Edit the comment
        firstComment.setContent("If we could somehow harness this lightning... "
                                + "channel it into the flux capacitor... it just might work.");
        firstComment.setStatus(CommentStatus.APPROVED.toString());

        // Push the edited comment
        RemoteCommentPayload pushCommentPayload = new RemoteCommentPayload(mSite, firstComment);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(pushCommentPayload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // OnChanged Events

    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(mSite, CommentStatus.ALL);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            if (mNextEvent != TEST_EVENTS.COMMENT_CHANGED_ERROR) {
                assertTrue("onCommentChanged Error", false);
            }
            mCountDownLatch.countDown();
            return;
        }
        AppLog.i(T.TESTS, "comments count " + comments.size());
        assertTrue(comments.size() != 0);
        assertEquals(TEST_EVENTS.COMMENT_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onCommentInstantiated(CommentStore.OnCommentInstantiated event) {
        mNewComment = event.comment;
        assertTrue(event.comment.getId() != 0);
        assertEquals(TEST_EVENTS.COMMENT_INSTANTIATED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        mPosts = mPostStore.getPostsForSite(mSite);
        assertEquals(mNextEvent, TEST_EVENTS.POSTS_FETCHED);
        mCountDownLatch.countDown();
    }

    // Private methods

    private void fetchFirstComments() throws InterruptedException {
        if (mComments != null) {
            return;
        }
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite, 10, 0);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mComments = mCommentStore.getCommentsForSite(mSite, CommentStatus.ALL);
    }

    private void fetchFirstPosts() throws InterruptedException {
        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(mSite, false)));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
