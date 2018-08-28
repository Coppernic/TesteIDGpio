package fr.coppernic.teste_idgpio;

import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UsbGpioStressTest {
    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<> (MainActivity.class);

    @Before
    public void before() {
        onView(withId(R.id.swGpio3)).perform(click());
        onView(withId(R.id.swUsbId)).perform(click());
        onView(withId(R.id.swUsbEn)).perform(click());
        onView(withId(R.id.swExternalEn)).perform(click());
        SystemClock.sleep(1000);
        onView(withId(R.id.btnPermission)).perform(click());
        allowUsbIfNeeded();
    }

    @Test
    public void stressTest() {
        Random rnd = new Random(123);
        for (int i = 0; i < 1000; i++) {
            int id = rnd.nextInt(4) + 1;
            clickOnGpio(id);
            SystemClock.sleep(1000);
        }
    }

    @After
    public void after() {
        onView(withId(R.id.swGpio3)).perform(click());
        onView(withId(R.id.swUsbId)).perform(click());
        onView(withId(R.id.swUsbEn)).perform(click());
        onView(withId(R.id.swExternalEn)).perform(click());
    }

    private void clickOnGpio(int gpio) {

        int buttonId = R.id.swUsbGpio1;
        switch(gpio) {
            case 2:
                buttonId = R.id.swUsbGpio2;
                break;
            case 3:
                buttonId = R.id.swUsbGpio3;
                break;
            case 4:
                buttonId = R.id.swUsbGpio4;
                break;
        }

        onView(withId(buttonId)).perform(click());
    }

    private void allowUsbIfNeeded() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (Build.VERSION.SDK_INT > 23) {
            UiObject allowUsb = device.findObject(new UiSelector().text("OK"));

            if (allowUsb.exists()) {
                try {
                    allowUsb.click();
                    SystemClock.sleep(500);
                } catch (UiObjectNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
