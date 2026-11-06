package com.cloudhis.service;

import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class RegistrationService {
    private final EncounterMapper encounters;private final CatalogMapper catalog;private final OrderMapper orders;
    private final FinanceMapper finance;private final BillingService billing;private final AuthService auth;
    public RegistrationService(EncounterMapper e,CatalogMapper c,OrderMapper o,FinanceMapper f,BillingService b,AuthService a) {encounters=e;catalog=c;orders=o;finance=f;billing=b;auth=a;}
    @Transactional public Map<String,Object> patient(Map<String,Object> user,Requests.Patient req) {
        Map<String,Object> row=map("id",id(),"case_no",number("P"),"name",req.name().trim(),"gender",req.gender(),"birth_date",req.birthDate(),"age",req.age(),"settle_type",req.settleType(),"id_card",req.idCard(),"address",req.address(),"phone",req.phone());
        encounters.addPatient(row);auth.audit(user,"PATIENT_CREATE",str(row,"id"),"登记患者主档"); return encounters.patient(str(row,"id"));
    }
    @Transactional public Map<String,Object> register(Map<String,Object> user,Requests.Registration req) {
        Map<String,Object> previous=finance.byRequest(req.requestKey());
        if(previous!=null) {
            Map<String,Object> e=encounters.get(str(previous,"encounter_id"));
            check(e!=null && str(e,"patient_id").equals(req.patientId()) && str(e,"schedule_id").equals(req.scheduleId()) && str(previous,"cashier_id").equals(str(user,"id")) && num(previous,"received_cents")==req.receivedCents() && str(previous,"pay_method").equals(req.payMethod()) && Boolean.TRUE.equals(e.get("book_requested"))==req.bookRequested(),"请求编号已使用，请重新发起挂号");
            return map("encounter",e,"receipt",billing.receipt(str(previous,"id")));
        }
        must(encounters.patient(req.patientId()),"患者不存在");
        Map<String,Object> schedule=must(catalog.schedule(req.scheduleId()),"排班不存在");
        check(LocalDate.parse(str(schedule,"visit_date")).equals(LocalDate.now(ZoneId.of("Asia/Shanghai"))),"仅支持当天现场挂号，请选择今日排班");
        check(Boolean.TRUE.equals(schedule.get("doctor_active")),"医生账号已停用");
        check(catalog.reserve(req.scheduleId())==1,"号源已满，请选择其他医生");
        String encounterId=id();
        encounters.add(map("id",encounterId,"visit_no",number("V"),"patient_id",req.patientId(),"schedule_id",req.scheduleId(),"doctor_id",str(schedule,"doctor_id"),"department_id",str(schedule,"department_id"),"book_requested",req.bookRequested()));
        String regOrder=id();long fee=num(schedule,"fee_cents")+(req.bookRequested()?100:0);
        orders.add(map("id",regOrder,"encounter_id",encounterId,"kind","REG","item_name",req.bookRequested()?"门诊挂号费（含病历本1元）":"门诊挂号费","unit_price_cents",fee,"quantity",1,"amount_cents",fee,"billing_status","UNPAID","execution_status","PENDING","doctor_id",str(schedule,"doctor_id")));
        Map<String,Object> receipt=billing.charge(user,new Requests.Charge(encounterId,List.of(regOrder),req.payMethod(),req.receivedCents(),req.requestKey()));
        auth.audit(user,"REGISTER",encounterId,"窗口挂号并收费");return map("encounter",encounters.get(encounterId),"receipt",receipt);
    }
    @Transactional public Map<String,Object> cancel(Map<String,Object> user,String id,String reason) {
        Map<String,Object> row=must(encounters.lock(id),"挂号不存在");
        check(str(row,"status").equals("REGISTERED"),"仅未看诊挂号可以退号");
        check(orders.activeCount(id)==0,"该挂号已有诊疗项目，不能退号");
        Map<String,Object> registration=orders.forEncounter(id).stream().filter(o->str(o,"kind").equals("REG")).findFirst().orElseThrow();
        Map<String,Object> refund=billing.refund(user,str(registration,"id"),reason,true);
        encounters.status(id,"CANCELLED");catalog.release(str(row,"schedule_id"));auth.audit(user,"REGISTRATION_CANCEL",id,reason);
        return refund;
    }
}
