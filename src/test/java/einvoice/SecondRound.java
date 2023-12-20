package einvoice;

import ch.qos.logback.classic.Level;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import com.sanluan.einvoice.service.Invoice;
import com.sanluan.einvoice.service.PdfInvoiceExtractor;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SecondRound {

    public static void main(String[] args) throws IOException {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.toLevel("error"));

        FileUtil.del("~/Downloads/发票/第二轮/MW");
        FileUtil.mkdir("~/Downloads/发票/第二轮/MW");

        List<File> files = FileUtil.loopFiles("~/Downloads/发票/第一轮/MW/");
        List<Invoice> invoices = new ArrayList<>();
        for (File file : files) {
            Invoice invoice = extracted(file);
            if (invoice != null) {
                invoices.add(invoice);
            }
        }

        invoices.sort(Comparator.comparing(Invoice::getDate));

        CsvWriter writer = CsvUtil.getWriter("~/Downloads/发票/第二轮/MW.csv", CharsetUtil.CHARSET_UTF_8);
        for (Invoice invoice : invoices) {
            String date = invoice.getDate();
            String code = invoice.getCode();
            //String totalAmountString = invoice.getTotalAmountString();
            String totalAmountString = invoice.getTotalAmount().toString() + "";
            String buyerName = invoice.getBuyerName();
            String buyerCode = invoice.getBuyerCode();
            String sellerName = invoice.getSellerName();
            String sellerCode = invoice.getSellerCode();
            Invoice.Detail detail = invoice.getDetailList().get(0);
            String model = detail.getModel();
            String destPath = "" + date + "_" +
                    code + "_" +
                    sellerName + "_" +
                    invoice.getTotalAmount().longValue() + ".pdf";
            writer.write(
                    new String[]{date,
                            code,
                            buyerName,
                            buyerCode,
                            sellerName,
                            sellerCode,
                            model,
                            totalAmountString,
                            destPath}
            );
        }
        writer.close();
    }

    private static Invoice extracted(File file) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final String path = file.getPath().toLowerCase();
        if (!file.isFile()) {
            throw new RuntimeException("noop pdf.");
        }
        Invoice invoice = null;
        String suffix = ".pdf";
        if (path.endsWith(".pdf")) {
            suffix = ".pdf";
            invoice = PdfInvoiceExtractor.extract(file);
        } else {
            System.out.println("noop pdf." + path);
            return null;
        }
        stopWatch.stop();
//            System.out.println(path + ":" + stopWatch.getTotalTimeSeconds());
//            System.out.println(JSONUtil.toJsonStr(invoice));

        // 开票日期去掉年月日
        String date = invoice.getDate();
        date = date.replaceAll("日", "");
        date = date.replaceAll("[\\u4E00-\\u9FA5]", "-");
        invoice.setDate(date);

        String code = invoice.getCode();
        String totalAmountString = invoice.getTotalAmount().longValue() + "";
        String sellerName = invoice.getSellerName();

        String destPath = "~/Downloads/发票/第二轮/MW/" + date + "_" +
                code + "_" +
                sellerName + "_" +
                totalAmountString + suffix;
        FileUtil.copyFile(file.getAbsolutePath(), destPath, StandardCopyOption.REPLACE_EXISTING);

        return invoice;
    }
}
