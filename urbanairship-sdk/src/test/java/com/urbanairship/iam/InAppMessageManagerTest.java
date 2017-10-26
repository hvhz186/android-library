package com.urbanairship.iam;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.urbanairship.BaseTestCase;
import com.urbanairship.TestActivityMonitor;
import com.urbanairship.automation.AutomationEngine;
import com.urbanairship.automation.Triggers;
import com.urbanairship.iam.custom.CustomDisplayContent;
import com.urbanairship.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;
import java.util.concurrent.Executor;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InAppMessageManager}.
 */
public class InAppMessageManagerTest extends BaseTestCase {

    private InAppMessageManager manager;

    private TestActivityMonitor activityMonitor;
    private InAppMessageDriver mockDriver;
    private AutomationEngine<InAppMessageSchedule> mockEngine;
    private InAppMessageDriver.Callbacks driverCallbacks;

    private InAppMessageSchedule schedule;

    private InAppMessageAdapter mockAdapter;
    private ShadowLooper mainLooper;

    @Before
    public void setup() {
        activityMonitor = new TestActivityMonitor();
        mockDriver = mock(InAppMessageDriver.class);
        mockAdapter = mock(InAppMessageAdapter.class);
        mainLooper = Shadows.shadowOf(Looper.getMainLooper());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                driverCallbacks = invocation.getArgument(0);
                return null;
            }
        }).when(mockDriver).setCallbacks(any(InAppMessageDriver.Callbacks.class));

        mockEngine = mock(AutomationEngine.class);

        manager = new InAppMessageManager(activityMonitor, new Executor() {
            @Override
            public void execute(@NonNull Runnable runnable) {
                runnable.run();
            }
        }, mockDriver, mockEngine);


        schedule = new InAppMessageSchedule("schedule id", InAppMessageScheduleInfo.newBuilder()
                                                                                   .addTrigger(Triggers.newAppInitTriggerBuilder().setGoal(1).build())
                                                                                   .setMessage(InAppMessage.newBuilder()
                                                                                                           .setDisplayContent(new CustomDisplayContent(JsonValue.NULL))
                                                                                                           .setId("message id")
                                                                                                           .build())
                                                                                   .build());

        manager.setAdapterFactory(InAppMessage.TYPE_CUSTOM, new InAppMessageAdapter.Factory() {
            @Override
            public InAppMessageAdapter createAdapter(InAppMessage message) {
                return mockAdapter;
            }
        });

        manager.init();
    }

    @Test
    public void testIsMessageReady() {
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);

        // First call should prefetch the assets
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        verify(mockAdapter).onPrepare(any(Context.class));

        // Resumed activity is required
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Resume an activity
        Activity activity = new Activity();
        activityMonitor.startActivity(activity);
        activityMonitor.resumeActivity(activity);

        // Verify the in-app message is ready to be displayed
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
    }

    @Test
    public void testMessageFinished() {
        // Resume an activity
        Activity activity = new Activity();
        activityMonitor.startActivity(activity);
        activityMonitor.resumeActivity(activity);

        // Prepare the schedule
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Display the schedule
        when(mockAdapter.onDisplay(eq(activity), eq(false), any(DisplayHandler.class))).thenReturn(InAppMessageAdapter.OK);
        driverCallbacks.onDisplay(schedule.getId());

        // Verify schedules are no longer ready to be displayed
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Finish displaying the in-app message
        manager.messageFinished(schedule.getId());

        // Verify schedules are still not displayed due to the display interval
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Advance the looper to free up the display lock
        mainLooper.runToEndOfTasks();

        // Verify in-app message is ready to be displayed
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
    }

    @Test
    public void testScheduleDataFetched() {
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        verify(mockEngine).checkPendingSchedules();
    }

    @Test
    public void testOnDisplay() {
        // Resume an activity
        Activity activity = new Activity();
        activityMonitor.startActivity(activity);
        activityMonitor.resumeActivity(activity);

        // Prepare the message
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Display it
        driverCallbacks.onDisplay(schedule.getId());

        verify(mockAdapter).onDisplay(eq(activity), eq(false), any(DisplayHandler.class));
    }

    @Test
    public void testRedisplay() {
        // Resume an activity
        Activity activity = new Activity();
        activityMonitor.startActivity(activity);
        activityMonitor.resumeActivity(activity);


        // Prepare the message
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Display it
        when(mockAdapter.onDisplay(eq(activity), anyBoolean(), any(DisplayHandler.class))).thenReturn(InAppMessageAdapter.OK);
        driverCallbacks.onDisplay(schedule.getId());

        // Notify the manager to display on next activity
        manager.continueOnNextActivity(schedule.getId());

        // Verify isScheduleReady returns false
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Resume a new activity
        activityMonitor.pauseActivity(activity);
        Activity anotherActivity = new Activity();
        activityMonitor.startActivity(anotherActivity);
        activityMonitor.resumeActivity(anotherActivity);

        // Verify the schedule is displayed
        verify(mockAdapter).onDisplay(eq(anotherActivity), eq(true), any(DisplayHandler.class));
    }

    @Test
    public void testActivityStopped() {
        // Resume an activity
        Activity activity = new Activity();
        activityMonitor.startActivity(activity);
        activityMonitor.resumeActivity(activity);

        // Prepare the message
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.OK);
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Display it
        when(mockAdapter.onDisplay(eq(activity), anyBoolean(), any(DisplayHandler.class))).thenReturn(InAppMessageAdapter.OK);
        driverCallbacks.onDisplay(schedule.getId());

        // Stop the activity
        activityMonitor.pauseActivity(activity);
        activityMonitor.stopActivity(activity);

        // Verify schedules are still not displayed due to the display interval
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Advance the looper to free up the display lock
        mainLooper.runToEndOfTasks();

        // Resume another activity
        Activity anotherActivity = new Activity();
        activityMonitor.startActivity(anotherActivity);
        activityMonitor.resumeActivity(anotherActivity);

        // Verify schedules are ready to be displayed
        assertTrue(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));
    }

    @Test
    public void testSchedule() {
        manager.scheduleMessage(schedule.getInfo());
        verify(mockEngine).schedule(schedule.getInfo());
    }

    @Test
    public void testCancelSchedule() {
        manager.cancelSchedule("schedule ID");
        verify(mockEngine).cancel(Collections.singletonList("schedule ID"));
    }

    @Test
    public void testCancelMessage() {
        manager.cancelMessage("message ID");
        verify(mockEngine).cancelGroup("message ID");
    }

    @Test
    public void testRetryFetchAssets() {
        when(mockAdapter.onPrepare(any(Context.class))).thenReturn(InAppMessageAdapter.RETRY);

        // First check should start downloading the assets
        assertFalse(driverCallbacks.isMessageReady(schedule.getId(), schedule.getInfo().getInAppMessage()));

        // Should call it once, but a runnable should be dispatched on the main thread with a delay to retry
        verify(mockAdapter, times(1)).onPrepare(any(Context.class));

        // Advance the looper
        ShadowLooper mainLooper = Shadows.shadowOf(Looper.getMainLooper());
        mainLooper.runToEndOfTasks();

        // Verify it was called again
        verify(mockAdapter, times(2)).onPrepare(any(Context.class));
    }

}