package com.xyrlsz.xcimoc;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ApplicationTest {

    private Application application;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testApplicationNotNull() {
        assertNotNull(application);
    }
}
