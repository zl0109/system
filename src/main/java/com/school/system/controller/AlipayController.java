package com.school.system.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.school.system.entity.FeeRecord;
import com.school.system.repository.FeeRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
@CrossOrigin
public class AlipayController {

    // 🌟 从 application.yml 中自动读取你刚刚配置的沙箱参数
    @Value("${alipay.appId}")
    private String appId;

    @Value("${alipay.appPrivateKey}")
    private String appPrivateKey;

    @Value("${alipay.alipayPublicKey}")
    private String alipayPublicKey;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.returnUrl}")
    private String returnUrl;

    @Value("${alipay.gatewayUrl}")
    private String gatewayUrl;

    @Autowired
    private FeeRecordRepository feeRecordRepository; // 注入缴费记录的数据库操作库

    /**
     * 接口 1：前端调用此接口，生成支付宝的付款页面 HTML
     */
    @GetMapping("/pay")
    public void pay(@RequestParam String recordId,
                    @RequestParam String amount,
                    @RequestParam String title,
                    HttpServletResponse httpResponse) throws Exception {

        AlipayClient alipayClient = new DefaultAlipayClient(
                gatewayUrl,
                appId,
                appPrivateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);

        // 关键修改：支付成功后跳转到自己的状态更新接口
        request.setReturnUrl("http://localhost:8080/fee/paySuccess?recordId=" + recordId);

        String bizContent = "{\"out_trade_no\":\"" + recordId + "\","
                + "\"total_amount\":\"" + amount + "\","
                + "\"subject\":\"" + title + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}";
        request.setBizContent(bizContent);

        String form = alipayClient.pageExecute(request).getBody();

        httpResponse.setContentType("text/html;charset=UTF-8");
        httpResponse.getWriter().write(form);
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    /**
     * 接口 2：支付宝服务器异步通知回调接口（Webhook）
     * 只有家长付款成功，支付宝才会偷偷调用这个接口告诉我们
     */
    @PostMapping("/notify")
    public String alipayNotify(HttpServletRequest request) throws Exception {
        // 1. 获取支付宝 POST 过来反馈信息
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        // 2. 调用支付宝官方的验签方法，防止黑客伪造付款成功请求！！！（极其关键的安全保障）
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");

        if (signVerified) {
            // 验证成功，提取交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

            // 只有交易状态为 TRADE_SUCCESS（支付成功）才处理
            if (trade_status.equals("TRADE_SUCCESS")) {
                // 拿到当时传给支付宝的 recordId（也就是我们要销账的明细记录的主键）
                String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
                Integer recordId = Integer.valueOf(out_trade_no);

                // 核心业务核销：去数据库把这笔缴费标记为 "已支付"
                FeeRecord record = feeRecordRepository.findById(recordId).orElse(null);
                if (record != null && record.getPayStatus() == 0) {
                    record.setPayStatus(1); // 1 代表已支付
                    record.setPayTime(new java.util.Date());
                    feeRecordRepository.save(record);
                }
            }
            return "success"; // 必须给支付宝返回 success，否则它会间隔 1m, 5m, 10m 一直疯狂重发通知！
        } else {
            return "failure"; // 验签失败，拒绝处理
        }
    }
}