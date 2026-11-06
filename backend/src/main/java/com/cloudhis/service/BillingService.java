package com.cloudhis.service;

import com.cloudhis.common.*;
import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class BillingService {
    private final FinanceMapper finance;
    private final OrderMapper orders;
    private final EncounterMapper encounters;
    private final AuthService auth;
    public BillingService(FinanceMapper finance,OrderMapper orders,EncounterMapper encounters,AuthService auth) { this.finance=finance;this.orders=orders;this.encounters=encounters;this.auth=auth; }
    private String fingerprint(Requests.Charge req) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((req.encounterId()+"|"+new TreeSet<>(req.orderIds())+"|"+req.payMethod()+"|"+req.receivedCents()).getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional public Map<String,Object> charge(Map<String,Object> user,Requests.Charge req) {
        check(Set.of("现金","银行卡","移动支付","医保").contains(req.payMethod()),"支付方式无效");
        Map<String,Object> encounter=must(encounters.lock(req.encounterId()),"就诊不存在");
        check(!str(encounter,"status").equals("CANCELLED"),"该挂号已取消");
        Map<String,Object> existing=finance.byRequest(req.requestKey());
        String fingerprint=fingerprint(req);
        if(existing!=null) {
            check(str(existing,"fingerprint").equals(fingerprint) && str(existing,"cashier_id").equals(str(user,"id")),"该请求编号已用于其他收款");
            return receipt(str(existing,"id"));
        }
        check(req.orderIds().size()==new HashSet<>(req.orderIds()).size(),"收费项目重复");
        List<Map<String,Object>> items=new ArrayList<>(); long total=0;
        for(String orderId:new TreeSet<>(req.orderIds())) {
            Map<String,Object> row=must(orders.lock(orderId),"收费项目不存在");
            check(str(row,"encounter_id").equals(req.encounterId()),"收费项目不属于本次就诊");
            check(str(row,"billing_status").equals("UNPAID") && str(row,"execution_status").equals("PENDING"),"项目已收费、已撤销或状态已变化");
            total=Math.addExact(total,num(row,"amount_cents")); items.add(row);
        }
        check(req.receivedCents()>=total && req.receivedCents()<=100000000000L,"实收金额不足或超出允许范围");
        if(!req.payMethod().equals("现金")) check(req.receivedCents()==total,"非现金支付实收金额必须等于应收金额");
        String invoiceId=id();
        finance.addInvoice(map("id",invoiceId,"receipt_no",number("F"),"encounter_id",req.encounterId(),"request_key",req.requestKey(),"fingerprint",fingerprint,"total_cents",total,"received_cents",req.receivedCents(),"change_cents",req.receivedCents()-total,"pay_method",req.payMethod(),"cashier_id",str(user,"id")));
        for(Map<String,Object> item:items) {
            finance.addLine(map("id",id(),"invoice_id",invoiceId,"order_id",str(item,"id"),"amount_cents",num(item,"amount_cents")));
            orders.billing(str(item,"id"),"PAID");
        }
        auth.audit(user,"CHARGE",invoiceId,"收取费用，金额分："+total);
        return receipt(invoiceId);
    }
    public Map<String,Object> receipt(String id) { Map<String,Object> row=must(finance.invoice(id),"收费单不存在");row.put("lines",finance.lines(id));return row; }
    @Transactional public Map<String,Object> refund(Map<String,Object> user,String orderId,String reason,boolean registration) {
        Map<String,Object> row=must(orders.lock(orderId),"项目不存在");
        check(str(row,"billing_status").equals("PAID"),"只有已收费且未退费项目可以退款");
        String kind=str(row,"kind"), execution=str(row,"execution_status");
        check(!kind.equals("REG") || registration,"挂号费请通过退号处理");
        if(kind.equals("DRUG")) check(execution.equals("PENDING") || execution.equals("RETURNED"),"已发药必须先到药房退药");
        else check(execution.equals("PENDING"),"非药品项目已登记或执行，不能退费");
        Map<String,Object> line=must(finance.lineForOrder(orderId),"原收费记录不存在");
        Map<String,Object> refund=map("id",id(),"invoice_line_id",str(line,"id"),"amount_cents",num(line,"amount_cents"),"reason",reason,"operator_id",str(user,"id"));
        finance.refund(refund); orders.billing(orderId,"REFUNDED"); orders.execution(orderId,"CANCELLED");
        auth.audit(user,"REFUND",orderId,reason); return refund;
    }
}
