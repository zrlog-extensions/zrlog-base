package com.zrlog.util.test;

import com.zrlog.util.UserAgentUtils;
import org.junit.Assert;
import org.junit.Test;

public class UserAgentUtilsTest {

    @Test
    public void shouldParseChromeVersion() {
        UserAgentUtils.UserAgentInfo info = UserAgentUtils.parse(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36");

        Assert.assertEquals("Linux", info.getOs());
        Assert.assertEquals("Chrome", info.getBrowser());
        Assert.assertEquals("146.0.0.0", info.getBrowserVersion());
        Assert.assertEquals("Chrome 146.0.0.0", info.getFullBrowser());
        Assert.assertFalse(info.isCrawler());
    }

    @Test
    public void shouldParseSafariVersionFromVersionToken() {
        UserAgentUtils.UserAgentInfo info = UserAgentUtils.parse(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15");

        Assert.assertEquals("Mac OS X", info.getOs());
        Assert.assertEquals("Safari", info.getBrowser());
        Assert.assertEquals("17.5", info.getBrowserVersion());
        Assert.assertFalse(info.isCrawler());
    }

    @Test
    public void shouldParseWeChatVersion() {
        UserAgentUtils.UserAgentInfo info = UserAgentUtils.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.49");

        Assert.assertEquals("iPhone (iOS)", info.getOs());
        Assert.assertEquals("WeChat", info.getBrowser());
        Assert.assertEquals("8.0.49", info.getBrowserVersion());
        Assert.assertFalse(info.isCrawler());
    }

    @Test
    public void shouldRecognizeCrawlerUserAgent() {
        UserAgentUtils.UserAgentInfo info = UserAgentUtils.parse(
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");

        Assert.assertEquals("Unknown", info.getOs());
        Assert.assertEquals("Unknown", info.getBrowser());
        Assert.assertEquals("Unknown", info.getBrowserVersion());
        Assert.assertTrue(info.isCrawler());
    }

    @Test
    public void shouldReturnUnknownForEmptyUserAgent() {
        UserAgentUtils.UserAgentInfo info = UserAgentUtils.parse("");

        Assert.assertEquals("Unknown", info.getOs());
        Assert.assertEquals("Unknown", info.getBrowser());
        Assert.assertEquals("Unknown", info.getBrowserVersion());
        Assert.assertEquals("Unknown", info.getFullBrowser());
        Assert.assertFalse(info.isCrawler());
    }
}
