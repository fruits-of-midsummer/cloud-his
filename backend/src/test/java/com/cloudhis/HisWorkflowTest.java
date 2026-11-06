package com.cloudhis;

import com.fasterxml.jackson.databind.*;
import com.cloudhis.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.cloudhis.common.Values.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","his.initial-password=TestPass2026!","his.seed=true"})
@AutoConfigureMockMvc
class HisWorkflowTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    @Autowired CatalogMapper catalog;@Autowired AuthMapper users;@Autowired EncounterMapper encounters;@Autowired OrderMapper orders;
    @BeforeEach void clean() {
        for(String table:List.of("audit_log","execution_result","refund","invoice_line","invoice","drug_movement","order_item","prescription","diagnosis","medical_record","encounter")) jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE schedule SET booked=0,quota=30");jdbc.update("UPDATE medicine SET stock=0,active=TRUE");jdbc.update("UPDATE service_item SET active=TRUE");jdbc.update("UPDATE app_user SET active=TRUE");
    }
    MockHttpSession login(String username) throws Exception {
        MvcResult begin=mvc.perform(get("/api/auth/session")).andReturn();MockHttpSession s=(MockHttpSession)begin.getRequest().getSession();
        call("POST","/api/auth/login",map("username",username,"password","TestPass2026!"),s,200);return s;
    }
    MvcResult raw(String method,String path,Object body,MockHttpSession session) throws Exception {
        var builder=request(HttpMethod.valueOf(method),path).contentType("application/json");
        if(session!=null)builder.session(session).header("X-CSRF-Token",Objects.toString(session.getAttribute("csrf"),""));
        if(body!=null)builder.content(json.writeValueAsBytes(body));return mvc.perform(builder).andReturn();
    }
    JsonNode call(String method,String path,Object body,MockHttpSession session,int status) throws Exception {
        MvcResult r=raw(method,path,body,session);assertEquals(status,r.getResponse().getStatus(),r.getResponse().getContentAsString());
        return json.readTree(r.getResponse().getContentAsByteArray());
    }
    String patient() {return jdbc.queryForObject("SELECT id FROM patient ORDER BY case_no LIMIT 1",String.class);}
    String schedule() {String today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString();return catalog.schedules(today,today).stream().filter(s->str(s,"doctor_id").equals(str(users.byUsername("doctor"),"id"))).findFirst().map(s->str(s,"id")).orElseThrow();}
    JsonNode register(MockHttpSession session,boolean book,long received,int expected) throws Exception {
        return call("POST","/api/registrations",map("patient_id",patient(),"schedule_id",schedule(),"book_requested",book,"pay_method","现金","received_cents",received,"request_key",id()),session,expected);
    }
    String visit(MockHttpSession cashier) throws Exception {return register(cashier,false,2000,200).path("encounter").path("id").asText();}
    void record(MockHttpSession doctor,String encounter) throws Exception {call("PUT","/api/encounters/"+encounter+"/record",map("chief_complaint","咳嗽三天","initial_diagnosis","上呼吸道感染","version",0),doctor,200);}
    void diagnose(MockHttpSession doctor,String encounter) throws Exception {call("POST","/api/encounters/"+encounter+"/diagnosis",map("disease_name","上呼吸道感染","opinion","复诊观察"),doctor,200);}
    String service(MockHttpSession doctor,String encounter,String kind) throws Exception {
        String item=catalog.items().stream().filter(i->str(i,"kind").equals(kind)).findFirst().map(i->str(i,"id")).orElseThrow();
        JsonNode rows=call("POST","/api/orders",map("encounter_id",encounter,"item_ids",List.of(item),"purpose","进一步诊断"),doctor,200);
        for(JsonNode row:rows)if(row.path("kind").asText().equals(kind))return row.path("id").asText();throw new AssertionError("No order");
    }
    JsonNode charge(MockHttpSession cashier,String encounter,List<String> ids,String requestKey) throws Exception {
        long amount=ids.stream().map(orders::get).mapToLong(o->num(o,"amount_cents")).sum();
        return call("POST","/api/charges",map("encounter_id",encounter,"order_ids",ids,"pay_method","移动支付","received_cents",amount,"request_key",requestKey),cashier,200);
    }
    @Test void sessionCsrfAndRoleProtection() throws Exception {
        call("GET","/api/catalog",null,null,401);
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"TestPass2026!\"}")).andExpect(r->assertEquals(403,r.getResponse().getStatus()));
        MockHttpSession cashier=login("cashier");call("GET","/api/admin/users",null,cashier,403);
        String catalogBody=call("GET","/api/catalog",null,cashier,200).toString();assertFalse(catalogBody.contains("password_hash"));
    }
    @Test void unsupportedMethodIsAClientError() throws Exception {
        call("POST","/api/encounters/example/record",map("chief_complaint","测试"),login("doctor"),405);
    }
    @Test void registrationChargesBookAndRollsBackInsufficientPayment() throws Exception {
        MockHttpSession cashier=login("cashier");register(cashier,true,2000,409);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM encounter",Integer.class));assertEquals(0,num(catalog.schedule(schedule()),"booked"));
        JsonNode r=register(cashier,true,2200,200);assertEquals(2100,r.path("receipt").path("total_cents").asLong());assertEquals(100,r.path("receipt").path("change_cents").asLong());
        assertEquals(1,call("GET","/api/encounters",null,login("doctor"),200).path("total").asInt());
    }
    @Test void lastQuotaIsSafeUnderConcurrentRegistration() throws Exception {
        jdbc.update("UPDATE schedule SET quota=1 WHERE id=?",schedule());MockHttpSession cashier=login("cashier");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> outcomes=new ArrayList<>();
            for(int i=0;i<2;i++)outcomes.add(pool.submit(()->raw("POST","/api/registrations",map("patient_id",patient(),"schedule_id",schedule(),"book_requested",false,"pay_method","现金","received_cents",2000,"request_key",id()),cashier).getResponse().getStatus()));
            List<Integer> statuses=new ArrayList<>();for(Future<Integer> f:outcomes)statuses.add(f.get(15,TimeUnit.SECONDS));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);
            assertEquals(1,num(catalog.schedule(schedule()),"booked"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM encounter",Integer.class));
        }finally{pool.shutdownNow();}
    }
    @Test void cancellationRefundsAndRestoresQuotaOnlyBeforeConsultation() throws Exception {
        MockHttpSession cashier=login("cashier");String encounter=visit(cashier);
        call("POST","/api/encounters/"+encounter+"/cancel",map("reason","挂错科室"),cashier,200);assertEquals("CANCELLED",str(encounters.get(encounter),"status"));assertEquals(0,num(catalog.schedule(schedule()),"booked"));
        call("POST","/api/encounters/"+encounter+"/cancel",map("reason","重复"),cashier,409);
        String second=visit(cashier);record(login("doctor"),second);call("POST","/api/encounters/"+second+"/cancel",map("reason","已接诊"),cashier,409);
    }
    @Test void recordsHaveVersionAndAssignedDoctorAccess() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);
        assertEquals(1,num(encounters.record(encounter),"version"));
        call("PUT","/api/encounters/"+encounter+"/record",map("chief_complaint","覆盖旧版本","initial_diagnosis","初诊","version",0),doctor,409);
        call("GET","/api/encounters/"+encounter,null,login("doctor2"),403);
        call("PUT","/api/encounters/"+encounter+"/record",map("chief_complaint","越权","initial_diagnosis","初诊","version",1),cashier,403);
        assertTrue(call("GET","/api/encounters/"+encounter,null,cashier,200).path("record").isNull());
    }
    @Test void diagnosisRequiredBeforeTreatmentAndPrescription() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);
        String treat=catalog.items().stream().filter(i->str(i,"kind").equals("TREAT")).findFirst().map(i->str(i,"id")).orElseThrow();
        call("POST","/api/orders",map("encounter_id",encounter,"item_ids",List.of(treat)),doctor,404);
        assertEquals(1,orders.forEncounter(encounter).size());diagnose(doctor,encounter);assertFalse(service(doctor,encounter,"TREAT").isBlank());
    }
    @Test void chargesAreIdempotentAndConcurrentDoubleChargeIsRejected() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);String order=service(doctor,encounter,"CHECK");String request=id();
        JsonNode a=charge(cashier,encounter,List.of(order),request);JsonNode b=charge(cashier,encounter,List.of(order),request);assertEquals(a.path("id"),b.path("id"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM invoice",Integer.class));
        String lab=service(doctor,encounter,"LAB");long amount=num(orders.get(lab),"amount_cents");ExecutorService pool=Executors.newFixedThreadPool(2);
        try{List<Future<Integer>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->raw("POST","/api/charges",map("encounter_id",encounter,"order_ids",List.of(lab),"pay_method","现金","received_cents",amount,"request_key",id()),cashier).getResponse().getStatus()));List<Integer> statuses=new ArrayList<>();for(Future<Integer> j:jobs)statuses.add(j.get(15,TimeUnit.SECONDS));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}finally{pool.shutdownNow();}
    }
    @Test void allThreeAncillaryKindsRequirePaymentAndPreventRefundAfterRegistration() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);diagnose(doctor,encounter);
        for(String kind:List.of("CHECK","LAB","TREAT")) {
            String order=service(doctor,encounter,kind);MockHttpSession executor=login(kind.toLowerCase());String executorId=str(users.byUsername(kind.toLowerCase()),"id");
            call("POST","/api/orders/"+order+"/register",map("executor_id",executorId),executor,409);
            charge(cashier,encounter,List.of(order),id());
            if(kind.equals("CHECK"))call("POST","/api/orders/"+order+"/register",map("executor_id",executorId),login("lab"),403);
            call("POST","/api/orders/"+order+"/register",map("executor_id",executorId),executor,200);
            call("POST","/api/orders/"+order+"/refund",map("reason","已登记"),cashier,409);
            call("POST","/api/orders/"+order+"/result",map("content","执行结果正常"),executor,200);
            call("POST","/api/orders/"+order+"/result",map("content","重复结果"),executor,409);
            assertEquals("COMPLETED",str(orders.get(order),"execution_status"));
        }
        JsonNode detail=call("GET","/api/encounters/"+encounter,null,doctor,200);assertTrue(detail.toString().contains("执行结果正常"));
        call("POST","/api/encounters/"+encounter+"/finish",null,doctor,200);
        call("POST","/api/encounters/"+encounter+"/diagnosis",map("disease_name","事后修改"),doctor,409);
    }
    @Test void unconsumedRefundCannotBeRepeated() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);String order=service(doctor,encounter,"LAB");charge(cashier,encounter,List.of(order),id());
        call("POST","/api/orders/"+order+"/refund",map("reason","患者放弃检查"),cashier,200);assertEquals("REFUNDED",str(orders.get(order),"billing_status"));
        call("POST","/api/orders/"+order+"/refund",map("reason","重复"),cashier,409);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM refund",Integer.class));
    }
    @Test void pharmacyInsufficientStockRollbackReturnAndRefund() throws Exception {
        MockHttpSession cashier=login("cashier"),doctor=login("doctor"),pharmacy=login("pharmacy");String encounter=visit(cashier);record(doctor,encounter);diagnose(doctor,encounter);
        String medicine=str(catalog.medicines().get(0),"id");
        JsonNode rx=call("POST","/api/prescriptions",map("encounter_id",encounter,"items",List.of(map("medicine_id",medicine,"quantity",2,"drug_usage","口服","dosage","每次1片","frequency","每日两次","days",3))),doctor,200);
        String order="";for(JsonNode item:rx)if(item.path("kind").asText().equals("DRUG"))order=item.path("id").asText();assertFalse(order.isBlank());charge(cashier,encounter,List.of(order),id());
        call("POST","/api/orders/"+order+"/dispense",null,pharmacy,409);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM drug_movement",Integer.class));
        call("POST","/api/stock/in",map("medicine_id",medicine,"quantity",10,"reason","采购验收入库"),pharmacy,200);
        call("POST","/api/orders/"+order+"/dispense",null,pharmacy,200);assertEquals(8,num(catalog.medicine(medicine),"stock"));
        call("POST","/api/orders/"+order+"/refund",map("reason","未退药"),cashier,409);
        call("POST","/api/orders/"+order+"/return",map("reason","患者退回"),pharmacy,200);assertEquals(10,num(catalog.medicine(medicine),"stock"));
        call("POST","/api/orders/"+order+"/return",map("reason","重复"),pharmacy,409);
        call("POST","/api/orders/"+order+"/refund",map("reason","药房已退"),cashier,200);assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM drug_movement",Integer.class));
    }
    @Test void deactivatedUserLosesExistingSessionAndAuditIsRestricted() throws Exception {
        MockHttpSession lab=login("lab"),admin=login("admin");
        call("PUT","/api/admin/users/"+str(users.byUsername("lab"),"id")+"/active",map("active",false),admin,200);
        call("GET","/api/catalog",null,lab,401);assertTrue(call("GET","/api/admin/audit",null,admin,200).path("total").asInt()>0);
    }
    @Test void medicinePriceUpdatePreservesExistingOrderSnapshot() throws Exception {
        MockHttpSession admin=login("admin"),cashier=login("cashier"),doctor=login("doctor");String encounter=visit(cashier);record(doctor,encounter);String order=service(doctor,encounter,"CHECK");Map<String,Object> item=catalog.item(str(orders.get(order),"service_item_id"));long oldPrice=num(orders.get(order),"amount_cents");
        call("PUT","/api/admin/items/"+str(item,"id"),map("code",str(item,"code"),"name",str(item,"name"),"kind",str(item,"kind"),"specification",str(item,"specification"),"department_id",str(item,"department_id"),"price_cents",oldPrice+100),admin,200);
        assertEquals(oldPrice,num(orders.get(order),"amount_cents"));
        jdbc.update("UPDATE service_item SET price_cents=? WHERE id=?",oldPrice,str(item,"id"));
    }
}
