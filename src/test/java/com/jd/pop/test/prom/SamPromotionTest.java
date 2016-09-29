package com.jd.pop.test.prom;


import org.apache.commons.lang3.time.DateFormatUtils;
import org.joda.time.DateTimeUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by yfxuxiaojun on 2016/9/29.
 */
public class SamPromotionTest {
    private WebDriver driver;
    private static final String NOW = DateFormatUtils.format(DateTimeUtils.currentTimeMillis(), "yyyyMMdd_HHmmss");

    @BeforeClass

    public void beforeClass() throws Exception {
        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.get("http://market.soa.pop.jd.local/x/samclub/init.action");
    }


    @AfterClass
    public void afterClass() {
        driver.quit();

    }

    @DataProvider(name = "getSamSkus")
    public static Object[][] getSamSkus() throws IOException {
        return getDataFromCSV("D:\\app\\sam_promotion\\src\\test\\resources\\samSkus.csv");

    }

    private static Object[][] getDataFromCSV(String filePath) throws IOException {
//		创建一个文件输入流
//		读取文件，并存放结果
//		创建二维数组，添加元素
//		返回二维数组

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        System.out.println(Arrays.toString(br.readLine().split(",")));
        List<Object[]> list = new ArrayList<Object[]>();
        String line = null;
        while ((line = br.readLine()) != null) {
            list.add(line.split(","));
        }

        Object[][] object = new Object[list.size()][];

        for (int i = 0; i < object.length; i++) {
            object[i] = list.get(i);
        }
        br.close();

        return object;


    }

    @Test(dataProvider = "getSamSkus")
    public void testCreateSamPrice(String skuId, String jdPrice, String venderId) throws Exception {

//        deleteExistResult();

        driver.findElement(By.id("venderId")).clear();
        driver.findElement(By.id("venderId")).sendKeys(venderId);

        driver.findElement(By.id("skuId")).clear();
        driver.findElement(By.id("skuId")).sendKeys(skuId);

        driver.findElement(By.id("normalPrice")).clear();
        driver.findElement(By.id("normalPrice")).sendKeys(jdPrice);

        double clubPrice = Double.parseDouble(jdPrice) * (0.6 + new Random().nextDouble() * 0.4);       //zhekou
        DecimalFormat df = new DecimalFormat("0.00");
        driver.findElement(By.id("clubPrice")).clear();
        driver.findElement(By.id("clubPrice")).sendKeys(df.format(clubPrice));

        driver.findElement(By.id("add")).click();

        Assert.assertTrue(driver.findElement(By.xpath(".//*[@id='content']//p")).getText().contains("成功"), "创建失败");

        //写入测试结果到CSV文件，供联调使用
        BufferedWriter bw;
        BufferedReader br;
        try {
            bw = new BufferedWriter(new FileWriter("./result" + NOW +
                    ".csv", true));
            br = new BufferedReader(new FileReader("./result" + NOW +
                    ".csv"));
            if (br.readLine() == null) {
                bw.write("skuId,normalPrice,clubPrice,venderId");
                bw.newLine();
            }
            bw.write(skuId + "," + jdPrice + "," + df.format(clubPrice) + "," + venderId);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Test(dataProvider = "getSamSkus")
    public void testAssertSamPrice(String skuId, String normalPrice, String venderId) throws Exception {

        driver.get("http://p.3.cn/prices/mgets?skuIds=J_" + skuId);
        Assert.assertTrue(driver.getPageSource().contains("p"));
        Assert.assertTrue(driver.getPageSource().contains("m"));
        Assert.assertTrue(driver.getPageSource().contains("id"));
        if (!driver.getPageSource().contains("sp")) {
            System.out.println(skuId + " 无山姆会员价，手动修复价格");
            driver.get("http://pricecallback.jd.local/getEndTimePriceList.action?skuids=J_" + skuId);
            if (driver.getPageSource().contains("-1.00")) {
                System.out.println(skuId + " 下架，无法同步会员价格");
                return;
            }
        }
        Assert.assertTrue(driver.getPageSource().contains("sp"), "无山姆会员价");
        Assert.assertTrue(driver.getPageSource().contains(skuId));
        Assert.assertTrue(driver.getPageSource().contains(normalPrice));


    }

    private void deleteExistResult() {
        File file = new File("./result.csv");
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

}
