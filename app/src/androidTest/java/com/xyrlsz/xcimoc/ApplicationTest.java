package com.xyrlsz.xcimoc;

import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.test.ComicImageTest;
import com.xyrlsz.xcimoc.test.ComicInfoParserTest;
import com.xyrlsz.xcimoc.test.SearchTest;
import com.xyrlsz.xcimoc.test.TestCallBack;
import com.xyrlsz.xcimoc.test.WebSocketLogger;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationTest {


    private App application;
    private WebSocketLogger webSocketLogger;

    @Before
    public void setUp() {

        application = ApplicationProvider.getApplicationContext();
        webSocketLogger = new WebSocketLogger();
        webSocketLogger.connect();
    }

    @Test
    public void test0_ApplicationNotNull() {
        assertNotNull(application);
    }

    @Test
    public void test1_searchTest() throws InterruptedException {

        SearchTest.test(application, new TestCallBack() {
            @Override
            public void onSuccess(Source source) {
                System.out.println(source.getTitle() + " 搜索测试通过");
                webSocketLogger.send(source.getTitle() + " 搜索测试通过");
            }

            @Override
            public void onFail(Source source) {
                System.out.println(source.getTitle() + " 搜索测试失败");
                webSocketLogger.send(source.getTitle() + " 搜索测试失败");
            }
        });

    }

    @Test
    public void test2_comicInfoParserTest() throws InterruptedException {

        ComicInfoParserTest.test(application, new TestCallBack() {

            @Override
            public void onSuccess(Source source) {
                System.out.println(source.getTitle() + " 解析测试通过");
                webSocketLogger.send(source.getTitle() + " 解析测试通过");
            }

            @Override
            public void onFail(Source source) {
                System.out.println(source.getTitle() + " 解析测试失败");
                webSocketLogger.send(source.getTitle() + " 解析测试失败");
            }
        });

    }

    @Test
    public void test3_comicImageTest() throws InterruptedException {
        ComicImageTest.test(application, new TestCallBack() {
            @Override
            public void onSuccess(Source source) {
                System.out.println(source.getTitle() + " 图片测试通过");
                webSocketLogger.send(source.getTitle() + " 图片测试通过");
            }

            @Override
            public void onFail(Source source) {
                System.out.println(source.getTitle() + " 图片测试失败");
                webSocketLogger.send(source.getTitle() + " 图片测试失败");
            }
        });

    }
    @Test
    public  void test4_closeWs(){
        webSocketLogger.close();
    }

}