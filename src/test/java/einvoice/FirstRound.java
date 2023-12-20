package einvoice;

import ch.qos.logback.classic.Level;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.sanluan.einvoice.service.Invoice;
import com.sanluan.einvoice.service.OfdInvoiceExtractor;
import com.sanluan.einvoice.service.PdfInvoiceExtractor;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FirstRound {

    public static void main(String[] args) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.toLevel("error"));

        FileUtil.del("~/Downloads/发票/YQ");
        FileUtil.del("~/Downloads/发票/XY");
        FileUtil.del("~/Downloads/发票/MW");
        FileUtil.del("~/Downloads/发票/OT");
        FileUtil.del("~/Downloads/发票/FT");

        FileUtil.mkdir("~/Downloads/发票/YQ");
        FileUtil.mkdir("~/Downloads/发票/XY");
        FileUtil.mkdir("~/Downloads/发票/MW");
        FileUtil.mkdir("~/Downloads/发票/OT");
        FileUtil.mkdir("~/Downloads/发票/FT");

        List<File> files = FileUtil.loopFiles("~/Downloads/发票");
        for (File file : files) {
            extracted(file);
        }
    }

    private static void extracted(File file) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final String path = file.getPath().toLowerCase();
        try {
            if (!file.isFile()) {
                return;
            }
            Invoice invoice = null;
            String suffix = ".pdf";
            if (path.endsWith(".pdf")) {
                suffix = ".pdf";
                invoice = PdfInvoiceExtractor.extract(file);
            } else if (path.endsWith(".ofd")) {
                suffix = ".ofd";
                invoice = OfdInvoiceExtractor.extract(file);
            } else {
                return;
            }
            stopWatch.stop();
            System.out.println(path + ":" + stopWatch.getTotalTimeSeconds());
            System.out.println(JSONUtil.toJsonStr(invoice));

//            {"date":"2023年01月11日","amount":1811.47,"code":"044002200711",
//            "sellerName":"中国南方航空股份有限公司","drawer":"南方航空",
//            "sellerAddress":"广东省广州市白云区齐心路68号02095539","reviewer":"南方航空",
//            "title":"广东增值税电子普通发票",
//            "buyerName":"大连云起科技有限公司",
//            "type":"普通发票","buyerCode":"91210211MA116PYR8T","payee":"南方航空",
//            "number":"06929591","totalAmountString":"壹仟玖佰柒拾圆整",
//            "totalAmount":1970,"sellerCode":"91440000100017600N","buyerAddress":"",
//            "password":"-05<93/*4235*7>+5+96<*658+/\n/2/+-89802416>9+77<696109</\n0-6194<9*53*-138*7>-5+962/0\n48+1/0>+>1>922050/>*9364066","buyerAccount":"","sellerAccount":"工商银行机场支行3602065209000097893","machineNumber":"661616285428","checksum":"63732355113885436036",
//            "detailList":[{"amount":1651.38,"count":1,"taxRate":0.09,"price":1651.38,"name":"*运输服务*客票款 *代收民航发展基金*民航发展基金 ","taxAmount":148.62},{"amount":50,"count":1,"taxRate":0,"price":50,"name":"*运输服务*客运燃油附加费 ","taxAmount":0},{"amount":110.09,"count":1,"taxRate":0.09,"price":110.09,"name":"","taxAmount":9.91}],"taxAmount":158.53}

            // 开票日期去掉年月日
            String date = invoice.getDate();
            date = date.replaceAll("日", "");
            date = date.replaceAll("[\\u4E00-\\u9FA5]", "-");
            String buyerName = invoice.getBuyerName();
            String totalAmountString = invoice.getTotalAmount().longValue() + "";

            String destPath = "";
            if (buyerName.contains("云起")) {
                destPath = "~/Downloads/发票/YQ/YQ_" + date + "_" + totalAmountString + suffix;
            } else if (buyerName.contains("新颜")) {
                destPath = "~/Downloads/发票/XY/XY_" + date + "_" + totalAmountString + suffix;
            } else if (buyerName.contains("迈威")) {
                destPath = "~/Downloads/发票/MW/MW_" + date + "_" + totalAmountString + suffix;
            } else {
                String sellerName = invoice.getSellerName();
                destPath = "~/Downloads/发票/OT/" + sellerName + "-" + buyerName + "-" +file.getName();
            }
            FileUtil.copyFile(file.getAbsolutePath(), destPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            System.out.println(path + ":" + stopWatch.getTotalTimeSeconds() + ":Failed");
            e.printStackTrace();

            String destPath = "~/Downloads/发票/FT/" + file.getName();
            FileUtil.copyFile(file.getAbsolutePath(), destPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
